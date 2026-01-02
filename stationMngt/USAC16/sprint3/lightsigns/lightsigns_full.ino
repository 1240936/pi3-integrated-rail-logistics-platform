/*
 * LightSigns Component - USAC16 (Complete Implementation)
 *
 * Hardware Requirements:
 * - Arduino Uno/Nano/Mega
 * - LEDs for each track (RGB or individual colors)
 *
 * Pin Configuration (Compatible with USAC10):
 * Track 1: Red=15, Yellow=14, Green=11 (USAC10 compatible)
 * Track 2: Red=18, Yellow=17, Green=16 (USAC10 compatible)
 * Track 3: Red=A0, Yellow=A1, Green=A2
 * Track 4: Red=A3, Yellow=A4, Green=A5
 * Track 5: Red=2, Yellow=3, Green=4 (if available)
 *
 * Commands supported (USAC10 compatible format):
 * GE,x - Green LED on track x
 * RE,x - Red LED on track x
 * YE,x - Yellow LED on track x
 * RB,x - Red blinking on track x (departure signal)
 * OFF,x - Turn off all LEDs on track x
 */

#define MAX_TRACKS 5

// Pin definitions for each track [Red, Yellow, Green]
// Compatible with USAC10 pinout
const int trackPins[MAX_TRACKS][3] = {
    {15, 14, 11},  // Track 1 (USAC10: Red=15, Yellow=14, Green=11)
    {18, 17, 16},  // Track 2 (USAC10: Red=18, Yellow=17, Green=16)
    {A0, A1, A2},  // Track 3
    {A3, A4, A5},  // Track 4
    {2, 3, 4}      // Track 5
};

#define RED 0
#define YELLOW 1
#define GREEN 2

// Serial communication
#define BAUD_RATE 9600

// Blinking state management
unsigned long lastBlinkTime = 0;
bool blinkState = false;
int blinkingTrack = 0; // 0 = none, 1-5 = track number
const unsigned long BLINK_INTERVAL = 500; // milliseconds

void setup() {
    Serial.begin(BAUD_RATE);
    Serial.println("USAC16 LightSigns Starting...");

    // Initialize all LED pins as OUTPUT
    for (int track = 0; track < MAX_TRACKS; track++) {
        for (int color = 0; color < 3; color++) {
            pinMode(trackPins[track][color], OUTPUT);
        }
        // Turn off all LEDs for this track
        turnOffTrack(track + 1);
    }

    Serial.println("LightSigns initialization complete");
}

void loop() {
    // Handle blinking if active
    handleBlinking();

    // Check for incoming commands
    if (Serial.available() > 0) {
        String command = Serial.readStringUntil('\n');
        command.trim();

        Serial.print("Received command: ");
        Serial.println(command);

        processCommand(command);
    }

    delay(50); // Small delay to prevent overwhelming the processor
}

void processCommand(String command) {
    // Parse command: CMD,XX
    int commaIndex = command.indexOf(',');
    if (commaIndex > 0) {
        String cmd = command.substring(0, commaIndex);
        cmd.trim();
        cmd.toUpperCase();

        String trackStr = command.substring(commaIndex + 1);
        trackStr.trim();
        int track = trackStr.toInt();

        // Support both USAC10 format (x) and USAC16 format (XX)
        // toInt() handles both "1" and "01" correctly

        // Validate track number
        if (track >= 1 && track <= MAX_TRACKS) {
            executeCommand(cmd, track);
        } else {
            Serial.print("Invalid track number: ");
            Serial.println(track);
        }
    } else {
        Serial.println("Invalid command format (expected CMD,XX)");
    }
}

void executeCommand(String cmd, int track) {
    Serial.print("Executing ");
    Serial.print(cmd);
    Serial.print(" on track ");
    Serial.println(track);

    if (cmd == "GE") {
        // Turn on green LED
        stopBlinking();
        turnOffTrack(track);
        digitalWrite(trackPins[track-1][GREEN], HIGH);
        Serial.println("Green LED ON");
    }
    else if (cmd == "YE") {
        // Turn on yellow LED
        stopBlinking();
        turnOffTrack(track);
        digitalWrite(trackPins[track-1][YELLOW], HIGH);
        Serial.println("Yellow LED ON");
    }
    else if (cmd == "RE") {
        // Turn on red LED
        stopBlinking();
        turnOffTrack(track);
        digitalWrite(trackPins[track-1][RED], HIGH);
        Serial.println("Red LED ON");
    }
    else if (cmd == "RB") {
        // Start blinking red LED (departure signal)
        stopBlinking();
        turnOffTrack(track);
        blinkingTrack = track;
        blinkState = false;
        lastBlinkTime = millis();
        Serial.println("Red LED blinking (departure)");
    }
    else if (cmd == "OFF") {
        // Turn off all LEDs and stop blinking
        stopBlinking();
        turnOffTrack(track);
        Serial.println("All LEDs OFF");
    }
    else {
        Serial.print("Unknown command: ");
        Serial.println(cmd);
    }
}

// Turn off all LEDs of a specific track
void turnOffTrack(int track) {
    if (track >= 1 && track <= MAX_TRACKS) {
        digitalWrite(trackPins[track-1][RED], LOW);
        digitalWrite(trackPins[track-1][YELLOW], LOW);
        digitalWrite(trackPins[track-1][GREEN], LOW);
    }
}

// Handle blinking logic for departure signals
void handleBlinking() {
    if (blinkingTrack > 0) {
        unsigned long currentTime = millis();
        if (currentTime - lastBlinkTime >= BLINK_INTERVAL) {
            blinkState = !blinkState;
            digitalWrite(trackPins[blinkingTrack-1][RED], blinkState ? HIGH : LOW);
            lastBlinkTime = currentTime;
        }
    }
}

// Stop any active blinking
void stopBlinking() {
    if (blinkingTrack > 0) {
        digitalWrite(trackPins[blinkingTrack-1][RED], LOW);
        blinkingTrack = 0;
        blinkState = false;
    }
}
