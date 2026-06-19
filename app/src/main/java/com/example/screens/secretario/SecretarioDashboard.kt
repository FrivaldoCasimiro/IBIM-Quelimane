package com.example.screens.secretario

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Aviso
import com.example.models.Evento
import com.example.models.Membro
import com.example.providers.AppViewModel
import com.example.widgets.CustomDrawerContent
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretarioDashboard(
    viewModel: AppViewModel,
    onTrocarPerfil: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val avisosList by viewModel.avisos.collectAsState()
    val membrosList by viewModel.membros.collectAsState()
    val eventosList by viewModel.eventos.collectAsState()

    // Dialog flags
    var showAvisoDialog by remember { mutableStateOf(false) }
    var editingAviso by remember { mutableStateOf<Aviso?>(null) }

    var showMembroDialog by remember { mutableStateOf(false) }
    var editingMembro by remember { mutableStateOf<Membro?>(null) }

    var showEventoDialog by remember { mutableStateOf(false) }
    var editingEvento by remember { mutableStateOf<Evento?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                CustomDrawerContent(
                    profileName = "Secretário",
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
                    title = { Text("IBI — Secretário") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                scope.launch {
                                    val now = System.currentTimeMillis()
                                    val activeAvisos = avisosList.filter { now in it.dataInicio..it.dataFim }
                                    val upcoming = viewModel.getUpcomingEventos()
                                    
                                    gerarBoletimPdf(context, activeAvisos, upcoming)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Boletim", fontSize = 12.sp)
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
                        icon = { Icon(imageVector = Icons.Default.Announcement, contentDescription = "Comunicados") },
                        label = { Text("Avisos") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(imageVector = Icons.Default.People, contentDescription = "Membros") },
                        label = { Text("Membros") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(imageVector = Icons.Default.Event, contentDescription = "Eventos") },
                        label = { Text("Eventos") }
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            0 -> {
                                editingAviso = null
                                showAvisoDialog = true
                            }
                            1 -> {
                                editingMembro = null
                                showMembroDialog = true
                            }
                            2 -> {
                                editingEvento = null
                                showEventoDialog = true
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Novo")
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> ComunicadosTab(
                        avisosList = avisosList,
                        onEdit = {
                            editingAviso = it
                            showAvisoDialog = true
                        }
                    )
                    1 -> MembrosTab(
                        viewModel = viewModel,
                        onEdit = {
                            editingMembro = it
                            showMembroDialog = true
                        }
                    )
                    2 -> EventosTab(
                        eventosList = eventosList,
                        onEdit = {
                            editingEvento = it
                            showEventoDialog = true
                        }
                    )
                }
            }
        }
    }

    // CRUD Announcements (Aviso) Dialog
    if (showAvisoDialog) {
        var titulo by remember { mutableStateOf(editingAviso?.titulo ?: "") }
        var conteudo by remember { mutableStateOf(editingAviso?.conteudo ?: "") }
        var dataIni by remember { mutableStateOf(editingAviso?.dataInicio ?: System.currentTimeMillis()) }
        var dataFim by remember { mutableStateOf(editingAviso?.dataFim ?: (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)) }
        var isDateSelection by remember { mutableIntStateOf(0) } // 1 for start, 2 for end

        AlertDialog(
            onDismissRequest = { showAvisoDialog = false },
            title = { Text(if (editingAviso == null) "Novo Aviso" else "Editar Aviso") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = conteudo,
                        onValueChange = { conteudo = it },
                        label = { Text("Conteúdo") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )

                    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { isDateSelection = 1 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Início: ${df.format(Date(dataIni))}", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { isDateSelection = 2 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Fim: ${df.format(Date(dataFim))}", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (titulo.isBlank() || conteudo.isBlank()) {
                        Toast.makeText(context, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val newAv = Aviso(
                        uuid = editingAviso?.uuid ?: UUID.randomUUID().toString(),
                        titulo = titulo,
                        conteudo = conteudo,
                        dataInicio = dataIni,
                        dataFim = dataFim,
                        ativo = 1
                    )
                    viewModel.saveAviso(newAv)
                    showAvisoDialog = false
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                Row {
                    if (editingAviso != null) {
                        TextButton(
                            onClick = {
                                viewModel.deleteAviso(editingAviso!!)
                                showAvisoDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Excluir")
                        }
                    }
                    TextButton(onClick = { showAvisoDialog = false }) { Text("Cancelar") }
                }
            }
        )

        if (isDateSelection > 0) {
            var dayIn by remember { mutableStateOf("17") }
            var monthIn by remember { mutableStateOf("06") }
            var yearIn by remember { mutableStateOf("2026") }

            AlertDialog(
                onDismissRequest = { isDateSelection = 0 },
                title = { Text(if (isDateSelection == 1) "Data de Início" else "Data de Expiração") },
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
                        if (isDateSelection == 1) {
                            dataIni = cal.timeInMillis
                        } else {
                            dataFim = cal.timeInMillis
                        }
                        isDateSelection = 0
                    }) {
                        Text("Confirmar")
                    }
                }
            )
        }
    }

    // CRUD Membros Dialog
    if (showMembroDialog) {
        var nome by remember { mutableStateOf(editingMembro?.nome ?: "") }
        var telefone by remember { mutableStateOf(editingMembro?.telefone ?: "") }
        var endereco by remember { mutableStateOf(editingMembro?.endereco ?: "") }
        var aniversario by remember { mutableStateOf(editingMembro?.aniversario ?: System.currentTimeMillis()) }
        var cargo by remember { mutableStateOf(editingMembro?.cargo ?: "Membro") }
        var showDatePicker by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showMembroDialog = false },
            title = { Text(if (editingMembro == null) "Cadastrar Membro" else "Editar Membro") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome do Membro") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = telefone, onValueChange = { telefone = it }, label = { Text("Telefone") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = endereco, onValueChange = { endereco = it }, label = { Text("Endereço") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cargo, onValueChange = { cargo = it }, label = { Text("Cargo / Função") }, modifier = Modifier.fillMaxWidth())

                    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    Button(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Aniversário: ${df.format(Date(aniversario))}")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nome.isBlank()) {
                        Toast.makeText(context, "Nome é obrigatório!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val currentMembro = Membro(
                        uuid = editingMembro?.uuid ?: UUID.randomUUID().toString(),
                        nome = nome,
                        telefone = telefone,
                        endereco = endereco,
                        aniversario = aniversario,
                        cargo = cargo
                    )
                    viewModel.saveMembro(currentMembro)
                    showMembroDialog = false
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                Row {
                    if (editingMembro != null) {
                        TextButton(
                            onClick = {
                                viewModel.deleteMembro(editingMembro!!)
                                showMembroDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Deletar")
                        }
                    }
                    TextButton(onClick = { showMembroDialog = false }) { Text("Cancelar") }
                }
            }
        )

        if (showDatePicker) {
            var dayIn by remember { mutableStateOf("17") }
            var monthIn by remember { mutableStateOf("06") }
            var yearIn by remember { mutableStateOf("1990") }

            AlertDialog(
                onDismissRequest = { showDatePicker = false },
                title = { Text("Data de Nascimento") },
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
                        cal.set(Calendar.YEAR, yearIn.toIntOrNull() ?: 1990)
                        aniversario = cal.timeInMillis
                        showDatePicker = false
                    }) {
                        Text("Confirmar")
                    }
                }
            )
        }
    }

    // CRUD Eventos Dialog
    if (showEventoDialog) {
        var titulo by remember { mutableStateOf(editingEvento?.titulo ?: "") }
        var desc by remember { mutableStateOf(editingEvento?.descricao ?: "") }
        var dataEv by remember { mutableStateOf(editingEvento?.dataEvento ?: System.currentTimeMillis()) }
        var showDatePicker by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEventoDialog = false },
            title = { Text(if (editingEvento == null) "Novo Evento" else "Editar Evento") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Nome do Evento") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Descrição") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )

                    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    Button(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Data: ${df.format(Date(dataEv))}")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (titulo.isBlank()) {
                        Toast.makeText(context, "Preencha o título do evento!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val newEv = Evento(
                        uuid = editingEvento?.uuid ?: UUID.randomUUID().toString(),
                        titulo = titulo,
                        descricao = desc,
                        dataEvento = dataEv
                    )
                    viewModel.saveEvento(newEv)
                    showEventoDialog = false
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                Row {
                    if (editingEvento != null) {
                        TextButton(
                            onClick = {
                                viewModel.deleteEvento(editingEvento!!)
                                showEventoDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Excluir")
                        }
                    }
                    TextButton(onClick = { showEventoDialog = false }) { Text("Cancelar") }
                }
            }
        )

        if (showDatePicker) {
            var dayIn by remember { mutableStateOf("17") }
            var monthIn by remember { mutableStateOf("06") }
            var yearIn by remember { mutableStateOf("2026") }

            AlertDialog(
                onDismissRequest = { showDatePicker = false },
                title = { Text("Data do Evento") },
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
                        dataEv = cal.timeInMillis
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
fun ComunicadosTab(
    avisosList: List<Aviso>,
    onEdit: (Aviso) -> Unit
) {
    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val now = System.currentTimeMillis()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Grade de Avisos & Comunicados",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (avisosList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum aviso cadastrado ainda.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(avisosList) { aviso ->
                    val isAtivo = now in aviso.dataInicio..aviso.dataFim
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEdit(aviso) },
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
                                    text = aviso.titulo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                // Active badge identifier
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isAtivo) Color(0xFFC8E6C9) else Color(0xFFFFCDD2))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isAtivo) "ATIVO" else "EXPIRADO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAtivo) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = aviso.conteudo, fontSize = 14.sp, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Vigência: ${df.format(Date(aviso.dataInicio))} a ${df.format(Date(aviso.dataFim))}",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MembrosTab(
    viewModel: AppViewModel,
    onEdit: (Membro) -> Unit
) {
    var queryStr by remember { mutableStateOf("") }
    val reactiveMembros by viewModel.filterMembros(queryStr).collectAsState(initial = emptyList())
    val dobFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Ficha de Membros",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = queryStr,
            onValueChange = { queryStr = it },
            placeholder = { Text("Pesquisar membro pelo nome...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (reactiveMembros.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum membro listado.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(reactiveMembros) { memb ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEdit(memb) },
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
                                    text = memb.nome,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = memb.cargo,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            if (memb.telefone.isNotBlank()) {
                                Text(text = "📞 Telefone: ${memb.telefone}", fontSize = 13.sp)
                            }
                            if (memb.endereco.isNotBlank()) {
                                Text(text = "📍 Endereço: ${memb.endereco}", fontSize = 13.sp, color = Color.DarkGray)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🎂 Aniversário: ${dobFormatter.format(Date(memb.aniversario))}",
                                fontSize = 11.sp,
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
fun EventosTab(
    eventosList: List<Evento>,
    onEdit: (Evento) -> Unit
) {
    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Agenda de Eventos",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (eventosList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum evento agendado ainda.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(eventosList) { ev ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEdit(ev) },
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
                                    text = ev.titulo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = df.format(Date(ev.dataEvento)),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = ev.descricao, fontSize = 14.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}

// PDF Export helper using native PdfDocument
private fun gerarBoletimPdf(
    context: Context,
    activeAvisos: List<Aviso>,
    upcomingEventos: List<Evento>
) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size standard in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint().apply {
            color = 0xFF0FB1AC.toInt() // Primary water green
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val paintSub = Paint().apply {
            color = 0xFF205F55.toInt() // Secondary Dark Green
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val paintHeaderSec = Paint().apply {
            color = 0xFF205F55.toInt()
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val paintBody = Paint().apply {
            color = 0xFF000000.toInt()
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val paintLine = Paint().apply {
            color = 0xFFCCCCCC.toInt()
        }

        var y = 50f

        // Draw header
        canvas.drawText("IBI Quelimane — Boletim Informativo", 40f, y, paintTitle)
        y += 24f
        val sdf = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
        canvas.drawText("Gerado offline em: ${sdf.format(Date())}", 40f, y, paintSub)
        y += 20f
        canvas.drawLine(40f, y, 550f, y, paintLine)
        y += 30f

        // Draw Avisos
        canvas.drawText("AVISOS E COMUNICADOS ATIVOS", 40f, y, paintHeaderSec)
        y += 10f
        canvas.drawLine(40f, y, 550f, y, paintLine)
        y += 20f

        if (activeAvisos.isEmpty()) {
            canvas.drawText("Nenhum aviso ativo para exibição no boletim corrente.", 50f, y, paintBody)
            y += 20f
        } else {
            for (av in activeAvisos) {
                if (y > 750f) break // Simple page overflow protection
                canvas.drawText("• ${av.titulo}", 50f, y, paintBody.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
                y += 14f
                canvas.drawText("  ${av.conteudo}", 50f, y, paintBody.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) })
                y += 18f
            }
        }

        y += 15f
        // Draw Upcoming agenda
        canvas.drawText("AGENDA PRÓXIMOS 7 DIAS", 40f, y, paintHeaderSec)
        y += 10f
        canvas.drawLine(40f, y, 550f, y, paintLine)
        y += 20f

        val dfEvent = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        if (upcomingEventos.isEmpty()) {
            canvas.drawText("Nenhum evento agendado para a próxima semana.", 50f, y, paintBody)
            y += 20f
        } else {
            for (ev in upcomingEventos) {
                if (y > 780f) break
                val evDate = dfEvent.format(Date(ev.dataEvento))
                canvas.drawText("• [$evDate] — ${ev.titulo}", 50f, y, paintBody.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
                y += 14f
                canvas.drawText("  ${ev.descricao}", 50f, y, paintBody.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) })
                y += 18f
            }
        }

        pdfDocument.finishPage(page)

        // Save file
        val downloadsFolder = context.getExternalFilesDir("Boletins") ?: context.filesDir
        if (!downloadsFolder.exists()) {
            downloadsFolder.mkdirs()
        }
        val filePdf = File(downloadsFolder, "Boletim_IBI_Quelimane_${System.currentTimeMillis()}.pdf")
        val fos = FileOutputStream(filePdf)
        pdfDocument.writeTo(fos)
        pdfDocument.close()
        fos.close()

        Toast.makeText(context, "BOLETIM PDF GERADO! Salvo em: ${filePdf.absolutePath}", Toast.LENGTH_LONG).show()

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Falha ao gerar boletim PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
