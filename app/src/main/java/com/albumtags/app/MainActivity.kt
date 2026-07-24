@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.albumtags.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage

private val Indigo = Color(0xFF5757D9)
private val Background = Color(0xFFF8F9FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = Background) {
                    AlbumTagsApp()
                }
            }
        }
    }
}

@Composable
private fun AlbumTagsApp(viewModel: AlbumViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        viewModel.loadAlbums(granted)
    }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                writer.write(viewModel.exportTags())
            }
        }
    }
    var importMessage by remember { mutableStateOf<String?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val contents = context.contentResolver.openInputStream(it)
                ?.bufferedReader()?.use { reader -> reader.readText() }.orEmpty()
            importMessage = if (viewModel.importTags(contents)) {
                "태그 백업을 불러왔어요."
            } else {
                "앨범태그 백업 파일을 읽지 못했어요."
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadAlbums(permissionGranted)
    }

    if (!permissionGranted) {
        PermissionScreen { launcher.launch(permission) }
    } else {
        AlbumListScreen(
            state = state,
            onQueryChange = viewModel::setQuery,
            onToggleTag = viewModel::toggleTag,
            onToggleUntagged = viewModel::toggleUntagged,
            onToggleMatchMode = viewModel::toggleMatchMode,
            onClearFilters = viewModel::clearFilters,
            onSaveTags = viewModel::saveTags,
            onRefresh = { viewModel.loadAlbums(true) },
            onExport = { backupLauncher.launch("album-tags-backup.json") },
            onImport = { importLauncher.launch(arrayOf("application/json")) },
            message = importMessage,
            onDismissMessage = { importMessage = null }
        )
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(Indigo.copy(alpha = 0.12f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Image, null, tint = Indigo, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("내 앨범을 한눈에", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "앨범을 불러오려면 사진 접근 권한이 필요해요.\n원본 사진은 수정하거나 복사하지 않습니다.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF626673)
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) { Text("사진 앨범 불러오기") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumListScreen(
    state: AlbumUiState,
    onQueryChange: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onToggleUntagged: () -> Unit,
    onToggleMatchMode: () -> Unit,
    onClearFilters: () -> Unit,
    onSaveTags: (String, Set<String>) -> Unit,
    onRefresh: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    message: String?,
    onDismissMessage: () -> Unit
) {
    var editingAlbum by remember { mutableStateOf<PhotoAlbum?>(null) }

    Scaffold(containerColor = Background) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("앨범태그", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${state.albums.size}개 앨범 · ${state.allTags.size}개 태그",
                        color = Color(0xFF777B88)
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onImport) { Text("복원") }
                    TextButton(onClick = onExport) { Text("백업") }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, "갤러리 새로고침")
                    }
                }
                if (message != null) {
                    AssistChip(onClick = onDismissMessage, label = { Text(message) })
                }
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("앨범명 또는 태그 검색") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Clear, "검색어 지우기")
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("필터", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    if (state.selectedTags.isNotEmpty()) {
                        TextButton(onClick = onToggleMatchMode) {
                            Text(if (state.matchMode == TagMatchMode.ALL) "모두 포함" else "하나라도 포함")
                        }
                    }
                    if (state.selectedTags.isNotEmpty() || state.showUntaggedOnly || state.query.isNotBlank()) {
                        TextButton(onClick = onClearFilters) { Text("초기화") }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = state.showUntaggedOnly,
                        onClick = onToggleUntagged,
                        label = { Text("미분류") }
                    )
                    state.allTags.forEach { tag ->
                        FilterChip(
                            selected = tag in state.selectedTags,
                            onClick = { onToggleTag(tag) },
                            label = { Text(tag) }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "앨범 ${state.visibleAlbums.size}개",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            when {
                state.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.visibleAlbums.isEmpty() -> item {
                    EmptyState(state.albums.isEmpty())
                }
                else -> items(state.visibleAlbums, key = { it.bucketId }) { album ->
                    AlbumCard(
                        album = album,
                        tags = state.tagMap[album.bucketId].orEmpty(),
                        onClick = { editingAlbum = album }
                    )
                }
            }
        }
    }

    editingAlbum?.let { album ->
        TagEditorDialog(
            album = album,
            initialTags = state.tagMap[album.bucketId].orEmpty(),
            suggestedTags = state.allTags,
            onDismiss = { editingAlbum = null },
            onSave = {
                onSaveTags(album.bucketId, it)
                editingAlbum = null
            }
        )
    }
}

@Composable
private fun AlbumCard(album: PhotoAlbum, tags: Set<String>, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = album.coverUri,
                contentDescription = "${album.name} 대표 사진",
                modifier = Modifier.size(92.dp).background(Color(0xFFE9EAF0), RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(album.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text("사진 ${album.photoCount}장", color = Color(0xFF777B88),
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(7.dp))
                if (tags.isEmpty()) {
                    Text("태그 추가", color = Indigo, style = MaterialTheme.typography.labelLarge)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        tags.sorted().take(4).forEach {
                            Text(
                                "#$it",
                                color = Indigo,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (tags.size > 4) Text("+${tags.size - 4}", color = Color.Gray)
                    }
                }
            }
            Icon(Icons.Default.Label, null, tint = Indigo.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun EmptyState(noAlbumsAtAll: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Image, null, modifier = Modifier.size(48.dp), tint = Color(0xFFB0B3BE))
        Spacer(Modifier.height(12.dp))
        Text(
            if (noAlbumsAtAll) "사진 앨범이 없어요" else "조건에 맞는 앨범이 없어요",
            fontWeight = FontWeight.Bold
        )
        Text(
            if (noAlbumsAtAll) "갤러리에 사진을 추가한 뒤 다시 실행해 주세요."
            else "필터나 검색어를 바꿔 보세요.",
            color = Color(0xFF777B88)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagEditorDialog(
    album: PhotoAlbum,
    initialTags: Set<String>,
    suggestedTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    var tags by remember(album.bucketId) { mutableStateOf(initialTags) }
    var input by remember(album.bucketId) { mutableStateOf("") }

    fun addInputTag() {
        val normalized = input.trim().removePrefix("#")
        if (normalized.isNotEmpty()) {
            tags = tags + normalized
            input = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(album.name, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("이 앨범과 관련된 사람·그룹·장소를 자유롭게 추가하세요.",
                    color = Color(0xFF626673))
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("새 태그") },
                    placeholder = { Text("예: 여자친구, 가족, 제주") },
                    trailingIcon = {
                        IconButton(onClick = ::addInputTag) { Icon(Icons.Default.Add, "추가") }
                    },
                    keyboardActions = KeyboardActions(onDone = { addInputTag() }),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    tags.sorted().forEach { tag ->
                        FilterChip(
                            selected = true,
                            onClick = { tags = tags - tag },
                            label = { Text(tag) },
                            trailingIcon = { Icon(Icons.Default.Clear, "삭제", Modifier.size(16.dp)) }
                        )
                    }
                }
                val unused = suggestedTags.filter { it !in tags }
                if (unused.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("기존 태그", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        unused.take(12).forEach { tag ->
                            AssistChip(onClick = { tags = tags + tag }, label = { Text(tag) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                addInputTag()
                val pending = input.trim().removePrefix("#")
                onSave(if (pending.isEmpty()) tags else tags + pending)
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
