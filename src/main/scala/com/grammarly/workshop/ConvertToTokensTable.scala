package com.grammarly.workshop

import com.grammarly.workshop.model.{AnnotatedRecord, Record, TokenRow}
import org.apache.spark.sql.{Dataset, SaveMode, SparkSession}
import com.johnsnowlabs.nlp._
import com.johnsnowlabs.nlp.annotators._
import com.johnsnowlabs.nlp.annotators.sbd.pragmatic.SentenceDetector
import com.johnsnowlabs.nlp.annotators.pos.perceptron.PerceptronApproach
import org.apache.spark.ml.Pipeline

object ConvertToTokensTable {

  def main(args: Array[String]): Unit = {

    val input = args(0) //s3://workshop-lviv/data/ds1/
    val output = args(1) //s3://workshop-lviv/data/ds3/

    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val ds = spark.read.parquet(input).as[Record]
    val tokens = convert(spark, ds)

    tokens
      .coalesce(50)
      .write
      .mode(SaveMode.Overwrite)
      .parquet(output)
  }

  def convert(spark: SparkSession, ds: Dataset[Record]): Dataset[TokenRow] = {
    import spark.implicits._

    val documentAssembler = new DocumentAssembler().setInputCol("content").setOutputCol("document")

    val sentenceDetector = new SentenceDetector()
      .setInputCols(Array("document"))
      .setOutputCol("sentence")

    val regexTokenizer = new Tokenizer()
      .setInputCols("sentence")
      .setOutputCol("token")

    val posTagger = new PerceptronApproach()
      .setInputCols("sentence", "token")
      .setOutputCol("pos")

    val finisher = new Finisher()
      .setInputCols("token")
      .setCleanAnnotations(false)

    val pipeline = new Pipeline()
      .setStages(Array(
        documentAssembler,
        sentenceDetector,
        regexTokenizer,
        posTagger,
        finisher
      ))

    val res =
      pipeline
        .fit(ds)
        .transform(ds)

    res
      .as[AnnotatedRecord]
      .flatMap { ar =>
        val docLength = ar.document.head.end
        val sentences = ar.sentence.zipWithIndex.map { case (sa, ind) =>
          (sa.begin, sa.end, ind)
        }

        ar.pos.zipWithIndex.map { case (pa, ind) =>
          val (sentStart, sentEnd, sentPos) = sentences.collectFirst {
            case sent@(s, e, i) if pa.begin >= s && pa.end <= e => sent
          } getOrElse (-1, -1, -1)

          TokenRow(
            uri = ar.uri,
            domain = ar.domain,
            token = pa.metadata("word"),
            posTag = pa.result,
            tokenStart = pa.begin,
            tokenEnd = pa.end,
            tokenPos = ind,
            sentenceStart = sentStart,
            sentenceEnd = sentEnd,
            sentencePos = sentPos,
            docLength = docLength
          )
        }
      }
  }

}



