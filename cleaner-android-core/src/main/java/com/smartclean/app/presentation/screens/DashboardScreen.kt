package com.smartclean.app.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartclean.app.presentation.viewmodel.DashboardUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onNavigateToEmptyFolders: () -> Unit,
    onNavigateToDuplicatePhotos: () -> Unit,
    onNavigateToDuplicateVideos: () -> Unit,
    onNavigateToDuplicateFiles: () -> Unit,
    onQuickScanClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "منظف التخزين الذكي",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B0F19),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onQuickScanClicked,
                containerColor = Color(0xFF0EA5E9),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                text = { Text("فحص وتنظيف شامل", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = Color(0xFF0B0F19)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Storage Circular Progress Card
            StorageOverviewCard(
                usedGB = (uiState.storageStats.usedBytes / (1024.0 * 1024 * 1024)).toFloat(),
                totalGB = (uiState.storageStats.totalBytes / (1024.0 * 1024 * 1024)).toFloat(),
                usedPercentage = uiState.storageStats.usedPercentage,
                healthScore = uiState.storageStats.healthScore
            )

            // 2. Section Title
            Text(
                text = "أدوات التنظيف الرئيسية",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )

            // 3. 4 Main Interactive Cards Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Card 1: Empty Folders
                item {
                    CleanerActionCard(
                        title = "المجلدات الفارغة",
                        subtitle = "${uiState.emptyFoldersCount} مجلد فارغ",
                        icon = Icons.Default.FolderOff,
                        accentColor = Color(0xFFF59E0B),
                        onClick = onNavigateToEmptyFolders
                    )
                }

                // Card 2: Duplicate & Similar Photos
                item {
                    CleanerActionCard(
                        title = "مكررات الصور",
                        subtitle = "${uiState.duplicatePhotosCount} صورة (${uiState.duplicatePhotosSizeMB} MB)",
                        icon = Icons.Default.PhotoLibrary,
                        accentColor = Color(0xFFA855F7),
                        onClick = onNavigateToDuplicatePhotos
                    )
                }

                // Card 3: Duplicate Videos
                item {
                    CleanerActionCard(
                        title = "الفيديوهات المكررة",
                        subtitle = "${uiState.duplicateVideosCount} مقاطع (${uiState.duplicateVideosSizeMB} MB)",
                        icon = Icons.Default.VideoLibrary,
                        accentColor = Color(0xFF06B6D4),
                        onClick = onNavigateToDuplicateVideos
                    )
                }

                // Card 4: Duplicate Files & Documents
                item {
                    CleanerActionCard(
                        title = "الملفات المتطابقة",
                        subtitle = "${uiState.duplicateFilesCount} ملفات (${uiState.duplicateFilesSizeMB} MB)",
                        icon = Icons.Default.Difference,
                        accentColor = Color(0xFF10B981),
                        onClick = onNavigateToDuplicateFiles
                    )
                }
            }
        }
    }
}

@Composable
fun StorageOverviewCard(
    usedGB: Float,
    totalGB: Float,
    usedPercentage: Int,
    healthScore: Int
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "المساحة المستخدمة",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "%.1f / %.0f GB".format(usedGB, totalGB),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (healthScore > 80) Color(0xFF10B981) else Color(0xFFF59E0B))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "صحة التخزين: $healthScore/100",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Circular progress ring
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                CircularProgressIndicator(
                    progress = { usedPercentage / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = if (usedPercentage > 85) Color(0xFFF43F5E) else Color(0xFF38BDF8),
                    trackColor = Color(0xFF1E293B),
                    strokeWidth = 8.dp
                )
                Text(
                    text = "$usedPercentage%",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun CleanerActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}
