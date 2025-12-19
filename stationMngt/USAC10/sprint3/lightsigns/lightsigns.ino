/*
 * LightSigns Component - USAC10
 * 
 * Purpose: Control track LEDs to signal track states
 * 
 * Communication: Waits for commands via Serial:
 * - RE,x : Turn on red LED for track x (x = 1 or 2)
 * - YE,x : Turn on yellow LED for track x
 * - GE,x : Turn on green LED for track x
 * - RB,x : Blink red LED for track x
 * - OFF,x : Turn off all LEDs and stop blinking for track x
 * 
 * Behavior: Before turning on an LED, all LEDs of that track must be turned off
 */

// LED Pin definitions
// Track 1 LEDs
#define TRACK1_RED_PIN 15
#define TRACK1_YELLOW_PIN 14
#define TRACK1_GREEN_PIN 11

// Track 2 LEDs
#define TRACK2_RED_PIN 18
#define TRACK2_YELLOW_PIN 17
#define TRACK2_GREEN_PIN 16

// Serial communication
#define BAUD_RATE 9600

// Blinking state
unsigned long lastBlinkTime = 0; 
bool blinkState = false;
int blinkingTrack = 0; // 0 = none, 1 = track 1, 2 = track 2
const unsigned long BLINK_INTERVAL = 500; // milliseconds

void setup() {
  Serial.begin(BAUD_RATE);
  
  // Initialize all LED pins as OUTPUT
  pinMode(TRACK1_RED_PIN, OUTPUT);
  pinMode(TRACK1_YELLOW_PIN, OUTPUT);
  pinMode(TRACK1_GREEN_PIN, OUTPUT);
  pinMode(TRACK2_RED_PIN, OUTPUT);
  pinMode(TRACK2_YELLOW_PIN, OUTPUT);
  pinMode(TRACK2_GREEN_PIN, OUTPUT);
  
  // Turn off all LEDs initially
  turnOffTrack(1);
  turnOffTrack(2);
}

void loop() {
  // Handle blinking if active
  handleBlinking();
  
  // Check for incoming command
  if (Serial.available() > 0) {
    String command = Serial.readStringUntil('\n');
    command.trim();
    
    // Parse command: CMD,x
    int commaIndex = command.indexOf(',');
    if (commaIndex > 0) {
      String cmd = command.substring(0, commaIndex);
      cmd.trim();
      cmd.toUpperCase();
      
      int track = command.substring(commaIndex + 1).toInt();
      
      if (track == 1 || track == 2) {
        if (cmd == "RE") {
          // Turn on red LED
          stopBlinking();
          turnOffTrack(track);
          digitalWrite(getRedPin(track), HIGH);
        }
        else if (cmd == "YE") {
          // Turn on yellow LED with PWM for brighter output
          stopBlinking();
          turnOffTrack(track);
          digitalWrite(getYellowPin(track), HIGH);
        }
        else if (cmd == "GE") {
          // Turn on green LED with PWM for brighter output
          stopBlinking();
          turnOffTrack(track);
          digitalWrite(getGreenPin(track), HIGH);
        }
        else if (cmd == "RB") {
          // Blink red LED
          stopBlinking();
          turnOffTrack(track);
          blinkingTrack = track;
          blinkState = false;
          lastBlinkTime = millis();
        }
        else if (cmd == "OFF") {
          // Turn off all LEDs and stop blinking
          stopBlinking();
          turnOffTrack(track);
        }
      }
    }
    else {
      // Command without comma (like "GTH" - should be ignored by LightSigns)
    }
  }
  
  delay(50); // Small delay
}

// Turn off all LEDs of a specific track
void turnOffTrack(int track) {
  if (track == 1) {
    digitalWrite(TRACK1_RED_PIN, LOW);
    analogWrite(TRACK1_YELLOW_PIN, 0); // Use analogWrite to turn off PWM
    analogWrite(TRACK1_GREEN_PIN, 0);  // Use analogWrite to turn off PWM
  }
  else if (track == 2) {
    digitalWrite(TRACK2_RED_PIN, LOW);
    analogWrite(TRACK2_YELLOW_PIN, 0); // Use analogWrite to turn off PWM
    analogWrite(TRACK2_GREEN_PIN, 0);  // Use analogWrite to turn off PWM
  }
}

// Get pin number for red LED of a track
int getRedPin(int track) {
  return (track == 1) ? TRACK1_RED_PIN : TRACK2_RED_PIN;
}

// Get pin number for yellow LED of a track
int getYellowPin(int track) {
  return (track == 1) ? TRACK1_YELLOW_PIN : TRACK2_YELLOW_PIN;
}

// Get pin number for green LED of a track
int getGreenPin(int track) {
  return (track == 1) ? TRACK1_GREEN_PIN : TRACK2_GREEN_PIN;
}

// Handle blinking logic
void handleBlinking() {
  if (blinkingTrack > 0) {
    unsigned long currentTime = millis();
    if (currentTime - lastBlinkTime >= BLINK_INTERVAL) {
      blinkState = !blinkState;
      digitalWrite(getRedPin(blinkingTrack), blinkState ? HIGH : LOW);
      lastBlinkTime = currentTime;
    }
  }
}

// Stop blinking
void stopBlinking() {
  if (blinkingTrack > 0) {
    digitalWrite(getRedPin(blinkingTrack), LOW);
    blinkingTrack = 0;
    blinkState = false;
  }
}

