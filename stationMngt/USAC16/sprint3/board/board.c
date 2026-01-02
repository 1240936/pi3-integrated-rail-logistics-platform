// Board Component - USAC16
// Displays track management information with a funny interface
// Uses ASCII art, boxes, icons, and symbols for visual appeal

#include "board.h"
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

/**
 * Clear the screen (simple implementation)
 */
void clear_screen(void) {
    // ANSI escape code to clear screen and move cursor to top-left
    printf("\033[2J\033[H");
}

/**
 * Print top border of a box
 */
void print_box_top(int width) {
    printf("╔");
    for (int i = 0; i < width - 2; i++) {
        printf("═");
    }
    printf("╗\n");
}

/**
 * Print middle line of a box with text
 */
void print_box_middle(const char* text, int width) {
    printf("║");
    int text_len = 0;
    const char* p = text;
    while (*p) {
        text_len++;
        p++;
    }
    
    int padding = (width - 2 - text_len) / 2;
    for (int i = 0; i < padding; i++) {
        printf(" ");
    }
    printf("%s", text);
    for (int i = 0; i < width - 2 - text_len - padding; i++) {
        printf(" ");
    }
    printf("║\n");
}

/**
 * Print bottom border of a box
 */
void print_box_bottom(int width) {
    printf("╚");
    for (int i = 0; i < width - 2; i++) {
        printf("═");
    }
    printf("╝\n");
}

/**
 * Display track assigned to train
 */
void display_track_assigned(int track_id, int train_id) {
    clear_screen();
    
    printf("\n");
    print_box_top(60);
    print_box_middle("╔═══════════════════════════════════════════════════╗", 60);
    print_box_middle("║  🚂 TRACK ASSIGNMENT SUCCESSFUL! 🚂              ║", 60);
    print_box_middle("╚═══════════════════════════════════════════════════╝", 60);
    printf("║                                                          ║\n");
    printf("║   Track ID: %-3d  ────────>  Train ID: %-3d              ║\n", track_id, train_id);
    printf("║                                                          ║\n");
    printf("║        ╔══════════╗              ╔══════════╗            ║\n");
    printf("║        ║  Track   ║              ║  Train   ║            ║\n");
    printf("║        ║    %2d   ║   =======>   ║    %3d   ║            ║\n", track_id, train_id);
    printf("║        ╚══════════╝              ╚══════════╝            ║\n");
    printf("║                                                          ║\n");
    printf("║   ✓ Assignment confirmed!                                ║\n");
    printf("║   ✓ Track is now OCCUPIED                                ║\n");
    print_box_bottom(60);
    printf("\n");
    fflush(stdout);
}

/**
 * Display track set as non-operational (maintenance)
 */
void display_track_maintenance(int track_id) {
    clear_screen();
    
    printf("\n");
    print_box_top(60);
    print_box_middle("╔═══════════════════════════════════════════════════╗", 60);
    print_box_middle("║  ⚠️  TRACK MAINTENANCE MODE  ⚠️                  ║", 60);
    print_box_middle("╚═══════════════════════════════════════════════════╝", 60);
    printf("║                                                          ║\n");
    printf("║   Track ID: %-3d  ────>  MAINTENANCE MODE                ║\n", track_id);
    printf("║                                                          ║\n");
    printf("║        ╔══════════╗                                      ║\n");
    printf("║        ║  Track   ║                                      ║\n");
    printf("║        ║    %2d   ║   🔧 🔨 🛠️                          ║\n", track_id);
    printf("║        ╚══════════╝                                      ║\n");
    printf("║                                                          ║\n");
    printf("║   ⚠️  Track is now NON-OPERATIONA                        ║\n");
    printf("║   ⚠️  No trains can use this track                       ║\n");
    print_box_bottom(60);
    printf("\n");
    fflush(stdout);
}

/**
 * Display track set as free
 */
void display_track_free(int track_id) {
    clear_screen();
    
    printf("\n");
    print_box_top(60);
    print_box_middle("╔═══════════════════════════════════════════════════╗", 60);
    print_box_middle("║  ✅ TRACK IS NOW FREE! ✅                        ║", 60);
    print_box_middle("╚═══════════════════════════════════════════════════╝", 60);
    printf("║                                                           ║\n");
    printf("║   Track ID: %-3d  ────>  AVAILABLE FOR USE                ║\n", track_id);
    printf("║                                                           ║\n");
    printf("║        ╔══════════╗                                       ║\n");
    printf("║        ║  Track   ║                                       ║\n");
    printf("║        ║    %2d   ║   ✓ FREE ✓                            ║\n", track_id);
    printf("║        ╚══════════╝                                       ║\n");
    printf("║                                                           ║\n");
    printf("║   ✓ Track is now available                                ║\n");
    printf("║   ✓ Ready for new assignments                             ║\n");
    print_box_bottom(60);
    printf("\n");
    fflush(stdout);
}

