package com.example.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "biblia_livros")
data class BibliaLivro(
    @PrimaryKey val id: Int,
    val nome: String,
    val abreviacao: String,
    val testamento: String // 'VT' or 'NT'
)

@Entity(tableName = "biblia_versiculos")
data class BibliaVersiculo(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "livro_id") val livroId: Int,
    val capitulo: Int,
    val numero: Int,
    val texto: String
)
