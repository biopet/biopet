package nl.lumc.sasc.biopet.core

trait MainCommand {

  lazy val name = this.getClass.getSimpleName
    .split("\\$").last

  def main(args: Array[String])
}
