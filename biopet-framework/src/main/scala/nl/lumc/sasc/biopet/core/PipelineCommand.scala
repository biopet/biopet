package nl.lumc.sasc.biopet.core


trait PipelineCommand extends MainCommand {

  def pipeline = "/" + getClass.getName.stripSuffix("$").replaceAll("\\.", "/") + ".class"

  def main(args: Array[String]): Unit = {
    var argv: Array[String] = Array()
    argv ++= Array("-S", pipeline)
    argv ++= args
    BiopetQCommandLine.main(argv)
  }
}