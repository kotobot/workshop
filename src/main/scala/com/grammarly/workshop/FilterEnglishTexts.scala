package com.grammarly.workshop

import org.apache.spark.sql.{Dataset, SaveMode, SparkSession}
import com.martinkl.warc._
import com.martinkl.warc.mapreduce._
import com.optimaize.langdetect.LanguageDetectorBuilder
import com.optimaize.langdetect.ngram.NgramExtractors
import com.optimaize.langdetect.profiles.LanguageProfileReader
import com.optimaize.langdetect.text.CommonTextObjectFactories
import org.apache.hadoop.io._
import org.apache.spark.rdd.RDD

object FilterEnglishTexts {

  val MinContentLength = 1000
  val Prefix = "s3://commoncrawl/"

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().getOrCreate()
    val wetPaths = spark.read.textFile("s3://commoncrawl/crawl-data/CC-MAIN-2018-09/wet.paths.gz").take(10).map(Prefix + _)
    val wetsRDD = spark.sparkContext.newAPIHadoopFile[LongWritable, WARCWritable, WARCInputFormat](wetPaths.mkString(","))
    val recordsDS = filter(spark, wetsRDD)
    recordsDS
      .coalesce(10)
      .write
      .mode(SaveMode.Overwrite)
      .parquet("s3://workshop-lviv/data/ds1/")

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
          !ps.isEmpty && ps.get(0).getLocale.getLanguage == "en"
        }

        it.flatMap { case (_, r) =>
          val content = new String(r.getRecord().getContent(), "UTF-8")
          if (content.length >= MinContentLength && isEnglish(content)) {
            Some(Record(uri = r.getRecord().getHeader().getTargetURI(), content))
          } else {
            None
          }
        }
      }
      .toDS()
  }

}

case class Record(uri: String, content: String)
