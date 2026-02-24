package com.github.giuoaejgiusejnb.mhrsb_qurious_crafting_ocr

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
            var showUpdateDialog by remember { mutableStateOf(false) }
            var latestVersion by remember { mutableStateOf("") }

            // 起動時にチェック（設定がONの場合のみ）
            LaunchedEffect(Unit) {
                val shouldCheck = prefs.getBoolean("show_update_notification", true)
                if (shouldCheck) {
                    val latest = checkUpdate()
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val current = packageInfo.versionName
                    if (latest != null && latest != current) {
                        latestVersion = latest
                        showUpdateDialog = true
                    }
                }
            }

            MyApp()

            if (showUpdateDialog) {
                AlertDialog(
                    onDismissRequest = { showUpdateDialog = false },
                    title = { Text("アップデートのお知らせ") },
                    text = { Text("新しいバージョン ($latestVersion) が利用可能です。更新しますか？") },
                    confirmButton = {
                        TextButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/giuoaejgiusejnb/mhrsb-qurious-crafting_ocr-app/releases"))
                            context.startActivity(intent)
                            showUpdateDialog = false
                        }) { Text("更新する") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUpdateDialog = false }) {
                            Text("後で")
                        }
                    }
                )
            }
        }
    }
}

private suspend fun checkUpdate(): String? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.github.com/repos/giuoaejgiusejnb/mhrsb-qurious-crafting-ocr-app/releases/latest")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connect()

        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            json.getString("tag_name")
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

@Preview(showBackground = true)
@Composable
fun MyApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onImageRecognitionClick = { navController.navigate("image_recognition") },
                onDesiredSkillSettingsClick = { navController.navigate("desired_skill_settings") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }

        composable("image_recognition") {
            ImageRecognitionScreen(onBackClick = { navController.popBackStack() })
        }

        composable("desired_skill_settings") {
            DesiredSkillSettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable("settings") {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
