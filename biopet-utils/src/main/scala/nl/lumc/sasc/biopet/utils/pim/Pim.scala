package nl.lumc.sasc.biopet.utils.pim

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import play.api.libs.ws._
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import nl.lumc.sasc.biopet.utils.ConfigUtils
import play.api.Play.current

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by pjvanthof on 17/03/2017.
  */
trait PimClasses {
  def toMap: Map[String, Any]

  def toJson = ConfigUtils.mapToJson(toMap)
}

case class Run(id: String ,
               network: Network,
               description: String,
               workflowEngine: String,
               collapse: Boolean = false) extends PimClasses {
  def toMap = Map(
    "id" -> id,
    "network" -> network.toMap,
    "description" -> description,
    "workflow_engine" -> workflowEngine,
    "collapse" -> collapse
  )
}

case class Network(description: String, groups: List[Group], nodes: List[Node], links: List[Link]) extends PimClasses {
  def toMap = Map(
    "description" -> description,
    "groups" -> groups.map(_.toMap),
    "nodes" -> nodes.map(_.toMap),
    "links" -> links.map(_.toMap)
  )
}

case class Group(description: String, id: String, parentGroup: String) extends PimClasses {
  def toMap = Map(
    "id" -> id,
    "description" -> description,
    "parent_group" -> parentGroup
  )
}
case class Node(id: String, groupId: Group, inPorts: List[Port], outPorts: List[Port], nodeType: String) extends PimClasses {
  def toMap = Map(
    "id" -> id,
    "group_id" -> groupId,
    "in_ports" -> inPorts.map(_.toMap),
    "out_ports" -> outPorts.map(_.toMap),
    "type" -> nodeType
  )
}
case class Link(id:String, fromNode: String, fromPort: String, toNode: String, toPort: String, linkType: String) extends PimClasses {
  def toMap = Map(
    "id" -> id,
    "from_node" -> fromNode,
    "from_port" -> fromPort,
    "to_node" -> toNode,
    "to_port" -> toPort,
    "type" -> linkType
  )
}
case class Port(id: String, description: String) extends PimClasses {
  def toMap = Map(
    "id" -> id,
    "description" -> description
  )
}

case class Job(id: String, nodeId: String, runId: String, sampleId: String, status: JobStatus.Value) extends PimClasses {
  def toMap() = Map(
    "id" -> id,
    "node_id" -> nodeId,
    "run_id" -> runId,
    "sample_id" -> sampleId,
    "status" -> status.toString
  )
}


object JobStatus extends Enumeration {
  val idle, running, success, failed = Value
}

object Pim {
  def createRun(run: Run): Unit = {
//    implicit val system = ActorSystem()
//    system.registerOnTermination {
//      System.exit(0)
//    }
    //implicit val materializer = ActorMaterializer()

    // Create the standalone WS client
    // no argument defaults to a AhcWSClientConfig created from
    // "AhcWSClientConfigFactory.forConfig(ConfigFactory.load, this.getClass.getClassLoader)"
//    val wsClient = WSClientConfigFactory.forConfig(ConfigFactory.load, this.getClass.getClassLoader)
//    wsClient.url("http://www.google.com")
//
//    println(wsClient.body)

  }
}