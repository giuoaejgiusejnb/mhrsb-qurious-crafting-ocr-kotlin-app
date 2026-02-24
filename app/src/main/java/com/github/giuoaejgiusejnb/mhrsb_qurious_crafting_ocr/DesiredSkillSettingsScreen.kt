package com.github.giuoaejgiusejnb.mhrsb_qurious_crafting_ocr

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader

@Composable
fun DesiredSkillSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val skills = remember { loadSkillsFromJson(context) }
    var selectedSkills by remember { mutableStateOf(setOf<String>()) }
    
    // 初回表示時にデフォルトの設定名を生成する
    val savedSettingsAtStart = remember { loadSavedSettings(context) }
    val defaultName = remember {
        var i = 1
        while (savedSettingsAtStart.containsKey("設定$i")) {
            i++
        }
        "設定$i"
    }
    
    var settingName by remember { mutableStateOf(defaultName) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var showOverwriteDialog by remember { mutableStateOf(false) }
    var detailSettingName by remember { mutableStateOf<String?>(null) }
    var settingToDelete by remember { mutableStateOf<String?>(null) }
    
    val savedSettings = remember(showLoadDialog, showOverwriteDialog, settingToDelete) { loadSavedSettings(context) }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "欲しいスキル設定",
                style = MaterialTheme.typography.headlineMedium
            )
            Button(onClick = { showLoadDialog = true }) {
                Text("読み込む")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = settingName,
            onValueChange = { settingName = it },
            label = { Text("設定名を入力") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            singleLine = true
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            skills.forEach { (cost, skillList) ->
                item {
                    Text(
                        text = "コスト: $cost",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                val skillChunks = skillList.chunked(2)
                
                items(skillChunks) { chunk ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        chunk.forEach { skill ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedSkills = if (selectedSkills.contains(skill)) {
                                            selectedSkills - skill
                                        } else {
                                            selectedSkills + skill
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = selectedSkills.contains(skill),
                                    onCheckedChange = { isChecked ->
                                        selectedSkills = if (isChecked) {
                                            selectedSkills + skill
                                        } else {
                                            selectedSkills - skill
                                        }
                                    }
                                )
                                Text(
                                    text = skill,
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        if (chunk.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    if (settingName.isBlank()) {
                        Toast.makeText(context, "設定名を入力してください", Toast.LENGTH_SHORT).show()
                    } else if (selectedSkills.isEmpty()) {
                        Toast.makeText(context, "スキルを選択してください", Toast.LENGTH_SHORT).show()
                    } else {
                        // 既存の設定名があるかチェック
                        val currentSaved = loadSavedSettings(context)
                        if (currentSaved.containsKey(settingName)) {
                            showOverwriteDialog = true
                        } else {
                            saveSetting(context, settingName, selectedSkills)
                            Toast.makeText(context, "「$settingName」として保存しました", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                Text("保存")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onBackClick) {
                Text("戻る")
            }
        }
    }

    // 読み込みダイアログ
    if (showLoadDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDialog = false },
            title = { Text("設定を読み込む") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    if (savedSettings.isEmpty()) {
                        item {
                            Text("保存された設定がありません", modifier = Modifier.padding(8.dp))
                        }
                    } else {
                        items(savedSettings.keys.toList()) { name ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        settingName = name
                                        selectedSkills = savedSettings[name]?.toSet() ?: emptySet()
                                        showLoadDialog = false
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
                                IconButton(onClick = { settingToDelete = name }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "削除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLoadDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 削除確認ダイアログ
    if (settingToDelete != null) {
        AlertDialog(
            onDismissRequest = { settingToDelete = null },
            title = { Text("設定の削除") },
            text = { Text("「$settingToDelete」を本当に削除しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteSetting(context, settingToDelete!!)
                        Toast.makeText(context, "「$settingToDelete」を削除しました", Toast.LENGTH_SHORT).show()
                        settingToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { settingToDelete = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 詳細表示ダイアログ
    if (detailSettingName != null) {
        val detailSkills = savedSettings[detailSettingName] ?: emptyList()
        AlertDialog(
            onDismissRequest = { detailSettingName = null },
            title = { Text("「$detailSettingName」の内容") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(detailSkills) { skill ->
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

    // 上書き確認ダイアログ
    if (showOverwriteDialog) {
        AlertDialog(
            onDismissRequest = { showOverwriteDialog = false },
            title = { Text("上書き確認") },
            text = { Text("「$settingName」は既に存在します。上書きしてもよろしいですか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveSetting(context, settingName, selectedSkills)
                        Toast.makeText(context, "「$settingName」を上書き保存しました", Toast.LENGTH_SHORT).show()
                        showOverwriteDialog = false
                    }
                ) {
                    Text("上書き")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

private fun loadSkillsFromJson(context: Context): Map<String, List<String>> {
    return try {
        val inputStream = context.assets.open("all_skills.json")
        val reader = InputStreamReader(inputStream)
        val jsonString = reader.readText()
        reader.close()

        val jsonObject = JSONObject(jsonString)
        val skillData = jsonObject.getJSONObject("skill_data")
        val result = mutableMapOf<String, List<String>>()

        val keys = skillData.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val jsonArray = skillData.getJSONArray(key)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            result[key] = list
        }
        result
    } catch (e: Exception) {
        e.printStackTrace()
        emptyMap()
    }
}

private fun saveSetting(context: Context, name: String, skills: Set<String>) {
    val prefs = context.getSharedPreferences("skill_settings", Context.MODE_PRIVATE)
    val currentSettingsJson = prefs.getString("saved_settings", "{}") ?: "{}"
    val jsonObject = JSONObject(currentSettingsJson)
    
    val skillArray = JSONArray()
    skills.forEach { skillArray.put(it) }
    
    jsonObject.put(name, skillArray)
    
    prefs.edit {
        putString("saved_settings", jsonObject.toString())
    }
}

private fun deleteSetting(context: Context, name: String) {
    val prefs = context.getSharedPreferences("skill_settings", Context.MODE_PRIVATE)
    val currentSettingsJson = prefs.getString("saved_settings", "{}") ?: "{}"
    val jsonObject = JSONObject(currentSettingsJson)
    
    jsonObject.remove(name)
    
    prefs.edit {
        putString("saved_settings", jsonObject.toString())
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
fun DesiredSkillSettingsScreenPreview() {
    DesiredSkillSettingsScreen(onBackClick = {})
}
