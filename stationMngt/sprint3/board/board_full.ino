/*
 * Board Component - USAC16 (Complete Implementation)
 *
 * Hardware Requirements:
 * - Arduino Uno/Nano/Mega
 * - LCD 16x2 with I2C backpack (address 0x27)
 * - 3 LEDs (Red, Yellow, Green) for track status
 * - Buzzer for emergency alerts
 *
 * Pin Connections:
 * - LCD SDA: A4, SCL: A5
 * - Red LED: pin 8
 * - Yellow LED: pin 9
 * - Green LED: pin 10
 * - Buzzer: pin 11
 */

#include <Wire.h>
#include <LiquidCrystal_I2C.h>

// Hardware pins
#define RED_LED_PIN 8
#define YELLOW_LED_PIN 9
#define GREEN_LED_PIN 10
#define BUZZER_PIN 11

// LCD setup
#define LCD_ADDRESS 0x27
#define LCD_COLUMNS 16
#define LCD_ROWS 2
LiquidCrystal_I2C lcd(LCD_ADDRESS, LCD_COLUMNS, LCD_ROWS);

// Protocol constants
#define STX 0x02
#define LF '\n'

// Track status storage
#define MAX_TRACKS 5
struct TrackInfo {
    int id;
    String state;
    int trainId;
};

TrackInfo tracks[MAX_TRACKS];
int numTracks = 0;

// Emergency state
bool emergencyMode = false;

// Serial communication
String currentPacket = "";
bool receivingPacket = false;

void setup() {
    // Initialize serial
    Serial.begin(9600);
    Serial.println("USAC16 Board System Starting...");

    // Initialize LCD
    lcd.init();
    lcd.backlight();
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Railway Station");
    lcd.setCursor(0, 1);
    lcd.print("Board Ready");
    delay(2000);

    // Initialize LEDs and buzzer
    pinMode(RED_LED_PIN, OUTPUT);
    pinMode(YELLOW_LED_PIN, OUTPUT);
    pinMode(GREEN_LED_PIN, OUTPUT);
    pinMode(BUZZER_PIN, OUTPUT);

    // Turn off all LEDs initially
    digitalWrite(RED_LED_PIN, LOW);
    digitalWrite(YELLOW_LED_PIN, LOW);
    digitalWrite(GREEN_LED_PIN, LOW);
    digitalWrite(BUZZER_PIN, LOW);

    // Initialize tracks array
    for (int i = 0; i < MAX_TRACKS; i++) {
        tracks[i].id = 0;
        tracks[i].state = "UNKNOWN";
        tracks[i].trainId = 0;
    }

    updateDisplay();
    Serial.println("Board initialization complete");
}

void loop() {
    // Process incoming serial data
    while (Serial.available() > 0) {
        char c = Serial.read();

        if (c == STX) {
            // Start of packet
            currentPacket = "";
            receivingPacket = true;
            Serial.println("DEBUG: STX received");
        }
        else if (c == LF && receivingPacket) {
            // End of packet
            receivingPacket = false;
            Serial.print("DEBUG: Packet received: ");
            Serial.println(currentPacket);
            processPacket(currentPacket);
        }
        else if (receivingPacket) {
            // Add character to current packet
            currentPacket += c;
        }
    }

    // Handle emergency mode
    if (emergencyMode) {
        handleEmergency();
    }

    // Update display periodically
    static unsigned long lastUpdate = 0;
    if (millis() - lastUpdate > 1000) {
        updateDisplay();
        lastUpdate = millis();
    }
}

void processPacket(String packet) {
    // Remove checksum (last character)
    if (packet.length() > 0) {
        packet = packet.substring(0, packet.length() - 1);
    }

    Serial.print("Processing: ");
    Serial.println(packet);

    // Parse packet
    if (packet.startsWith("TRACK:")) {
        processTrackUpdate(packet);
    }
    else if (packet.equals("EMERGENCY_STOP")) {
        processEmergencyStop();
    }
    else if (packet.startsWith("DEPARTURE:")) {
        processDeparture(packet);
    }
    else {
        Serial.println("Unknown packet type");
    }
}

void processTrackUpdate(String packet) {
    // Format: TRACK:id:state[:train_id]
    // Remove "TRACK:" prefix
    String data = packet.substring(6);

    Serial.print("Track update data: ");
    Serial.println(data);

    // Split by ':'
    int firstColon = data.indexOf(':');
    int secondColon = data.indexOf(':', firstColon + 1);

    if (firstColon > 0) {
        int trackId = data.substring(0, firstColon).toInt();
        String state = data.substring(firstColon + 1, secondColon > 0 ? secondColon : data.length());
        int trainId = 0;

        if (secondColon > 0) {
            trainId = data.substring(secondColon + 1).toInt();
        }

        Serial.print("Track ");
        Serial.print(trackId);
        Serial.print(" -> ");
        Serial.print(state);
        if (trainId > 0) {
            Serial.print(" (Train ");
            Serial.print(trainId);
            Serial.print(")");
        }
        Serial.println();

        // Update track information
        updateTrackInfo(trackId, state, trainId);
        emergencyMode = false;  // Clear emergency if we get a track update

        // Update LEDs based on primary track (track 1)
        if (trackId == 1) {
            updateLEDs(state);
        }
    }
}

