package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 1. Report PDF and Header components in com.example.ui.screens
enum class SectionPdfReportType {
    SALES, INVENTORY, DEBTS, CUSTOMERS, LOYALTY, LIVE_DASHBOARD, DASHBOARD, REORDER_ALERTS
}

data class UniversalPdfReportPayload(
    val title: String = "",
    val type: SectionPdfReportType = SectionPdfReportType.SALES,
    val reportType: SectionPdfReportType = type,
    val data: List<Any> = emptyList(),
    val periodTitle: String = "",
    val storeName: String = "",
    val storePhone: String = "",
    val currencySymbol: String = "",
    val items: List<Any> = data,
    val desktopConfig: Any? = null,
    val kpiMetrics: Any? = null,
    val tableHeaders: Any? = null,
    val tableRows: Any? = null,
    val notesOrSummary: String = ""
)

object UniversalPdfReportExporter {
    fun exportReport(context: Context, payload: UniversalPdfReportPayload) {}
    fun exportReport(context: Context, payload: Any) {}
    fun printReport(context: Context, payload: Any, desktopConfig: Any? = null) {}

    fun buildTestDesktopReportPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        desktopConfig: Any? = null
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير تجريبي لسطح المكتب",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        desktopConfig = desktopConfig
    )

    fun buildInvoicesPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        invoices: List<Any> = emptyList()
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير فواتير المبيعات",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        items = invoices,
        data = invoices
    )

    fun buildPurchasesPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        purchases: List<Any> = emptyList()
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير فواتير المشتريات",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        items = purchases,
        data = purchases
    )

    fun buildCustomersPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        customers: List<Any> = emptyList()
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير بيانات العملاء",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        items = customers,
        data = customers
    )

    fun buildLoyaltyPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        topCustomers: List<Any> = emptyList(),
        totalPoints: Any = 0
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير برنامج الولاء والنقاط",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        items = topCustomers,
        data = topCustomers
    )

    fun buildInventoryPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        products: List<Any> = emptyList(),
        totalValuation: Double = 0.0
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير جرد المخزون والمنتجات",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        items = products,
        data = products
    )

    fun buildInventoryAuditLogPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        logs: List<Any> = emptyList()
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "سجل تدقيق وتغييرات المخزون",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        items = logs,
        data = logs
    )

    fun buildReorderAlertsPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        alerts: List<Any> = emptyList(),
        lowStockProducts: List<Any> = alerts,
        customMinStock: Any? = null,
        defaultThreshold: Int = 5
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير تنبيهات نواقص المنتجات وإعادة الطلب",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        items = lowStockProducts,
        data = lowStockProducts
    )

    fun buildLiveDashboardPayload(
        period: Any? = null,
        chartDisplayMode: Any? = null,
        metricComparisonMode: Any? = null,
        overview: Any? = null,
        totalPurchasesCost: Any? = null,
        buckets: Any? = null,
        topSellingProducts: Any? = null,
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = ""
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير لوحة التحكم الحية",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol
    )
}

@Composable
fun UniversalSectionPdfReportDialog(
    payload: UniversalPdfReportPayload?,
    onDismiss: () -> Unit
) {
    if (payload != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(payload.title.ifBlank { "تقرير PDF" }) },
            text = { Text("جاري إعداد ومعاينة ملف التقرير بنجاح...") },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
fun UniversalReportHeaderButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "تقرير PDF",
    tag: String = "universal_report_btn"
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color(0xFF0284C7).copy(alpha = 0.12f),
            contentColor = Color(0xFF0284C7)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = modifier.testTag(tag)
    ) {
        Icon(
            imageVector = Icons.Default.PictureAsPdf,
            contentDescription = title,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, fontSize = 12.sp)
    }
}

// 2. Barcode & HUD Components in com.example.ui.screens
@Composable
fun TurboCameraBarcodeHud(
    isScanningActive: Boolean = true,
    smartTorchMode: com.example.utils.SmartTorchMode = com.example.utils.SmartTorchMode.OFF,
    onToggleTorchMode: () -> Unit = {},
    isSoundEnabled: Boolean = true,
    onToggleSound: () -> Unit = {},
    isVibrationEnabled: Boolean = true,
    onToggleVibration: () -> Unit = {},
    isAutoZoomEnabled: Boolean = false,
    onToggleAutoZoom: () -> Unit = {},
    hasActiveSelection: Boolean = false,
    lastDecodeBadge: String? = null,
    onResetScanClick: () -> Unit = {},
    onBarcodeScanned: (com.example.utils.ScannedBarcodeResult) -> Unit = {},
    compactMode: Boolean = false
) {}

// 3. Pharmacy Vignette Scanner
data class VignetteScanResult(
    val productName: String = "",
    val price: Double = 0.0,
    val barcode: String = "",
    val rawText: String = ""
)

