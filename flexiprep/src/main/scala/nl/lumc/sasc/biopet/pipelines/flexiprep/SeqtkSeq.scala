package nl.lumc.sasc.biopet.pipelines.flexiprep

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.Ln

class SeqtkSeq(root: Configurable) extends nl.lumc.sasc.biopet.extensions.seqtk.SeqtkSeq(root) {
  var fastqc: Fastqc = _

  override def beforeCmd {
    super.beforeCmd
    if (fastqc != null && Q == None) {
      Q = fastqc.getEncoding match {
        case null => None
        case s if (s.contains("Sanger / Illumina 1.9")) => None
        case s if (s.contains("Illumina <1.3")) => Option(64)
        case s if (s.contains("Illumina 1.3")) => Option(64)
        case s if (s.contains("Illumina 1.5")) => Option(64)
      }
      if (Q != None) V = true
    }
  }
  
  override def cmdLine = {
    if (Q != None) {
      analysisName = getClass.getName
      super.cmdLine
    } else {
      analysisName = getClass.getSimpleName + "-ln"
      Ln(this, input, output).cmd
    }
  }
}

object SeqtkSeq {
  def apply(root: Configurable, input:File, output:File, fastqc:Fastqc = null): SeqtkSeq = {
    val seqtkSeq = new SeqtkSeq(root)
    seqtkSeq.input = input
    seqtkSeq.output = output
    seqtkSeq.fastqc = fastqc
    return seqtkSeq
  }
}