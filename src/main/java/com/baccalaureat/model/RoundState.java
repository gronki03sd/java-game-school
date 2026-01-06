package com.baccalaureat.model;

/**
 * Authoritative round state enum for proper game lifecycle management.
 */
public enum RoundState {
    INIT,        // Round is being initialized
    RUNNING,     // Round is actively running (timer counting, inputs enabled)
    FINISHED,    // Round has ended (timer stopped, no more scoring/validation)
    DIALOG_SHOWN, // Results dialog is displayed
    TRANSITIONING // Moving to next round or ending game
}