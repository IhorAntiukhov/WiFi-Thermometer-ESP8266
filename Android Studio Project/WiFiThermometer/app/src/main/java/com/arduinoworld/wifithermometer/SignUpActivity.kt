package com.arduinoworld.wifithermometer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


@Suppress("DEPRECATION")
class SignUpActivity : AppCompatActivity() {
    lateinit var vibrator : Vibrator
    lateinit var soundPlayer : MediaPlayer
    lateinit var firebaseAuth : FirebaseAuth
    lateinit var firebaseUser : FirebaseUser
    lateinit var sharedPreferences : SharedPreferences
    lateinit var editPreferences : SharedPreferences.Editor
    lateinit var inputUserEmail : EditText
    lateinit var inputUserPassword : EditText
    lateinit var inputConfirmPassword : EditText
    lateinit var buttonSignUp : Button
    lateinit var buttonSaveUpdates : Button
    lateinit var layoutUserCreation : LinearLayout
    lateinit var labelUserCreation : TextView

    private var isVibrateEnabled = true
    private var isSoundsEnabled = false
    private var isEmailUpdated = false
    private var isPasswordUpdated = false

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        soundPlayer = MediaPlayer.create(this, R.raw.click)
        firebaseAuth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        editPreferences = sharedPreferences.edit()
        inputUserEmail = findViewById(R.id.inputUserEmail)
        inputUserPassword = findViewById(R.id.inputUserPassword)
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword)
        buttonSignUp = findViewById(R.id.buttonSignUp)
        buttonSaveUpdates = findViewById(R.id.buttonSaveUpdates)
        layoutUserCreation = findViewById(R.id.layoutUserCreation)
        labelUserCreation = findViewById(R.id.labelUserCreation)
        readSharedPreferences()

        if (intent.getStringExtra("ReasonForLaunch").toString() == "Register") {
            supportActionBar!!.title = "Регистрация"
        } else if (intent.getStringExtra("ReasonForLaunch").toString() == "UpdateUser") {
            supportActionBar!!.title = "Обновить Пользователя"
            buttonSaveUpdates.visibility = View.VISIBLE
            buttonSignUp.visibility = View.GONE
            inputUserEmail.setText(sharedPreferences.getString("UserEmail", ""))
            inputUserPassword.setText(sharedPreferences.getString("UserPassword", ""))
            inputConfirmPassword.setText(sharedPreferences.getString("UserPassword", ""))
            firebaseUser = firebaseAuth.currentUser!!
        }

        buttonSignUp.setOnClickListener {
            vibrateOrSound()
            if (isNetworkConnected()) {
                if (inputUserEmail.text.isNotEmpty() && inputUserPassword.text.isNotEmpty() && inputConfirmPassword.text.isNotEmpty()) {
                    if (isValidEmail(inputUserEmail.text.toString())) {
                        if (inputUserPassword.text.length >= 6) {
                            if (inputUserPassword.text.toString() == inputConfirmPassword.text.toString()) {
                                layoutUserCreation.visibility = View.VISIBLE
                                buttonSignUp.visibility = View.GONE
                                firebaseAuth.createUserWithEmailAndPassword(inputUserEmail.text.toString(), inputUserPassword.text.toString())
                                    .addOnCompleteListener(this) { signUpTask ->
                                    if (signUpTask.isSuccessful) {
                                        hideKeyboard()
                                        Toast.makeText(baseContext, "Пользователь успешно зарегистрирован!", Toast.LENGTH_LONG).show()
                                        val activity = Intent(this, UserProfile::class.java)
                                        activity.putExtra("UserEmail", inputUserEmail.text.toString())
                                        activity.putExtra("UserPassword", inputUserPassword.text.toString())
                                        startActivity(activity)
                                        finish()
                                    } else {
                                        Toast.makeText(baseContext, "Эта почта уже существует!", Toast.LENGTH_LONG).show()
                                        buttonSignUp.visibility = View.VISIBLE
                                        layoutUserCreation.visibility = View.GONE
                                    }
                                }
                            } else {
                                Toast.makeText(baseContext, "Подтверждённый пароль не совпадает!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(baseContext, "Пароль должен состоять не \n менее чем из 6 символов!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(baseContext, "Введите корректный адрес \n электронной почты!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    if (inputUserEmail.text.isEmpty() && inputUserPassword.text.isEmpty() && inputConfirmPassword.text.isEmpty()) {
                        Toast.makeText(baseContext, "Введите почту, пароль \n пользователя и подтвердите его!", Toast.LENGTH_LONG).show()
                    } else {
                        if (inputUserEmail.text.isEmpty()) { Toast.makeText(baseContext, "Введите почту пользователя!", Toast.LENGTH_LONG).show()
                        }
                        if (inputUserPassword.text.isEmpty()) {
                            Toast.makeText(baseContext, "Введите пароль пользователя!", Toast.LENGTH_LONG).show()
                        }
                        if (inputConfirmPassword.text.isEmpty()) {
                            Toast.makeText(baseContext, "Подтвердите пароль пользователя!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Toast.makeText(baseContext, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonSaveUpdates.setOnClickListener {
            vibrateOrSound()
            if (isNetworkConnected()) {
                if (inputUserEmail.text.isNotEmpty() && inputUserPassword.text.isNotEmpty() && inputConfirmPassword.text.isNotEmpty()) {
                    if (isValidEmail(inputUserEmail.text.toString())) {
                        if (inputUserPassword.text.length >= 6) {
                            if (inputUserPassword.text.toString() == inputConfirmPassword.text.toString()) {
                                if ((inputUserEmail.text.toString() != sharedPreferences.getString("UserEmail", "").toString()) ||
                                   (inputUserPassword.text.toString() != sharedPreferences.getString("UserPassword", "").toString())) {
                                    isEmailUpdated = if (inputUserEmail.text.toString() != sharedPreferences.getString("UserEmail", "").toString()) {
                                        firebaseUser.updateEmail(inputUserEmail.text.toString())
                                        true
                                    } else {
                                        false
                                    }
                                    isPasswordUpdated = if (inputUserPassword.text.toString() != sharedPreferences.getString("UserPassword", "").toString()) {
                                        firebaseUser.updatePassword(inputUserPassword.text.toString())
                                        true
                                    } else {
                                        false
                                    }
                                } else {
                                    Toast.makeText(baseContext, "Введите новую почту, или пароль пользователя!", Toast.LENGTH_LONG).show()
                                }
                                if (isEmailUpdated && isPasswordUpdated) {
                                    Toast.makeText(baseContext, "Почта и пароль \n пользователя обновлены!", Toast.LENGTH_LONG).show()
                                    editPreferences.putString("UserEmail", inputUserEmail.text.toString())
                                    editPreferences.putString("UserPassword", inputUserPassword.text.toString())
                                    editPreferences.apply()
                                }
                                if (isEmailUpdated && !isPasswordUpdated) {
                                    Toast.makeText(baseContext, "Почта пользователя обновлена!", Toast.LENGTH_LONG).show()
                                    editPreferences.putString("UserEmail", inputUserEmail.text.toString())
                                    editPreferences.apply()
                                }
                                if (!isEmailUpdated && isPasswordUpdated) {
                                    Toast.makeText(baseContext, "Пароль пользователя обновлен!", Toast.LENGTH_LONG).show()
                                    editPreferences.putString("UserPassword", inputUserPassword.text.toString())
                                    editPreferences.apply()
                                }
                            } else {
                                Toast.makeText(baseContext, "Подтверждённый пароль не совпадает!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(baseContext, "Пароль должен состоять не \n менее чем из 6 символов!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(baseContext, "Введите корректный адрес \n электронной почты!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    if (inputUserEmail.text.isEmpty() && inputUserPassword.text.isEmpty() && inputConfirmPassword.text.isEmpty()) {
                        Toast.makeText(baseContext, "Введите почту, пароль \n пользователя и подтвердите его!", Toast.LENGTH_LONG).show()
                    } else {
                        if (inputUserEmail.text.isEmpty()) {
                            Toast.makeText(baseContext, "Введите почту пользователя!", Toast.LENGTH_LONG).show()
                        }
                        if (inputUserPassword.text.isEmpty()) {
                            Toast.makeText(baseContext, "Введите пароль пользователя!", Toast.LENGTH_LONG).show()
                        }
                        if (inputConfirmPassword.text.isEmpty()) {
                            Toast.makeText(baseContext, "Подтвердите пароль пользователя!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Toast.makeText(baseContext, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        readSharedPreferences()
    }

    private fun readSharedPreferences() {
        isVibrateEnabled = sharedPreferences.getBoolean("VibrateEnabled", true)
        isSoundsEnabled = sharedPreferences.getBoolean("SoundsEnabled", false)
    }

    private fun isValidEmail(inputText: CharSequence) : Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(inputText).matches()
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
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