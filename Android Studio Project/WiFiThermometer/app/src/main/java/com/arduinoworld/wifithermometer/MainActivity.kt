package com.arduinoworld.wifithermometer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.*
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import kotlin.collections.ArrayList


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    lateinit var gson : Gson
    lateinit var vibrator : Vibrator
    lateinit var soundPlayer : MediaPlayer
    lateinit var firebaseAuth : FirebaseAuth
    lateinit var lineGraphUpdateHandler : Handler
    lateinit var humidityNode : DatabaseReference
    lateinit var temperatureNode : DatabaseReference
    lateinit var sharedPreferences : SharedPreferences
    lateinit var editPreferences : SharedPreferences.Editor
    lateinit var progressBarTemperature : ProgressBar
    lateinit var progressBarHumidity : ProgressBar
    lateinit var labelTemperature : TextView
    lateinit var labelHumidity : TextView
    lateinit var lineGraph : LineChart
    lateinit var lineGraphXAxis : XAxis
    lateinit var lineGraphYAxis : YAxis
    lateinit var lineGraphLegend : Legend

    private var temperatureEntryArray = ArrayList<Entry>()
    private var humidityEntryArray = ArrayList<Entry>()
    private var temperatureEntryArrayJson = ""
    private var humidityEntryArrayJson = ""
    private var timestampsArrayJson = ""
    private var timestampsArray = ArrayList<String>()
    private var lineGraphDataSets = ArrayList<LineDataSet>()

    private var isDHTSensorValuesReceived = false
    private var isFirstTimestampAdded = false
    private var isTemperatureEnabled = true
    private var isHumidityEnabled = true
    private var isVibrateEnabled = true
    private var isSoundsEnabled = false
    private var isGraphEnabled = true
    private var isUserLogged = false
    private var temperature = 0
    private var humidity = 0
    private var dataSetStamps = 1
    private var graphUpdateInterval = 0

    @SuppressLint("SetTextI18n", "CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar!!.title = "Главная"

        gson = Gson()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        soundPlayer = MediaPlayer.create(this, R.raw.click)
        firebaseAuth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        editPreferences = sharedPreferences.edit()
        progressBarTemperature = findViewById(R.id.progressBarTemperature)
        progressBarHumidity = findViewById(R.id.progressBarHumidity)
        labelTemperature = findViewById(R.id.labelTemperature)
        labelHumidity = findViewById(R.id.labelHumidity)
        lineGraph = findViewById(R.id.lineGraph)
        lineGraphXAxis = lineGraph.xAxis
        lineGraphYAxis = lineGraph.axisLeft
        lineGraphLegend = lineGraph.legend
        readSharedPreferences()

        if (isGraphEnabled) {
            lineGraph.setBackgroundColor(Color.parseColor("#00FFFFFF"))
            lineGraph.setNoDataText("Температура и влажность не получены!")
            lineGraph.setNoDataTextColor(Color.parseColor("#0388FC"))
            lineGraph.extraTopOffset = 10F
            lineGraph.setTouchEnabled(true)
            lineGraph.isDragEnabled = true
            lineGraph.isScaleXEnabled = true
            lineGraph.isScaleYEnabled = false
            lineGraph.axisRight.isEnabled = false
            lineGraph.description.isEnabled = false

            lineGraphXAxis.isEnabled = true
            lineGraphXAxis.setDrawAxisLine(true)
            lineGraphXAxis.setDrawGridLines(true)
            lineGraphXAxis.isGranularityEnabled = true
            lineGraphXAxis.position = XAxis.XAxisPosition.TOP
            lineGraphXAxis.gridColor = Color.parseColor("#0388FC")
            lineGraphXAxis.textColor = Color.parseColor("#0388FC")
            lineGraphXAxis.gridLineWidth = 1.5F
            lineGraphXAxis.granularity = 1f
            lineGraphXAxis.textSize = 12F

            lineGraphYAxis.isEnabled = true
            lineGraphYAxis.setDrawAxisLine(false)
            lineGraphYAxis.setDrawGridLines(true)
            lineGraphYAxis.gridColor = Color.parseColor("#0388FC")
            lineGraphYAxis.textColor = Color.parseColor("#0388FC")
            lineGraphYAxis.axisMinimum = -20F
            lineGraphYAxis.axisMaximum = 80F
            lineGraphYAxis.gridLineWidth = 1.5F
            lineGraphYAxis.textSize = 12F

            if (dataSetStamps > 1) {
                isFirstTimestampAdded = true

                drawGraph()

                lineGraphXAxis.valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        return timestampsArray.getOrNull(value.toInt()) ?: value.toString()
                    }
                }
            }
            lineGraph.invalidate()
        } else {
            lineGraph.visibility = View.GONE
        }

        isUserLogged = sharedPreferences.getBoolean("UserLogged", false)
        if (isNetworkConnected()) {
            if (isUserLogged) {
                temperatureNode = FirebaseDatabase.getInstance().reference.child("users").child(firebaseAuth.currentUser!!.uid).child("temperature")
                temperatureNode.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        temperature = snapshot.getValue(Int::class.java)!!
                        labelTemperature.text = "$temperature°C"
                        if (temperature == 0 && humidity == 0) {
                            progressBarTemperature.progress = 0
                        } else {
                            progressBarTemperature.progress = temperature + 20
                        }
                        if (isGraphEnabled) {
                            if (temperature != 0 && humidity != 0 && !isDHTSensorValuesReceived) {
                                lineGraphUpdateHandler = Handler()
                                runOnUiThread(lineGraphUpdate)
                                isDHTSensorValuesReceived = true
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            if (temperature <= -10) {
                                progressBarTemperature.progressTintList = ColorStateList.valueOf(Color.parseColor("#034EFC"))
                                labelTemperature.setTextColor(Color.parseColor("#034EFC"))
                            } else if (temperature >= -10 && temperature <= 0) {
                                progressBarTemperature.progressTintList = ColorStateList.valueOf(Color.parseColor("#0388FC"))
                                labelTemperature.setTextColor(Color.parseColor("#0388FC"))
                            } else if (temperature in 1..10) {
                                progressBarTemperature.progressTintList = ColorStateList.valueOf(Color.parseColor("#00D679"))
                                labelTemperature.setTextColor(Color.parseColor("#00D679"))
                            } else if (temperature in 11..17) {
                                progressBarTemperature.progressTintList = ColorStateList.valueOf(Color.parseColor("#00FF7B"))
                                labelTemperature.setTextColor(Color.parseColor("#00FF7B"))
                            } else if (temperature in 18..28) {
                                progressBarTemperature.progressTintList = ColorStateList.valueOf(Color.parseColor("#00E000"))
                                labelTemperature.setTextColor(Color.parseColor("#00E000"))
                            } else if (temperature in 29..35) {
                                progressBarTemperature.progressTintList = ColorStateList.valueOf(Color.parseColor("#FF9900"))
                                labelTemperature.setTextColor(Color.parseColor("#FF9900"))
                            } else if (temperature >= 36) {
                                progressBarTemperature.progressTintList = ColorStateList.valueOf(Color.parseColor("#FF4D00"))
                                labelTemperature.setTextColor(Color.parseColor("#FF4D00"))
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(baseContext, "Не удалось получить температуру!", Toast.LENGTH_LONG).show()
                    }

                })
                humidityNode = FirebaseDatabase.getInstance().reference.child("users").child(firebaseAuth.currentUser!!.uid).child("humidity")
                humidityNode.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        humidity = snapshot.getValue(Int::class.java)!!
                        progressBarHumidity.progress = humidity
                        labelHumidity.text = "$humidity%"
                        if (temperature == 0 && humidity == 0) {
                            progressBarTemperature.progress = 0
                        } else {
                            progressBarTemperature.progress = temperature + 20
                        }
                        if (isGraphEnabled) {
                            if (temperature != 0 && humidity != 0 && !isDHTSensorValuesReceived) {
                                lineGraphUpdateHandler = Handler()
                                lineGraphUpdate.run()
                                isDHTSensorValuesReceived = true
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            when {
                                humidity <= 35 -> {
                                    progressBarHumidity.progressTintList = ColorStateList.valueOf(Color.parseColor("#FF0000"))
                                    labelHumidity.setTextColor(Color.parseColor("#FF0000"))
                                }
                                humidity in 36..45 -> {
                                    progressBarHumidity.progressTintList = ColorStateList.valueOf(Color.parseColor("#FF7B00"))
                                    labelHumidity.setTextColor(Color.parseColor("#FF7B00"))
                                }
                                humidity in 46..65 -> {
                                    progressBarHumidity.progressTintList = ColorStateList.valueOf(Color.parseColor("#00E000"))
                                    labelHumidity.setTextColor(Color.parseColor("#00E000"))
                                }
                                humidity in 66..80 -> {
                                    progressBarHumidity.progressTintList = ColorStateList.valueOf(Color.parseColor("#0388FC"))
                                    labelHumidity.setTextColor(Color.parseColor("#0388FC"))
                                }
                                humidity > 80 -> {
                                    progressBarHumidity.progressTintList = ColorStateList.valueOf(Color.parseColor("#034EFC"))
                                    labelHumidity.setTextColor(Color.parseColor("#034EFC"))
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(baseContext, "Не удалось получить влажность!", Toast.LENGTH_LONG).show()
                    }
                })
            }
        } else {
            Toast.makeText(baseContext, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
        }
    }

    private val lineGraphUpdate : Runnable = object : Runnable {
        override fun run() {
            val calendarTime = Calendar.getInstance()
            val currentTime = String.format("%02d:%02d", calendarTime.get(Calendar.HOUR_OF_DAY), calendarTime.get(Calendar.MINUTE))
            dataSetStamps = dataSetStamps + 1
            if (isTemperatureEnabled) {
                temperatureEntryArray.add(Entry(dataSetStamps.toFloat(), temperature.toFloat()))
            }
            if (isHumidityEnabled) {
                humidityEntryArray.add(Entry(dataSetStamps.toFloat(), humidity.toFloat()))
            }
            lineGraphDataSets.clear()

            drawGraph()

            if (!isFirstTimestampAdded) {
                timestampsArray.add(currentTime)
                isFirstTimestampAdded = true
            }
            timestampsArray.add(currentTime)
            lineGraphXAxis.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return timestampsArray.getOrNull(value.toInt()) ?: value.toString()
                }
            }
            lineGraph.invalidate()
            if (dataSetStamps > 1) {
                if (isTemperatureEnabled) {
                    temperatureEntryArrayJson = gson.toJson(temperatureEntryArray)
                    editPreferences.putString("TemperatureArray", temperatureEntryArrayJson)
                }
                if (isHumidityEnabled) {
                    humidityEntryArrayJson = gson.toJson(humidityEntryArray)
                    editPreferences.putString("HumidityArray", humidityEntryArrayJson)
                }
                timestampsArrayJson = gson.toJson(timestampsArray)
                editPreferences.putString("TimestampsArray", timestampsArrayJson)
                editPreferences.putInt("DataSetStamps", dataSetStamps)
                editPreferences.apply()
            }
            lineGraphUpdateHandler.postDelayed(this, graphUpdateInterval.toLong() * 1000 * 60)
        }
    }

    private fun drawGraph() {
        lineGraphLegend = lineGraph.legend
        lineGraphLegend.isEnabled = true
        lineGraphLegend.textSize = 14F
        lineGraphLegend.xEntrySpace = 5F
        lineGraphLegend.yEntrySpace = 5F
        lineGraphLegend.form = Legend.LegendForm.CIRCLE
        lineGraphLegend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        if (isTemperatureEnabled) {
            val temperatureLegend = LegendEntry("Температура", Legend.LegendForm.CIRCLE, 10f, 2f, null, Color.RED)
            lineGraphLegend.setCustom(arrayOf(temperatureLegend))
        }
        if (isHumidityEnabled) {
            val humidityLegend = LegendEntry("Влажность", Legend.LegendForm.CIRCLE, 10f, 2f, null, Color.BLUE)
            lineGraphLegend.setCustom(arrayOf(humidityLegend))
        }
        if (isTemperatureEnabled && isHumidityEnabled) {
            val temperatureLegend = LegendEntry("Температура", Legend.LegendForm.CIRCLE, 10f, 2f, null, Color.RED)
            val humidityLegend = LegendEntry("Влажность", Legend.LegendForm.CIRCLE, 10f, 2f, null, Color.BLUE)
            lineGraphLegend.setCustom(arrayOf(temperatureLegend, humidityLegend))
        }

        if (isTemperatureEnabled) {
            val temperatureDataSet = LineDataSet(temperatureEntryArray, "Температура")
            temperatureDataSet.lineWidth = 1.5F
            temperatureDataSet.circleRadius = 3F
            temperatureDataSet.valueTextSize = 12F
            temperatureDataSet.cubicIntensity = 1F
            temperatureDataSet.color = Color.RED
            temperatureDataSet.setCircleColor(Color.RED)
            temperatureDataSet.valueTextColor = Color.RED
            temperatureDataSet.setDrawCircleHole(false)
            temperatureDataSet.enableDashedLine(10f, 5f, 0f)
            lineGraphDataSets.add(temperatureDataSet)
        }

        if (isHumidityEnabled) {
            val humidityDataSet = LineDataSet(humidityEntryArray, "Влажность")
            humidityDataSet.lineWidth = 1.5F
            humidityDataSet.circleRadius = 3F
            humidityDataSet.valueTextSize = 12F
            humidityDataSet.cubicIntensity = 1F
            humidityDataSet.color = Color.BLUE
            humidityDataSet.setCircleColor(Color.BLUE)
            humidityDataSet.valueTextColor = Color.BLUE
            humidityDataSet.setDrawCircleHole(false)
            humidityDataSet.enableDashedLine(10f, 5f, 0f)
            lineGraphDataSets.add(humidityDataSet)
        }

        val allDataSets = LineData(lineGraphDataSets as List<ILineDataSet>?)
        lineGraph.data = allDataSets
    }

    override fun onRestart() {
        super.onRestart()
        readSharedPreferences()
    }

    private fun readSharedPreferences() {
        isUserLogged = sharedPreferences.getBoolean("UserLogged", false)
        isVibrateEnabled = sharedPreferences.getBoolean("VibrateEnabled", true)
        isSoundsEnabled = sharedPreferences.getBoolean("SoundsEnabled", false)
        isTemperatureEnabled = sharedPreferences.getBoolean("TemperatureOnGraph", true)
        isHumidityEnabled = sharedPreferences.getBoolean("HumidityOnGraph", true)
        isGraphEnabled = sharedPreferences.getBoolean("GraphEnabled", true)
        graphUpdateInterval = sharedPreferences.getInt("GraphInterval", 20)

        dataSetStamps = sharedPreferences.getInt("DataSetStamps", 0)
        if (dataSetStamps > 1) {
            timestampsArrayJson = sharedPreferences.getString("TimestampsArray", "")!!
            temperatureEntryArrayJson = sharedPreferences.getString("TemperatureArray", "")!!
            humidityEntryArrayJson = sharedPreferences.getString("HumidityArray", "")!!

            timestampsArray = gson.fromJson(timestampsArrayJson, object : TypeToken<ArrayList<String?>?>() {}.type)
            if (isTemperatureEnabled) {
                temperatureEntryArray = gson.fromJson(temperatureEntryArrayJson, object : TypeToken<ArrayList<Entry?>?>() {}.type)
            }
            if (isHumidityEnabled) {
                humidityEntryArray = gson.fromJson(humidityEntryArrayJson, object : TypeToken<ArrayList<Entry?>?>() {}.type)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val actionButtonsInflater = menuInflater
        actionButtonsInflater.inflate(R.menu.main_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.buttonUserProfile -> {
                vibrateOrSound()
                val activity = Intent(this, UserProfile::class.java)
                startActivity(activity)
                true
            }
            R.id.buttonNotifications -> {
                vibrateOrSound()
                val activity = Intent(this, NotificationsActivity::class.java)
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
        val connectivityManager = (getSystemService(Context.CONNECTIVITY_SERVICE)) as ConnectivityManager
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