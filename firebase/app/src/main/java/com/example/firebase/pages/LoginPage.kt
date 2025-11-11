package com.example.firebase.pages

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firebase.R
import com.example.firebase.components.CustomDarkTextField
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginPage(
    onLogin: (String) -> Unit, // Espera o apelido (String)
    onRegisterClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var mostrarSenha by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Usando cores padrão do MaterialTheme
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardBackground = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val detailsColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Image(
                    painter = painterResource(id = R.drawable.nota),
                    contentDescription = "Sem notas",
                    modifier = Modifier.size(230.dp)
                        .padding(bottom = 16.dp)
                )

                Text(
                    "Conecte-se conosco",
                    fontFamily = FontFamily.Serif,
                    fontSize = 26.sp,
                    color = primaryColor,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                CustomDarkTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "E-mail",
                    backgroundColor = cardBackground,
                    textColor = textColor,
                    labelColor = labelColor
                )

                CustomDarkTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = "Senha",
                    backgroundColor = cardBackground,
                    textColor = textColor,
                    labelColor = labelColor,
                    isPassword = !mostrarSenha,
                    trailingIcon = {
                        IconButton(onClick = { mostrarSenha = !mostrarSenha }) {
                            Icon(
                                // Agora isso vai funcionar
                                imageVector = if (mostrarSenha) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Mostrar/ocultar senha",
                                tint = MaterialTheme.colorScheme.primary // <-- Cor primária
                            )
                        }
                    }
                )

                Text(
                    "Seja bem-vindo!",
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    color = detailsColor,
                    modifier = Modifier.padding(vertical = 5.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || senha.isBlank()) {
                            errorMessage = "Preencha todos os campos"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = ""

                        auth.signInWithEmailAndPassword(email, senha)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userId = auth.currentUser?.uid ?: run {
                                        errorMessage = "Erro: UID não encontrado."
                                        isLoading = false
                                        return@addOnCompleteListener
                                    }

                                    db.collection("banco")
                                        .document(userId)
                                        .get()
                                        .addOnSuccessListener { document ->
                                            if (document.exists()) {
                                                val nomeUsuario = document.getString("apelido") ?: email
                                                onLogin(nomeUsuario) // Envia o apelido
                                            } else {
                                                errorMessage = "Usuário não encontrado no banco de dados."
                                                isLoading = false
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            errorMessage = "Erro ao buscar dados: ${e.message}"
                                            Log.w("Login", "Erro Firestore", e)
                                            isLoading = false
                                        }
                                } else {
                                    errorMessage = "Erro de autenticação: ${task.exception?.message}"
                                    isLoading = false
                                }
                            }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Entrar", fontSize = 18.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onRegisterClick,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = primaryColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, primaryColor)
                ) {
                    Text("Não tem conta? Cadastre-se", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Rodapé
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(primaryColor),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "© 2025 | Letícia Guanaes Moreira",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Todos os direitos reservados.",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}