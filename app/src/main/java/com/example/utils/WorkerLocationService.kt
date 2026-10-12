package com.example.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.IBinder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.util.UUID

class WorkerLocationService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("worker_tracking", "Worker Tracking", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification = Notification.Builder(this, "worker_tracking")
            .setContentTitle("Worker Tracking Active")
            .setContentText("Syncing location every 5 minutes")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startTracking()
        return START_STICKY
    }

    private fun startTracking() {
        serviceScope.launch {
            while (isActive) {
                val user = auth.currentUser
                if (user != null) {
                    try {
                        val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                        val location = try {
                            locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        } catch (_: SecurityException) {
                            null
                        }
                        if (location != null) {
                            val data = hashMapOf(
                                "lat" to location.latitude,
                                "lng" to location.longitude,
                                "timestamp" to System.currentTimeMillis()
                            )
                            db.collection("users").document(user.uid)
                                .collection("location_history")
                                .document(UUID.randomUUID().toString())
                                .set(data)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                delay(5 * 60 * 1000L) // 5 minutes interval
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
