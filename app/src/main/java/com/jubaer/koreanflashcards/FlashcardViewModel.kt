package com.jubaer.koreanflashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    data class Setup(
        val chapters: List<Int> = emptyList(),
        val selectedChapter: Int? = null,
        val totalTracked: Int = 0,
        val mastered: Int = 0,
        val loading: Boolean = false,
        val error: String? = null
    ) : UiState()

    data class Practicing(
        val queue: List<FlashcardItem>,
        val index: Int,
        val showAnswer: Boolean,
        val correctCount: Int,
        val wrongCount: Int
    ) : UiState()

    data class Finished(val correctCount: Int, val wrongCount: Int) : UiState()
}

class FlashcardViewModel(private val repo: FlashcardRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Setup())
    val uiState: StateFlow<UiState> = _uiState

    init {
        loadSetupData()
    }

    fun loadSetupData() {
        viewModelScope.launch {
            _uiState.value = (_uiState.value as? UiState.Setup ?: UiState.Setup()).copy(loading = true, error = null)
            try {
                val chapters = repo.getAllChapters()
                val (total, mastered) = repo.getStats()
                _uiState.value = UiState.Setup(chapters, null, total, mastered, loading = false)
            } catch (e: Exception) {
                _uiState.value = UiState.Setup(loading = false, error = e.message ?: "Unknown error")
            }
        }
    }

    fun selectChapter(chapter: Int?) {
        val current = _uiState.value as? UiState.Setup ?: return
        _uiState.value = current.copy(selectedChapter = chapter)
    }

    fun startSession() {
        val current = _uiState.value as? UiState.Setup ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(loading = true)
            try {
                val due = repo.getDueCards(current.selectedChapter)
                _uiState.value = if (due.isEmpty()) {
                    UiState.Finished(0, 0)
                } else {
                    UiState.Practicing(due, 0, false, 0, 0)
                }
            } catch (e: Exception) {
                _uiState.value = current.copy(loading = false, error = e.message ?: "Unknown error")
            }
        }
    }

    fun revealAnswer() {
        val current = _uiState.value as? UiState.Practicing ?: return
        _uiState.value = current.copy(showAnswer = true)
    }

    fun answer(correct: Boolean) {
        val current = _uiState.value as? UiState.Practicing ?: return
        val card = current.queue[current.index]
        viewModelScope.launch {
            try {
                repo.updateProgress(card, correct)
            } catch (_: Exception) {
                // network সমস্যা হলেও local ভাবে পরের card এ চলে যাবে, পরের session এ আবার sync হবে
            }
            val newIndex = current.index + 1
            val newCorrect = current.correctCount + if (correct) 1 else 0
            val newWrong = current.wrongCount + if (!correct) 1 else 0
            _uiState.value = if (newIndex >= current.queue.size) {
                UiState.Finished(newCorrect, newWrong)
            } else {
                current.copy(index = newIndex, showAnswer = false, correctCount = newCorrect, wrongCount = newWrong)
            }
        }
    }

    fun backToSetup() {
        loadSetupData()
    }
}

class FlashcardViewModelFactory(private val repo: FlashcardRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return FlashcardViewModel(repo) as T
    }
}
