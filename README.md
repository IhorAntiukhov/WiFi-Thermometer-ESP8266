## WiFi Thermometer on ESP8266

This is an *outdoor* WiFi thermometer on an *ESP8266* board. It sends the *temperature* and *humidity* to the *Firebase* Realtime Database. You can view the received values in *my app*. This thermometer is powered by a *Li-Ion 18650* battery, so you can put it anywhere you like. For this thermometer, I also *3D printed* the case. **I talked about my project more detailed in the video below**.

[![Video about my project on YouTube](https://img.youtube.com/vi/DzB9ES87Y24/0.jpg)](https://www.youtube.com/watch?v=DzB9ES87Y24)

## Tools and frameworks that I used

[<img align="left" alt="ArduinoIDE" width="36px" src="https://raw.githubusercontent.com/github/explore/80688e429a7d4ef2fca1e82350fe8e3517d3494d/topics/arduino/arduino.png"/>](https://www.arduino.cc/en/software)
[<img align="left" alt="AndroidStudio" width="36px" src="https://img.icons8.com/color/344/android-studio--v3.png"/>](https://developer.android.com/studio)
[<img align="left" alt="Firebase" width="36px" src="https://raw.githubusercontent.com/github/explore/80688e429a7d4ef2fca1e82350fe8e3517d3494d/topics/firebase/firebase.png"/>](https://firebase.google.com)
[<img align="left" alt="Fusion360" width="36px" src="https://img.icons8.com/color/344/autodesk-fusion-360.png"/>](https://www.autodesk.com/products/fusion-360/overview?term=1-YEAR&tab=subscription)
</br>
</br>

## Arduino IDE libraries

+ [Firebase ESP Client](https://github.com/mobizt/Firebase-ESP-Client)
+ [Arduino JSON v6.15.2](https://github.com/bblanchon/ArduinoJson)
+ [DHT Sensor Library](https://github.com/adafruit/DHT-sensor-library)

## Firebase

I also created a project on the [Firebase](https://firebase.google.com) platform. I have used the following Firebase tools:
+ [Firebase Authentication](https://firebase.google.com/docs/auth)
+ [Realtime Database](https://firebase.google.com/docs/database)

## Components needed to create a device

1. ESP12F
2. DHT21 Sensor
3. Li-Ion 18650 battery
4. Lithium battery charging board
5. Power switch
6. Button
7. 5 10 kOhm resistors
8. MCP1700-3302E low dropout voltage regulator
9. 3 capacitors