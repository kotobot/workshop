package com.grammarly.workshop

import java.net.URL

import com.grammarly.workshop.model.Record
import org.apache.spark.sql.{Dataset, SaveMode, SparkSession}
import com.martinkl.warc._
import com.martinkl.warc.mapreduce._
import com.optimaize.langdetect.LanguageDetectorBuilder
import com.optimaize.langdetect.ngram.NgramExtractors
import com.optimaize.langdetect.profiles.LanguageProfileReader
import com.optimaize.langdetect.text.CommonTextObjectFactories
import org.apache.hadoop.io._
import org.apache.spark.rdd.RDD

import scala.util.Try

object FilterEnglishTexts {

  val MinContentLength = 1000
  val Prefix = "s3://commoncrawl/"

  def main(args: Array[String]): Unit = {

    require(args.length >= 2)
    val paths = args(0) //"s3://commoncrawl/crawl-data/CC-MAIN-2018-09/wet.paths.gz"
    val output = args(1) //"s3://workshop-lviv/data/ds1/"
    val count = if (args.size > 2) Some(args(3).toInt) else None

    val spark = SparkSession.builder().getOrCreate()
    val allWetPaths = spark.read.textFile(paths)
    val wetPaths = count match {
      case Some(c) => allWetPaths.take(c)
      case None => allWetPaths.collect()
    }
    val s3Paths = wetPaths.map(Prefix + _).mkString(",")

    val wetsRDD = spark.sparkContext.newAPIHadoopFile[LongWritable, WARCWritable, WARCInputFormat](s3Paths)
    val recordsDS = filter(spark, wetsRDD)
    recordsDS
      .coalesce(10)
      .write
      .mode(SaveMode.Overwrite)
      .parquet(output)

    spark.close()
  }

  def filter(spark: SparkSession, wetsRDD: RDD[(LongWritable, WARCWritable)]): Dataset[Record] = {
    import spark.implicits._
    wetsRDD
      .repartition(100)
      .mapPartitions { it =>
        val languageProfiles = new LanguageProfileReader().readAllBuiltIn()
        val languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard).withProfiles(languageProfiles).build
        val textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText

        def isEnglish(text: String) = {
          val textObject = textObjectFactory.forText(text)
          val ps = languageDetector.getProbabilities(textObject)
          !ps.isEmpty && ps.get(0).getProbability > 0.7 && ps.get(0).getLocale.getLanguage == "en"
        }

        it.flatMap { case (_, r) =>
          val content = new String(r.getRecord().getContent(), "UTF-8")
          val uri = r.getRecord().getHeader().getTargetURI()
          if (content.length >= MinContentLength && isEnglish(content)) {
            Try {
              val url = new URL(uri)
              Record(uri, url.getHost, content)
            }.toOption
          } else {
            None
          }
        }
      }
      .toDS()
  }

}