package nl.lumc.sasc.biopet.core

import java.util.Properties

object BiopetExecutable {

  val modules: Map[String, List[MainCommand]] = Map(
    "pipeline" -> List(
      nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep,
      nl.lumc.sasc.biopet.pipelines.mapping.Mapping,
      nl.lumc.sasc.biopet.pipelines.gentrap.Gentrap,
      nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics,
      nl.lumc.sasc.biopet.pipelines.gatk.GatkBenchmarkGenotyping,
      nl.lumc.sasc.biopet.pipelines.gatk.GatkGenotyping,
      nl.lumc.sasc.biopet.pipelines.gatk.GatkVariantcalling,
      nl.lumc.sasc.biopet.pipelines.gatk.GatkPipeline,
      nl.lumc.sasc.biopet.pipelines.gatk.GatkVariantRecalibration,
      nl.lumc.sasc.biopet.pipelines.gatk.GatkVcfSampleCompare,
      nl.lumc.sasc.biopet.pipelines.sage.Sage,
      nl.lumc.sasc.biopet.pipelines.basty.Basty,
      nl.lumc.sasc.biopet.pipelines.yamsvp.Yamsvp),
    "tool" -> List(
      nl.lumc.sasc.biopet.tools.WipeReads,
      nl.lumc.sasc.biopet.tools.ExtractAlignedFastq,
      nl.lumc.sasc.biopet.tools.BiopetFlagstat,
      nl.lumc.sasc.biopet.tools.CheckAllelesVcfInBam,
      nl.lumc.sasc.biopet.tools.VcfToTsv,
      nl.lumc.sasc.biopet.tools.VcfFilter,
      nl.lumc.sasc.biopet.tools.FindRepeatsPacBio,
      nl.lumc.sasc.biopet.tools.BedToInterval,
      nl.lumc.sasc.biopet.tools.MpileupToVcf,
      nl.lumc.sasc.biopet.tools.FastqSplitter)
  )
  
  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {

    def toBulletedList(m: List[MainCommand], kind: String = "", bullet: String = "-") =
      "Available %ss:\n  ".format(kind) + bullet + " " + m.map(x => x.commandName).sorted.mkString("\n  " + bullet + " ")

    def usage(module: String = null): String = {
      if (module != null) checkModule(module)
      val usage: String = {
        val set = if (module == null) modules.keySet else Set(module)
        val u = for (m <- set) yield toBulletedList(modules(m), m)
        u.mkString("\n\n")
      }
      """
        |Usage   : java -jar BiopetFramework.jar {%s} <name> [args]
        |Version : %s
        |
        |%s
        |
        |Subcommands:
        |  - version
        |
        |Questions or comments? Email sasc@lumc.nl or check out the project page at https://git.lumc.nl/biopet/biopet.git
      """.stripMargin.format(modules.keys.mkString(","), getVersion, usage)
    }

    def checkModule(module: String) {
      if (!modules.contains(module)) {
        System.err.println(s"ERROR: module '$module' does not exist\n" + usage())
        System.exit(1)
      }
    }

    def getCommand(module: String, name: String): MainCommand = {
      checkModule(module)
      val command = modules(module).find(p => p.commandName.toLowerCase == name.toLowerCase)
      if (command == None) {
        System.err.println(s"ERROR: command '$name' does not exist in module '$module'\n" + usage(module))
        System.exit(1)
      }
      return command.get
    }

    args match {
      case Array("version") => {
          println("version: " + getVersion)
      }
      case Array(module, name, passArgs @ _*) => {
        getCommand(module, name).main(passArgs.toArray)
      }
      case Array(module) => {
        System.err.println(usage(module))
        System.exit(1)
      }
      case _ => {
        System.err.println(usage())
        System.exit(1)
      }
    }
  }
  
  def getVersion = {
    getClass.getPackage.getImplementationVersion + " (" + getCommitHash + ")"
  }
  
  def getCommitHash = {
    val prop = new Properties()
    prop.load(getClass.getClassLoader.getResourceAsStream("git.properties"))
    prop.getProperty("git.commit.id.abbrev")
  }
}
