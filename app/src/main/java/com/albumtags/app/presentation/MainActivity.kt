@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.albumtags.app.presentation

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.albumtags.app.AlbumTagsApplication
import com.albumtags.app.domain.model.AlbumPhoto
import com.albumtags.app.domain.model.PhotoAlbum
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Indigo = Color(0xFF5757D9)
private val Background = Color(0xFFF8F9FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = density.fontScale * 0.9f
                )
            ) {
                MaterialTheme {
                    Surface(color = Background) {
                        AlbumTagsApp()
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumTagsApp() {
    val context = LocalContext.current
    val application = context.applicationContext as AlbumTagsApplication
    val viewModel: AlbumViewModel = viewModel(
        factory = AlbumViewModel.Factory(application.container)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions = if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    var permissionGranted by remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) ==
                    PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = permissions.all { results[it] == true }
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
        PermissionScreen { launcher.launch(permissions) }
    } else if (state.openedAlbum != null) {
        AlbumDetailScreen(
            album = state.openedAlbum!!,
            photos = state.albumPhotos,
            tags = state.tagMap[state.openedAlbum!!.bucketId].orEmpty(),
            allTags = state.allTags,
            isLoading = state.arePhotosLoading,
            onBack = viewModel::closeAlbum,
            onSaveTags = viewModel::saveTags,
            onSetAlbumCover = { albumId, media ->
                viewModel.setAlbumCover(albumId, media)
                Toast.makeText(
                    context,
                    "앨범 대표 미디어로 설정했어요.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
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
            onDismissMessage = { importMessage = null },
            onOpenAlbum = viewModel::openAlbum,
            onRenameTag = viewModel::renameTag,
            onDeleteTag = viewModel::deleteTag,
            onSaveTagGroup = viewModel::saveTagGroup,
            onDeleteTagGroup = viewModel::deleteTagGroup,
            onBulkAddTags = viewModel::addTagsToAlbums
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
    onDismissMessage: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onRenameTag: (String, String) -> Boolean,
    onDeleteTag: (String) -> Unit,
    onSaveTagGroup: (String?, String, Set<String>) -> Boolean,
    onDeleteTagGroup: (String) -> Unit,
    onBulkAddTags: (Set<String>, Set<String>) -> Unit
) {
    val context = LocalContext.current
    val sortPreferences = remember {
        context.getSharedPreferences("album_sort_preferences", android.content.Context.MODE_PRIVATE)
    }
    var sortMode by remember {
        mutableStateOf(
            runCatching {
                AlbumSortMode.valueOf(
                    sortPreferences.getString("sort_mode", AlbumSortMode.NEWEST.name)!!
                )
            }.getOrDefault(AlbumSortMode.NEWEST)
        )
    }
    var customOrder by remember {
        mutableStateOf(sortPreferences.getString("custom_order", "").orEmpty()
            .split("|").filter { it.isNotBlank() })
    }
    var showSortMenu by remember { mutableStateOf(false) }
    var editingAlbum by remember { mutableStateOf<PhotoAlbum?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedAlbumIds by remember { mutableStateOf(emptySet<String>()) }
    var showBulkTagEditor by remember { mutableStateOf(false) }
    var showTagManager by remember { mutableStateOf(false) }
    var tagToRename by remember { mutableStateOf<String?>(null) }
    var tagToDelete by remember { mutableStateOf<String?>(null) }
    var tagActionTarget by remember { mutableStateOf<String?>(null) }
    var collapsedTagGroups by remember { mutableStateOf(emptySet<String>()) }
    val availableTags = state.allTags.toSet()
    val visibleTagGroups = state.tagGroups
        .toSortedMap()
        .mapValues { (_, tags) -> tags.intersect(availableTags) }
        .filterValues { it.isNotEmpty() }
    val groupedTags = visibleTagGroups.values.flatten().toSet()
    val ungroupedTags = state.allTags.filter { it !in groupedTags }
    val visibleAlbums = when (sortMode) {
        AlbumSortMode.NEWEST -> state.visibleAlbums.sortedByDescending { it.newestDateSeconds }
        AlbumSortMode.OLDEST -> state.visibleAlbums.sortedBy { it.oldestDateSeconds }
        AlbumSortMode.NAME -> state.visibleAlbums.sortedBy { it.name.lowercase() }
        AlbumSortMode.COUNT -> state.visibleAlbums.sortedByDescending { it.photoCount }
        AlbumSortMode.CUSTOM -> {
            val order = customOrder.withIndex().associate { it.value to it.index }
            state.visibleAlbums.sortedWith(
                compareBy<PhotoAlbum> { order[it.bucketId] ?: Int.MAX_VALUE }
                    .thenByDescending { it.newestDateSeconds }
            )
        }
    }
    fun saveSort(mode: AlbumSortMode, order: List<String> = customOrder) {
        sortMode = mode
        customOrder = order
        sortPreferences.edit()
            .putString("sort_mode", mode.name)
            .putString("custom_order", order.joinToString("|"))
            .apply()
    }

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedAlbumIds = emptySet()
    }

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
                    TextButton(onClick = {
                        tagToRename = null
                        tagToDelete = null
                        showTagManager = true
                    }) {
                        Text("태그 관리")
                    }
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
                }
                visibleTagGroups.forEach { (groupName, tags) ->
                    val isCollapsed = groupName in collapsedTagGroups
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                collapsedTagGroups = if (isCollapsed) {
                                    collapsedTagGroups - groupName
                                } else {
                                    collapsedTagGroups + groupName
                                }
                            }
                            .padding(top = 8.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCollapsed) {
                                Icons.Default.KeyboardArrowRight
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = if (isCollapsed) {
                                "$groupName 펼치기"
                            } else {
                                "$groupName 접기"
                            },
                            tint = Indigo,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = groupName,
                            color = Indigo,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = " (${tags.size})",
                            color = Color(0xFF777B88),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (!isCollapsed) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.sorted().chunked(2).forEach { tagColumn ->
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    tagColumn.forEach { tag ->
                                        LongPressEditableTagChip(
                                            selected = tag in state.selectedTags,
                                            onClick = { onToggleTag(tag) },
                                            onLongClick = {
                                                tagActionTarget = tag
                                            },
                                            label = { Text(tag) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (ungroupedTags.isNotEmpty()) {
                    if (visibleTagGroups.isNotEmpty()) {
                        Text(
                            "그룹 없음",
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                            color = Color(0xFF777B88),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ungroupedTags.forEach { tag ->
                            LongPressEditableTagChip(
                                selected = tag in state.selectedTags,
                                onClick = { onToggleTag(tag) },
                                onLongClick = {
                                    tagActionTarget = tag
                                },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "앨범 ${visibleAlbums.size}개",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, "앨범 정렬")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            listOf(
                                AlbumSortMode.NEWEST to "최신 앨범순",
                                AlbumSortMode.OLDEST to "오래된 앨범순",
                                AlbumSortMode.NAME to "이름순",
                                AlbumSortMode.COUNT to "사진·동영상 수순",
                                AlbumSortMode.CUSTOM to "사용자 지정 순서"
                            ).forEach { (mode, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(if (sortMode == mode) "✓ $label" else label)
                                    },
                                    onClick = {
                                        val initialOrder = if (mode == AlbumSortMode.CUSTOM) {
                                            (customOrder + state.albums.map { it.bucketId }).distinct()
                                        } else {
                                            customOrder
                                        }
                                        saveSort(mode, initialOrder)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                    TextButton(onClick = {
                        selectionMode = !selectionMode
                        selectedAlbumIds = emptySet()
                    }) {
                        Text(if (selectionMode) "선택 취소" else "여러 앨범 선택")
                    }
                }
            }

            if (selectionMode) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Indigo.copy(alpha = 0.09f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${selectedAlbumIds.size}개 선택",
                                fontWeight = FontWeight.Bold,
                                color = Indigo
                            )
                            Spacer(Modifier.weight(1f))
                            Button(
                                enabled = selectedAlbumIds.isNotEmpty(),
                                onClick = { showBulkTagEditor = true }
                            ) {
                                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                Text(" 태그 추가")
                            }
                        }
                    }
                }
            }

            when {
                state.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                visibleAlbums.isEmpty() -> item {
                    EmptyState(state.albums.isEmpty())
                }
                else -> items(visibleAlbums, key = { it.bucketId }) { album ->
                    val customIndex = visibleAlbums.indexOfFirst { it.bucketId == album.bucketId }
                    AlbumCard(
                        album = album,
                        tags = state.tagMap[album.bucketId].orEmpty(),
                        selected = album.bucketId in selectedAlbumIds,
                        selectionMode = selectionMode,
                        onOpen = {
                            if (selectionMode) {
                                selectedAlbumIds = if (album.bucketId in selectedAlbumIds) {
                                    selectedAlbumIds - album.bucketId
                                } else {
                                    selectedAlbumIds + album.bucketId
                                }
                            } else {
                                onOpenAlbum(album.bucketId)
                            }
                        },
                        onLongPress = {
                            selectionMode = true
                            selectedAlbumIds = selectedAlbumIds + album.bucketId
                        },
                        onEditTags = {
                            if (!selectionMode) editingAlbum = album
                        },
                        showOrderControls = sortMode == AlbumSortMode.CUSTOM,
                        onMoveUp = {
                            if (customIndex > 0) {
                                val ids = visibleAlbums.map { it.bucketId }.toMutableList()
                                java.util.Collections.swap(ids, customIndex, customIndex - 1)
                                saveSort(
                                    AlbumSortMode.CUSTOM,
                                    ids + customOrder.filter { it !in ids }
                                )
                            }
                        },
                        onMoveDown = {
                            if (customIndex in 0 until visibleAlbums.lastIndex) {
                                val ids = visibleAlbums.map { it.bucketId }.toMutableList()
                                java.util.Collections.swap(ids, customIndex, customIndex + 1)
                                saveSort(
                                    AlbumSortMode.CUSTOM,
                                    ids + customOrder.filter { it !in ids }
                                )
                            }
                        }
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

    if (showBulkTagEditor) {
        BulkTagDialog(
            albumCount = selectedAlbumIds.size,
            suggestedTags = state.allTags,
            onDismiss = { showBulkTagEditor = false },
            onSave = { tags ->
                onBulkAddTags(selectedAlbumIds, tags)
                showBulkTagEditor = false
                selectionMode = false
                selectedAlbumIds = emptySet()
            }
        )
    }

    if (showTagManager) {
        TagManagerDialog(
            tags = state.allTags,
            groups = state.tagGroups,
            initialRenamingTag = tagToRename,
            initialDeletingTag = tagToDelete,
            onDismiss = {
                showTagManager = false
                tagToRename = null
                tagToDelete = null
            },
            onRename = onRenameTag,
            onDelete = onDeleteTag,
            onSaveGroup = onSaveTagGroup,
            onDeleteGroup = onDeleteTagGroup
        )
    }

    tagActionTarget?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagActionTarget = null },
            title = { Text("#$tag", fontWeight = FontWeight.Bold) },
            text = { Text("이 태그를 어떻게 관리할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    tagActionTarget = null
                    tagToRename = tag
                    tagToDelete = null
                    showTagManager = true
                }) {
                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                    Text(" 이름 바꾸기")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    tagActionTarget = null
                    tagToDelete = tag
                    tagToRename = null
                    showTagManager = true
                }) {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFC44747)
                    )
                    Text(" 태그 삭제", color = Color(0xFFC44747))
                }
            }
        )
    }
}

@Composable
private fun LongPressEditableTagChip(
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    label: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Indigo.copy(alpha = 0.14f) else Color.Transparent,
        contentColor = if (selected) Indigo else Color(0xFF3F424B),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Indigo else Color(0xFF777B88)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            label()
        }
    }
}

@Composable
private fun AlbumCard(
    album: PhotoAlbum,
    tags: Set<String>,
    selected: Boolean,
    selectionMode: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onEditTags: () -> Unit,
    showOrderControls: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {}
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (selected) BorderStroke(2.dp, Indigo) else null
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = remember(album.coverUri, album.coverIsVideo) {
                    thumbnailModel(context, album.coverUri, album.coverIsVideo)
                },
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
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onOpen() })
            } else if (showOrderControls) {
                Column {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Default.ArrowUpward, "위로 이동", tint = Indigo)
                    }
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Default.ArrowDownward, "아래로 이동", tint = Indigo)
                    }
                }
            } else {
                IconButton(onClick = onEditTags) {
                    Icon(Icons.Default.Label, "태그 편집", tint = Indigo.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun AlbumDetailScreen(
    album: PhotoAlbum,
    photos: List<AlbumPhoto>,
    tags: Set<String>,
    allTags: List<String>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSaveTags: (String, Set<String>) -> Unit,
    onSetAlbumCover: (String, AlbumPhoto) -> Unit
) {
    val context = LocalContext.current
    var editingTags by remember { mutableStateOf(false) }
    var showAlbumInfo by remember { mutableStateOf(false) }
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var selectedMediaUris by remember { mutableStateOf(emptySet<String>()) }
    val mediaSelectionMode = selectedMediaUris.isNotEmpty()

    BackHandler(enabled = selectedPhotoIndex == null) {
        if (mediaSelectionMode) selectedMediaUris = emptySet() else onBack()
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (mediaSelectionMode) selectedMediaUris = emptySet() else onBack()
            }) {
                Icon(Icons.Default.ArrowBack, "앨범 목록으로")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (mediaSelectionMode) "${selectedMediaUris.size}개 선택" else album.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "사진 ${album.photoCount}장",
                    color = Color(0xFF777B88),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (mediaSelectionMode) {
                IconButton(onClick = {
                    val selected = photos.filter { it.uri in selectedMediaUris }
                    shareMedia(context, selected)
                }) {
                    Icon(Icons.Default.Share, "선택한 미디어 공유", tint = Indigo)
                }
                IconButton(
                    enabled = selectedMediaUris.size == 1,
                    onClick = {
                        val media = photos.first { it.uri in selectedMediaUris }
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(media.uri), media.mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "다른 앱으로 열기"))
                    }
                ) {
                    Icon(Icons.Default.OpenInNew, "다른 앱으로 열기", tint = Indigo)
                }
                IconButton(onClick = { selectedMediaUris = emptySet() }) {
                    Icon(Icons.Default.Close, "선택 취소")
                }
            } else {
            IconButton(onClick = { showAlbumInfo = true }) {
                Icon(Icons.Default.Info, "앨범 정보", tint = Indigo)
            }
            IconButton(onClick = { editingTags = true }) {
                Icon(Icons.Default.Label, "태그 편집", tint = Indigo)
            }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (tags.isEmpty()) {
                AssistChip(
                    onClick = { editingTags = true },
                    label = { Text("태그 추가") },
                    leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) }
                )
            } else {
                tags.sorted().forEach { tag ->
                    AssistChip(
                        onClick = { editingTags = true },
                        label = { Text("#$tag") }
                    )
                }
            }
        }

        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            photos.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("이 앨범에서 표시할 사진을 찾지 못했어요.", color = Color(0xFF777B88))
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                gridItemsIndexed(photos, key = { _, photo -> photo.stableKey }) { index, photo ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .combinedClickable(
                                onClick = {
                                    if (mediaSelectionMode) {
                                        selectedMediaUris =
                                            if (photo.uri in selectedMediaUris) {
                                                selectedMediaUris - photo.uri
                                            } else {
                                                selectedMediaUris + photo.uri
                                            }
                                    } else {
                                        selectedPhotoIndex = index
                                    }
                                },
                                onLongClick = {
                                    selectedMediaUris = selectedMediaUris + photo.uri
                                }
                            )
                    ) {
                        AsyncImage(
                            model = remember(photo.uri) {
                                thumbnailModel(context, photo.uri, photo.isVideo)
                            },
                            contentDescription = if (photo.isVideo) "앨범 동영상" else "앨범 사진",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (photo.isVideo) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(38.dp)
                                    .background(Color.Black.copy(alpha = 0.58f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    "동영상 재생",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        if (photo.uri in selectedMediaUris) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Indigo.copy(alpha = 0.32f))
                            )
                            Icon(
                                Icons.Default.CheckCircle,
                                "선택됨",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(7.dp)
                                    .size(27.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingTags) {
        TagEditorDialog(
            album = album,
            initialTags = tags,
            suggestedTags = allTags,
            onDismiss = { editingTags = false },
            onSave = {
                onSaveTags(album.bucketId, it)
                editingTags = false
            }
        )
    }

    if (showAlbumInfo) {
        AlbumInfoDialog(
            album = album,
            onDismiss = { showAlbumInfo = false }
        )
    }

    selectedPhotoIndex?.let { initialIndex ->
        FullScreenPhotoViewer(
            photos = photos,
            initialIndex = initialIndex,
            onShare = { shareMedia(context, listOf(it)) },
            onSetAlbumCover = {
                onSetAlbumCover(album.bucketId, it)
            },
            onDismiss = { selectedPhotoIndex = null }
        )
    }
}

@Composable
private fun AlbumInfoDialog(
    album: PhotoAlbum,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault()) }
    fun formatDate(seconds: Long): String =
        if (seconds > 0) dateFormat.format(Date(seconds * 1_000L)) else "정보 없음"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("앨범 정보", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = remember(album.coverUri, album.coverIsVideo) {
                        thumbnailModel(context, album.coverUri, album.coverIsVideo)
                    },
                    contentDescription = "${album.name} 대표 사진",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(Color(0xFFE9EAF0), RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(album.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                AlbumInfoRow("전체 미디어", "${album.photoCount}개")
                AlbumInfoRow(
                    "구성",
                    "사진 ${album.photoCount - album.videoCount}개 · 동영상 ${album.videoCount}개"
                )
                AlbumInfoRow(
                    "촬영 기간",
                    "${formatDate(album.oldestDateSeconds)} ~ " +
                        formatDate(album.newestCaptureDateSeconds)
                )
                AlbumInfoRow(
                    "저장 위치",
                    album.relativePath.ifBlank { "정보 없음" }
                )
                AlbumInfoRow("최근 추가", formatDate(album.newestDateSeconds))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )
}

private fun thumbnailModel(
    context: android.content.Context,
    uri: String,
    isVideo: Boolean
): Any {
    if (!isVideo) return uri
    return ImageRequest.Builder(context)
        .data(uri)
        .decoderFactory(VideoFrameDecoder.Factory())
        .build()
}

private fun shareMedia(
    context: android.content.Context,
    media: List<AlbumPhoto>
) {
    if (media.isEmpty()) return
    val uris = media.map { Uri.parse(it.uri) }
    val commonType = when {
        media.all { it.isVideo } -> "video/*"
        media.none { it.isVideo } -> "image/*"
        else -> "*/*"
    }
    val shareIntent = if (media.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = media.first().mimeType.ifBlank { commonType }
            putExtra(Intent.EXTRA_STREAM, uris.first())
            clipData = ClipData.newUri(
                context.contentResolver,
                "공유할 미디어",
                uris.first()
            )
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = commonType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            clipData = ClipData.newUri(
                context.contentResolver,
                "공유할 미디어",
                uris.first()
            ).also { clip ->
                uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            }
        }
    }.apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(shareIntent, "외부 앱으로 공유").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    )
}

