package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile

class VariantRecalibrator(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.VariantRecalibrator with GatkGeneral {
  nt = Option(getThreads(4))
  memoryLimit = Option(nt.getOrElse(1) * 2)
  
  if (config.contains("dbsnp")) resource :+= new TaggedFile(config("dbsnp").getString, "known=true,training=false,truth=false,prior=2.0")
  
  an = config("annotation", default = List("QD", "DP", "FS", "ReadPosRankSum", "MQRankSum")).getStringList
  minNumBadVariants = config("minnumbadvariants")
  maxGaussians = config("maxgaussians")
}
  
object VariantRecalibrator {
  def apply(root: Configurable, input:File, recal_file:File, tranches_file:File, indel: Boolean = false): VariantRecalibrator = {
    val vr = new VariantRecalibrator(root) {
      override val configName = "variantrecalibrator"
      override val configFullPath = (if (indel) "indel" else "snp") :: configName :: configPath
      if (indel) {
        mode = org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.INDEL
        defaults ++= Map("ts_filter_level" -> 99.0)
        if (config.contains("mills")) resource :+= new TaggedFile(config("mills").getString, "known=false,training=true,truth=true,prior=12.0")
      } else  {
        mode = org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.SNP
        defaults ++= Map("ts_filter_level" -> 99.5)
        if (config.contains("hapmap")) resource +:= new TaggedFile(config("hapmap").getString, "known=false,training=true,truth=true,prior=15.0")
        if (config.contains("omni")) resource +:= new TaggedFile(config("omni").getString, "known=false,training=true,truth=true,prior=12.0")
        if (config.contains("1000G")) resource +:= new TaggedFile(config("1000G").getString, "known=false,training=true,truth=false,prior=10.0")
      }
    }
    vr.input :+= input
    vr.recal_file = recal_file
    vr.tranches_file = tranches_file
    return vr
  }
}