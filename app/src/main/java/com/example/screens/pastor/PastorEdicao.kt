package com.example.screens.pastor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Esboco
import com.example.models.EsbocoVersiculo
import com.example.providers.AppViewModel
import com.example.utils.RegexHelper
import com.example.utils.VerseRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

// Accent mapping helper to support unaccented input (e.g. joao -> João)
fun normalizeBookName(inputOfBook: String): String {
    val clean = inputOfBook.trim().lowercase()
        .replace("á", "a").replace("â", "a").replace("ã", "a")
        .replace("é", "e").replace("ê", "e")
        .replace("í", "i")
        .replace("ó", "o").replace("ô", "o").replace("õ", "o")
        .replace("ú", "u")
        .replace("ç", "c")
    
    return when (clean) {
        "genesis" -> "Gênesis"
        "exodo" -> "Êxodo"
        "levitico" -> "Levítico"
        "numeros" -> "Números"
        "deuteronomio" -> "Deuteronômio"
        "josue" -> "Josué"
        "juizes" -> "Juízes"
        "1 cronicas", "1cronicas" -> "1 Crônicas"
        "2 cronicas", "2cronicas" -> "2 Crônicas"
        "jo" -> "Jó"
        "proverbios" -> "Provérbios"
        "isaias" -> "Isaías"
        "lamentacoes" -> "Lamentações"
        "oseias" -> "Oseias"
        "amos" -> "Amós"
        "miqueias" -> "Miqueias"
        "joao" -> "João"
        "1 joao", "1joao" -> "1 João"
        "2 joao", "2joao" -> "2 João"
        "3 joao", "3joao" -> "3 João"
        "galatas" -> "Gálatas"
        "efesios" -> "Efésios"
        "1 timoteo", "1timoteo" -> "1 Timóteo"
        "2 timoteo", "2timoteo" -> "2 Timóteo"
        "lucas" -> "Lucas"
        "mateus" -> "Mateus"
        "marcos" -> "Marcos"
        "atos" -> "Atos"
        "romanos" -> "Romanos"
        "hebreus" -> "Hebreus"
        "apocalipse" -> "Apocalipse"
        else -> inputOfBook
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastorEdicao(
    viewModel: AppViewModel,
    esbocoUuid: String?, // Null for creating, otherwise editing
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Stable ID to reference linked verses even during new drafts
    val currentEsbocoUuid = remember { esbocoUuid ?: UUID.randomUUID().toString() }
    
    var titulo by remember { mutableStateOf("") }
    var livroSelecionado by remember { mutableStateOf("") }
    var statusSelec by remember { mutableStateOf("Rascunho") }
    var tempoEstimado by remember { mutableStateOf("15") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Use TextFieldValue to track current text selection for Linking
    var textoEsbocoValue by remember { mutableStateOf(TextFieldValue("")) }
    
    // Theme preference
    val themePref by viewModel.themePreference.collectAsState()
    val isDarkTheme = themePref == "dark"

    // Sub-mode tabs inside the editor
    var activeSubTab by remember { mutableIntStateOf(0) } // 0: Editor, 1: Leitura Interativa

    val livrosList by viewModel.livros.collectAsState()
    val linkedVerses by viewModel.getVersiculosForEsboco(currentEsbocoUuid).collectAsState(initial = emptyList())

    // Tracks selected text in TextFieldValue
    val selectedText = remember(textoEsbocoValue) {
        val selection = textoEsbocoValue.selection
        if (!selection.collapsed && selection.start < textoEsbocoValue.text.length && selection.end <= textoEsbocoValue.text.length) {
            textoEsbocoValue.text.substring(selection.start, selection.end).trim()
        } else {
            ""
        }
    }

    // Modal Bottom Sheet state
    var selectedRefForSheet by remember { mutableStateOf<VerseRef?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Load existing esboço
    LaunchedEffect(esbocoUuid) {
        if (esbocoUuid != null) {
            val esb = viewModel.esbocos.value.find { it.uuid == esbocoUuid }
            if (esb != null) {
                titulo = esb.titulo
                livroSelecionado = esb.livroBiblia
                textoEsbocoValue = TextFieldValue(esb.textoEsboco)
                statusSelec = esb.status
                tempoEstimado = esb.tempoEstimado.toString()
            }
        } else {
            if (livrosList.isNotEmpty()) {
                livroSelecionado = livrosList.first().nome
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esbocoUuid == null) "Novo Esboço" else "Editar Esboço", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (titulo.isBlank() || textoEsbocoValue.text.isBlank()) {
                            Toast.makeText(context, "Preencha Título e Conteúdo!", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        val finalEsboco = Esboco(
                            uuid = currentEsbocoUuid,
                            titulo = titulo,
                            textoEsboco = textoEsbocoValue.text,
                            livroBiblia = livroSelecionado,
                            status = statusSelec,
                            dataCriacao = System.currentTimeMillis(),
                            tempoEstimado = tempoEstimado.toIntOrNull() ?: 15
                        )
                        viewModel.saveEsboco(finalEsboco)
                        Toast.makeText(context, "Esboço salvo com sucesso!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Salvar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // General Details Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Informações do Sermão",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Title
                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { titulo = it },
                        label = { Text("Título do Sermão") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Book dropdown selector
                        Box(modifier = Modifier.weight(1.3f)) {
                            OutlinedTextField(
                                value = livroSelecionado,
                                onValueChange = {},
                                label = { Text("Livro Focal") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { dropdownExpanded = true }) {
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Mostrar livros")
                                    }
                                }
                            )
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                val displayBooks = if (livrosList.isEmpty()) {
                                    listOf("Gênesis", "Salmos", "João", "Romanos", "Efésios")
                                } else {
                                    livrosList.map { it.nome }
                                }
                                displayBooks.forEach { book ->
                                    DropdownMenuItem(
                                        text = { Text(book) },
                                        onClick = {
                                            livroSelecionado = book
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Time setting
                        OutlinedTextField(
                            value = tempoEstimado,
                            onValueChange = { tempoEstimado = it },
                            label = { Text("Tempo (m)") },
                            modifier = Modifier.weight(0.7f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Status: ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        FilterChip(
                            selected = statusSelec == "Rascunho",
                            onClick = { statusSelec = "Rascunho" },
                            label = { Text("Rascunho") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = statusSelec == "Finalizado",
                            onClick = { statusSelec = "Finalizado" },
                            label = { Text("Finalizado") }
                        )
                    }
                }
            }

            // Modes selection Tabs
            TabRow(
                selectedTabIndex = activeSubTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = activeSubTab == 0,
                    onClick = { activeSubTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null) },
                    text = { Text("📝 Bloco (Editor)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.MenuBook, contentDescription = null) },
                    text = { Text("📖 Leitura Interativa", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            // Selection tool overlay: "Marcar Seleção" to link verse
            if (activeSubTab == 0 && selectedText.isNotEmpty()) {
                val detectedInSelection = RegexHelper.extractVerses(selectedText)
                val topMatch = if (detectedInSelection.isNotEmpty()) detectedInSelection.first() else null

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Lincar texto selecionado no Bloco?", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = if (topMatch != null) "Referência: ${topMatch.fullText}" else "\"$selectedText\"",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val referenceToQuery = topMatch?.fullText ?: selectedText
                                scope.launch(Dispatchers.IO) {
                                    val extracted = RegexHelper.extractVerses(referenceToQuery)
                                    if (extracted.isNotEmpty()) {
                                        val firstRef = extracted.first()
                                        val norm = normalizeBookName(firstRef.book)
                                        val offlineVerse = viewModel.findWordInBible(norm, firstRef.chapter, firstRef.verse)
                                        
                                        withContext(Dispatchers.Main) {
                                            if (offlineVerse != null) {
                                                viewModel.saveEsbocoVersiculo(
                                                    EsbocoVersiculo(
                                                        esbocoUuid = currentEsbocoUuid,
                                                        referencia = firstRef.fullText,
                                                        textoVersiculo = offlineVerse.texto
                                                    )
                                                )
                                                Toast.makeText(context, "Versículo '${firstRef.fullText}' lincado com sucesso!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Não foi possível encontrar '${firstRef.fullText}' offline. Tente baixar a Bíblia completa ou ajustar a escrita.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Selecione uma referência válida (ex: João 3:16) ou adicione pelo painel.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Sim, Lincar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Notebook Binder spiral decoration at the top edge
            NotepadSpiralRings()

            // The Notepad visual card sheet!
            NotepadPaper(isDark = isDarkTheme) {
                if (activeSubTab == 0) {
                    // TAB 0: Editor Mode
                    TextField(
                        value = textoEsbocoValue,
                        onValueChange = { textoEsbocoValue = it },
                        placeholder = {
                            Text(
                                "Escreva as notas do seu sermão aqui...\n\nPor exemplo:\n\"Hoje leremos o livro de Joao 3:16 e meditaremos na palavra.\"",
                                fontSize = 15.sp,
                                color = if (isDarkTheme) Color.Gray else Color.LightGray,
                                lineHeight = 28.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = if (isDarkTheme) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkTheme) Color.White else Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 350.dp, max = 600.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 28.sp, // perfectly aligns with the ruled paper background lines
                            fontWeight = FontWeight.Medium
                        )
                    )
                } else {
                    // TAB 1: Clickable Interactive Notebook
                    val textToParse = textoEsbocoValue.text
                    if (textToParse.isBlank()) {
                        Text(
                            text = "Insira algum conteúdo na aba de Editor para visualizar suas notas interativas de bloco.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        val annotatedText = buildSermonAnnotatedString(textToParse, isDarkTheme)
                        ClickableText(
                            text = annotatedText,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 28.sp,
                                color = if (isDarkTheme) Color.White else Color.Black,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 350.dp),
                            onClick = { offset ->
                                annotatedText.getStringAnnotations(tag = "VERSE_LINK", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        val linkStr = annotation.item
                                        val versesExtr = RegexHelper.extractVerses(linkStr)
                                        if (versesExtr.isNotEmpty()) {
                                            selectedRefForSheet = versesExtr.first()
                                        }
                                    }
                            }
                        )
                    }
                }
            }

            // Linked Verses Row (bottom summary display)
            if (linkedVerses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Versículos Lincados neste Bloco (${linkedVerses.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    items(linkedVerses) { currentLinked ->
                        AssistChip(
                            onClick = {
                                val versesExtr = RegexHelper.extractVerses(currentLinked.referencia)
                                if (versesExtr.isNotEmpty()) {
                                    selectedRefForSheet = versesExtr.first()
                                } else {
                                    // Custom fallback search
                                    selectedRefForSheet = VerseRef(currentLinked.referencia, currentLinked.referencia, 1, 1)
                                }
                            },
                            label = { Text(currentLinked.referencia, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remover",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            // Handle delete linked relation
                                            viewModel.deleteVersiculosForEsboco(currentEsbocoUuid)
                                        },
                                    tint = Color.Red
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet displays corresponding scripture text instantly!
    if (selectedRefForSheet != null) {
        val currRef = selectedRefForSheet!!
        var retrievedVerseText by remember { mutableStateOf<String?> (null) }
        var isSearching by remember { mutableStateOf(true) }

        LaunchedEffect(currRef) {
            isSearching = true
            val normBook = normalizeBookName(currRef.book)
            // 1. Check if we already have it in local Linked Repository
            val savedMatch = linkedVerses.find { it.referencia.lowercase() == currRef.fullText.lowercase() }
            if (savedMatch != null) {
                retrievedVerseText = savedMatch.textoVersiculo
            } else {
                // 2. Query offline Bible
                val verseObj = viewModel.findWordInBible(normBook, currRef.chapter, currRef.verse)
                retrievedVerseText = verseObj?.texto
            }
            isSearching = false
        }

        ModalBottomSheet(
            onDismissRequest = { selectedRefForSheet = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                val isLinked = linkedVerses.any { it.referencia.lowercase() == currRef.fullText.lowercase() }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currRef.fullText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Chip highlighting dynamic sync status
                    SuggestionChip(
                        onClick = {},
                        label = { Text(if (isLinked) "Lincado" else "Linguagem Comum") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isLinked) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    val finalVerse = retrievedVerseText
                    if (finalVerse != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDarkTheme) Color(0xFF2C2C35) else Color(0xFFFFF9C4))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "\"$finalVerse\"",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkTheme) Color.White else Color.Black,
                                lineHeight = 24.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Connect/Save this specifically to notebook if not connected
                        if (!isLinked) {
                            Button(
                                onClick = {
                                    viewModel.saveEsbocoVersiculo(
                                        EsbocoVersiculo(
                                            esbocoUuid = currentEsbocoUuid,
                                            referencia = currRef.fullText,
                                            textoVersiculo = finalVerse
                                        )
                                    )
                                    Toast.makeText(context, "'${currRef.fullText}' vinculado à página!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Link, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Lincar esta Referência à Notas", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Fallback option when bible does not have the verse offline
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Linguagem local: O versículo correspondente não foi localizado offline na base de dados.",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            var manualText by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = manualText,
                                onValueChange = { manualText = it },
                                label = { Text("Adicionar conteúdo em português") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (manualText.isNotBlank()) {
                                        viewModel.saveEsbocoVersiculo(
                                            EsbocoVersiculo(
                                                esbocoUuid = currentEsbocoUuid,
                                                referencia = currRef.fullText,
                                                textoVersiculo = manualText
                                            )
                                        )
                                        Toast.makeText(context, "Salvo offline!", Toast.LENGTH_SHORT).show()
                                        selectedRefForSheet = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Salvar Versículo Personalizado", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom 3D metal rings decoration
@Composable
fun NotepadSpiralRings() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(12) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                )
            }
        }
    }
}

// Customized paper surface with horizontal notebook ruled lines and vertical red sheet margin line
@Composable
fun NotepadPaper(
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val paperColor = if (isDark) Color(0xFF1E1E2C) else Color(0xFFFDFBF0)
    val lineCol = if (isDark) Color(0xFF2E2E3D) else Color(0xFFE3F2FD)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(paperColor)
            .drawBehind {
                val density = this.density
                val lineSpacingPx = 28 * density
                val totalLines = (size.height / lineSpacingPx).toInt()
                
                // Draw horizontal page rules
                for (i in 1..totalLines) {
                    val y = i * lineSpacingPx
                    drawLine(
                        color = lineCol,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw standard vertical red legal sheet margin line
                val marginXPx = 36 * density
                drawLine(
                    color = if (isDark) Color(0xFFEF5350).copy(alpha = 0.35f) else Color(0xFFFF9E80),
                    start = androidx.compose.ui.geometry.Offset(marginXPx, 0f),
                    end = androidx.compose.ui.geometry.Offset(marginXPx, size.height),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
            .padding(start = 44.dp, end = 16.dp, top = 20.dp, bottom = 20.dp),
        content = content
    )
}

// Annotated String generator to convert all occurrences of scripture references (e.g. joao 3:16) to clickable underlines
@Composable
fun buildSermonAnnotatedString(text: String, isDark: Boolean): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val regex = """(\d?\s*[A-Za-zÀ-ÖØ-öø-ÿ]+(?:\s+[A-Za-zÀ-ÖØ-öø-ÿ]+)?)\s*(\d+):(\d+)""".toRegex()
    var lastIndex = 0
    val matches = regex.findAll(text)
    
    for (match in matches) {
        if (match.range.first > lastIndex) {
            builder.append(text.substring(lastIndex, match.range.first))
        }
        
        val start = builder.length
        builder.append(match.value)
        val end = builder.length
        
        builder.addStyle(
            style = SpanStyle(
                color = if (isDark) Color(0xFF80DEEA) else Color(0xFF00796B),
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline
            ),
            start = start,
            end = end
        )
        
        builder.addStringAnnotation(
            tag = "VERSE_LINK",
            annotation = match.value,
            start = start,
            end = end
        )
        
        lastIndex = match.range.last + 1
    }
    
    if (lastIndex < text.length) {
        builder.append(text.substring(lastIndex))
    }
    
    return builder.toAnnotatedString()
}
