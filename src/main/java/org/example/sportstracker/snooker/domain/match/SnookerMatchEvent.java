package org.example.sportstracker.snooker.domain.match;

import lombok.Getter;
import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.MatchEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
public abstract class SnookerMatchEvent implements MatchEvent {

    private final String eventId;
    private final LocalDateTime timestamp;
    private final Competitor actor;
    private final String description;
    private final int frameNumber;
    private final int pointsValue;
    private final List<String> relatedEventIds;

    protected SnookerMatchEvent(
            String eventId,
            LocalDateTime timestamp,
            Competitor actor,
            String description,
            int frameNumber,
            int pointsValue,
            List<String> relatedEventIds
    ) {
        this.eventId = eventId == null ? UUID.randomUUID().toString() : eventId;
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        this.actor = actor;
        this.description = description;
        this.frameNumber = frameNumber;
        this.pointsValue = pointsValue;
        this.relatedEventIds = relatedEventIds == null ? List.of() : List.copyOf(relatedEventIds);
    }

    public abstract void applyTo(SnookerMatch.SnookerScore score);

    public void undoFrom(SnookerMatch.SnookerScore score) {
        // Domyślnie event nie ma efektu do cofnięcia.
    }

    public boolean canBeUndone() {
        // Domyślnie event nie jest odwracalny.
        return false;
    }

    public boolean requiresRelatedEvents() {
        // Domyślnie event nie wymaga powiązanych eventów.
        return false;
    }

    public boolean canReferTo(List<SnookerMatchEvent> relatedEvents) {
        // Konkretne eventy mogą zawęzić typy powiązań.
        return true;
    }

    public String getEventName() {
        // Nazwa klasy pełni rolę typu eventu.
        return getClass().getSimpleName();
    }
}