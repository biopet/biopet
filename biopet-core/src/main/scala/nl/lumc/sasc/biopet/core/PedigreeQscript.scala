package nl.lumc.sasc.biopet.core

import java.io.PrintWriter

import nl.lumc.sasc.biopet.core.PedigreeQscript.PedMergeStrategy
import org.broadinstitute.gatk.queue.QScript

import scala.io.Source

/**
 * Created by Sander Bollen on 28-3-17.
 */
trait PedigreeQscript extends MultiSampleQScript { qscript: QScript =>

  /* Optionally parse from ped file */
  def ped: Option[File] = None

  /* The merge stategy to use when we have both a ped file and sample tag information */
  def mergeStrategy: PedMergeStrategy.Value = PedMergeStrategy.Concatenate

  /**
   * Case class representing a PED samples
   * For the PED format, see:
   * http://pngu.mgh.harvard.edu/~purcell/plink/data.shtml
   * @param familyId family id
   * @param individualId individual id
   * @param paternalId Optional paternal id
   * @param maternalId Optional maternal id
   * @param gender gender
   * @param affectedPhenotype Optional boolean
   * @param genotypeFields optional genotype fileds
   */
  case class PedSample(familyId: String, individualId: String,
                       paternalId: Option[String],
                       maternalId: Option[String],
                       gender: MultiSampleQScript.Gender.Value,
                       affectedPhenotype: Option[Boolean],
                       genotypeFields: List[String] = Nil)

  lazy val pedSamples: List[PedSample] = {
    mergeStrategy match {
      case PedMergeStrategy.Concatenate => pedSamplesFromConfig() ::: parsePedFile()
      case _                            => throw new NotImplementedError() // todo
    }
  }

  /**
   * Get affected samples
   *
   * @return List[PedSample]
   */
  def getIndexSamples: List[PedSample] = {
    pedSamples.filter(x => x.affectedPhenotype)
  }

  /**
   * Get pedSamples from sample tags in config
   * May return empty list if no pedigree can be constructed
   *
   * @return
   */
  def pedSamplesFromConfig(): List[PedSample] = {
    val totalSampleIds = samples.values.map(_.sampleId).toList
    samples.values.filter(x => x.father.isDefined && x.mother.isDefined && x.family.isDefined).map { x =>
      (totalSampleIds.contains(x.mother.get), totalSampleIds.contains(x.father.get)) match {
        case (true, true)  => PedSample(x.family.get, x.sampleId, x.father, x.mother, x.gender, None)
        case (true, false) => PedSample(x.family.get, x.sampleId, None, x.mother, x.gender, None)
        case (false, true) => PedSample(x.family.get, x.sampleId, x.father, None, x.gender, None)
        case _             => PedSample(x.family.get, x.sampleId, None, None, x.gender, None)
      }
    }.toList
  }

  def parsePedFile(): List[PedSample] = {
    ped match {
      case Some(p) => Source.fromFile(p).getLines().map { x => parseSinglePedLine(x) }.toList
      case _       => Nil
    }
  }

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
      case _   => MultiSampleQScript.Gender.Unknown
    }
    val affected = arr(5) match {
      case "1" => Some(false)
      case "2" => Some(true)
      case _   => None
    }
    val paternalId = arr(2) match {
      case "0"       => None
      case otherwise => Some(otherwise)
    }
    val maternalId = arr(3) match {
      case "0"       => None
      case otherwise => Some(otherwise)
    }
    PedSample(arr(0), arr(1), paternalId, maternalId, gender, affected, genotypeFields)
  }

  def isMother(pedSample: PedSample): Boolean = {
    val motherIds = pedSamples.flatMap(_.maternalId)
    motherIds.contains(pedSample.individualId)
  }

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
      (getIndexSamples.head.maternalId.isDefined && pedSamples.map(_.individualId).contains(getIndexSamples.head.maternalId.get)) &&
      (getIndexSamples.head.paternalId.isDefined && pedSamples.map(_.individualId).contains(getIndexSamples.head.paternalId.get))
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
        case _       => "0"
      }
      val maternalField = p.maternalId match {
        case Some(s) => s
        case _       => "0"
      }
      val genderField = p.gender match {
        case MultiSampleQScript.Gender.Male   => "1"
        case MultiSampleQScript.Gender.Female => "2"
        case _                                => "-9"
      }
      val affectedField = p.affectedPhenotype match {
        case Some(b) => b match {
          case true => "2"
          case _    => "1"
        }
        case _ => "0"
      }
      val line: String = s"${p.familyId}\t${p.individualId}\t$paternalField\t$maternalField\t$genderField\t$affectedField\t" +
        p.genotypeFields.mkString("\t")
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
