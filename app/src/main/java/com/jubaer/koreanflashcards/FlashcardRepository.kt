package com.jubaer.koreanflashcards

/**
 * এখন থেকে সব READ (Flashcard due-card, Vocabulary browse, stats) হয় local
 * Room database থেকে — তাই তাৎক্ষণিক (internet lag নেই)। WRITE (progress
 * update, chapter upload) সরাসরি Supabase এ যায় (source of truth), তারপর
 * local Room ও আপডেট হয়ে যায় যাতে UI সাথে সাথে ঠিক দেখায়।
 *
 * syncFromServer() — app চালু হওয়ার সময় (বা manual "Sync" চাপলে) পুরো
 * Supabase data নতুন করে download করে Room এ replace করে।
 */
class FlashcardRepository(private val api: SupabaseApi, private val db: AppDatabase) {

    companion object {
        val LEITNER_INTERVALS = mapOf(1 to 0, 2 to 1, 3 to 3, 4 to 7, 5 to 14, 6 to 30)
        const val MAX_BOX = 6
    }

    private suspend fun <T> fetchAllPaged(pageSize: Int = 1000, fetchPage: suspend (String) -> List<T>): List<T> {
        val all = mutableListOf<T>()
        var start = 0
        while (true) {
            val range = "$start-${start + pageSize - 1}"
            val batch = fetchPage(range)
            all.addAll(batch)
            if (batch.size < pageSize) break
            start += pageSize
        }
        return all
    }

    // ---------- Sync: Supabase → Room (local cache) ----------

    suspend fun syncFromServer() {
        val vocab = fetchAllPaged { range -> api.getVocabWords(range = range) }
        val progress = fetchAllPaged { range -> api.getProgress(range = range) }

        db.vocabDao().clearAll()
        db.vocabDao().insertAll(
            vocab.map { VocabWordEntity(it.korean_word, it.bangla_meaning, it.chapter_number) }
        )

        db.progressDao().clearAll()
        db.progressDao().insertAll(
            progress.map {
                ProgressEntity(it.korean_word, it.box_level, it.next_review_date, it.times_reviewed, it.times_correct)
            }
        )
    }

    // ---------- READ: এখন সব Room থেকে (দ্রুত) ----------

    suspend fun getAllChapters(): List<Int> = db.vocabDao().getChapters()

    suspend fun getStats(): Pair<Int, Int> {
        val progress = db.progressDao().getAll()
        val total = progress.size
        val mastered = progress.count { it.box_level >= MAX_BOX }
        return Pair(total, mastered)
    }

    suspend fun getDueCards(chapterFilter: Int?): List<FlashcardItem> {
        val vocabAll = db.vocabDao().getAll()
        val vocab = if (chapterFilter != null) vocabAll.filter { it.chapter_number == chapterFilter } else vocabAll

        val progressMap = db.progressDao().getAll().associateBy { it.korean_word }
        val today = DateUtils.today()

        val due = vocab.mapNotNull { w ->
            val p = progressMap[w.korean_word]
            when {
                p == null -> FlashcardItem(w.korean_word, w.bangla_meaning, w.chapter_number, 1, 0, 0)
                p.next_review_date <= today -> FlashcardItem(
                    w.korean_word, w.bangla_meaning, w.chapter_number,
                    p.box_level, p.times_reviewed, p.times_correct
                )
                else -> null
            }
        }
        return due.shuffled()
    }

    suspend fun getAllVocabWords(): List<VocabWordEntity> = db.vocabDao().getAll()

    // ---------- WRITE: সরাসরি Supabase এ, তারপর Room ও আপডেট ----------

    suspend fun updateProgress(item: FlashcardItem, correct: Boolean) {
        val newBox = if (correct) minOf(item.boxLevel + 1, MAX_BOX) else 1
        val nextReview = DateUtils.plusDays(LEITNER_INTERVALS[newBox] ?: 0)
        val newTimesReviewed = item.timesReviewed + 1
        val newTimesCorrect = item.timesCorrect + if (correct) 1 else 0

        val body = listOf(
            ProgressUpsert(
                korean_word = item.korean,
                chapter_number = item.chapter,
                box_level = newBox,
                next_review_date = nextReview,
                last_reviewed = DateUtils.nowIso(),
                times_reviewed = newTimesReviewed,
                times_correct = newTimesCorrect
            )
        )
        api.upsertProgress(body)

        // Local Room ও সাথে সাথে আপডেট (পরের বার fetch এ instant দেখাবে)
        db.progressDao().insertAll(
            listOf(ProgressEntity(item.korean, newBox, nextReview, newTimesReviewed, newTimesCorrect))
        )
    }

    // ---------- Upload Chapter / Rebuild (সরাসরি Supabase, এরপর syncFromServer() কল করতে হবে) ----------

    suspend fun chapterExists(chapterNumber: Int): Boolean {
        val rows = api.checkChapterRawExists(chapterFilter = "eq.$chapterNumber")
        return rows.isNotEmpty()
    }

    suspend fun saveRawChapter(chapterNumber: Int, pairs: List<Pair<String, String>>, overwrite: Boolean) {
        if (overwrite) {
            api.deleteRawWordsForChapter(chapterFilter = "eq.$chapterNumber")
        }
        val cleaned = pairs.mapNotNull { (k, b) ->
            val kt = k.trim()
            if (kt.isEmpty() || kt.equals("nan", ignoreCase = true)) null
            else RawWordInsert(chapterNumber, kt, b.trim())
        }
        if (cleaned.isNotEmpty()) {
            cleaned.chunked(500).forEach { chunk -> api.insertRawWords(chunk) }
        }
    }

    suspend fun getWordsForChapterFinal(chapterNumber: Int): List<VocabWord> {
        return api.getVocabWordsByChapter(chapterFilter = "eq.$chapterNumber")
    }

    suspend fun rebuildDatabase() {
        val raw = fetchAllPaged { range -> api.getRawChapterWords(range = range) }
        val byChapter = raw.groupBy { it.chapter_number }.toSortedMap()

        val seenWords = mutableSetOf<String>()
        val vocabPayload = mutableListOf<VocabWordInsert>()
        val logPayload = mutableListOf<ChapterLogInsert>()
        val now = DateUtils.nowIso()

        for ((chapterNumber, rowsUnsorted) in byChapter) {
            val rows = rowsUnsorted.sortedBy { it.id }
            val totalInFile = rows.size

            val localSeen = mutableSetOf<String>()
            val newThisChapter = mutableListOf<RawWordRow>()
            for (r in rows) {
                val k = r.korean_word.trim()
                if (k.isEmpty() || localSeen.contains(k)) continue
                localSeen.add(k)
                if (seenWords.contains(k)) continue
                seenWords.add(k)
                newThisChapter.add(r)
            }

            for (r in newThisChapter) {
                vocabPayload.add(VocabWordInsert(r.korean_word.trim(), r.bangla_meaning, chapterNumber, now))
            }
            logPayload.add(ChapterLogInsert(chapterNumber, totalInFile, newThisChapter.size, now))
        }

        api.deleteAllVocabWords()
        api.deleteAllChaptersLog()

        vocabPayload.chunked(500).forEach { chunk -> api.insertVocabWords(chunk) }
        if (logPayload.isNotEmpty()) {
            logPayload.chunked(500).forEach { chunk -> api.insertChaptersLog(chunk) }
        }
    }
}
