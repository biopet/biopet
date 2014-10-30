/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.tools

import java.io.File
import scala.collection.JavaConverters._
import scala.math.{ max, min }

import com.google.common.hash.{ Funnel, BloomFilter, PrimitiveSink }
import htsjdk.samtools.SamReader
import htsjdk.samtools.SamReaderFactory
import htsjdk.samtools.QueryInterval
import htsjdk.samtools.ValidationStringency
import htsjdk.samtools.SAMFileWriter
import htsjdk.samtools.SAMFileWriterFactory
import htsjdk.samtools.SAMRecord
import htsjdk.samtools.util.{ Interval, IntervalTreeMap }
import htsjdk.tribble.AbstractFeatureReader.getFeatureReader
import htsjdk.tribble.bed.BEDCodec
import org.apache.commons.io.FilenameUtils.getExtension
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.ToolCommand
import nl.lumc.sasc.biopet.core.config.Configurable

// TODO: finish implementation for usage in pipelines
/**
 * WipeReads function class for usage in Biopet pipelines
 *
 * @param root Configuration object for the pipeline
 */
class WipeReads(val root: Configurable) extends BiopetJavaCommandLineFunction {

  javaMainClass = getClass.getName

  @Input(doc = "Input BAM file (must be indexed)", shortName = "I", required = true)
  var inputBam: File = _

  @Output(doc = "Output BAM", shortName = "o", required = true)
  var outputBam: File = _

}

object WipeReads extends ToolCommand {

  /**
   * Creates a SamReader object from an input BAM file, ensuring it is indexed
   *
   * @param inBam input BAM file
   * @return
   */
  private def prepInBam(inBam: File): SamReader = {
    val bam = SamReaderFactory
      .make()
      .validationStringency(ValidationStringency.LENIENT)
      .open(inBam)
    require(bam.hasIndex)
    bam
  }

  private def prepOutBam(outBam: File, templateBam: File,
                         writeIndex: Boolean = true, async: Boolean = true): SAMFileWriter =
    new SAMFileWriterFactory()
      .setCreateIndex(writeIndex)
      .setUseAsyncIo(async)
      .makeBAMWriter(prepInBam(templateBam).getFileHeader, true, outBam)

  /**
   * Creates a list of intervals given an input File
   *
   * @param inFile input interval file
   */
  def makeIntervalFromFile(inFile: File): List[Interval] = {

    logger.info("Parsing interval file ...")

    /** Function to create iterator from BED file */
    def makeIntervalFromBed(inFile: File): Iterator[Interval] =
      asScalaIteratorConverter(getFeatureReader(inFile.toPath.toString, new BEDCodec(), false).iterator)
        .asScala
        .map(x => new Interval(x.getChr, x.getStart, x.getEnd))

    /** Function to create iterator from refFlat file */
    def makeIntervalFromRefFlat(inFile: File): Iterator[Interval] = ???
      // convert coordinate to 1-based fully closed
      // parse chrom, start blocks, end blocks, strands

    /** Function to create iterator from GTF file */
    def makeIntervalFromGtf(inFile: File): Iterator[Interval] = ???
        // convert coordinate to 1-based fully closed
        // parse chrom, start blocks, end blocks, strands

    // detect interval file format from extension
    val iterFunc: (File => Iterator[Interval]) =
      if (getExtension(inFile.toString.toLowerCase) == "bed")
        makeIntervalFromBed
      else
        throw new IllegalArgumentException("Unexpected interval file type: " + inFile.getPath)

    iterFunc(inFile).toList
      .sortBy(x => (x.getSequence, x.getStart, x.getEnd))
      .foldLeft(List.empty[Interval])(
        (acc, x) => {
          acc match {
            case head :: tail if x.intersects(head) =>
              new Interval(x.getSequence, min(x.getStart, head.getStart), max(x.getEnd, head.getEnd)) :: tail
            case  _ => x :: acc
          }
        }
      )
  }

