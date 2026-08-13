package com.lumokato.nunulo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
private fun PreviewFrame(title: String, content: @Composable () -> Unit) {
    NunuloTheme {
        Surface(color = NunuloColors.Background, modifier = Modifier.fillMaxSize()) {
            Column {
                Row(Modifier.fillMaxWidth().height(56.dp).background(Color.White).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                content()
            }
        }
    }
}

@Preview(name = "记录-空状态-360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun CaptureEmptyPreview() = PreviewFrame("记录") {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("照片", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("先选好这次记录的 1–9 张照片", color = NunuloColors.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { repeat(4) { index -> Surface(color = if (index == 0) NunuloColors.Coral else NunuloColors.Hairline, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f).height(4.dp)) {} } }
            }
        }
        item {
            SectionCard("实时记录", "应用内拍摄默认单图，相册可选择 1–9 张，首图即封面。") {
                Column(Modifier.fillMaxWidth().height(280.dp).background(NunuloColors.Placeholder, RoundedCornerShape(16.dp)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("拍下第一张照片", fontWeight = FontWeight.Bold)
                    Text("也可以从系统相册批量选择", color = NunuloColors.Muted)
                }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("打开相机") }
            }
        }
    }
}

@Preview(name = "选择器-无结果-393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun CatalogNoResultPreview() = PreviewFrame("登记伙伴") {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("作品与角色", "可搜索正式名、日文、罗马字和必要别名。") {
                OutlinedTextField("不存在的作品名称", {}, label = { Text("搜索作品 / IP") }, modifier = Modifier.fillMaxWidth())
                Text("没有找到“ 不存在的作品名称 ”", color = NunuloColors.Muted)
                Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("提交作品候选") }
                Text("候选会进入待审核状态，正式归并由 Web Admin 完成。", color = NunuloColors.Muted)
            }
        }
    }
}

@Preview(name = "动态-离线错误-360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun FeedOfflinePreview() = PreviewFrame("动态") {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { EmptyState("暂时无法同步", "当前网络不可用。已保留本地草稿和上次浏览状态，联网后可重试。") }
        item { Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("重新同步") } }
    }
}

@Preview(name = "我的-管理员边界-393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun AdminBoundaryPreview() = PreviewFrame("我的") {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("我的 Nunulo") {
                Text("旅行收藏者", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("@nunulo_owner", color = NunuloColors.Muted)
                Text("站点所有者", color = NunuloColors.Coral, fontWeight = FontWeight.Bold)
                Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("打开 Web 管理台") }
                Text("Android 只显示身份和入口；目录、举报、活动与存储治理仍在独立 Web Admin。", color = NunuloColors.Muted)
            }
        }
    }
}
