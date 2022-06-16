package com.arduinoworld.wifithermometer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Patterns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.*
import com.google.firebase.database.*

@Suppress("DEPRECATION")
class UserProfile : AppCompatActivity() {
    lateinit var vibrator : Vibrator
    lateinit var soundPlayer : MediaPlayer
    lateinit var firebaseAuth : FirebaseAuth
    lateinit var firebaseUser : FirebaseUser
    lateinit var sharedPreferences : SharedPreferences
    lateinit var editPreferences : SharedPreferences.Editor
    lateinit var layoutUserCreation : LinearLayout
    lateinit var alertDialogDeleteUser : AlertDialog
    lateinit var alertDialogResetPassword : AlertDialog
    lateinit var alertDialogDeleteUserBuilder : AlertDialog.Builder
    lateinit var alertDialogResetPasswordBuilder : AlertDialog.Builder
    lateinit var userIsntLogged : TextView
    lateinit var inputUserEmail : EditText
    lateinit var inputUserPassword : EditText
    lateinit var labelUserCreation : TextView
    lateinit var buttonSignIn : Button
    lateinit var buttonResetPassword : Button
    lateinit var buttonRegister : Button
    lateinit var buttonLogout : Button
    lateinit var buttonUpdateUser : Button
    lateinit var buttonDeleteUser : Button

    private var isVibrateEnabled = true
    private var isSoundsEnabled = false
    private var isUserLogged = false

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        soundPlayer = MediaPlayer.create(this, R.raw.click)
        firebaseAuth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        editPreferences = sharedPreferences.edit()
        userIsntLogged = findViewById(R.id.labelUserLogged)
        inputUserEmail = findViewById(R.id.inputUserEmail)
        inputUserPassword = findViewById(R.id.inputUserPassword)
        buttonSignIn = findViewById(R.id.buttonSignIn)
        buttonResetPassword = findViewById(R.id.buttonResetPassword)
        buttonRegister = findViewById(R.id.buttonRegister)
        buttonLogout = findViewById(R.id.buttonLogout)
        buttonUpdateUser = findViewById(R.id.buttonUpdateUser)
        buttonDeleteUser = findViewById(R.id.buttonDeleteUser)
        layoutUserCreation = findViewById(R.id.layoutUserCreation)
        labelUserCreation = findViewById(R.id.labelUserCreation)
        readSharedPreferences()

        isUserLogged = sharedPreferences.getBoolean("UserLogged", false)
        if (isUserLogged) {
            supportActionBar!!.title = "Ваш Профиль"
            userIsntLogged.text = "Вы вошли в пользователя!"
            firebaseUser = firebaseAuth.currentUser!!
            buttonLogout.visibility = View.VISIBLE
            buttonUpdateUser.visibility = View.VISIBLE
            buttonDeleteUser.visibility = View.VISIBLE
            buttonSignIn.visibility = View.GONE
            buttonResetPassword.visibility = View.GONE
            buttonRegister.visibility = View.GONE
            inputUserEmail.setText(sharedPreferences.getString("UserEmail", "").toString())
            inputUserPassword.setText(sharedPreferences.getString("UserPassword", "").toString())
        } else {
            supportActionBar!!.title = "Вход"
        }

        editPreferences = sharedPreferences.edit()
        if (intent.hasExtra("UserEmail") && intent.hasExtra("UserPassword")) {
            layoutUserCreation.visibility = View.VISIBLE
            buttonSignIn.visibility = View.GONE
            buttonResetPassword.visibility = View.GONE
            labelUserCreation.text = "Входим в пользователя ..."
            firebaseAuth.signInWithEmailAndPassword(intent.getStringExtra("UserEmail").toString(), intent.getStringExtra("UserPassword").toString())
                    .addOnCompleteListener(this) { p0 ->
                        if (p0.isSuccessful) {
                            editPreferences.putBoolean("UserLogged", true)
                            editPreferences.putString("UserEmail", intent.getStringExtra("UserEmail").toString())
                            editPreferences.putString("UserPassword", intent.getStringExtra("UserPassword").toString())
                            editPreferences.apply()
                            firebaseUser = firebaseAuth.currentUser!!
                            supportActionBar!!.title = "Ваш Профиль"
                            userIsntLogged.text = "Вы вошли в пользователя!"
                            buttonLogout.visibility = View.VISIBLE
                            buttonUpdateUser.visibility = View.VISIBLE
                            buttonDeleteUser.visibility = View.VISIBLE
                            buttonSignIn.visibility = View.GONE
                            buttonResetPassword.visibility = View.GONE
                            buttonRegister.visibility = View.GONE
                            inputUserEmail.setText(intent.getStringExtra("UserEmail").toString())
                            inputUserPassword.setText(intent.getStringExtra("UserPassword").toString())
                            FirebaseDatabase.getInstance().reference.child("users").child(firebaseUser.uid).child("temperature").setValue(0)
                            FirebaseDatabase.getInstance().reference.child("users").child(firebaseUser.uid).child("humidity").setValue(0)
                        } else {
                            Toast.makeText(baseContext, "Не удалось войти в пользователя!", Toast.LENGTH_LONG).show()
                        }
                        layoutUserCreation.visibility = View.GONE
                    }
        }

