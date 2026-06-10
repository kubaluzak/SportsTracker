package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class ShootoutPenaltyScoredEvent extends FootballMatchEvent {

    public ShootoutPenaltyScoredEvent(Competitor actor, int minute, String description) {
        super(null, null, actor, description, minute, List.of());
    }

    @Override
    public void applyTo(FootballMatch.FootballScore score) {
        // Dodaje trafiony karny w serii.
        score.addShootoutPenaltyFor(getActor());
    }

    @Override
    public void undoFrom(FootballMatch.FootballScore score) {
        // Cofa trafiony karny w serii.
        score.removeShootoutPenaltyFor(getActor());
    }

    @Override
    public boolean canBeUndone() {
        return true;
    }
}