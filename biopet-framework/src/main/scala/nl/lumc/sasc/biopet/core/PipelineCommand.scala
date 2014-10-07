package nl.lumc.sasc.biopet.core


trait PipelineCommand extends MainCommand {

  val pipeline = ""

  def main(args: Array[String]): Unit = {
    var argv: Array[String] = Array()
    argv ++= Array("-S", pipeline)
    argv ++= args
    BiopetQCommandLine.main(argv)
  }
}