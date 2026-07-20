package com.jubaer.koreanflashcards

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApi {

    @GET("rest/v1/vocab_words")
    suspend fun getVocabWords(
        @Query("select") select: String = "korean_word,bangla_meaning,chapter_number",
        @Header("Range") range: String
    ): List<VocabWord>

    @GET("rest/v1/vocab_words")
    suspend fun getVocabWordsByChapter(
        @Query("chapter_number") chapterFilter: String,
        @Query("select") select: String = "korean_word,bangla_meaning,chapter_number"
    ): List<VocabWord>

    @GET("rest/v1/flashcard_progress")
    suspend fun getProgress(
        @Query("select") select: String = "korean_word,box_level,next_review_date,times_reviewed,times_correct",
        @Header("Range") range: String
    ): List<ProgressRow>

    @Headers("Content-Type: application/json", "Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/flashcard_progress?on_conflict=korean_word")
    suspend fun upsertProgress(@Body body: List<ProgressUpsert>): Response<Unit>

    // ---------- Upload Chapter / Rebuild ----------

    @GET("rest/v1/raw_chapter_words")
    suspend fun getRawChapterWords(
        @Query("select") select: String = "chapter_number,korean_word,bangla_meaning,id",
        @Query("order") order: String = "chapter_number.asc,id.asc",
        @Header("Range") range: String
    ): List<RawWordRow>

    @GET("rest/v1/raw_chapter_words")
    suspend fun checkChapterRawExists(
        @Query("chapter_number") chapterFilter: String,
        @Query("select") select: String = "id",
        @Query("limit") limit: Int = 1
    ): List<IdOnly>

    @Headers("Content-Type: application/json", "Prefer: return=minimal")
    @POST("rest/v1/raw_chapter_words")
    suspend fun insertRawWords(@Body body: List<RawWordInsert>): Response<Unit>

    @DELETE("rest/v1/raw_chapter_words")
    suspend fun deleteRawWordsForChapter(@Query("chapter_number") chapterFilter: String): Response<Unit>

    @DELETE("rest/v1/vocab_words")
    suspend fun deleteAllVocabWords(@Query("id") idFilter: String = "gte.0"): Response<Unit>

    @Headers("Content-Type: application/json", "Prefer: return=minimal")
    @POST("rest/v1/vocab_words")
    suspend fun insertVocabWords(@Body body: List<VocabWordInsert>): Response<Unit>

    @DELETE("rest/v1/chapters_log")
    suspend fun deleteAllChaptersLog(@Query("chapter_number") chapterFilter: String = "gte.0"): Response<Unit>

    @Headers("Content-Type: application/json", "Prefer: return=minimal")
    @POST("rest/v1/chapters_log")
    suspend fun insertChaptersLog(@Body body: List<ChapterLogInsert>): Response<Unit>
}
