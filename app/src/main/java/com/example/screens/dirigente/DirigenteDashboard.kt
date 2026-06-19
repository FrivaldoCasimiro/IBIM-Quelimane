package com.example.screens.dirigente

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Escala
import com.example.models.Template
import com.example.providers.AppViewModel
import com.example.widgets.CustomDrawerContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirigenteDashboard(
    viewModel: AppViewModel,
    onTrocarPerfil: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val templatesList by viewModel.templates.collectAsState()
    val escalasList by viewModel.escalas.collectAsState()

    // Dialog state
    var showTemplateDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<Template?>(null) }

    var showCreateEscalaDialog by remember { mutableStateOf(false) }
    var scaleTemplateUuid by remember { mutableStateOf("") }
    var scaleDate by remember { mutableStateOf(System.currentTimeMillis()) }

    var configuringEscala by remember { mutableStateOf<Escala?>(null) }
    var activeCultoScreenEscala by remember { mutableStateOf<Escala?>(null) }

    if (activeCultoScreenEscala != null) {
        CultoModeScreen(
            escala = activeCultoScreenEscala!!,
            viewModel = viewModel,
            onClose = { activeCultoScreenEscala = null }
        )
    } else if (configuringEscala != null) {
        ConfigureEscalaScreen(
            escala = configuringEscala!!,
            viewModel = viewModel,
            onSave = { updated ->
                viewModel.saveEscala(updated)
                configuringEscala = null
                Toast.makeText(context, "Escala salva!", Toast.LENGTH_SHORT).show()
            },
            onClose = { configuringEscala = null }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    CustomDrawerContent(
                        profileName = "Dirigente",
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
                        title = { Text("IBI — Dirigente") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = null)
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
                            icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = null) },
                            label = { Text("Templates") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(imageVector = Icons.Default.AssignmentInd, contentDescription = null) },
                            label = { Text("Escalas") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(imageVector = Icons.Default.AccessTimeFilled, contentDescription = null) },
                            label = { Text("Mestre de C.") }
                        )
                    }
                },
                floatingActionButton = {
                    if (selectedTab == 0) {
                        FloatingActionButton(
                            onClick = {
                                editingTemplate = null
                                showTemplateDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        }
                    } else if (selectedTab == 1) {
                        FloatingActionButton(
                            onClick = {
                                if (templatesList.isEmpty()) {
                                    Toast.makeText(context, "Crie um template primeiro!", Toast.LENGTH_LONG).show()
                                    return@FloatingActionButton
                                }
                                scaleTemplateUuid = templatesList.first().uuid
                                scaleDate = System.currentTimeMillis()
                                showCreateEscalaDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Icon(imageVector = Icons.Default.AssignmentTurnedIn, contentDescription = null)
                        }
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
                        0 -> TemplatesTab(
                            templatesList = templatesList,
                            onEdit = {
                                editingTemplate = it
                                showTemplateDialog = true
                            }
                        )
                        1 -> EscalasTab(
                            escalasList = escalasList,
                            templatesList = templatesList,
                            onConfigure = { configuringEscala = it },
                            onDelete = { viewModel.deleteEscala(it) }
                        )
                        2 -> MestreDeCerimoniasIntroTab(
                            escalasList = escalasList,
                            onSelectEscala = { activeCultoScreenEscala = it }
                        )
                    }
                }
            }
        }
    }

    // CRUD Templates Dialog
    if (showTemplateDialog) {
        var templateName by remember { mutableStateOf(editingTemplate?.nomeTemplate ?: "") }
        var rawEtapasText by remember {
            mutableStateOf(
                if (editingTemplate == null) "Acolhida:5\nLouvor:15\nPregação:30"
                else {
                    try {
                        val arr = JSONArray(editingTemplate!!.etapasJson)
                        val sb = StringBuilder()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            sb.append("${obj.getString("nome")}:${obj.getInt("duracao")}\n")
                        }
                        sb.toString().trim()
                    } catch (e: Exception) {
                        "Acolhida:5\nLouvor:15\nPregação:30"
                    }
                }
            )
        }

        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text(if (editingTemplate == null) "Novo Template" else "Editar Template") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("Nome do Template") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = rawEtapasText,
                        onValueChange = { rawEtapasText = it },
                        label = { Text("Etapas (Formato Nome:Minutos)") },
                        placeholder = { Text("Exemplo:\nAcolhida:5\nLouvor:15\nPregação:30") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )
                    Text(
                        text = "Digite uma etapa por linha.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (templateName.isBlank() || rawEtapasText.isBlank()) {
                        Toast.makeText(context, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Parse line entries Name:Minutes
                    val stepsArray = JSONArray()
                    val lines = rawEtapasText.split("\n")
                    for (line in lines) {
                        if (line.contains(":")) {
                            val parts = line.split(":")
                            val name = parts[0].trim()
                            val parsedDur = parts[1].trim().toIntOrNull() ?: 5
                            val obj = JSONObject()
                            obj.put("nome", name)
                            obj.put("duracao", parsedDur)
                            stepsArray.put(obj)
                        }
                    }

                    val newTemp = Template(
                        uuid = editingTemplate?.uuid ?: UUID.randomUUID().toString(),
                        nomeTemplate = templateName,
                        etapasJson = stepsArray.toString()
                    )
                    viewModel.saveTemplate(newTemp)
                    showTemplateDialog = false
                    Toast.makeText(context, "Template salvo com sucesso!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                Row {
                    if (editingTemplate != null) {
                        TextButton(
                            onClick = {
                                viewModel.deleteTemplate(editingTemplate!!)
                                showTemplateDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Excluir")
                        }
                    }
                    TextButton(onClick = { showTemplateDialog = false }) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    // Create Scale Dialog
    if (showCreateEscalaDialog) {
        var dropdownExpanded by remember { mutableStateOf(false) }
        var showDatePicker by remember { mutableStateOf(false) }
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        AlertDialog(
            onDismissRequest = { showCreateEscalaDialog = false },
            title = { Text("Criar Nova Escala Culto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Template Dropdown Selection
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val activeSelectorTemp = templatesList.find { it.uuid == scaleTemplateUuid }
                        OutlinedTextField(
                            value = activeSelectorTemp?.nomeTemplate ?: "Nenhum",
                            onValueChange = {},
                            label = { Text("Template Litúrgico") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = true }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            templatesList.forEach { temp ->
                                DropdownMenuItem(
                                    text = { Text(temp.nomeTemplate) },
                                    onClick = {
                                        scaleTemplateUuid = temp.uuid
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Date Selection
                    Button(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Data Culto: ${formatter.format(Date(scaleDate))}")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val scaleUuid = UUID.randomUUID().toString()
                    val newScale = Escala(
                        uuid = scaleUuid,
                        dataCulto = scaleDate,
                        templateUuid = scaleTemplateUuid,
                        responsaveisJson = "{}" // empty responsibles
                    )
                    viewModel.saveEscala(newScale)
                    showCreateEscalaDialog = false
                    configuringEscala = newScale
                }) {
                    Text("Configurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateEscalaDialog = false }) {
                    Text("Cancelar")
                }
            }
        )

        if (showDatePicker) {
            var dayIn by remember { mutableStateOf("17") }
            var monthIn by remember { mutableStateOf("06") }
            var yearIn by remember { mutableStateOf("2026") }

            AlertDialog(
                onDismissRequest = { showDatePicker = false },
                title = { Text("Selecione data do Culto") },
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
                        scaleDate = cal.timeInMillis
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
fun TemplatesTab(
    templatesList: List<Template>,
    onEdit: (Template) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Templates Litúrgicos",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "Grade de etapas litúrgicas predefinidas para ritos e cultos:",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (templatesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum template litúrgico criado ainda.", color = Color.Gray, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(templatesList) { temp ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEdit(temp) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = temp.nomeTemplate,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Format printed stages info
                            val summary = remember(temp.etapasJson) {
                                try {
                                    val arr = JSONArray(temp.etapasJson)
                                    val namesList = mutableListOf<String>()
                                    var totalDur = 0
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.getJSONObject(i)
                                        namesList.add(obj.getString("nome"))
                                        totalDur += obj.getInt("duracao")
                                    }
                                    "Etapas: ${namesList.joinToString(" → ")} (${totalDur} min)"
                                } catch (e: Exception) {
                                    "Configuração incorreta de etapas."
                                }
                            }
                            Text(text = summary, fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EscalasTab(
    escalasList: List<Escala>,
    templatesList: List<Template>,
    onConfigure: (Escala) -> Unit,
    onDelete: (Escala) -> Unit
) {
    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Escalas Litúrgicas Ativas",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "Selecione uma escala para definir os pregadores, levitas de acolhimento e responsáveis de rito:",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (escalasList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhuma escala vinculada.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(escalasList) { escala ->
                    val temp = templatesList.find { it.uuid == escala.templateUuid }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfigure(escala) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Culto — ${df.format(Date(escala.dataCulto))}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Template: ${temp?.nomeTemplate ?: "Desconhecido"}",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }

                            IconButton(onClick = { onDelete(escala) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MestreDeCerimoniasIntroTab(
    escalasList: List<Escala>,
    onSelectEscala: (Escala) -> Unit
) {
    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Mestre de Cerimônias Litúrgico",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "Escolha um culto com escala ativa para abrir o cronômetro visual em tempo de execução real do rito e controlar o tempo litúrgico:",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (escalasList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhuma escala literária de culto disponível.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(escalasList) { escala ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectEscala(escala) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
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
                                    text = "Iniciar Culto — ${df.format(Date(escala.dataCulto))}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Abrir cronômetros de púlpito",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
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
fun ConfigureEscalaScreen(
    escala: Escala,
    viewModel: AppViewModel,
    onSave: (Escala) -> Unit,
    onClose: () -> Unit
) {
    val templatesList by viewModel.templates.collectAsState()
    val activeTemplate = remember(templatesList) {
        templatesList.find { it.uuid == escala.templateUuid }
    }

    var answers by remember {
        mutableStateOf<Map<String, String>>(
            try {
                val obj = JSONObject(escala.responsaveisJson)
                val map = mutableMapOf<String, String>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = obj.getString(k)
                }
                map
            } catch (e: Exception) {
                emptyMap()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar Responsáveis") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val obj = JSONObject()
                        answers.forEach { (k, v) ->
                            obj.put(k, v)
                        }
                        onSave(escala.copy(responsaveisJson = obj.toString()))
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Liturgia: ${activeTemplate?.nomeTemplate ?: "Template"}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "Insira o responsável por cada uma das etapas do culto descritas no template:",
                fontSize = 13.sp,
                color = Color.Gray
            )

            if (activeTemplate == null) {
                Text("Template não encontrado!")
            } else {
                val stepsList = remember(activeTemplate.etapasJson) {
                    val list = mutableListOf<Pair<String, Int>>()
                    try {
                        val arr = JSONArray(activeTemplate.etapasJson)
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            list.add(o.getString("nome") to o.getInt("duracao"))
                        }
                    } catch (e: Exception) {}
                    list
                }

                stepsList.forEach { (stepName, dur) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "$stepName ($dur min)", fontWeight = FontWeight.Bold)
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.LightGray)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = answers[stepName] ?: "",
                                onValueChange = { ans ->
                                    val map = answers.toMutableMap()
                                    map[stepName] = ans
                                    answers = map
                                },
                                placeholder = { Text("Nome do responsável") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
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
fun CultoModeScreen(
    escala: Escala,
    viewModel: AppViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val templatesList by viewModel.templates.collectAsState()
    val activeTemplate = remember(templatesList) {
        templatesList.find { it.uuid == escala.templateUuid }
    }

    // Parse template stages Name & Duration (duration in seconds)
    val stages = remember(activeTemplate) {
        val list = mutableListOf<StageEntry>()
        if (activeTemplate != null) {
            try {
                val arr = JSONArray(activeTemplate.etapasJson)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(StageEntry(o.getString("nome"), o.getInt("duracao") * 60))
                }
            } catch (e: Exception) {}
        }
        if (list.isEmpty()) {
            list.add(StageEntry("Exemplo Entrada", 30))
        }
        list
    }

    // Parse responsibles
    val responsaveis = remember(escala) {
        val map = mutableMapOf<String, String>()
        try {
            val o = JSONObject(escala.responsaveisJson)
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = o.getString(k)
            }
        } catch (e: Exception) {}
        map
    }

    var currentStageIndex by remember { mutableIntStateOf(0) }
    var currentStageElapsed by remember { mutableIntStateOf(0) } // Elapsed in seconds
    var totalServiceElapsed by remember { mutableIntStateOf(0) } // Entire service elapsed in seconds
    var isTimerPlaying by remember { mutableStateOf(true) }

    val currentStage = stages.getOrNull(currentStageIndex) ?: stages.first()
    val stageDurationSeconds = currentStage.durationSeconds
    val currentResponsible = responsaveis[currentStage.name] ?: "Nenhum responsável"

    // Timer Loop
    LaunchedEffect(isTimerPlaying, currentStageIndex) {
        while (isTimerPlaying) {
            delay(1000L)
            currentStageElapsed++
            totalServiceElapsed++

            // Edge condition alert check vibration: when stage time is exactly reached/exceeded
            if (currentStageElapsed == stageDurationSeconds + 1) {
                triggerVibrate(context)
            }
        }
    }

    val totalTimeLimitSeconds = remember(stages) {
        stages.sumOf { it.durationSeconds }
    }

    Scaffold(
        containerColor = Color(0xFF1E1E1E) // Absolute pulpit dark background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White)
                }
                Text("MODO CULTO ATIVO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                IconButton(onClick = { isTimerPlaying = !isTimerPlaying }) {
                    Icon(
                        imageVector = if (isTimerPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            // Top Linear Progress for the entire Service
            Spacer(modifier = Modifier.height(12.dp))
            val overallProgress = remember(totalServiceElapsed, totalTimeLimitSeconds) {
                (totalServiceElapsed.toFloat() / totalTimeLimitSeconds.toFloat()).coerceIn(0f, 1f)
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tempo Total Culto Decorrido: ${formatSeconds(totalServiceElapsed)} / ${formatSeconds(totalTimeLimitSeconds)}",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                LinearProgressIndicator(
                    progress = { overallProgress },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.DarkGray,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Stage Header
            Text(
                text = "ETAPA LITÚRGICA ATUAL:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.LightGray
            )
            Text(
                text = currentStage.name.uppercase(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Responsável: $currentResponsible",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Massive Circular Cronometro
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                val isTimeOver = currentStageElapsed > stageDurationSeconds
                val progress = remember(currentStageElapsed, stageDurationSeconds) {
                    (currentStageElapsed.toFloat() / stageDurationSeconds.toFloat()).coerceIn(0f, 1f)
                }

                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    color = if (isTimeOver) Color.Red else MaterialTheme.colorScheme.primary,
                    trackColor = Color.DarkGray.copy(alpha = 0.5f)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatSeconds(currentStageElapsed),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isTimeOver) Color.Red else Color.White
                    )
                    Text(
                        text = "Limite: ${formatSeconds(stageDurationSeconds)}",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    if (isTimeOver) {
                        Text(
                            text = "TEMPO EXCEDIDO",
                            color = Color.Red,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Stage controller buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Prev step
                Button(
                    onClick = {
                        if (currentStageIndex > 0) {
                            currentStageIndex--
                            currentStageElapsed = 0
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    enabled = currentStageIndex > 0
                ) {
                    Text("Anterior")
                }

                // Next step
                Button(
                    onClick = {
                        if (currentStageIndex < stages.size - 1) {
                            currentStageIndex++
                            currentStageElapsed = 0
                        } else {
                            isTimerPlaying = false
                            Toast.makeText(context, "Fim do Culto!", Toast.LENGTH_LONG).show()
                            onClose()
                        }
                    },
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStageIndex < stages.size - 1) MaterialTheme.colorScheme.primary else Color(0xFFC8E6C9)
                    )
                ) {
                    Text(
                        text = if (currentStageIndex < stages.size - 1) "Mudar Etapa" else "Encerrar Culto",
                        color = if (currentStageIndex < stages.size - 1) Color.White else Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class StageEntry(val name: String, val durationSeconds: Int)

private fun formatSeconds(total: Int): String {
    val m = total / 60
    val s = total % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

private fun triggerVibrate(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500L)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
