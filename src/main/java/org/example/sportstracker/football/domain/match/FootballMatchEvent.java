package org.example.sportstracker.football.domain.match;

import lombok.Getter;
import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.MatchEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
public abstract class FootballMatchEvent implements MatchEvent {

    private final String eventId;
    private final LocalDateTime timestamp;
    private final Competitor actor;
    private final String description;
    private final int minute;
    private final List<String> relatedEventIds;

    protected FootballMatchEvent(
            String eventId,
            LocalDateTime timestamp,
            Competitor actor,
            String description,
            int minute,
            List<String> relatedEventIds
    ) {
        this.eventId = eventId == null ? UUID.randomUUID().toString() : eventId;
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        this.actor = actor;
        this.description = description;
        this.minute = minute;
        this.relatedEventIds = relatedEventIds == null ? List.of() : List.copyOf(relatedEventIds);
    }

    public abstract FootballEventTypeId getEventTypeId();

    public abstract void applyTo(FootballMatch.FootballScore score);

    public void undoFrom(FootballMatch.FootballScore score) {
        // Domyślnie event nie ma efektu do cofnięcia.
    }

    public boolean canBeUndone() {
        // Domyślnie event nie jest odwracalny.
        return false;
    }

    public boolean requiresRelatedEvents() {
        // Domyślnie event nie musi odnosić się do innych eventów.
        return false;
    }

    public boolean canReferTo(List<FootballMatchEvent> relatedEvents) {
        // Domyślnie pozwalamy, konkretne eventy mogą to zawęzić.
        return true;
    }

    public String getEventName() {
        return getEventTypeId().name();
    }
}