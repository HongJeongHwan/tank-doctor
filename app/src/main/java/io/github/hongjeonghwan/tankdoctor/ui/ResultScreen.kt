@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.hongjeonghwan.tankdoctor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hongjeonghwan.tankdoctor.AppViewModel
import io.github.hongjeonghwan.tankdoctor.Photo
import io.github.hongjeonghwan.tankdoctor.Screen
import io.github.hongjeonghwan.tankdoctor.data.Diagnosis
import io.github.hongjeonghwan.tankdoctor.data.Issue
import io.github.hongjeonghwan.tankdoctor.ui.theme.color

@Composable
fun ResultScreen(result: Diagnosis, photos: List<Photo>, vm: AppViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("진단 결과", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.open(Screen.HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (photos.size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(photos, key = { _, p -> p.id }) { i, photo ->
                        PhotoThumb(photo = photo, number = i + 1, size = 60.dp)
                    }
                }
            }

            if (!result.isAquarium) {
                SectionCard("어항 사진이 아닌 것 같아요") {
                    Text(result.summary.ifBlank { "어항 전체가 보이도록 다시 찍어 주세요." })
                }
            } else {
                ScoreCard(result)

                if (result.actions.isNotEmpty()) {
                    SectionCard("지금 할 일") { NumberedList(result.actions) }
                }

                if (result.issues.isNotEmpty()) {
                    Text(
                        "발견된 문제 ${result.issues.size}개",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    result.issues.forEach { IssueCard(it) }
                }

                if (result.categories.isNotEmpty()) {
                    SectionCard("항목별 상태") {
                        result.categories.forEachIndexed { i, c ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Dot(c.level.color())
                                    Text(c.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text(c.level.label, color = c.level.color(), style = MaterialTheme.typography.labelLarge)
                                }
                                Text(
                                    c.comment,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (result.tests.isNotEmpty()) {
                    SectionCard("사진으로는 알 수 없어요 · 검사 권장") { BulletList(result.tests) }
                }
            }

            Text(
                "AI가 사진만 보고 추정한 결과예요. 물고기가 급격히 나빠지면 수질 검사와 전문 수족관 상담을 함께 받아 보세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(onClick = { vm.open(Screen.HOME) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("다른 사진으로 진단하기")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ScoreCard(result: Diagnosis) {
    val color = result.level.color()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { result.score / 100f },
                        modifier = Modifier.size(92.dp),
                        color = color,
                        strokeWidth = 9.dp,
                        trackColor = color.copy(alpha = 0.18f),
                    )
                    Text(
                        "${result.score}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Badge(result.level.label, color)
                    Text(result.headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            if (result.summary.isNotBlank()) {
                Text(result.summary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun IssueCard(issue: Issue) {
    val color = issue.severity.color()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Badge(issue.severity.label, color)
                Text(issue.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (issue.evidence.isNotBlank()) LabeledText("사진에서 보인 것", issue.evidence)
            if (issue.cause.isNotBlank()) LabeledText("추정 원인", issue.cause)
            if (issue.solutions.isNotEmpty()) {
                Text("해결 방법", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                BulletList(issue.solutions)
            }
        }
    }
}

@Composable
private fun LabeledText(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
