package org.example.sportstracker.core.domain.match;

import org.example.sportstracker.core.domain.score.Score;

public interface MatchEventListener {
    void onEventRecorded(MatchEvent event, Score currentScore);
}