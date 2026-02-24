package com.github.giuoaejgiusejnb.mhrsb_qurious_crafting_ocr

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onImageRecognitionClick: () -> Unit,
    onDesiredSkillSettingsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "ホーム",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onImageRecognitionClick) {
            Text("画像認識ページへ")
        }
        Button(onClick = onDesiredSkillSettingsClick) {
            Text("欲しいスキル設定ページへ")
        }
        Button(onClick = onSettingsClick) {
            Text("ユーザー設定を開く")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        onImageRecognitionClick = {},
        onDesiredSkillSettingsClick = {},
        onSettingsClick = {}
    )
}
