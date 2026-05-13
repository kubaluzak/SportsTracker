package org.example.sportstracker.snooker.domain.match;

import lombok.Builder;
import lombok.Data;
import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.MatchEvent;

import java.time.LocalDateTime;

@Data
@Builder
public class SnookerMatchEvent implements MatchEvent {
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    private Competitor actor;
    private String description;

    // Snooker-specific fields
    private SnookerEventType eventType;
    private int pointsValue;
    private String relatedEventId;
}