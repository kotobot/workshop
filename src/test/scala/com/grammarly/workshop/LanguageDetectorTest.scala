package com.grammarly.workshop

import com.optimaize.langdetect.LanguageDetectorBuilder
import com.optimaize.langdetect.ngram.NgramExtractors
import com.optimaize.langdetect.profiles.LanguageProfileReader
import com.optimaize.langdetect.text.CommonTextObjectFactories
import org.scalatest.FunSpec

import scala.collection.JavaConverters._

class LanguageDetectorTest extends FunSpec {

  val languageProfiles = new LanguageProfileReader().readAllBuiltIn()
  val languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard).withProfiles(languageProfiles).build
  val textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText

  describe("Language detactor") {
    it("should recognize English language") {
      assertResult("en")(detectLang("my text"))
      assertResult("uk")(detectLang("добрий день"))
    }
  }

  def detectLang(text: String) = {
    val textObject = textObjectFactory.forText(text)
    languageDetector.getProbabilities(textObject).asScala.head.getLocale.getLanguage
  }

}
