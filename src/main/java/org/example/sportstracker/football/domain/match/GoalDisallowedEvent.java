package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class GoalDisallowedEvent extends FootballMatchEvent {

    public GoalDisallowedEvent(
            Competitor actor,
            int minute,
            String description,
            List<String> relatedEventIds
    ) {
        super(null, null, actor, description, minute, relatedEventIds);
    }

    @Override
    public void applyTo(FootballMatch.FootballScore score) {
        // Cofa wskazane gole.
        score.undoEvents(getRelatedEventIds(), this);
    }

    @Override
    public boolean requiresRelatedEvents() {
        return true;
    }

    @Override
    public boolean canReferTo(List<FootballMatchEvent> relatedEvents) {
        return relatedEvents.stream().allMatch(event ->
                event instanceof GoalScoredEvent
                        || event instanceof PenaltyScoredEvent
        );
    }
}