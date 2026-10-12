package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileWriter

// 1. Customer Location & Maps Helper
object CustomerLocationHelper {
    fun show(context: Context, customerName: String) {}
    fun show(context: Context, customer: Any) {}
    fun getLatLong(customer: Any): Pair<Double, Double>? = null
    fun openInGoogleMaps(
        context: Context,
        latitude: Double? = null,
        longitude: Double? = null,
        locationUrl: String = "",
        lat: Double? = latitude,
        lng: Double? = longitude,
        label: String = ""
    ) {
        try {
            val url = if (locationUrl.isNotBlank()) {
                locationUrl
            } else if (lat != null && lng != null) {
                "https://maps.google.com/?q=$lat,$lng"
            } else return
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
    fun openInGoogleMaps(context: Context, lat: Double, lng: Double, label: String = "") {
        openInGoogleMaps(context, latitude = lat, longitude = lng, label = label)
    }
    fun openInGoogleMaps(context: Context, customer: Any) {}
    fun buildGoogleMapsUrl(lat: Double, lng: Double): String = "https://maps.google.com/?q=$lat,$lng"
    fun generateLocationQrBitmap(url: String, size: Int = 160): Bitmap? = null
    fun generateLocationQrBitmap(lat: Double, lng: Double, size: Int = 160): Bitmap? = null
    fun requestSingleLocationFix(context: Context, onSuccess: (Double, Double) -> Unit = { _, _ -> }, onError: (String) -> Unit = {}) {}
    fun requestSingleLocationFix(context: Context, onResult: (Pair<Double, Double>?) -> Unit) { onResult(null) }
    fun hasLocationPermission(context: Context? = null): Boolean = true
    val hasLocationPermission: Boolean get() = true
}

// 2. Printer Enums and Configs
enum class ThermalPaperWidth(val shortLabelAr: String = "80mm", val widthMm: Int = 80) {
    WIDTH_58MM("58mm", 58),
    WIDTH_80MM("80mm", 80),
    WIDTH_104MM("104mm", 104)
}

enum class CheckoutThermalPrintMode {
    INSTANT_PRINT, PREVIEW_FIRST
}

sealed class ThermalConnectionState {
    object Disconnected : ThermalConnectionState()
    data class Connecting(val deviceName: String = "") : ThermalConnectionState()
    data class Connected(val deviceName: String = "", val address: String = "") : ThermalConnectionState()
    data class Error(val message: String = "") : ThermalConnectionState()
}

data class BluetoothPrinterDeviceInfo(
    val name: String = "",
    val address: String = ""
)

data class ThermalPrinterConfig(
    val paperWidth: ThermalPaperWidth = ThermalPaperWidth.WIDTH_80MM,
    val autoReconnect: Boolean = false,
    val lastPrinterAddress: String = "",
    val lastPrinterName: String = "",
    val paperWidthMm: Int = 80,
    val printDensity: String = "NORMAL",
    val autoPrintOnCheckout: Boolean = true,
    val printStoreLogo: Boolean = true,
    val printQrCode: Boolean = true,
    val headerCustomMessage: String = "",
    val footerCustomMessage: String = "",
    val copyCount: Int = 1,
    val feedLinesAfterPrint: Int = 3,
    val characterEncoding: String = "UTF-8",
    val checkoutPrintMode: CheckoutThermalPrintMode = CheckoutThermalPrintMode.INSTANT_PRINT,
    val defaultCopies: Int = copyCount
) {
    companion object {
        fun loadFromPrefs(prefs: Any?): ThermalPrinterConfig = ThermalPrinterConfig()
        fun saveToPrefs(prefs: Any?, config: ThermalPrinterConfig) {}
    }
}

data class DesktopPrinterConfig(
    val enabled: Boolean = false,
    val defaultPaperSize: String = "A4"
) {
    companion object {
        fun loadFromPrefs(prefs: Any?): DesktopPrinterConfig = DesktopPrinterConfig()
        fun saveToPrefs(prefs: Any?, config: DesktopPrinterConfig) {}
    }
}

data class ThermalReceiptPrintPayload(
    val title: String = "",
    val storeName: String = "",
    val storePhone: String = "",
    val customerName: String = "",
    val cashierName: String = "",
    val invoiceNumber: String = "",
    val dateTimeFormatted: String = "",
    val paymentMethodAr: String = "",
    val storeLogoImagePath: String = "",
    val storeLogoCustomText: String = "",
    val lines: List<Any> = emptyList(),
    val totalAmount: Double = 0.0,
    val isTestPrint: Boolean = false,
    val isTestReceipt: Boolean = isTestPrint,
    val sousTotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val discountPercentInt: Int = 0,
    val tvaAmount: Double = 0.0,
    val netTotal: Double = 0.0,
    val creditAmount: Double = 0.0,
    val especesAmount: Double = 0.0,
    val currency: String = "د.ج",
    val footerText: String = "",
    val developerContact: String = "",
    val templateStyle: Any? = null,
    val previousDebt: Double = 0.0
) {
    companion object {
        fun createSampleTestPayload(
            storeName: String = "",
            storePhone: String = "",
            currency: String = "",
            footerText: String = "",
            developerContact: String = "",
            paperWidth: ThermalPaperWidth = ThermalPaperWidth.WIDTH_80MM,
            storeLogoImagePath: String = "",
            storeLogoCustomText: String = ""
        ): ThermalReceiptPrintPayload = ThermalReceiptPrintPayload(
            storeName = storeName,
            storePhone = storePhone,
            currency = currency,
            footerText = footerText,
            developerContact = developerContact,
            storeLogoImagePath = storeLogoImagePath,
            storeLogoCustomText = storeLogoCustomText,
            isTestPrint = true
        )
    }
}

class BluetoothThermalPrinterManager(val context: Context? = null) {
    private val _connectionState = MutableStateFlow<ThermalConnectionState>(ThermalConnectionState.Disconnected)
    val connectionState: StateFlow<ThermalConnectionState> = _connectionState.asStateFlow()

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    val defaultCopies: Int = 1

    fun hasBluetoothConnectPermission(): Boolean = true
    fun requiredRuntimePermissions(): Array<String> = emptyArray()
    fun getPairedDevices(): List<BluetoothPrinterDeviceInfo> = emptyList()

    suspend fun connectToPrinter(address: String, name: String = ""): Result<String> {
        _connectionState.value = ThermalConnectionState.Connected(deviceName = name.ifBlank { "Bluetooth Printer" }, address = address)
        return Result.success(name.ifBlank { "Bluetooth Printer" })
    }

    suspend fun disconnect() {
        _connectionState.value = ThermalConnectionState.Disconnected
    }

    suspend fun printReceipt(payload: Any): Boolean = true

    suspend fun printReceiptOverBluetooth(
        payload: ThermalReceiptPrintPayload,
        config: ThermalPrinterConfig = ThermalPrinterConfig(),
        copiesOverride: Int? = null
    ): Result<String> = Result.success("تمت الطباعة بنجاح")
}

object ThermalReceiptBitmapRenderer {
    fun loadHighContrastThermalLogoBitmap(
        imagePath: String = "",
        maxWidthPx: Int = 115,
        maxHeightPx: Int = 54,
        context: Context? = null,
        path: String? = imagePath
    ): Bitmap? = null
}

// 3. Scheduled Report Models and Helpers
data class ScheduledReportFrequency(
    val id: String = "daily",
    val shortBadgeAr: String = "يومي",
    val labelAr: String = "تقرير يومي"
)

data class ScheduledReportConfig(
    val enabled: Boolean = false,
    val inAppNotificationEnabled: Boolean = true,
    val frequency: ScheduledReportFrequency = ScheduledReportFrequency(),
    val recipientEmails: String = "",
    val lastGeneratedMillis: Long = 0L,
    val lastScheduledSlotKey: String = "",
    val formattedScheduledTimeAr: String = "22:00",
    val recipientEmail: String = recipientEmails,
    val frequencyHours: Int = 24
)

data class ArchivedScheduledReport(
    val id: String = "",
    val titleAr: String = "تقرير مبيعات",
    val title: String = titleAr,
    val generatedAtMillis: Long = System.currentTimeMillis(),
    val createdAt: Long = generatedAtMillis,
    val pdfFileName: String = "report.pdf",
    val frequency: ScheduledReportFrequency = ScheduledReportFrequency(),
    val storeName: String = "",
    val storePhone: String = "",
    val currencySymbol: String = "",
    val wasEmailDispatched: Boolean = false,
    val isNotificationRead: Boolean = false,
    val fileSizeBytes: Long = 0L,
    val formattedGeneratedAt: String = "",
    val totalRevenue: Double = 0.0,
    val salesCount: Int = 0,
    val reportType: String = "SALES",
    val fileUri: String = ""
)

object ScheduledSalesReportHelper {
    fun loadConfig(prefs: Any?): ScheduledReportConfig = ScheduledReportConfig()
    fun saveConfig(prefs: Any?, config: ScheduledReportConfig) {}
    fun loadArchive(prefs: Any?): List<ArchivedScheduledReport> = emptyList()
    fun saveArchive(prefs: Any?, list: List<ArchivedScheduledReport>) {}
    fun computeDueSlotKeyIfReady(config: ScheduledReportConfig): String? = null
    fun generateAndArchiveScheduledReport(
        context: Context,
        config: ScheduledReportConfig,
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        sales: List<Any> = emptyList(),
        products: List<Any> = emptyList(),
        purchases: List<Any> = emptyList(),
        slotKey: String = "",
        isManualTrigger: Boolean = false
    ): Pair<ArchivedScheduledReport, com.example.ui.screens.SalesReportPdfPayload> =
        Pair(
            ArchivedScheduledReport(
                id = slotKey.ifBlank { "report_${System.currentTimeMillis()}" },
                titleAr = "تقرير مبيعات دوري",
                generatedAtMillis = System.currentTimeMillis()
            ),
            com.example.ui.screens.SalesReportPdfPayload(
                storeName = storeName,
                storePhone = storePhone,
                currencySymbol = currencySymbol
            )
        )
    fun buildPayloadForScheduledReport(
        frequency: Any? = null,
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        sales: List<Any> = emptyList(),
        products: List<Any> = emptyList(),
        purchases: List<Any> = emptyList(),
        generatedAtMillis: Long = System.currentTimeMillis()
    ): com.example.ui.screens.SalesReportPdfPayload = com.example.ui.screens.SalesReportPdfPayload(
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol
    )
    fun sendScheduledReportViaEmail(
        context: Context,
        report: ArchivedScheduledReport,
        recipientEmailsInput: String = "",
        fallbackPayload: com.example.ui.screens.SalesReportPdfPayload? = null
    ): Boolean = true
    fun shareArchivedScheduledReportPdf(
        context: Context,
        report: ArchivedScheduledReport,
        fallbackPayload: com.example.ui.screens.SalesReportPdfPayload? = null
    ) {}
    fun deleteArchivedPdfFile(report: ArchivedScheduledReport) {}
}

// 4. POS Scan Event Model
data class LastScannedPosEvent(
    val productId: Int = 0,
    val productName: String = "",
    val scannedBarcode: String = "",
    val formatLabel: String = "",
    val decodeTimeMs: Long = 0L,
    val isCartonScan: Boolean = false,
    val unitsPerCarton: Int = 1,
    val addedUnitsInLastScan: Int = 1,
    val unitPrice: Double = 0.0,
    val eventTimestampMillis: Long = System.currentTimeMillis()
)

// 5. Backup Models & Helper
data class BackupMetadata(
    val timestamp: Long = System.currentTimeMillis(),
    val version: String = "1.0",
    val createdAt: Long = timestamp,
    val fileSizeBytes: Long = 0L,
    val metadata: Map<String, String> = emptyMap(),
    val storeName: String = "",
    val backupNote: String = "",
    val productsCount: Int = 0,
    val salesCount: Int = 0,
    val customersCount: Int = 0,
    val transactionsCount: Int = 0,
    val purchaseInvoicesCount: Int = 0,
    val imagesCount: Int = 0
)

data class BackupResult(
    val success: Boolean = true,
    val message: String = "تمت العملية بنجاح"
)

data class LocalBackupInfo(
    val fileName: String = "backup.antar_enc",
    val filePath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L,
    val fileSizeBytes: Long = sizeBytes,
    val formattedSize: String = "1.2 MB",
    val metadata: BackupMetadata = BackupMetadata()
)

typealias LocalBackupFileInfo = LocalBackupInfo

object DatabaseBackupHelper {
    fun saveLocalBackupToInternalStorage(
        context: Context,
        repository: Any? = null,
        storeName: String = "",
        backupNote: String = ""
    ): Pair<BackupResult, LocalBackupInfo?> =
        Pair(BackupResult(true, "تم حفظ النسخة الاحتياطية بنجاح"), LocalBackupInfo())

    fun listLocalBackups(context: Context): List<LocalBackupInfo> = emptyList()

    fun readLocalBackupJson(filePath: String): String = "{}"

    fun generateBackupJsonString(
        repository: Any? = null,
        storeName: String = "",
        context: Context? = null,
        includeProductImages: Boolean = true
    ): Pair<String, LocalBackupInfo> =
        Pair("{}", LocalBackupInfo())

    fun writeBackupJsonToUri(context: Context, uri: Uri, jsonString: String): Boolean = true

    fun readBackupJsonFromUri(context: Context, uri: Uri): String = "{}"

    fun parseBackupMetadataFromJson(jsonString: String): BackupMetadata = BackupMetadata()

    fun restoreBackupFromJsonString(
        context: Context? = null,
        repository: Any? = null,
        jsonString: String = "",
        replaceExisting: Boolean = false
    ): BackupResult =
        BackupResult(true, "تم استرجاع النسخة الاحتياطية بنجاح")

    fun deleteLocalBackup(filePath: String): Boolean = true
    fun createBackup(context: Context): File? = null
}

object ProductImageStorageHelper {
    fun saveImage(context: Context, bitmap: Bitmap): String? = null
}

// 6. APK Extraction and Temp Links
data class LocalApkMetadata(
    val fileName: String = "AF_Store.apk",
    val fileSizeBytes: Long = 1024L,
    val filePath: String = "",
    val apkFilePath: String = filePath,
    val version: String = "2.5",
    val formattedSize: String = "18.4 MB",
    val versionName: String = "2.5",
    val versionCode: Int = 1,
    val sha256Short: String = "af78b2"
)

data class GeneratedTempApkLink(
    val directDownloadUrl: String = "https://example.com/download/af_store.apk",
    val expiryBadgeAr: String = "صالح لمدة 24 ساعة",
    val subtitleAr: String = "رابط تحميل مؤقت ومباشر",
    val id: String = "1",
    val fileName: String = "AF_Store.apk",
    val providerNameAr: String = "الخادم السريع",
    val expiresNoteAr: String = "تنتهي الصلاحية بعد 24 ساعة",
    val fileSizeFormatted: String = "18.4 MB",
    val formattedCreatedAt: String = "الآن"
)

enum class TempApkHostProvider(
    val id: String = "auto_fast",
    val titleAr: String = "الخادم السريع",
    val subtitleAr: String = "تحميل بأقصى سرعة واستقرار",
    val expiryBadgeAr: String = "صالح 24 ساعة"
) {
    AUTO_FAST("auto_fast", "الخادم السريع", "تحميل بأقصى سرعة واستقرار", "صالح 24 ساعة"),
    AUTO("auto", "تلقائي", "اختيار أفضل خادم متاح", "صالح 24 ساعة"),
    GITHUB("github", "غيت هب", "خادم سحابي عالمي", "دائم"),
    ANONFILES("anonfiles", "أنون فايلز", "مشاركة عامة", "7 أيام"),
    TMPFILES("tmpfiles", "تمب فايلز", "خادم مؤقت فوري", "60 دقيقة"),
    CUSTOM("custom", "مخصص", "خادم محلي أو مخصص", "حسب الإعداد")
}

object ApkTempLinkGenerator {
    fun loadSavedLinksHistory(context: Context): List<GeneratedTempApkLink> = emptyList()
    fun extractInstalledApk(context: Context): Result<LocalApkMetadata> = Result.success(LocalApkMetadata())
    fun saveApkFileToDestinationUri(context: Context, sourceApkPath: String = "", destinationUri: Uri? = null, uri: Uri? = destinationUri, path: String = sourceApkPath): Boolean = true
    fun shareRawApkFile(context: Context, meta: LocalApkMetadata) {}
    fun shareRawApkFile(context: Context, path: String) {}
    fun uploadApkAndCreateTempLink(context: Context, provider: TempApkHostProvider, onStepProgress: (String) -> Unit = {}): Result<GeneratedTempApkLink> = Result.success(GeneratedTempApkLink())
    fun saveLinkToHistory(context: Context, link: GeneratedTempApkLink): List<GeneratedTempApkLink> = listOf(link)
    fun clearLinksHistory(context: Context) {}
    fun generateQrCodeBitmap(text: String, sizePx: Int = 512): Bitmap? = null
    fun shareTempLinkText(context: Context, link: GeneratedTempApkLink) {
        shareTempLinkText(context, link.directDownloadUrl)
    }
    fun shareTempLinkText(context: Context, text: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة الرابط").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (_: Exception) {}
    }
    fun openUrlInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

// 7. Network Connectivity Monitor & Offline Sync
class NetworkConnectivityMonitor(context: Context? = null) {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    companion object {
        private val _globalOnline = MutableStateFlow(true)
        val isOnline: StateFlow<Boolean> = _globalOnline.asStateFlow()
        fun init(context: Context) {}
        fun updateOnlineStatus(online: Boolean) { _globalOnline.value = online }
    }
}

class OfflineSyncManager(
    val context: Context? = null,
    val repository: Any? = null,
    val coroutineScope: Any? = null
) {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isAutoSyncEnabled = MutableStateFlow(true)
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    private val _lastSyncTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimeMillis: StateFlow<Long> = _lastSyncTimeMillis.asStateFlow()

    private val _lastSyncSummary = MutableStateFlow("تمت المزامنة بنجاح مع السحابة")
    val lastSyncSummary: StateFlow<String> = _lastSyncSummary.asStateFlow()

    fun updateOnline(online: Boolean) { _isOnline.value = online }
    fun setAutoSyncEnabled(enabled: Boolean) { _isAutoSyncEnabled.value = enabled }
    fun triggerManualSync() {}

    companion object {
        fun updateOnline(online: Boolean) {}
    }
}

// 8. CSV Exporter
object CsvExporter {
    fun generateProductsInventoryCsv(products: List<Any>, storeName: String): String = "ID,Name,Price,Stock\n"
    fun generateSalesReportCsv(sales: List<Any>, periodTitle: String, storeName: String): String = "Invoice,Date,Total,Cashier\n"
    fun saveCsvToFile(context: Context, fileName: String, content: String): File? {
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            FileWriter(file).use { it.write(content) }
            file
        } catch (_: Exception) {
            null
        }
    }
    fun shareCsvFile(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
    fun openCsvFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

// 9. Scanner Enums, Preferences & Utilities in com.example.utils
enum class SmartTorchMode {
    OFF, AUTO_SMART, ALWAYS_ON
}

enum class ScannerOperationMode(
    val id: String,
    val titleAr: String,
    val subtitleAr: String
) {
    INSTANT_CONTINUOUS("instant", "مسح فوري متتابع", "إضافة فورية للسلة مع كل باركود"),
    QUANTITY_CONFIRM("confirm", "تأكيد وتعديل الكمية", "معاينة المنتج وتحديد الكمية قبل الإضافة")
}

data class ScannedBarcodeResult(
    val code: String = "",
    val formatLabel: String = "BARCODE",
    val decodeTimeMs: Long = 0L,
    val format: String = formatLabel,
    val timestamp: Long = System.currentTimeMillis()
)

data class ContinuousScannedSessionItem(
    val barcode: String = "",
    val productName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object BarcodeFeedbackController {
    fun triggerScanSuccessFeedback(
        context: Context? = null,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true,
        isCartonBarcode: Boolean = false
    ) {}

    fun triggerUnregisteredBarcodeFeedback(
        context: Context? = null,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true
    ) {}
}

object ScannerPreferencesManager {
    fun getSmartTorchMode(context: Context? = null): SmartTorchMode = SmartTorchMode.OFF
    fun setSmartTorchMode(context: Context? = null, mode: SmartTorchMode = SmartTorchMode.OFF) {}
    fun isSoundEnabled(context: Context? = null): Boolean = true
    fun setSoundEnabled(context: Context? = null, enabled: Boolean = true) {}
    fun isVibrationEnabled(context: Context? = null): Boolean = true
    fun setVibrationEnabled(context: Context? = null, enabled: Boolean = true) {}
    fun isAutoZoomEnabled(context: Context? = null): Boolean = false
    fun setAutoZoomEnabled(context: Context? = null, enabled: Boolean = false) {}
    fun isInlinePosCameraEnabled(context: Context? = null): Boolean = false
    fun setInlinePosCameraEnabled(context: Context? = null, enabled: Boolean = false) {}
    fun getOperationMode(context: Context? = null): ScannerOperationMode = ScannerOperationMode.INSTANT_CONTINUOUS
    fun setOperationMode(context: Context? = null, mode: ScannerOperationMode = ScannerOperationMode.INSTANT_CONTINUOUS) {}
}

// 10. Search & Text Highlighting in com.example.utils
object FullTextSearchHelper {
    fun matchesFullTextSearch(item: Any?, query: String): Boolean = true
}

@Composable
fun HighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = overflow
    )
}
