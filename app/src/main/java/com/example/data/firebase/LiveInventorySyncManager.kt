package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.AntarRepository
import com.example.data.database.AntarDatabase
import com.example.data.models.Product
import com.example.data.models.RecordSyncState
import com.example.utils.ProductCloudImageHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * مدير المزامنة اللحظية الشاملة لمخزون السلع وصورها بين المدير وعماله التابعين.
 * يتيح ربط قاعدة بيانات Room المحلية بمستمع Firestore اللحظي في الخلفية
 * لمزامنة إضافة الأصناف الجديدة، التعديلات، حركة الكميات عند البيع أو الشراء فورياً.
 */
class LiveInventorySyncManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "LiveInventorySync"

        fun getInventoryDocId(product: Product): String {
            return when {
                product.firestoreId.isNotBlank() -> product.firestoreId
                product.barcode.isNotBlank() -> "bc_${product.barcode.trim().replace(Regex("[^a-zA-Z0-9]"), "_")}"
                product.id > 0 -> "pid_${product.id}"
                else -> "p_${System.currentTimeMillis()}"
            }
        }
    }

    private val db: FirebaseFirestore = StaffAccountRepository.getFirestoreDb(context)
    private val auth = Firebase.auth
    private val repository: AntarRepository by lazy {
        val database = AntarDatabase.getDatabase(context.applicationContext)
        AntarRepository(database.storeDao())
    }

    private var productsListenerRegistration: ListenerRegistration? = null
    private var currentOwnerUid: String = ""

    private val _isLiveSyncActive = MutableStateFlow(false)
    val isLiveSyncActive: StateFlow<Boolean> = _isLiveSyncActive.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long>(0L)
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    data class StockDeduction(
        val productId: Int,
        val barcode: String,
        val firestoreId: String,
        val unitsDeducted: Int,
        val resultingStock: Int
    )

    data class StockIncrease(
        val productId: Int,
        val barcode: String,
        val firestoreId: String,
        val unitsAdded: Int,
        val resultingStock: Int
    )

    /**
     * استخراج المعرف السحابي المشترك لمخزون المدير التابع له العامل.
     */
    fun resolveInventoryOwnerUid(activeAccount: StaffAccount?): String {
        if (activeAccount == null) {
            return auth.currentUser?.uid ?: ""
        }
        return when {
            activeAccount.isRegularManager -> activeAccount.userId
            activeAccount.role.equals("worker", ignoreCase = true) && activeAccount.parentManagerUid.isNotBlank() -> {
                activeAccount.parentManagerUid
            }
            activeAccount.isOwner -> activeAccount.userId.ifBlank { auth.currentUser?.uid ?: "" }
            else -> activeAccount.parentManagerUid.ifBlank { activeAccount.userId.ifBlank { auth.currentUser?.uid ?: "" } }
        }
    }

    /**
     * بدء مستمع المزامنة اللحظية لمخزون السلع وصورها في الخلفية.
     */
    fun startLiveInventorySync(
        activeAccount: StaffAccount,
        onStatusMessage: (String) -> Unit = {}
    ) {
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) {
            Log.w(TAG, "تعذر تحديد معرّف مخزون المدير للمزامنة الحية.")
            return
        }

        if (currentOwnerUid == targetOwnerUid && productsListenerRegistration != null) {
            return
        }

        stopLiveInventorySync()
        currentOwnerUid = targetOwnerUid
        _isLiveSyncActive.value = true

        Log.i(TAG, "بدء مراقبة المخزون اللحظي للمدير: $targetOwnerUid لحساب: ${activeAccount.fullName} (${activeAccount.role})")

        val productsCollection = db.collection("users").document(targetOwnerUid).collection("products")

        productsListenerRegistration = productsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "خطأ في استقبال تحديثات المخزون اللحظية: ${error.message}")
                _syncMessage.value = "خطأ في اتصال المزامنة: ${error.localizedMessage}"
                return@addSnapshotListener
            }

            if (snapshot == null || snapshot.isEmpty) {
                _lastSyncTimestamp.value = System.currentTimeMillis()
                return@addSnapshotListener
            }

            scope.launch(Dispatchers.IO) {
                try {
                    val remoteProducts = mutableListOf<Product>()
                    for (doc in snapshot.documents) {
                        val prod = doc.toObject(Product::class.java)
                        if (prod != null) {
                            if (prod.firestoreId.isBlank()) {
                                prod.firestoreId = doc.id
                            }
                            // التحقق من فك تشفير وتخزين صورة المنتج محلياً
                            val resolvedImagePath = ProductCloudImageHelper.ensureLocalProductImage(context, prod)
                            if (resolvedImagePath.isNotBlank()) {
                                prod.imagePath = resolvedImagePath
                            }
                            remoteProducts.add(prod)
                        }
                    }

                    if (remoteProducts.isNotEmpty()) {
                        val mergedCount = repository.mergeRemoteProductsWithTimestampResolution(remoteProducts)
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                        val feedback = "تمت مزامنة $mergedCount صنفاً من مخزون الإدارة بنجاح ✓"
                        _syncMessage.value = feedback
                        withContext(Dispatchers.Main) {
                            onStatusMessage(feedback)
                        }
                        Log.i(TAG, "تم دمج $mergedCount منتجاً سحابياً في قاعدة بيانات Room المحلية.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "فشل دمج السلع المستلمة من Firestore: ${e.message}", e)
                }
            }
        }
    }

    /**
     * إيقاف مستمع المزامنة الحية عند تسجيل الخروج.
     */
    fun stopLiveInventorySync() {
        productsListenerRegistration?.remove()
        productsListenerRegistration = null
        _isLiveSyncActive.value = false
        currentOwnerUid = ""
    }

    /**
     * رفع أو تحديث سلعة جديدة مع ضغط صورتها إلى Base64 لمزامنتها مع العمال فوراً.
     */
    suspend fun pushProductToCloud(
        product: Product,
        activeAccount: StaffAccount? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) {
            return@withContext Result.failure(IllegalStateException("لم يتم العثور على معرّف سحابي لمخزون المدير"))
        }

        try {
            val docId = getInventoryDocId(product)
            val docRef = db.collection("users").document(targetOwnerUid).collection("products").document(docId)

            // معالجة وضغط الصورة إلى Base64 Data URL خفيف
            val base64Image = if (product.imagePath.isNotBlank()) {
                ProductCloudImageHelper.compressLocalImageToBase64(context, product.imagePath) ?: product.imageUrl
            } else {
                product.imageUrl
            }

            val payload = hashMapOf<String, Any?>(
                "id" to product.id,
                "firestoreId" to docId,
                "userId" to targetOwnerUid,
                "name" to product.name.trim(),
                "purchasePrice" to product.purchasePrice,
                "price" to product.price,
                "stock" to product.stock,
                "unitsPerCarton" to product.unitsPerCarton.coerceAtLeast(1),
                "barcode" to product.barcode.trim(),
                "cartonBarcode" to product.cartonBarcode.trim(),
                "category" to product.category.ifBlank { "عام" },
                "minStockAlert" to product.minStockAlert,
                "imagePath" to product.imagePath,
                "imageUrl" to (base64Image ?: ""),
                "isDeleted" to product.isDeleted,
                "syncStatus" to RecordSyncState.SYNCED,
                "serverUpdatedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to System.currentTimeMillis()
            )

            docRef.set(payload, SetOptions.merge()).await()
            Log.i(TAG, "تم رفع وتحديث السلعة «${product.name}» في سحابة المدير ($targetOwnerUid).")
            Result.success(docId)
        } catch (e: Exception) {
            Log.e(TAG, "فشل رفع السلعة إلى Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * خصم كميات المخزون في Firestore دفعة واحدة وبطريقة ذرية عند اكتمال عملية بيع (POS).
     */
    suspend fun pushBatchStockDeductions(
        deductions: List<StockDeduction>,
        activeAccount: StaffAccount? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (deductions.isEmpty()) return@withContext Result.success(Unit)
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return@withContext Result.success(Unit)

        try {
            val batch = db.batch()
            val productsCollection = db.collection("users").document(targetOwnerUid).collection("products")

            for (item in deductions) {
                val docId = when {
                    item.firestoreId.isNotBlank() -> item.firestoreId
                    item.barcode.isNotBlank() -> "bc_${item.barcode.trim().replace(Regex("[^a-zA-Z0-9]"), "_")}"
                    item.productId > 0 -> "pid_${item.productId}"
                    else -> null
                }
                if (docId != null) {
                    val ref = productsCollection.document(docId)
                    val updates = mapOf(
                        "stock" to item.resultingStock,
                        "serverUpdatedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batch.set(ref, updates, SetOptions.merge())
                }
            }

            batch.commit().await()
            Log.i(TAG, "تم خصم مخزون ${deductions.size} أصناف سحابياً بنجاح بعد عملية البيع.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء خصم المخزون سحابياً: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * زيادة كميات المخزون في Firestore عند تسجيل مشتريات جديدة أو إعادة تزويد سريع.
     */
    suspend fun pushBatchStockIncreases(
        increases: List<StockIncrease>,
        activeAccount: StaffAccount? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (increases.isEmpty()) return@withContext Result.success(Unit)
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return@withContext Result.success(Unit)

        try {
            val batch = db.batch()
            val productsCollection = db.collection("users").document(targetOwnerUid).collection("products")

            for (item in increases) {
                val docId = when {
                    item.firestoreId.isNotBlank() -> item.firestoreId
                    item.barcode.isNotBlank() -> "bc_${item.barcode.trim().replace(Regex("[^a-zA-Z0-9]"), "_")}"
                    item.productId > 0 -> "pid_${item.productId}"
                    else -> null
                }
                if (docId != null) {
                    val ref = productsCollection.document(docId)
                    val updates = mapOf(
                        "stock" to item.resultingStock,
                        "serverUpdatedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batch.set(ref, updates, SetOptions.merge())
                }
            }

            batch.commit().await()
            Log.i(TAG, "تمت زيادة مخزون ${increases.size} أصناف سحابياً بنجاح بعد المشتريات/التزويد.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء زيادة المخزون سحابياً: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * حذف أو تعطيل سلعة سحابياً لجميع العمال التابعين.
     */
    suspend fun deleteProductFromCloud(
        product: Product,
        activeAccount: StaffAccount? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return@withContext Result.success(Unit)

        try {
            val docId = getInventoryDocId(product)
            val ref = db.collection("users").document(targetOwnerUid).collection("products").document(docId)
            // استخدام Soft Delete لتحديث الأجهزة
            val updates = mapOf(
                "isDeleted" to true,
                "serverUpdatedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to System.currentTimeMillis()
            )
            ref.set(updates, SetOptions.merge()).await()
            Log.i(TAG, "تم حذف السلعة ${product.name} سحابياً.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء حذف السلعة سحابياً: ${e.message}", e)
            Result.failure(e)
        }
    }
}
