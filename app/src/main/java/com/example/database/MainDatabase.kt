package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Esboco::class,
        EsbocoVersiculo::class,
        Musica::class,
        Setlist::class,
        Aviso::class,
        Membro::class,
        Evento::class,
        Template::class,
        Escala::class,
        BibliaLivro::class,
        BibliaVersiculo::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MainDatabase : RoomDatabase() {

    abstract fun esbocoDao(): EsbocoDao
    abstract fun levitaDao(): LevitaDao
    abstract fun secretarioDao(): SecretarioDao
    abstract fun dirigenteDao(): DirigenteDao
    abstract fun bibliaDao(): BibliaDao

    companion object {
        @Volatile
        private var INSTANCE: MainDatabase? = null

        fun getDatabase(context: Context): MainDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MainDatabase::class.java,
                    "ibi_quelimane_database.db"
                )
                .addCallback(DatabaseCreationCallback(context.applicationContext))
                .build()
                INSTANCE = instance
                instance
            }
        }

        // Exposed custom replacement for restoring backup simple file-system overwrite
        fun closeAndResetInstance() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                INSTANCE = null
            }
        }
    }

    private class DatabaseCreationCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = getDatabase(context)
                val bibliaDao = database.bibliaDao()

                // Insert Books
                val livros = listOf(
                    BibliaLivro(1, "Gênesis", "Gn", "VT"),
                    BibliaLivro(2, "Êxodo", "Êx", "VT"),
                    BibliaLivro(3, "Levítico", "Lv", "VT"),
                    BibliaLivro(4, "Números", "Nm", "VT"),
                    BibliaLivro(5, "Deuteronômio", "Dt", "VT"),
                    BibliaLivro(6, "Josué", "Js", "VT"),
                    BibliaLivro(7, "Juízes", "Jz", "VT"),
                    BibliaLivro(8, "Rute", "Rt", "VT"),
                    BibliaLivro(9, "1 Samuel", "1Sm", "VT"),
                    BibliaLivro(10, "2 Samuel", "2Sm", "VT"),
                    BibliaLivro(11, "1 Reis", "1Re", "VT"),
                    BibliaLivro(12, "2 Reis", "2Re", "VT"),
                    BibliaLivro(13, "1 Crônicas", "1Cr", "VT"),
                    BibliaLivro(14, "2 Crônicas", "2Cr", "VT"),
                    BibliaLivro(15, "Esdras", "Ed", "VT"),
                    BibliaLivro(16, "Neemias", "Ne", "VT"),
                    BibliaLivro(17, "Ester", "Et", "VT"),
                    BibliaLivro(18, "Jó", "Jó", "VT"),
                    BibliaLivro(19, "Salmos", "Sl", "VT"),
                    BibliaLivro(20, "Provérbios", "Pv", "VT"),
                    BibliaLivro(21, "Eclesiastes", "Ec", "VT"),
                    BibliaLivro(22, "Cânticos", "Ct", "VT"),
                    BibliaLivro(23, "Isaías", "Is", "VT"),
                    BibliaLivro(24, "Jeremias", "Jr", "VT"),
                    BibliaLivro(25, "Lamentações", "Lm", "VT"),
                    BibliaLivro(26, "Ezequiel", "Ez", "VT"),
                    BibliaLivro(27, "Daniel", "Dn", "VT"),
                    BibliaLivro(28, "Oseias", "Os", "VT"),
                    BibliaLivro(29, "Joel", "Jl", "VT"),
                    BibliaLivro(30, "Amós", "Am", "VT"),
                    BibliaLivro(31, "Obadias", "Ob", "VT"),
                    BibliaLivro(32, "Jonas", "Jon", "VT"),
                    BibliaLivro(33, "Miqueias", "Mq", "VT"),
                    BibliaLivro(34, "Naum", "Na", "VT"),
                    BibliaLivro(35, "Habacuque", "Hc", "VT"),
                    BibliaLivro(36, "Sofonias", "Sf", "VT"),
                    BibliaLivro(37, "Ageu", "Ag", "VT"),
                    BibliaLivro(38, "Zacarias", "Zc", "VT"),
                    BibliaLivro(39, "Malaquias", "Ml", "VT"),
                    BibliaLivro(40, "Mateus", "Mt", "NT"),
                    BibliaLivro(41, "Marcos", "Mc", "NT"),
                    BibliaLivro(42, "Lucas", "Lc", "NT"),
                    BibliaLivro(43, "João", "Jo", "NT"),
                    BibliaLivro(44, "Atos", "At", "NT"),
                    BibliaLivro(45, "Romanos", "Rm", "NT"),
                    BibliaLivro(46, "1 Coríntios", "1Co", "NT"),
                    BibliaLivro(47, "2 Coríntios", "2Co", "NT"),
                    BibliaLivro(48, "Gálatas", "Gl", "NT"),
                    BibliaLivro(49, "Efésios", "Ef", "NT"),
                    BibliaLivro(50, "Filipenses", "Fl", "NT"),
                    BibliaLivro(51, "Colossenses", "Cl", "NT"),
                    BibliaLivro(52, "1 Tessalonicenses", "1Ts", "NT"),
                    BibliaLivro(53, "2 Tessalonicenses", "2Ts", "NT"),
                    BibliaLivro(54, "1 Timóteo", "1Ti", "NT"),
                    BibliaLivro(55, "2 Timóteo", "2Ti", "NT"),
                    BibliaLivro(56, "Tito", "Tt", "NT"),
                    BibliaLivro(57, "Filemom", "Flm", "NT"),
                    BibliaLivro(58, "Hebreus", "Hb", "NT"),
                    BibliaLivro(59, "Tiago", "Tg", "NT"),
                    BibliaLivro(60, "1 Pedro", "1Pe", "NT"),
                    BibliaLivro(61, "2 Pedro", "2Pe", "NT"),
                    BibliaLivro(62, "1 João", "1Jo", "NT"),
                    BibliaLivro(63, "2 João", "2Jo", "NT"),
                    BibliaLivro(64, "3 João", "3Jo", "NT"),
                    BibliaLivro(65, "Judas", "Jud", "NT"),
                    BibliaLivro(66, "Apocalipse", "Ap", "NT")
                )

                for (l in livros) {
                    bibliaDao.insertBook(l)
                }

                // Insert Test Verses
                val versiculos = listOf(
                    BibliaVersiculo(1, 43, 3, 16, "Porque Deus amou o mundo de tal maneira que deu o seu Filho unigênito, para que todo aquele que nele crê não pereça, mas tenha a vida eterna."),
                    BibliaVersiculo(2, 45, 8, 28, "E sabemos que todas as coisas contribuem juntamente para o bem daqueles que amam a Deus, daqueles que são chamados segundo o seu propósito."),
                    BibliaVersiculo(3, 49, 2, 8, "Porque pela graça sois salvos, por meio da fé; e isto não vem de vós, é dom de Deus."),
                    BibliaVersiculo(4, 50, 4, 13, "Posso todas as coisas naquele que me fortalece."),
                    BibliaVersiculo(5, 19, 23, 1, "O Senhor é o meu pastor, nada me faltará."),
                    BibliaVersiculo(6, 46, 13, 1, "Ainda que eu falasse as línguas dos homens e dos anjos, e não tivesse amor, seria como o metal que soa ou como o sino que tine."),
                    BibliaVersiculo(7, 46, 13, 2, "E ainda que tivesse o dom de profecia, e conhecesse todos os mistérios e toda a ciência, e ainda que tivesse toda a fé, de maneira tal que transportasse os montes, e não tivesse amor, nada seria.")
                )

                bibliaDao.insertVerses(versiculos)
                
                // Populating templates, musicas for immediate high quality testing
                val templates = listOf(
                    Template("t1", "Culto de Domingo", """[{"nome":"Acolhida","duracao":5},{"nome":"Louvor","duracao":15},{"nome":"Dízimos","duracao":10},{"nome":"Pregação","duracao":35},{"nome":"Oração Final","duracao":5}]"""),
                    Template("t2", "Culto de Quinta", """[{"nome":"Louvor Inicial","duracao":10},{"nome":"Palavra Estudo","duracao":40},{"nome":"Avisos","duracao":5}]""")
                )
                val escalaDao = database.dirigenteDao()
                for (t in templates) {
                    escalaDao.insertTemplate(t)
                }
                
                val musicas = listOf(
                    Musica("m1", "Grandioso És Tu", "Sol Maior", "Senhor meu Deus, quando eu maravilhado\nFico a pensar nas obras de Tuas mãos...\nO Teu poder mostrado no universo\nMinha alma canta a Ti, Senhor!", "G C G D D7 G", "Entrada"),
                    Musica("m2", "Porque Ele Vive", "Lá Maior", "Deus enviou seu Filho amado\nPara morrer em meu lugar\nNa cruz pagou o meu pecado...\nMas ressurgiu e vivo com o Pai está!", "A D A E E7 A", "Comunhão"),
                    Musica("m3", "Aclame ao Senhor", "Dó Maior", "Meu Jesus, Salvador\nOutro igual não há\nTodos os dias quero louvar...", "C G Am F C G", "Ofertório")
                )
                val levDao = database.levitaDao()
                for (m in musicas) {
                    levDao.insertMusica(m)
                }
            }
        }
    }
}
