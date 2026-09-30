package com.example.disasternet.model

data class AiClassification(
    val category: String,
    val urgency: String,
    val confidence: Int,
    val priority: Int,
    val modelVersion: String = "DN-Softmax-1.0",
    val classProbabilities: Map<String, Float> = emptyMap(),
    val featureScores: Map<String, Float> = emptyMap()
)
