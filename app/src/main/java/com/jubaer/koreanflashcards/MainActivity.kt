package com.jubaer.koreanflashcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var db: AppDatabase
    private lateinit var repo: FlashcardRepository
    private val flashcardViewModel: FlashcardViewModel by viewModels { FlashcardViewModelFactory(repo) }
    private val browseViewModel: BrowseVocabularyViewModel by viewModels { BrowseVocabularyViewModelFactory(repo) }
    private val uploadViewModel: UploadChapterViewModel by viewModels { UploadChapterViewModelFactory(repo) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getInstance(applicationContext)
        repo = FlashcardRepository(ApiClient.api, db)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(repo, flashcardViewModel, browseViewModel, uploadViewModel)
                }
            }
        }
    }
}

@Composable
fun AppRoot(
    repo: FlashcardRepository,
    flashcardViewModel: FlashcardViewModel,
    browseViewModel: BrowseVocabularyViewModel,
    uploadViewModel: UploadChapterViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun runSync() {
        scope.launch {
            if (!NetworkUtils.isOnline(context)) {
                syncMessage = "⚠️ Internet নেই — পুরনো (local) data দেখানো হচ্ছে"
                return@launch
            }
            syncing = true
            syncMessage = null
            try {
                repo.syncFromServer()
                flashcardViewModel.loadSetupData()
                browseViewModel.loadChapters()
                syncMessage = "✅ Sync সম্পন্ন"
            } catch (e: Exception) {
                syncMessage = "⚠️ Sync ব্যর্থ: ${e.message}"
            } finally {
                syncing = false
            }
        }
    }

    LaunchedEffect(Unit) { runSync() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Korean Flashcards") },
                actions = {
                    if (syncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 8.dp))
                    } else {
                        IconButton(onClick = { runSync() }) { Text("🔄") }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("🎴") },
                    label = { Text("Flashcards") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Text("📚") },
                    label = { Text("Vocabulary") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Text("📤") },
                    label = { Text("Upload") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (syncMessage != null) {
                Text(
                    syncMessage!!,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> FlashcardApp(flashcardViewModel)
                    1 -> BrowseVocabularyScreen(browseViewModel)
                    2 -> UploadChapterScreen(uploadViewModel)
                }
            }
        }
    }
}

@Composable
fun FlashcardApp(viewModel: FlashcardViewModel) {
    val state by viewModel.uiState.collectAsState()

    when (val s = state) {
        is UiState.Setup -> SetupScreen(s, viewModel)
        is UiState.Practicing -> PracticeScreen(s, viewModel)
        is UiState.Finished -> FinishedScreen(s, viewModel)
    }
}

@Composable
fun SetupScreen(state: UiState.Setup, viewModel: FlashcardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("🎴 Flashcard Practice", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        if (state.error != null) {
            Text("⚠️ ${state.error}", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { viewModel.loadSetupData() }) { Text("আবার চেষ্টা করো") }
            Spacer(Modifier.height(24.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("মোট Track", state.totalTracked.toString())
            StatCard("✅ Mastered", state.mastered.toString())
        }

        Spacer(Modifier.height(32.dp))

        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(if (state.selectedChapter == null) "সব Chapter" else "Chapter ${state.selectedChapter}")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("সব Chapter") }, onClick = {
                    viewModel.selectChapter(null); expanded = false
                })
                state.chapters.forEach { ch ->
                    DropdownMenuItem(text = { Text("Chapter $ch") }, onClick = {
                        viewModel.selectChapter(ch); expanded = false
                    })
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { viewModel.startSession() },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("🔄 Session শুরু করো")
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Card(modifier = Modifier.width(140.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 12.sp)
        }
    }
}

@Composable
fun PracticeScreen(state: UiState.Practicing, viewModel: FlashcardViewModel) {
    val card = state.queue[state.index]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "${state.index + 1} / ${state.queue.size}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(card.korean, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Chapter ${card.chapter} | Box ${card.boxLevel}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                if (state.showAnswer) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        card.bangla,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        if (!state.showAnswer) {
            Button(onClick = { viewModel.revealAnswer() }, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("👁️ উত্তর দেখাও")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = { viewModel.answer(false) }) {
                    Text("❌ জানতাম না")
                }
                Button(onClick = { viewModel.answer(true) }) {
                    Text("✅ ঠিক বলেছিলাম")
                }
            }
        }
    }
}

@Composable
fun FinishedScreen(state: UiState.Finished, viewModel: FlashcardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text("আজকের জন্য সব শেষ!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("✅ সঠিক: ${state.correctCount}  |  ❌ ভুল: ${state.wrongCount}")
        Spacer(Modifier.height(32.dp))
        Button(onClick = { viewModel.backToSetup() }) {
            Text("Setup এ ফিরে যাও")
        }
    }
}
