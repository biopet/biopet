/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.tools

import java.io.File

import com.google.common.hash.{ PrimitiveSink, Funnel, BloomFilter }
import htsjdk.samtools.{ QueryInterval, SAMFileWriter, SAMFileWriterFactory, SAMRecord, SamReader, SamReaderFactory, ValidationStringency }
import htsjdk.samtools.util.{ Interval, IntervalTreeMap }
import nl.lumc.sasc.biopet.utils.ToolCommand
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList
import org.apache.commons.io.FilenameUtils.getExtension

import scala.collection.JavaConverters._
import scala.io.Source
import scala.math.{ max, min }

object WipeReads extends ToolCommand {

  /** Creates a SamReader object from an input BAM file, ensuring it is indexed */
  private def prepInBam(inBam: File): SamReader = {
    val bam = SamReaderFactory
      .make()
      .validationStringency(ValidationStringency.LENIENT)
      .open(inBam)
    require(bam.hasIndex)
    bam
  }

  /** Creates a [[SAMFileWriter]] object for writing, indexed */
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
  def makeIntervalFromFile(inFile: File, gtfFeatureType: String = "exon"): List[Interval] = {

    logger.info("Parsing interval file ...")

    /** Function to create iterator from BED file */
    def makeIntervalFromBed(inFile: File) = BedRecordList.fromFile(inFile).sorted.toSamIntervals.toIterator

    /**
     * Parses a refFlat file to yield Interval objects
     *
     * Format description:
     * http://genome.csdb.cn/cgi-bin/hgTables?hgsid=6&hgta_doSchemaDb=hg18&hgta_doSchemaTable=refFlat
     *
     * @param inFile input refFlat file
     */
    def makeIntervalFromRefFlat(inFile: File): Iterator[Interval] =
      Source.fromFile(inFile)
        // read each line
        .getLines()
        // skip all empty lines
        .filterNot(x => x.trim.isEmpty)
        // split per column
        .map(line => line.trim.split("\t"))
        // take chromosome and exonEnds and exonStars
        .map(x => (x(2), x.reverse.take(2)))
        // split starts and ends based on comma
        .map(x => (x._1, x._2.map(y => y.split(","))))
        // zip exonStarts and exonEnds, note the index was reversed because we did .reverse above
        .map(x => (x._1, x._2(1).zip(x._2(0))))
        // make Intervals, accounting for the fact that refFlat coordinates are 0-based
        .map(x => x._2.map(y => new Interval(x._1, y._1.toInt + 1, y._2.toInt)))
        // flatten sublist
        .flatten

    /**
     * Parses a GTF file to yield Interval objects
     *
     * @param inFile input GTF file
     * @return
     */
    def makeIntervalFromGtf(inFile: File): Iterator[Interval] =
      Source.fromFile(inFile)
        // read each line
        .getLines()
        // skip all empty lines
        .filterNot(x => x.trim.isEmpty)
        // skip all UCSC track lines and/or ensembl comment lines
        .dropWhile(x => x.matches("^track | ^browser | ^#"))
        // split to columns
        .map(x => x.split("\t"))
        // exclude intervals whose type is different from the supplied one
        .filter(x => x(2) == gtfFeatureType)
        // and finally create the interval objects
        .map(x => new Interval(x(0), x(3).toInt, x(4).toInt))

    // detect interval file format from extension
    val iterFunc: (File => Iterator[Interval]) =
      if (getExtension(inFile.toString.toLowerCase) == "bed")
        makeIntervalFromBed
      else if (getExtension(inFile.toString.toLowerCase) == "refflat")
        makeIntervalFromRefFlat
      else if (getExtension(inFile.toString.toLowerCase) == "gtf")
        makeIntervalFromGtf
      else
        throw new IllegalArgumentException("Unexpected interval file type: " + inFile.getPath)

    iterFunc(inFile).toList
      .sortBy(x => (x.getContig, x.getStart, x.getEnd))
      .foldLeft(List.empty[Interval])(
        (acc, x) => {
          acc match {
            case head :: tail if x.intersects(head) =>
              new Interval(x.getContig, min(x.getStart, head.getStart), max(x.getEnd, head.getEnd)) :: tail
            case _ => x :: acc
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
      if (getIndex(iv.getContig) > -1)
        Some(new QueryInterval(getIndex(iv.getContig), iv.getStart, iv.getEnd))
      else if (iv.getContig.startsWith("chr") && getIndex(iv.getContig.substring(3)) > -1) {
        logger.warn("Removing 'chr' prefix from interval " + iv.toString)
        Some(new QueryInterval(getIndex(iv.getContig.substring(3)), iv.getStart, iv.getEnd))
      } else if (!iv.getContig.startsWith("chr") && getIndex("chr" + iv.getContig) > -1) {
        logger.warn("Adding 'chr' prefix to interval " + iv.toString)
        Some(new QueryInterval(getIndex("chr" + iv.getContig), iv.getStart, iv.getEnd))
      } else {
        logger.warn("Sequence " + iv.getContig + " does not exist in alignment")
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
      if (readGroupIds.isEmpty)
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

  /** Default arguments */
  case class Args(inputBam: File = new File(""),
                  targetRegions: File = new File(""),
                  outputBam: File = new File(""),
                  filteredOutBam: Option[File] = None,
                  readGroupIds: Set[String] = Set.empty[String],
                  minMapQ: Int = 0,
                  limitToRegion: Boolean = false,
                  noMakeIndex: Boolean = false,
                  featureType: String = "exon",
                  bloomSize: Long = 70000000,
                  bloomFp: Double = 4e-7) extends AbstractArgs

  /** Command line argument parser */
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

    opt[File]('r', "interval_file") required () valueName "<bed/gtf/refflat>" action { (x, c) =>
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

    note("\nGTF-only options:")

    opt[String]('t', "feature_type") optional () valueName "<gtf_feature_type>" action { (x, c) =>
      c.copy(featureType = x)
    } text "GTF feature containing intervals (default: exon)"

    note("\nAdvanced options:")

    opt[Long]("bloom_size") optional () action { (x, c) =>
      c.copy(bloomSize = x)
    } text "Expected maximum number of reads in target regions (default: 7e7)"

    opt[Double]("false_positive") optional () action { (x, c) =>
      c.copy(bloomFp = x)
    } text "False positive rate (default: 4e-7)"

    note(
      """
        |This tool will remove BAM records that overlaps a set of given regions.
        |By default, if the removed reads are also mapped to other regions outside
        |the given ones, they will also be removed.
      """.stripMargin)

  }

  /** Parses the command line argument */
  def parseArgs(args: Array[String]): Args = new OptParser()
    .parse(args, Args())
    .getOrElse(throw new IllegalArgumentException)

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = parseArgs(args)

    // cannot use SamReader as inBam directly since it only allows one active iterator at any given time
    val filterFunc = makeFilterNotFunction(
      ivl = makeIntervalFromFile(commandArgs.targetRegions, gtfFeatureType = commandArgs.featureType),
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
