/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.core.apps

import java.io.{ File, IOException }
import scala.collection.JavaConverters._
import scala.io.Source

import com.twitter.algebird.{ BF, BloomFilter }
import htsjdk.samtools.AlignmentBlock
import htsjdk.samtools.SAMFileReader
import htsjdk.samtools.SAMFileReader.QueryInterval
import htsjdk.samtools.SAMFileWriterFactory
import htsjdk.samtools.SAMRecord
import htsjdk.tribble.index.interval.{ Interval, IntervalTree }
import org.apache.commons.io.FilenameUtils.getExtension
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
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

object WipeReads {

  /** Container type for command line flags */
  type OptionMap = Map[String, Any]

  /** Container class for interval parsing results */
  case class RawInterval(chrom: String, start: Int, end: Int, strand: String)

  /** Enum type for strand overlap orientation */
  object Strand extends Enumeration {
    type Strand = Value
    val Identical, Opposite, Both = Value
  }

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
          case Array(chrom, start, end)                   => new RawInterval(chrom, start.toInt + 1, end.toInt, "")
          case Array(chrom, start, end, _, _, strand, _*) => new RawInterval(chrom, start.toInt + 1, end.toInt, strand)
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

    iterFunc(inFile)
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
                            bloomSize: Int = 100000000, bloomFp: Double = 1e-10): (Any => Boolean) = {

    // TODO: implement optional index creation
    /** Function to check for BAM file index and return a SAMFileReader given a File */
    def prepIndexedInputBAM(): SAMFileReader =
      if (inBAMIndex != null)
        new SAMFileReader(inBAM, inBAMIndex)
      else {
        val sfr = new SAMFileReader(inBAM)
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

    // TODO: can we accumulate errors / exceptions instead of ignoring them?
    /**
     * Function to query mate from a BAM file
     *
     * @param inBAM BAM file to query as SAMFileReader
     * @param rec SAMRecord whose mate will be queried
     * @return SAMRecord wrapped in Option
     */
    def monadicMateQuery(inBAM: SAMFileReader, rec: SAMRecord): Option[SAMRecord] =
      // catching unpaired read here since queryMate will raise an exception if not
      if (!rec.getReadPairedFlag) {
        None
      } else {
        inBAM.queryMate(rec) match {
          case null     => None
          case qres @ _ => Some(qres)
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
    def alignmentBlockOverlaps(rec: SAMRecord, ivtm: Map[String, IntervalTree]): Boolean =
      // if SAMRecord is not spliced, assume queryOverlap has done its job
      // otherwise check for alignment block overlaps in our interval list
      // using raw SAMString to bypass cigar string decoding
      if (rec.getSAMString.split("\t")(5).contains("N")) {
        for (ab: AlignmentBlock <- rec.getAlignmentBlocks.asScala) {
          if (!ivtm(rec.getReferenceName).findOverlapping(
            new Interval(ab.getReferenceStart, ab.getReferenceStart + ab.getLength - 1, null)).isEmpty)
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
    val SAMToElem =
      if (filterOutMulti)
        (r: SAMRecord) => r.getReadName
      else
        (r: SAMRecord) => r.getSAMString

    val firstBAM = prepIndexedInputBAM()
    val secondBAM = prepIndexedInputBAM()
    val bfm = BloomFilter(bloomSize, bloomFp, 13)

    val intervals = iv.toList
    val queryIntervals = intervals.flatMap(x => monadicMakeQueryInterval(firstBAM, x)).toArray

    /** interval tree for ensuring that split reads do overlap our target regions */
    val ivtm = scala.collection.mutable.Map.empty[String, IntervalTree]
    for (iv: RawInterval <- intervals) {
      ivtm.getOrElseUpdate(iv.chrom, new IntervalTree).insert(new Interval(iv.start, iv.end))
    }

    val filteredOutSet: BF = firstBAM.queryOverlapping(queryIntervals).asScala
      // ensure spliced reads have at least one block overlapping target region
      .filter(x => alignmentBlockOverlaps(x, ivtm.toMap))
      // filter for MAPQ on target region reads
      .filter(x => x.getMappingQuality >= minMapQ)
      // filter on specific read group IDs
      .filter(x => rgFilter(x))
      // TODO: how to directly get SAMRecord and its pairs without multiple flattens?
      .flatMap(x => Vector(Some(x), monadicMateQuery(secondBAM, x)).flatten)
      // transfrom SAMRecord to string
      .map(x => SAMToElem(x))
      // build bloom filter using fold to prevent loading all strings to memory
      .foldLeft(bfm.create())(_.+(_))

    if (filterOutMulti)
      (rec: Any) => rec match {
        case rec: SAMRecord => filteredOutSet.contains(rec.getReadName).isTrue
        case rec: String    => filteredOutSet.contains(rec).isTrue
        case _              => false
      }
    else
      (rec: Any) => rec match {
        case rec: SAMRecord => filteredOutSet.contains(rec.getSAMString).isTrue
        case rec: String    => filteredOutSet.contains(rec).isTrue
        case _              => false
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

  /**
   * Recursive function to parse command line options
   *
   * @param opts OptionMap instance which may contain parsed options
   * @param list remaining command line arguments
   * @return OptionMap instance
   */
  def parseOption(opts: OptionMap, list: List[String]): OptionMap =
    // format: OFF
    list match {
      case Nil
          => opts
      case ("--inputBAM" | "-I") :: value :: tail if !opts.contains("inputBAM")
          => parseOption(opts ++ Map("inputBAM" -> checkInputBAM(new File(value))), tail)
      case ("--targetRegions" | "-l") :: value :: tail if !opts.contains("targetRegions")
          => parseOption(opts ++ Map("targetRegions" -> checkInputFile(new File(value))), tail)
      case ("--outputBAM" | "-o") :: value :: tail if !opts.contains("outputBAM")
          => parseOption(opts ++ Map("outputBAM" -> new File(value)), tail)
      case ("--minMapQ" | "-Q") :: value :: tail if !opts.contains("minMapQ")
          => parseOption(opts ++ Map("minMapQ" -> value.toInt), tail)
      // TODO: better way to parse multiple flag values?
      case ("--readGroup" | "-RG") :: value :: tail if !opts.contains("readGroup")
      => parseOption(opts ++ Map("readGroup" -> value.split(",").toSet), tail)
      case ("--noMakeIndex") :: tail
          => parseOption(opts ++ Map("noMakeIndex" -> true), tail)
      case ("--limitToRegion" | "-limit") :: tail
          => parseOption(opts ++ Map("limitToRegion" -> true), tail)
      // TODO: implementation
      case ("--minOverlapFraction" | "-f") :: value :: tail if !opts.contains("minOverlapFraction")
      => parseOption(opts ++ Map("minOverlapFraction" -> value.toDouble), tail)
      // TODO: implementation
      case ("--strand" | "-s") :: (value @ ("identical" | "opposite" | "both")) :: tail if !opts.contains("strand")
      => parseOption(opts ++ Map("strand" -> Strand.withName(value.capitalize)), tail)
      case option :: tail
          => throw new IllegalArgumentException("Unexpected or duplicate option flag: " + option)
    }
  // format: ON

  /** Function to validate OptionMap instances */
  private def validateOption(opts: OptionMap): Unit = {
    // TODO: better way to check for required arguments ~ use scalaz.Validation?
    if (opts.get("inputBAM") == None)
      throw new IllegalArgumentException("Input BAM not supplied")
    if (opts.get("targetRegions") == None)
      throw new IllegalArgumentException("Target regions file not supplied")
  }

  /** Function that returns the given File if it exists */
  def checkInputFile(inFile: File): File =
    if (inFile.exists)
      inFile
    else
      throw new IOException("Input file " + inFile.getPath + " not found")

  /** Function that returns the given BAM file if it exists and is indexed */
  def checkInputBAM(inBAM: File): File = {
    // input BAM must have a .bam.bai index
    if (new File(inBAM.getPath + ".bai").exists || new File(inBAM.getPath + ".bam.bai").exists)
      checkInputFile(inBAM)
    else
      throw new IOException("Index for input BAM file " + inBAM.getPath + " not found")
  }

  def main(args: Array[String]): Unit = {

    if (args.length == 0) {
      println(usage)
      System.exit(1)
    }
    val options = parseOption(Map(), args.toList)
    validateOption(options)

    val inputBAM = options("inputBAM").asInstanceOf[File]
    val outputBAM = options("outputBAM").asInstanceOf[File]

    val iv = makeRawIntervalFromFile(options("targetRegions").asInstanceOf[File])
    // limiting bloomSize to 70M and bloomFp to 4e-7 due to Int size limitation set in algebird
    val filterFunc = makeFilterOutFunction(iv = iv,
      inBAM = inputBAM,
      filterOutMulti = !options.getOrElse("limitToRegion", false).asInstanceOf[Boolean],
      minMapQ = options.getOrElse("minMapQ", 0).asInstanceOf[Int],
      readGroupIDs = options.getOrElse("readGroupIDs", Set()).asInstanceOf[Set[String]],
      bloomSize = 70000000, bloomFp = 4e-7)

    writeFilteredBAM(filterFunc, inputBAM, outputBAM,
      writeIndex = !options.getOrElse("noMakeIndex", false).asInstanceOf[Boolean])
  }

  val usage: String =
    """
      |usage: java -cp BiopetFramework.jar nl.lumc.sasc.biopet.core.apps.WipeReads [options] -I input -l regions -o output
      |
      |WipeReads - Tool for reads removal from an indexed BAM file
      |
      |positional arguments:
      |  -I,--inputBAM              Input BAM file, must be indexed with '.bam.bai' or 'bai' extension
      |  -l,--targetRegions         Input BED file
      |  -o,--outputBAM             Output BAM file
      |
      |optional arguments:
      |  -RG,--readGroup            Read groups to remove; set multiple read groups using commas
      |                             (default: all)
      |  -Q,--minMapQ               Minimum MAPQ value of reads in target region
      |                             (default: 0)
      |  --makeIndex                Write BAM output file index
      |                             (default: true)
      |  --limitToRegion            Whether to remove only reads in the target regions and and
      |                             keep the same reads if they map to other regions
      |                             (default: not set)
      |
      |This tool will remove BAM records that overlaps a set of given regions.
      |By default, if the removed reads are also mapped to other regions outside
      |the given ones, they will also be removed.
    """.stripMargin
}