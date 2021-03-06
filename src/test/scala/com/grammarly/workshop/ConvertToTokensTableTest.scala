package com.grammarly.workshop

import com.grammarly.workshop.model.{Record, TokenRow}
import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

class ConvertToTokensTableTest extends FunSpec with Matchers with BeforeAndAfterAll {

  val spark = SparkSession.builder().master("local[4]").getOrCreate()

  // It slow (about 5 minutes) because spark-nlp trains model for pos tagging
  describe("ConvertToTokensTable") {
    it("should convert records to token table rows") {
      import spark.implicits._

      val text1 = "USS Lexington (CV-2), nicknamed \"Lady Lex\",[1] was an early aircraft carrier built for the United States Navy. She was the lead ship of the Lexington class; her only sister ship, Saratoga, was commissioned a month earlier. Originally designed as a battlecruiser, she was converted into one of the Navy's first aircraft carriers during construction to comply with the terms of the Washington Naval Treaty of 1922, which essentially terminated all new battleship and battlecruiser construction. The ship entered service in 1928 and was assigned to the Pacific Fleet for her entire career"
      val input = Seq(
        Record("https://www.wikiwand.com/en/USS_Lexington_(CV-2)", "www.wikiwand.com", text1),
        Record("https://www.wikiwand.com/en/Aircraft_catapult", "www.wikiwand.com", "An aircraft catapult is a device used to launch aircraft from ships, most commonly used on aircraft carriers, as a form of assisted take off. It consists of a track built into the flight deck, below which is a large piston or shuttle that is attached through the track to the nose gear of the aircraft, or in some cases a wire rope, called a catapult bridle, is attached to the aircraft and the catapult shuttle. Different means have been used to propel the catapult, such as weight and derrick, gunpowder, flywheel, air pressure, hydraulic, and steam power.")
      ).toDS()

      val output = ConvertToTokensTable.convert(spark, input)

      val expected0 = TokenRow("https://www.wikiwand.com/en/USS_Lexington_(CV-2)", "www.wikiwand.com", "USS", "NNP", 0, 2, 0, 0, 109, 0, text1.length - 1)
      output.where($"tokenPos" === 0).head() should equal(expected0)
    }
  }

  override protected def afterAll(): Unit = {
    spark.close()
  }
}
