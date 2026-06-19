package com.example.providers

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.*
import com.example.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.io.File

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("ibi_prefs", Context.MODE_PRIVATE)
    
    // Perfil active view state
    private val _perfilAtuante = MutableStateFlow<String?>(null)
    val perfilAtuante: StateFlow<String?> = _perfilAtuante.asStateFlow()

    // Room Database init
    private var database: MainDatabase = MainDatabase.getDatabase(application)
    
    // Repositories
    private val pastorRepository = PastorRepository(database.esbocoDao())
    private val levitaRepository = LevitaRepository(database.levitaDao())
    private val secretarioRepository = SecretarioRepository(database.secretarioDao())
    private val dirigenteRepository = DirigenteRepository(database.dirigenteDao())
    private val bibliaRepository = BibliaRepository(database.bibliaDao())

    // Exposed Flows for UI Reactive Observation
    val esbocos: StateFlow<List<Esboco>> = pastorRepository.allEsbocos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val musicas: StateFlow<List<Musica>> = levitaRepository.allMusicas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val setlists: StateFlow<List<Setlist>> = levitaRepository.allSetlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val avisos: StateFlow<List<Aviso>> = secretarioRepository.allAvisos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val membros: StateFlow<List<Membro>> = secretarioRepository.allMembros
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val eventos: StateFlow<List<Evento>> = secretarioRepository.allEventos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<Template>> = dirigenteRepository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val escalas: StateFlow<List<Escala>> = dirigenteRepository.allEscalas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val livros: StateFlow<List<BibliaLivro>> = bibliaRepository.allLivros
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Load default perfil from SharedPreferences
        val savedPerfil = sharedPreferences.getString("perfil_atuante", null)
        _perfilAtuante.value = savedPerfil
    }

    // Theme Preference State: "system", "light", "dark"
    private val _themePreference = MutableStateFlow(sharedPreferences.getString("theme_preference", "system") ?: "system")
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    fun setThemePreference(preference: String) {
        sharedPreferences.edit().putString("theme_preference", preference).apply()
        _themePreference.value = preference
    }

    // Refresh database instance after import
    fun refreshDatabase() {
        database = MainDatabase.getDatabase(getApplication())
        // No explicit need to recreate repositories since they are delegates to DAOs, 
        // but let's make sure the DAOs are fresh
    }

    fun selectPerfil(perfil: String?) {
        sharedPreferences.edit().putString("perfil_atuante", perfil).apply()
        _perfilAtuante.value = perfil
    }

    // Pastor Operations
    fun saveEsboco(esboco: Esboco) {
        viewModelScope.launch(Dispatchers.IO) {
            pastorRepository.insertEsboco(esboco)
        }
    }

    fun deleteEsboco(esboco: Esboco) {
        viewModelScope.launch(Dispatchers.IO) {
            pastorRepository.deleteEsboco(esboco)
        }
    }

    fun getVersiculosForEsboco(esbocoUuid: String): Flow<List<EsbocoVersiculo>> {
        return pastorRepository.getVersiculosForEsboco(esbocoUuid)
    }

    fun saveEsbocoVersiculo(v: EsbocoVersiculo) {
        viewModelScope.launch(Dispatchers.IO) {
            pastorRepository.insertEsbocoVersiculo(v)
        }
    }

    fun deleteVersiculosForEsboco(esbocoUuid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            pastorRepository.deleteVersiculosForEsboco(esbocoUuid)
        }
    }

    suspend fun findWordInBible(bookName: String, chapter: Int, verse: Int): BibliaVersiculo? {
        return bibliaRepository.getVersiculoByRef(bookName, chapter, verse)
    }

    // Levita Operations
    fun saveMusica(musica: Musica) {
        viewModelScope.launch(Dispatchers.IO) {
            levitaRepository.insertMusica(musica)
        }
    }

    fun deleteMusica(musica: Musica) {
        viewModelScope.launch(Dispatchers.IO) {
            levitaRepository.deleteMusica(musica)
        }
    }

    fun saveSetlist(setlist: Setlist) {
        viewModelScope.launch(Dispatchers.IO) {
            levitaRepository.insertSetlist(setlist)
        }
    }

    fun deleteSetlist(setlist: Setlist) {
        viewModelScope.launch(Dispatchers.IO) {
            levitaRepository.deleteSetlist(setlist)
        }
    }

    // Secretario Operations
    fun saveAviso(aviso: Aviso) {
        viewModelScope.launch(Dispatchers.IO) {
            secretarioRepository.insertAviso(aviso)
        }
    }

    fun deleteAviso(aviso: Aviso) {
        viewModelScope.launch(Dispatchers.IO) {
            secretarioRepository.deleteAviso(aviso)
        }
    }

    fun saveMembro(membro: Membro) {
        viewModelScope.launch(Dispatchers.IO) {
            secretarioRepository.insertMembro(membro)
        }
    }

    fun deleteMembro(membro: Membro) {
        viewModelScope.launch(Dispatchers.IO) {
            secretarioRepository.deleteMembro(membro)
        }
    }

    fun filterMembros(query: String): Flow<List<Membro>> {
        return if (query.isEmpty()) {
            secretarioRepository.allMembros
        } else {
            secretarioRepository.searchMembros(query)
        }
    }

    fun saveEvento(evento: Evento) {
        viewModelScope.launch(Dispatchers.IO) {
            secretarioRepository.insertEvento(evento)
        }
    }

    fun deleteEvento(evento: Evento) {
        viewModelScope.launch(Dispatchers.IO) {
            secretarioRepository.deleteEvento(evento)
        }
    }

    suspend fun getUpcomingEventos(): List<Evento> {
        val nextWeek = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
        return secretarioRepository.getEventosFromDate(System.currentTimeMillis()).filter {
            it.dataEvento <= nextWeek
        }
    }

    // Dirigente Operations
    fun saveTemplate(template: Template) {
        viewModelScope.launch(Dispatchers.IO) {
            dirigenteRepository.insertTemplate(template)
        }
    }

    fun deleteTemplate(template: Template) {
        viewModelScope.launch(Dispatchers.IO) {
            dirigenteRepository.deleteTemplate(template)
        }
    }

    fun saveEscala(escala: Escala) {
        viewModelScope.launch(Dispatchers.IO) {
            dirigenteRepository.insertEscala(escala)
        }
    }

    fun deleteEscala(escala: Escala) {
        viewModelScope.launch(Dispatchers.IO) {
            dirigenteRepository.deleteEscala(escala)
        }
    }

    suspend fun getTemplateById(uuid: String): Template? {
        return dirigenteRepository.getTemplateById(uuid)
    }

    // Bible Reading Operations
    fun getVersiculosOfChapter(livroId: Int, capitulo: Int): Flow<List<BibliaVersiculo>> {
        return bibliaRepository.getVersiculos(livroId, capitulo)
    }

    suspend fun getLivrosList(): List<BibliaLivro> {
        return bibliaRepository.getAllLivrosList()
    }

    // Bible Download State
    private val _bibleDownloadState = MutableStateFlow<BibleDownloadState>(BibleDownloadState.Idle)
    val bibleDownloadState: StateFlow<BibleDownloadState> = _bibleDownloadState.asStateFlow()

    fun checkAndTriggerBibleDownload() {
        viewModelScope.launch(Dispatchers.IO) {
            _bibleDownloadState.value = BibleDownloadState.Checking
            try {
                val count = bibliaRepository.getVersiculosCount()
                if (count < 100) { // Default setup has only 7 verses
                    _bibleDownloadState.value = BibleDownloadState.NeedsDownload
                } else {
                    _bibleDownloadState.value = BibleDownloadState.Success
                }
            } catch (e: Exception) {
                _bibleDownloadState.value = BibleDownloadState.Error("Erro ao verificar banco de dados: ${e.message}")
            }
        }
    }

    fun startBibleDownload() {
        viewModelScope.launch(Dispatchers.IO) {
            _bibleDownloadState.value = BibleDownloadState.Downloading
            
            val urls = listOf(
                "https://raw.githubusercontent.com/alanmarcell/biblia-nvi-json/master/nvi.json",
                "https://cdn.jsdelivr.net/gh/alanmarcell/biblia-nvi-json@master/nvi.json",
                "https://raw.githubusercontent.com/mario-medeiros/biblia-pt-json/master/biblias/nvi.json",
                "https://raw.githubusercontent.com/renatocandido/biblia-nvi/master/biblia_nvi.json"
            )
            
            var success = false
            var errorMsg = "Não foi possível conectar com nenhum servidor."
            
            for (url in urls) {
                try {
                    val connection = URL(url).openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 20000
                    connection.requestMethod = "GET"
                    
                    if (connection.responseCode == 200) {
                        val reader = connection.inputStream.bufferedReader()
                        val jsonStr = reader.use { it.readText() }
                        
                        _bibleDownloadState.value = BibleDownloadState.Saving
                        
                        // Parse and Save in Transactions
                        parseAndSaveBible(jsonStr)
                        success = true
                        break
                    } else {
                        errorMsg = "Servidor respondeu com código ${connection.responseCode}"
                    }
                } catch (e: Exception) {
                    errorMsg = e.message ?: e.toString()
                }
            }
            
            if (success) {
                _bibleDownloadState.value = BibleDownloadState.Success
            } else {
                _bibleDownloadState.value = BibleDownloadState.Error("Erro no download: $errorMsg")
            }
        }
    }

    private suspend fun parseAndSaveBible(jsonStr: String) {
        withContext(Dispatchers.IO) {
            try {
                // Clear existing verses first
                bibliaRepository.clearAllVersiculos()
                
                val rootArray = JSONArray(jsonStr)
                val chunkList = mutableListOf<BibliaVersiculo>()
                var verseGlobalId = 1
                
                for (i in 0 until rootArray.length()) {
                    val bookObj = rootArray.getJSONObject(i)
                    
                    val bookId = i + 1 // Protestant order: 1 - 66
                    
                    val chaptersField = when {
                        bookObj.has("chapters") -> "chapters"
                        bookObj.has("capitulos") -> "capitulos"
                        else -> null
                    }
                    
                    if (chaptersField != null) {
                        val chaptersArray = bookObj.getJSONArray(chaptersField)
                        for (c in 0 until chaptersArray.length()) {
                            val chapterNum = c + 1
                            val versesArray = chaptersArray.getJSONArray(c)
                            for (v in 0 until versesArray.length()) {
                                val verseNum = v + 1
                                val verseText = versesArray.getString(v)
                                
                                chunkList.add(
                                    BibliaVersiculo(
                                        id = verseGlobalId++,
                                        livroId = bookId,
                                        capitulo = chapterNum,
                                        numero = verseNum,
                                        texto = verseText
                                    )
                                )
                                
                                // Insert in batches to prevent SQLite transaction size/memory limits (e.g. 1000 records per insert)
                                if (chunkList.size >= 1000) {
                                    bibliaRepository.insertVerses(chunkList.toList())
                                    chunkList.clear()
                                }
                            }
                        }
                    }
                }
                
                // Insert any remaining verses
                if (chunkList.isNotEmpty()) {
                    bibliaRepository.insertVerses(chunkList.toList())
                }
            } catch (e: Exception) {
                _bibleDownloadState.value = BibleDownloadState.Error("Erro ao salvar Bíblia: ${e.message}")
                throw e
            }
        }
    }
}

sealed class BibleDownloadState {
    object Idle : BibleDownloadState()
    object Checking : BibleDownloadState()
    object NeedsDownload : BibleDownloadState()
    object Downloading : BibleDownloadState()
    object Saving : BibleDownloadState()
    object Success : BibleDownloadState()
    data class Error(val message: String) : BibleDownloadState()
}
