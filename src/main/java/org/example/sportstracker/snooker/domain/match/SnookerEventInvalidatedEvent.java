package org.example.sportstracker.snooker.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class SnookerEventInvalidatedEvent extends SnookerMatchEvent {

    public SnookerEventInvalidatedEvent(
            Competitor actor,
            int frameNumber,
            String description,
            List<String> relatedEventIds
    ) {
        super(null, null, actor, description, frameNumber, 0, relatedEventIds);
    }

    @Override
    public void applyTo(SnookerMatch.SnookerScore score) {
        // Cofa wskazane eventy.
        score.undoEvents(getRelatedEventIds(), this);
    }

    @Override
    public boolean requiresRelatedEvents() {
        return true;
    }

    @Override
    public boolean canReferTo(List<SnookerMatchEvent> relatedEvents) {
        return relatedEvents.stream().allMatch(SnookerMatchEvent::canBeUndone);
    }
}