        buttonSignIn.setOnClickListener {
            vibrateOrSound()
            if (isNetworkConnected()) {
                if (inputUserEmail.text.isNotEmpty() && inputUserPassword.text.isNotEmpty()) {
                    layoutUserCreation.visibility = View.VISIBLE
                    buttonSignIn.visibility = View.GONE
                    buttonResetPassword.visibility = View.GONE
                    labelUserCreation.text = "Входим в пользователя ..."
                    firebaseAuth.signInWithEmailAndPassword(inputUserEmail.text.toString(), inputUserPassword.text.toString()).
                        addOnCompleteListener(this) { p0 ->
                            if (p0.isSuccessful) {
                                hideKeyboard()
                                editPreferences.putBoolean("UserLogged", true)
                                editPreferences.putString("UserEmail", inputUserEmail.text.toString())
                                editPreferences.putString("UserPassword", inputUserPassword.text.toString())
                                editPreferences.apply()
                                firebaseUser = firebaseAuth.currentUser!!
                                supportActionBar!!.title = "Ваш Профиль"
                                userIsntLogged.text = "Вы вошли в пользователя!"
                                buttonLogout.visibility = View.VISIBLE
                                buttonUpdateUser.visibility = View.VISIBLE
                                buttonDeleteUser.visibility = View.VISIBLE
                                buttonSignIn.visibility = View.GONE
                                buttonResetPassword.visibility = View.GONE
                                buttonRegister.visibility = View.GONE
                                Toast.makeText(baseContext, "Вы вошли в пользователя!", Toast.LENGTH_LONG).show()
                            } else {
                                try {
                                    throw p0.exception!!
                                } catch (e: FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(baseContext, "Введён неверный пароль!", Toast.LENGTH_LONG).show()
                                } catch (e: FirebaseAuthInvalidUserException) {
                                    when (e.errorCode) {
                                        "ERROR_USER_NOT_FOUND" -> {
                                            Toast.makeText(baseContext, "Введённая почта не обнаружена!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                buttonSignIn.visibility = View.VISIBLE
                                buttonResetPassword.visibility = View.VISIBLE
                            }
                            layoutUserCreation.visibility = View.GONE
                        }
                } else {
                    if (inputUserEmail.text.isEmpty() && inputUserPassword.text.isEmpty()) {
                        Toast.makeText(baseContext, "Введите почту и \n пароль пользователя!", Toast.LENGTH_LONG).show()
                    }
                    if (inputUserEmail.text.isEmpty() && inputUserPassword.text.isNotEmpty()) {
                        Toast.makeText(baseContext, "Введите почту пользователя!", Toast.LENGTH_LONG).show()
                    }
                    if (inputUserEmail.text.isNotEmpty() && inputUserPassword.text.isEmpty()) {
                        Toast.makeText(baseContext, "Введите пароль пользователя!", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(baseContext, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonRegister.setOnClickListener {
            vibrateOrSound()
            if (isNetworkConnected()) {
                val activity = Intent(this, SignUpActivity::class.java)
                activity.putExtra("ReasonForLaunch", "Register")
                startActivity(activity)
            } else {
                Toast.makeText(baseContext, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonLogout.setOnClickListener {
            vibrateOrSound()
            if (isNetworkConnected()) {
                hideKeyboard()
                firebaseAuth.signOut()
                editPreferences.putBoolean("UserLogged", false)
                editPreferences.apply()
                supportActionBar!!.title = "Вход"
                userIsntLogged.text = "Вы не вошли в пользователя!"
                buttonSignIn.visibility = View.VISIBLE
                buttonResetPassword.visibility = View.VISIBLE
                buttonRegister.visibility = View.VISIBLE
                buttonLogout.visibility = View.GONE
                buttonUpdateUser.visibility = View.GONE
                buttonDeleteUser.visibility = View.GONE
                Toast.makeText(baseContext, "Вы вышли из пользователя!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(baseContext, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonUpdateUser.setOnClickListener {
            vibrateOrSound()
            if (isNetworkConnected()) {
                val activity = Intent(this, SignUpActivity::class.java)
                activity.putExtra("ReasonForLaunch", "UpdateUser")
                startActivity(activity)
            } else {
                Toast.makeText(baseContext, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonDeleteUser.setOnClickListener {
            vibrateOrSound()
            if (isNetworkConnected()) {
                alertDialogDeleteUserBuilder = AlertDialog.Builder(this)
                alertDialogDeleteUserBuilder.setTitle("Удаление Пользователя")
                alertDialogDeleteUserBuilder.setMessage("Вы уверены, что хотите удалить пользователя " +
                        "${sharedPreferences.getString("UserEmail", "")} и все связанные с ним данные?")
                alertDialogDeleteUserBuilder.setPositiveButton("Подтвердить") { _, _ ->
                    vibrateOrSound()
                    layoutUserCreation.visibility = View.VISIBLE
                    buttonLogout.visibility = View.GONE
                    buttonUpdateUser.visibility = View.GONE
                    buttonDeleteUser.visibility = View.GONE
                    labelUserCreation.text = "Удаляем пользователя ..."
                    firebaseUser.reauthenticate(
                                EmailAuthProvider.getCredential(sharedPreferences.getString("UserEmail", "").toString(),
                                sharedPreferences.getString("UserPassword", "").toString())).addOnCompleteListener { p0 ->
                                if (p0.isSuccessful) {
                                    firebaseUser.delete().addOnCompleteListener { p1 ->
                                        if (p1.isSuccessful) {
                                            editPreferences.putBoolean("UserLogged", false)
                                            editPreferences.apply()
                                            inputUserEmail.setText("")
                                            inputUserPassword.setText("")
                                            supportActionBar!!.title = "Вход"
                                            userIsntLogged.text = "Вы не вошли в пользователя!"
                                            buttonSignIn.visibility = View.VISIBLE
                                            buttonResetPassword.visibility = View.VISIBLE
                                            buttonRegister.visibility = View.VISIBLE
                                            buttonLogout.visibility = View.GONE
                                            buttonUpdateUser.visibility = View.GONE
                                            buttonDeleteUser.visibility = View.GONE
                                            layoutUserCreation.visibility = View.GONE
                                            Toast.makeText(baseContext, "Пользователь удалён!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(baseContext, "Не удалось удалить пользователя!", Toast.LENGTH_LONG).show()
                                            buttonLogout.visibility = View.VISIBLE
                                            buttonUpdateUser.visibility = View.VISIBLE
                                            buttonDeleteUser.visibility = View.VISIBLE
                                            layoutUserCreation.visibility = View.GONE
                                            editPreferences.putBoolean("UserLogged", false)
                                            editPreferences.apply()
                                        }
                                    }
                                } else {
                                    Toast.makeText(baseContext, "Не удалось повторно авторизироваться!", Toast.LENGTH_LONG).show()
                                    buttonLogout.visibility = View.VISIBLE
                                    buttonUpdateUser.visibility = View.VISIBLE
                                    buttonDeleteUser.visibility = View.VISIBLE
                                    layoutUserCreation.visibility = View.GONE
                                    editPreferences.putBoolean("UserLogged", false)
                                    editPreferences.apply()
                                }
                            }
                }
                alertDialogDeleteUserBuilder.setNegativeButton("Отмена") { _, _ ->
                    vibrateOrSound()
                }
                alertDialogDeleteUser = alertDialogDeleteUserBuilder.create()
                alertDialogDeleteUser.show()
            } else {
                Toast.makeText(baseContext, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonResetPassword.setOnClickListener {
            vibrateOrSound()
            if (isNetworkConnected()) {
                if (inputUserEmail.text.isNotEmpty()) {
                    if (isValidEmail(inputUserEmail.text.toString())) {
                        alertDialogResetPasswordBuilder = AlertDialog.Builder(this)
                        alertDialogResetPasswordBuilder.setTitle("Сброс Пароля")
                        alertDialogResetPasswordBuilder.setMessage("Отправка письма для сброса пароля на почту ${inputUserEmail.text}")
                        alertDialogResetPasswordBuilder.setPositiveButton("Продолжить") { _, _ ->
                            vibrateOrSound()
                            layoutUserCreation.visibility = View.VISIBLE
                            buttonSignIn.visibility = View.GONE
                            buttonResetPassword.visibility = View.GONE
                            labelUserCreation.text = "Отправляем письмо ..."
                            firebaseAuth.sendPasswordResetEmail(inputUserEmail.text.toString()).addOnCompleteListener(this) { p0 ->
                                if (p0.isSuccessful) {
                                    Toast.makeText(baseContext, "Письмо для сброса \n пароля отправлено!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(baseContext, "Не удалось отправить \n письмо для сброса пароля!", Toast.LENGTH_LONG).show()
                                }
                                buttonSignIn.visibility = View.VISIBLE
                                buttonResetPassword.visibility = View.VISIBLE
                                layoutUserCreation.visibility = View.GONE
                            }
                        }
                        alertDialogResetPasswordBuilder.setNegativeButton("Отмена") { _, _ ->
                            vibrateOrSound()
                        }
                        alertDialogResetPassword = alertDialogResetPasswordBuilder.create()
                        alertDialogResetPassword.show()
                    } else {
                        Toast.makeText(baseContext, "Введите корректный адрес \n электронной почты!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(baseContext, "Введите почту пользователя!", Toast.LENGTH_LONG).show()
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
        isUserLogged = sharedPreferences.getBoolean("UserLogged", false)
        isVibrateEnabled = sharedPreferences.getBoolean("VibrateEnabled", true)
        isSoundsEnabled = sharedPreferences.getBoolean("SoundsEnabled", false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val actionButtonsInflater = menuInflater
        actionButtonsInflater.inflate(R.menu.user_profile_activity_menu, menu)
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