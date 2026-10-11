package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.StaffAccount
import com.example.data.firebase.StaffApprovalStatus
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top bar badge component for Primary Owner (Deffas Antar) showing pending manager requests & active trial accounts.
 */
@Composable
fun OwnerManagerRequestsNotificationBadge(
    pendingCount: Int,
    trialCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = pendingCount + trialCount
    if (totalCount <= 0) return

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF59E0B).copy(alpha = 0.18f),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
        modifier = modifier.testTag("owner_manager_requests_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "طلبات المدراء الجدد",
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(16.dp)
                )
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEF4444),
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                ) {}
            }
            Text(
                text = "$totalCount طلب/حساب تجريبي",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFB45309)
            )
        }
    }
}

/**
 * Full-screen non-dismissible Access Denied screen displayed to managers whose
 * 7-day trial period has expired and have not been permanently activated by the owner.
 * Blocks application access until activated by the primary owner (Deffas Antar),
 * and provides direct action buttons to request activation and contact the owner.
 */
@Composable
fun TrialExpiredOverlay(
    currentUser: User,
    onRequestApprovalFromOwner: () -> Unit,
    onRefreshStatus: () -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val semantic = LocalAFStoreSemanticColors.current

    var requestSentState by remember { mutableStateOf(false) }

    // Intercept back button to strictly enforce access denial
    BackHandler {
        Toast.makeText(
            context,
            "تم إغلاق الوصول: انتهت فترة التجربة ويجب تفعيل الحساب من المالك (Deffas Antar) للمتابعة.",
            Toast.LENGTH_LONG
        ).show()
    }

    val expiryDateStr = remember(currentUser.trialEndsAt) {
        if (currentUser.trialEndsAt > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(currentUser.trialEndsAt))
        } else {
            "غير محدد"
        }
    }

    val ownerPhone = "0662931569"
    val ownerName = "Deffas Antar"

    val whatsappRequestMessage = remember(currentUser.fullName, currentUser.storeName) {
        "السلام عليكم ورحمة الله، أنا المدير ${currentUser.fullName} لمتجر (${currentUser.storeName}). انتهت فترة التجربة المجانية لحسابي وأطلب تفعيل الحساب بشكل دائم للاستمرار في العمل. شكراً لك."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F17).copy(alpha = 0.94f))
            .padding(16.dp)
            .testTag("access_denied_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, Color(0xFFEF4444)),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    // Badge: ACCESS DENIED
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "إغلاق الوصول • ACCESS DENIED",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }

                item {
                    // Shield Icon with Red Circle
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444).copy(alpha = 0.14f),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "تم إغلاق الوصول - انتهاء التجربة",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.5.sp,
                        color = Color(0xFFDC2626),
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    Text(
                        text = "مرحباً ${currentUser.fullName}، لقد تم إغلاق الوصول إلى النظام والمبيعات لمتجرك (${currentUser.storeName}) نظراً لانتهاء فترة التجربة الأسبوعية المجانية بتاريخ $expiryDateStr.\nيرجى طلب التفعيل من المالك الرئيسي مباشرة لفتح الحساب واستئناف العمل بشكل دائم.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                }

                item {
                    // Account info details card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "تفاصيل الحساب المعلق:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("اسم المدير:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(currentUser.fullName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("اسم المتجر:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(currentUser.storeName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("تاريخ الانتهاء:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(expiryDateStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("حالة الحساب:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("بانتظار تفعيل المالك", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            }
                        }
                    }
                }

                item {
                    // Owner direct info card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "بيانات المالك الرئيسي للاعتماد والتفعيل:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("المالك:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$ownerName (المالك والمطور)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("الهاتف المباشر:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(ownerPhone, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // If request was sent, show confirmation card
                if (requestSentState) {
                    item {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                                Text(
                                    text = "تم إرسال طلب التفعيل المباشر إلى المالك سحابياً بنجاح! يمكنك أيضاً التواصل المباشر أدناه للتفعيل الفوري.",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }
                }

                item {
                    // Main action button: Request activation from owner directly
                    Button(
                        onClick = {
                            onRequestApprovalFromOwner()
                            requestSentState = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("request_owner_activation_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (requestSentState) "إعادة إرسال طلب التفعيل للمالك" else "طلب التفعيل من المالك مباشرة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                item {
                    // Contact owner directly action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$ownerPhone"))
                                    context.startActivity(dialIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "هاتف المالك: $ownerPhone", Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("call_owner_btn")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اتصال هاتفياً", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val waUri = Uri.parse("https://api.whatsapp.com/send?phone=213${ownerPhone.removePrefix("0")}&text=${Uri.encode(whatsappRequestMessage)}")
                                    val waIntent = Intent(Intent.ACTION_VIEW, waUri)
                                    context.startActivity(waIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح واتساب، يرجى الاتصال هاتفياً", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("whatsapp_owner_btn")
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    // Copy message and check status row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(whatsappRequestMessage))
                                Toast.makeText(context, "تم نسخ رسالة طلب التفعيل للحافظة بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_request_msg_btn")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ الرسالة", fontSize = 11.5.sp)
                        }

                        Button(
                            onClick = onRefreshStatus,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("refresh_activation_status_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحقق من التفعيل", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    // Logout button
                    OutlinedButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("access_denied_logout_btn")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل الخروج والتبديل لحساب آخر", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog for regular users or cashier staff to request opening a new Manager account
 * with automatic 1-week cloud synchronized trial period.
 */
@Composable
fun RequestTrialManagerAccountDialog(
    onDismiss: () -> Unit,
    onSubmitRequest: (fullName: String, phone: String, storeName: String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        errorMessage = "يرجى كتابة الاسم الكامل"
                        return@Button
                    }
                    if (storeName.isBlank()) {
                        errorMessage = "يرجى كتابة اسم المتجر / الفرع"
                        return@Button
                    }
                    onSubmitRequest(fullName.trim(), phone.trim(), storeName.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تأكيد وفتح حساب تجريبي (7 أيام)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("طلب فتح حساب مدير جديد (تجربة أسبوع)", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "سيتم إنشاء حساب مدير جديد معزول تماماً وبفترة تجريبية تلقائية لمدة 7 أيام مع تزامن سحابي، وسيتم إرسال إشعار فوري للمالك الرئيسي (Deffas Antar) لتفعيل حسابك بشكل دائم.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; errorMessage = null },
                    label = { Text("الاسم الكامل للمدير") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف (اختياري)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it; errorMessage = null },
                    label = { Text("اسم المتجر / الفرع المستقل") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let { err ->
                    Text(text = err, color = Color(0xFFEF4444), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

/**
 * Control Card for Owner (Deffas Antar) in Settings to review, approve, extend, or reject
 * Trial Managers and pending account requests.
 */
@Composable
fun OwnerManagerRequestsAndTrialsCard(
    allAccounts: List<StaffAccount>,
    onApprovePermanently: (StaffAccount) -> Unit,
    onExtendTrialSevenDays: (StaffAccount) -> Unit,
    onSuspendAccount: (StaffAccount) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current

    val pendingOrTrialList = remember(allAccounts) {
        allAccounts.filter { acc ->
            acc.role.lowercase() == "manager" && (!acc.isApproved || acc.isTrial || !acc.isApprovedByOwner || acc.isTrialExpired || acc.approvalStatus == StaffApprovalStatus.PENDING)
        }
    }

    if (pendingOrTrialList.isEmpty()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
            border = BorderStroke(1.dp, semantic.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text("جميع حسابات المدراء مفعّلة ومستقرة", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    Text("لا توجد طلبات مدراء جديدة أو حسابات تجريبية بانتظار المراجعة.", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text("طلبات المدراء والحسابات التجريبية (${pendingOrTrialList.size})", fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp)
                        Text("مراجعة وتفعيل المدراء المستقلين بخصائص Multi-Tenant", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pendingOrTrialList.forEach { acc ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = acc.fullName.ifBlank { acc.email },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "المتجر: ${acc.branchName.ifBlank { "غير مسمى" }} • ${acc.phone.ifBlank { acc.email }}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                val badgeColor = when {
                                    acc.isTrialExpired -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                    acc.isTrial -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                    acc.isApproved -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    else -> Color(0xFF6B7280).copy(alpha = 0.15f)
                                }
                                val textColor = when {
                                    acc.isTrialExpired -> Color(0xFFDC2626)
                                    acc.isTrial -> Color(0xFFB45309)
                                    acc.isApproved -> Color(0xFF065F46)
                                    else -> Color(0xFF374151)
                                }
                                val badgeText = when {
                                    acc.isTrialExpired -> "منتهي التجربة (مغلق)"
                                    acc.isTrial -> "تجريبي (أسبوع)"
                                    else -> StaffApprovalStatus.labelAr(acc.approvalStatus)
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = badgeColor
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { onApprovePermanently(acc) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تفعيل دائم", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onExtendTrialSevenDays(acc) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+7 أيام تجربة", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { onSuspendAccount(acc) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("إيقاف", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
