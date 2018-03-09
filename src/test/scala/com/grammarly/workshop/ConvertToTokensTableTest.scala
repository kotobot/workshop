package com.grammarly.workshop

import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, FunSpec}

class ConvertToTokensTableTest extends FunSpec with BeforeAndAfterAll {

  val spark = SparkSession.builder().master("local[4]").getOrCreate()

  describe("ConvertToTokensTable") {
    it("should convert records to token table rows") {
      import spark.implicits._

      val input = Seq(
        Record("https://www.wikiwand.com/en/USS_Lexington_(CV-2)", "USS Lexington (CV-2), nicknamed \"Lady Lex\",[1] was an early aircraft carrier built for the United States Navy. She was the lead ship of the Lexington class; her only sister ship, Saratoga, was commissioned a month earlier. Originally designed as a battlecruiser, she was converted into one of the Navy's first aircraft carriers during construction to comply with the terms of the Washington Naval Treaty of 1922, which essentially terminated all new battleship and battlecruiser construction. The ship entered service in 1928 and was assigned to the Pacific Fleet for her entire career"),
        Record("https://www.wikiwand.com/en/Aircraft_catapult", "An aircraft catapult is a device used to launch aircraft from ships, most commonly used on aircraft carriers, as a form of assisted take off. It consists of a track built into the flight deck, below which is a large piston or shuttle that is attached through the track to the nose gear of the aircraft, or in some cases a wire rope, called a catapult bridle, is attached to the aircraft and the catapult shuttle. Different means have been used to propel the catapult, such as weight and derrick, gunpowder, flywheel, air pressure, hydraulic, and steam power.")
      ).toDS()

      val output = ConvertToTokensTable.convert(spark, input)

      output.show()
    }
   }

  override protected def afterAll(): Unit = {
    spark.close()
  }
}
