package com.example.screens.levita

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Musica
import com.example.models.Setlist
import com.example.providers.AppViewModel
import com.example.widgets.CustomDrawerContent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevitaDashboard(
    viewModel: AppViewModel,
    onTrocarPerfil: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val musicasList by viewModel.musicas.collectAsState()
    val setlistsList by viewModel.setlists.collectAsState()

    // Dialog state for adding/editing song
    var showSongDialog by remember { mutableStateOf(false) }
    var editingSong by remember { mutableStateOf<Musica?>(null) }

    // Dialog state for creating/editing setlist
    var showSetlistDialog by remember { mutableStateOf(false) }
    var enteringSetlistTitle by remember { mutableStateOf("") }
    var enteringSetlistDate by remember { mutableStateOf(System.currentTimeMillis()) }

    // Navigate inside setlist view (detail)
    var activeSetlistDetail by remember { mutableStateOf<Setlist?>(null) }

    // Full screen lyric view
    var activeLyricsSetlist by remember { mutableStateOf<Setlist?>(null) }

    if (activeLyricsSetlist != null) {
        ModoLeytraFullScreen(
            setlist = activeLyricsSetlist!!,
            musicasList = musicasList,
            onClose = { activeLyricsSetlist = null }
        )
    } else if (activeSetlistDetail != null) {
        SetlistDetailScreen(
            setlist = activeSetlistDetail!!,
            musicasList = musicasList,
            onSave = { updated ->
                viewModel.saveSetlist(updated)
                activeSetlistDetail = null
                Toast.makeText(context, "Setlist atualizado!", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                viewModel.deleteSetlist(it)
                activeSetlistDetail = null
                Toast.makeText(context, "Setlist removido!", Toast.LENGTH_SHORT).show()
            },
            onClose = { activeSetlistDetail = null }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    CustomDrawerContent(
                        profileName = "Levita",
                        viewModel = viewModel,
                        onClose = { scope.launch { drawerState.close() } },
                        onTrocarPerfil = onTrocarPerfil
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("IBI Quelimane — Levita") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(imageVector = Icons.Default.QueueMusic, contentDescription = "Hinário") },
                            label = { Text("Hinário") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(imageVector = Icons.Default.FormatListBulleted, contentDescription = "Setlists") },
                            label = { Text("Setlists") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Letrista") },
                            label = { Text("Modo Letrista") }
                        )
                    }
                },
                floatingActionButton = {
                    if (selectedTab == 0) {
                        FloatingActionButton(
                            onClick = {
                                editingSong = null
                                showSongDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Nova Música")
                        }
                    } else if (selectedTab == 1) {
                        FloatingActionButton(
                            onClick = {
                                enteringSetlistTitle = ""
                                enteringSetlistDate = System.currentTimeMillis()
                                showSetlistDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = "Novo Setlist")
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> HinarioTab(
                            musicasList = musicasList,
                            onEditSong = { song ->
                                editingSong = song
                                showSongDialog = true
                            }
                        )
                        1 -> SetlistsTab(
                            setlistsList = setlistsList,
                            onOpenSetlist = { activeSetlistDetail = it }
                        )
                        2 -> ModoLetristaTab(
                            setlistsList = setlistsList,
                            onStartLetrista = { activeLyricsSetlist = it }
                        )
                    }
                }
            }
        }
    }

    // Add/Edit Song Dialog
    if (showSongDialog) {
        var songTitle by remember { mutableStateOf(editingSong?.titulo ?: "") }
        var songTone by remember { mutableStateOf(editingSong?.tom ?: "") }
        var songLyrics by remember { mutableStateOf(editingSong?.letra ?: "") }
        var songChords by remember { mutableStateOf(editingSong?.cifras ?: "") }
        var songCategory by remember { mutableStateOf(editingSong?.categoria ?: "Entrada") }
        var categoryExpanded by remember { mutableStateOf(false) }

        val categories = listOf("Entrada", "Ofertório", "Comunhão", "Final")

        AlertDialog(
            onDismissRequest = { showSongDialog = false },
            title = { Text(if (editingSong == null) "Nova Música" else "Editar Música") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = songTitle,
                        onValueChange = { songTitle = it },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = songTone,
                            onValueChange = { songTone = it },
                            label = { Text("Tom") },
                            modifier = Modifier.weight(1f)
                        )

                        Box(modifier = Modifier.weight(1.5f)) {
                            OutlinedTextField(
                                value = songCategory,
                                onValueChange = {},
                                label = { Text("Categoria") },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { categoryExpanded = true }) {
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            songCategory = cat
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = songLyrics,
                        onValueChange = { songLyrics = it },
                        label = { Text("Letra") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 200.dp)
                    )

                    OutlinedTextField(
                        value = songChords,
                        onValueChange = { songChords = it },
                        label = { Text("Cifras") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 150.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (songTitle.isBlank()) {
                        Toast.makeText(context, "Preencha o título!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val currentUuid = editingSong?.uuid ?: UUID.randomUUID().toString()
                    val music = Musica(
                        uuid = currentUuid,
                        titulo = songTitle,
                        tom = songTone,
                        letra = songLyrics,
                        cifras = songChords,
                        categoria = songCategory
                    )
                    viewModel.saveMusica(music)
                    showSongDialog = false
                    Toast.makeText(context, "Música salva!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                Row {
                    if (editingSong != null) {
                        TextButton(
                            onClick = {
                                viewModel.deleteMusica(editingSong!!)
                                showSongDialog = false
                                Toast.makeText(context, "Música removida!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Excluir")
                        }
                    }
                    TextButton(onClick = { showSongDialog = false }) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    // Add Setlist Dialog
    if (showSetlistDialog) {
        val dateFormater = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var tempTitle by remember { mutableStateOf("") }
        var showDatePicker by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSetlistDialog = false },
            title = { Text("Novo Setlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("Nome do Culto / Evento") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { showDatePicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Data: ${dateFormater.format(Date(enteringSetlistDate))}")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (tempTitle.isBlank()) {
                        Toast.makeText(context, "Preencha o nome do culto!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val newSet = Setlist(
                        uuid = UUID.randomUUID().toString(),
                        nomeCulto = tempTitle,
                        dataCulto = enteringSetlistDate,
                        ordemMusicasUuids = ""
                    )
                    viewModel.saveSetlist(newSet)
                    showSetlistDialog = false
                    Toast.makeText(context, "Setlist criado! Adicione as músicas clicando nele.", Toast.LENGTH_LONG).show()
                }) {
                    Text("Criar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetlistDialog = false }) {
                    Text("Cancelar")
                }
            }
        )

        if (showDatePicker) {
            // Elegant simple simulation of native Datepicker for offline speed or custom modal
            var dayIn by remember { mutableStateOf("17") }
            var monthIn by remember { mutableStateOf("06") }
            var yearIn by remember { mutableStateOf("2026") }

            AlertDialog(
                onDismissRequest = { showDatePicker = false },
                title = { Text("Selecione a Data") },
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = dayIn, onValueChange = { dayIn = it }, label = { Text("Dia") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = monthIn, onValueChange = { monthIn = it }, label = { Text("Mês") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = yearIn, onValueChange = { yearIn = it }, label = { Text("Ano") }, modifier = Modifier.weight(1.5f))
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.DAY_OF_MONTH, dayIn.toIntOrNull() ?: 17)
                        cal.set(Calendar.MONTH, (monthIn.toIntOrNull() ?: 6) - 1)
                        cal.set(Calendar.YEAR, yearIn.toIntOrNull() ?: 2026)
                        enteringSetlistDate = cal.timeInMillis
                        showDatePicker = false
                    }) {
                        Text("Confirmar")
                    }
                }
            )
        }
    }
}

@Composable
fun HinarioTab(
    musicasList: List<Musica>,
    onEditSong: (Musica) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, musicasList) {
        if (searchQuery.isBlank()) musicasList else {
            musicasList.filter {
                it.titulo.contains(searchQuery, ignoreCase = true) ||
                        it.categoria.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Hinário Congregacional",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar música pelo título ou categoria...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma música encontrada.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(filtered) { song ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditSong(song) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = song.titulo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Tom: ${song.tom}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Categoria: ${song.categoria}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetlistsTab(
    setlistsList: List<Setlist>,
    onOpenSetlist: (Setlist) -> Unit
) {
    val dateFormater = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Setlists de Louvor",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "Toque em um setlist para ordenar ou selecionar as músicas dele.",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (setlistsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum setlist de culto agendado.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(setlistsList) { set ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSetlist(set) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = set.nomeCulto,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Culto em: ${dateFormater.format(Date(set.dataCulto))}",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModoLetristaTab(
    setlistsList: List<Setlist>,
    onStartLetrista: (Setlist) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Modo Letrista — Púlpito",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "Projete ou acompanhe as letras em Fonte Clássica 30.0 gigante para fácil visualização no altar:",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (setlistsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Crie setlists primeiro para habilitar o modo Letrista.", color = Color.Gray, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(setlistsList) { set ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartLetrista(set) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = set.nomeCulto,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Entrar no púlpito projetor de letras",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetlistDetailScreen(
    setlist: Setlist,
    musicasList: List<Musica>,
    onSave: (Setlist) -> Unit,
    onDelete: (Setlist) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var orderedUuids by remember {
        mutableStateOf(
            if (setlist.ordemMusicasUuids.isBlank()) emptyList()
            else setlist.ordemMusicasUuids.split(",")
        )
    }

    // Music matches for this setlist
    val currentSetSongs = remember(orderedUuids, musicasList) {
        orderedUuids.mapNotNull { optUuid ->
            musicasList.find { it.uuid == optUuid }
        }
    }

    var showChooseSongsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(setlist.nomeCulto) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { onDelete(setlist) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = Color.White)
                    }
                    IconButton(onClick = {
                        val csv = orderedUuids.joinToString(",")
                        val updated = setlist.copy(ordemMusicasUuids = csv)
                        onSave(updated)
                    }) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Salvar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Músicas Selecionadas (${currentSetSongs.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Button(onClick = { showChooseSongsDialog = true }) {
                    Icon(imageVector = Icons.Default.PlaylistAddCheck, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Selecionar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentSetSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma música adicionada ainda. Clique no botão acima para adicionar músicas do Hinário.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "Dica: Reordene as músicas do culto utilizando as setas de ordem rápida:",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(currentSetSongs) { index, song ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${index + 1}. ${song.titulo}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(text = "Tom: ${song.tom} • ${song.categoria}", fontSize = 12.sp, color = Color.Gray)
                                }

                                Row {
                                    // Move Up
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mutable = orderedUuids.toMutableList()
                                                val element = mutable.removeAt(index)
                                                mutable.add(index - 1, element)
                                                orderedUuids = mutable
                                            }
                                        },
                                        enabled = index > 0
                                    ) {
                                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(20.dp))
                                    }

                                    // Move Down
                                    IconButton(
                                        onClick = {
                                            if (index < currentSetSongs.size - 1) {
                                                val mutable = orderedUuids.toMutableList()
                                                val element = mutable.removeAt(index)
                                                mutable.add(index + 1, element)
                                                orderedUuids = mutable
                                            }
                                        },
                                        enabled = index < currentSetSongs.size - 1
                                    ) {
                                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(20.dp))
                                    }

                                    // Remove
                                    IconButton(
                                        onClick = {
                                            val mutable = orderedUuids.toMutableList()
                                            mutable.removeAt(index)
                                            orderedUuids = mutable
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChooseSongsDialog) {
        var mutableSelection by remember { mutableStateOf(orderedUuids.toSet()) }

        AlertDialog(
            onDismissRequest = { showChooseSongsDialog = false },
            title = { Text("Selecionar Músicas") },
            text = {
                if (musicasList.isEmpty()) {
                    Text("Adicione músicas no Hinário primeiro!")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(musicasList) { m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        mutableSelection = if (mutableSelection.contains(m.uuid)) {
                                            mutableSelection - m.uuid
                                        } else {
                                            mutableSelection + m.uuid
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = mutableSelection.contains(m.uuid),
                                    onCheckedChange = { ch ->
                                        mutableSelection = if (ch == true) {
                                            mutableSelection + m.uuid
                                        } else {
                                            mutableSelection - m.uuid
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(m.titulo, fontWeight = FontWeight.SemiBold)
                                    Text("Tom: ${m.tom} • ${m.categoria}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    orderedUuids = mutableSelection.toList()
                    showChooseSongsDialog = false
                }) {
                    Text("Ok")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChooseSongsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ModoLeytraFullScreen(
    setlist: Setlist,
    musicasList: List<Musica>,
    onClose: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    val setSongs = remember(setlist, musicasList) {
        if (setlist.ordemMusicasUuids.isBlank()) emptyList()
        else {
            setlist.ordemMusicasUuids.split(",").mapNotNull { opt ->
                musicasList.find { it.uuid == opt }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF1E1E1E) // Absolute pulpit dark background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Sair", tint = Color.White)
                }
                Text(
                    text = setlist.nomeCulto.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                )
                Text(
                    text = "MODO LETRISTA GIGANTE",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                )
            }

            if (setSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Este setlist não possui músicas adicionadas.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                val currentSong = setSongs.getOrNull(currentIndex)
                
                if (currentSong != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Progress indicators at the top
                        LinearProgressIndicator(
                            progress = { (currentIndex + 1).toFloat() / setSongs.size.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.DarkGray
                        )

                        // Sub Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Música ${currentIndex + 1} de ${setSongs.size}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Tom original: ${currentSong.tom}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }

                        // Massive Clickable Interactive Screen
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clickable { /* We'll let user click right and left buttons for simpler tap target size class navigation */ }
                        ) {
                            // Column representing Lyrics with scroll
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentSong.titulo.uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                    text = currentSong.letra,
                                    color = Color.White,
                                    fontSize = 30.sp, // Mandated 30.0 size
                                    lineHeight = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Touch area split guides
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Tap Left: Prev
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            if (currentIndex > 0) currentIndex--
                                        }
                                ) {
                                    if (currentIndex > 0) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronLeft,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.15f),
                                            modifier = Modifier
                                                .size(64.dp)
                                                .align(Alignment.CenterStart)
                                                .padding(start = 8.dp)
                                        )
                                    }
                                }

                                // Tap Right: Next
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            if (currentIndex < setSongs.size - 1) currentIndex++
                                        }
                                ) {
                                    if (currentIndex < setSongs.size - 1) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.15f),
                                            modifier = Modifier
                                                .size(64.dp)
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
