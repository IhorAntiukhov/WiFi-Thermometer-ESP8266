package com.arduinoworld.wifithermometer

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.*
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import kotlin.collections.ArrayList


@Suppress("NAME_SHADOWING", "DEPRECATION")
class NotificationsActivity : AppCompatActivity() {
    lateinit var gson : Gson
    lateinit var vibrator : Vibrator
    lateinit var soundPlayer : MediaPlayer
    lateinit var firebaseAuth : FirebaseAuth
    lateinit var humidityNode : DatabaseReference
    lateinit var temperatureNode : DatabaseReference
    lateinit var notificationManager : NotificationManager
    lateinit var notificationChannel : NotificationChannel
    lateinit var sharedPreferences : SharedPreferences
    lateinit var editPreferences : SharedPreferences.Editor
    lateinit var inputNotificationType : AutoCompleteTextView
    lateinit var checkBoxShowTemperature : CheckBox
    lateinit var checkBoxShowHumidity : CheckBox
    lateinit var buttonShowNotification : Button
    lateinit var buttonHideNotification : Button
    lateinit var buttonStopNotifications : Button
    lateinit var buttonStartNotifications : Button
    lateinit var buttonDeleteAllNotifications : Button
    lateinit var buttonAddNotificationTimestamp : Button
    lateinit var notificationTimestampsLayout : ScrollView
    lateinit var notificationTimePicker : TimePickerDialog
    lateinit var recyclerViewAdapter : RecyclerViewAdapter
    lateinit var recyclerView : RecyclerView

    private var notificationTypesArray = listOf("Постоянное уведомление", "Показывать по времени")
    private var notificationTimestampsArray = ArrayList<String>()
    private var notificationTimestampsArrayJson = ""
    private var notificationTimestampsCount = 0
    private var notificationType = 1
    private var isUserLogged = false
    private var isVibrateEnabled = true
    private var isSoundsEnabled = false
    private var isServiceRunning = false
    private var isNotificationGroupCreated = false
    private var isTimeNotificationChannelCreated = false
    private var isPermanentNotificationChannelCreated = false

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
        supportActionBar!!.title = "Уведомления"

        gson = Gson()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        soundPlayer = MediaPlayer.create(this, R.raw.click)
        sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        editPreferences = sharedPreferences.edit()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        firebaseAuth = FirebaseAuth.getInstance()
        inputNotificationType = findViewById(R.id.inputNotificationType)
        checkBoxShowTemperature = findViewById(R.id.checkBoxShowTemperature)
        checkBoxShowHumidity = findViewById(R.id.checkBoxShowHumidity)
        buttonShowNotification = findViewById(R.id.buttonShowNotification)
        buttonHideNotification = findViewById(R.id.buttonHideNotification)
        buttonStopNotifications = findViewById(R.id.buttonStopNotifications)
        buttonStartNotifications = findViewById(R.id.buttonStartNotifications)
        buttonDeleteAllNotifications = findViewById(R.id.buttonDeleteAllNotification)
        buttonAddNotificationTimestamp = findViewById(R.id.buttonAddNotificationTime)
        notificationTimestampsLayout = findViewById(R.id.notificationTimestampsLayout)
        recyclerView = findViewById(R.id.recyclerView)
        readSharedPreferences()

        if (isUserLogged) {
            temperatureNode = FirebaseDatabase.getInstance().reference.child("users").child(firebaseAuth.currentUser!!.uid).child("temperature")
            humidityNode = FirebaseDatabase.getInstance().reference.child("users").child(firebaseAuth.currentUser!!.uid).child("humidity")
        }