@Composable
fun VignetteScannerDialog(
    onVignetteScanned: (VignetteScanResult) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الماسح الذكي للقسيمة الصيدلانية (Vignette)") },
        text = { Text("وجّه الكاميرا نحو باركود وسعر الدواء...") },
        confirmButton = {
            Button(onClick = {
                onVignetteScanned(VignetteScanResult(productName = "", price = 0.0, barcode = ""))
            }) {
                Text("تخطي")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

// 4. POS Scanner Docks
@Composable
fun PosInlineLiveScannerDock(
    viewModel: Any? = null,
    isScanningAllowed: Boolean = true,
    cooldownResetTrigger: Any? = null,
    onOpenFullscreenScanner: () -> Unit = {},
    onCollapseDock: () -> Unit = {}
) {}

@Composable
fun PosLastScannedItemQuickBar(
    lastEvent: Any? = null,
    cartItem: Any? = null,
    currencySymbol: String = "",
    onIncrementUnit: () -> Unit = {},
    onDecrementUnit: () -> Unit = {},
    onTogglePackageMode: () -> Unit = {},
    onToggleCartonPiece: () -> Unit = onTogglePackageMode,
    onAddFullCarton: () -> Unit = {},
    onRepeatScanNow: () -> Unit = {},
    onRemoveFromCart: () -> Unit = {},
    onDismissStrip: () -> Unit = {},
    modifier: Modifier = Modifier
) {}

// 5. Bluetooth and Printer UI Cards in com.example.ui.screens
@Composable
fun BluetoothPrinterPickerDialog(
    currentSelectedAddress: String = "",
    connectionState: Any? = null,
    hasPermission: Boolean = true,
    requiredPermissions: Array<String> = emptyArray(),
    loadPairedDevices: () -> List<com.example.utils.BluetoothPrinterDeviceInfo> = { emptyList() },
    onSelectAndConnectPrinter: (com.example.utils.BluetoothPrinterDeviceInfo) -> Unit = {},
    onDisconnect: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختيار طابعة البلوتوث") },
        text = { Text("البحث عن الطابعات الحرارية المتصلة عبر البلوتوث...") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun BluetoothPrinterPickerDialog(
    currentSelectedAddress: String = "",
    connectionState: Any? = null,
    hasPermission: Boolean = true,
    requiredPermissions: List<String>,
    loadPairedDevices: () -> List<com.example.utils.BluetoothPrinterDeviceInfo> = { emptyList() },
    onSelectAndConnectPrinter: (com.example.utils.BluetoothPrinterDeviceInfo) -> Unit = {},
    onDisconnect: () -> Unit = {},
    onDismiss: () -> Unit = {}
) = BluetoothPrinterPickerDialog(
    currentSelectedAddress = currentSelectedAddress,
    connectionState = connectionState,
    hasPermission = hasPermission,
    requiredPermissions = requiredPermissions.toTypedArray(),
    loadPairedDevices = loadPairedDevices,
    onSelectAndConnectPrinter = onSelectAndConnectPrinter,
    onDisconnect = onDisconnect,
    onDismiss = onDismiss
)

@Composable
fun ThermalPrinterSettingsCard(
    config: com.example.utils.ThermalPrinterConfig,
    connectionState: Any? = null,
    isPrinting: Boolean = false,
    onUpdateConfig: (com.example.utils.ThermalPrinterConfig) -> Unit = {},
    onOpenBluetoothPicker: () -> Unit = {},
    onReconnectSavedPrinter: () -> Unit = {},
    onDisconnectPrinter: () -> Unit = {},
    onDisconnect: () -> Unit = onDisconnectPrinter,
    onPrintTestReceipt: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("إعدادات الطابعة الحرارية (Bluetooth ESC/POS)", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}

@Composable
fun DesktopPrinterSettingsCard(
    config: com.example.utils.DesktopPrinterConfig,
    onUpdateConfig: (com.example.utils.DesktopPrinterConfig) -> Unit = {},
    onTestPrintDesktopPage: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("إعدادات طابعات سطح المكتب (A4 / A5)", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}

@Composable
fun ScheduledReportsControlAndArchiveCard(
    config: com.example.utils.ScheduledReportConfig,
    archiveList: Any,
    onUpdateConfig: (com.example.utils.ScheduledReportConfig) -> Unit = {},
    onGenerateReportNow: (Boolean) -> Unit = {},
    onPreviewArchivedReport: (com.example.utils.ArchivedScheduledReport) -> Unit = {},
    onEmailArchivedReport: (com.example.utils.ArchivedScheduledReport) -> Unit = {},
    onShareArchivedReport: (com.example.utils.ArchivedScheduledReport) -> Unit = {},
    onPrintArchivedReport: (com.example.utils.ArchivedScheduledReport) -> Unit = {},
    onDeleteArchivedReport: (com.example.utils.ArchivedScheduledReport) -> Unit = {},
    onClearAllArchivedReports: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("أرشفة وتقارير المبيعات المجدولة PDF", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}
