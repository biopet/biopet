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
package nl.lumc.sasc.biopet.pipelines.flexiprep

import nl.lumc.sasc.biopet.utils.config.Configurable

/**
 * Cutadapt wrapper specific for Flexiprep.
 *
 * This wrapper overrides the summary part so that instead of only displaying the clipped adapters, the sequence names
 * are also displayed. In Flexiprep the sequence will always have names since they are detected by FastQC from a list
 * of known adapters / contaminants.
 *
 * @param root: Configurable object from which this wrapper is initialized.
 * @param fastqc: Fastqc wrapper that contains adapter information.
 */
class Cutadapt(root: Configurable, fastqc: Fastqc) extends nl.lumc.sasc.biopet.extensions.Cutadapt(root) {

  val ignoreFastqcAdapters: Boolean = config("ignore_fastqc_adapters", default = false)

  val customAdaptersEnd: Map[String, Any] = config("custom_adapters_end", default = Map())
  adapter ++= customAdaptersEnd.values.map(_.toString)

  val customAdaptersFront: Map[String, Any] = config("custom_adapters_front", default = Map())
  front ++= customAdaptersFront.values.map(_.toString)

  val customAdaptersAny: Map[String, Any] = config("custom_adapters_any", default = Map())
  anywhere ++= customAdaptersAny.values.map(_.toString)

  /** Clipped adapter names from FastQC */
  protected def seqToName: Map[String, String] = {
    if (!ignoreFastqcAdapters) {
      (fastqc.foundAdapters ++ customAdapters)
        .map(adapter => adapter.seq -> adapter.name).toMap
    } else {
      customAdapters.map(adapter => adapter.seq -> adapter.name).toMap
    }
  }

  def customAdapters: Set[AdapterSequence] = {
    (customAdaptersEnd ++ customAdaptersFront ++ customAdaptersAny).flatMap(adapter => {
      adapter match {
        case (adapterName: String, sequence: String) =>
          Some(AdapterSequence(adapterName, sequence))
        case _ =>
          throw new IllegalStateException(s"Custom adapter was setup wrong for: $adapter")
      }
    }).toSet
  }

  override def summaryStats: Map[String, Any] = {
    val initStats = super.summaryStats
    // translationTable of sequences to the sequence-name, run once
    val seqToNameMap: Map[String, String] = seqToName
    // Map of adapter sequence and how many times it is found
    val adapterCounts: Map[String, Any] = initStats.get(adaptersStatsName) match {
      // "adapters" key found in statistics
      case Some(m: Map[_, _]) => m.flatMap {
        case (adapterSequence: String, adapterStats: Map[_, _]) =>
          seqToNameMap.get(adapterSequence) match {
            // adapter sequence is found by FastQC
            case Some(adapterSeqName) => {
              Some(adapterSeqName ->
                Map("sequence" -> adapterSequence, "stats" -> adapterStats.toMap)
              )
            }
            // adapter sequence is clipped but not found by FastQC ~ should not happen since all clipped adapter
            // sequences come from FastQC
            case _ =>
              throw new IllegalStateException(s"Adapter '$adapterSequence' is clipped but not found by FastQC in '$fastqInput'.")
          }
        // FastQC found no adapters
        case otherwise =>
          logger.debug(s"No adapters found for summarizing in '$fastqInput'.")
          None
      }
      // "adapters" key not found ~ something went wrong in our part
      case _ => throw new RuntimeException(s"Required key '${adaptersStatsName}' not found in stats entry '${fastqInput}'.")
    }
    initStats.updated(adaptersStatsName, adapterCounts)
  }
}
