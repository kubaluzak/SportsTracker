package org.example.sportstracker.football.domain.match;

import lombok.Builder;
import lombok.Data;
import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.MatchEvent;

import java.time.LocalDateTime;

@Data
@Builder
public class FootballMatchEvent implements MatchEvent {
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    private Competitor actor;
    private String description;

    // Pola specyficzne dla Football (z UML)
    private FootballEventType eventType;
    private int minute;
    private String relatedEventId;
}