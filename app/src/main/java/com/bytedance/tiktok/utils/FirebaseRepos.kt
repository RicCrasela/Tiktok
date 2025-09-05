package com.bytedance.tiktok.utils

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

data class Post(
    val id: String = "",
    val userId: String = "",
    val content: String = "",
    val isPhoto: Boolean = false,
    val mediaUrl: String = "",
    val createdAt: Long = 0L
)

object FirebaseUploadRepository {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun ensureAnonLogin(): String {
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
        return user?.uid ?: throw IllegalStateException("Auth failed")
    }

    suspend fun uploadPost(uri: Uri, isPhoto: Boolean, content: String): Post {
        val uid = ensureAnonLogin()
        val postId = firestore.collection("posts").document().id
        val ext = if (isPhoto) "jpg" else "mp4"
        val path = "uploads/$uid/$postId.$ext"
        val ref = storage.reference.child(path)
        ref.putFile(uri).await()
        val downloadUrl = ref.downloadUrl.await().toString()

        val post = Post(
            id = postId,
            userId = uid,
            content = content,
            isPhoto = isPhoto,
            mediaUrl = downloadUrl,
            createdAt = System.currentTimeMillis()
        )
        firestore.collection("posts").document(postId).set(post).await()
        return post
    }
}

object FirebaseFeedRepository {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun fetchLatest(limit: Long = 20): List<Post> {
        val snap = firestore.collection("posts")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return snap.documents.mapNotNull { it.toObject(Post::class.java) }
    }
}