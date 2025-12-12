package com.example.reading_app.utils

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val sourceLanguageCode: String,
    val targetLanguage: String,
    val targetLanguageCode: String
)

object TranslationManager {
    
    private var currentTranslator: Translator? = null
    private var currentSourceLang: String? = null
    private var currentTargetLang: String? = null
    private val translatorCache = mutableMapOf<String, Translator>()
    
    /**
     * Detect the language of the given text
     * Returns language code (e.g., "en", "es", "fr") or "und" if undetermined
     */
    suspend fun detectLanguage(text: String): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val languageIdentifier = LanguageIdentification.getClient()
            
            languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { languageCode ->
                    if (languageCode != "und") {
                        continuation.resume(languageCode)
                    } else {
                        continuation.resume("und")
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
    }
    
    /**
     * Translate text from source language to target language
     * If sourceLanguage is null, it will be auto-detected
     */
    suspend fun translate(
        text: String,
        targetLanguageCode: String,
        sourceLanguageCode: String? = null
    ): TranslationResult = withContext(Dispatchers.IO) {
        // Detect source language if not provided
        val detectedSourceCode = sourceLanguageCode ?: detectLanguage(text)
        
        if (detectedSourceCode == "und" || detectedSourceCode == targetLanguageCode) {
            return@withContext TranslationResult(
                originalText = text,
                translatedText = text,
                sourceLanguage = getLanguageName(detectedSourceCode),
                sourceLanguageCode = detectedSourceCode,
                targetLanguage = getLanguageName(targetLanguageCode),
                targetLanguageCode = targetLanguageCode
            )
        }
        
        // Check if we can reuse existing translator
        val cacheKey = "${detectedSourceCode}_${targetLanguageCode}"
        val translator = if (currentSourceLang == detectedSourceCode && 
                              currentTargetLang == targetLanguageCode && 
                              currentTranslator != null) {
            // Reuse current translator
            currentTranslator!!
        } else if (translatorCache.containsKey(cacheKey)) {
            // Use cached translator
            translatorCache[cacheKey]!!
        } else {
            // Create new translator
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(detectedSourceCode)
                .setTargetLanguage(targetLanguageCode)
                .build()
            
            val newTranslator = Translation.getClient(options)
            
            // Download model if needed (removed WiFi requirement for faster downloads)
            val conditions = DownloadConditions.Builder()
                .build()
            
            // Ensure model is downloaded
            suspendCancellableCoroutine<Unit> { continuation ->
                newTranslator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        continuation.resume(Unit)
                    }
                    .addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
            }
            
            // Cache the translator
            translatorCache[cacheKey] = newTranslator
            currentTranslator = newTranslator
            currentSourceLang = detectedSourceCode
            currentTargetLang = targetLanguageCode
            
            newTranslator
        }
        
        // Translate
        val translatedText = suspendCancellableCoroutine<String> { continuation ->
            translator.translate(text)
                .addOnSuccessListener { result ->
                    continuation.resume(result)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
        
        TranslationResult(
            originalText = text,
            translatedText = translatedText,
            sourceLanguage = getLanguageName(detectedSourceCode),
            sourceLanguageCode = detectedSourceCode,
            targetLanguage = getLanguageName(targetLanguageCode),
            targetLanguageCode = targetLanguageCode
        )
    }
    
    /**
     * Check if a language model is downloaded
     */
    suspend fun isLanguageModelDownloaded(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val modelManager = RemoteModelManager.getInstance()
            val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(languageCode).build()
            
            modelManager.getDownloadedModels(com.google.mlkit.nl.translate.TranslateRemoteModel::class.java)
                .addOnSuccessListener { models ->
                    val isDownloaded = models.any { it.language == languageCode }
                    continuation.resume(isDownloaded)
                }
                .addOnFailureListener {
                    continuation.resume(false)
                }
        }
    }
    
    /**
     * Download a language model for offline translation
     */
    suspend fun downloadLanguageModel(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val modelManager = RemoteModelManager.getInstance()
            val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(languageCode).build()
            
            val conditions = DownloadConditions.Builder()
                .build()
            
            modelManager.download(model, conditions)
                .addOnSuccessListener {
                    continuation.resume(true)
                }
                .addOnFailureListener {
                    continuation.resume(false)
                }
        }
    }
    
