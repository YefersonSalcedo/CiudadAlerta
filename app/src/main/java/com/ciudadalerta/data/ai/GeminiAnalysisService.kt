package com.ciudadalerta.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.ciudadalerta.BuildConfig
import com.ciudadalerta.data.model.Categoria
import com.ciudadalerta.data.model.GeminiAnalisisResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class GeminiAnalysisService {

    suspend fun analizarFoto(bitmap: Bitmap): Result<GeminiAnalisisResponse> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(
                        Exception("Falta GEMINI_API_KEY en local.properties")
                    )
                }
                val rawText = llamarGemini(apiKey, bitmap.reducir())
                Result.success(parsear(rawText))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun llamarGemini(apiKey: String, bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val imagenB64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts",
                JSONArray().put(JSONObject().put("text", promptSistema))))
            .put("contents", JSONArray().put(JSONObject()
                .put("role", "user")
                .put("parts", JSONArray()
                    .put(JSONObject().put("inlineData", JSONObject()
                        .put("mimeType", "image/jpeg")
                        .put("data", imagenB64)))
                    .put(JSONObject().put("text",
                        "Devuelve únicamente el JSON con el análisis de la fotografía.")))))
            .put("generationConfig", JSONObject()
                .put("responseMimeType", "application/json")
                .put("temperature", 0.2))

        val conn = (URL("$BASE_URL/$MODELO:generateContent").openConnection()
                as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
        }

        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val detalle = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.w("GeminiAnalysis", "HTTP $code: $detalle")
                throw Exception(
                    if (code == 429) "Se alcanzó el límite del servicio de análisis. Intenta en unos minutos."
                    else "El servicio de análisis no respondió correctamente ($code)."
                )
            }
            val respuesta = conn.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(respuesta)
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                ?.takeIf { it.isNotBlank() }
                ?: throw Exception("Respuesta vacía o bloqueada")
        } finally {
            conn.disconnect()
        }
    }

    private fun parsear(rawText: String): GeminiAnalisisResponse {
        val json = JSONObject(rawText.replace("```json", "").replace("```", "").trim())

        val catDevuelta = json.optString("categoria", "Otros")
        val estado = json.optString("estado", "valida")
        val motivo = if (json.isNull("motivoBloqueo")) null else json.optString("motivoBloqueo")
        val descripcion = json.optString("descripcion", "Incidencia urbana detectada")
        val esEmergencia = json.optBoolean("esEmergencia", false)

        val categoria = if (Categoria.CATEGORIAS_BLOQUEADAS.contains(catDevuelta.uppercase())) {
            Categoria.Otros.nombre
        } else {
            Categoria.fromString(catDevuelta).nombre
        }

        return GeminiAnalisisResponse(
            categoria = categoria,
            estado = if (esEmergencia) "bloqueada" else estado,
            motivoBloqueo = if (esEmergencia && motivo.isNullOrBlank())
                "Riesgo vital detectado. Llama de inmediato al 123." else motivo,
            descripcion = descripcion,
            esEmergencia = esEmergencia
        )
    }

    private fun Bitmap.reducir(maxLado: Int = 1024): Bitmap {
        val escala = maxLado.toFloat() / maxOf(width, height)
        if (escala >= 1f) return this
        return Bitmap.createScaledBitmap(this, (width * escala).toInt(), (height * escala).toInt(), true)
    }

    private val promptSistema = """
        Eres el analizador de visión cívica de "CiudadAlerta", plataforma colaborativa urbana en Colombia.
        Evalúa la imagen capturada y responde EXCLUSIVAMENTE con un JSON válido.
        
        REGLAS DE PRIVACIDAD Y RECHAZO:
        1. Si hay rostros reconocibles en primer plano: "estado": "bloqueada", "motivoBloqueo": "Se detectaron rostros humanos identificables. Protege la privacidad ciudadana."
        2. Si hay placas de vehículos particulares legibles: "estado": "bloqueada", "motivoBloqueo": "Se detectaron placas vehiculares visibles."
        3. Si es meme, captura de pantalla, foto privada o sin relación con espacio público: "estado": "bloqueada", "motivoBloqueo": "No representa una incidencia urbana en espacio público."
        
        EMERGENCIAS VITALES:
        4. Si detectas cables caídos con chispas, incendios activos, colapso de edificios o personas heridas: "esEmergencia": true, "estado": "bloqueada", "motivoBloqueo": "Riesgo vital detectado. Llama de inmediato al 123."
        
        CATEGORÍAS PERMITIDAS:
        - "Vías"
        - "Alumbrado público"
        - "Residuos y aseo"
        - "Parques y zonas verdes"
        - "Semáforos"
        - "Árboles (sin riesgo inminente)"
        - "Animales abandonados"
        - "Otros"
        
        ESQUEMA JSON OBLIGATORIO:
        {
          "categoria": string,
          "estado": "valida" | "bloqueada",
          "motivoBloqueo": string | null,
          "descripcion": string,
          "esEmergencia": boolean
        }
    """.trimIndent()

    private companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val MODELO = "gemini-3.5-flash"
    }


}