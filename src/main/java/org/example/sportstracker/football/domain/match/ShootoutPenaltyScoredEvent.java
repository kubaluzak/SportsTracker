package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class ShootoutPenaltyScoredEvent extends FootballMatchEvent {

    public ShootoutPenaltyScoredEvent(Competitor actor, int minute, String description) {
        super(null, null, actor, description, minute, List.of());
    }

    @Override
    public FootballEventTypeId getEventTypeId() {
        return FootballEventTypeId.SHOOTOUT_PENALTY_SCORED;
    }

    @Override
    public void applyTo(FootballMatch.FootballScore score) {
        score.addShootoutPenaltyFor(getActor());
    }

    @Override
    public void undoFrom(FootballMatch.FootballScore score) {
        score.removeShootoutPenaltyFor(getActor());
    }

    @Override
    public boolean canBeUndone() {
        return true;
    }
}