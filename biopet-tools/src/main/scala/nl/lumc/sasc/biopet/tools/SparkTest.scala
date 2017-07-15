package nl.lumc.sasc.biopet.tools

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

/**
  * Created by pjvan_thof on 14-7-17.
  */
object SparkTest {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setAppName(this.getClass.getSimpleName)
      .setMaster("local[*]")
    val sc = new SparkContext(conf)

    def sleep(i: Int): Int = {
      Thread.sleep(10)
      i
    }

    (1 until 1000).toList
    println(sc.defaultParallelism)
    val bla = sc.parallelize(1 until 1000, 1000).map(x => x -> sleep(x))
    val bla2 = bla.groupBy(_._1).map(x => x._1 -> x._2.map(_._2).fold(0)(_ + _))

    println(bla2.cache().collect().mkString(";"))

    Thread.sleep(1000000)

    sc.stop
  }
}
