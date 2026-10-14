package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

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
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeClient = remember { BarcodeScanning.getClient() }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }

    // Handle Torch Mode changes
    LaunchedEffect(smartTorchMode, cameraControl) {
        cameraControl?.let { ctrl ->
            val enableTorch = when (smartTorchMode) {
                com.example.utils.SmartTorchMode.ALWAYS_ON -> true
                com.example.utils.SmartTorchMode.OFF -> false
                com.example.utils.SmartTorchMode.AUTO_SMART -> false // or ambient sensor logic
            }
            try {
                ctrl.enableTorch(enableTorch)
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("turbo_camera_barcode_hud")
    ) {
        // CameraX Preview View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }
            },
            update = { previewView ->
                if (!isScanningActive) return@AndroidView
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = androidx.camera.core.Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null && isScanningActive) {
                                        val startTime = System.currentTimeMillis()
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        barcodeClient.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                val barcode = barcodes.firstOrNull()
                                                if (barcode != null) {
                                                    val rawValue = barcode.rawValue ?: barcode.displayValue ?: ""
                                                    if (rawValue.isNotBlank()) {
                                                        val decodeDuration = System.currentTimeMillis() - startTime
                                                        val formatName = when (barcode.format) {
                                                            Barcode.FORMAT_EAN_13 -> "EAN-13"
                                                            Barcode.FORMAT_EAN_8 -> "EAN-8"
                                                            Barcode.FORMAT_QR_CODE -> "QR-Code"
                                                            Barcode.FORMAT_CODE_128 -> "Code-128"
                                                            Barcode.FORMAT_CODE_39 -> "Code-39"
                                                            Barcode.FORMAT_UPC_A -> "UPC-A"
                                                            Barcode.FORMAT_UPC_E -> "UPC-E"
                                                            else -> "BARCODE"
                                                        }
                                                        onBarcodeScanned(
                                                            com.example.utils.ScannedBarcodeResult(
                                                                code = rawValue.trim(),
                                                                formatLabel = formatName,
                                                                decodeTimeMs = decodeDuration
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                            .addOnFailureListener {
                                                // ignore frame parse failures silently for high performance
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                        cameraControl = camera.cameraControl
                        cameraInfo = camera.cameraInfo
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // Overlay HUD & Controls
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Torch, Sound, Vibration, Auto-Zoom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Torch Toggle
                IconButton(
                    onClick = onToggleTorchMode,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("scanner_torch_btn")
                ) {
                    Icon(
                        imageVector = when (smartTorchMode) {
                            com.example.utils.SmartTorchMode.ALWAYS_ON -> Icons.Default.FlashOn
                            com.example.utils.SmartTorchMode.AUTO_SMART -> Icons.Default.AutoMode
                            com.example.utils.SmartTorchMode.OFF -> Icons.Default.FlashOff
                        },
                        contentDescription = "الفلاش",
                        tint = if (smartTorchMode != com.example.utils.SmartTorchMode.OFF) Color(0xFFF59E0B) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Last Decode Badge
                if (!lastDecodeBadge.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = lastDecodeBadge,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Sound & Vibration Toggles
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "الصوت",
                            tint = if (isSoundEnabled) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleVibration,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isVibrationEnabled) Icons.Default.Vibration else Icons.Default.PhoneDisabled,
                            contentDescription = "الاهتزاز",
                            tint = if (isVibrationEnabled) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Center Scanning Laser / Target Reticle
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(if (compactMode) 90.dp else 125.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.dp.toPx()
                    val cornerLength = 24.dp.toPx()
                    val color = if (hasActiveSelection) Color(0xFF10B981) else Color(0xFF3B82F6)

                    // Top-Left
                    drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(cornerLength, 0f), strokeWidth)
                    drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(0f, cornerLength), strokeWidth)
                    // Top-Right
                    drawLine(color, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width - cornerLength, 0f), strokeWidth)
                    drawLine(color, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width, cornerLength), strokeWidth)
                    // Bottom-Left
                    drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(cornerLength, size.height), strokeWidth)
                    drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(0f, size.height - cornerLength), strokeWidth)
                    // Bottom-Right
                    drawLine(color, androidx.compose.ui.geometry.Offset(size.width, size.height), androidx.compose.ui.geometry.Offset(size.width - cornerLength, size.height), strokeWidth)
                    drawLine(color, androidx.compose.ui.geometry.Offset(size.width, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height - cornerLength), strokeWidth)
                }

                if (!isScanningActive) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = "تم التقاط الباركود ✓",
                            color = Color(0xFF10B981),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Bottom Bar: Reset Scan Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasActiveSelection || !isScanningActive) {
                    Button(
                        onClick = onResetScanClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("scanner_reset_scan_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مسح باركود جديد", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Text(
                        text = "وجّه العدسة نحو باركود المنتج للمسح الفوري ⚡",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}


// 3. Purchase Invoices, Receipts & Pharmacy Vignette Scanner (ML Kit Text Recognition)
data class OcrExtractedItem(
    val title: String = "",
    val price: Double = 0.0,
    val quantity: Double = 1.0
)

data class VignetteScanResult(
    val productName: String = "",
    val price: Double = 0.0,
    val barcode: String = "",
    val rawText: String = "",
    val supplierName: String = "",
    val invoiceNumber: String = "",
    val invoiceDate: String = "",
    val detectedItems: List<OcrExtractedItem> = emptyList()
)

object InvoiceReceiptOcrParser {
    fun parseInvoiceText(rawText: String): VignetteScanResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return VignetteScanResult(rawText = rawText)

        var detectedTotal = 0.0
        var detectedSupplier = ""
        var detectedInvoiceNo = ""
        var detectedDate = ""
        var detectedBarcode = ""
        val detectedItems = mutableListOf<OcrExtractedItem>()

        val totalKeywords = listOf(
            "total ttc", "total", "net à payer", "net a payer", "montant total",
            "المجموع", "الإجمالي", "الاجمالي", "المبلغ الإجمالي", "المبلغ الكلي", "مجموع الفاتورة",
            "montant", "somme", "a payer", "reste a payer"
        )
        val dateRegex = Regex("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{4}[/-]\d{1,2}[/-]\d{1,2})\b""")
        val invoiceNoRegex = Regex("""(?i)(?:Facture|Invoice|BL|BC|N°|No\.|رقم|فاتورة|وصل)\s*[:#\-]?\s*([A-Za-z0-9\-_/]+)""")
        val barcodeRegex = Regex("""\b(\d{8}|\d{12}|\d{13}|\d{14})\b""")

        for (line in lines) {
            val dateMatch = dateRegex.find(line)
            if (dateMatch != null && detectedDate.isBlank()) {
                detectedDate = dateMatch.value
            }
            val invMatch = invoiceNoRegex.find(line)
            if (invMatch != null && detectedInvoiceNo.isBlank() && invMatch.groupValues[1].length >= 2) {
                detectedInvoiceNo = invMatch.groupValues[1]
            }
            val barMatch = barcodeRegex.find(line)
            if (barMatch != null && detectedBarcode.isBlank()) {
                detectedBarcode = barMatch.value
            }
        }

        // Supplier name is usually one of the top non-date, non-number lines
        for (i in 0 until minOf(4, lines.size)) {
            val line = lines[i]
            if (!line.matches(Regex(""".*\b\d{4}\b.*""")) &&
                !line.contains("facture", ignoreCase = true) &&
                !line.contains("total", ignoreCase = true) &&
                !line.contains("فاتورة", ignoreCase = true) &&
                line.length >= 3
            ) {
                detectedSupplier = line
                break
            }
        }

        // Search for Total keyword
        var foundExplicitTotal = false
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            val lowerLine = line.lowercase()
            for (keyword in totalKeywords) {
                if (lowerLine.contains(keyword)) {
                    val parsedAmount = extractPriceFromLine(line)
                        ?: (if (i + 1 < lines.size) extractPriceFromLine(lines[i + 1]) else null)
                    if (parsedAmount != null && parsedAmount > 0.0) {
                        detectedTotal = parsedAmount
                        foundExplicitTotal = true
                        break
                    }
                }
            }
            if (foundExplicitTotal) break
        }

        // Fallback: Plausible monetary amount
        if (detectedTotal == 0.0) {
            val allNumbers = mutableListOf<Double>()
            for (line in lines) {
                val p = extractPriceFromLine(line)
                if (p != null && p > 0.0 && p < 10000000.0) {
                    allNumbers.add(p)
                }
            }
            if (allNumbers.isNotEmpty()) {
                detectedTotal = allNumbers.maxOrNull() ?: 0.0
            }
        }

        // Extract items (lines with both text and numeric amount)
        for (line in lines) {
            val p = extractPriceFromLine(line)
            if (p != null && p > 0.0) {
                val cleanTitle = line.replace(Regex("""[0-9]+[.,]?[0-9]*"""), "")
                    .replace("DA", "", ignoreCase = true)
                    .replace("DZD", "", ignoreCase = true)
                    .replace("د.ج", "")
                    .replace("دج", "")
                    .trim()
                if (cleanTitle.length >= 3 &&
                    !cleanTitle.contains("total", ignoreCase = true) &&
                    !cleanTitle.contains("مجموع", ignoreCase = true) &&
                    !cleanTitle.contains("net", ignoreCase = true)
                ) {
                    detectedItems.add(OcrExtractedItem(title = cleanTitle, price = p, quantity = 1.0))
                }
            }
        }

        val primaryProductName = if (detectedItems.isNotEmpty()) {
            detectedItems.first().title
        } else {
            detectedSupplier.ifBlank { "سلعة مستخرجة عبر الـ OCR" }
        }

        return VignetteScanResult(
            productName = primaryProductName,
            price = detectedTotal,
            barcode = detectedBarcode,
            rawText = rawText,
            supplierName = detectedSupplier,
            invoiceNumber = detectedInvoiceNo,
            invoiceDate = detectedDate,
            detectedItems = detectedItems
        )
    }

    private fun extractPriceFromLine(line: String): Double? {
        val matches = Regex("""(\d{1,3}(?:[ ,]\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?|\d+(?:,\d{1,2})?)""").findAll(line)
        for (m in matches) {
            val cleanStr = m.value.replace(" ", "").replace(",", ".")
            val d = cleanStr.toDoubleOrNull()
            if (d != null && d > 0.0) {
                return d
            }
        }
        return null
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun VignetteScannerDialog(
    onVignetteScanned: (VignetteScanResult) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isAnalyzing by remember { mutableStateOf(true) }
    var lastScannedText by remember { mutableStateOf("") }
    var parsedResult by remember { mutableStateOf<VignetteScanResult?>(null) }
    var isProcessingImage by remember { mutableStateOf(false) }

    // User editable values on detected result
    var editedProductName by remember { mutableStateOf("") }
    var editedPrice by remember { mutableStateOf("") }
    var editedBarcode by remember { mutableStateOf("") }
    var editedSupplier by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                isProcessingImage = true
                val inputImage = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val result = InvoiceReceiptOcrParser.parseInvoiceText(visionText.text)
                        lastScannedText = visionText.text
                        parsedResult = result
                        editedProductName = result.productName
                        editedPrice = if (result.price > 0.0) result.price.toString() else ""
                        editedBarcode = result.barcode
                        editedSupplier = result.supplierName
                        isAnalyzing = false
                    }
                    .addOnFailureListener {
                        isProcessingImage = false
                    }
                    .addOnCompleteListener {
                        isProcessingImage = false
                    }
            } catch (e: Exception) {
                isProcessingImage = false
            }
        }
    }

    fun triggerHapticFeedback() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
                .testTag("ocr_invoice_scanner_dialog"),
            color = Color(0xFF0B0F19)
        ) {
            if (!hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "إذن الكاميرا مطلوب",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "يحتاج ماسح الـ OCR إلى إذن الكاميرا لقراءة وتدقيق فواتير الشراء والوصولات الورقية مباشرة في المتجر.",
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.fillMaxWidth().testTag("grant_camera_permission_btn")
                            ) {
                                Text("منح صلاحية الكاميرا ✓", color = Color.White)
                            }
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إلغاء", color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. Camera Live Preview
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            val executor = Executors.newSingleThreadExecutor()
                            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                        if (!isAnalyzing) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }

                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            recognizer.process(image)
                                                .addOnSuccessListener { visionText ->
                                                    val raw = visionText.text
                                                    if (raw.isNotBlank() && raw.length > 5) {
                                                        lastScannedText = raw
                                                        val parsed = InvoiceReceiptOcrParser.parseInvoiceText(raw)
                                                        if (parsed.price > 0.0 || parsed.productName.isNotBlank()) {
                                                            parsedResult = parsed
                                                            if (editedPrice.isBlank() && parsed.price > 0.0) {
                                                                editedPrice = parsed.price.toString()
                                                            }
                                                            if (editedProductName.isBlank() && parsed.productName.isNotBlank()) {
                                                                editedProductName = parsed.productName
                                                            }
                                                            if (editedBarcode.isBlank() && parsed.barcode.isNotBlank()) {
                                                                editedBarcode = parsed.barcode
                                                            }
                                                            if (editedSupplier.isBlank() && parsed.supplierName.isNotBlank()) {
                                                                editedSupplier = parsed.supplierName
                                                            }
                                                        }
                                                    }
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }

                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                    cameraProvider.unbindAll()
                                    val cam = cameraProvider.bindToLifecycle(
                                        context as androidx.lifecycle.LifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageAnalysis
                                    )
                                    cameraControl = cam.cameraControl
                                } catch (e: Exception) {
                                    android.util.Log.e("OcrCamera", "Binding error: ${e.message}")
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 2. HUD Framing Guides for Receipts & Invoices
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent,
                            border = BorderStroke(2.dp, if (parsedResult != null) Color(0xFF10B981) else Color(0xFF38BDF8).copy(alpha = 0.8f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = if (parsedResult != null) "تم رصد بيانات الفاتورة بنجاح ✓" else "ضع فاتورة الشراء أو الوصل الورقي داخل الإطار",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                        .padding(horizontal = 14.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // 3. Top Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .testTag("ocr_scanner_close_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Flashlight / Torch toggle
                            IconButton(
                                onClick = {
                                    isTorchOn = !isTorchOn
                                    cameraControl?.enableTorch(isTorchOn)
                                },
                                modifier = Modifier.background(
                                    if (isTorchOn) Color(0xFFFBBF24) else Color.Black.copy(alpha = 0.6f),
                                    CircleShape
                                ).testTag("ocr_torch_toggle_btn")
                            ) {
                                Icon(
                                    if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "الفلاش",
                                    tint = if (isTorchOn) Color.Black else Color.White
                                )
                            }

                            // Pick from gallery button
                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .testTag("ocr_gallery_pick_btn")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "معرض الصور", tint = Color.White)
                            }
                        }
                    }

                    // 4. Bottom Controls & Detected Results Sheet
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Color(0xFF0F172A).copy(alpha = 0.95f),
                                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ماسح فواتير الشراء والوصولات (OCR)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAnalyzing) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, if (isAnalyzing) Color(0xFF0284C7) else Color(0xFF10B981))
                            ) {
                                Text(
                                    text = if (isAnalyzing) "مسح حي 🔴" else "تم التثبيت ⏸",
                                    color = if (isAnalyzing) Color(0xFF38BDF8) else Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Extracted Details Form (Editable before saving)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editedProductName,
                                onValueChange = { editedProductName = it },
                                label = { Text("اسم السلعة / البيان", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).testTag("ocr_product_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                maxLines = 1
                            )

                            OutlinedTextField(
                                value = editedPrice,
                                onValueChange = { editedPrice = it },
                                label = { Text("السعر / الإجمالي (د.ج)", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).testTag("ocr_price_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF10B981),
                                    unfocusedTextColor = Color(0xFF34D399),
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                maxLines = 1
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editedSupplier,
                                onValueChange = { editedSupplier = it },
                                label = { Text("المورد / اسم المحل", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).testTag("ocr_supplier_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                maxLines = 1
                            )

                            OutlinedTextField(
                                value = editedBarcode,
                                onValueChange = { editedBarcode = it },
                                label = { Text("الباركود إن وجد", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).testTag("ocr_barcode_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                maxLines = 1
                            )
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    triggerHapticFeedback()
                                    val finalPrice = editedPrice.toDoubleOrNull() ?: parsedResult?.price ?: 0.0
                                    val finalResult = VignetteScanResult(
                                        productName = editedProductName.ifBlank { parsedResult?.productName ?: "منتج مستخرج عبر OCR" },
                                        price = finalPrice,
                                        barcode = editedBarcode.ifBlank { parsedResult?.barcode ?: "" },
                                        rawText = lastScannedText,
                                        supplierName = editedSupplier.ifBlank { parsedResult?.supplierName ?: "" },
                                        invoiceNumber = parsedResult?.invoiceNumber ?: "",
                                        invoiceDate = parsedResult?.invoiceDate ?: "",
                                        detectedItems = parsedResult?.detectedItems ?: emptyList()
                                    )
                                    onVignetteScanned(finalResult)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                modifier = Modifier.weight(1f).testTag("ocr_confirm_save_btn")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تأكيد واستيراد ✓", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    isAnalyzing = !isAnalyzing
                                    if (isAnalyzing) {
                                        parsedResult = null
                                        editedProductName = ""
                                        editedPrice = ""
                                        editedBarcode = ""
                                        editedSupplier = ""
                                    }
                                },
                                modifier = Modifier.weight(0.7f).testTag("ocr_toggle_freeze_btn")
                            ) {
                                Icon(
                                    if (isAnalyzing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (isAnalyzing) "تثبيت" else "استئناف")
                            }
                        }
                    }
                }
            }
        }
    }
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
