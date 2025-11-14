package com.example.firebase.pages

import android.util.Log
import androidx.compose.foundation.Image 
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource 
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebase.R 
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ----------------------------------------------------------
// 1. DATA CLASS
// ----------------------------------------------------------

/**
 * Representa a estrutura de dados de uma única nota,
 * correspondendo aos campos no Firebase Firestore.
 *
 * @property id O ID único do documento no Firestore (opcional, nulo ao criar).
 * @property title O título da nota.
 * @property content O conteúdo principal da nota.
 * @property userId O ID do usuário (do Firebase Auth) ao qual esta nota pertence.
 * @property createdAt O timestamp (em milissegundos) de quando a nota foi criada.
 */
data class Note(
    var id: String? = null,
    val title: String = "",
    val content: String = "",
    val userId: String = "",
    val createdAt: Long = 0L
)

// ----------------------------------------------------------
// 2. VIEWMODEL
// ----------------------------------------------------------

/**
 * ViewModel responsável pela lógica de negócios da [HomePage].
 * Gerencia o estado das notas (leitura, criação, atualização, exclusão)
 * e se comunica com o Firebase.
 */
class NotesViewModel : ViewModel() {
    // Instâncias dos serviços do Firebase.
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Estado de fluxo mutável para a lista de notas (privado).
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    // Estado de fluxo público e imutável exposto à UI.
    val notes: StateFlow<List<Note>> = _notes

    // Referência ao listener do Firestore para poder removê-lo posteriormente.
    private var listenerRegistration: ListenerRegistration? = null

    /**
     * Listener para o estado de autenticação do Firebase.
     * Inicia a escuta de notas quando o usuário faz login e
     * limpa a lista e o listener quando o usuário faz logout.
     */
    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            // Usuário logado: inicia a escuta de notas.
            escutarNotas(user.uid)
        } else {
            // Usuário deslogado: remove o listener e limpa a lista.
            listenerRegistration?.remove()
            _notes.value = emptyList()
        }
    }

    /**
     * Bloco de inicialização. Anexa o [authListener]
     * assim que o ViewModel é criado.
     */
    init {
        auth.addAuthStateListener(authListener)
    }

    /**
     * Anexa um listener de snapshot em tempo real à coleção 'notes' no Firestore,
     * filtrando pelo 'userId' e ordenando por data de criação (descendente).
     *
     * @param userId O ID do usuário para o qual as notas devem ser buscadas.
     */
    private fun escutarNotas(userId: String) {
        listenerRegistration?.remove() // Remove listeners antigos para evitar duplicatas.

        listenerRegistration = db.collection("notes")
            .whereEqualTo("userId", userId) // Filtra por usuário
            .orderBy("createdAt", Query.Direction.DESCENDING) // Ordena (mais novas primeiro)
            .addSnapshotListener { snapshot, e ->
                // Tratamento de erro (ex: índice faltando no Firestore)
                if (e != null) {
                    Log.e("Firestore", "Erro ao escutar notas. VERIFIQUE O ÍNDICE:", e)
                    return@addSnapshotListener
                }

                // Mapeia os documentos do snapshot para a lista de [Note].
                if (snapshot != null) {
                    val lista = snapshot.documents.mapNotNull { doc ->
                        try {
                            // Converte o documento em um objeto Note.
                            Note(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                content = doc.getString("content") ?: "",
                                userId = doc.getString("userId") ?: "",
                                createdAt = doc.getLong("createdAt") ?: 0L
                            )
                        } catch (ex: Exception) {
                            Log.e("Firestore", "Erro ao converter nota", ex)
                            null // Ignora documentos malformados.
                        }
                    }
                    _notes.value = lista // Atualiza o StateFlow com a nova lista.
                } else {
                    _notes.value = emptyList()
                }
            }
    }

    /**
     * Chamado quando o ViewModel está prestes a ser destruído (ex: tela fechada).
     * Remove os listeners de autenticação e do Firestore para evitar memory leaks.
     */
    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        listenerRegistration?.remove()
    }

    /**
     * Salva (cria ou atualiza) uma nota no Firestore.
     * Se [note.id] for nulo, cria um novo documento.
     * Se [note.id] existir, atualiza (set) o documento existente.
     *
     * @param note O objeto [Note] a ser salvo.
     */
    fun salvarNota(note: Note) {
        val userId = auth.currentUser?.uid ?: run {
            Log.w("NotesViewModel", "Usuário não autenticado - não salva nota")
            return
        }

        // Cria um mapa de dados para o Firestore.
        val data = hashMapOf(
            "title" to note.title,
            "content" to note.content,
            "userId" to userId,
            // Garante um novo timestamp se a nota for nova.
            "createdAt" to (note.createdAt.takeIf { it != 0L } ?: System.currentTimeMillis())
        )

        val collection = db.collection("notes")

        if (note.id.isNullOrEmpty()) {
            // Cria um novo documento (ID automático)
            collection.add(data)
                .addOnSuccessListener { Log.d("NotesViewModel", "Nota criada") }
                .addOnFailureListener { e -> Log.e("NotesViewModel", "Erro ao criar nota", e) }
        } else {
            // Atualiza um documento existente usando o ID.
            collection.document(note.id!!)
                .set(data) // .set() sobrescreve; .update() mescla.
                .addOnSuccessListener { Log.d("NotesViewModel", "Nota atualizada") }
                .addOnFailureListener { e -> Log.e("NotesViewModel", "Erro ao atualizar nota", e) }
        }
    }

    /**
     * Deleta uma nota do Firestore com base no seu ID de documento.
     *
     * @param id O ID do documento a ser deletado.
     */
    fun deletarNota(id: String) {
        db.collection("notes").document(id)
            .delete()
            .addOnSuccessListener { Log.d("NotesViewModel", "Nota deletada") }
            .addOnFailureListener { e -> Log.e("NotesViewModel", "Erro ao deletar nota", e) }
    }
}

