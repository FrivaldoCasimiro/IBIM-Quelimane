package com.example.widgets

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.BackupService
import com.example.providers.AppViewModel

@Composable
fun CustomDrawerContent(
    profileName: String,
    viewModel: AppViewModel,
    onClose: () -> Unit,
    onTrocarPerfil: () -> Unit
) {
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }

    // Launcher for file picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            BackupService.importBackup(context, uri, {
                viewModel.refreshDatabase()
                Toast.makeText(context, "Backup importado com SUCESSO! Dados atualizados.", Toast.LENGTH_LONG).show()
                onClose()
                onTrocarPerfil() // Redirect to splash to fully refresh
            }, { error ->
                Toast.makeText(context, "Erro ao importar backup: $error", Toast.LENGTH_LONG).show()
            })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        // App Title
        Text(
            text = "IBI Quelimane",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Divider
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        
        Spacer(modifier = Modifier.height(16.dp))

        // Profile Badge Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (profileName.lowercase()) {
                        "pastor" -> Icons.Default.Face
                        "levita" -> Icons.Default.MusicNote
                        "secretário" -> Icons.Default.ContactPhone
                        else -> Icons.Default.Mic
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Perfil Ativo",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = profileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // MenuItem - Trocar Perfil
        DrawerItem(
            icon = Icons.Default.SwitchAccount,
            label = "Trocar Perfil",
            onClick = {
                onClose()
                onTrocarPerfil()
            }
        )

        // MenuItem - Exportar Backup
        DrawerItem(
            icon = Icons.Default.Backup,
            label = "Exportar Dados (.ibi)",
            onClick = {
                BackupService.exportBackup(context, { path ->
                    Toast.makeText(context, "Backup exportado para: $path", Toast.LENGTH_LONG).show()
                }, { error ->
                    Toast.makeText(context, "Erro ao exportar backup: $error", Toast.LENGTH_LONG).show()
                })
                onClose()
            }
        )

        // MenuItem - Importar Backup
        DrawerItem(
            icon = Icons.Default.SettingsBackupRestore,
            label = "Importar Dados (.ibi)",
            onClick = {
                filePickerLauncher.launch("*/*")
            }
        )

        // Theme Toggle Section
        val currentThemePref by viewModel.themePreference.collectAsState()
        
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "TEMA DO DISPOSITIVO (CULTOS À NOITE)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    val nextTheme = if (currentThemePref == "dark") "light" else "dark"
                    viewModel.setThemePreference(nextTheme)
                }
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (currentThemePref == "dark") Icons.Default.Info else Icons.Default.SwitchAccount,
                    contentDescription = null,
                    tint = if (currentThemePref == "dark") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Modo Noturno",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Switch(
                checked = currentThemePref == "dark",
                onCheckedChange = { isDark ->
                    viewModel.setThemePreference(if (isDark) "dark" else "light")
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
                modifier = Modifier.scale(0.85f)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(8.dp))

        // MenuItem - Sobre
        DrawerItem(
            icon = Icons.Default.Info,
            label = "Sobre",
            onClick = {
                showAboutDialog = true
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Version Info
        Text(
            text = "Versão 1.0.0 — 100% Offline",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("Ok")
                }
            },
            title = {
                Text(text = "Sobre o IBI Quelimane", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "IBI Quelimane é um sistema completo e 100% offline para o auxílio do cotidiano litúrgico e congregacional local.",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Desenvolvido com alta performance nativa, Jetpack Compose e banco SQLite seguro.",
                        fontSize = 14.sp
                    )
                }
            }
        )
    }
}

@Composable
fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