        if (notificationType == 1) {
            if (isServiceRunning) {
                buttonHideNotification.visibility = View.VISIBLE
                buttonShowNotification.visibility = View.GONE
            }
        } else if (notificationType == 2) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                inputNotificationType.setText("Показывать по времени", false)
            }
            notificationTimestampsLayout.visibility = View.VISIBLE
            if (!isServiceRunning) {
                buttonAddNotificationTimestamp.visibility = View.VISIBLE
            }
            buttonShowNotification.visibility = View.GONE
            if (notificationTimestampsArrayJson != "") {
                if (!isServiceRunning) {
                    buttonDeleteAllNotifications.visibility = View.VISIBLE
                }
                recyclerViewAdapter = RecyclerViewAdapter(notificationTimestampsArray)
                recyclerView.apply {
                    adapter = recyclerViewAdapter
                    layoutManager = LinearLayoutManager(this@NotificationsActivity, LinearLayoutManager.HORIZONTAL, false)
                }
            }
            if (isServiceRunning) {
                buttonStopNotifications.visibility = View.VISIBLE
            } else {
                if (notificationTimestampsArrayJson != "") {
                    buttonStartNotifications.visibility = View.VISIBLE
                }
            }
        }

        inputNotificationType.setOnItemClickListener { _, _, position, _ ->
            vibrateOrSound()
            if (position == 0) {
                if (isServiceRunning) {
                    buttonHideNotification.visibility = View.VISIBLE
                } else {
                    buttonShowNotification.visibility = View.VISIBLE
                }
                notificationTimestampsLayout.visibility = View.GONE
                buttonAddNotificationTimestamp.visibility = View.GONE
                buttonDeleteAllNotifications.visibility = View.GONE
                buttonStartNotifications.visibility = View.GONE
                buttonStopNotifications.visibility = View.GONE
                editPreferences.putInt("NotificationType", 1)
                editPreferences.apply()
            } else if (position == 1) {
                notificationTimestampsLayout.visibility = View.VISIBLE
                buttonAddNotificationTimestamp.visibility = View.VISIBLE
                buttonShowNotification.visibility = View.GONE
                buttonHideNotification.visibility = View.GONE
                if (notificationTimestampsArrayJson != "") {
                    buttonDeleteAllNotifications.visibility = View.VISIBLE
                }
                if (isServiceRunning) {
                    buttonStopNotifications.visibility = View.VISIBLE
                } else {
                    if (notificationTimestampsArrayJson != "") {
                        buttonStartNotifications.visibility = View.VISIBLE
                        buttonDeleteAllNotifications.visibility = View.VISIBLE
                    }
                }
                editPreferences.putInt("NotificationType", 2)
                editPreferences.apply()
            }
        }

        checkBoxShowTemperature.setOnCheckedChangeListener { _, isChecked ->
            vibrateOrSound()
            if (!isChecked && !checkBoxShowHumidity.isChecked) {
                checkBoxShowTemperature.isChecked = true
                Toast.makeText(baseContext, "Вы не можете не показывать \n и температуру и влажность!", Toast.LENGTH_LONG).show()
            } else {
                editPreferences.putBoolean("ShowTemperature", isChecked)
                editPreferences.apply()
                if (isServiceRunning) {
                    val service = Intent(this, NotificationService::class.java)
                    stopService(service)
                    startService(service)
                }
            }
        }
        checkBoxShowHumidity.setOnCheckedChangeListener { _, isChecked ->
            vibrateOrSound()
            if (!isChecked && !checkBoxShowTemperature.isChecked) {
                checkBoxShowHumidity.isChecked = true
                Toast.makeText(baseContext, "Вы не можете не показывать \n и температуру и влажность!", Toast.LENGTH_LONG).show()
            } else {
                editPreferences.putBoolean("ShowHumidity", isChecked)
                editPreferences.apply()
                if (isServiceRunning) {
                    val service = Intent(this, NotificationService::class.java)
                    stopService(service)
                    startService(service)
                }
            }
        }

        buttonShowNotification.setOnClickListener {
            vibrateOrSound()
            if (isNetworkConnected()) {
                if (isUserLogged) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (!isNotificationGroupCreated) {
                            notificationManager.createNotificationChannelGroup(NotificationChannelGroup("WiFiThermometer", "WiFi Термометер"))
                            editPreferences.putBoolean("NotificationGroupCreated", true)
                            editPreferences.apply()
                        }
                        if (!isPermanentNotificationChannelCreated) {
                            notificationChannel = NotificationChannel("PermanentNotification", "Постоянное уведомление", NotificationManager.IMPORTANCE_HIGH)
                            notificationChannel.description = "Уведомление с температурой и влажностью, которое отображается постоянно"
                            notificationChannel.enableLights(false)
                            notificationChannel.enableVibration(false)
                            notificationChannel.setSound(null, null)
                            notificationChannel.group = "WiFiThermometer"
                            notificationChannel.lockscreenVisibility = View.VISIBLE
                            notificationManager.createNotificationChannel(notificationChannel)
                            editPreferences.putBoolean("PermanentNotificationChannelCreated", true)
                            editPreferences.apply()
                        }
                        buttonHideNotification.visibility = View.VISIBLE
                        buttonShowNotification.visibility = View.GONE
                        editPreferences.putBoolean("ServiceRunning", true)
                        editPreferences.apply()
                        isServiceRunning = true
                        val service = Intent(this, NotificationService::class.java)
                        startService(service)
                    }
                } else {
                    Toast.makeText(baseContext, "Вы не вошли в пользователя!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(baseContext, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonHideNotification.setOnClickListener {
            vibrateOrSound()
            buttonShowNotification.visibility = View.VISIBLE
            buttonHideNotification.visibility = View.GONE
            editPreferences.putBoolean("ServiceRunning", false)
            editPreferences.apply()
            with(NotificationManagerCompat.from(baseContext)) {
                cancel(1)
            }
            isServiceRunning = false
            val service = Intent(this, NotificationService::class.java)
            stopService(service)
        }

        buttonAddNotificationTimestamp.setOnClickListener {
            vibrateOrSound()
            val calendarTime = Calendar.getInstance()
            val hours = calendarTime.get(Calendar.HOUR_OF_DAY)
            val minutes = calendarTime.get(Calendar.MINUTE)
            notificationTimePicker = TimePickerDialog(this, { _, hours, minutes ->
                vibrateOrSound()
                buttonDeleteAllNotifications.visibility = View.VISIBLE
                buttonStartNotifications.visibility = View.VISIBLE
                if (hours < 10 && minutes < 10) {
                    notificationTimestampsArray.add("0$hours:0$minutes")
                }
                if (hours < 10 && minutes >= 10) {
                    notificationTimestampsArray.add("0$hours:$minutes")
                }
                if (hours >= 10 && minutes < 10) {
                    notificationTimestampsArray.add("$hours:0$minutes")
                }
                if (hours >= 10 && minutes >= 10) {
                    notificationTimestampsArray.add("$hours:$minutes")
                }
                recyclerViewAdapter = RecyclerViewAdapter(notificationTimestampsArray)
                recyclerView.apply {
                    adapter = recyclerViewAdapter
                    layoutManager = LinearLayoutManager(this@NotificationsActivity, LinearLayoutManager.HORIZONTAL, false)
                }
                notificationTimestampsArrayJson = gson.toJson(notificationTimestampsArray)
                editPreferences.putString("NotificationTimestampsArray", notificationTimestampsArrayJson)
                editPreferences.apply()
            }, hours, minutes, DateFormat.is24HourFormat(this))
            notificationTimePicker.show()
        }

        buttonDeleteAllNotifications.setOnClickListener {
            vibrateOrSound()
            buttonDeleteAllNotifications.visibility = View.GONE
            buttonStartNotifications.visibility = View.GONE
            notificationTimestampsCount = notificationTimestampsArray.size
            notificationTimestampsArray.clear()
            recyclerViewAdapter.notifyItemRangeRemoved(0, notificationTimestampsCount)
            editPreferences.putString("NotificationTimestampsArray", "")
            editPreferences.apply()
        }

        buttonStartNotifications.setOnClickListener {
            vibrateOrSound()
            if (isNetworkConnected()) {
                if (isUserLogged) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (!isNotificationGroupCreated) {
                            notificationManager.createNotificationChannelGroup(NotificationChannelGroup("WiFiThermometer", "WiFi Термометер"))
                            editPreferences.putBoolean("NotificationGroupCreated", true)
                            editPreferences.apply()
                        }
                        if (!isTimeNotificationChannelCreated) {
                            val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
                            notificationChannel = NotificationChannel("TimeNotification0", "Уведомление по времени", NotificationManager.IMPORTANCE_DEFAULT)
                            notificationChannel.description = "Уведомление с температурой и влажностью, которое появляется по времени"
                            notificationChannel.lightColor = Color.parseColor("#0388FC")
                            notificationChannel.enableLights(true)
                            notificationChannel.enableVibration(false)
                            notificationChannel.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/" + R.raw.notification), audioAttributes)
                            notificationChannel.group = "WiFiThermometer"
                            notificationChannel.lockscreenVisibility = View.VISIBLE
                            notificationManager.createNotificationChannel(notificationChannel)

                            notificationChannel = NotificationChannel("AuxiliaryNotification", "Вспомогательное уведомление", NotificationManager.IMPORTANCE_HIGH)
                            notificationChannel.description = "Уведомление, которое отображает то, что показывание уведомлений по времени включено"
                            notificationChannel.enableLights(false)
                            notificationChannel.enableVibration(false)
                            notificationChannel.setSound(null, null)
                            notificationChannel.group = "WiFiThermometer"
                            notificationChannel.lockscreenVisibility = View.GONE
                            notificationManager.createNotificationChannel(notificationChannel)
                            editPreferences.putBoolean("TimeNotificationChannelCreated", true)
                            editPreferences.apply()
                        }
                    }
                    buttonStopNotifications.visibility = View.VISIBLE
                    buttonStartNotifications.visibility = View.GONE
                    buttonDeleteAllNotifications.visibility = View.GONE
                    buttonAddNotificationTimestamp.visibility = View.GONE
                    editPreferences.putBoolean("ServiceRunning", true)
                    editPreferences.apply()
                    isServiceRunning = true
                    val service = Intent(this, NotificationService::class.java)
                    startService(service)
                } else {
                    Toast.makeText(baseContext, "Вы не вошли в пользователя!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(baseContext, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonStopNotifications.setOnClickListener {
            vibrateOrSound()
            buttonStartNotifications.visibility = View.VISIBLE
            buttonDeleteAllNotifications.visibility = View.VISIBLE
            buttonAddNotificationTimestamp.visibility = View.VISIBLE
            buttonStopNotifications.visibility = View.GONE
            editPreferences.putBoolean("ServiceRunning", false)
            editPreferences.apply()
            with(NotificationManagerCompat.from(baseContext)) {
                cancel(2)
            }
            with(NotificationManagerCompat.from(baseContext)) {
                cancel(3)
            }
            isServiceRunning = false
            val service = Intent(this, NotificationService::class.java)
            stopService(service)
        }
    }

    override fun onRestart() {
        super.onRestart()
        readSharedPreferences()
    }

    private fun readSharedPreferences() {
        val notificationTypesArrayAdapter = ArrayAdapter(applicationContext, R.layout.dropdown_menu_item, notificationTypesArray)
        inputNotificationType.setAdapter(notificationTypesArrayAdapter)
        inputNotificationType.threshold = 1

        isUserLogged = sharedPreferences.getBoolean("UserLogged", false)
        isVibrateEnabled = sharedPreferences.getBoolean("VibrateEnabled", true)
        isSoundsEnabled = sharedPreferences.getBoolean("SoundsEnabled", false)
        isServiceRunning = sharedPreferences.getBoolean("ServiceRunning", false)
        notificationType = sharedPreferences.getInt("NotificationType", 1)
        isNotificationGroupCreated = sharedPreferences.getBoolean("NotificationGroupCreated", false)
        isTimeNotificationChannelCreated = sharedPreferences.getBoolean("TimeNotificationChannelCreated", false)
        isPermanentNotificationChannelCreated = sharedPreferences.getBoolean("PermanentNotificationChannelCreated", false)
        checkBoxShowTemperature.isChecked = sharedPreferences.getBoolean("ShowTemperature", true)
        checkBoxShowHumidity.isChecked = sharedPreferences.getBoolean("ShowHumidity", true)

        notificationTimestampsArrayJson = sharedPreferences.getString("NotificationTimestampsArray", "").toString()
        if (notificationTimestampsArrayJson != "") {
            notificationTimestampsArray = gson.fromJson(notificationTimestampsArrayJson, object : TypeToken<ArrayList<String?>?>() {}.type)
            recyclerViewAdapter = RecyclerViewAdapter(notificationTimestampsArray)
            recyclerView.apply {
                adapter = recyclerViewAdapter
                layoutManager = LinearLayoutManager(this@NotificationsActivity, LinearLayoutManager.HORIZONTAL, false)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val actionButtonsInflater = menuInflater
        actionButtonsInflater.inflate(R.menu.notifications_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.buttonMainScreen -> {
                vibrateOrSound()
                val activity = Intent(this, MainActivity::class.java)
                startActivity(activity)
                true
            }
            R.id.buttonUserProfile -> {
                vibrateOrSound()
                val activity = Intent(this, UserProfile::class.java)
                startActivity(activity)
                true
            }
            R.id.buttonSettings -> {
                vibrateOrSound()
                val activity = Intent(this, SettingsActivity::class.java)
                startActivity(activity)
                true
            }
            else->super.onOptionsItemSelected(item)
        }
    }

    private fun isNetworkConnected() : Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!.state == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state == NetworkInfo.State.CONNECTED
    }

    private fun vibrateOrSound() {
        if (isVibrateEnabled) {
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(20)
                }
            }
        }
        if (isSoundsEnabled) {
            soundPlayer.start()
        }
    }
}