package org.example.sportstracker.core.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchEvent {
    String getEventId();

    LocalDateTime getTimestamp();

    Competitor getActor();

    String getDescription();

    List<String> getRelatedEventIds();
}