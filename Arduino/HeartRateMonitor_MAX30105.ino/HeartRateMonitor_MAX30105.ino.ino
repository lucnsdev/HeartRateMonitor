// ESP32H2 Consumption:
// Advertsing: 27.2mA
// Connected: 26.8mA

#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <Wire.h>
#include <MAX30105.h>
#include <LogicChanger.h>
#include <Delay.h>

#define MTU 40
#define BUFFER_LENGTH 8

LogicChanger logicChanger;
BLECharacteristic *txCharacteristic = NULL;
BLECharacteristic *rxCharacteristic = NULL;
QueueHandle_t bufferQueue = NULL;
bool connected = false;
MAX30105 particleSensor;

class ConnectionCallback : public BLEServerCallbacks {
  void onConnect(BLEServer *pServer) {
    connected = true;
    Serial.println("Client connected");
    BLEDevice::stopAdvertising();
    logicChanger.setTimers(25, 1975);
  };
  void onDisconnect(BLEServer *pServer) {
    connected = false;
    Serial.println("Client disconnected");
    BLEDevice::startAdvertising();
    logicChanger.setTimers(25, 475);
  }
};

void onLogicChanged(bool enabled) {
  if (connected) {
    if (enabled) neopixelWrite(RGB_BUILTIN, 0, 0, 32);
    else neopixelWrite(RGB_BUILTIN, 0, 0, 0);
  } else {
    if (enabled) neopixelWrite(RGB_BUILTIN, 32, 0, 0);
    else neopixelWrite(RGB_BUILTIN, 0, 0, 0);
  }
}

void run(void *arg) {  // BleTask
  uint8_t buffer[BUFFER_LENGTH];
  while (1) {
    logicChanger.compute();
    if (xQueueReceive(bufferQueue, &buffer, portMAX_DELAY) == pdTRUE) {
      if (connected) {
        //Serial.println("Send data to android...");
        txCharacteristic->setValue(buffer, BUFFER_LENGTH);
        txCharacteristic->notify();
      }
    }
    if (connected) {
      size_t size = rxCharacteristic->getLength();
      if (size) {
        uint8_t *data = rxCharacteristic->getData();
        rxCharacteristic->setValue(nullptr, 0);  // flush
      }
    }
  }
}

void run2(void *arg) {  // ReaderTask
  int64_t initialTime = 0;
  uint16_t index = 0;
  uint8_t buffer[BUFFER_LENGTH];
  uint32_t ppg = 0;
  while (1) {
    int64_t now = esp_timer_get_time();
    if (now - initialTime >= 820) {  // 1000sps
      initialTime = now;
      particleSensor.check();
      if (particleSensor.available()) {
        ppg = 262143 - particleSensor.getFIFORed();
        //ppg = 262143 - particleSensor.getFIFOIR();  // 262.143
        //Serial.println(ppg);
        buffer[index++] = (ppg >> 8) & 0xFF;
        buffer[index++] = ppg & 0xFF;
        particleSensor.nextSample();
      }

      if (index == BUFFER_LENGTH) {
        index = 0;
        if (xQueueSend(bufferQueue, &buffer, 0) != pdTRUE) {
          Serial.println("Send buffer process to another task failed!");
          vTaskDelay(pdMS_TO_TICKS(10));
        }
      }
    }
    taskYIELD();
  }
}

void setup(void) {
  neopixelWrite(RGB_BUILTIN, 8, 8, 8);
  pinMode(15, INPUT);
  logicChanger.setCallback(onLogicChanged);
  logicChanger.setTimers(25, 475);

  Serial.begin(115200);
  Wire.setPins(16, 17);  // SDA, SCL
  if (!particleSensor.begin(Wire, I2C_SPEED_FAST)) {
    Serial.println("MAX30105 was not found. Please check wiring/power.");
    while (!particleSensor.begin(Wire, I2C_SPEED_FAST)) {
      delay(500);
      neopixelWrite(RGB_BUILTIN, 8, 0, 0);
      delay(500);
      neopixelWrite(RGB_BUILTIN, 0, 0, 0);
    }
  }
  byte ledBrightness = 0x24;  //32;  //Options: 0=Off to 255=50mA
  byte sampleAverage = 1;     //4;   //Options: 1, 2, 4, 8, 16, 32
  byte ledMode = 2;           //Options: 1 = Red only, 2 = Red + IR, 3 = Red + IR + Green
  int sampleRate = 3200;      //Options: 50, 100, 200, 400, 800, 1000, 1600, 3200
  int pulseWidth = 69;        //Options: 69, 118, 215, 411
  int adcRange = 4096;        //Options: 2048, 4096, 8192, 16384
  particleSensor.setup(ledBrightness, sampleAverage, ledMode, sampleRate, pulseWidth, adcRange);
  //particleSensor.setup();

  BLEDevice::init("Heart Rate Monitor");
  BLEDevice::setMTU(MTU);
  Serial.print("BLE MTU: ");
  Serial.println(BLEDevice::getMTU());
  Serial.print("BLE Device address: ");
  Serial.println(BLEDevice::getAddress().toString());

  BLEServer *server = BLEDevice::createServer();
  server->setCallbacks(new ConnectionCallback());

  const char *UUID_SERVICE = "f87bf854-b36b-417c-a3d1-c12f91a30001";
  BLEService *service = server->createService(UUID_SERVICE);
  txCharacteristic = service->createCharacteristic("f87bf854-b36b-417c-a3d1-c12f91a30002", BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  rxCharacteristic = service->createCharacteristic("f87bf854-b36b-417c-a3d1-c12f91a30003", BLECharacteristic::PROPERTY_WRITE);
  service->start();

  BLEAdvertising *advertising = BLEDevice::getAdvertising();
  advertising->addServiceUUID(UUID_SERVICE);
  advertising->setScanResponse(true);
  advertising->setMinPreferred(0x06);
  advertising->setMaxPreferred(0x0C);
  BLEDevice::startAdvertising();

  Serial.println("Initializing tasks....");
  bufferQueue = xQueueCreate(32, BUFFER_LENGTH);
  xTaskCreatePinnedToCore(run, "BleTask", 4096, NULL, 4, NULL, APP_CPU_NUM);
  xTaskCreatePinnedToCore(run2, "ReaderTask", 4096, NULL, 4, NULL, APP_CPU_NUM);

  Serial.println("Ready.");
}

void loop(void) {
}
