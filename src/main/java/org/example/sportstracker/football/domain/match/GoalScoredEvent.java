package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class GoalScoredEvent extends FootballMatchEvent {

    public GoalScoredEvent(Competitor actor, int minute, String description) {
        super(null, null, actor, description, minute, List.of());
    }

    @Override
    public void applyTo(FootballMatch.FootballScore score) {
        // Dodaje gola dla drużyny actor.
        score.addGoalFor(getActor());
    }

    @Override
    public void undoFrom(FootballMatch.FootballScore score) {
        // Cofa gola dla drużyny actor.
        score.removeGoalFor(getActor());
    }

    @Override
    public boolean canBeUndone() {
        return true;
    }
}