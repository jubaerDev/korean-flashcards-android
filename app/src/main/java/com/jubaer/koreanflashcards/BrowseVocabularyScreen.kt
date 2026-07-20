package com.jubaer.koreanflashcards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrowseVocabularyScreen(viewModel: BrowseVocabularyViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("📚 Browse Vocabulary", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        if (state.error != null) {
            Text("⚠️ ${state.error}", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.loadChapters() }) { Text("আবার চেষ্টা করো") }
            Spacer(Modifier.height(8.dp))
        }

        if (state.loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (state.chapters.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text("Chapter ${state.selectedChapter ?: ""}")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    state.chapters.forEach { ch ->
                        DropdownMenuItem(text = { Text("Chapter $ch") }, onClick = {
                            viewModel.selectChapter(ch)
                            expanded = false
                        })
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "${state.words.size} টা word",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.words) { word ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(word.korean_word, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                word.bangla_meaning,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