void processEmergencyStop() {
    Serial.println("EMERGENCY STOP activated!");
    emergencyMode = true;

    // Turn on buzzer
    digitalWrite(BUZZER_PIN, HIGH);

    // Show emergency on LCD
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("*** EMERGENCY ***");
    lcd.setCursor(0, 1);
    lcd.print("  STOP TRAINS  ");

    // Flash red LED
    for (int i = 0; i < 5; i++) {
        digitalWrite(RED_LED_PIN, HIGH);
        delay(200);
        digitalWrite(RED_LED_PIN, LOW);
        delay(200);
    }
}

void processDeparture(String packet) {
    // Format: DEPARTURE:id:train_id
    String data = packet.substring(10);

    int colonIndex = data.indexOf(':');
    if (colonIndex > 0) {
        int trackId = data.substring(0, colonIndex).toInt();
        int trainId = data.substring(colonIndex + 1).toInt();

        Serial.print("Departure: Track ");
        Serial.print(trackId);
        Serial.print(", Train ");
        Serial.println(trainId);

        // Show departure message temporarily
        showDepartureMessage(trackId, trainId);
    }
}

void updateTrackInfo(int trackId, String state, int trainId) {
    // Find existing track or add new one
    int trackIndex = -1;
    for (int i = 0; i < numTracks; i++) {
        if (tracks[i].id == trackId) {
            trackIndex = i;
            break;
        }
    }

    if (trackIndex == -1 && numTracks < MAX_TRACKS) {
        // Add new track
        trackIndex = numTracks++;
        tracks[trackIndex].id = trackId;
    }

    if (trackIndex >= 0) {
        tracks[trackIndex].state = state;
        tracks[trackIndex].trainId = trainId;
    }
}

void updateLEDs(String state) {
    // Turn off all LEDs first
    digitalWrite(RED_LED_PIN, LOW);
    digitalWrite(YELLOW_LED_PIN, LOW);
    digitalWrite(GREEN_LED_PIN, LOW);

    // Turn on appropriate LED
    if (state == "FREE") {
        digitalWrite(GREEN_LED_PIN, HIGH);
        Serial.println("LED: GREEN (Free)");
    }
    else if (state == "OCCUPIED") {
        digitalWrite(RED_LED_PIN, HIGH);
        Serial.println("LED: RED (Occupied)");
    }
    else if (state == "MAINTENANCE") {
        digitalWrite(YELLOW_LED_PIN, HIGH);
        Serial.println("LED: YELLOW (Maintenance)");
    }
}

void updateDisplay() {
    if (emergencyMode) {
        return; // Emergency message already displayed
    }

    lcd.clear();

    // Show track status
    if (numTracks == 0) {
        lcd.setCursor(0, 0);
        lcd.print("No tracks");
        lcd.setCursor(0, 1);
        lcd.print("configured");
        return;
    }

    // Display up to 2 tracks on 16x2 LCD
    int tracksToShow = min(2, numTracks);

    for (int i = 0; i < tracksToShow; i++) {
        lcd.setCursor(0, i);
        lcd.print("T");
        lcd.print(tracks[i].id);
        lcd.print(":");
        lcd.print(tracks[i].state.substring(0, 6));  // Truncate if needed

        if (tracks[i].state == "OCCUPIED" && tracks[i].trainId > 0) {
            lcd.print(" T");
            lcd.print(tracks[i].trainId);
        }
    }

    // If we have more tracks, indicate with "..."
    if (numTracks > 2) {
        lcd.setCursor(14, 1);
        lcd.print("...");
    }
}

void showDepartureMessage(int trackId, int trainId) {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("DEPARTURE");
    lcd.setCursor(0, 1);
    lcd.print("Track ");
    lcd.print(trackId);
    lcd.print(" Train ");
    lcd.print(trainId);

    // Flash green LED
    for (int i = 0; i < 6; i++) {
        digitalWrite(GREEN_LED_PIN, !digitalRead(GREEN_LED_PIN));
        delay(300);
    }

    // Return to normal display after 3 seconds
    delay(3000);
    updateDisplay();
}

void handleEmergency() {
    // Continue buzzing during emergency
    static unsigned long lastBeep = 0;
    if (millis() - lastBeep > 1000) {
        digitalWrite(BUZZER_PIN, !digitalRead(BUZZER_PIN));
        lastBeep = millis();
    }
}
