package com.grammarly.workshop.model

import com.johnsnowlabs.nlp.Annotation

case class AnnotatedRecord(uri: String, domain: String, content: String,
                           document: Array[Annotation],
                           sentence: Array[Annotation],
                           token: Array[Annotation],
                           pos: Array[Annotation],
                           lemma: Array[Annotation])
