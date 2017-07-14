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
      Thread.sleep(1000)
      i
    }

    val bla = sc.parallelize(1 until 1000).map(sleep)
    val bla2 = bla.reduce((a, b) => a + b)

    println(bla2)

    Thread.sleep(1000000)

    sc.stop
  }
}
