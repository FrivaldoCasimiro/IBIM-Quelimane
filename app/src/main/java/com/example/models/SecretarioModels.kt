package com.example.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secretario_avisos")
data class Aviso(
    @PrimaryKey val uuid: String,
    val titulo: String,
    val conteudo: String,
    @ColumnInfo(name = "data_inicio") val dataInicio: Long,
    @ColumnInfo(name = "data_fim") val dataFim: Long,
    @ColumnInfo(name = "ativo") val ativo: Int // 0 or 1
)

@Entity(tableName = "secretario_membros")
data class Membro(
    @PrimaryKey val uuid: String,
    val nome: String,
    val telefone: String,
    val endereco: String,
    val aniversario: Long,
    val cargo: String
)

@Entity(tableName = "secretario_eventos")
data class Evento(
    @PrimaryKey val uuid: String,
    val titulo: String,
    @ColumnInfo(name = "data_evento") val dataEvento: Long,
    val descricao: String
)
