package com.u2k.w4llpaperch4nger

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
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

data class ImageFile(val uri: Uri, val name: String)

fun listImagesInFolder(context: android.content.Context, folderUri: Uri): List<ImageFile> {
    val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()
    return folder.listFiles()
        .filter { it.isFile && (it.type?.startsWith("image/") == true) }
        .map { ImageFile(it.uri, it.name ?: "(不明なファイル名)") }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("壁紙自動変更") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // フォルダ設定カード
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (folderUri == null) "フォルダが未選択です"
                        else "選択中の画像: ${images.size}枚",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { folderPicker.launch(null) }) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("画像フォルダを選択する")
                    }
                }
            }

            // 間隔設定カード
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("切り替え間隔", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box {
                        OutlinedButton(onClick = { dropdownExpanded = true }) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            val currentLabel = intervalOptions.firstOrNull { it.second == selectedIntervalMinutes }?.first
                                ?: "${selectedIntervalMinutes}分ごと"
                            Text(currentLabel)
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            intervalOptions.forEach { (label, minutes) ->
                                DropdownMenuItem(
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
                }
            }

            // 状態表示カード
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("状態", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("稼働状況: $workStatus")
                    Text(lastUpdatedText)
                }
            }

            // 操作ボタン(開始・停止)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
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
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("開始")
                }

                OutlinedButton(
                    onClick = {
                        androidx.work.WorkManager.getInstance(context).cancelUniqueWork("wallpaper_change_work")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("停止")
                }
            }

            // 画像ファイル名リスト
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(images) { imageFile ->
                        Text(
                            text = imageFile.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 8.dp)
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}