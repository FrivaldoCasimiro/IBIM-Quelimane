package com.example.screens.splash

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.providers.AppViewModel

@Composable
fun SplashScreen(
    viewModel: AppViewModel,
    onNavigateToProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper Content Container
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Church Logo (Teal `#0FB1AC` rounded-2xl Box with a white church representation)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(6.dp, shape = RoundedCornerShape(20.dp))
                    .background(Color(0xFF0FB1AC), shape = RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(44.dp)) {
                    val strokeWidthPx = 2.5.dp.toPx()
                    val path = Path().apply {
                        // Roof: M3 9l9-6 9 6
                        moveTo(size.width * 0.125f, size.height * 0.45f)
                        lineTo(size.width * 0.5f, size.height * 0.2f)
                        lineTo(size.width * 0.875f, size.height * 0.45f)
                        // Walls: goes down
                        lineTo(size.width * 0.8125f, size.height * 0.85f)
                        lineTo(size.width * 0.1875f, size.height * 0.85f)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(
                            width = strokeWidthPx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    // Mid-Door pillar
                    drawLine(
                        color = Color.White,
                        start = Offset(size.width * 0.5f, size.height * 0.85f),
                        end = Offset(size.width * 0.5f, size.height * 0.55f),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                    // Left door side
                    drawLine(
                        color = Color.White,
                        start = Offset(size.width * 0.35f, size.height * 0.85f),
                        end = Offset(size.width * 0.35f, size.height * 0.55f),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                    // Right door side
                    drawLine(
                        color = Color.White,
                        start = Offset(size.width * 0.65f, size.height * 0.85f),
                        end = Offset(size.width * 0.65f, size.height * 0.55f),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                    // Small cross on top
                    drawLine(
                        color = Color.White,
                        start = Offset(size.width * 0.5f, size.height * 0.18f),
                        end = Offset(size.width * 0.5f, size.height * 0.04f),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(size.width * 0.4f, size.height * 0.09f),
                        end = Offset(size.width * 0.6f, size.height * 0.09f),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Title
            Text(
                text = "IBI Quelimane",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF205F55), // Deep Moss Green
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )

            // Subtitle
            Text(
                text = "SISTEMA DE GESTÃO ECLESIÁSTICA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF404040),
                textAlign = TextAlign.Center,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            Text(
                text = "Escolha o seu perfil de atuação:",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF404040),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Rows of profile cards to avoid nested scroll views
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ProfileCard(
                        emoji = "🕊️",
                        name = "Pastor",
                        description = "Esboços e Bíblia",
                        onClick = {
                            viewModel.selectPerfil("pastor")
                            onNavigateToProfile()
                        }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ProfileCard(
                        emoji = "🎵",
                        name = "Levita",
                        description = "Cifras e Setlists",
                        onClick = {
                            viewModel.selectPerfil("levita")
                            onNavigateToProfile()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ProfileCard(
                        emoji = "📋",
                        name = "Secretário",
                        description = "Membros e Avisos",
                        onClick = {
                            viewModel.selectPerfil("secretario")
                            onNavigateToProfile()
                        }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ProfileCard(
                        emoji = "🎤",
                        name = "Dirigente",
                        description = "Escalas e Culto",
                        onClick = {
                            viewModel.selectPerfil("dirigente")
                            onNavigateToProfile()
                        }
                    )
                }
            }
        }

        // Bottom Offline Banner Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, shape = RoundedCornerShape(12.dp))
                    .background(Color(0xFF205F55), shape = RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Green pulsing-like dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF4ADE80), shape = RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Modo Offline Ativo",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Banco SQLite Local (API 21+)",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Modo Offline Ativo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCard(
    emoji: String,
    name: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
            .shadow(1.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFF0FB1AC).copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 26.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black
            )
            Text(
                text = description,
                fontSize = 10.sp,
                color = Color(0xFF404040),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

