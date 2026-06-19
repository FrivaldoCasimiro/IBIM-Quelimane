package com.example.utils

data class VerseRef(
    val fullText: String,
    val book: String,
    val chapter: Int,
    val verse: Int
)

object RegexHelper {
    // Regex matches e.g. "João 3:16", "Efésios 2:8", "1 Coríntios 13:1"
    private val regex = """(\d?\s*[A-Za-zÀ-ÖØ-öø-ÿ]+(?:\s+[A-Za-zÀ-ÖØ-öø-ÿ]+)?)\s*(\d+):(\d+)""".toRegex()

    fun extractVerses(text: String): List<VerseRef> {
        val matches = regex.findAll(text)
        return matches.map { match ->
            val fullText = match.value
            val book = match.groupValues[1].trim()
            val chapter = match.groupValues[2].toIntOrNull() ?: 1
            val verse = match.groupValues[3].toIntOrNull() ?: 1
            VerseRef(fullText, book, chapter, verse)
        }.distinctBy { it.fullText }.toList()
    }
}
