package org.example.sportstracker.football.domain.match;

import lombok.Builder;
import lombok.Data;
import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.MatchEvent;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FootballMatchEvent implements MatchEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private Competitor actor;
    private String description;

    // Pola specyficzne dla Football (z UML)
    private FootballEventType eventType;
    private int minute;

    /*
     * relatedEventId wskazuje na konkretne wcześniejsze zdarzenie.
     * Przykład:
     * - VAR_REVIEW może wskazywać na GOAL_SCORED
     * - GOAL_DISALLOWED może wskazywać na GOAL_SCORED
     * - PENALTY_DISALLOWED może wskazywać na SHOOTOUT_PENALTY_SCORED
     */
    private String relatedEventId;
}