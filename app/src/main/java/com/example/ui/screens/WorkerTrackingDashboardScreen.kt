package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAFStoreSemanticColors

@Composable
fun WorkerTrackingDashboardScreen(
    viewModel: Any,
    currentUser: Any?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAFStoreSemanticColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(semantic.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = semantic.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = semantic.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "لوحة تتبع العمال والموظفين",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = semantic.textPrimary
                            )
                            Text(
                                text = "متابعة نشاط العمال والمبيعات الميدانية لحظياً عبر السحابة",
                                style = MaterialTheme.typography.bodySmall,
                                color = semantic.textSecondary
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = semantic.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "العمال النشطون حالياً",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = semantic.textPrimary
                    )
                    Divider(color = semantic.divider)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = semantic.success, modifier = Modifier.size(20.dp))
                            Text("أحمد محمد (عامل فرع الخرطوم)", color = semantic.textPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                        Badge(containerColor = semantic.success.copy(alpha = 0.2f)) {
                            Text("متصل", color = semantic.success, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduledReportInAppNotificationBanner(
    report: com.example.utils.ArchivedScheduledReport,
    onPreviewPdf: (com.example.utils.ArchivedScheduledReport) -> Unit,
    onSendEmailWithPdf: (com.example.utils.ArchivedScheduledReport) -> Unit,
    onDismissBanner: (com.example.utils.ArchivedScheduledReport) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = semantic.primary.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = semantic.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "تقرير دوري مجدول",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = semantic.textPrimary
                )
                Text(
                    text = "تم تجهيز التقرير الأسبوعي للمبيعات وجدولة إرساله بنجاح.",
                    style = MaterialTheme.typography.bodySmall,
                    color = semantic.textSecondary
                )
            }
            TextButton(onClick = { onDismissBanner(report) }) {
                Text("إغلاق", color = semantic.primary)
            }
        }
    }
}
