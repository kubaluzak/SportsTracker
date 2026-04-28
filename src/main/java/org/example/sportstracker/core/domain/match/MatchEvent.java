package org.example.sportstracker.core.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.time.LocalDateTime;

public interface MatchEvent {
    LocalDateTime getTimestamp();
    Competitor getActor();
    String getDescription();
    String getRelatedEventId();
}