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
    (Console.readLine.trim, default) match {
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
    Console.readLine.trim.toLowerCase match {
      case "" => default match {
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
    (Console.readLine.split(",").toList.map(_.trim), default) match {
      case (Nil, Some(d)) => d
      case (Nil, None) =>
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
