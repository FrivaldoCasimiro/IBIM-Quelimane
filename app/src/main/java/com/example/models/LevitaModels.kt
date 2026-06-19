package com.example.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "levita_musicas")
data class Musica(
    @PrimaryKey val uuid: String,
    val titulo: String,
    val tom: String,
    val letra: String,
    val cifras: String,
    val categoria: String // 'Entrada', 'Ofertório', etc.
)

@Entity(tableName = "levita_setlists")
data class Setlist(
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "nome_culto") val nomeCulto: String,
    @ColumnInfo(name = "data_culto") val dataCulto: Long,
    @ColumnInfo(name = "ordem_musicas_uuids") val ordemMusicasUuids: String // Comma-separated UUIDs
)
