package com.example.booktranslator.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.nl.languageid.LanguageIdentification

fun extractTextFromImage(
    context: Context,
    imageUri: Uri,
    onResult: (String) -> Unit
) {
    try {
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                onResult(visionText.text)
            }
            .addOnFailureListener { e ->
                onResult("Błąd OCR: ${e.message}")
            }
    } catch (e: Exception) {
        onResult("Błąd przygotowania obrazu: ${e.message}")
    }
}

fun translateText(
    text: String,
    targetLangCode: String, // "pl", "en"
    onResult: (String) -> Unit
) {
    val languageIdentifier = LanguageIdentification.getClient()
    languageIdentifier.identifyLanguage(text)
        .addOnSuccessListener { langCode ->
            if (langCode == "und") {
                onResult("Nie udało się wykryć języka źródłowego")
                return@addOnSuccessListener
            }

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(langCode) // wykryty język
                .setTargetLanguage(targetLangCode)
                .build()

            val translator = Translation.getClient(options)
            translator.downloadModelIfNeeded()
                .addOnSuccessListener {
                    translator.translate(text)
                        .addOnSuccessListener { translated -> onResult(translated) }
                        .addOnFailureListener { e -> onResult("Błąd tłumaczenia: ${e.message}") }
                }
                .addOnFailureListener { e ->
                    onResult("Błąd pobierania modelu: ${e.message}")
                }
        }
        .addOnFailureListener { e ->
            onResult("Błąd wykrywania języka: ${e.message}")
        }
}