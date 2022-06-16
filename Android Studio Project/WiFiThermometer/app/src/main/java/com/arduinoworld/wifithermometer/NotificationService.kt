package com.arduinoworld.wifithermometer

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import kotlin.collections.ArrayList


@Suppress("DEPRECATION")
class NotificationService : Service() {
    lateinit var gson : Gson
    lateinit var checkTimeHandler : Handler
    lateinit var contentIntent : PendingIntent
    lateinit var sharedPreferences : SharedPreferences
    lateinit var firebaseAuth : FirebaseAuth
    lateinit var humidityNode : DatabaseReference
    lateinit var temperatureNode : DatabaseReference
    lateinit var powerManager : PowerManager
    lateinit var wakeLock : PowerManager.WakeLock

    private var notificationTimestampsArray = ArrayList<String>()
    private var notificationTimestampsArrayJson = ""
    private var timeNotificationChannels = 0
    private var isNotificationShowed = true
    private var isNotificationSound = true
    private var isTemperatureShowed = true
    private var isHumidityShowed = true
    private var notificationText = ""
    private var notificationType = 1
    private var temperature = 0
    private var humidity = 0
    private var hours = ""
    private var minutes = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        gson = Gson()
        sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP, "WiFiThermometer:TimeNotificationLock")
        firebaseAuth = FirebaseAuth.getInstance()
        temperatureNode = FirebaseDatabase.getInstance().getReference("users").child(firebaseAuth.currentUser!!.uid).child("temperature")
        humidityNode = FirebaseDatabase.getInstance().getReference("users").child(firebaseAuth.currentUser!!.uid).child("humidity")
        contentIntent = PendingIntent.getActivity(baseContext, 0, Intent(this, NotificationsActivity::class.java), 0)

        isTemperatureShowed = sharedPreferences.getBoolean("ShowTemperature", true)
        isHumidityShowed = sharedPreferences.getBoolean("ShowHumidity", true)
        isNotificationSound = sharedPreferences.getBoolean("NotificationSound", true)
        notificationType = sharedPreferences.getInt("NotificationType", 1)
        timeNotificationChannels = sharedPreferences.getInt("TimeNotificationChannels", 0)

        temperatureNode.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                temperature = snapshot.getValue(Int::class.java)!!
                if (notificationType == 1) {
                    if (isTemperatureShowed && !isHumidityShowed) {
                        notificationText = "Температура: $temperature°C"
                    } else if (!isTemperatureShowed && isHumidityShowed) {
                        notificationText = "Влажность: $humidity%"
                    } else if (isTemperatureShowed && isHumidityShowed) {
                        notificationText = "Температура: $temperature°C\nВлажность: $humidity%"
                    }
                    startForeground(1, NotificationCompat.Builder(baseContext, "PermanentNotification")
                            .setOngoing(true)
                            .setSmallIcon(R.drawable.notification_icon)
                            .setContentTitle("WiFi Термометр")
                            .setContentText(notificationText)
                            .setCategory(NotificationCompat.CATEGORY_SERVICE)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(contentIntent)
                            .build())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(baseContext, "Не удалось получить температуру!", Toast.LENGTH_LONG).show()
            }
        })

        humidityNode.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                humidity = snapshot.getValue(Int::class.java)!!
                if (notificationType == 1) {
                    if (isTemperatureShowed && !isHumidityShowed) {
                        notificationText = "Температура: $temperature°C"
                    } else if (!isTemperatureShowed && isHumidityShowed) {
                        notificationText = "Влажность: $humidity%"
                    } else if (isTemperatureShowed && isHumidityShowed) {
                        notificationText = "Температура: $temperature°C\nВлажность: $humidity%"
                    }
                    startForeground(1, NotificationCompat.Builder(baseContext, "PermanentNotification")
                            .setOngoing(true)
                            .setSmallIcon(R.drawable.notification_icon)
                            .setContentTitle("WiFi Термометр")
                            .setContentText(notificationText)
                            .setCategory(NotificationCompat.CATEGORY_SERVICE)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(contentIntent)
                            .build())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(baseContext, "Не удалось получить влажность!", Toast.LENGTH_LONG).show()
            }
        })

        if (notificationType == 2) {
            notificationTimestampsArrayJson = sharedPreferences.getString("NotificationTimestampsArray", "").toString()
            notificationTimestampsArray = gson.fromJson(notificationTimestampsArrayJson, object : TypeToken<ArrayList<String?>?>() {}.type)
            startForeground(3, NotificationCompat.Builder(baseContext, "AuxiliaryNotification")
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentTitle("WiFi Термометр")
                    .setContentText("Появление уведомлений по времени запущено!")
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(contentIntent)
                    .build())
            checkTimeHandler = Handler()
            checkTimeHandler.post(checkTime)
            checkTime.run()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private val checkTime : Runnable = object : Runnable {
        override fun run() {
            hours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY).toString()
            minutes = Calendar.getInstance().get(Calendar.MINUTE).toString()
            if (hours.toInt() < 10) hours = "0$hours"
            if (minutes.toInt() < 10) minutes = "0$minutes"

            if (notificationTimestampsArray.contains("$hours:$minutes")) {
                if (!isNotificationShowed) {
                    if (isTemperatureShowed && !isHumidityShowed) {
                        notificationText = "Температура: $temperature°C"
                    } else if (!isTemperatureShowed && isHumidityShowed) {
                        notificationText = "Влажность: $humidity%"
                    } else if (isTemperatureShowed && isHumidityShowed) {
                        notificationText = "Температура: $temperature°C\nВлажность: $humidity%"
                    }

                    if (if (Build.VERSION.SDK_INT >= 20) !powerManager.isInteractive else !powerManager.isScreenOn) {
                        wakeLock.acquire(3000)
                        wakeLock.release()
                    }
                    if (isNotificationSound) {
                        startForeground(2, NotificationCompat.Builder(baseContext, "TimeNotification$timeNotificationChannels")
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.notification_icon)
                            .setContentTitle("WiFi Термометр")
                            .setContentText(notificationText)
                            .setCategory(NotificationCompat.CATEGORY_EVENT)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(contentIntent)
                            .build())
                    } else {
                        startForeground(2, NotificationCompat.Builder(baseContext, "TimeNotification$timeNotificationChannels")
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.notification_icon)
                            .setContentTitle("WiFi Термометр")
                            .setContentText(notificationText)
                            .setCategory(NotificationCompat.CATEGORY_EVENT)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(contentIntent)
                            .build())
                    }
                    isNotificationShowed = true
                }
            } else {
                isNotificationShowed = false
            }
            checkTimeHandler.postDelayed(this, 1000)
        }
    }

    override fun onDestroy() {}

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}