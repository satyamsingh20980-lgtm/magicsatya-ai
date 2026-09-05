package com.example.magicsatya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ChatScreen()
            }
        }
    }
}

data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun ChatScreen() {
    var textState by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(ChatMessage("Hello! Main MagicSatya AI hu. Kaise madad karu?", false)) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val apiKey = "AQ.Ab8RN6I5VvgvbMV2e_y2r2vCJ0kGoiqDjzrs07Gp2BK_uvSMWw"

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "MagicSatya AI",
            fontSize = 22.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { msg ->
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isUser) Color(0xFF2196F3) else Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (msg.isUser) Color.White else Color.Black,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp))
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = textState,
                onValueChange = { textState = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask MagicSatya AI...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (textState.isNotBlank() && !isLoading) {
                        val userQuery = textState
                        messages.add(ChatMessage(userQuery, true))
                        textState = ""
                        isLoading = true

                        coroutineScope.launch(Dispatchers.IO) {
                            val reply = getAiResponse(userQuery, apiKey)
                            withContext(Dispatchers.Main) {
                                messages.add(ChatMessage(reply, false))
                                isLoading = false
                            }
                        }
                    }
                }
            ) {
                Text("Send")
            }
        }
    }
}

fun getAiResponse(prompt: String, apiKey: String): String {
    return try {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        val jsonInput = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
        }

        conn.outputStream.use { os ->
            os.write(jsonInput.toString().toByteArray())
        }

        if (conn.responseCode == 200) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(response)
            jsonResponse
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } else {
            "Error: ${conn.responseCode} - API Call Failed"
        }
    } catch (e: Exception) {
        "Error: ${e.localizedMessage}"
    }
}
