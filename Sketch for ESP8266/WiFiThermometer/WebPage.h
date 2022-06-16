const char WebPage[] PROGMEM = R"rawliteral(
<HTML>
  <HEAD>
     <TITLE>WiFi Термометр</TITLE>
     <meta charset="utf-8">
      
      <style>
          input[type="text"] {
              border-color: #0388FC;
              border-width: 3px;
              border-style: solid;
              border-radius: 10px;
              font-size: 25px;
              font-family: monospace;
              font-weight: bold;
              margin-top: -10px;
              color: #54A7FF;
              transition: 0.5s;
              opacity: 1;
              padding-left: 6px;
              padding-right: 6px;
          }
          input[type="password"] {
              border-color: #0388FC;
              border-width: 3px;
              border-style: solid;
              border-radius: 10px;
              font-size: 25px;
              font-family: monospace;
              font-weight: bold;
              margin-top: -10px;
              color: #54A7FF;
              transition: 0.5s;
              opacity: 1;
              padding-left: 6px;
              padding-right: 6px;
          }
          input[type="email"] {
              border-color: #0388FC;
              border-width: 3px;
              border-style: solid;
              border-radius: 10px;
              font-size: 25px;
              font-family: monospace;
              font-weight: bold;
              margin-top: -10px;
              color: #54A7FF;
              transition: 0.5s;
              opacity: 1;
              padding-left: 6px;
              padding-right: 6px;
          }
          input[type="number"] {
              border-color: #0388FC;
              border-width: 3px;
              border-style: solid;
              border-radius: 10px;
              font-size: 25px;
              font-family: monospace;
              font-weight: bold;
              margin-top: -10px;
              color: #54A7FF;
              transition: 0.5s;
              opacity: 1;
              padding-left: 6px;
              padding-right: 6px;
          }
          input[type="text"]::-webkit-input-placeholder {
              color: #0388FC              
          }
          input[type="text"]:focus, input:focus {
              border-color: #54A7FF;
              outline: none;
          }
          input[type="email"]::-webkit-input-placeholder {
              color: #0388FC              
          }
          input[type="email"]:focus, input:focus {
              border-color: #54A7FF;
              outline: none;
          }
          input[type="number"]::-webkit-input-placeholder {
              color: #0388FC             
          }
          input[type="number"]:focus, input:focus {
              border-color: #54A7FF;
              outline: none;
          }
          input[type="password"]::-webkit-input-placeholder {
              color: #0388FC             
          }
          input:-webkit-autofill { 
              -webkit-box-shadow: 0 0 0 30px white inset !important;
              -webkit-text-fill-color: #47A0ff;
          }
          .button {
              background-color: transparent;
              border-color: #0388FC;
              border-style: solid;
              border-width: 3px;
              border-radius: 10px;
              font-size: 28px;
              font-family: monospace;
              opacity: 1;
              transition: 0.5s;
              font-weight: bold;
              color: #0388FC;
          }
          .button:hover {
              opacity: 1;
              transition: 0.5s;
              border-color: #54A7FF;
          }
          .button:focus {
              background-color: #0388FC;
              transition: 0.3s;
              color: white;
              outline: none;
          }
          .suffix {
              padding: 6px, 6px, 6px, 6px;
          }
      </style>
  </HEAD>
  <BODY>
    <CENTER>
        <h1 style="font-family:Helvetica;font-size:40;color:#0388FC;user-select:none;margin-bottom:6px">WiFi Термометр</h1>
        <form>
            <p><input id="ssid_name" name="ssid_name" required type="text" spellcheck="false" placeholder="Название WiFi сети"></p>
            <p><input id="ssid_pass" name="ssid_pass" required type="password" spellcheck="false" minlength="8" placeholder="Пароль WiFi сети"></p>
            <p><input id="user_email" name="user_email" required type="email" spellcheck="false"  placeholder="Почта пользователя"></p>
            <p><input id="user_pass" name="user_pass" required type="password" spellcheck="false" minlength="6" placeholder="Пароль пользователя"></p>
            <p><input id="dht_sensor_interval" name="dht_sensor_interval" required type="number" spellcheck="false" suffix="" min="1" placeholder="Интервал DHT датчика"></p>
            <button style="margin-top:-6px" required class="button">Сохранить Параметры</button>
        </form>
    </CENTER> 
  </BODY>
</HTML>
)rawliteral";
