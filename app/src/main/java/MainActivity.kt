package com.u2k.w4llpaperch4nger

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items

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
// 画像1件分の情報(場所とファイル名)をまとめて扱うためのデータクラス
data class ImageFile(val uri: Uri, val name: String)

fun listImagesInFolder(context: android.content.Context, folderUri: Uri): List<ImageFile> {
    val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()
    return folder.listFiles()
        .filter { it.isFile && (it.type?.startsWith("image/") == true) }
        .map { ImageFile(it.uri, it.name ?: "(不明なファイル名)") }
}

@Composable
fun ImagePickerScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var folderUri by remember { mutableStateOf<Uri?>(null) }
    var images by remember { mutableStateOf<List<ImageFile>>(emptyList()) }

    val intervalOptions = listOf(
        "15分ごと" to 15,
        "1時間ごと" to 60,
        "3時間ごと" to 180,
        "6時間ごと" to 360,
        "12時間ごと" to 720,
        "24時間ごと" to 1440
    )
    var selectedIntervalMinutes by remember { mutableStateOf(60) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var workStatus by remember { mutableStateOf("停止中") }
    var lastUpdatedText by remember { mutableStateOf("まだ切り替えられていません") }

    LaunchedEffect(Unit) {
        ImageStorage.getFolder(context).collect { savedUriString ->
            if (savedUriString != null) {
                val uri = Uri.parse(savedUriString)
                folderUri = uri
                images = listImagesInFolder(context, uri)
            }
        }
    }

    LaunchedEffect(Unit) {
        ImageStorage.getInterval(context).collect { minutes ->
            selectedIntervalMinutes = minutes
        }
    }

    LaunchedEffect(Unit) {
        androidx.work.WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow("wallpaper_change_work")
            .collect { infos ->
                val info = infos.firstOrNull()
                workStatus = when (info?.state) {
                    androidx.work.WorkInfo.State.ENQUEUED,
                    androidx.work.WorkInfo.State.RUNNING -> "稼働中"
                    else -> "停止中"
                }
            }
    }

    LaunchedEffect(Unit) {
        ImageStorage.getLastUpdated(context).collect { millis ->
            lastUpdatedText = if (millis != null) {
                val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.JAPAN)
                "最終切替: ${sdf.format(java.util.Date(millis))}"
            } else {
                "まだ切り替えられていません"
            }
        }
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
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

        Spacer(modifier = Modifier.height(8.dp))

        Text("状態: $workStatus")
        Text(lastUpdatedText)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { folderPicker.launch(null) }) {
            Text("画像フォルダを選択する")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box {
            Button(onClick = { dropdownExpanded = true }) {
                val currentLabel = intervalOptions.firstOrNull { it.second == selectedIntervalMinutes }?.first
                    ?: "${selectedIntervalMinutes}分ごと"
                Text("切り替え間隔: $currentLabel")
            }

            androidx.compose.material3.DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                intervalOptions.forEach { (label, minutes) ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            selectedIntervalMinutes = minutes
                            dropdownExpanded = false
                            coroutineScope.launch {
                                ImageStorage.saveInterval(context, minutes)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(images) { imageFile ->
                Text(
                    text = imageFile.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val workRequest = androidx.work.PeriodicWorkRequestBuilder<WallpaperWorker>(
                selectedIntervalMinutes.toLong(), java.util.concurrent.TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "wallpaper_change_work",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }) {
            Text("自動切り替えを開始する")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("wallpaper_change_work")
        }) {
            Text("自動切り替えを停止する")
        }
    }
}