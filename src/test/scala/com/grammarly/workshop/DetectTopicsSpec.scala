package com.grammarly.workshop

import com.grammarly.workshop.model.Record
import org.apache.commons.io.IOUtils
import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, FunSpec}

import scala.collection.JavaConverters._
import scala.io.Source

class DetectTopicsSpec extends FunSpec with BeforeAndAfterAll {

  val spark = SparkSession.builder().master("local[4]").getOrCreate()

  it("should get topics from documents") {
    val docs = IOUtils.readLines(this.getClass.getClassLoader.getResourceAsStream("docs/")).asScala

    import spark.implicits._
    val ds = docs.map { d =>
      val text = Source.fromInputStream(this.getClass.getClassLoader.getResourceAsStream(s"docs/$d")).getLines().mkString("\n")
      Record(d, "local", text)
    }.toDS()

    ds.coalesce(10).write.parquet("file:///tmp/sherlock/")

    val res = DetectTopics.detect(ds, "NN", vocabSize = 50000, numOfTopics = 10)
    res.select($"uri", $"topicDistribution").show(20, false)
  }

  override protected def afterAll(): Unit = {
    spark.close()
  }
}
