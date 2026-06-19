package com.example.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dirigente_templates")
data class Template(
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "nome_template") val nomeTemplate: String,
    @ColumnInfo(name = "etapas_json") val etapasJson: String // [{"nome":"Acolhida","duracao":5}, ...]
)

@Entity(tableName = "dirigente_escalas")
data class Escala(
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "data_culto") val dataCulto: Long,
    @ColumnInfo(name = "template_uuid") val templateUuid: String,
    @ColumnInfo(name = "responsaveis_json") val responsaveisJson: String // {"Acolhida":"João", ...}
)
