package com.jubaer.koreanflashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class BrowseUiState(
    val chapters: List<Int> = emptyList(),
    val selectedChapter: Int? = null,
    val words: List<VocabWordEntity> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class BrowseVocabularyViewModel(private val repo: FlashcardRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState

    private var allWords: List<VocabWordEntity> = emptyList()

    init {
        loadChapters()
    }

    fun loadChapters() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                allWords = repo.getAllVocabWords()
                val chapters = allWords.map { it.chapter_number }.distinct().sorted()
                val firstChapter = chapters.firstOrNull()
                _uiState.value = BrowseUiState(
                    chapters = chapters,
                    selectedChapter = firstChapter,
                    words = if (firstChapter != null) allWords.filter { it.chapter_number == firstChapter } else emptyList(),
                    loading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message ?: "Unknown error")
            }
        }
    }

    fun selectChapter(chapter: Int) {
        _uiState.value = _uiState.value.copy(
            selectedChapter = chapter,
            words = allWords.filter { it.chapter_number == chapter }
        )
    }
}

class BrowseVocabularyViewModelFactory(private val repo: FlashcardRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BrowseVocabularyViewModel(repo) as T
    }
}
