package com.arduinoworld.wifithermometer

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.*
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

@Suppress("DEPRECATION")
class SettingsActivity : AppCompatActivity() {
    lateinit var vibrator : Vibrator
    lateinit var soundPlayer : MediaPlayer
    lateinit var sharedPreferences : SharedPreferences
    lateinit var editPreferences : SharedPreferences.Editor
    lateinit var alertDialogClearGraph : AlertDialog
    lateinit var alertDialogClearGraphBuilder : AlertDialog.Builder
    lateinit var notificationManager : NotificationManager
    lateinit var notificationChannel : NotificationChannel
    lateinit var checkBoxSounds : CheckBox
    lateinit var checkBoxVibrate : CheckBox
    lateinit var checkBoxNotificationSound : CheckBox
    lateinit var checkBoxShowGraph : CheckBox
    lateinit var checkBoxTemperatureOnGraph : CheckBox
    lateinit var checkBoxHumidityOnGraph : CheckBox
    lateinit var inputGraphInterval : EditText
    lateinit var buttonDefaultSettings : Button
    lateinit var buttonClearGraph : Button

    private var isGraphCleared = false
    private var isNotificationGroupCreated = false
    private var isTimeNotificationChannelCreated = false
    private var timeNotificationChannels = 0

    @SuppressLint("CommitPrefEdits", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar!!.title = "Настройки"

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        soundPlayer = MediaPlayer.create(this, R.raw.click)
        sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        editPreferences = sharedPreferences.edit()
        checkBoxSounds = findViewById(R.id.checkBoxSounds)
        checkBoxVibrate = findViewById(R.id.checkBoxVibrate)
        checkBoxNotificationSound = findViewById(R.id.checkBoxNotificationSound)
        checkBoxShowGraph = findViewById(R.id.checkBoxShowGraph)
        checkBoxTemperatureOnGraph = findViewById(R.id.checkBoxTemperatureOnGraph)
        checkBoxHumidityOnGraph = findViewById(R.id.checkBoxHumidityOnGraph)
        inputGraphInterval = findViewById(R.id.inputGraphInterval)
        buttonDefaultSettings = findViewById(R.id.buttonDefaultSettings)
        buttonClearGraph = findViewById(R.id.buttonClearGraph)
        readSharedPreferences()

        checkBoxSounds.setOnCheckedChangeListener { _, isChecked ->
            vibrateOrSound()
            editPreferences.putBoolean("SoundsEnabled", isChecked)
            editPreferences.apply()
        }
        checkBoxVibrate.setOnCheckedChangeListener { _, isChecked ->
            vibrateOrSound()
            editPreferences.putBoolean("VibrateEnabled", isChecked)
            editPreferences.apply()
        }
        checkBoxNotificationSound.setOnCheckedChangeListener { _, isChecked ->
            vibrateOrSound()
            editPreferences.putBoolean("NotificationSound", isChecked)
            editPreferences.apply()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!isNotificationGroupCreated) {
                    notificationManager.createNotificationChannelGroup(NotificationChannelGroup("WiFiThermometer", "WiFi Термометер"))
                    editPreferences.putBoolean("NotificationGroupCreated", true)
                    editPreferences.apply()
                }
                if (isTimeNotificationChannelCreated) {
                    notificationManager.deleteNotificationChannel("TimeNotification$timeNotificationChannels")
                } else {
                    notificationChannel = NotificationChannel("AuxiliaryNotification", "Вспомогательное уведомление", NotificationManager.IMPORTANCE_HIGH)
                    notificationChannel.description = "Уведомление, которое отображает то, что показывание уведомлений по времени включено"
                    notificationChannel.enableLights(false)
                    notificationChannel.enableVibration(false)
                    notificationChannel.setSound(null, null)
                    notificationChannel.group = "WiFiThermometer"
                    notificationChannel.lockscreenVisibility = View.GONE
                    notificationManager.createNotificationChannel(notificationChannel)
                }
                timeNotificationChannels += 1
                editPreferences.putInt("TimeNotificationChannels", timeNotificationChannels)
                editPreferences.putBoolean("TimeNotificationChannelCreated", true)
                editPreferences.apply()
                notificationChannel = NotificationChannel("TimeNotification$timeNotificationChannels", "Уведомление по времени", NotificationManager.IMPORTANCE_DEFAULT)
                notificationChannel.description = "Уведомление с температурой и влажностью, которое появляется по времени"
                notificationChannel.lightColor = Color.parseColor("#0388FC")
                notificationChannel.enableLights(true)
                notificationChannel.enableVibration(false)
                if (isChecked) {
                    val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
                    notificationChannel.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/" + R.raw.notification), audioAttributes)
                } else {
                    notificationChannel.setSound(null, null)
                }
                notificationChannel.group = "WiFiThermometer"
                notificationChannel.lockscreenVisibility = View.VISIBLE
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }
        checkBoxShowGraph.setOnCheckedChangeListener { _, isChecked ->
            vibrateOrSound()
            editPreferences.putBoolean("GraphEnabled", isChecked)
            editPreferences.apply()
        }
        checkBoxTemperatureOnGraph.setOnCheckedChangeListener { _, isChecked ->
            vibrateOrSound()
            if (!isChecked && !checkBoxHumidityOnGraph.isChecked) {
                checkBoxTemperatureOnGraph.isChecked = true
                Toast.makeText(baseContext, "Вы не можете не показывать и \n температуру и влажность на графике!", Toast.LENGTH_LONG).show()
            } else {
                editPreferences.putBoolean("TemperatureOnGraph", isChecked)
                editPreferences.apply()
            }
        }
        checkBoxHumidityOnGraph.setOnCheckedChangeListener { _, isChecked ->
            vibrateOrSound()
            if (!isChecked && !checkBoxTemperatureOnGraph.isChecked) {
                checkBoxHumidityOnGraph.isChecked = true
                Toast.makeText(baseContext, "Вы не можете не показывать и \n температуру и влажность на графике!", Toast.LENGTH_LONG).show()
            } else {
                editPreferences.putBoolean("HumidityOnGraph", isChecked)
                editPreferences.apply()
            }
        }
        buttonDefaultSettings.setOnClickListener {
            vibrateOrSound()
            if (!checkBoxNotificationSound.isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
                    notificationManager.deleteNotificationChannel("TimeNotification$timeNotificationChannels")
                    timeNotificationChannels = timeNotificationChannels + 1
                    editPreferences.putInt("TimeNotificationChannels", timeNotificationChannels)
                    notificationChannel = NotificationChannel("TimeNotification$timeNotificationChannels", "Уведомление по времени", NotificationManager.IMPORTANCE_DEFAULT)
                    notificationChannel.description = "Уведомление с температурой и влажностью, которое появляется по времени"
                    notificationChannel.lightColor = Color.parseColor("#0388FC")
                    notificationChannel.enableLights(true)
                    notificationChannel.enableVibration(false)
                    notificationChannel.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/" + R.raw.notification), audioAttributes)
                    notificationChannel.group = "WiFiThermometer"
                    notificationChannel.lockscreenVisibility = View.VISIBLE
                    notificationManager.createNotificationChannel(notificationChannel)
                }
            }
            checkBoxSounds.isChecked = false
            checkBoxVibrate.isChecked = true
            checkBoxNotificationSound.isChecked = true
            checkBoxShowGraph.isChecked = true
            checkBoxTemperatureOnGraph.isChecked = true
            checkBoxHumidityOnGraph.isChecked = true
            inputGraphInterval.setText("20")
            editPreferences.putInt("GraphInterval", 20)
            editPreferences.apply()
        }
        buttonClearGraph.setOnClickListener {
            vibrateOrSound()
            if (!isGraphCleared) {
                alertDialogClearGraphBuilder = AlertDialog.Builder(this)
                alertDialogClearGraphBuilder.setTitle("Очистка Данных Графика")
                alertDialogClearGraphBuilder.setMessage("Вы уверены, что хотите очистить данные графика?")
                alertDialogClearGraphBuilder.setPositiveButton("Подтвердить") { _, _ ->
                    vibrateOrSound()
                    Toast.makeText(baseContext, "Данные графика очищены!", Toast.LENGTH_LONG).show()
                    isGraphCleared = true
                    buttonClearGraph.setBackgroundDrawable(ContextCompat.getDrawable(baseContext, R.drawable.button_second_background_style))
                    editPreferences.putInt("DataSetStamps", 0)
                    editPreferences.putString("TimestampsArray", "")
                    editPreferences.putString("TemperatureArray", "")
                    editPreferences.putString("HumidityArray", "")
                    editPreferences.apply()
                }
                alertDialogClearGraphBuilder.setNegativeButton("Отмена") { _, _ ->
                    vibrateOrSound()
                }
                alertDialogClearGraph = alertDialogClearGraphBuilder.create()
                alertDialogClearGraph.show()
            } else {
                Toast.makeText(baseContext, "Данные графика уже очищены!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        readSharedPreferences()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (inputGraphInterval.text.isNotEmpty()) {
            if (inputGraphInterval.text.toString().toInt() >= 1) {
                editPreferences.putInt("GraphInterval", inputGraphInterval.text.toString().toInt())
                editPreferences.apply()
            } else {
                Toast.makeText(baseContext, "Интервал графика должен быть не меньше 1 минуты!", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(baseContext, "Введите интервал графика!", Toast.LENGTH_LONG).show()
        }
    }

    private fun readSharedPreferences() {
        checkBoxSounds.isChecked = sharedPreferences.getBoolean("SoundsEnabled", false)
        checkBoxVibrate.isChecked = sharedPreferences.getBoolean("VibrateEnabled", true)
        isNotificationGroupCreated = sharedPreferences.getBoolean("NotificationGroupCreated", false)
        isTimeNotificationChannelCreated = sharedPreferences.getBoolean("TimeNotificationChannelCreated", false)
        checkBoxNotificationSound.isChecked = sharedPreferences.getBoolean("NotificationSound", true)
        checkBoxShowGraph.isChecked = sharedPreferences.getBoolean("GraphEnabled", true)
        checkBoxTemperatureOnGraph.isChecked = sharedPreferences.getBoolean("TemperatureOnGraph", true)
        checkBoxHumidityOnGraph.isChecked = sharedPreferences.getBoolean("HumidityOnGraph", true)
        inputGraphInterval.setText(sharedPreferences.getInt("GraphInterval", 20).toString())
        timeNotificationChannels = sharedPreferences.getInt("TimeNotificationChannels", 0)

        if (sharedPreferences.getInt("DataSetStamps", 0) == 0) {
            isGraphCleared = true
            buttonClearGraph.setBackgroundDrawable(ContextCompat.getDrawable(baseContext, R.drawable.button_second_background_style))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val actionButtonsInflater = menuInflater
        actionButtonsInflater.inflate(R.menu.settings_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.buttonMainScreen -> {
                vibrateOrSound()
                if (inputGraphInterval.text.isNotEmpty()) {
                    if (inputGraphInterval.text.toString().toInt() >= 1) {
                        editPreferences.putInt("GraphInterval", inputGraphInterval.text.toString().toInt())
                        editPreferences.apply()
                    } else {
                        Toast.makeText(baseContext, "Интервал графика должен быть не меньше 1 минуты!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(baseContext, "Введите интервал графика!", Toast.LENGTH_LONG).show()
                }
                val activity = Intent(this, MainActivity::class.java)
                startActivity(activity)
                true
            }
            R.id.buttonUserProfile -> {
                vibrateOrSound()
                if (inputGraphInterval.text.isNotEmpty()) {
                    if (inputGraphInterval.text.toString().toInt() >= 1) {
                        editPreferences.putInt("GraphInterval", inputGraphInterval.text.toString().toInt())
                        editPreferences.apply()
                    } else {
                        Toast.makeText(baseContext, "Интервал графика должен быть не меньше 1 минуты!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(baseContext, "Введите интервал графика!", Toast.LENGTH_LONG).show()
                }
                val activity = Intent(this, UserProfile::class.java)
                startActivity(activity)
                true
            }
            R.id.buttonNotifications -> {
                vibrateOrSound()
                if (inputGraphInterval.text.isNotEmpty()) {
                    if (inputGraphInterval.text.toString().toInt() >= 1) {
                        editPreferences.putInt("GraphInterval", inputGraphInterval.text.toString().toInt())
                        editPreferences.apply()
                    } else {
                        Toast.makeText(baseContext, "Интервал графика должен быть не меньше 1 минуты!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(baseContext, "Введите интервал графика!", Toast.LENGTH_LONG).show()
                }
                val activity = Intent(this, NotificationsActivity::class.java)
                startActivity(activity)
                true
            }
            else->super.onOptionsItemSelected(item)
        }
    }

    private fun vibrateOrSound() {
        if (checkBoxVibrate.isChecked) {
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(20)
                }
            }
        }
        if (checkBoxSounds.isChecked) {
            soundPlayer.start()
        }
    }
}