    /**
     * Delete a downloaded language model
     */
    suspend fun deleteLanguageModel(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val modelManager = RemoteModelManager.getInstance()
            val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(languageCode).build()
            
            modelManager.deleteDownloadedModel(model)
                .addOnSuccessListener {
                    continuation.resume(true)
                }
                .addOnFailureListener {
                    continuation.resume(false)
                }
        }
    }
    
    /**
     * Get list of all downloaded language models
     */
    suspend fun getDownloadedLanguages(): List<String> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val modelManager = RemoteModelManager.getInstance()
            
            modelManager.getDownloadedModels(com.google.mlkit.nl.translate.TranslateRemoteModel::class.java)
                .addOnSuccessListener { models ->
                    val languages = models.map { it.language }
                    continuation.resume(languages)
                }
                .addOnFailureListener {
                    continuation.resume(emptyList())
                }
        }
    }
    
    /**
     * Close current translator to free resources
     */
    fun closeTranslator() {
        currentTranslator?.close()
        currentTranslator = null
        currentSourceLang = null
        currentTargetLang = null
    }
    
    /**
     * Close all cached translators
     */
    fun closeAllTranslators() {
        translatorCache.values.forEach { it.close() }
        translatorCache.clear()
        closeTranslator()
    }
    
    /**
     * Get human-readable language name from language code
     */
    fun getLanguageName(languageCode: String): String {
        return when (languageCode) {
            TranslateLanguage.AFRIKAANS -> "Afrikaans"
            TranslateLanguage.ARABIC -> "Arabic"
            TranslateLanguage.BELARUSIAN -> "Belarusian"
            TranslateLanguage.BENGALI -> "Bengali"
            TranslateLanguage.BULGARIAN -> "Bulgarian"
            TranslateLanguage.CATALAN -> "Catalan"
            TranslateLanguage.CHINESE -> "Chinese"
            TranslateLanguage.CZECH -> "Czech"
            TranslateLanguage.DANISH -> "Danish"
            TranslateLanguage.DUTCH -> "Dutch"
            TranslateLanguage.ENGLISH -> "English"
            TranslateLanguage.ESPERANTO -> "Esperanto"
            TranslateLanguage.ESTONIAN -> "Estonian"
            TranslateLanguage.FINNISH -> "Finnish"
            TranslateLanguage.FRENCH -> "French"
            TranslateLanguage.GALICIAN -> "Galician"
            TranslateLanguage.GEORGIAN -> "Georgian"
            TranslateLanguage.GERMAN -> "German"
            TranslateLanguage.GREEK -> "Greek"
            TranslateLanguage.GUJARATI -> "Gujarati"
            TranslateLanguage.HEBREW -> "Hebrew"
            TranslateLanguage.HINDI -> "Hindi"
            TranslateLanguage.HUNGARIAN -> "Hungarian"
            TranslateLanguage.ICELANDIC -> "Icelandic"
            TranslateLanguage.INDONESIAN -> "Indonesian"
            TranslateLanguage.IRISH -> "Irish"
            TranslateLanguage.ITALIAN -> "Italian"
            TranslateLanguage.JAPANESE -> "Japanese"
            TranslateLanguage.KANNADA -> "Kannada"
            TranslateLanguage.KOREAN -> "Korean"
            TranslateLanguage.LATVIAN -> "Latvian"
            TranslateLanguage.LITHUANIAN -> "Lithuanian"
            TranslateLanguage.MACEDONIAN -> "Macedonian"
            TranslateLanguage.MARATHI -> "Marathi"
            TranslateLanguage.NORWEGIAN -> "Norwegian"
            TranslateLanguage.PERSIAN -> "Persian"
            TranslateLanguage.POLISH -> "Polish"
            TranslateLanguage.PORTUGUESE -> "Portuguese"
            TranslateLanguage.ROMANIAN -> "Romanian"
            TranslateLanguage.RUSSIAN -> "Russian"
            TranslateLanguage.SLOVAK -> "Slovak"
            TranslateLanguage.SLOVENIAN -> "Slovenian"
            TranslateLanguage.SPANISH -> "Spanish"
            TranslateLanguage.SWEDISH -> "Swedish"
            TranslateLanguage.TAGALOG -> "Tagalog"
            TranslateLanguage.TAMIL -> "Tamil"
            TranslateLanguage.TELUGU -> "Telugu"
            TranslateLanguage.THAI -> "Thai"
            TranslateLanguage.TURKISH -> "Turkish"
            TranslateLanguage.UKRAINIAN -> "Ukrainian"
            TranslateLanguage.URDU -> "Urdu"
            TranslateLanguage.VIETNAMESE -> "Vietnamese"
            TranslateLanguage.WELSH -> "Welsh"
            "und" -> "Unknown"
            else -> languageCode.uppercase()
        }
    }
    
    /**
     * Get list of all supported languages for translation
     */
    fun getSupportedLanguages(): List<Pair<String, String>> {
        return listOf(
            TranslateLanguage.ENGLISH to "English",
            TranslateLanguage.SPANISH to "Spanish",
            TranslateLanguage.FRENCH to "French",
            TranslateLanguage.GERMAN to "German",
            TranslateLanguage.ITALIAN to "Italian",
            TranslateLanguage.PORTUGUESE to "Portuguese",
            TranslateLanguage.RUSSIAN to "Russian",
            TranslateLanguage.CHINESE to "Chinese",
            TranslateLanguage.JAPANESE to "Japanese",
            TranslateLanguage.KOREAN to "Korean",
            TranslateLanguage.ARABIC to "Arabic",
            TranslateLanguage.HINDI to "Hindi",
            TranslateLanguage.BENGALI to "Bengali",
            TranslateLanguage.DUTCH to "Dutch",
            TranslateLanguage.TURKISH to "Turkish",
            TranslateLanguage.VIETNAMESE to "Vietnamese",
            TranslateLanguage.THAI to "Thai",
            TranslateLanguage.INDONESIAN to "Indonesian",
            TranslateLanguage.POLISH to "Polish",
            TranslateLanguage.UKRAINIAN to "Ukrainian",
            TranslateLanguage.GREEK to "Greek",
            TranslateLanguage.CZECH to "Czech",
            TranslateLanguage.SWEDISH to "Swedish",
            TranslateLanguage.DANISH to "Danish",
            TranslateLanguage.FINNISH to "Finnish",
            TranslateLanguage.NORWEGIAN to "Norwegian",
            TranslateLanguage.HUNGARIAN to "Hungarian",
            TranslateLanguage.HEBREW to "Hebrew",
            TranslateLanguage.ROMANIAN to "Romanian",
            TranslateLanguage.PERSIAN to "Persian",
            TranslateLanguage.TAGALOG to "Tagalog",
            TranslateLanguage.TAMIL to "Tamil",
            TranslateLanguage.TELUGU to "Telugu",
            TranslateLanguage.GUJARATI to "Gujarati",
            TranslateLanguage.KANNADA to "Kannada",
            TranslateLanguage.MARATHI to "Marathi",
            TranslateLanguage.URDU to "Urdu"
        ).sortedBy { it.second }
    }
}