  // TODO: set minimum fraction for overlap
  /**
   * Function to create function to check SAMRecord for exclusion in filtered BAM file.
   *
   * The returned function evaluates all filtered-in SAMRecord to false.
   *
   * @param ivl iterator yielding Feature objects
   * @param inBam input BAM file
   * @param filterOutMulti whether to filter out reads with same name outside target region (default: true)
   * @param minMapQ minimum MapQ of reads in target region to filter out (default: 0)
   * @param readGroupIds read group IDs of reads in target region to filter out (default: all IDs)
   * @param bloomSize expected size of elements to contain in the Bloom filter
   * @param bloomFp expected Bloom filter false positive rate
   * @return function that checks whether a SAMRecord or String is to be excluded
   */
  def makeFilterNotFunction(ivl: List[Interval],
                            inBam: File,
                            filterOutMulti: Boolean = true,
                            minMapQ: Int = 0, readGroupIds: Set[String] = Set(),
                            bloomSize: Long, bloomFp: Double): (SAMRecord => Boolean) = {

    logger.info("Building set of reads to exclude ...")

    /**
     * Creates an Option[QueryInterval] object from the given Interval
     *
     * @param in input BAM file
     * @param iv input interval
     * @return
     */
    def makeQueryInterval(in: SamReader, iv: Interval): Option[QueryInterval] = {
      val getIndex = in.getFileHeader.getSequenceIndex _
      if (getIndex(iv.getSequence) > -1)
        Some(new QueryInterval(getIndex(iv.getSequence), iv.getStart, iv.getEnd))
      else if (iv.getSequence.startsWith("chr") && getIndex(iv.getSequence.substring(3)) > -1) {
        logger.warn("Removing 'chr' prefix from interval " + iv.toString)
        Some(new QueryInterval(getIndex(iv.getSequence.substring(3)), iv.getStart, iv.getEnd))
      }
      else if (!iv.getSequence.startsWith("chr") && getIndex("chr" + iv.getSequence) > -1) {
        logger.warn("Adding 'chr' prefix to interval " + iv.toString)
        Some(new QueryInterval(getIndex("chr" + iv.getSequence), iv.getStart, iv.getEnd))
      }
      else {
        logger.warn("Sequence " + iv.getSequence + " does not exist in alignment")
        None
      }
    }

    /**
     * Function to ensure that a SAMRecord overlaps our target regions
     *
     * This is required because htsjdk's queryOverlap method does not take into
     * account the SAMRecord splicing structure
     *
     * @param rec SAMRecord to check
     * @param ivtm mutable mapping of a chromosome and its interval tree
     * @return
     */
    def alignmentBlockOverlaps(rec: SAMRecord, ivtm: IntervalTreeMap[_]): Boolean =
      // if SAMRecord is not spliced, assume queryOverlap has done its job
      // otherwise check for alignment block overlaps in our interval list
      // using raw SAMString to bypass cigar string decoding
      if (rec.getSAMString.split("\t")(5).contains("N"))
        rec.getAlignmentBlocks.asScala
          .exists(x =>
            ivtm.containsOverlapping(
              new Interval(rec.getReferenceName,
                x.getReferenceStart, x.getReferenceStart + x.getLength - 1)))
      else
        true

    /** function to create a fake SAMRecord pair ~ hack to limit querying BAM file for real pair */
    def makeMockPair(rec: SAMRecord): SAMRecord = {
      require(rec.getReadPairedFlag)
      val fakePair = rec.clone.asInstanceOf[SAMRecord]
      fakePair.setAlignmentStart(rec.getMateAlignmentStart)
      fakePair
    }

    /** function to create set element from SAMRecord */
    def elemFromSam(rec: SAMRecord): String = {
      if (filterOutMulti)
        rec.getReadName
      else
        rec.getReadName + "_" + rec.getAlignmentStart.toString
    }

    /** object for use by BloomFilter */
    object SAMFunnel extends Funnel[SAMRecord] {
      override def funnel(rec: SAMRecord, into: PrimitiveSink): Unit = {
        val elem = elemFromSam(rec)
        logger.debug("Adding " + elem + " to set ...")
        into.putUnencodedChars(elem)
      }
    }

    /** filter function for read IDs */
    val rgFilter =
      if (readGroupIds.size == 0)
        (r: SAMRecord) => true
      else
        (r: SAMRecord) => readGroupIds.contains(r.getReadGroup.getReadGroupId)

    val readyBam = prepInBam(inBam)

    val queryIntervals = ivl
      .flatMap(x => makeQueryInterval(readyBam, x))
      // queryOverlapping only accepts a sorted QueryInterval collection ...
      .sortBy(x => (x.referenceIndex, x.start, x.end))
      // and it has to be an array
      .toArray

    val ivtm: IntervalTreeMap[_] = ivl
      .foldLeft(new IntervalTreeMap[Boolean])(
        (acc, x) => {
          acc.put(x, true)
          acc
        }
      )

    lazy val filteredOutSet: BloomFilter[SAMRecord] = readyBam
      // query BAM file with intervals
      .queryOverlapping(queryIntervals)
      // for compatibility
      .asScala
      // ensure spliced reads have at least one block overlapping target region
      .filter(x => alignmentBlockOverlaps(x, ivtm))
      // filter for MAPQ on target region reads
      .filter(x => x.getMappingQuality >= minMapQ)
      // filter on specific read group IDs
      .filter(x => rgFilter(x))
      // fold starting from empty set
      .foldLeft(BloomFilter.create(SAMFunnel, bloomSize.toInt, bloomFp)
        )((acc, rec) => {
            acc.put(rec)
            if (rec.getReadPairedFlag) acc.put(makeMockPair(rec))
            acc
          })

    if (filterOutMulti)
      (rec: SAMRecord) => filteredOutSet.mightContain(rec)
    else
      (rec: SAMRecord) => {
        if (rec.getReadPairedFlag)
          filteredOutSet.mightContain(rec) && filteredOutSet.mightContain(makeMockPair(rec))
        else
          filteredOutSet.mightContain(rec)
      }
  }

