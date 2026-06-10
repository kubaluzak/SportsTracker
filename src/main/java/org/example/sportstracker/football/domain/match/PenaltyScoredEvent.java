package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class PenaltyScoredEvent extends FootballMatchEvent {

    public PenaltyScoredEvent(Competitor actor, int minute, String description) {
        super(null, null, actor, description, minute, List.of());
    }

    @Override
    public void applyTo(FootballMatch.FootballScore score) {
        // Karny w czasie meczu liczy się jako gol.
        score.addGoalFor(getActor());
    }

    @Override
    public void undoFrom(FootballMatch.FootballScore score) {
        // Cofa gola z karnego.
        score.removeGoalFor(getActor());
    }

    @Override
    public boolean canBeUndone() {
        return true;
    }
}