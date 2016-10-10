package nl.lumc.sasc.biopet.tools

import java.io.File
import java.net.URI

import argonaut.Argonaut._
import argonaut.Json._
import argonaut._
import nl.lumc.sasc.biopet.utils.{ConfigUtils, ToolCommand}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by pjvan_thof on 10-10-16.
  */
object GitlabIssueToJira extends ToolCommand {

  case class Args(gitlabUrl: String = null,
                  gitlabProject: String = null,
                  gitlabToken: String = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[String]("gitlabUrl") required () maxOccurs 1 valueName "<url>" action { (x, c) =>
      c.copy(gitlabUrl = x)
    }
    opt[String]("gitlabProject") required () maxOccurs 1 valueName "<string>" action { (x, c) =>
      c.copy(gitlabProject = x)
    }
    opt[String]("gitlabToken") required () maxOccurs 1 valueName "<string>" action { (x, c) =>
      c.copy(gitlabToken = x)
    }
  }

  def main(args: Array[String]): Unit = {

    val argsParser = new OptParser
    val cmdArgs = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    // Instantiation of the client
    // In a real-life application, you would instantiate one, share it everywhere,
    // and call wsClient.close() when you're done
    val url = s"${cmdArgs.gitlabUrl}/api/v3/projects/${cmdArgs.gitlabProject.replace("/", "%2F")}/issues"
    val wsClient = NingWSClient()
    val bla = wsClient
      .url(url)
      //.withQueryString("page" -> "1")
      .withHeaders("PRIVATE-TOKEN" -> cmdArgs.gitlabToken)
      .get()
      .map { wsResponse =>
        if (! (200 to 299).contains(wsResponse.status)) {
          logger.error(s"Received unexpected status ${wsResponse.status} : ${wsResponse.body}")
        }
        println(s"OK, received ${wsResponse.body}")
        //println(s"The response header PRIVATE-TOKEN was ${wsResponse.header("PRIVATE-TOKEN")}")
        ConfigUtils.textToJson(wsResponse.body)
      }

    Await.ready(bla, Duration.Inf)


    println(bla.value.get.get.array.get.size)
    bla.value.get.get match {
      case a:JsonArray  => println(a.size)
      case a => println(a.getClass.getName)
    }

    println(bla.value.get.get.spaces2)

    wsClient.close()

    logger.info(url)
  }
}
