package com.example.database

import androidx.room.*
import com.example.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EsbocoDao {
    @Query("SELECT * FROM pastor_esbocos ORDER BY data_criacao DESC")
    fun getAllEsbocos(): Flow<List<Esboco>>

    @Query("SELECT * FROM pastor_esbocos WHERE uuid = :uuid LIMIT 1")
    suspend fun getEsbocoById(uuid: String): Esboco?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEsboco(esboco: Esboco)

    @Delete
    suspend fun deleteEsboco(esboco: Esboco)

    @Query("SELECT * FROM pastor_versoes WHERE esboco_uuid = :esbocoUuid")
    fun getVersiculosForEsboco(esbocoUuid: String): Flow<List<EsbocoVersiculo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEsbocoVersiculo(versiculo: EsbocoVersiculo)

    @Query("DELETE FROM pastor_versoes WHERE esboco_uuid = :esbocoUuid")
    suspend fun deleteVersiculosForEsboco(esbocoUuid: String)
}

@Dao
interface LevitaDao {
    @Query("SELECT * FROM levita_musicas")
    fun getAllMusicas(): Flow<List<Musica>>

    @Query("SELECT * FROM levita_musicas WHERE uuid = :uuid LIMIT 1")
    suspend fun getMusicaById(uuid: String): Musica?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMusica(musica: Musica)

    @Delete
    suspend fun deleteMusica(musica: Musica)

    @Query("SELECT * FROM levita_setlists ORDER BY data_culto DESC")
    fun getAllSetlists(): Flow<List<Setlist>>

    @Query("SELECT * FROM levita_setlists WHERE uuid = :uuid LIMIT 1")
    suspend fun getSetlistById(uuid: String): Setlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetlist(setlist: Setlist)

    @Delete
    suspend fun deleteSetlist(setlist: Setlist)
}

@Dao
interface SecretarioDao {
    @Query("SELECT * FROM secretario_avisos ORDER BY data_inicio DESC")
    fun getAllAvisos(): Flow<List<Aviso>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAviso(aviso: Aviso)

    @Delete
    suspend fun deleteAviso(aviso: Aviso)

    @Query("SELECT * FROM secretario_membros ORDER BY nome ASC")
    fun getAllMembros(): Flow<List<Membro>>

    @Query("SELECT * FROM secretario_membros WHERE nome LIKE '%' || :query || '%' ORDER BY nome ASC")
    fun searchMembros(query: String): Flow<List<Membro>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembro(membro: Membro)

    @Delete
    suspend fun deleteMembro(membro: Membro)

    @Query("SELECT * FROM secretario_eventos ORDER BY data_evento ASC")
    fun getAllEventos(): Flow<List<Evento>>

    @Query("SELECT * FROM secretario_eventos WHERE data_evento >= :minTime ORDER BY data_evento ASC")
    suspend fun getEventosFromDate(minTime: Long): List<Evento>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvento(evento: Evento)

    @Delete
    suspend fun deleteEvento(evento: Evento)
}

@Dao
interface DirigenteDao {
    @Query("SELECT * FROM dirigente_templates")
    fun getAllTemplates(): Flow<List<Template>>

    @Query("SELECT * FROM dirigente_templates WHERE uuid = :uuid LIMIT 1")
    suspend fun getTemplateById(uuid: String): Template?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: Template)

    @Delete
    suspend fun deleteTemplate(template: Template)

    @Query("SELECT * FROM dirigente_escalas ORDER BY data_culto DESC")
    fun getAllEscalas(): Flow<List<Escala>>

    @Query("SELECT * FROM dirigente_escalas WHERE uuid = :uuid LIMIT 1")
    suspend fun getEscalaById(uuid: String): Escala?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEscala(escala: Escala)

    @Delete
    suspend fun deleteEscala(escala: Escala)
}

@Dao
interface BibliaDao {
    @Query("SELECT * FROM biblia_livros ORDER BY id ASC")
    fun getAllLivros(): Flow<List<BibliaLivro>>

    @Query("SELECT * FROM biblia_livros ORDER BY id ASC")
    suspend fun getAllLivrosList(): List<BibliaLivro>

    @Query("SELECT * FROM biblia_versiculos WHERE livro_id = :livroId AND capitulo = :capitulo ORDER BY numero ASC")
    fun getVersiculos(livroId: Int, capitulo: Int): Flow<List<BibliaVersiculo>>

    @Query("SELECT * FROM biblia_versiculos WHERE texto LIKE '%' || :query || '%' LIMIT 100")
    suspend fun searchVersiculos(query: String): List<BibliaVersiculo>

    @Query("SELECT COUNT(*) FROM biblia_versiculos")
    suspend fun getVersiculosCount(): Int

    @Query("DELETE FROM biblia_versiculos")
    suspend fun clearAllVersiculos()

    @Query("""
        SELECT v.* FROM biblia_versiculos v 
        INNER JOIN biblia_livros l ON v.livro_id = l.id 
        WHERE (UPPER(l.nome) = UPPER(:bookName) OR UPPER(l.abreviacao) = UPPER(:bookName)) 
          AND v.capitulo = :chapter 
          AND v.numero = :verse 
        LIMIT 1
    """)
    suspend fun getVersiculoByRef(bookName: String, chapter: Int, verse: Int): BibliaVersiculo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BibliaLivro)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<BibliaVersiculo>)
}