  /**
   * Function to filter input BAM and write its output to the filesystem
   *
   * @param filterFunc filter function that evaluates true for excluded SAMRecord
   * @param inBam input BAM file
   * @param outBam output BAM file
   * @param filteredOutBam whether to write excluded SAMRecords to their own BAM file
   */
  def writeFilteredBam(filterFunc: (SAMRecord => Boolean), inBam: SamReader, outBam: SAMFileWriter,
                       filteredOutBam: Option[SAMFileWriter] = None) = {

    logger.info("Writing output file(s) ...")
    try {
      var (incl, excl) = (0, 0)
      for (rec <- inBam.asScala) {
        if (!filterFunc(rec)) {
          outBam.addAlignment(rec)
          incl += 1
        } else {
          excl += 1
          filteredOutBam.foreach(x => x.addAlignment(rec))
        }
      }
      println(List("count_included", "count_excluded").mkString("\t"))
      println(List(incl, excl).mkString("\t"))
    } finally {
      inBam.close()
      outBam.close()
      filteredOutBam.foreach(x => x.close())
    }
  }

  case class Args(inputBam: File = new File(""),
                  targetRegions: File = new File(""),
                  outputBam: File = new File(""),
                  filteredOutBam: Option[File] = None,
                  readGroupIds: Set[String] = Set.empty[String],
                  minMapQ: Int = 0,
                  limitToRegion: Boolean = false,
                  noMakeIndex: Boolean = false,
                  bloomSize: Long = 70000000,
                  bloomFp: Double = 4e-7) extends AbstractArgs

  class OptParser extends AbstractOptParser {

    head(
      s"""
        |$commandName - Region-based reads removal from an indexed BAM file
      """.stripMargin)

    opt[File]('I', "input_file") required () valueName "<bam>" action { (x, c) =>
      c.copy(inputBam = x)
    } validate {
      x => if (x.exists) success else failure("Input BAM file not found")
    } text "Input BAM file"

    opt[File]('r', "interval_file") required () valueName "<bed>" action { (x, c) =>
      c.copy(targetRegions = x)
    } validate {
      x => if (x.exists) success else failure("Target regions file not found")
    } text "Interval BED file"

    opt[File]('o', "output_file") required () valueName "<bam>" action { (x, c) =>
      c.copy(outputBam = x)
    } text "Output BAM file"

    opt[File]('f', "discarded_file") optional () valueName "<bam>" action { (x, c) =>
      c.copy(filteredOutBam = Some(x))
    } text "Discarded reads BAM file (default: none)"

    opt[Int]('Q', "min_mapq") optional () action { (x, c) =>
      c.copy(minMapQ = x)
    } text "Minimum MAPQ of reads in target region to remove (default: 0)"

    opt[String]('G', "read_group") unbounded () optional () valueName "<rgid>" action { (x, c) =>
      c.copy(readGroupIds = c.readGroupIds + x)
    } text "Read group IDs to be removed (default: remove reads from all read groups)"

    opt[Unit]("limit_removal") optional () action { (_, c) =>
      c.copy(limitToRegion = true)
    } text
      "Whether to remove multiple-mapped reads outside the target regions (default: yes)"

    opt[Unit]("no_make_index") optional () action { (_, c) =>
      c.copy(noMakeIndex = true)
    } text
      "Whether to index output BAM file or not (default: yes)"

    note("\nAdvanced options")

    opt[Long]("bloom_size") optional () action { (x, c) =>
      c.copy(bloomSize = x)
    } text "expected maximum number of reads in target regions (default: 7e7)"

    opt[Double]("false_positive") optional () action { (x, c) =>
      c.copy(bloomFp = x)
    } text "false positive rate (default: 4e-7)"

    note(
      """
        |This tool will remove BAM records that overlaps a set of given regions.
        |By default, if the removed reads are also mapped to other regions outside
        |the given ones, they will also be removed.
      """.stripMargin)

  }

  /**
   * Parses the command line argument
   *
   * @param args Array of arguments
   * @return
   */
  def parseArgs(args: Array[String]): Args = new OptParser()
    .parse(args, Args())
    .getOrElse(sys.exit(1))

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = parseArgs(args)

    // cannot use SamReader as inBam directly since it only allows one active iterator at any given time
    val filterFunc = makeFilterNotFunction(
      ivl = makeIntervalFromFile(commandArgs.targetRegions),
      inBam = commandArgs.inputBam,
      filterOutMulti = !commandArgs.limitToRegion,
      minMapQ = commandArgs.minMapQ,
      readGroupIds = commandArgs.readGroupIds,
      bloomSize = commandArgs.bloomSize,
      bloomFp = commandArgs.bloomFp
    )

    writeFilteredBam(
      filterFunc,
      prepInBam(commandArgs.inputBam),
      prepOutBam(commandArgs.outputBam, commandArgs.inputBam, writeIndex = !commandArgs.noMakeIndex),
      commandArgs.filteredOutBam.map(x => prepOutBam(x, commandArgs.inputBam, writeIndex = !commandArgs.noMakeIndex))
    )
  }
}
