package com.example.database

import com.example.models.*
import kotlinx.coroutines.flow.Flow

class PastorRepository(private val esbocoDao: EsbocoDao) {
    val allEsbocos: Flow<List<Esboco>> = esbocoDao.getAllEsbocos()

    suspend fun getEsbocoById(uuid: String): Esboco? = esbocoDao.getEsbocoById(uuid)
    suspend fun insertEsboco(esboco: Esboco) = esbocoDao.insertEsboco(esboco)
    suspend fun deleteEsboco(esboco: Esboco) = esbocoDao.deleteEsboco(esboco)

    fun getVersiculosForEsboco(esbocoUuid: String): Flow<List<EsbocoVersiculo>> = 
        esbocoDao.getVersiculosForEsboco(esbocoUuid)
    suspend fun insertEsbocoVersiculo(v: EsbocoVersiculo) = esbocoDao.insertEsbocoVersiculo(v)
    suspend fun deleteVersiculosForEsboco(esbocoUuid: String) = esbocoDao.deleteVersiculosForEsboco(esbocoUuid)
}

class LevitaRepository(private val levitaDao: LevitaDao) {
    val allMusicas: Flow<List<Musica>> = levitaDao.getAllMusicas()
    val allSetlists: Flow<List<Setlist>> = levitaDao.getAllSetlists()

    suspend fun getMusicaById(uuid: String): Musica? = levitaDao.getMusicaById(uuid)
    suspend fun insertMusica(musica: Musica) = levitaDao.insertMusica(musica)
    suspend fun deleteMusica(musica: Musica) = levitaDao.deleteMusica(musica)

    suspend fun getSetlistById(uuid: String): Setlist? = levitaDao.getSetlistById(uuid)
    suspend fun insertSetlist(setlist: Setlist) = levitaDao.insertSetlist(setlist)
    suspend fun deleteSetlist(setlist: Setlist) = levitaDao.deleteSetlist(setlist)
}

class SecretarioRepository(private val secretarioDao: SecretarioDao) {
    val allAvisos: Flow<List<Aviso>> = secretarioDao.getAllAvisos()
    val allMembros: Flow<List<Membro>> = secretarioDao.getAllMembros()
    val allEventos: Flow<List<Evento>> = secretarioDao.getAllEventos()

    fun searchMembros(query: String): Flow<List<Membro>> = secretarioDao.searchMembros(query)
    suspend fun insertAviso(aviso: Aviso) = secretarioDao.insertAviso(aviso)
    suspend fun deleteAviso(aviso: Aviso) = secretarioDao.deleteAviso(aviso)

    suspend fun insertMembro(membro: Membro) = secretarioDao.insertMembro(membro)
    suspend fun deleteMembro(membro: Membro) = secretarioDao.deleteMembro(membro)

    suspend fun insertEvento(evento: Evento) = secretarioDao.insertEvento(evento)
    suspend fun deleteEvento(evento: Evento) = secretarioDao.deleteEvento(evento)
    suspend fun getEventosFromDate(minTime: Long): List<Evento> = secretarioDao.getEventosFromDate(minTime)
}

class DirigenteRepository(private val dirigenteDao: DirigenteDao) {
    val allTemplates: Flow<List<Template>> = dirigenteDao.getAllTemplates()
    val allEscalas: Flow<List<Escala>> = dirigenteDao.getAllEscalas()

    suspend fun getTemplateById(uuid: String): Template? = dirigenteDao.getTemplateById(uuid)
    suspend fun insertTemplate(template: Template) = dirigenteDao.insertTemplate(template)
    suspend fun deleteTemplate(template: Template) = dirigenteDao.deleteTemplate(template)

    suspend fun getEscalaById(uuid: String): Escala? = dirigenteDao.getEscalaById(uuid)
    suspend fun insertEscala(escala: Escala) = dirigenteDao.insertEscala(escala)
    suspend fun deleteEscala(escala: Escala) = dirigenteDao.deleteEscala(escala)
}

class BibliaRepository(private val bibliaDao: BibliaDao) {
    val allLivros: Flow<List<BibliaLivro>> = bibliaDao.getAllLivros()
    suspend fun getAllLivrosList(): List<BibliaLivro> = bibliaDao.getAllLivrosList()

    fun getVersiculos(livroId: Int, capitulo: Int): Flow<List<BibliaVersiculo>> = 
        bibliaDao.getVersiculos(livroId, capitulo)

    suspend fun searchVersiculos(query: String): List<BibliaVersiculo> = 
        bibliaDao.searchVersiculos(query)

    suspend fun getVersiculoByRef(bookName: String, chapter: Int, verse: Int): BibliaVersiculo? = 
        bibliaDao.getVersiculoByRef(bookName, chapter, verse)

    suspend fun getVersiculosCount(): Int = bibliaDao.getVersiculosCount()
    suspend fun clearAllVersiculos() = bibliaDao.clearAllVersiculos()
    suspend fun insertVerses(verses: List<BibliaVersiculo>) = bibliaDao.insertVerses(verses)
}
