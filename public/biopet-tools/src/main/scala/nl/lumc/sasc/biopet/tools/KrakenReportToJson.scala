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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.tools

/**
 * Created by wyleung on 25-9-15.
 */

import java.io.{ PrintWriter, File }

import nl.lumc.sasc.biopet.utils.ConfigUtils._
import nl.lumc.sasc.biopet.utils.ToolCommand
import scala.collection.{ immutable, mutable }

import scala.io.Source

case class KrakenHit(taxonomyID: Long,
                     taxonomyName: String,
                     cladeCount: Long,
                     cladeSize: Long, // size of parent - including itself
                     taxonRank: String,
                     cladeLevel: Int,
                     parentTaxonomyID: Long,
                     children: List[KrakenHit]) {
  def toJSON(): Map[String, Any] = {
    val childJSON = children.map(entry => entry.toJSON())
    Map(
      "name" -> taxonomyName,
      "taxid" -> taxonomyID,
      "taxonrank" -> taxonRank,
      "cladelevel" -> cladeLevel,
      "count" -> cladeCount,
      "size" -> cladeSize,
      "children" -> childJSON
    )
  }

}

object KrakenReportToJson extends ToolCommand {

  var cladeIDs: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer.fill(32)(0)
  val spacePattern = "^( +)".r

  case class Args(krakenreport: File = null, outputJson: Option[File] = None) extends AbstractArgs

  class OptParser extends AbstractOptParser {

    head(
      s"""
         |$commandName - Convert Kraken-report (full) output to JSON
      """.stripMargin)

    opt[File]('i', "krakenreport") required () unbounded () valueName "<krakenreport>" action { (x, c) =>
      c.copy(krakenreport = x)
    } validate {
      x => if (x.exists) success else failure("Krakenreport not found")
    } text "Kraken report to generate stats from"
    opt[File]('o', "output") unbounded () valueName "<json>" action { (x, c) =>
      c.copy(outputJson = Some(x))
    } text "File to write output to, if not supplied output go to stdout"
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

  def mergeBranch(branchA: Map[Long, KrakenHit],
                  branchB: KrakenHit): KrakenHit = {

    var brA = branchA.head._2
    var children = branchB.children
    var cladeCount = branchB.cladeCount
    var cladeSize = branchB.cladeSize

    /* special case for the root node */
    if (brA.taxonomyID == branchB.taxonomyID) {
      cladeCount = brA.cladeCount
      cladeSize = brA.cladeSize

    }

    /* determine to scan in branchB or return Map containing a because we cannot merge? */
    if (brA.cladeLevel > branchB.cladeLevel) {
      /* if brA's cladelevel is deeper than branchB, work on the children if any when it doesn't match it as parent */

      if (brA.parentTaxonomyID == branchB.taxonomyID) {
        children :+= brA
      } else {
        /* extend in its children */
        // TODO: do preliminary escape, don't check deeper in the tree when we have a hit.
        children = children.map(child => {
          mergeBranch(branchA, child)
        })
      }
    } else {
      /*  Hits are on the same level(have siblings, adding to parent) */
    }

    new KrakenHit(
      taxonomyID = branchB.taxonomyID,
      taxonomyName = branchB.taxonomyName,
      cladeCount = cladeCount,
      cladeSize = cladeSize,
      taxonRank = branchB.taxonRank,
      cladeLevel = branchB.cladeLevel,
      parentTaxonomyID = branchB.parentTaxonomyID,
      children = children
    )
  }

  def reportToJson(reportRaw: File): String = {
    val reader = Source.fromFile(reportRaw)
    val lines = reader.getLines().toList.filter(!_.isEmpty)

    /*
    * http://ccb.jhu.edu/software/kraken/MANUAL.html
    * The header layout is:
    * 1. Percentage of reads covered by the clade rooted at this taxon
    * 2. Number of reads covered by the clade rooted at this taxon
    * 3. Number of reads assigned directly to this taxon
    * 4. A rank code, indicating (U)nclassified, (D)omain, (K)ingdom, (P)hylum, (C)lass, (O)rder, (F)amily, (G)enus, or (S)pecies. All other ranks are simply '-'.
    * 5. NCBI taxonomy ID
    * 6. indented scientific name
    * */

    /*
    * Entries will be formatted to:
    * entries[ <taxid> ] = Map( <taxid>, Map(...))
    * */
    val entries: List[Map[Long, KrakenHit]] = for (tsvLine <- lines.tail) yield {
      val values = tsvLine.split("\t")
      val scientificName: String = values(5)
      val cladeLevel = spacePattern.findFirstIn(scientificName).getOrElse("").length / 2

      if (cladeIDs.length <= cladeLevel + 1) {
        cladeIDs ++= mutable.ArrayBuffer.fill(10)(0L)
      }

      cladeIDs(cladeLevel + 1) = values(4).toLong
      Map(
        values(4).toLong -> new KrakenHit(
          taxonomyID = values(4).toLong,
          taxonomyName = scientificName.trim,
          cladeCount = values(2).toLong,
          cladeSize = values(1).toLong,
          taxonRank = values(3),
          cladeLevel = cladeLevel,
          parentTaxonomyID = cladeIDs(cladeLevel),
          children = List()
        ))
    }
    val mm: KrakenHit = entries.foldLeft(
      new KrakenHit(
        taxonomyID = 1L,
        taxonomyName = "root",
        cladeCount = 0L,
        cladeSize = 0L,
        taxonRank = "-",
        cladeLevel = 0,
        parentTaxonomyID = 0L,
        children = List()
      )) { (bb: KrakenHit, aa: Map[Long, KrakenHit]) =>
        {
          mergeBranch(aa, bb)
        }
      }
    mapToJson(mm.toJSON()).spaces2
  }

  def main(args: Array[String]): Unit = {
    val commandArgs: Args = parseArgs(args)

    val jsonString: String = reportToJson(commandArgs.krakenreport)
    commandArgs.outputJson match {
      case Some(file) => {
        val writer = new PrintWriter(file)
        writer.println(jsonString)
        writer.close()
      }
      case _ => println(jsonString)
    }

  }
}
