package com.ciudadalerta.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.ciudadalerta.data.model.Usuario
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun iniciarConGoogle(context: Context, webClientId: String): FirebaseUser {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val credential = CredentialManager.create(context)
            .getCredential(context, request)
            .credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
            return auth.signInWithCredential(firebaseCred).await().user
                ?: throw IllegalStateException("Firebase no devolvió el usuario")
        }
        throw IllegalStateException("Credencial de Google no válida")
    }

    suspend fun crearOActualizarPerfilCiudadano(): Usuario {
        val user = auth.currentUser
            ?: throw IllegalStateException("No hay sesión iniciada")
        val docRef = firestore.collection("usuarios").document(user.uid)

        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(Usuario::class.java)?.let { return it }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error leyendo usuario: ${e.message}", e)
        }

        val nuevo = Usuario(
            id = user.uid,
            nombre = user.displayName ?: "Ciudadano",
            correo = user.email ?: "",
            fotoUrl = user.photoUrl?.toString(),
            puntosCivicos = 25,
            nivel = 1
        )

        try {
            docRef.set(nuevo).await()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error creando usuario en Firestore: ${e.message}", e)
        }
        return nuevo
    }

    fun cerrarSesion() {
        auth.signOut()
    }
}