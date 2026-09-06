package com.u2k.w4llpaperch4nger

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ImagePickerScreen()
                }
            }
        }
    }
}

// 指定したフォルダの中から、画像ファイルのURIだけを一覧取得する
fun listImagesInFolder(context: android.content.Context, folderUri: Uri): List<Uri> {
    val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()
    return folder.listFiles()
        .filter { it.isFile && (it.type?.startsWith("image/") == true) }
        .map { it.uri }
}

@Composable
fun ImagePickerScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var folderUri by remember { mutableStateOf<Uri?>(null) }
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // 起動時に、保存済みのフォルダがあれば読み込む
    LaunchedEffect(Unit) {
        ImageStorage.getFolder(context).collect { savedUriString ->
            if (savedUriString != null) {
                val uri = Uri.parse(savedUriString)
                folderUri = uri
                images = listImagesInFolder(context, uri)
            }
        }
    }

    // フォルダ選択ダイアログを起動する仕組み
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // フォルダへの永続的なアクセス権限を取得する
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            folderUri = uri
            images = listImagesInFolder(context, uri)

            coroutineScope.launch {
                ImageStorage.saveFolder(context, uri.toString())
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (folderUri == null) "フォルダが未選択です"
            else "選択中の画像: ${images.size}枚"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            folderPicker.launch(null)
        }) {
            Text("画像フォルダを選択する")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(images) { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(4.dp)
                        .height(100.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}