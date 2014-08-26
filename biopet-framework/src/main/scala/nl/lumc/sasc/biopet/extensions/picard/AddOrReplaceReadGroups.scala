package nl.lumc.sasc.biopet.extensions.picard

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

class AddOrReplaceReadGroups(val root: Configurable) extends Picard {
  javaMainClass = "picard.sam.AddOrReplaceReadGroups"

  @Input(doc = "The input SAM or BAM files to analyze.", required = true)
  var input: File = _

  @Output(doc = "The output file to bam file to", required = true)
  var output: File = _

  @Argument(doc = "Sort order of output file Required. Possible values: {unsorted, queryname, coordinate} ", required = true)
  var sortOrder: String = _
  
  @Argument(doc = "RGID", required = true)
  var RGID: String = _
  
  @Argument(doc = "RGLB", required = true)
  var RGLB: String = _
  
  @Argument(doc = "RGPL", required = true)
  var RGPL: String = _
  
  @Argument(doc = "RGPU", required = true)
  var RGPU: String = _
  
  @Argument(doc = "RGSM", required = true)
  var RGSM: String = _
  
  @Argument(doc = "RGCN", required = false)
  var RGCN: String = _
  
  @Argument(doc = "RGDS", required = false)
  var RGDS: String = _
  
  @Argument(doc = "RGDT", required = false)
  var RGDT: String = _
  
  @Argument(doc = "RGPI", required = false)
  var RGPI: Option[Int] = _

  override def commandLine = super.commandLine +
    required("INPUT=", input, spaceSeparated = false) +
    required("OUTPUT=", output, spaceSeparated = false) +
    required("SORT_ORDER=", sortOrder, spaceSeparated = false) +
    required("RGID=", RGID, spaceSeparated = false) +
    required("RGLB=", RGLB, spaceSeparated = false) +
    required("RGPL=", RGPL, spaceSeparated = false) +
    required("RGPU=", RGPU, spaceSeparated = false) +
    required("RGSM=", RGSM, spaceSeparated = false) +
    optional("RGCN=", RGCN, spaceSeparated = false) +
    optional("RGDS=", RGDS, spaceSeparated = false) +
    optional("RGDT=", RGDT, spaceSeparated = false) +
    optional("RGPI=", RGPI, spaceSeparated = false)
}

object AddOrReplaceReadGroups {
  def apply(root: Configurable, input: File, output: File, sortOrder: String = null): AddOrReplaceReadGroups = {
    val addOrReplaceReadGroups = new AddOrReplaceReadGroups(root)
    addOrReplaceReadGroups.input = input
    addOrReplaceReadGroups.output = output
    if (sortOrder == null) addOrReplaceReadGroups.sortOrder = "coordinate"
    else addOrReplaceReadGroups.sortOrder = sortOrder
    return addOrReplaceReadGroups
  }
}