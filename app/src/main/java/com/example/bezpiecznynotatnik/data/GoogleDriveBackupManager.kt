package com.example.bezpiecznynotatnik.data

import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.SecureNotesApp

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.Room
import com.example.bezpiecznynotatnik.UserState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

class GoogleDriveBackupManager {

    private var googleSignInClient: GoogleSignInClient? = null
    private var driveService: Drive? = null

    fun initializeGoogleSignIn(context: Context) {
        val clientId = context.getString(R.string.web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun isUserSignedIn(): Boolean {
        val isUserSignedIn = UserState.isUserSignedIn
        return isUserSignedIn
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient?.signInIntent
            ?: throw IllegalStateException("Google Sign-In client is not initialized.")
    }

    fun silentSignIn(context: Context, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            try {
                initializeDriveService(context, account)
                onSuccess()
            } catch (e: Exception) {
                onFailure("Failed to initialize Drive service: ${e.message}")
            }
        } else {
            onFailure("No signed-in account found.")
        }
    }

    fun handleSignInResult(
        context: Context,
        data: Intent?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            if (account != null) {
                initializeDriveService(context, account)
                onSuccess()
            } else {
                onFailure("Failed to retrieve Google account.")
            }
        } catch (e: ApiException) {
            onFailure("Google Sign-In failed: ${e.message}")
        }
    }
    fun getSignedInUserName(context: Context): String {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
        return googleAccount?.displayName ?: context.getString(R.string.user)
    }

    fun getProfilePictureUrl(context: Context): String? {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
        return googleAccount?.photoUrl?.toString() // Returns the URL of the profile picture
    }

    private fun initializeDriveService(context: Context, account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        driveService = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
            .setApplicationName("NoteLocker")
            .build()
    }
    fun isDriveServiceInitialized(): Boolean {
        return driveService != null
    }

    suspend fun uploadDatabase(context: Context) {
        if (driveService == null) throw IllegalStateException("Drive service is not initialized.")
        try {
            val dbPath = context.getDatabasePath("notes_db")
            val shmPath = context.getDatabasePath("notes_db-shm")
            val walPath = context.getDatabasePath("notes_db-wal")

            if (!dbPath.exists()) throw FileNotFoundException("Database file not found!")

            // Upload the main database file
            uploadFileToDrive(dbPath, "notes_db")

            // Upload auxiliary files if they exist
            uploadFileToDrive(shmPath, "notes_db-shm")
            uploadFileToDrive(walPath, "notes_db-wal")

            Log.d("GoogleDriveManager", "All database files uploaded successfully!")
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Error uploading database files: ${e.message}")
            throw e
        }
    }

    private suspend fun uploadFileToDrive(file: File, fileName: String) {
        val existingFileId = findFileOnDrive(fileName)

        if (existingFileId != null) {
            // Update the existing file
            val fileContent = FileContent("application/octet-stream", file)
            withContext(Dispatchers.IO) {
                driveService!!.files().update(existingFileId, null, fileContent).execute()
            }
            Log.d("GoogleDriveManager", "File $fileName updated successfully on Google Drive.")
        } else {
            // Create a new file
            val metadata = com.google.api.services.drive.model.File().setName(fileName)
            val fileContent = FileContent("application/octet-stream", file)
            withContext(Dispatchers.IO) {
                driveService!!.files().create(metadata, fileContent).execute()
            }
            Log.d("GoogleDriveManager", "File $fileName created successfully on Google Drive.")
        }
    }

    suspend fun downloadDatabase(context: Context) {
        if (driveService == null) throw IllegalStateException("Drive service is not initialized.")
        try {
            closeDatabase(context)

            val dbPath = context.getDatabasePath("notes_db")
            val shmPath = context.getDatabasePath("notes_db-shm")
            val walPath = context.getDatabasePath("notes_db-wal")

            downloadFileFromDrive("notes_db")?.copyTo(dbPath, overwrite = true)
            downloadFileFromDrive("notes_db-shm")?.copyTo(shmPath, overwrite = true)
            downloadFileFromDrive("notes_db-wal")?.copyTo(walPath, overwrite = true)

            reloadDatabase(context)
            Log.d("GoogleDriveManager", "Database restored successfully!")
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Error restoring database files: ${e.message}")
            throw e
        }
    }

    private suspend fun findFileOnDrive(fileName: String): String? {
        return try {
            val result = withContext(Dispatchers.IO) {
                driveService!!.files().list()
                    .setQ("name='$fileName' and trashed=false") // Search by name and ignore trashed files
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute()
            }
            result.files.firstOrNull()?.id // Return the ID of the first matching file
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Error finding file $fileName: ${e.message}")
            null
        }
    }

    private suspend fun downloadFileFromDrive(fileName: String): File? {
        return try {
            val result = withContext(Dispatchers.IO) {
                driveService!!.files().list()
                    .setQ("name='$fileName' and trashed=false")
                    .setSpaces("drive")
                    .execute()
            }

            val fileId = result.files.firstOrNull()?.id
            if (fileId != null) {
                val tempFile = withContext(Dispatchers.IO) {
                    File.createTempFile(fileName, null)
                }
                withContext(Dispatchers.IO) {
                    driveService!!.files().get(fileId).executeMediaAndDownloadTo(tempFile.outputStream())
                }
                tempFile
            } else {
                Log.e("GoogleDriveManager", "File not found: $fileName")
                null
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Error downloading file: ${e.message}")
            null
        }
    }
    private fun closeDatabase(context: Context) {
        try {
            (context as SecureNotesApp).noteDatabase.close()
        } catch (e: Exception) {
            Log.e("DatabaseClose", "Error closing database: ${e.message}")
        }
        val dbPath = context.getDatabasePath("notes_db")
        File(dbPath.absolutePath + "-shm").delete()
        File(dbPath.absolutePath + "-wal").delete()
    }

    private fun reloadDatabase(context: Context) {
        val app = context as SecureNotesApp
        app.noteDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "notes_db"
        ).build()
    }

    fun signOut(onResult: (Boolean) -> Unit) {
        googleSignInClient!!.signOut().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onResult(true)

            } else {
                onResult(false)
            }
        }
    }
}