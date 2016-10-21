package nl.lumc.sasc.biopet.tools

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.utils.{ ConfigUtils, ToolCommand }
import play.api.libs.ws.WSRequest
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.io.Source

/**
 * Created by pjvan_thof on 10-10-16.
 */
object GitlabIssueToJira extends ToolCommand {

  case class Args(gitlabUrl: String = null,
                  gitlabProject: String = null,
                  gitlabToken: String = null,
                  outputFile: File = null,
                  userMappings: Option[File] = null) extends AbstractArgs

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
    opt[File]("outputFile") required () maxOccurs 1 valueName "<string>" action { (x, c) =>
      c.copy(outputFile = x)
    }
    opt[File]("userMappings") required () maxOccurs 1 valueName "<string>" action { (x, c) =>
      c.copy(userMappings = Some(x))
    } text "tsv to say which user belong to eachother, 1e column gitlab user, 2e column jira user"

  }

  def main(args: Array[String]): Unit = {

    val argsParser = new OptParser
    val cmdArgs = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

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

    val userMappings = cmdArgs.userMappings.map(file =>
      Source.fromFile(file).getLines().filter(_.nonEmpty).map(_.split("\t", 2)).map(x => (x(0), x(1))).toMap
    ).getOrElse(Map())

    val gitlabUsers = (gitlabIssues.map(_.issue("author").asInstanceOf[Map[String, Any]]("username").toString) :::
      gitlabIssues.flatMap { x =>
        if (!x.isInstanceOf[Map[String, Any]]) None
        else Some(x.issue("assignee").asInstanceOf[Map[String, Any]]("username").toString)
      }).distinct

    gitlabUsers.foreach(user => require(userMappings.contains(user), s"User $user can't be found"))

    val issueValues = gitlabIssues.reverse.map { gitlabIssue =>
      val reporter = userMappings(gitlabIssue.issue("author").asInstanceOf[Map[String, Any]]("username").toString)
      val assignee = if (!gitlabIssue.issue("author").isInstanceOf[Map[String, Any]]) None
      else Some(userMappings(gitlabIssue.issue("author").asInstanceOf[Map[String, Any]]("username").toString))
      val milestone = if (!gitlabIssue.issue("milestone").isInstanceOf[Map[String, Any]]) None
      else Some(gitlabIssue.issue("milestone").asInstanceOf[Map[String, Any]]("title").toString)

      val comments = gitlabIssue.comments.map { comment =>
        Map(
          "body" -> comment("body"),
          "author" -> userMappings(comment("author").asInstanceOf[Map[String, Any]]("username").toString),
          "created_at" -> comment("created_at")
        )
      }

      val closedDate: Option[String] = gitlabIssue.issue("state") match {
        case "closed" =>
          Some(comments.filter(c => c("body").toString.contains("Status changed to closed")).last("created_at").toString)
        case _ => None
      }

      val commentFields = comments.zipWithIndex.map {
        case (comment, key) =>
          s"comment-$key" -> s"${comment("created_at")};${comment("author")};${comment("body")}"
      } toMap

      val fields = Map(
        "summary" -> gitlabIssue.issue("title"),
        "description" -> gitlabIssue.issue("description"),
        "reporter" -> reporter,
        "gitlab_iid" -> gitlabIssue.issue("iid"),
        "gitlab_id" -> gitlabIssue.issue("id"),
        "state" -> gitlabIssue.issue("state"),
        "labels" -> gitlabIssue.issue("labels").asInstanceOf[List[String]].mkString(" "),
        "weburl" -> gitlabIssue.issue("web_url"),
        "created" -> gitlabIssue.issue("created_at"),
        "updated_at" -> gitlabIssue.issue("updated_at")
      ) ++ assignee.map("assignee" -> _) ++
        milestone.map("milestone" -> _) ++
        closedDate.map("resolution_date" -> _) ++
        closedDate.map(x => "resolution" -> "Done")

      fields ++ commentFields
    }

    val keys = issueValues.flatMap(_.keys).distinct
    val writer = new PrintWriter(cmdArgs.outputFile)
    writer.println(keys.mkString(","))
    issueValues.foreach { issue =>
      writer.print("\"" + issue.get(keys.head).getOrElse("").toString.replaceAll("\"", "\"\"") + "\"")
      keys.tail.foreach { key =>
        val value = issue.get(key).getOrElse("").toString.replaceAll("\"", "\"\"")
        writer.print("," + (if (value.nonEmpty) "\"" + value + " \"" else ""))
      }
      writer.println
    }
    writer.close()

    logger.info("Done2")
    wsClient.close()

    logger.info("Done3")
  }

  def getWsrequestJsonToMap(request: WSRequest, body: Option[String] = None, method: String = "GET") = {
    val f = (method, body) match {
      case ("POST", Some(body)) => request.withHeaders("Content-Type" -> "application/json").post(body)
      case ("PUT", Some(body))  => request.withHeaders("Content-Type" -> "application/json").put(body)
      case ("GET", None)        => request.get()
      case ("DELETE", None)     => request.delete()
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
          //if (issue("user_notes_count") == 0) GitlabIssue(issue, Nil)
          GitlabIssue(issue, waitForFuture(getGitlabPagedOutput(issuesUrl + s"/${issue("id")}/notes", token, wsClient)))
          //          GitlabIssue(issue, Nil)
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
