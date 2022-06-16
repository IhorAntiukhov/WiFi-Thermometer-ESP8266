#include <ESP8266WiFi.h> // библиотека для работы с WiFi на плате ESP8266
#include <ESP8266WebServer.h> // библиотека для работы с Web сервером на плате ESP8266
#include <Firebase_ESP_Client.h> // библиотека для работы с облачной базой данных Firebase
#include "addons/TokenHelper.h" // дополнительная библиотека для работы с облачной базой данных Firebase

#include <FS.h> // библиотека для работы с оболочкой файловой системы SPIFFS
#include <LittleFS.h> // библиотека для работы с файловой системой SPIFFS
#include "WebPage.h" // HTML страница для настройки параметров работы

#include "DHT.h" // библиотека для работы с DHT датчиком

#define DHT_PIN 14 // пин к которому подключен DHT датчик
#define RESET_BUTTON_PIN 12 // пин к которому подключена кнопка для сброса параметров работы
#define DHT_TYPE DHT21 // тип DHT датчика (DHT11, DHT22 (AM2302), DHT21 (AM2301))

const char *SSIDName = "WiFi Термометр"; // название WiFi сети для настройки параметров работы
const char *SSIDPass = "12345678"; // пароль WiFi сети для настройки параметров работы

String settings; // переменая для хранения всех параметров работы
String ssidName; // переменная для хранения названия вашей WiFi сети
String ssidPass; // переменная для хранения пароля вашей WiFi сети
String userEmail; // переменная для хранения почты вашего пользователя
String userPassword; // переменная для хранения пароля вашего пользователя

int dhtSensorInterval; // переменная для хранения интервала получения температуры и влажности

int firstHashIndex;  // переменная для хранения индекса первого разделительного символа
int secondHashIndex; // переменная для хранения индекса второго разделительного символа
int thirdHashIndex;  // переменная для хранения индекса третьего разделительного символа
int fourthHashIndex; // переменная для хранения индекса четвёртого разделительного символа

int temperature = 0; // переменная для хранения температуры
int humidity = 0; // переменная для хранения влажности

bool isTemperatureSavedSuccessfully; // переменная для хранения того, сохранена ли температура в Firebase
bool isHumiditySavedSuccessfully; // переменная для хранения того, сохранена ли влажность в Firebase
int readDHTValuesAttempts; // переменная для хранения количества попыток получения температуры и влажности

int connectToWiFiAttempts;

bool resetButtonFlag; // переменная-флажок для кнопки сброса параметров работы
bool resetButtonState; // переменная для хранения текущего состояния кнопки сброса параметров работы

unsigned long resetButtonMillis; // переменная для хранения времени нажатия или отпускания кнопки для сброса параметров работы

DHT dht(DHT_PIN, DHT_TYPE); // объект для работы с DHT датчиком

FirebaseData firebaseData; // объект для работы с данными в Firebase
FirebaseAuth firebaseAuth; // объект для работы с cистемой авторизации в Firebase
FirebaseConfig firebaseConfig; // объект для настройки подключения к облачной базе данных Firebase

ESP8266WebServer server(80); // объект для работы с Web сервером для настройки параметров работы

// функция для отправки Web клиенту HTML страницы, или получения параметров работы от него
void handleRoot() {
  server.send(200, "text/html", WebPage); // отправляем Web клиенту HTML страницу для настройки параметров работы

  // если Web клиент указал параметры работы в url строке, записываем их в переменные
  if (server.hasArg("ssid_name"))  ssidName = server.arg("ssid_name");
  if (server.hasArg("ssid_pass"))  ssidPass = server.arg("ssid_pass");
  if (server.hasArg("user_email")) userEmail = server.arg("user_email");
  if (server.hasArg("user_pass"))  userPassword = server.arg("user_pass");
  if (server.hasArg("dht_sensor_interval")) dhtSensorInterval = (server.arg("dht_sensor_interval")).toInt();

  if (ssidName != "-" and ssidPass != "-" and userEmail != "-" and userPassword != "-") { // если Web клиент указал параметры работы
    settings = ssidName + "#" + ssidPass + "#" + userEmail + "#" + userPassword + "#" + String(dhtSensorInterval); // записываем параметры работы в одну переменную
    Serial.println("Параметры работы: " + String(settings));
    File settingsFile = LittleFS.open("/Settings.txt", "w"); // открываем файл с параметрами работы для записи

    if (settingsFile) { // если файл с параметрами работы успешно открыт для записи
      Serial.println("Файл с параметрами работы успешно открыт для записи!");
    } else {
      Serial.println("Не удалось открыть файл с параметрами работы для записи :(");
      return;
    }
    
    if (settingsFile.print(settings)) { // записываем параметры работы в SPIFFS память
      Serial.println("Файл был успешно записан!");
      settingsFile.close(); // закрываем файл с параметрами работы
      server.stop(); // останавливаем Web сервер для настройки параметров работы
      WiFi.softAPdisconnect(true); // останавливаем WiFi точку, на которой запущен Web сервер для настройки параметров работы
      LittleFS.end(); // останавливаем файловую систему SPIFFS
      ESP.restart(); // перезагружаем плату ESP8266
    } else {
      Serial.println("Не удалось записать файл :(");
      return;
    }
  }
}

