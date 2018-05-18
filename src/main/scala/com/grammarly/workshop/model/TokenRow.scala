package com.grammarly.workshop.model

case class TokenRow(uri: String, domain: String, token: String, posTag: String, tokenStart: Int, tokenEnd: Int, tokenPos: Int, sentenceStart: Int, sentenceEnd: Int, sentencePos: Int, docLength: Int)
