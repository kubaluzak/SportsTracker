package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class VarReviewEvent extends FootballMatchEvent {

    public VarReviewEvent(
            Competitor actor,
            int minute,
            String description,
            List<String> relatedEventIds
    ) {
        super(null, null, actor, description, minute, relatedEventIds);
    }

    @Override
    public void applyTo(FootballMatch.FootballScore score) {
        // VAR sam nie zmienia wyniku.
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
                        || event instanceof ShootoutPenaltyScoredEvent
                        || event instanceof FoulEvent
                        || event instanceof YellowCardEvent
                        || event instanceof RedCardEvent
        );
    }
}