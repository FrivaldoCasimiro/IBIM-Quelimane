package com.example.database

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupService {

    fun exportBackup(context: Context, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        try {
            // Close database first to ensure all transactions are written to disk
            MainDatabase.closeAndResetInstance()

            val dbFile = context.getDatabasePath("ibi_quelimane_database.db")
            if (!dbFile.exists()) {
                onError("Banco de dados não encontrado.")
                return
            }

            // Create temporal or local downloads directory
            val downloadsDir = context.getExternalFilesDir("Backups") ?: context.filesDir
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val backupFile = File(downloadsDir, "IBI_Quelimane_Backup_$timestamp.ibi")

            // Zip the DB
            ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                val entry = ZipEntry("ibi_quelimane_database.db")
                zos.putNextEntry(entry)
                FileInputStream(dbFile).use { fis ->
                    fis.copyTo(zos)
                }
                zos.closeEntry()
            }

            // Share the file
            shareBackupFile(context, backupFile)
            onSuccess(backupFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e.message ?: "Erro desconhecido ao exportar.")
        }
    }

    private fun shareBackupFile(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Backup IBI Quelimane")
            putExtra(Intent.EXTRA_TEXT, "Aqui está o arquivo de backup (.ibi) do IBI Quelimane.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(intent, "Exportar Backup IBI").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }

    fun importBackup(context: Context, fileUri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            // Close database before replacement
            MainDatabase.closeAndResetInstance()

            val dbFile = context.getDatabasePath("ibi_quelimane_database.db")
            
            // Unzip the selected file into a temporary file
            val tempDb = File(context.cacheDir, "ibi_temp.db")
            
            context.contentResolver.openInputStream(fileUri).use { inputStream ->
                if (inputStream == null) {
                    onError("Não foi possível ler o arquivo selecionado.")
                    return
                }

                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    var found = false
                    while (entry != null) {
                        if (entry.name == "ibi_quelimane_database.db") {
                            FileOutputStream(tempDb).use { fos ->
                                zis.copyTo(fos)
                            }
                            found = true
                            break
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }

                    if (!found) {
                        onError("O arquivo de backup não é válido para o IBI Quelimane.")
                        return
                    }
                }
            }

            // Overwrite database
            if (tempDb.exists() && tempDb.length() > 0) {
                if (dbFile.exists()) {
                    dbFile.delete()
                }
                tempDb.renameTo(dbFile)
                onSuccess()
            } else {
                onError("Arquivo de backup vazio ou inválido.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e.message ?: "Erro desconhecido ao importar.")
        }
    }
}
