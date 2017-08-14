package nl.lumc.sasc.biopet.pipelines.gears
import nl.lumc.sasc.biopet.utils.summary.db.Schema.{Library, Sample}
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.{ModuleName, SampleId}
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.Implicts._

object GearsKronaPlot {
  def values(summary: SummaryDb,
             runId: Int,
             summaryPipelineName: String,
             summaryModuleName: String,
             allSamples: Seq[Sample],
             allLibraries: Seq[Library],
             sampleId: Option[Int] = None,
             libId: Option[Int] = None,
             centrifugeTag: Option[String] = None): Map[String, Any] = {
    val summariesVal =
      summaries(summary, runId, sampleId, libId, summaryPipelineName, summaryModuleName)
    val totalReadsVal = totalReads(summary,
      runId,
      sampleId,
      libId,
      summaryPipelineName,
      centrifugeTag,
      allSamples,
      allLibraries)
    Map(
      "sampleId" -> sampleId,
      "summaryPipelineName" -> summaryPipelineName,
      "summaryModuleName" -> summaryModuleName,
      "centrifugeTag" -> centrifugeTag,
      "allSamples" -> allSamples,
      "summaries" -> summariesVal,
      "totalReads" -> totalReadsVal
    )
  }

  def summaries(summary: SummaryDb,
                runId: Int,
                sampleId: Option[Int],
                libId: Option[Int],
                summaryPipelineName: String,
                summaryModuleName: String): Map[Int, Map[String, Option[Any]]] = {
    if (libId.isDefined)
      summary
        .getStatsForLibraries(runId,
          summaryPipelineName,
          summaryModuleName,
          sampleId,
          Map("all" -> Nil))
        .filter(_._1._2 == libId.get)
        .map(x => x._1._1 -> x._2)
    else
      summary.getStatsForSamples(runId,
        summaryPipelineName,
        summaryModuleName,
        sampleId.map(SampleId),
        Map("all" -> Nil))
  }
  def totalReads(
                  summary: SummaryDb,
                  runId: Int,
                  sampleId: Option[Int],
                  libId: Option[Int],
                  summaryPipelineName: String,
                  centrifugeTag: Option[String],
                  allSamples: Seq[Sample],
                  allLibraries: Seq[Library]
                ): Option[Map[String, Long]] = {
    centrifugeTag.map { tag =>
      if (libId.isDefined) {
        val stats = summary
          .getStatsForLibraries(runId,
            summaryPipelineName,
            ModuleName(tag),
            sampleId,
            Map("total" -> List("metrics", "Read")))
          .filter(_._1._2 == libId.get)
          .head
        val lib = allLibraries.filter(_.id == stats._1._2).head
        val sample = allSamples.filter(_.id == stats._1._1).head
        Map(s"${sample.name}" -> stats._2("total").map(_.toString.toLong).getOrElse(0L))
      } else
        summary
          .getStatsForSamples(runId,
            summaryPipelineName,
            ModuleName(tag),
            sampleId.map(SummaryDb.SampleId),
            Map("total" -> List("metrics", "Read")))
          .map(
            x =>
              allSamples
                .find(_.id == x._1)
                .head
                .name -> x._2("total").map(_.toString.toLong).getOrElse(0L))
    }
  }

}
