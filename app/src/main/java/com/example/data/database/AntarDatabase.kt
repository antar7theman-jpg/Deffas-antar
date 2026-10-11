package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.StoreDao
import com.example.data.models.ActivityLog
import com.example.data.models.Customer
import com.example.data.models.CustomerTransaction
import com.example.data.models.Product
import com.example.data.models.PurchaseInvoice
import com.example.data.models.Sale
import com.example.data.models.SyncQueueItem
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.data.models.WorkerLocation
import com.example.data.models.WorkerPermissions
import com.example.data.models.WorkerRouteCoordinate

import androidx.room.TypeConverters

@Database(
    entities = [
        Product::class,
        Sale::class,
        Customer::class,
        CustomerTransaction::class,
        PurchaseInvoice::class,
        User::class,
        ActivityLog::class,
        SyncQueueItem::class,
        WorkerLocation::class,
        WorkerRouteCoordinate::class
    ],
    version = 13,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class AntarDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao

    companion object {
        @Volatile
        private var INSTANCE: AntarDatabase? = null

        internal val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Products offline sync columns + indices
                db.execSQL("ALTER TABLE products ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE products ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_barcode ON products(barcode)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_cartonBarcode ON products(cartonBarcode)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_syncStatus ON products(syncStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_updatedAt ON products(updatedAt)")

                // Sales offline sync columns + indices
                db.execSQL("ALTER TABLE sales ADD COLUMN invoiceGroupId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sales ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sales ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_saleDate ON sales(saleDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_productId ON sales(productId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_syncStatus ON sales(syncStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_invoiceGroupId ON sales(invoiceGroupId)")

                // Customers offline sync columns + indices
                db.execSQL("ALTER TABLE customers ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE customers ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_syncStatus ON customers(syncStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_updatedAt ON customers(updatedAt)")

                // Customer transactions offline sync columns + indices
                db.execSQL("ALTER TABLE customer_transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE customer_transactions ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_transactions_customerId ON customer_transactions(customerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_transactions_date ON customer_transactions(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_transactions_syncStatus ON customer_transactions(syncStatus)")

                // Purchase invoices offline sync columns + indices
                db.execSQL("ALTER TABLE purchase_invoices ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE purchase_invoices ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_invoices_dateMillis ON purchase_invoices(dateMillis)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_invoices_syncStatus ON purchase_invoices(syncStatus)")

                // Persistent Sync Queue table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_queue` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `entityId` INTEGER NOT NULL,
                        `operation` TEXT NOT NULL,
                        `titleAr` TEXT NOT NULL,
                        `detailsAr` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `lastError` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_status ON sync_queue(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_entityType_entityId ON sync_queue(entityType, entityId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_createdAt ON sync_queue(createdAt)")
            }
        }

        internal val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sales ADD COLUMN orderType TEXT NOT NULL DEFAULT 'direct'")
                db.execSQL("ALTER TABLE sales ADD COLUMN status TEXT NOT NULL DEFAULT 'completed'")
                db.execSQL("ALTER TABLE sales ADD COLUMN expectedDeliveryDate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sales ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_orderType ON sales(orderType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_status ON sales(status)")
            }
        }

        internal val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN latitude REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE customers ADD COLUMN longitude REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE customers ADD COLUMN locationUrl TEXT NOT NULL DEFAULT ''")
            }
        }

        internal val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `worker_locations` (
                        `userId` INTEGER NOT NULL,
                        `workerName` TEXT NOT NULL,
                        `storeName` TEXT NOT NULL DEFAULT 'AF store',
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `accuracy` REAL NOT NULL DEFAULT 0,
                        `speed` REAL NOT NULL DEFAULT 0,
                        `timestamp` INTEGER NOT NULL,
                        `isTrackingActive` INTEGER NOT NULL DEFAULT 1,
                        `statusNote` TEXT NOT NULL DEFAULT 'في وردية العمل',
                        `batteryPercent` INTEGER NOT NULL DEFAULT 100,
                        PRIMARY KEY(`userId`)
                    )
                """.trimIndent())
            }
        }

        private fun migrateCustomersAndWorkerRoute(db: SupportSQLiteDatabase) {
            // Check existing columns in customers table
            val customerCols = mutableSetOf<String>()
            try {
                val cursor = db.query("PRAGMA table_info(customers)")
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0) {
                        customerCols.add(cursor.getString(nameIndex).lowercase())
                    }
                }
                cursor.close()
            } catch (_: Exception) {}

            if (!customerCols.contains("email")) {
                try { db.execSQL("ALTER TABLE customers ADD COLUMN email TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            }
            if (!customerCols.contains("address")) {
                try { db.execSQL("ALTER TABLE customers ADD COLUMN address TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            }
            if (!customerCols.contains("loyaltypoints")) {
                try { db.execSQL("ALTER TABLE customers ADD COLUMN loyaltyPoints INTEGER NOT NULL DEFAULT 0") } catch (_: Exception) {}
            }
            if (!customerCols.contains("loyaltygoal")) {
                try { db.execSQL("ALTER TABLE customers ADD COLUMN loyaltyGoal INTEGER NOT NULL DEFAULT 500") } catch (_: Exception) {}
            }
            if (!customerCols.contains("loyaltyrewardvalue")) {
                try { db.execSQL("ALTER TABLE customers ADD COLUMN loyaltyRewardValue REAL NOT NULL DEFAULT 50.0") } catch (_: Exception) {}
            }
            if (!customerCols.contains("loyaltytier")) {
                try { db.execSQL("ALTER TABLE customers ADD COLUMN loyaltyTier TEXT NOT NULL DEFAULT 'Standard'") } catch (_: Exception) {}
            }
            if (!customerCols.contains("latitude")) {
                try { db.execSQL("ALTER TABLE customers ADD COLUMN latitude REAL DEFAULT NULL") } catch (_: Exception) {}
            }
            if (!customerCols.contains("longitude")) {
                try { db.execSQL("ALTER TABLE customers ADD COLUMN longitude REAL DEFAULT NULL") } catch (_: Exception) {}
            }
            if (!customerCols.contains("locationurl")) {
                try { db.execSQL("ALTER TABLE customers ADD COLUMN locationUrl TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            }

            // Customer indices
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_name` ON `customers` (`name`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_phone` ON `customers` (`phone`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_loyaltyTier` ON `customers` (`loyaltyTier`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_syncStatus_updatedAt` ON `customers` (`syncStatus`, `updatedAt`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_syncStatus` ON `customers` (`syncStatus`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_updatedAt` ON `customers` (`updatedAt`)") } catch (_: Exception) {}

            // Product indices
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_cartonBarcode` ON `products` (`cartonBarcode`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_name_category` ON `products` (`name`, `category`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_category` ON `products` (`category`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_syncStatus_updatedAt` ON `products` (`syncStatus`, `updatedAt`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_syncStatus` ON `products` (`syncStatus`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_updatedAt` ON `products` (`updatedAt`)") } catch (_: Exception) {}

            // Sales indices
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_saleDate` ON `sales` (`saleDate`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_productId` ON `sales` (`productId`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_customerId_saleDate` ON `sales` (`customerId`, `saleDate`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_customerId` ON `sales` (`customerId`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_syncStatus` ON `sales` (`syncStatus`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_invoiceGroupId` ON `sales` (`invoiceGroupId`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_orderType_status` ON `sales` (`orderType`, `status`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_orderType` ON `sales` (`orderType`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_status` ON `sales` (`status`)") } catch (_: Exception) {}

            // Customer Transaction indices
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_transactions_customerId_date` ON `customer_transactions` (`customerId`, `date`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_transactions_customerId` ON `customer_transactions` (`customerId`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_transactions_date` ON `customer_transactions` (`date`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_transactions_type` ON `customer_transactions` (`type`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_transactions_syncStatus` ON `customer_transactions` (`syncStatus`)") } catch (_: Exception) {}

            // Purchase Invoices indices
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoices_supplierName` ON `purchase_invoices` (`supplierName`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoices_dateMillis` ON `purchase_invoices` (`dateMillis`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoices_paymentStatus` ON `purchase_invoices` (`paymentStatus`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoices_syncStatus` ON `purchase_invoices` (`syncStatus`)") } catch (_: Exception) {}

            // Sync Queue indices
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_status_createdAt` ON `sync_queue` (`status`, `createdAt`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_status` ON `sync_queue` (`status`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_entityType_entityId` ON `sync_queue` (`entityType`, `entityId`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_createdAt` ON `sync_queue` (`createdAt`)") } catch (_: Exception) {}

            // Activity Log indices
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_logs_timestamp` ON `activity_logs` (`timestamp`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_logs_userId` ON `activity_logs` (`userId`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_logs_actionType` ON `activity_logs` (`actionType`)") } catch (_: Exception) {}

            // Worker Locations indices
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_locations_timestamp` ON `worker_locations` (`timestamp`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_locations_isTrackingActive` ON `worker_locations` (`isTrackingActive`)") } catch (_: Exception) {}

            // For worker_route_coordinates: verify columns match entity exactly
            val routeCols = mutableSetOf<String>()
            try {
                val routeCursor = db.query("PRAGMA table_info(worker_route_coordinates)")
                val nameIdx = routeCursor.getColumnIndex("name")
                while (routeCursor.moveToNext()) {
                    if (nameIdx >= 0) {
                        routeCols.add(routeCursor.getString(nameIdx).lowercase())
                    }
                }
                routeCursor.close()
            } catch (_: Exception) {}

            if (routeCols.isNotEmpty() && (!routeCols.contains("timestamp") || !routeCols.contains("workername"))) {
                try { db.execSQL("DROP TABLE IF EXISTS `worker_route_coordinates`") } catch (_: Exception) {}
            }

            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `worker_route_coordinates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` INTEGER NOT NULL,
                        `workerName` TEXT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `speed` REAL NOT NULL,
                        `accuracy` REAL NOT NULL,
                        `statusNote` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            } catch (_: Exception) {}

            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_route_coordinates_userId_timestamp` ON `worker_route_coordinates` (`userId`, `timestamp`)") } catch (_: Exception) {}
            try { db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_route_coordinates_timestamp` ON `worker_route_coordinates` (`timestamp`)") } catch (_: Exception) {}
        }

        internal val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateCustomersAndWorkerRoute(db)
            }
        }

        internal val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateCustomersAndWorkerRoute(db)
            }
        }

        internal val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateCustomersAndWorkerRoute(db)
            }
        }

        internal val MIGRATION_11_13 = object : Migration(11, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateCustomersAndWorkerRoute(db)
            }
        }

        internal val MIGRATION_10_13 = object : Migration(10, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateCustomersAndWorkerRoute(db)
            }
        }

        internal val MIGRATION_10_12 = object : Migration(10, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateCustomersAndWorkerRoute(db)
            }
        }

        internal val MIGRATION_8_12 = object : Migration(8, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateCustomersAndWorkerRoute(db)
            }
        }

        internal val MIGRATION_9_12 = object : Migration(9, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateCustomersAndWorkerRoute(db)
            }
        }

        fun getDatabase(context: Context): AntarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AntarDatabase::class.java,
                    "antar_store_database"
                )
                    .addMigrations(
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_11_13,
                        MIGRATION_10_13,
                        MIGRATION_10_12,
                        MIGRATION_8_12,
                        MIGRATION_9_12
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialDataIfEmpty(dao: StoreDao) {
            val existingUsers = dao.getAllUsersOnce()
            val ownerUser = User(
                id = 1,
                username = "Antar7theman@gmail.com",
                passwordHash = "Antar19930716",
                role = UserRole.SUPER_ADMIN,
                fullName = "Deffas antar",
                phone = "0662931569",
                storeName = "AF store",
                isActive = true,
                permissionsJson = WorkerPermissions(
                    canMakeSales = true,
                    canManageInventory = true,
                    canEditPrices = true,
                    canViewReports = true,
                    canManageDebtsAndCustomers = true,
                    canRefundOrVoid = true,
                    canApplyDiscounts = true,
                    canViewCostPrices = true,
                    canExportData = true,
                    canManagePurchaseInvoices = true
                ).toJsonString()
            )
            if (existingUsers.isEmpty()) {
                dao.insertUser(ownerUser)
                dao.insertUser(
                    User(
                        id = 2,
                        username = "cashier",
                        passwordHash = "1111",
                        role = UserRole.WORKER,
                        fullName = "كاشير المبيعات",
                        phone = "01100000000",
                        storeName = "AF store",
                        managerId = 1,
                        isActive = true,
                        permissionsJson = WorkerPermissions().toJsonString()
                    )
                )
            } else {
                val currentPrimary = existingUsers.find { it.id == 1 }
                if (currentPrimary == null ||
                    currentPrimary.username != "Antar7theman@gmail.com" ||
                    currentPrimary.fullName != "Deffas antar" ||
                    currentPrimary.passwordHash != "Antar19930716" ||
                    currentPrimary.role != UserRole.SUPER_ADMIN
                ) {
                    dao.insertUser(ownerUser)
                }
            }
        }
    }
}

