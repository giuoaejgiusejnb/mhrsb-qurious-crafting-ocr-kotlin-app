package com.github.giuoaejgiusejnb.mhrsb_qurious_crafting_ocr

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageRecognitionScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("skill_settings", Context.MODE_PRIVATE) }
    
    // 保存されている設定を読み込む
    val savedSettings = remember { loadSavedSettings(context) }
    
    var showDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    
    // savedSettings が変わるたびに（削除された時など）、選択中の名前が有効かチェックする
    var selectedSettingName by remember(savedSettings) { 
        val lastSelected = prefs.getString("last_selected_setting", "未選択") ?: "未選択"
        // 保存された設定の中に名前がない、かつ「未選択」でない場合はリセット
        val validatedName = if (lastSelected != "未選択" && !savedSettings.containsKey(lastSelected)) {
            "未選択"
        } else {
            lastSelected
        }
        
        // もしリセットが発生したなら、Prefsも更新しておく
        if (validatedName != lastSelected) {
            prefs.edit { putString("last_selected_setting", validatedName) }
        }
        
        mutableStateOf(validatedName) 
    }
    
    var detailSettingName by remember { mutableStateOf<String?>(null) }
    var selectedFolderUri by remember { mutableStateOf<Uri?>(null) }

    // ZIP作成中の状態管理
    var isZipping by remember { mutableStateOf(false) }
    var zipProgress by remember { mutableStateOf(0f) }
    var processedCount by remember { mutableStateOf(0) }
    var totalCount by remember { mutableStateOf(0) }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        selectedFolderUri = uri
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text(
            text = "画像認識",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "現在の設定: ", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = selectedSettingName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("設定を選択する")
        }



        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "選択フォルダ: ", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = selectedFolderUri?.let { getFolderName(context, it) } ?: "未選択",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }

        Button(
            onClick = { folderLauncher.launch(null) },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("画像フォルダを選択する")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (selectedSettingName == "未選択") {
                    Toast.makeText(context, "設定を選択してください", Toast.LENGTH_SHORT).show()
                } else if (selectedFolderUri == null) {
                    Toast.makeText(context, "フォルダを選択してください", Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch {
                        isZipping = true
                        zipProgress = 0f
                        processedCount = 0
                        val zipFile = createZipWithSettingsLarge(
                            context, 
                            selectedFolderUri!!, 
                            savedSettings[selectedSettingName] ?: emptyList(),
                            onProgress = { progress, current, total ->
                                zipProgress = progress
                                processedCount = current
                                totalCount = total
                            }
                        )
                        isZipping = false
                        if (zipFile != null) {
                            shareToGoogleDrive(context, zipFile)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedSettingName != "未選択" && selectedFolderUri != null && !isZipping,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            if (isZipping) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(24.dp))
            } else {
                Text("1. ZIPをDriveへ共有/保存")
            }
        }

        Text(
            text = "※画像枚数（1万枚等）が多い場合、ZIP作成には数分かかります。容量不足にご注意ください。また，一度SDカードから本体ストレージに画像フォルダを移動してからzip化した方が速いかもしれません．もし速度差が感じられるなら，自動化するので開発者に連絡",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW,
                        "https://colab.research.google.com/github/giuoaejgiusejnb/mhrsb-qurious-crafting-ocr-app/blob/main/notebooks/qurious_crafting_app.ipynb".toUri())
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("2. Colabを開いて実行")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { showHelpDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Colabの使い方")
            }
        }

        Text(
            text = "※zipファイルのアップロードが終わってから，colabを開いてください",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { deleteZipFile(context) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("3. 作成したZIPファイルを削除")
        }

        Text(
            text = "※画像認識後の画像フォルダをダウンロードしてから，zipファイルを削除してください",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = onBackClick) {
            Text("戻る")
        }
    }

    // 使い方のダイアログ
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Google Colabの使い方") },
            text = {
                Column {
                    Text("① （スマホの場合は≡を押す⇒）ランタイムを選択⇒ランタイムのタイプを変更⇒ハードウェア アクセラレータのT4 GPUを選択して保存")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("② （スマホの場合は≡を押す⇒ランタイムを選択⇒）すべてのセルを実行")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("③ 一番下までスクロールし，処理が終わったら画像zipファイルをダウンロード")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("閉じる")
                }
            }
        )
    }

    // ZIP作成中のプログレス表示
    if (isZipping) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("ZIPファイル作成中") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = { zipProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${processedCount} / ${totalCount} 枚完了 (${(zipProgress * 100).toInt()}%)")
                }
            },
            confirmButton = { }
        )
    }

    // 設定選択ダイアログ
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("設定を選択") },
            text = {
                Column {
                    if (savedSettings.isEmpty()) {
                        Text("保存された設定がありません")
                    } else {
                        savedSettings.keys.forEach { name ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        selectedSettingName = name
                                        prefs.edit { putString("last_selected_setting", name) }
                                        showDialog = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(name, modifier = Modifier.fillMaxWidth())
                                }
                                OutlinedButton(
                                    onClick = { detailSettingName = name },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("詳細")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 詳細表示ダイアログ
    if (detailSettingName != null) {
        val skills = savedSettings[detailSettingName] ?: emptyList()
        AlertDialog(
            onDismissRequest = { detailSettingName = null },
            title = { Text("「$detailSettingName」の内容") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(skills) { skill ->
                        Text("・ $skill", modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailSettingName = null }) {
                    Text("閉じる")
                }
            }
        )
    }
}

private fun getFolderName(context: Context, uri: Uri): String? {
    val documentFile = DocumentFile.fromTreeUri(context, uri)
    return documentFile?.name
}

private suspend fun createZipWithSettingsLarge(
    context: Context, 
    folderUri: Uri, 
    skills: List<String>,
    onProgress: (Float, Int, Int) -> Unit
): File? {
    return withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            val zipFile = File(cacheDir, "mhrsb_temp.zip")
            if (zipFile.exists()) zipFile.delete()
            
            val zos = ZipOutputStream(FileOutputStream(zipFile))
            zos.setMethod(ZipOutputStream.DEFLATED)
            zos.setLevel(0)

            // 1. ファイル情報の取得（名前とUriのペア）
            val treeId = DocumentsContract.getTreeDocumentId(folderUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, treeId)
            
            val imageInfoList = mutableListOf<Pair<String, Uri>>()
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID, 
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME, 
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null, null, null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                
                while (cursor.moveToNext()) {
                    val mime = cursor.getString(mimeIndex)
                    if (mime != null && mime.startsWith("image/")) {
                        val docId = cursor.getString(idIndex)
                        val name = cursor.getString(nameIndex) ?: "unknown_${System.currentTimeMillis()}"
                        val uri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId)
                        imageInfoList.add(name to uri)
                    }
                }
            }

            // 【重要】ファイル名で昇順ソートして順番を保証する
            imageInfoList.sortBy { it.first }

            val totalEntries = imageInfoList.size + 1
            var processed = 0

            // 2. temp_settings.json の追加
            val skillArray = JSONArray()
            skills.forEach { skillArray.put(it) }
            val settingEntry = ZipEntry("temp_settings.json")
            zos.putNextEntry(settingEntry)
            zos.write(skillArray.toString().toByteArray())
            zos.closeEntry()
            processed++
            onProgress(processed.toFloat() / totalEntries, processed, totalEntries)

            // 3. ソートされた順序で画像を追加
            imageInfoList.forEach { (name, uri) ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val imageEntry = ZipEntry(name) // 元のファイル名を使用
                    zos.putNextEntry(imageEntry)
                    inputStream.copyTo(zos)
                    zos.closeEntry()
                }
                processed++
                onProgress(processed.toFloat() / totalEntries, processed, totalEntries)
            }

            zos.close()
            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private fun shareToGoogleDrive(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Google Driveに保存"))
}

private fun deleteZipFile(context: Context) {
    val cacheDir = context.cacheDir
    val zipFile = File(cacheDir, "mhrsb_temp.zip")
    if (zipFile.exists()) {
        if (zipFile.delete()) {
            Toast.makeText(context, "ZIPファイルを削除しました", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "削除に失敗しました", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "削除するファイルが見つかりません", Toast.LENGTH_SHORT).show()
    }
}

private fun loadSavedSettings(context: Context): Map<String, List<String>> {
    val prefs = context.getSharedPreferences("skill_settings", Context.MODE_PRIVATE)
    val jsonString = prefs.getString("saved_settings", "{}") ?: "{}"
    val result = mutableMapOf<String, List<String>>()
    try {
        val jsonObject = JSONObject(jsonString)
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val jsonArray = jsonObject.getJSONArray(key)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            result[key] = list
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return result
}

@Preview(showBackground = true)
@Composable
fun ImageRecognitionScreenPreview() {
    ImageRecognitionScreen(onBackClick = {})
}
