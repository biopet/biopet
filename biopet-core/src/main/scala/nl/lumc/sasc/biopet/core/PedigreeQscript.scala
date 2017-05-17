package nl.lumc.sasc.biopet.core

import java.io.PrintWriter

import nl.lumc.sasc.biopet.core.PedigreeQscript.PedMergeStrategy
import nl.lumc.sasc.biopet.utils.Logging
import org.broadinstitute.gatk.queue.QScript

import scala.io.Source

/**
  * Created by Sander Bollen on 28-3-17.
  *
  * A multi-sample Qscript with additional Pedigree information.
  * Pedigrees follow the PED standard.
  * See: http://zzz.bwh.harvard.edu/plink/data.shtml#ped
  *
  * Pedigrees may be parsed from the sample config and/or a supplied PED file.
  */
trait PedigreeQscript extends MultiSampleQScript { qscript: QScript =>

  /* Optionally parse from ped file */
  def ped: Option[File] = config("ped_file", default = None)

  /* The merge stategy to use when we have both a ped file and sample tag information */
  def mergeStrategy: PedMergeStrategy.Value = PedMergeStrategy.Concatenate

  /**
    * Case class representing a PED samples
    * For the PED format, see:
    * http://zzz.bwh.harvard.edu/plink/data.shtml#ped
    * @param familyId family id
    * @param individualId individual id
    * @param paternalId Optional paternal id
    * @param maternalId Optional maternal id
    * @param gender gender
    * @param affectedPhenotype Optional boolean
    * @param genotypeFields optional genotype fields
    */
  case class PedSample(familyId: String,
                       individualId: String,
                       paternalId: Option[String],
                       maternalId: Option[String],
                       gender: MultiSampleQScript.Gender.Value,
                       affectedPhenotype: Option[Boolean],
                       genotypeFields: List[String] = Nil)

  lazy val pedSamples: List[PedSample] = {
    mergeStrategy match {
      case PedMergeStrategy.Concatenate => pedSamplesFromConfig() ::: parsePedFile()
      case _ => throw new NotImplementedError() // todo
    }
  }

  /**
    * Get affected samples
    *
    * @return List[PedSample]
    */
  def getIndexSamples: List[PedSample] = {
    pedSamples.filter(x => x.affectedPhenotype.contains(true))
  }

  /**
    * Get pedSamples from sample tags in config
    * May return empty list if no pedigree can be constructed
    * PedSamples can only be constructed for those samples where family is defined
    * Furthermore, if a father or mother is given, another sample with this id must also exist
    *
    * @return
    */
  def pedSamplesFromConfig(): List[PedSample] = {
    val withFam = samples.values.filter(_.family.isDefined)
    val sampleIds = withFam.map(_.sampleId).toSet
    val fathers = withFam.flatMap(_.father)
    val mothers = withFam.flatMap(_.mother)
    fathers.foreach { f =>
      if (!sampleIds.contains(f)) {
        Logging.addError(s"Father $f does not exist in samples")
      }
    }
    mothers.foreach { m =>
      if (!sampleIds.contains(m)) {
        Logging.addError(s"Mother $m does not exist in samples")
      }
    }
    withFam.map { s =>
      PedSample(s.family.get, s.sampleId, s.father, s.mother, s.gender, None)
    }.toList
  }

  /* Parse ped file to list of PedSamples */
  def parsePedFile(): List[PedSample] = {
    ped match {
      case Some(p) =>
        Source
          .fromFile(p)
          .getLines()
          .map { x =>
            parseSinglePedLine(x)
          }
          .toList
      case _ => Nil
    }
  }

  /* Parse a single Ped line to a PedSample */
  def parseSinglePedLine(line: String): PedSample = {
    val arr = line.split("\\s")
    var genotypeFields: List[String] = Nil
    if (arr.size < 6) {
      throw new IllegalArgumentException("Ped file contains less than 6 columns")
    } else if (arr.length == 6) {
      genotypeFields = Nil
    } else {
      genotypeFields = arr.drop(6).toList
    }
    val gender = arr(4) match {
      case "1" => MultiSampleQScript.Gender.Male
      case "2" => MultiSampleQScript.Gender.Female
      case _ => MultiSampleQScript.Gender.Unknown
    }
    val affected = arr(5) match {
      case "1" => Some(false)
      case "2" => Some(true)
      case _ => None
    }
    val paternalId = arr(2) match {
      case "0" => None
      case otherwise => Some(otherwise)
    }
    val maternalId = arr(3) match {
      case "0" => None
      case otherwise => Some(otherwise)
    }
    PedSample(arr(0), arr(1), paternalId, maternalId, gender, affected, genotypeFields)
  }

  /* Check whether sample is a mother */
  def isMother(pedSample: PedSample): Boolean = {
    val motherIds = pedSamples.flatMap(_.maternalId)
    motherIds.contains(pedSample.individualId)
  }

  /* Check whether sample is a father */
  def isFather(pedSample: PedSample): Boolean = {
    val fatherIds = pedSamples.flatMap(_.paternalId)
    fatherIds.contains(pedSample.individualId)
  }

  /**
    * Convenience method for checking whether current pedigree constitutes a single patient pedigree
    */
  def isSingle: Boolean = {
    pedSamples.size == 1 && pedSamples.head.maternalId.isEmpty && pedSamples.head.paternalId.isEmpty
  }

  /**
    * Convenience method for checking whether current pedigree constitutes a trio pedigree
    */
  def isTrio: Boolean = {
    getIndexSamples.size == 1 &&
    pedSamples.size == 3 &&
    (getIndexSamples.head.maternalId.isDefined && pedSamples
      .map(_.individualId)
      .contains(getIndexSamples.head.maternalId.get)) &&
    (getIndexSamples.head.paternalId.isDefined && pedSamples
      .map(_.individualId)
      .contains(getIndexSamples.head.paternalId.get))
  }

  /**
    * Write list of ped samples to a PED file
    *
    * For the PED format see:
    * http://pngu.mgh.harvard.edu/~purcell/plink/data.shtml
    */
  def writeToPedFile(outFile: File): Unit = {
    val writer = new PrintWriter(outFile)
    pedSamples.foreach { p =>
      val paternalField = p.paternalId match {
        case Some(s) => s
        case _ => "0"
      }
      val maternalField = p.maternalId match {
        case Some(s) => s
        case _ => "0"
      }
      val genderField = p.gender match {
        case MultiSampleQScript.Gender.Male => "1"
        case MultiSampleQScript.Gender.Female => "2"
        case _ => "-9"
      }
      val affectedField = p.affectedPhenotype match {
        case Some(b) =>
          b match {
            case true => "2"
            case _ => "1"
          }
        case _ => "0"
      }
      val mainLine: String =
        s"${p.familyId}\t${p.individualId}\t$paternalField\t$maternalField\t$genderField\t$affectedField"
      val line = if (p.genotypeFields.nonEmpty) {
        mainLine + "\t" + p.genotypeFields.mkString("\t")
      } else mainLine

      writer.write(line + "\n")
    }
    writer.close()
  }

}

object PedigreeQscript {
  object PedMergeStrategy extends Enumeration {
    val Concatenate, Update, ConcatenatedAndUpdate = Value
  }
}