@Composable
private fun AlbumInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            modifier = Modifier.weight(0.34f),
            color = Color(0xFF777B88),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            value,
            modifier = Modifier.weight(0.66f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun FullScreenPhotoViewer(
    photos: List<AlbumPhoto>,
    initialIndex: Int,
    onShare: (AlbumPhoto) -> Unit,
    onSetAlbumCover: (AlbumPhoto) -> Unit,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, photos.lastIndex),
        pageCount = { photos.size }
    )
    var controlsVisible by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                if (photos[page].isVideo) {
                    FullScreenVideo(
                        media = photos[page],
                        isActive = page == pagerState.currentPage,
                        onTap = { controlsVisible = !controlsVisible }
                    )
                } else {
                    ZoomablePhoto(
                        photo = photos[page],
                        onTap = { controlsVisible = !controlsVisible }
                    )
                }
            }

            if (controlsVisible) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .background(
                            Color.Black.copy(alpha = 0.58f),
                            RoundedCornerShape(24.dp)
                        )
                ) {
                    IconButton(
                        onClick = { onShare(photos[pagerState.currentPage]) }
                    ) {
                        Icon(Icons.Default.Share, "외부 앱으로 공유", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            onSetAlbumCover(photos[pagerState.currentPage])
                        }
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            "앨범 대표 미디어로 설정",
                            tint = Color.White
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .background(Color.Black.copy(alpha = 0.58f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "사진 닫기", tint = Color.White)
                }

                val currentPhoto = photos[pagerState.currentPage]
                val dateText = remember(currentPhoto.id) {
                    if (currentPhoto.dateTakenMillis > 0) {
                        SimpleDateFormat(
                            "yyyy년 M월 d일 HH:mm",
                            Locale.getDefault()
                        ).format(Date(currentPhoto.dateTakenMillis))
                    } else {
                        "촬영 날짜 정보 없음"
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        dateText,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${pagerState.currentPage + 1} / ${photos.size}",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenVideo(
    media: AlbumPhoto,
    isActive: Boolean,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    val player = remember(media.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(media.uri)))
            prepare()
            playWhenReady = false
        }
    }
    LaunchedEffect(isActive, player) {
        if (!isActive) player.pause()
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = {
            PlayerView(it).apply {
                this.player = player
                useController = true
                setOnClickListener { onTap() }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ZoomablePhoto(
    photo: AlbumPhoto,
    onTap: () -> Unit
) {
    var scale by remember(photo.id) { mutableStateOf(1f) }
    var offset by remember(photo.id) { mutableStateOf(Offset.Zero) }
    var imageWidth by remember(photo.id) { mutableStateOf(0f) }
    var imageHeight by remember(photo.id) { mutableStateOf(0f) }
    val interactionSource = remember(photo.id) { MutableInteractionSource() }

    fun boundedOffset(candidate: Offset, atScale: Float): Offset {
        val maxX = imageWidth * (atScale - 1f) / 2f
        val maxY = imageHeight * (atScale - 1f) / 2f
        return Offset(
            x = candidate.x.coerceIn(-maxX, maxX),
            y = candidate.y.coerceIn(-maxY, maxY)
        )
    }

    AsyncImage(
        model = photo.uri,
        contentDescription = "확대된 사진",
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                imageWidth = it.width.toFloat()
                imageHeight = it.height.toFloat()
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .pointerInput(photo.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val shouldTransform =
                            pressedCount >= 2 || (scale > 1f && pan != Offset.Zero)

                        if (shouldTransform) {
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            offset = if (newScale == 1f) {
                                Offset.Zero
                            } else {
                                boundedOffset(offset + pan, newScale)
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap
            ),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun TagManagerDialog(
    tags: List<String>,
    groups: Map<String, Set<String>>,
    initialRenamingTag: String? = null,
    initialDeletingTag: String? = null,
    onDismiss: () -> Unit,
    onRename: (String, String) -> Boolean,
    onDelete: (String) -> Unit,
    onSaveGroup: (String?, String, Set<String>) -> Boolean,
    onDeleteGroup: (String) -> Unit
) {
    var renamingTag by remember(initialRenamingTag) { mutableStateOf(initialRenamingTag) }
    var deletingTag by remember(initialDeletingTag) { mutableStateOf(initialDeletingTag) }
    var editingGroup by remember { mutableStateOf<String?>(null) }
    var showGroupEditor by remember { mutableStateOf(false) }
    var deletingGroup by remember { mutableStateOf<String?>(null) }
    var newName by remember(initialRenamingTag) { mutableStateOf(initialRenamingTag.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("태그 관리", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.height(430.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Button(
                        enabled = tags.isNotEmpty(),
                        onClick = {
                            editingGroup = null
                            showGroupEditor = true
                        }
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Text(" 태그 그룹 만들기")
                    }
                    if (tags.isEmpty()) {
                        Text(
                            "태그를 먼저 만든 뒤 그룹을 만들 수 있어요.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = Color(0xFF626673)
                        )
                    }
                }

                if (groups.isNotEmpty()) {
                    item {
                        Text(
                            "태그 그룹",
                            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                            fontWeight = FontWeight.Bold,
                            color = Indigo
                        )
                    }
                    items(groups.keys.sorted(), key = { "group:$it" }) { groupName ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(groupName, fontWeight = FontWeight.Bold)
                                Text(
                                    groups[groupName].orEmpty().sorted().joinToString(" · "),
                                    color = Color(0xFF777B88),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = {
                                editingGroup = groupName
                                showGroupEditor = true
                            }) {
                                Icon(Icons.Default.Edit, "$groupName 그룹 수정", tint = Indigo)
                            }
                            IconButton(onClick = { deletingGroup = groupName }) {
                                Icon(
                                    Icons.Default.Delete,
                                    "$groupName 그룹 삭제",
                                    tint = Color(0xFFC44747)
                                )
                            }
                        }
                    }
                }

                if (tags.isNotEmpty()) {
                    item {
                        Text(
                            "태그",
                            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(tags, key = { it }) { tag ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#$tag", modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                renamingTag = tag
                                newName = tag
                            }) {
                                Icon(Icons.Default.Edit, "$tag 이름 변경", tint = Indigo)
                            }
                            IconButton(onClick = { deletingTag = tag }) {
                                Icon(
                                    Icons.Default.Delete,
                                    "$tag 삭제",
                                    tint = Color(0xFFC44747)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )

    renamingTag?.let { oldName ->
        AlertDialog(
            onDismissRequest = { renamingTag = null },
            title = { Text("태그 이름 변경") },
            text = {
                Column {
                    Text(
                        "#$oldName 태그가 붙은 모든 앨범에 새 이름이 적용됩니다.",
                        color = Color(0xFF626673)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("새 태그명") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = newName.trim().removePrefix("#").isNotEmpty() &&
                        newName.trim().removePrefix("#") != oldName,
                    onClick = {
                        onRename(oldName, newName)
                        renamingTag = null
                    }
                ) { Text("변경") }
            },
            dismissButton = {
                TextButton(onClick = { renamingTag = null }) { Text("취소") }
            }
        )
    }

    deletingTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { deletingTag = null },
            title = { Text("태그 삭제") },
            text = {
                Text(
                    "#$tag 태그를 모든 앨범에서 삭제할까요?\n사진과 앨범은 삭제되지 않습니다."
                )
            },
            confirmButton = {
                Button(onClick = {
                    onDelete(tag)
                    deletingTag = null
                }) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTag = null }) { Text("취소") }
            }
        )
    }

    if (showGroupEditor) {
        TagGroupEditorDialog(
            originalName = editingGroup,
            initialTags = editingGroup?.let { groups[it].orEmpty() }.orEmpty(),
            allTags = tags,
            existingGroupNames = groups.keys,
            onDismiss = { showGroupEditor = false },
            onSave = { name, selectedTags ->
                val saved = onSaveGroup(editingGroup, name, selectedTags)
                if (saved) showGroupEditor = false
                saved
            }
        )
    }

    deletingGroup?.let { groupName ->
        AlertDialog(
            onDismissRequest = { deletingGroup = null },
            title = { Text("태그 그룹 삭제") },
            text = {
                Text(
                    "$groupName 그룹을 삭제할까요?\n그룹 안의 태그와 앨범 연결은 유지됩니다."
                )
            },
            confirmButton = {
                Button(onClick = {
                    onDeleteGroup(groupName)
                    deletingGroup = null
                }) {
                    Text("그룹 삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingGroup = null }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun TagGroupEditorDialog(
    originalName: String?,
    initialTags: Set<String>,
    allTags: List<String>,
    existingGroupNames: Set<String>,
    onDismiss: () -> Unit,
    onSave: (String, Set<String>) -> Boolean
) {
    var groupName by remember(originalName) { mutableStateOf(originalName.orEmpty()) }
    var selectedTags by remember(originalName) { mutableStateOf(initialTags) }
    val normalizedName = groupName.trim()
    val duplicateName = normalizedName != originalName &&
        normalizedName in existingGroupNames

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (originalName == null) "태그 그룹 만들기" else "태그 그룹 수정",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("그룹 이름") },
                    placeholder = { Text("예: 학창시절") },
                    isError = duplicateName,
                    supportingText = {
                        if (duplicateName) Text("이미 같은 이름의 그룹이 있어요.")
                    },
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "포함할 태그",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    "다른 그룹에 있던 태그를 선택하면 이 그룹으로 이동합니다.",
                    color = Color(0xFF777B88),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    allTags.forEach { tag ->
                        FilterChip(
                            selected = tag in selectedTags,
                            onClick = {
                                selectedTags = if (tag in selectedTags) {
                                    selectedTags - tag
                                } else {
                                    selectedTags + tag
                                }
                            },
                            label = { Text(tag) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = normalizedName.isNotEmpty() &&
                    selectedTags.isNotEmpty() &&
                    !duplicateName,
                onClick = { onSave(normalizedName, selectedTags) }
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

@Composable
private fun BulkTagDialog(
    albumCount: Int,
    suggestedTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    var selectedTags by remember { mutableStateOf(emptySet<String>()) }
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${albumCount}개 앨범에 태그 추가", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "각 앨범의 기존 태그는 그대로 유지됩니다.",
                    color = Color(0xFF626673)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("새 태그") },
                    placeholder = { Text("예: 가족") },
                    singleLine = true,
                    keyboardActions = KeyboardActions(onDone = {
                        val tag = input.trim().removePrefix("#")
                        if (tag.isNotEmpty()) {
                            selectedTags = selectedTags + tag
                            input = ""
                        }
                    })
                )
                if (suggestedTags.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text("기존 태그", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(5.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        suggestedTags.forEach { tag ->
                            FilterChip(
                                selected = tag in selectedTags,
                                onClick = {
                                    selectedTags = if (tag in selectedTags) {
                                        selectedTags - tag
                                    } else {
                                        selectedTags + tag
                                    }
                                },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
                if (selectedTags.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "추가 예정: ${selectedTags.sorted().joinToString { "#$it" }}",
                        color = Indigo,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            val pending = input.trim().removePrefix("#")
            Button(
                enabled = selectedTags.isNotEmpty() || pending.isNotEmpty(),
                onClick = {
                    onSave(
                        if (pending.isEmpty()) selectedTags else selectedTags + pending
                    )
                }
            ) { Text("추가") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
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
