package nl.lumc.sasc.biopet.tools

import java.io.{BufferedReader, File, InputStreamReader}

import nl.lumc.sasc.biopet.utils.{ConfigUtils, ToolCommand}
import play.api.libs.ws.{InMemoryBody, WSAuthScheme, WSRequest}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

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
                  jiraUsername: String = null,
                  jiraIssueType: String = null,
                  userMappings: Option[File] = null,
                  jiraPassword: Option[String] = None) extends AbstractArgs

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
    opt[String]("jiraPassword") maxOccurs 1 valueName "<string>" action { (x, c) =>
      c.copy(jiraPassword = Some(x))
    }
    opt[String]("jiraIssueType") required () maxOccurs 1 valueName "<string>" action { (x, c) =>
      c.copy(jiraIssueType = x)
    }
    opt[File]("userMappings") required () maxOccurs 1 valueName "<string>" action { (x, c) =>
      c.copy(userMappings = Some(x))
    } text "tsv to say which user belong to eachother, 1e column gitlab user, 2e column jira user"

  }

  def main(args: Array[String]): Unit = {

    val argsParser = new OptParser
    val cmdArgs = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val c = System.console()

    val pw = cmdArgs.jiraPassword match {
      case Some(pass) => pass
      case _ if c != null => c.readPassword("Jira password >").toString
      case _ => ""
    }

    // Instantiation of the client
    // In a real-life application, you would instantiate one, share it everywhere,
    // and call wsClient.close() when you're done
    val wsClient = NingWSClient()

    val gitlabIssuesFuture = getGitlabIssues(cmdArgs.gitlabUrl, cmdArgs.gitlabProject, cmdArgs.gitlabToken, wsClient)
    gitlabIssuesFuture.onFailure { case e => throw e }

    val gitlabIssues = Await.result(gitlabIssuesFuture, Duration.Inf)

    println(gitlabIssues.size)
    println(gitlabIssues.head.issue("author"))
    println(gitlabIssues.head.comments.map(_.keys))

    val createmetaUrl = s"${cmdArgs.jiraUrl}/rest/api/2/issue/createmeta"
    logger.info("Project URL: " + createmetaUrl)
    val metacreateFuture = getWsrequestJsonToMap(wsClient
      .url(createmetaUrl)
      .withAuth(cmdArgs.jiraUsername, pw, WSAuthScheme.BASIC)
      .withQueryString("projectKeys" -> cmdArgs.jiraProject, "issuetypeNames" -> cmdArgs.jiraIssueType))

    logger.info("Done1")

    val metacreate = waitForFuture(metacreateFuture)
    val projects = metacreate("projects").asInstanceOf[List[Map[String, Any]]]
    require(projects.size == 1)
    val issuesTypes = projects.head("issuetypes").asInstanceOf[List[Map[String, Any]]]
    require(issuesTypes.size == 1)
    val projectId = projects.head("id")
    val issueTypeId = issuesTypes.head("id")
    logger.info("projectid: " + projectId)
    logger.info("issueTypeId: " + issueTypeId)

    def issueBody = s"""
{
  "fields": {
    "project": {
      "id": "${projectId}"
    },
    "summary": "something's wrong",
    "issuetype": {
      "id": "${issueTypeId}"
    },
    "reporter": {
      "name": "pjvan_thof"
    },
    "description": "description"
  }
}
"""

    val bla = cmdArgs.userMappings.map(file =>
      Source.fromFile(file).getLines().map(_.split("\t", 2)).map(x => (x(0), x(1))).toMap
    ).getOrElse(Map())

    val gitlabUsers = (gitlabIssues.map(_.issue("author").asInstanceOf[Map[String, Any]]("username").toString) :::
      gitlabIssues.flatMap { x =>
        if (!x.isInstanceOf[Map[String, Any]]) None
        else Some(x.issue("assignee").asInstanceOf[Map[String, Any]]("username").toString)
      }).distinct

    gitlabUsers.foreach(user => require(bla.contains(user), s"User $user can't be found"))

    gitlabIssues.map { gitlabIssue =>
      val fields = Map(
        "project" -> Map("id" -> projectId),
        "summary" -> gitlabIssue.issue("title"),
        "issuetype" -> Map("id" -> issueTypeId),
        "description" -> gitlabIssue.issue("description"),
        "reporter" -> Map("name" -> gitlabIssue.issue("")),
        "labels" -> gitlabIssue.issue("labels"),
        "created" -> gitlabIssue.issue("created_at")
      )
      val body = ConfigUtils.mapToJson(Map("fields" -> fields)).nospaces

    }

    val issueUrl = s"${cmdArgs.jiraUrl}/rest/api/2/issue"
    logger.info("Issue URL: " + issueUrl)
    val issueFuture = getWsrequestJsonToMap(wsClient
      .url(issueUrl)
      .withAuth(cmdArgs.jiraUsername, pw, WSAuthScheme.BASIC), Some(issueBody), "POST")

    val issue = waitForFuture(issueFuture)

    println(issue)

    logger.info("Done2")
    wsClient.close()

    logger.info("Done3")
  }

  def getWsrequestJsonToMap(request: WSRequest, body: Option[String] = None, method: String = "GET") = {
    val f = (method, body) match {
      case ("POST", Some(body)) => request.withHeaders("Content-Type" -> "application/json").post(body)
      case ("PUT", Some(body)) => request.withHeaders("Content-Type" -> "application/json").put(body)
      case ("GET", None) => request.get()
      case ("DELETE", None) => request.delete()
    }
    val r = f.map { wsResponse =>
      if (wsResponse.status >= 200 && wsResponse.status <= 299)
        ConfigUtils.jsonToMap(ConfigUtils.textToJson(wsResponse.body))
      else {
        logger.error(s"Request gave status ${wsResponse.status}: ${wsResponse.body}")
        throw new IllegalStateException(s"Request gave status ${wsResponse.status}")
      }
    }
    r.onFailure { case e => throw new RuntimeException(e) }
    r
  }

  case class GitlabIssue(issue: Map[String, Any], comments: List[Map[String, Any]])

  def waitForFuture[T](future: Future[T]): T = {
    Await.ready(future, Duration.Inf)
    if (future.value.get.isFailure) sys.exit(1)
    Await.result(future, Duration.Inf)
  }

  def getGitlabIssues(url: String,
                      project: String,
                      token: String,
                      wsClient: NingWSClient,
                      page: Int = 1,
                      perPage: Int = 100): Future[List[GitlabIssue]] = {
    val issuesUrl = s"${url}/api/v3/projects/${project.replace("/", "%2F")}/issues"
    getGitlabPagedOutput(issuesUrl, token, wsClient, Map("per_page" -> "100"))
      .map { issues =>
        issues.map { issue =>
          if (issue("user_notes_count") == 0) GitlabIssue(issue, Nil)
          else GitlabIssue(issue, waitForFuture(getGitlabPagedOutput(issuesUrl + s"/${issue("id")}/notes", token, wsClient)))
        }
      }
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

      val (page1, pages) = waitForFuture(init)

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

      results.foldLeft(page1) { case (a, b) => a ++ waitForFuture(b) }
    }
    f.onFailure { case e => throw e }
    f
  }
}
