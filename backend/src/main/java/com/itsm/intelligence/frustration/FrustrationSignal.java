package com.itsm.intelligence.frustration;

/** Pattern: Strategy. New signals can be injected without editing the detector. */
public interface FrustrationSignal {
    SignalResult evaluate(FrustrationInput input);
}
