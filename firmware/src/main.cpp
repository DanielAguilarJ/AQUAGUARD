#include <Arduino.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <Wire.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <RTClib.h>
#include "config.h"  // WIFI_SSID, SERVER_URL

// Pines de sensores
#define FLOW_SENSOR_PIN  4   // D4
#define PRESSURE_SENSOR_PIN 34 // ADC1_CH6

// Variables de flujo
volatile uint32_t pulseCount = 0;
float flowRate = 0.0;
unsigned long flowLastMeasure = 0;

// Componentes
Adafruit_MPU6050 mpu;
RTC_DS3231 rtc;

// ISR para contador de pulsos del sensor de flujo
void IRAM_ATTR onPulse() {
  pulseCount++;
}

void setup() {
  Serial.begin(115200);
  delay(1000);

  // Configurar pin de flujo
  pinMode(FLOW_SENSOR_PIN, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(FLOW_SENSOR_PIN), onPulse, FALLING);

  // Inicializar I2C
  Wire.begin();

  // Inicializar MPU6050
  if (!mpu.begin()) {
    Serial.println("Error al inicializar MPU6050");
  } else {
    mpu.setAccelerometerRange(MPU6050_RANGE_8_G);
    mpu.setGyroRange(MPU6050_RANGE_500_DEG);
    Serial.println("MPU6050 inicializado");
  }

  // Inicializar RTC
  if (!rtc.begin()) {
    Serial.println("Error al inicializar DS3231");
  } else {
    if (rtc.lostPower()) {
      rtc.adjust(DateTime(F(__DATE__), F(__TIME__)));
    }
    Serial.println("RTC DS3231 listo");
  }

  // Conectar a WiFi
  Serial.printf("Conectando a WiFi %s...", WIFI_SSID);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 20000) {
    Serial.print('.');
    delay(500);
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("Conectado");
  } else {
    Serial.println("Fallo en conexión WiFi");
  }

  flowLastMeasure = millis();
}

void loop() {
  // Calcular caudal cada segundo
  if (millis() - flowLastMeasure >= 1000) {
    noInterrupts();
    uint32_t pulses = pulseCount;
    pulseCount = 0;
    interrupts();

    // Sensor YF-S201: ~7.5 pulsos por L/min
    flowRate = (pulses / 7.5);
    flowLastMeasure = millis();
  }

  // Leer presión (ADC 12-bit -> 0-4095)
  int rawPressure = analogRead(PRESSURE_SENSOR_PIN);
  float voltage = rawPressure * (3.3 / 4095.0);
  // Convertir voltaje a presión (según rango del sensor)
  float pressure = (voltage - 0.5) * 100.0; // ejemplo: 0.5V->0, 2.5V->200kPa

  // Leer acelerómetro
  sensors_event_t a, g, temp;
  mpu.getEvent(&a, &g, &temp);
  float vibration = a.acceleration.x; // eje X como vibración

  // Timestamp
  DateTime now = rtc.now();
  char timestamp[32];
  snprintf(timestamp, sizeof(timestamp), "%04u-%02u-%02uT%02u:%02u:%02uZ", now.year(), now.month(), now.day(), now.hour(), now.minute(), now.second());

  // Construir JSON
  String payload = "{";
  payload += "\"timestamp\":\"" + String(timestamp) + "\",";
  payload += "\"flujo\":" + String(flowRate, 2) + ",";
  payload += "\"presion\":" + String(pressure, 2) + ",";
  payload += "\"vibracion\":" + String(vibration, 2);
  payload += "}";

  // Enviar datos via HTTP POST
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    http.begin(SERVER_URL);
    http.addHeader("Content-Type", "application/json");
    int code = http.POST(payload);
    if (code > 0) {
      Serial.printf("HTTP %d, payload: %s\n", code, payload.c_str());
    } else {
      Serial.printf("Error en POST: %s\n", http.errorToString(code).c_str());
    }
    http.end();
  }

  delay(5000); // espera entre lecturas
}
