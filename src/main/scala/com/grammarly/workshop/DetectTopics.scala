package com.grammarly.workshop

import com.grammarly.workshop.model.{AnnotatedRecord, Record}
import com.johnsnowlabs.nlp.annotator.{LemmatizerModel, PerceptronModel}
import com.johnsnowlabs.nlp.annotators.Tokenizer
import com.johnsnowlabs.nlp.annotators.sbd.pragmatic.SentenceDetector
import com.johnsnowlabs.nlp.{DocumentAssembler, Finisher}
import org.apache.spark.ml.clustering.{LDA, LDAModel}
import org.apache.spark.ml.feature.{CountVectorizer, CountVectorizerModel, StopWordsRemover}
import org.apache.spark.ml.{Pipeline, PipelineModel, PipelineStage}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.slf4j.LoggerFactory

object DetectTopics {

  val logger = LoggerFactory.getLogger(getClass)

  val word = "[A-Za-z]+".r.pattern

  def main(args: Array[String]): Unit = {
    val inputPath = args(0)
    val outputPath = args(1)

    val spark = SparkSession.builder().getOrCreate()

    import spark.implicits._
    val records = spark.read.parquet(inputPath).as[Record]

    val result = detect(records, posTag = "NN", vocabSize = 50000, numOfTopics = 1000)

    result.write.parquet(outputPath)
  }

  def detect(ds: Dataset[Record], posTag: String, vocabSize: Int, numOfTopics: Int, numOfIterations: Int = 100,
             optimizer: String = "em"): DataFrame = {

    val documentAssembler = new DocumentAssembler().setInputCol("content").setOutputCol("document")

    val sentenceDetector = new SentenceDetector()
      .setInputCols(Array("document"))
      .setOutputCol("sentence")

    val regexTokenizer = new Tokenizer()
      .setInputCols(Array("sentence"))
      .setOutputCol("token")

    // Lemmatizer which uses default lemma dictionary
    val lemmatizer = LemmatizerModel
      .pretrained(language = Some("en"))
      .setInputCols("token")
      .setOutputCol("lemma")

    val posTagger = PerceptronModel
      .pretrained(language = Some("en"))
      .setInputCols("sentence", "token")
      .setOutputCol("pos")

    val finisher = new Finisher()
      .setInputCols("token")
      .setCleanAnnotations(false)

    val posPipeline = new Pipeline()
      .setStages(Array(
        documentAssembler,
        sentenceDetector,
        regexTokenizer,
        lemmatizer,
        posTagger,
        finisher
      ))

    val posDS =
      posPipeline
        .fit(ds)
        .transform(ds)

    import ds.sqlContext.implicits._

    val posTagDs = posDS
      .as[AnnotatedRecord]
      .map(filterPosTag(posTag))
      .toDF("uri", "domain", "lemma")

    StopWordsRemover.loadDefaultStopWords("english")
    val stopWordsRemover = new StopWordsRemover()
      .setCaseSensitive(false)
      .setInputCol("lemma")
      .setOutputCol("cleanLemma")

    val countVectorizer = new CountVectorizer()
      .setVocabSize(vocabSize)
      .setInputCol("cleanLemma")
      .setOutputCol("features")

    val lda = new LDA()
      .setFeaturesCol("features")
      .setOptimizer(optimizer)
      .setK(numOfTopics)
      .setMaxIter(numOfIterations)

    val pipeline = new Pipeline()
        .setStages(Array(
            stopWordsRemover,
            countVectorizer,
            lda
          )
        )

    val model = pipeline.fit(posTagDs)
    val topics = model.transform(posTagDs)

    val countVectorizerModel: CountVectorizerModel = getStage(model, 1)
    val ldaModel: LDAModel = getStage(model, 2)

    val ll = ldaModel.logLikelihood(topics)
    val lp = ldaModel.logPerplexity(topics)
    logger.info(s"The lower bound on the log likelihood of the entire corpus: $ll")
    logger.info(s"The upper bound on perplexity: $lp")

    val vocabulary = countVectorizerModel.vocabulary

    ldaModel.describeTopics().as[TopicInfo]
      .map(mapTopic(vocabulary))
      .toDF("topic", "description")
      .show(100, false)

    topics
  }

  private def filterPosTag(posTag: String)(ar: AnnotatedRecord) = {
    val lemma = ar.lemma.map(_.result)
    val posTags = ar.pos.map(_.result)
    val filteredLemma = lemma.zip(posTags).collect {
      case (l, p) if p == posTag && word.matcher(l).matches() => l
    }
    (ar.uri, ar.domain, filteredLemma)
  }

  private def mapTopic(vocabulary: Array[String])(t: TopicInfo) = {
    t -> t.termIndices.zip(t.termWeights).map { case (i, w) => vocabulary(i) -> w }
  }

  private def getStage[T <: PipelineStage](pipeline: PipelineModel, ind: Int): T = {
    pipeline.stages(ind).asInstanceOf[T]
  }

}

case class TopicInfo(topic: Int, termIndices: Array[Int], termWeights: Array[Double])