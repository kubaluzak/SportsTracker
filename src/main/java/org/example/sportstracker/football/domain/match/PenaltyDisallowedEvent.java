package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class PenaltyDisallowedEvent extends FootballMatchEvent {

    public PenaltyDisallowedEvent(
            Competitor actor,
            int minute,
            String description,
            List<String> relatedEventIds
    ) {
        super(null, null, actor, description, minute, relatedEventIds);
    }

    @Override
    public void applyTo(FootballMatch.FootballScore score) {
        // Cofa trafione karne w serii.
        score.undoEvents(getRelatedEventIds(), this);
    }

    @Override
    public boolean requiresRelatedEvents() {
        return true;
    }

    @Override
    public boolean canReferTo(List<FootballMatchEvent> relatedEvents) {
        return relatedEvents.stream().allMatch(event ->
                event instanceof ShootoutPenaltyScoredEvent
        );
    }
}