/**
 * Display departure order for a train
 */
void display_departure_order(int track_id, int train_id) {
    clear_screen();
    
    printf("\n");
    print_box_top(60);
    print_box_middle("╔═══════════════════════════════════════════════════╗", 60);
    print_box_middle("║  🚦 DEPARTURE ORDER AUTHORIZED! 🚦               ║", 60);
    print_box_middle("╚═══════════════════════════════════════════════════╝", 60);
    printf("║                                                           ║\n");
    printf("║   Track ID: %-3d  ────>  Train ID: %-3d                   ║\n", track_id, train_id);
    printf("║                                                           ║\n");
    printf("║        ╔══════════╗              ╔══════════╗             ║\n");
    printf("║        ║  Track   ║              ║  Train   ║             ║\n");
    printf("║        ║    %2d   ║   =======>   ║    %3d   ║             ║\n", track_id, train_id);
    printf("║        ╚══════════╝              ╚══════════╝             ║\n");
    printf("║                                                           ║\n");
    printf("║   🚦 GREEN LIGHT - PROCEED!                               ║\n");
    printf("║   ✓ Departure order confirmed                             ║\n");
    printf("║   ✓ Train can now leave the station                       ║\n");
    print_box_bottom(60);
    printf("\n");
    fflush(stdout);
}

/**
 * Display emergency stop order
 */
void display_emergency_stop(int train_id) {
    clear_screen();
    
    printf("\n");
    print_box_top(60);
    print_box_middle("╔═══════════════════════════════════════════════════╗", 60);
    print_box_middle("║  🚨 EMERGENCY STOP ORDER! 🚨                     ║", 60);
    print_box_middle("╚═══════════════════════════════════════════════════╝", 60);
    printf("║                                                          ║\n");
    printf("║   Train ID: %-3d  ────>  NO TRACK AVAILABLE!             ║\n", train_id);
    printf("║                                                          ║\n");
    printf("║              ╔══════════╗                                ║\n");
    printf("║              ║  Train   ║                                ║\n");
    printf("║              ║    %3d   ║    🚨 STOP! 🚨                ║\n", train_id);
    printf("║              ╚══════════╝                                ║\n");
    printf("║                                                          ║\n");
    printf("║   ⚠️  EMERGENCY STOP ORDER ISSUED                        ║\n");
    printf("║   ⚠️  No available tracks for assignment                 ║\n");
    printf("║   ⚠️  Train must remain stopped                          ║\n");
    print_box_bottom(60);
    printf("\n");
    fflush(stdout);
}

/**
 * Display synoptic board with all tracks and trains
 */
void display_synopsis(int tracks[], int trains[], int states[], int num_tracks) {
    clear_screen();
    
    printf("\n");
    print_box_top(70);
    print_box_middle("╔═══════════════════════════════════════════════════════════════╗", 70);
    print_box_middle("║      📊 STATION SYNOPSIS BOARD 📊                            ║", 70);
    print_box_middle("╚═══════════════════════════════════════════════════════════════╝", 70);
    printf("║                                                                 ║\n");
    printf("║  ┌─────────────────────────────────────────────────────────┐    ║\n");
    printf("║  │  Track ID  │  Status      │  Train ID  │  Icon          │    ║\n");
    printf("║  ├─────────────────────────────────────────────────────────┤    ║\n");
    
    for (int i = 0; i < num_tracks; i++) {
        const char* status_text;
        const char* icon;
        
        if (states[i] == 0) {  // FREE
            status_text = "FREE      ";
            icon = "✅";
        } else if (states[i] == 1) {  // OCCUPIED
            status_text = "OCCUPIED  ";
            icon = "🚂";
        } else {  // MAINTENANCE
            status_text = "MAINTENANCE";
            icon = "🔧";
        }
        
        printf("║  │     %2d    │  %-11s │     %3d    │  %s           │    ║\n",
               tracks[i], status_text, trains[i], icon);
        if (i < num_tracks - 1) {
            printf("║  ├─────────────────────────────────────────────────────────┤    ║\n");
        }
    }
    
    printf("║  └─────────────────────────────────────────────────────────┘    ║\n");
    printf("║                                                                   ║\n");
    printf("║  Legend: ✅ FREE  🚂 OCCUPIED  🔧 MAINTENANCE                    ║\n");
    print_box_bottom(70);
    printf("\n");
    fflush(stdout);
}

