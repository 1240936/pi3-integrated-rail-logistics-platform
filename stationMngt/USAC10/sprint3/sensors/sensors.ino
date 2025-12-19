/*
 * Sensors Component - USAC10
 * 
 * Purpose: Get temperature and humidity values from DHT sensor
 * and provide them to the Manager component using Moving Median filter
 * 
 * Communication: Waits for "GTH" command via Serial, returns formatted string:
 * "TEMP&unit:celsius&value:xx#HUM&unit:percentage&value:xx"
 */

#include <DHT.h>
#include "circular_buffer.h"
#include "moving_median.h"

// DHT Sensor configuration
#define DHTPIN 28         // Pin connected to DHT sensor (GPIO28)
#define DHTTYPE DHT11     // DHT11 sensor

// Circular buffer parameters (configurable)
#define TEMP_BUFFER_LENGTH 10
#define TEMP_WINDOW_LENGTH 5
#define HUM_BUFFER_LENGTH 10
#define HUM_WINDOW_LENGTH 5

// Serial communication
#define BAUD_RATE 9600
#define CMD_BUFFER_SIZE 10

// DHT sensor object
DHT dht(DHTPIN, DHTTYPE);

// Circular buffers for Moving Median filter
int temp_buffer[TEMP_BUFFER_LENGTH];
int temp_nelem = 0;
int temp_tail = 0;
int temp_head = 0;

int hum_buffer[HUM_BUFFER_LENGTH];
int hum_nelem = 0;
int hum_tail = 0;
int hum_head = 0;

// Working arrays for median calculation
int temp_window[TEMP_WINDOW_LENGTH];
int hum_window[HUM_WINDOW_LENGTH];

void setup() {
  Serial.begin(BAUD_RATE);
  
  // Initialize buffers
  for (int i = 0; i < TEMP_BUFFER_LENGTH; i++) {
    temp_buffer[i] = 0;
  }
  for (int i = 0; i < HUM_BUFFER_LENGTH; i++) {
    hum_buffer[i] = 0;
  }
  
  // Initialize DHT sensor
  pinMode(DHTPIN, INPUT_PULLUP);
  dht.begin();
  
  // DHT11 needs time to initialize
  delay(2000);
  
  // Send ready message
  Serial.println("Sensors ready. Send 'GTH' to read sensor.");
}

void loop() {
  // Check for incoming command
  if (Serial.available() > 0) {
    String command = Serial.readStringUntil('\n');
    command.trim();
    
    if (command == "GTH") {
      // DHT11 needs minimum 2 seconds between readings
      delay(2000);
      
      // Read sensor values
      float temp_raw = dht.readTemperature(false);
      float hum_raw = dht.readHumidity();
      
      // Check for errors
      if (isnan(temp_raw) || isnan(hum_raw)) {
        Serial.println("TEMP&unit:celsius&value:0#HUM&unit:percentage&value:0");
        return;
      }
      
      // Check if readings are valid
      if (temp_raw > -50 && temp_raw < 80 && hum_raw >= 0 && hum_raw <= 100) {
        // Add to circular buffers
        enqueue_value(temp_buffer, TEMP_BUFFER_LENGTH, &temp_nelem, &temp_tail, &temp_head, (int)(temp_raw * 10));
        enqueue_value(hum_buffer, HUM_BUFFER_LENGTH, &hum_nelem, &hum_tail, &hum_head, (int)(hum_raw * 10));
        
        // Apply Moving Median filter
        int temp_median = 0;
        int hum_median = 0;
        
        if (temp_nelem >= TEMP_WINDOW_LENGTH) {
          calculate_moving_median(temp_buffer, TEMP_BUFFER_LENGTH, temp_nelem, temp_tail, temp_head, 
                                 TEMP_WINDOW_LENGTH, &temp_median);
        } else {
          temp_median = (int)(temp_raw * 10);
        }
        
        if (hum_nelem >= HUM_WINDOW_LENGTH) {
          calculate_moving_median(hum_buffer, HUM_BUFFER_LENGTH, hum_nelem, hum_tail, hum_head, 
                                 HUM_WINDOW_LENGTH, &hum_median);
        } else {
          hum_median = (int)(hum_raw * 10);
        }
        
        // Format and send response
        String response = "TEMP&unit:celsius&value:";
        response += String(temp_median / 10);
        response += "#HUM&unit:percentage&value:";
        response += String(hum_median / 10);
        
        Serial.println(response);
      } else {
        // Invalid reading - return error format
        Serial.println("TEMP&unit:celsius&value:0#HUM&unit:percentage&value:0");
      }
    }
  }
  
  delay(100); // Small delay to prevent overwhelming the serial buffer
}

