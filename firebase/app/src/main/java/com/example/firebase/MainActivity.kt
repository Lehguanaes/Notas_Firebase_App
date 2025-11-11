package com.example.firebase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.firebase.pages.HomePage
import com.example.firebase.pages.LoginPage
import com.example.firebase.pages.RegisterPage
import com.example.firebase.ui.theme.FirebaseTheme // Certifique-se que o nome do seu tema está correto
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FirebaseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val startDestination = "login"

    NavHost(navController = navController, startDestination = startDestination) {

        // Rota 1: Tela de Login
        composable("login") {
            LoginPage(
                onLogin = { userName ->
                    // Navega para a home passando o apelido e limpa a pilha
                    navController.navigate("home/$userName") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate("register")
                }
            )
        }

        // Rota 2: Tela Principal (Notas)
        composable(
            route = "home/{userName}",
            arguments = listOf(navArgument("userName") { type = NavType.StringType })
        ) { backStackEntry ->
            val userName = backStackEntry.arguments?.getString("userName") ?: "Usuário"
            HomePage(
                userName = userName, // Passa o apelido para a HomePage
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo("home/{userName}") { inclusive = true }
                    }
                }
            )
        }

        // Rota 3: Tela de Registro (Seu código)
        composable("register") {
            RegisterPage(
                // Ambas as ações voltam para a tela de Login
                onRegisterComplete = {
                    navController.popBackStack()
                },
                onLoginClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}