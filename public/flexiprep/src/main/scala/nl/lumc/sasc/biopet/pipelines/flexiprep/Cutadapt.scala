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

  /** Clipped adapter names from FastQC */
  protected def seqToName = fastqc.foundAdapters
    .map(adapter => adapter.seq -> adapter.name).toMap

  override def summaryStats: Map[String, Any] = {
    val initStats = super.summaryStats
    // Map of adapter sequence and how many times it is found
    val adapterCounts: Map[String, Map[String, Any]] = initStats.get("adapters") match {
      // "adapters" key found in statistics
      case Some(v) => v match {
        case m: Map[String, Int] => m.toSeq
          .map {
            case (seq, count) =>
              seqToName.get(seq) match {
                // adapter sequence is found by FastQC
                case Some(n) => n -> Map("sequence" -> seq, "count" -> count)
                // adapter sequence is clipped but not found by FastQC ~ should not happen since all clipped adapter
                // sequences come from FastQC
                case _ =>
                  throw new IllegalStateException(s"Adapter '$seq' is clipped but not found by FastQC in '$fastq_input'.")
              }
          }.toMap
        // FastQC found no adapters
        case otherwise =>
          logger.info(s"No adapters found for summarizing in '$fastq_input'.")
          Map.empty[String, Map[String, Any]]
      }
      // "adapters" key not found ~ something went wrong in our part
      case _ => throw new RuntimeException(s"Required key 'adapters' not found in stats entry '$fastq_input'.")
    }
    initStats.updated("adapters", adapterCounts)
  }
}
