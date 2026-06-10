package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class YellowCardEvent extends FootballMatchEvent {

    public YellowCardEvent(Competitor actor, int minute, String description) {
        super(null, null, actor, description, minute, List.of());
    }

    @Override
    public void applyTo(FootballMatch.FootballScore score) {
        // Dodaje żółtą kartkę.
        score.addYellowCardFor(getActor());
    }

    @Override
    public void undoFrom(FootballMatch.FootballScore score) {
        // Cofa żółtą kartkę.
        score.removeYellowCardFor(getActor());
    }

    @Override
    public boolean canBeUndone() {
        return true;
    }
}