// ----------------------------------------------------------
// 3. COMPOSABLE (INTERFACE DO USUÁRIO)
// ----------------------------------------------------------

/**
 * O Composable principal que exibe a interface do usuário para a lista de notas.
 *
 * @param userName O "apelido" do usuário logado, vindo da navegação.
 * @param onLogout Callback invocado quando o usuário clica no botão de sair.
 * @param viewModel Instância do [NotesViewModel] (injetada automaticamente pelo Compose).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    userName: String,
    onLogout: () -> Unit,
    viewModel: NotesViewModel = viewModel()
) {
    // Coleta o estado da lista de notas do ViewModel de forma reativa.
    val notes by viewModel.notes.collectAsState()

    // --- Variáveis de Estado para Gerenciamento de UI ---

    // Armazena a nota que está sendo editada (nulo se for uma nova nota).
    var notaEmEdicao by remember { mutableStateOf<Note?>(null) }
    // Controla a visibilidade do dialog de criação/edição.
    var showDialog by remember { mutableStateOf(false) }

    // Estado para os campos de texto do dialog de criação/Eedição.
    var titleText by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }

    // Armazena a nota selecionada para exclusão (controla o dialog de confirmação).
    var notaParaDeletar by remember { mutableStateOf<Note?>(null) }

    // Controla a visibilidade do dialog de confirmação de logout.
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Define um gradiente de fundo para o Scaffold.
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.background
        )
    )

    // Estrutura principal da tela (barra superior, conteúdo, botão de ação).
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notas de $userName") },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) { // Abre o dialog de logout
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Sair",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Prepara o dialog para "criar" uma nova nota.
                    notaEmEdicao = null
                    titleText = ""
                    contentText = ""
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Nota")
            }
        },
        modifier = Modifier.background(brush = gradient)
    ) { padding ->
        // --- Conteúdo Principal (Lista ou Estado Vazio) ---

        if (notes.isEmpty()) {
            // Se a lista de notas estiver vazia, exibe a imagem e o texto.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.add_notas),
                        contentDescription = "Sem notas",
                        modifier = Modifier.size(250.dp) // Tamanho ajustável
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Nenhuma nota encontrada.")
                }
            }
        } else {
            // Se a lista não estiver vazia, exibe a LazyColumn com as notas.
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(notes, key = { it.id ?: it.title }) { nota ->
                    // Card individual para cada nota.
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(

                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Título da nota
                                Text(
                                    nota.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                // Botões de ação (Editar, Excluir)
                                Row {
                                    IconButton(onClick = {
                                        // Prepara o dialog para "editar" esta nota.
                                        notaEmEdicao = nota
                                        titleText = nota.title
                                        contentText = nota.content
                                        showDialog = true
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Editar",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = {
                                        // Abre o dialog de confirmação de exclusão.
                                        notaParaDeletar = nota
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Excluir",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            // Conteúdo da nota (se houver)
                            if (nota.content.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(nota.content, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs (Modais) ---

    // Dialog 1: Criação e Edição de Nota
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },

            // Slot para o ícone/imagem no topo do dialog.
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.notas),
                    contentDescription = "Editar Nota",
                    modifier = Modifier.size(150.dp) // Tamanho ajustável
                )
            },

            // Título do dialog (Nova Nota ou Editar Nota).
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(if (notaEmEdicao == null) "Nova Nota" else "Editar Nota")
                }
            },

            // Corpo do dialog com os campos de texto.
            text = {
                Column {
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contentText,
                        onValueChange = { contentText = it },
                        label = { Text("Conteúdo") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp) // Altura aumentada
                    )
                }
            },

            // Botão de confirmação.
            confirmButton = {
                TextButton(onClick = {
                    // Prepara o objeto nota (novo ou atualizado)
                    val notaParaSalvar = (notaEmEdicao ?: Note()).copy(
                        title = titleText,
                        content = contentText
                    )
                    // Chama o ViewModel para salvar.
                    viewModel.salvarNota(notaParaSalvar)

                    // Limpa e fecha o dialog.
                    titleText = ""
                    contentText = ""
                    notaEmEdicao = null
                    showDialog = false
                }) {
                    Text(if (notaEmEdicao == null) "Adicionar" else "Salvar")
                }
            },

            // Botão de cancelamento.
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog 2: Confirmação de Exclusão
    if (notaParaDeletar != null) {
        AlertDialog(
            onDismissRequest = { notaParaDeletar = null }, // Fecha se clicar fora.
            title = { Text("Confirmar Exclusão") },
            text = { Text("Deseja realmente excluir a nota \"${notaParaDeletar!!.title}\"?") },

            // Botão de confirmação (Excluir).
            confirmButton = {
                TextButton(onClick = {
                    // Chama o ViewModel para deletar.
                    notaParaDeletar!!.id?.let { viewModel.deletarNota(it) }
                    notaParaDeletar = null // Fecha o dialog.
                }) {
                    Text("Excluir")
                }
            },

            // Botão de cancelamento.
            dismissButton = {
                TextButton(onClick = { notaParaDeletar = null }) { // Apenas fecha o dialog.
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog 3: Confirmação de Logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirmar Saída") },
            text = { Text("Deseja realmente sair?") },

            // Botão de confirmação (Sair).
            confirmButton = {
                TextButton(onClick = {
                    onLogout() // Chama a função de logout
                    showLogoutDialog = false
                }) {
                    Text("Sair")
                }
            },

            // Botão de cancelamento.
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