void setup() {
  Serial.begin(115200); // настраиваем скорость COM порта
  pinMode(RESET_BUTTON_PIN, INPUT); // настраиваем пин к которому подключена кнопка для сброса параметров работы

  if (LittleFS.begin()) { // если файловая система SPIFFS успешно запущена
    Serial.println("Монтирование SPIFFS прошло успешно!");
  } else {
    Serial.println("Не удалось монтировать SPIFFS :(");
    return;
  }

  if (LittleFS.exists("/Settings.txt")) { // если файл с параметрами работы находится в SPIFFS памяти
    File settingsFile = LittleFS.open("/Settings.txt", "r"); // открываем файл с параметрами работы для чтения
    if (settingsFile) { // если файл с параметрами работы успешно открыт для чтения
      Serial.println("Файл с параметрами работы успешно открыт для чтения!");
    } else {
      Serial.println("Не удалось открыть файл с параметрами работы для чтения :(");
      return;
    }

    while (settingsFile.available()) { // пока файл с параметрами работы открыт
      settings = settingsFile.readString(); // получаем содержимое файла 
      Serial.println("Содержимое файла: " + String(settings));

      firstHashIndex  = settings.indexOf('#'); // записываем индекс первого разделительного символа
      secondHashIndex = settings.indexOf('#', firstHashIndex + 1);  // записываем индекс второго разделительного символа
      thirdHashIndex  = settings.indexOf('#', secondHashIndex + 1); // записываем индекс третьего разделительного символа
      fourthHashIndex = settings.indexOf('#', thirdHashIndex + 1);  // записываем индекс четвертого разделительного символа

      ssidName     = settings.substring(0, firstHashIndex); // выделяем из всех параметров работы название вашей WiFi сети 
      ssidPass     = settings.substring(firstHashIndex + 1, secondHashIndex); // выделяем из всех параметров работы пароль вашей WiFi сети 
      userEmail    = settings.substring(secondHashIndex + 1, thirdHashIndex); // выделяем из всех параметров работы почту вашего пользователя
      userPassword = settings.substring(thirdHashIndex + 1, fourthHashIndex); // выделяем из всех параметров работы пароль вашего пользователя
      dhtSensorInterval = (settings.substring(fourthHashIndex + 1, settings.length())).toInt(); // выделяем из всех параметров работы интервал получения температуры и влажности
    }

    settingsFile.close(); // закрываем файл с параметрами работы
  } else {
    // записываем то, что параметры работы не указаны
    settings     = "-";
    ssidName     = "-";
    ssidPass     = "-";
    userEmail    = "-";
    userPassword = "-";
  }

  if (settings != "-") { // если параметры работы указаны
    if (!digitalRead(RESET_BUTTON_PIN)) { // если кнопка для сброса параметров работы нажата (PS: на пинах платы ESP8266 по умолчанию единица, а не ноль)
      for (int i = 0; i <= 3025; i++) { // цикл который выполняется 3025 миллисекунд
        resetButtonState = !digitalRead(RESET_BUTTON_PIN); // записываем состояние кнопки для сброса параметров работы
        if (resetButtonState && !resetButtonFlag && millis() - resetButtonMillis > 100) { // если кнопка нажата
          resetButtonMillis = millis(); // записываем время нажатия кнопки
          resetButtonFlag = true; // поднимаем флажок
          Serial.println("Кнопка сброса настроек нажата!");
        }
        
        if (!resetButtonState && resetButtonFlag && millis() - resetButtonMillis > 250) { // если кнопка отпущена
          resetButtonMillis = millis(); // записываем время отпускания кнопки
          resetButtonFlag = false; // опускаем флажок
          Serial.println("Кнопка сброса настроек отпущена!");
          break; // выходим из цикла
        }
        
        if (resetButtonState && resetButtonFlag && millis() - resetButtonMillis > 3000) { // если кнопка удерживается 3 секунды
          Serial.println("Кнопка сброса настроек зажата!");
          File settingsFile = LittleFS.open("/Settings.txt", "w"); // открываем файл с параметрами работы для записи
        
          if (settingsFile) { // если файл с параметрами работы успешно открыт для записи
            Serial.println("Файл с параметрами работы успешно открыт для записи!");
          } else {
            Serial.println("Не удалось открыть файл с параметрами работы для записи :(");
            return;
          }
            
          if (settingsFile.print("-")) { // сбрасываем параметры работы
            Serial.println("Параметры работы успешно сброшены!");
            settingsFile.close(); // закрываем файл с параметрами работы
            LittleFS.end(); // останавливаем файловую систему SPIFFS
            ESP.restart(); // перезагружаем плату ESP8266
          } else {
            Serial.println("Не удалось сбросить параметры работы :(");
            return;
          }

        }
        delay(1); // делаем задержку для того, чтобы цикл выполнялся определённое время
      }
    }
    dht.begin(); // запускаем DHT датчик

    do {
      connectToWiFiAttempts++;
      Serial.println("Подключаемся к " + String(ssidName));
      WiFi.begin(ssidName.c_str(), ssidPass.c_str()); // подключаемся к вашей WiFi сети
      if (WiFi.waitForConnectResult() == WL_CONNECTED) { // если плата ESP8266 подключена к WiFi сети
        Serial.print("Подключились к WiFi сети! Локальный IP адрес: "); Serial.println(WiFi.localIP());
    
        firebaseAuth.user.email = userEmail.c_str(); // устанавливаем почту пользователя
        firebaseAuth.user.password = userPassword.c_str(); // устанавливаем пароль пользователя
    
        firebaseConfig.api_key = "AIzaSyAAAEaj-ldHhqKvSEjWwEIz-I0UlDvDevs"; // устанавливаем api ключ Firebase
        firebaseConfig.database_url = "https://wifioutdoorthermometer-default-rtdb.firebaseio.com/"; // устанавливаем адрес Firebase
        firebaseConfig.token_status_callback = tokenStatusCallback;
    
        Firebase.begin(&firebaseConfig, &firebaseAuth); // подключаемся к Firebase
        Firebase.reconnectWiFi(true); // устанавливаем то, будет ли плата ESP8266 переподключаться к вашей WiFi сети при потери соединения
    
        firebaseData.setBSSLBufferSize(1024, 1024); // устанавливаем размер буферов WiFi rx/tx, в том случае, если мы будем работать с большими данными
        firebaseData.setResponseSize(1024); // устанавливаем размер HTTP ответов, в том случае, если мы будем работать с большими данными
        Firebase.RTDB.setwriteSizeLimit(&firebaseData, "small"); // устанавливаем таймаут записи данных в Firebase ("tiny" - 1 сек, "small" - 10 сек, "medium" - 30 сек, "large" - 60 сек)
    
        while (!Firebase.ready()) { // ждём пока плата ESP8266 подключится к облачной базе данных Firebase
          Serial.print(".");
          delay(100);
        }
  
        int humidity;
        int temperature;
    
        do { // пока температура и влажность не получена правильно
          if (readDHTValuesAttempts < 5) { // если количество попыток получения температуры и влажности меньше 5
            humidity = dht.readHumidity();
            temperature = dht.readTemperature();
            readDHTValuesAttempts++; // увеличиваем количество попыток получения температуры и влажности на 1
            Serial.println("Температура: " + String(temperature) + " Влажность: " + String(humidity));
            if ((temperature > 100 && humidity > 100) || (isnan(humidity) || isnan(temperature))
            || (temperature == -50 && humidity == 1)) { // если получить температуру и влажность не удалось
              delay(2000); // делаем задержку для того, чтобы DHT датчик успел обновить значения температуры и влажности
            }
          } else {
            ESP.deepSleep(dhtSensorInterval * 60000000); // переходим в глубокий сон на определённый интервал
          }
        } while ((temperature > 100 && humidity > 100) || (isnan(humidity) || isnan(temperature)) || (temperature == -50 && humidity == 1));
    
        while (!isTemperatureSavedSuccessfully && !isHumiditySavedSuccessfully) { // пока температура и влажность не сохранена в Firebase
          if (!isTemperatureSavedSuccessfully) { // если температура не сохранена в Firebase
            Serial.println("Записываем температуру в Firebase ...");
            isTemperatureSavedSuccessfully = Firebase.RTDB.setInt(&firebaseData, ("/users/" + firebaseAuth.token.uid + "/temperature").c_str(), temperature); // сохраняем температуру
          }
          if (!isHumiditySavedSuccessfully) { // если влажность не сохранена в Firebase
            Serial.println("Записываем влажность в Firebase ...");
            isHumiditySavedSuccessfully = Firebase.RTDB.setInt(&firebaseData, ("/users/" + firebaseAuth.token.uid + "/humidity").c_str(), humidity); // сохраняем влажность
          }
        }
        Serial.println("Температура и влажность записаны в Firebase!");
    
        LittleFS.end(); // останавливаем файловую систему SPIFFS
        ESP.deepSleep(dhtSensorInterval * 60000000); // переходим в глубокий сон на определённый интервал
      } else if (WiFi.waitForConnectResult() == WL_CONNECT_FAILED) { // если плате ESP8266 не удалось подключиться к WiFi сети 
        Serial.println("Не удалось подключиться к WiFi сети :(");
        ESP.deepSleep(dhtSensorInterval * 60000000); // переходим в глубокий сон на определённый интервал
      }
    } while (WiFi.waitForConnectResult() != WL_CONNECTED && connectToWiFiAttempts < 3);
  } else {
    WiFi.softAP(SSIDName, SSIDPass); // запускаем WiFi точку для настройки параметров работы
    server.on("/", handleRoot); // указываем функцию для отправки Web клиенту HTML страницы, или получения параметров работы от него
    server.begin(); // запускаем Web сервер для настройки параметров работы
    Serial.println("Web сервер для настройки параметров работы запущен! Локальный IP адрес: 192.168.4.1");
  }
}

void loop() {
  server.handleClient(); // выполняем функцию для отправки Web клиенту HTML страницы, или получения параметров работы от него, когда Web клиент переходит на Web сервер
}
