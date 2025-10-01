    package com.example.booktranslator

    import android.content.Context
    import android.content.Intent
    import android.net.Uri
    import android.os.Bundle
    import android.util.Log
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.activity.enableEdgeToEdge
    import androidx.compose.runtime.*
    import androidx.core.app.ActivityCompat
    import androidx.core.content.ContextCompat
    import androidx.core.content.FileProvider
    import androidx.lifecycle.ViewModelProvider
    import com.example.booktranslator.data.local.AppDatabase
    import com.example.booktranslator.data.repository.AlbumRepository
    import com.example.booktranslator.ui.screens.*
    import com.example.booktranslator.ui.theme.BookTranslatorTheme
    import com.example.booktranslator.viewmodel.AlbumViewModel
    import com.example.booktranslator.viewmodel.AlbumViewModelFactory
    import com.google.android.gms.auth.api.signin.GoogleSignIn
    import com.google.android.gms.auth.api.signin.GoogleSignInClient
    import com.google.android.gms.auth.api.signin.GoogleSignInOptions
    import com.google.firebase.FirebaseApp
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.auth.GoogleAuthProvider
    import android.Manifest
    import android.content.pm.PackageManager
    import java.io.File
    import com.example.booktranslator.data.local.Photo

    class MainActivity : ComponentActivity() {

        private var photoUri: Uri? = null
        private lateinit var auth: FirebaseAuth
        private lateinit var googleSignInClient: GoogleSignInClient
        private lateinit var albumViewModel: AlbumViewModel

        private var currentAlbumId by mutableStateOf<Long?>(null)
        private var selectedPhoto by mutableStateOf<Photo?>(null)

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            FirebaseApp.initializeApp(this)
            auth = FirebaseAuth.getInstance()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
            }

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)

            val db = AppDatabase.getDatabase(this)
            val repository = AlbumRepository(db.albumDao(), db.photoDao())
            albumViewModel = ViewModelProvider(
                this,
                AlbumViewModelFactory(repository)
            )[AlbumViewModel::class.java]

            val signInLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(Exception::class.java)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: Exception) {
                    Log.e("Login", "Google sign in failed: ${e.message}")
                }
            }

            val pickImageLauncher = registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    currentAlbumId?.let { id ->
                        albumViewModel.addPhoto(id, it.toString())
                    }
                }
            }

            val takePhotoLauncher = registerForActivityResult(
                ActivityResultContracts.TakePicture()
            ) { success ->
                if (success && photoUri != null) {
                    currentAlbumId?.let { id ->
                        albumViewModel.addPhoto(id, photoUri.toString())
                    }
                }
            }

            enableEdgeToEdge()
            setContent {
                BookTranslatorTheme {
                    var showDialog by remember { mutableStateOf(false) }
                    val user = auth.currentUser

                    if (user == null) {
                        LoginScreen(onLoginClick = {
                            val signInIntent: Intent = googleSignInClient.signInIntent
                            signInLauncher.launch(signInIntent)
                        })
                    } else {
                        when {
                            selectedPhoto != null && currentAlbumId != null -> {
                                PhotoTranslationPagerScreen(
                                    albumId = currentAlbumId!!,
                                    selectedPhoto = selectedPhoto!!,
                                    viewModel = albumViewModel,
                                    onBack = { selectedPhoto = null }
                                )
                            }
                            currentAlbumId == null -> {
                                AlbumsScreen(
                                    viewModel = albumViewModel,
                                    onAlbumClick = { albumId -> currentAlbumId = albumId },
                                    onAddAlbum = { showDialog = true }
                                )

                                if (showDialog) {
                                    AddAlbumDialog(
                                        onDismiss = { showDialog = false },
                                        onCreate = { title ->
                                            albumViewModel.addAlbum(title)
                                            showDialog = false
                                        }
                                    )
                                }
                            }
                            else -> {
                                AlbumDetailScreen(
                                    albumId = currentAlbumId!!,
                                    viewModel = albumViewModel,
                                    onAddPhotoFromGallery = { pickImageLauncher.launch("image/*") },
                                    onAddPhotoFromCamera = {
                                        val uri = createImageUri(this@MainActivity)
                                        photoUri = uri
                                        takePhotoLauncher.launch(uri)
                                    },
                                    onBack = { currentAlbumId = null },
                                    onPhotoClick = { photo ->
                                        selectedPhoto = photo
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        private fun firebaseAuthWithGoogle(idToken: String) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("Login", "✅ Zalogowano! ${auth.currentUser?.email}")
                    } else {
                        Log.e("Login", "Logowanie nieudane", task.exception)
                    }
                }
        }

        private fun createImageUri(context: Context): Uri {
            val file = File(context.externalCacheDir, "images/${System.currentTimeMillis()}.jpg")
            file.parentFile?.mkdirs()
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }