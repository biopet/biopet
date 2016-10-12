package nl.lumc.sasc.biopet.tools

import java.io.{ BufferedReader, InputStreamReader }

import nl.lumc.sasc.biopet.utils.{ ConfigUtils, ToolCommand }
import play.api.libs.ws.{ WSAuthScheme, WSRequest }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by pjvan_thof on 10-10-16.
 */
object GitlabIssueToJira extends ToolCommand {

  case class Args(gitlabUrl: String = null,
                  gitlabProject: String = null,
                  gitlabToken: String = null,
                  jiraUrl: String = null,
                  jiraProject: String = null,
                  jiraToken: String = null,
                  jiraUsername: String = null) extends AbstractArgs

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
    opt[String]("jiraUrl") required () maxOccurs 1 valueName "<url>" action { (x, c) =>
      c.copy(jiraUrl = x)
    }
    opt[String]("jiraProject") required () maxOccurs 1 valueName "<string>" action { (x, c) =>
      c.copy(jiraProject = x)
    }
    opt[String]("jiraUsername") required () maxOccurs 1 valueName "<string>" action { (x, c) =>
      c.copy(jiraUsername = x)
    }
  }

  def main(args: Array[String]): Unit = {

    val argsParser = new OptParser
    val cmdArgs = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val c = System.console()

    //Console.out.close()
    val pw = if (c != null) c.readPassword("Jira password >").toString else ""
    //println(pw)

    // Instantiation of the client
    // In a real-life application, you would instantiate one, share it everywhere,
    // and call wsClient.close() when you're done
    val wsClient = NingWSClient()

    //    val gitlabIssuesFuture = getGitlabIssues(cmdArgs.gitlabUrl, cmdArgs.gitlabProject, cmdArgs.gitlabToken, wsClient)
    //    gitlabIssuesFuture.onFailure { case e => throw e }
    //
    //    val gitlabIssues = Await.result(gitlabIssuesFuture, Duration.Inf)
    //
    //    println(gitlabIssues.size)
    //    println(gitlabIssues.head.issue("author"))
    //    println(gitlabIssues.head.comments.map(_.keys))

    val url = s"${cmdArgs.jiraUrl}/rest/api/2/project/${cmdArgs.jiraProject}"
    println(url)
    val test = getWsrequestJsonToMap(wsClient
      .url(url)
      .withAuth(cmdArgs.jiraUsername, pw, WSAuthScheme.BASIC))

    logger.info("Done1")
    Await.ready(test, Duration.Inf)
    if (test.value.get.isFailure) sys.exit(1)
    logger.info("Done2")
    wsClient.close()

    logger.info("Done3")
  }

  def getWsrequestJsonToMap(request: WSRequest) = {
    val f = request.get()
    val r = f.map { wsResponse =>
      val map = if (wsResponse.status >= 200 && wsResponse.status <= 299)
        ConfigUtils.jsonToMap(ConfigUtils.textToJson(wsResponse.body))
      else throw new IllegalStateException(s"Request gave status ${wsResponse.status}")
    }
    r.onFailure { case e => throw new RuntimeException(e) }
    r
  }

  case class GitlabIssue(issue: Map[String, Any], comments: List[Map[String, Any]])

  def getGitlabIssues(url: String,
                      project: String,
                      token: String,
                      wsClient: NingWSClient,
                      page: Int = 1,
                      perPage: Int = 100): Future[List[GitlabIssue]] = {
    val issuesUrl = s"${url}/api/v3/projects/${project.replace("/", "%2F")}/issues"
    getGitlabPagedOutput(issuesUrl, token, wsClient, Map("per_page" -> "100"))
      .map { x => x.map(i => GitlabIssue(i, Await.result(getGitlabPagedOutput(issuesUrl + s"/${i("id")}/notes", token, wsClient), Duration.Inf))) }
  }

  def getGitlabPagedOutput(url: String,
                           token: String,
                           wsClient: NingWSClient,
                           queryParams: Map[String, String] = Map()): Future[List[Map[String, Any]]] = {
    val f = Future {
      val init = wsClient
        .url(url)
        .withHeaders("PRIVATE-TOKEN" -> token)
        .withQueryString(queryParams.toSeq: _*)
        .get().map { wsResponse =>
          val pages = wsResponse.header("X-Total-Pages").map(_.toInt).getOrElse(1)
          val page1 = ConfigUtils.textToJson(wsResponse.body).array.get.map(ConfigUtils.jsonToMap)
          (page1, pages)
        }

      val (page1, pages) = Await.result(init, Duration.Inf)

      val results = for (i <- 2 to pages) yield {
        wsClient
          .url(url)
          .withHeaders("PRIVATE-TOKEN" -> token)
          .withQueryString((queryParams.toSeq :+ ("page" -> s"$i")): _*)
          .get().map { wsResponse =>
            if (wsResponse.status >= 200 && wsResponse.status <= 299)
              ConfigUtils.textToJson(wsResponse.body).array.get.map(ConfigUtils.jsonToMap)
            else throw new IllegalStateException(s"Request gave status ${wsResponse.status}")
          }
      }

      results.foldLeft(page1) { case (a, b) => a ++ Await.result(b, Duration.Inf) }
    }
    f.onFailure { case e => throw e }
    f
  }
}
