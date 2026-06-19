package com.example.models

import androidx.room.*

@Entity(tableName = "pastor_esbocos")
data class Esboco(
    @PrimaryKey val uuid: String,
    val titulo: String,
    @ColumnInfo(name = "texto_esboco") val textoEsboco: String,
    @ColumnInfo(name = "livro_biblia") val livroBiblia: String,
    val status: String, // 'Rascunho' or 'Finalizado'
    @ColumnInfo(name = "data_criacao") val dataCriacao: Long,
    @ColumnInfo(name = "tempo_estimado") val tempoEstimado: Int
)

@Entity(
    tableName = "pastor_versoes",
    foreignKeys = [
        ForeignKey(
            entity = Esboco::class,
            parentColumns = ["uuid"],
            childColumns = ["esboco_uuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["esboco_uuid"])]
)
data class EsbocoVersiculo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "esboco_uuid") val esbocoUuid: String,
    val referencia: String,
    @ColumnInfo(name = "texto_versiculo") val textoVersiculo: String
)
