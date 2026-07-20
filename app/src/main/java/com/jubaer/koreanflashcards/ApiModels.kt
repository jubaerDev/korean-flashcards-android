package com.jubaer.koreanflashcards

data class VocabWord(
    val korean_word: String,
    val bangla_meaning: String,
    val chapter_number: Int
)

data class ProgressRow(
    val korean_word: String,
    val box_level: Int,
    val next_review_date: String,
    val times_reviewed: Int,
    val times_correct: Int
)

data class ProgressUpsert(
    val korean_word: String,
    val chapter_number: Int,
    val box_level: Int,
    val next_review_date: String,
    val last_reviewed: String,
    val times_reviewed: Int,
    val times_correct: Int
)

// UI তে ব্যবহারের জন্য (vocab_words ও flashcard_progress এর তথ্য একসাথে করা)
data class FlashcardItem(
    val korean: String,
    val bangla: String,
    val chapter: Int,
    val boxLevel: Int,
    val timesReviewed: Int,
    val timesCorrect: Int
)

// ---------- Upload Chapter এর জন্য ----------

data class RawWordRow(
    val chapter_number: Int,
    val korean_word: String,
    val bangla_meaning: String?,
    val id: Long
)

data class RawWordInsert(
    val chapter_number: Int,
    val korean_word: String,
    val bangla_meaning: String?
)

data class VocabWordInsert(
    val korean_word: String,
    val bangla_meaning: String?,
    val chapter_number: Int,
    val date_added: String
)

data class ChapterLogInsert(
    val chapter_number: Int,
    val total_words_in_file: Int,
    val unique_new_words: Int,
    val upload_date: String
)

data class IdOnly(val id: Long)
