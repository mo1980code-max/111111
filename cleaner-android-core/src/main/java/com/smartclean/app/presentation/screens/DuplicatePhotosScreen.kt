package com.smartclean.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartclean.app.domain.model.SimilarPhotoGroup
import com.smartclean.app.domain.model.SimilarPhotoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatePhotosScreen(
    photoGroups: List<SimilarPhotoGroup>,
    onDeleteSelectedPhotos: (List<SimilarPhotoItem>) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedList by remember { mutableStateOf(photoGroups) }

    val totalSelectedCount = selectedList.flatMap { it.photos }.count { it.isSelectedForDeletion }
    val totalSavedBytes = selectedList.flatMap { it.photos }
        .filter { it.isSelectedForDeletion }
        .sumOf { it.fileSize }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الصور المتشابهة والمكررة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B0F19),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (totalSelectedCount > 0) {
                Surface(
                    color = Color(0xFF111827),
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "محدد: $totalSelectedCount صورة",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "توفير: ${totalSavedBytes / (1024 * 1024)} MB",
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حذف المحدد بأمان", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0B0F19)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "حدد الذكاء الاصطناعي النسخ المكررة الأقل دقة تلقائياً مع الاحتفاظ بالنسخة الأفضل.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            items(selectedList) { group ->
                PhotoGroupCard(
                    group = group,
                    onTogglePhoto = { photo ->
                        // Toggle item selection
                        photo.isSelectedForDeletion = !photo.isSelectedForDeletion
                    }
                )
            }
        }
    }

    // Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFF43F5E)) },
            title = { Text("تأكيد حذف $totalSelectedCount صورة") },
            text = { Text("هل أنت متأكد من رغبتك في حذف الصور المحددة؟ سيتم تحرير مساحة ${totalSavedBytes / (1024 * 1024)} MB فوراً.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        val itemsToDelete = selectedList.flatMap { it.photos }.filter { it.isSelectedForDeletion }
                        onDeleteSelectedPhotos(itemsToDelete)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFF94A3B8)
        )
    }
}

@Composable
fun PhotoGroupCard(
    group: SimilarPhotoGroup,
    onTogglePhoto: (SimilarPhotoItem) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مجموعة صور متشابهة (${group.photos.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "تطابق ${group.similarityPercentage}%",
                    color = Color(0xFFA855F7),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(Color(0xFFA855F7).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(group.photos) { photo ->
                    PhotoItemThumbnail(photo = photo, onToggle = { onTogglePhoto(photo) })
                }
            }
        }
    }
}

@Composable
fun PhotoItemThumbnail(
    photo: SimilarPhotoItem,
    onToggle: () -> Unit
) {
    var isChecked by remember { mutableStateOf(photo.isSelectedForDeletion) }

    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(
                width = 1.5.dp,
                color = if (photo.isBestQuality) Color(0xFF10B981) else if (isChecked) Color(0xFFF43F5E) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable {
                isChecked = !isChecked
                photo.isSelectedForDeletion = isChecked
                onToggle()
            }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF64748B))

            if (photo.isBestQuality) {
                Text(
                    text = "الأفضل ✓",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(Color(0xFF10B981), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            Checkbox(
                checked = isChecked,
                onCheckedChange = {
                    isChecked = it
                    photo.isSelectedForDeletion = it
                    onToggle()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFFF43F5E),
                    checkmarkColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${photo.width}x${photo.height}",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "${photo.fileSize / (1024 * 1024)} MB",
            color = Color(0xFF94A3B8),
            fontSize = 10.sp
        )
    }
}
