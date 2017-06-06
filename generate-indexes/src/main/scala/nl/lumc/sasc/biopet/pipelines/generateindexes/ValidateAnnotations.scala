package nl.lumc.sasc.biopet.pipelines.generateindexes

import java.io.File

import nl.lumc.sasc.biopet.core.extensions.CheckValidateVcf
import nl.lumc.sasc.biopet.core.{BiopetQScript, PipelineCommand}
import nl.lumc.sasc.biopet.extensions.tools.ValidateVcf
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by pjvan_thof on 6-6-17.
  */
class ValidateAnnotations(val parent: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Argument(required = true)
  var speciesDir: File = _

  override def defaults = Map("abort_on_error" -> false)

  /** Init for pipeline */
  def init(): Unit = {
    require(speciesDir.exists(), s"speciesDir does not exist: ${speciesDir.getAbsolutePath}")
    require(speciesDir.isDirectory, "speciesDir is not a directory")
  }

  /** Pipeline itself */
  def biopetScript(): Unit = {
    val genomes: Map[String, List[String]] = speciesDir
      .list()
      .filter(!_.startsWith("."))
      .map { species =>
        species -> new File(speciesDir, species).list().filter(!_.startsWith(".")).toList
      }
      .toMap

    for ((species, g) <- genomes; genome <- g) validateGenome(species, genome)
  }

  protected def validateGenome(species: String, genomeName: String): Unit = {
    val genomeDir = new File(speciesDir, species + File.separator + genomeName)
    val referenceFile = new File(genomeDir, "reference.fa")
    require(referenceFile.exists(), s"Reference file does not exist: $referenceFile")
    val annotationDir = new File(genomeDir, "annotation")

    val dbsnpDir = new File(annotationDir, "dbsnp")
    if (dbsnpDir.exists()) {
      val vcfFiles = dbsnpDir.list().filter(_.endsWith(".vcf.gz"))
      if (vcfFiles.isEmpty) logger.warn(s"Genome '$species-$genomeName is missing dbsnp files'")
      vcfFiles.foreach { vcfFile =>
        val vv = new ValidateVcf(this)
        vv.reference = referenceFile
        vv.inputVcf = new File(dbsnpDir, vcfFile)
        vv.disableFail = true
        vv.jobOutputFile =
          new File(outputDir, s"$species-$genomeName.${vcfFile.getName}.ValidateVcf.out")
        add(vv)

        val cvv = new CheckValidateVcf(this)
        cvv.inputLogFile = vv.jobOutputFile
        cvv.species = species
        cvv.genomeName = genomeName
        cvv.jobOutputFile =
          new File(outputDir, s"$species-$genomeName.${vcfFile.getName}.CheckValidateVcf.out")
        add(cvv)
      }
    } else logger.warn(s"Genome '$species-$genomeName is missing dbsnp files'")

    val featuresDir = new File(annotationDir, "features")

    if (featuresDir.exists()) {
      for (source <- featuresDir.list().filter(!_.startsWith("."))) {
        val sourceDir = new File(featuresDir, source)
        val prefixes = sourceDir
          .list()
          .filter(x => x.endsWith(".gtf") || x.endsWith(".gff3") || x.endsWith(".refflat"))
          .map(_.stripSuffix(".gtf"))
          .map(_.stripSuffix(".gff3"))
          .map(_.stripSuffix(".refflat"))
          .distinct
        if (prefixes.isEmpty)
          logger.warn(s"No features annotations found for $species-$genomeName")
        logger.info(s"For $source, found prefixes: ${prefixes.mkString(",")}")
        for (prefix <- prefixes) {
          val gtfFile = new File(sourceDir, prefix + ".gtf")
          val gff3File = new File(sourceDir, prefix + ".gff3")
          val refflatFile = new File(sourceDir, prefix + ".refflat")
          val validateGtf = new nl.lumc.sasc.biopet.extensions.tools.ValidateAnnotation(this)
          validateGtf.gtfFile = List(gtfFile)
          validateGtf.refflatFile = Some(refflatFile)
          validateGtf.reference = referenceFile
          validateGtf.disableFail = true
          validateGtf.jobOutputFile =
            new File(outputDir, s"$species-$genomeName.$source.$prefix.gtf.ValidateAnnotation.out")
          add(validateGtf)

          val ca = new nl.lumc.sasc.biopet.extensions.tools.CheckValidateAnnotation(this)
          ca.inputLogFile = validateGtf.jobOutputFile
          ca.species = species
          ca.genomeName = genomeName
          ca.jobOutputFile = new File(
            outputDir,
            s"$species-$genomeName.$source.$prefix.gtf.CheckValidateAnnotation.out")
          add(ca)

          if (gff3File.exists()) {
            val validateGff3 = new nl.lumc.sasc.biopet.extensions.tools.ValidateAnnotation(this)
            validateGff3.gtfFile = List(gff3File)
            validateGff3.reference = referenceFile
            validateGff3.disableFail = true
            validateGff3.jobOutputFile = new File(
              outputDir,
              s"$species-$genomeName.$source.$prefix.gff3.ValidateAnnotation.out")
            add(validateGff3)

            val ca = new nl.lumc.sasc.biopet.extensions.tools.CheckValidateAnnotation(this)
            ca.inputLogFile = validateGff3.jobOutputFile
            ca.species = species
            ca.genomeName = genomeName
            ca.jobOutputFile = new File(
              outputDir,
              s"$species-$genomeName.$source.$prefix.gff3.CheckValidateAnnotation.out")
            add(ca)
          }
        }
      }
    } else logger.warn(s"No features annotations found for $species-$genomeName")
  }
}

object ValidateAnnotations extends PipelineCommand
