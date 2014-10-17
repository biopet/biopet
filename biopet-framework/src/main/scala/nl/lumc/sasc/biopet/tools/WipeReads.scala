/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.tools

import java.io.{ File, IOException }

import scala.collection.JavaConverters._
import scala.io.Source

import htsjdk.samtools.AlignmentBlock
import htsjdk.samtools.SAMFileReader
import htsjdk.samtools.SAMFileReader.QueryInterval
import htsjdk.samtools.SAMFileWriterFactory
import htsjdk.samtools.SAMRecord
import htsjdk.tribble.index.interval.{ Interval, IntervalTree }
import orestes.bloomfilter.HashProvider.HashMethod
import orestes.bloomfilter.{ BloomFilter, FilterBuilder }
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
  var inputBAM: File = _

  @Output(doc = "Output BAM", shortName = "o", required = true)
  var outputBAM: File = _

}

object WipeReads extends ToolCommand {

  /** Container type for command line flags */
  type OptionMap = Map[String, Any]

  /** Container class for interval parsing results */
  case class RawInterval(chrom: String, start: Int, end: Int) {

    require(start <= end, s"Start coordinate $start is larger than end coordinate $end")

    /** Function to check whether one interval contains the other */
    def contains(that: RawInterval): Boolean =
      if (this.chrom != that.chrom)
        false
      else
        this.start <= that.start && this.end >= that.end

    /** Function to check whether two intervals overlap each other */
    def overlaps(that: RawInterval): Boolean =
      if (this.chrom != that.chrom)
        false
      else
        this.start <= that.start && this.end >= that.start

    /** Function to merge two overlapping intervals */
    def merge(that: RawInterval): RawInterval = {
      if (this.chrom != that.chrom)
        throw new IllegalArgumentException("Can not merge RawInterval objects from different chromosomes")
      if (contains(that))
        this
      else if (overlaps(that))
        RawInterval(this.chrom, this.start, that.end)
      else
        throw new IllegalArgumentException("Can not merge non-overlapping RawInterval objects")
    }

  }

  /**
   * Function to create an iterator yielding non-overlapped RawInterval objects
   *
   * @param ri iterator yielding RawInterval objects
   * @return iterator yielding RawInterval objects
   */
  def mergeOverlappingIntervals(ri: Iterator[RawInterval]): Iterator[RawInterval] =
    ri.toList
      .sortBy(x => (x.chrom, x.start, x.end))
      .foldLeft(List.empty[RawInterval]) {
        (acc, i) => acc match {
          case head :: tail if head.overlaps(i) => head.merge(i) :: tail
          case _                                => i :: acc

        }}
      .toIterator

  /**
   * Function to create iterator over intervals from input interval file
   *
   * @param inFile input interval file
   */
  def makeRawIntervalFromFile(inFile: File): Iterator[RawInterval] = {

    /** Function to create iterator from BED file */
    def makeRawIntervalFromBED(inFile: File): Iterator[RawInterval] =
      // BED file coordinates are 0-based, half open so we need to do some conversion
      Source.fromFile(inFile)
        .getLines()
        .filterNot(_.trim.isEmpty)
        .dropWhile(_.matches("^track | ^browser "))
        .map(line => line.trim.split("\t") match {
          case Array(chrom, start, end, _*) => new RawInterval(chrom, start.toInt + 1, end.toInt)
        })

    // TODO: implementation
    /** Function to create iterator from refFlat file */
    def makeRawIntervalFromRefFlat(inFile: File): Iterator[RawInterval] = ???
    // convert coordinate to 1-based fully closed
    // parse chrom, start blocks, end blocks, strands

    // TODO: implementation
    /** Function to create iterator from GTF file */
    def makeRawIntervalFromGTF(inFile: File): Iterator[RawInterval] = ???
    // convert coordinate to 1-based fully closed
    // parse chrom, start blocks, end blocks, strands

    // detect interval file format from extension
    val iterFunc: (File => Iterator[RawInterval]) =
      if (getExtension(inFile.toString.toLowerCase) == "bed")
        makeRawIntervalFromBED
      else
        throw new IllegalArgumentException("Unexpected interval file type: " + inFile.getPath)

    mergeOverlappingIntervals(iterFunc(inFile))
  }

