/**
  * Biopet is built on top of GATK Queue for building bioinformatic
  * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
  * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
  * should also be able to execute Biopet tools and pipelines.
  *
  * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
  *
  * Contact us at: sasc@lumc.nl
  *
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
package nl.lumc.sasc.biopet.utils

/**
  * Created by pjvanthof on 16/12/2016.
  */
object Question {

  def string(name: String,
             default: Option[String] = None,
             description: Option[String] = None,
             posibleValues: List[String] = Nil,
             validation: List[(String) => Boolean] = Nil): String = {
    description.foreach(println)
    if (posibleValues.nonEmpty) println(s"possible values: ${posibleValues.mkString(", ")}")
    default.foreach(x => println(s"Default value: $x"))
    print(s"$name > ")
    (scala.io.StdIn.readLine.trim, default) match {
      case (a, Some(d)) if a.isEmpty => d
      case (a, None) if a.isEmpty =>
        println("ERROR: Value is required")
        string(name, default, description, posibleValues, validation)
      case (a, _) =>
        if (!validation.forall(_(a))) {
          println("ERROR: Validation of failed")
          string(name, default, description, posibleValues, validation)
        } else if (posibleValues.nonEmpty && !posibleValues.contains(a)) {
          println("ERROR: Value not allowed")
          string(name, default, description, posibleValues, validation)
        } else a
    }
  }

  def boolean(name: String,
              default: Option[Boolean] = None,
              description: Option[String] = None): Boolean = {
    description.foreach(println)
    default.foreach(x => println(s"Default value: $x"))
    print(s"$name (y/n) > ")
    scala.io.StdIn.readLine.trim.toLowerCase match {
      case "" =>
        default match {
          case Some(d) => d
          case _ =>
            println("ERROR: Value is required")
            boolean(name, default, description)
        }
      case "y" | "yes" | "true" => true
      case "n" | "no" | "false" => false
      case _ =>
        println("ERROR: Value is a boolean value, please select 'y' of 'n'")
        boolean(name, default, description)
    }
  }

  def list(name: String,
           default: Option[List[String]] = None,
           description: Option[String] = None,
           posibleValues: List[String] = Nil,
           validation: (String) => Boolean = String => true): List[String] = {
    description.foreach(println)
    if (posibleValues.nonEmpty) println(s"possible values: ${posibleValues.mkString(", ")}")
    default.foreach(x => println(s"Default value: $x"))
    print(s"$name > ")
    (scala.io.StdIn.readLine.split(",").toList.map(_.trim), default) match {
      case (List(""), Some(d)) => d
      case (List(""), None) =>
        println("ERROR: Value is required")
        list(name, default, description, posibleValues, validation)
      case (a, _) =>
        if (!a.forall(validation)) {
          println("ERROR: Validation of failed")
          list(name, default, description, posibleValues, validation)
        } else if (posibleValues.nonEmpty && !a.forall(posibleValues.contains)) {
          println("ERROR: Value not allowed")
          list(name, default, description, posibleValues, validation)
        } else a
    }
  }
}
