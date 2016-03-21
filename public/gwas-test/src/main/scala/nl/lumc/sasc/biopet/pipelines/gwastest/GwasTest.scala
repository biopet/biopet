package nl.lumc.sasc.biopet.pipelines.gwastest

import java.io.File

import nl.lumc.sasc.biopet.core.{ PipelineCommand, Reference, BiopetQScript }
import nl.lumc.sasc.biopet.extensions.gatk.{ SelectVariants, CombineVariants }
import nl.lumc.sasc.biopet.extensions.tools.GensToVcf
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvanthof on 16/03/16.
 */
class GwasTest(val root: Configurable) extends QScript with BiopetQScript with Reference {
  def this() = this(null)

  lazy val inputVcf: Option[File] = config("input_vcf")

  lazy val phenotypeFile: File = config("phenotype_file")

  case class GensInput(genotypes: File, info: Option[File], contig: String)
  lazy val inputGens: Option[List[GensInput]] = if (inputVcf.isDefined) None
  else {
    if (config.contains("input_gens")) {
      val gens: List[Any] = configValue2list(config("input_gens"))
      Some(gens.map {
        case value: Map[String, Any] =>
          GensInput(new File(value("genotypes").toString),
            value.get("info").map(x => new File(x.toString)),
            value("contig").toString)
      })
    } else None
  }

  /** Init for pipeline */
  def init(): Unit = {
  }

  /** Pipeline itself */
  def biopetScript(): Unit = {
    val vcfFile: File = inputVcf.getOrElse {
      val gens = inputGens.getOrElse(throw new IllegalArgumentException("No vcf file or gens files defined in config"))
      val outputDirGens = new File(outputDir, "gens_to_vcf")
      val cv = new CombineVariants(this)
      cv.outputFile = new File(outputDirGens, "merge.gens.vcf.gz")
      cv.setKey = "null"
      gens.foreach { gen =>
        val gensToVcf = new GensToVcf(this)
        gensToVcf.inputGens = gen.genotypes
        gensToVcf.inputInfo = gen.info
        gensToVcf.contig = gen.contig
        gensToVcf.samplesFile = phenotypeFile
        gensToVcf.outputVcf = new File(outputDirGens, gen.genotypes.getName + ".vcf.gz")
        gensToVcf.isIntermediate = true
        add(gensToVcf)
        cv.inputFiles :+= gensToVcf.outputVcf
      }
      add(cv)
      cv.outputFile
    }

    val snpTests = BedRecordList.fromReference(referenceFasta())
      .scatter(config("bin_size", default = 10 ^ 6))
      .allRecords.map { region =>
        val regionDir = new File(outputDir, "snptest" + File.separator + region.chr)
        regionDir.mkdirs()
        val bedFile = new File(regionDir, s"${region.chr}-${region.start + 1}-${region.end}.bed")
        BedRecordList.fromList(List(region)).writeToFile(bedFile)
        bedFile.deleteOnExit()

        val sv = new SelectVariants(this)
        sv.inputFiles :+= vcfFile
        sv.outputFile = new File(regionDir, s"${region.chr}-${region.start + 1}-${region.end}.vcf.gz")
        sv.intervals :+= bedFile
        sv.isIntermediate = true
        add(sv)

        //TODO: snptest
        (region -> "")
      }
  }
}

object GwasTest extends PipelineCommand