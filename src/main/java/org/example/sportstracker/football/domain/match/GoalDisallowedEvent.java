package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class GoalDisallowedEvent extends FootballMatchEvent {

    private static final Set<FootballEventTypeId> ALLOWED_RELATED_EVENT_TYPES = EnumSet.of(
            FootballEventTypeId.GOAL_SCORED,
            FootballEventTypeId.PENALTY_SCORED
    );

    public GoalDisallowedEvent(
            Competitor actor,
            int minute,
            String description,
            List<String> relatedEventIds
    ) {
        super(null, null, actor, description, minute, relatedEventIds);
    }

    @Override
    public FootballEventTypeId getEventTypeId() {
        return FootballEventTypeId.GOAL_DISALLOWED;
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
        return relatedEvents.stream()
                .map(FootballMatchEvent::getEventTypeId)
                .allMatch(ALLOWED_RELATED_EVENT_TYPES::contains);
    }
}