  // TODO: set minimum fraction for overlap
  /**
   * Function to create function to check SAMRecord for exclusion in filtered BAM file.
   *
   * The returned function evaluates all filtered-in SAMRecord to false.
   *
   * @param iv iterator yielding RawInterval objects
   * @param inBAM input BAM file
   * @param inBAMIndex input BAM file index
   * @param filterOutMulti whether to filter out reads with same name outside target region (default: true)
   * @param minMapQ minimum MapQ of reads in target region to filter out (default: 0)
   * @param readGroupIDs read group IDs of reads in target region to filter out (default: all IDs)
   * @param bloomSize expected size of elements to contain in the Bloom filter
   * @param bloomFp expected Bloom filter false positive rate
   * @return function that checks whether a SAMRecord or String is to be excluded
   */
  def makeFilterOutFunction(iv: Iterator[RawInterval],
                            inBAM: File, inBAMIndex: File = null,
                            filterOutMulti: Boolean = true,
                            minMapQ: Int = 0, readGroupIDs: Set[String] = Set(),
                            bloomSize: Int = 100000000, bloomFp: Double = 1e-10): (SAMRecord => Boolean) = {

    // TODO: implement optional index creation
    /** Function to check for BAM file index and return a SAMFileReader given a File */
    def prepIndexedInputBAM(): SAMFileReader =
      if (inBAMIndex != null)
        new SAMFileReader(inBAM, inBAMIndex)
      else {
        val sfr = new SAMFileReader(inBAM)
        sfr.setValidationStringency(SAMFileReader.ValidationStringency.LENIENT)
        if (!sfr.hasIndex)
          throw new IllegalStateException("Input BAM file must be indexed")
        else
          sfr
      }

    /**
     * Function to query intervals from a BAM file
     *
     * The function still works when only either one of the interval or BAM
     * file contig is prepended with "chr"
     *
     * @param inBAM BAM file to query as SAMFileReader
     * @param ri raw interval object containing query
     * @return QueryInterval wrapped in Option
     */
    def monadicMakeQueryInterval(inBAM: SAMFileReader, ri: RawInterval): Option[QueryInterval] =
      if (inBAM.getFileHeader.getSequenceIndex(ri.chrom) > -1)
        Some(inBAM.makeQueryInterval(ri.chrom, ri.start, ri.end))
      else if (ri.chrom.startsWith("chr")
        && inBAM.getFileHeader.getSequenceIndex(ri.chrom.substring(3)) > -1)
        Some(inBAM.makeQueryInterval(ri.chrom.substring(3), ri.start, ri.end))
      else if (!ri.chrom.startsWith("chr")
        && inBAM.getFileHeader.getSequenceIndex("chr" + ri.chrom) > -1)
        Some(inBAM.makeQueryInterval("chr" + ri.chrom, ri.start, ri.end))
      else
        None

    /** function to make IntervalTree from our RawInterval objects
      *
      * @param ri iterable over RawInterval objects
      * @return IntervalTree objects, filled with intervals from RawInterval
      */
    def makeIntervalTree(ri: Iterable[RawInterval]): IntervalTree = {
      val ivt = new IntervalTree
      for (iv: RawInterval <- ri)
        ivt.insert(new Interval(iv.start, iv.end))
      ivt
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
    def alignmentBlockOverlaps(rec: SAMRecord, ivtm: Map[String, IntervalTree]): Boolean =
      // if SAMRecord is not spliced, assume queryOverlap has done its job
      // otherwise check for alignment block overlaps in our interval list
      // using raw SAMString to bypass cigar string decoding
      if (rec.getSAMString.split("\t")(5).contains("N")) {
        for (ab: AlignmentBlock <- rec.getAlignmentBlocks.asScala) {
          if (!ivtm(rec.getReferenceName).findOverlapping(
            new Interval(ab.getReferenceStart, ab.getReferenceStart + ab.getLength - 1)).isEmpty)
            return true
        }
        false
      } else
        true

    /** filter function for read IDs */
    val rgFilter =
      if (readGroupIDs.size == 0)
        (r: SAMRecord) => true
      else
        (r: SAMRecord) => readGroupIDs.contains(r.getReadGroup.getReadGroupId)

    /** function to get set element */
    val SAMRecordElement =
      if (filterOutMulti)
        (r: SAMRecord) => r.getReadName
      else
        (r: SAMRecord) => r.getReadName + "_" + r.getAlignmentStart.toString

    val SAMRecordMateElement =
      (r: SAMRecord)   => r.getReadName + "_" + r.getMateAlignmentStart.toString

    val firstBAM = prepIndexedInputBAM()

    /* NOTE: the interval vector here should be bypass-able if we can make
             the BAM query intervals with Interval objects. This is not possible
             at the moment since we can not retrieve star and end coordinates
             of an Interval, so we resort to our own RawInterval vector
    */
    lazy val intervals = iv.toVector
    lazy val intervalTreeMap: Map[String, IntervalTree] = intervals
      .groupBy(x => x.chrom)
      .map({ case (key, value) => (key, makeIntervalTree(value)) })
    lazy val queryIntervals = intervals
      .flatMap(x => monadicMakeQueryInterval(firstBAM, x))
      // makeQueryInterval only accepts a sorted QueryInterval list
      .sortBy(x => (x.referenceIndex, x.start, x.end))
      .toArray

    val filteredRecords: Iterator[SAMRecord] = firstBAM.queryOverlapping(queryIntervals).asScala
      // ensure spliced reads have at least one block overlapping target region
      .filter(x => alignmentBlockOverlaps(x, intervalTreeMap))
      // filter for MAPQ on target region reads
      .filter(x => x.getMappingQuality >= minMapQ)
      // filter on specific read group IDs
      .filter(x => rgFilter(x))

    val filteredOutSet: BloomFilter[String] = new FilterBuilder(bloomSize, bloomFp)
      .hashFunction(HashMethod.Murmur3KirschMitzenmacher)
      .buildBloomFilter()

    for (rec <- filteredRecords) {
      if ((!filterOutMulti) && rec.getReadPairedFlag) {
        filteredOutSet.add(SAMRecordElement(rec))
        filteredOutSet.add(SAMRecordMateElement(rec))
      }
      else
        filteredOutSet.add(SAMRecordElement(rec))
    }

    if (filterOutMulti)
      (rec: SAMRecord) => filteredOutSet.contains(rec.getReadName)
    else
      (rec: SAMRecord) => {
        if (rec.getReadPairedFlag)
          filteredOutSet.contains(SAMRecordElement(rec)) &&
          filteredOutSet.contains(SAMRecordMateElement(rec))
        else
          filteredOutSet.contains(SAMRecordElement(rec))
      }
  }

  // TODO: implement stats file output
  /**
   * Function to filter input BAM and write its output to the filesystem
   *
   * @param filterFunc filter function that evaluates true for excluded SAMRecord
   * @param inBAM input BAM file
   * @param outBAM output BAM file
   * @param writeIndex whether to write index for output BAM file
   * @param async whether to write asynchronously
   * @param filteredOutBAM whether to write excluded SAMRecords to their own BAM file
   */
  def writeFilteredBAM(filterFunc: (SAMRecord => Boolean), inBAM: File, outBAM: File,
                       writeIndex: Boolean = true, async: Boolean = true,
                       filteredOutBAM: File = null) = {

    val factory = new SAMFileWriterFactory()
      .setCreateIndex(writeIndex)
      .setUseAsyncIo(async)
    val templateBAM = new SAMFileReader(inBAM)
    templateBAM.setValidationStringency(SAMFileReader.ValidationStringency.LENIENT)
    val targetBAM = factory.makeBAMWriter(templateBAM.getFileHeader, true, outBAM)
    val filteredBAM =
      if (filteredOutBAM != null)
        factory.makeBAMWriter(templateBAM.getFileHeader, true, filteredOutBAM)
      else
        null

    try {
      for (rec: SAMRecord <- templateBAM.asScala) {
        if (!filterFunc(rec)) targetBAM.addAlignment(rec)
        else if (filteredBAM != null) filteredBAM.addAlignment(rec)
      }
    } finally {
      templateBAM.close()
      targetBAM.close()
      if (filteredBAM != null) filteredBAM.close()
    }
  }

  case class Args (inputBAM: File = null,
                   targetRegions: File = null,
                   outputBAM: File = null,
                   readGroupIDs: Set[String] = Set.empty[String],
                   minMapQ: Int = 0,
                   limitToRegion: Boolean = false,
                   noMakeIndex: Boolean = false) extends AbstractArgs

  class OptParser extends AbstractOptParser {

    head(
      s"""
        |$commandName - Region-based reads removal from an indexed BAM file
      """.stripMargin)

    opt[File]('I', "input_file") required() valueName "<bam>" action { (x, c) =>
      c.copy(inputBAM = x) } validate {
        x => if (x.exists) success else failure("Input BAM file not found")
      } text "Input BAM file"

    opt[File]('r', "interval_file") required() valueName "<bed>" action { (x, c) =>
      c.copy(targetRegions = x) } validate {
        x => if (x.exists) success else failure("Target regions file not found")
      } text "Interval BED file"

    opt[File]('o', "output_file") required() valueName "<bam>" action { (x, c) =>
      c.copy(outputBAM = x) } text "Output BAM file"

    opt[Int]('Q', "min_mapq") optional() action { (x, c) =>
      c.copy(minMapQ = x) } text "Minimum MAPQ of reads in target region to remove (default: 0)"

    opt[String]('G', "read_group") unbounded() optional() valueName "<rgid>" action { (x, c) =>
      c.copy(readGroupIDs = c.readGroupIDs + x) } text "Read group IDs to be removed (default: remove reads from all read groups)"

    opt[Boolean]("limit_removal") optional() valueName "" action { (_, c) =>
      c.copy(limitToRegion = true) } text
      "Whether to remove multiple-mapped reads outside the target regions (default: yes)"

    opt[Boolean]("no_make_index") optional() valueName "" action { (_, c) =>
      c.copy(noMakeIndex = true) } text
      "Whether to index output BAM file or not (default: yes)"

    note(
      """
        |This tool will remove BAM records that overlaps a set of given regions.
        |By default, if the removed reads are also mapped to other regions outside
        |the given ones, they will also be removed.
      """.stripMargin)
  }

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = new OptParser()
      .parse(args, Args())
      .getOrElse(sys.exit(1))

    val filterFunc = makeFilterOutFunction(
      iv = makeRawIntervalFromFile(commandArgs.targetRegions),
      inBAM = commandArgs.inputBAM,
      filterOutMulti = !commandArgs.limitToRegion,
      minMapQ = commandArgs.minMapQ,
      readGroupIDs = commandArgs.readGroupIDs,
      bloomSize = 70000000,
      bloomFp = 4e-7
    )

    writeFilteredBAM(
      filterFunc,
      commandArgs.inputBAM,
      commandArgs.outputBAM,
      writeIndex = !commandArgs.noMakeIndex
    )
  }
}
