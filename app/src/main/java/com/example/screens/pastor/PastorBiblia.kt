package com.example.screens.pastor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.BibliaLivro
import com.example.models.BibliaVersiculo
import com.example.providers.AppViewModel
import com.example.providers.BibleDownloadState
import kotlinx.coroutines.launch

@Composable
fun PastorBiblia(
    viewModel: AppViewModel
) {
    val scope = rememberCoroutineScope()
    val livrosList by viewModel.livros.collectAsState()
    val downloadState by viewModel.bibleDownloadState.collectAsState()

    var selectedLivro by remember { mutableStateOf<BibliaLivro?>(null) }
    var currentChapter by remember { mutableStateOf(1) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<BibliaVersiculo>>(emptyList()) }
    var isSearchingResult by remember { mutableStateOf(false) }

    // Verses of current selection
    val currentVerses = remember(selectedLivro, currentChapter, downloadState) {
        if (selectedLivro != null) {
            viewModel.getVersiculosOfChapter(selectedLivro!!.id, currentChapter)
        } else {
            flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    // Check Bible status on mount
    LaunchedEffect(Unit) {
        viewModel.checkAndTriggerBibleDownload()
    }

    // Update selection when books load
    LaunchedEffect(livrosList) {
        if (selectedLivro == null && livrosList.isNotEmpty()) {
            selectedLivro = livrosList.firstOrNull { it.nome == "João" } ?: livrosList.first()
            currentChapter = 3 // default to João 3
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Dynamic Bible Seeding Status Notifications
        if (downloadState is BibleDownloadState.NeedsDownload) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📥 Bíblia Sagrada Completa Offline",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Baixe todos os 66 livros e mais de 31.000 versículos da Bíblia NVI para ler sem nenhuma conexão de internet.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.startBibleDownload() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Baixar Bíblia Agora (Grátis)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (downloadState is BibleDownloadState.Downloading || downloadState is BibleDownloadState.Saving) {
            val statusText = if (downloadState is BibleDownloadState.Saving) "Salvando Bíblia localmente..." else "Baixando Bíblia Completa..."
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusText,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Por favor, aguarde. Isto levará apenas alguns segundos...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else if (downloadState is BibleDownloadState.Error) {
            val errorMsg = (downloadState as BibleDownloadState.Error).message
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "❌ Falha no Download",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Falha ao sincronizar Bíblia: $errorMsg.\nPor favor, garanta que está conectado à rede e tente novamente.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.startBibleDownload() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tentar Novamente", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                if (query.isNotBlank()) {
                    isSearchingResult = true
                    // Search in model database
                    scope.launch {
                        // Normally search is fast on small sets, limit to 50
                        val res = viewModel.filterMembros("") // placeholder
                        // Search in bible
                        val resBible = viewModel.findWordInBible("", 1, 1) // trigger search
                        // Let's call database search
                        val database = com.example.database.MainDatabase.getDatabase(viewModel.getApplication())
                        val dbResults = database.bibliaDao().searchVersiculos(query)
                        searchResults = dbResults
                        isSearchingResult = false
                    }
                } else {
                    searchResults = emptyList()
                    isSearchingResult = false
                }
            },
            placeholder = { Text("Pesquisar na Bíblia (Ex: Deus amou o mundo)") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Pesquisar") },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        searchResults = emptyList()
                    }) {
                        Text("X", fontWeight = FontWeight.Bold)
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (searchQuery.isNotBlank()) {
            // Display Search Results
            Text(
                text = "Resultados da pesquisa (${searchResults.size}):",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (isSearchingResult) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum versículo encontrado localmente.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults) { versiculo ->
                        val livroNome = livrosList.find { it.id == versiculo.livroId }?.nome ?: "Livro"
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "$livroNome ${versiculo.capitulo}:${versiculo.numero}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = versiculo.texto, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // Display Reader View
            val themePref by viewModel.themePreference.collectAsState()
            val isDark = themePref == "dark"
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1E2C) else Color(0xFFE0F2F1)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.Info else Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF80DEEA) else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Modo Escuro (Cultos à Noite)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.secondary
                        )
                    }
                    TextButton(
                        onClick = {
                            viewModel.setThemePreference(if (isDark) "light" else "dark")
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isDark) Color(0xFF80DEEA) else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.height(32.dp).padding(0.dp)
                    ) {
                        Text(
                            text = if (isDark) "Desativar" else "Ativar Modo Escuro",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Book selector
                var bookSelectorExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1.5f)) {
                    Button(
                        onClick = { bookSelectorExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Book, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(selectedLivro?.nome ?: "Selecione Livro")
                    }
                    DropdownMenu(
                        expanded = bookSelectorExpanded,
                        onDismissRequest = { bookSelectorExpanded = false }
                    ) {
                        livrosList.forEach { book ->
                            DropdownMenuItem(
                                text = { Text(book.nome) },
                                onClick = {
                                    selectedLivro = book
                                    currentChapter = 1
                                    bookSelectorExpanded = false
                                }
                            )
                        }
                    }
                }

                // Chapter step selector
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { if (currentChapter > 1) currentChapter-- },
                        enabled = currentChapter > 1
                    ) {
                        Text("<", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Text(
                        text = "Cap. $currentChapter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(
                        onClick = { currentChapter++ }
                    ) {
                        Text(">", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Verses List
            Text(
                text = "${selectedLivro?.nome ?: ""} Capítulo $currentChapter",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (currentVerses.value.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Este capítulo não está disponível localmente.",
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Baixe a Bíblia Sagrada Completa tocando no botão 'Baixar Bíblia Agora' no topo para salvar todos os 66 livros e os mais de 31.000 versículos offline de forma definitiva!",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(currentVerses.value) { verse ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "${verse.numero} ",
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                modifier = Modifier.width(28.dp)
                            )
                            Text(
                                text = verse.texto,
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Simple fallback flowOf helper since we are not using full Rx libraries
private fun <T> flowOf(value: T): kotlinx.coroutines.flow.Flow<T> = kotlinx.coroutines.flow.flow {
    emit(value)
}
