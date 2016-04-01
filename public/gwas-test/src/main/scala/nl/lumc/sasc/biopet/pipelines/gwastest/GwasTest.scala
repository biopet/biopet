package nl.lumc.sasc.biopet.pipelines.gwastest

import java.io.File
import java.util

import htsjdk.samtools.reference.FastaSequenceFile
import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand, Reference }
import nl.lumc.sasc.biopet.extensions.Snptest
import nl.lumc.sasc.biopet.extensions.gatk.{ CatVariants, SelectVariants }
import nl.lumc.sasc.biopet.extensions.tools.GensToVcf
import nl.lumc.sasc.biopet.pipelines.gwastest.impute.ImputeOutput
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList
import org.broadinstitute.gatk.queue.QScript

import scala.collection.JavaConversions._

/**
 * Created by pjvanthof on 16/03/16.
 */
class GwasTest(val root: Configurable) extends QScript with BiopetQScript with Reference {
  def this() = this(null)

  import GwasTest._

  val inputVcf: Option[File] = config("input_vcf")

  val phenotypeFile: File = config("phenotype_file")

  val specsFile: Option[File] = config("imute_specs_file")

  val inputGens: List[GensInput] = if (inputVcf.isDefined) List[GensInput]()
  else config("input_gens", default = Nil).asList.map(x => x match {
    case value: Map[String, Any] =>
      GensInput(new File(value("genotypes").toString),
        value.get("info").map(x => new File(x.toString)),
        value("contig").toString)
    case value: util.LinkedHashMap[String, _] =>
      GensInput(new File(value.get("genotypes").toString),
        value.toMap.get("info").map(x => new File(x.toString)),
        value.get("contig").toString)
    case _ => throw new IllegalArgumentException
  }) ++ (specsFile match {
    case Some(file) => imputeSpecsToGensInput(file, config("validate_specs", default = true))
    case _          => Nil
  })

  lazy val referenceDict = new FastaSequenceFile(referenceFasta(), true).getSequenceDictionary

  override def dictRequired = true

  /** Init for pipeline */
  def init(): Unit = {
    inputGens.foreach { g =>
      if (referenceDict.getSequenceIndex(g.contig) == -1)
        Logging.addError(s"Contig '${g.contig}' does not exist on reference: ${referenceFasta()}")
    }
  }

  /** Pipeline itself */
  def biopetScript(): Unit = {
    val (vcfFile, chrVcfFiles): (File, Map[String, File]) = inputVcf.map((_, Map[String, File]())).getOrElse {
      require(inputGens.nonEmpty, "No vcf file or gens files defined in config")
      val outputDirGens = new File(outputDir, "gens_to_vcf")
      val cvTotal = new CatVariants(this)
      cvTotal.assumeSorted = true
      cvTotal.outputFile = new File(outputDirGens, "merge.gens.vcf.gz")
      val chrGens = inputGens.groupBy(_.contig).map {
        case (contig, gens) =>
          val cvChr = new CatVariants(this)
          cvChr.assumeSorted = true
          //cvChr.isIntermediate = true
          cvChr.outputFile = new File(outputDirGens, s"${contig}.merge.gens.vcf.gz")
          gens.zipWithIndex.foreach { gen =>
            val gensToVcf = new GensToVcf(this)
            gensToVcf.inputGens = gen._1.genotypes
            gensToVcf.inputInfo = gen._1.info
            gensToVcf.contig = gen._1.contig
            gensToVcf.samplesFile = phenotypeFile
            gensToVcf.outputVcf = new File(outputDirGens, gen._1.genotypes.getName + s".${gen._2}.vcf.gz")
            gensToVcf.isIntermediate = true
            add(gensToVcf)
            cvChr.inputFiles :+= gensToVcf.outputVcf
          }
          add(cvChr)
          cvTotal.inputFiles :+= cvChr.outputFile
          contig -> cvChr.outputFile
      }
      add(cvTotal)
      (cvTotal.outputFile, chrGens)
    }

    val snpTests = BedRecordList.fromReference(referenceFasta())
      .scatter(config("bin_size", default = 1000000))
      .allRecords.map { region =>
        val regionDir = new File(outputDir, "snptest" + File.separator + region.chr)
        regionDir.mkdirs()
        val bedFile = new File(regionDir, s"${region.chr}-${region.start + 1}-${region.end}.bed")
        BedRecordList.fromList(List(region)).writeToFile(bedFile)
        bedFile.deleteOnExit()

        val sv = new SelectVariants(this)
        sv.inputFiles :+= chrVcfFiles.getOrElse(region.chr, vcfFile)
        sv.outputFile = new File(regionDir, s"${region.chr}-${region.start + 1}-${region.end}.vcf.gz")
        sv.intervals :+= bedFile
        sv.isIntermediate = true
        add(sv)

        //TODO: snptest

        val snptest = new Snptest(this)
        snptest.inputGenotypes :+= sv.outputFile
        snptest.inputSampleFiles :+= phenotypeFile
        snptest.outputFile = Some(new File(regionDir, s"${region.chr}-${region.start + 1}-${region.end}.snptest"))
        add(snptest)
        region -> snptest
      }
  }
}

object GwasTest extends PipelineCommand {
  case class GensInput(genotypes: File, info: Option[File], contig: String)

  def imputeSpecsToGensInput(specsFile: File, validate: Boolean = true): List[GensInput] = {
    ImputeOutput.readSpecsFile(specsFile, validate)
      .map(x => GensInput(x.gens, Some(x.gensInfo), x.chromosome))
  }
}