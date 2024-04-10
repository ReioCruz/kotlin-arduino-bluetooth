#include <SoftwareSerial.h>

String command = "";

// Define pins for Bluetooth module
const int rxPin = 11;
const int txPin = 10;
const int ledPin = 7;

SoftwareSerial bluetooth(rxPin, txPin);

void setup() {
  Serial.begin(9600);
  bluetooth.begin(9600);
  pinMode(ledPin, OUTPUT);
}

void loop() {
  if (bluetooth.available()) {
    char c = bluetooth.read();
      Serial.println(c);
    
    if (c != '\n') {
      command += c;
    } else {
      Serial.println(command);
      toggleLed(command);
      command = "";
    }
  }
}

void toggleLed(String rawCommand) {
  delay(100);
  
  if (rawCommand.charAt(0) != 'C') return;

  String command = rawCommand.substring(2);

  if (command == "ON") {
    digitalWrite(ledPin, HIGH);
  }

  if (command == "OFF") {
    digitalWrite(ledPin, LOW);
  }
} 
