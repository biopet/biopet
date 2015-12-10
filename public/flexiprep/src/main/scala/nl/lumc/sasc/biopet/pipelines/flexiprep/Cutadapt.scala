/*
 * Copyright (c) 2015 Leiden University Medical Center and contributors
 *                    (see AUTHORS.md file for details).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
