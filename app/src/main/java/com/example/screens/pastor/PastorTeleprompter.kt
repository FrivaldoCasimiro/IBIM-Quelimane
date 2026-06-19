package com.example.screens.pastor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Esboco
import com.example.providers.AppViewModel
import kotlinx.coroutines.delay

@Composable
fun PastorTeleprompter(
    viewModel: AppViewModel
) {
    val esboços by viewModel.esbocos.collectAsState()
    val finalizados = remember(esboços) {
        esboços.filter { it.status == "Finalizado" }
    }

    var selectedEsboco by remember { mutableStateOf<Esboco?>(null) }

    if (selectedEsboco != null) {
        TeleprompterFullScreen(
            esboco = selectedEsboco!!,
            onClose = { selectedEsboco = null }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = "Teleprompter de Sermões",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Selecione um esboço finalizado para iniciar a leitura no púpito com rolagem automática:",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (finalizados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Abas de rascunhos vazias.", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Para aparecer aqui, crie um sermão na aba 'Esboços' e salve-o com o status 'Finalizado'.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(finalizados) { esb ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedEsboco = esb },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = esb.titulo,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Livro focal: ${esb.livroBiblia} • ${esb.tempoEstimado} min",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Iniciar",
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
}

@Composable
fun TeleprompterFullScreen(
    esboco: Esboco,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isScrolling by remember { mutableStateOf(false) }
    var scrollSpeedMs by remember { mutableStateOf(45L) } // higher means slower scroll

    // Core rolagem automática Loop
    LaunchedEffect(isScrolling, scrollSpeedMs) {
        if (isScrolling) {
            while (scrollState.value < scrollState.maxValue && isScrolling) {
                scrollState.scrollTo(scrollState.value + 1)
                delay(scrollSpeedMs)
            }
            if (scrollState.value >= scrollState.maxValue) {
                isScrolling = false
            }
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E)) // Elegant absolute dark screen for readability
                .padding(innerPadding)
        ) {
            // Header bar for controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Return button
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                }

                // Title
                Text(
                    text = esboco.titulo,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )

                // Speeds toggle and controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isScrolling) "ROLAGEM ATIVA" else "PAUSADO",
                        color = if (isScrolling) Color(0xFF0FB1AC) else Color.Yellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    IconButton(onClick = {
                        scrollSpeedMs = when (scrollSpeedMs) {
                            60L -> 45L // Normal
                            45L -> 30L // Fast
                            30L -> 15L // Very Fast
                            else -> 60L // Slow
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Velocidade",
                            tint = Color.White
                        )
                    }
                }
            }

            // Quick instruction bar
            Text(
                text = "Dica: Toque no corpo do texto para pausar ou retomar a rolagem.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(bottom = 6.dp)
            )

            // Scrolling text body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { isScrolling = !isScrolling }
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(vertical = 100.dp) // Generous top/bottom margin for focus reading
                ) {
                    // Title in display
                    Text(
                        text = esboco.titulo,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Splitting into lines to check verses format: [João 3:16: "..."]
                    val lines = esboco.textoEsboco.split("\n")
                    lines.forEach { line ->
                        if (line.trim().startsWith("[") && line.trim().contains(": \"")) {
                            // Render as custom yellow verse block
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFFF59D)) // Prominent soft yellow
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = line,
                                    color = Color.Black,
                                    fontSize = 22.sp, // Larger font size
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 28.sp
                                )
                            }
                        } else {
                            // Standard line
                            if (line.isNotBlank()) {
                                Text(
                                    text = line,
                                    color = Color.White,
                                    fontSize = 20.sp, // standard legible reading text
                                    lineHeight = 28.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                    Text(
                        text = "— FIM DO SERMÃO —",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
