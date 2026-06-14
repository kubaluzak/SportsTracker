package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class EventInvalidatedEvent extends FootballMatchEvent {

    public EventInvalidatedEvent(
            Competitor actor,
            int minute,
            String description,
            List<String> relatedEventIds
    ) {
        super(null, null, actor, description, minute, relatedEventIds);
    }

    @Override
    public FootballEventTypeId getEventTypeId() {
        return FootballEventTypeId.EVENT_INVALIDATED;
    }

    @Override
    public void applyTo(FootballMatch.FootballScore score) {
        score.undoEvents(getRelatedEventIds(), this);
    }

    @Override
    public boolean requiresRelatedEvents() {
        return true;
    }

    @Override
    public boolean canReferTo(List<FootballMatchEvent> relatedEvents) {
        return relatedEvents.stream().allMatch(FootballMatchEvent::canBeUndone);
    }
}