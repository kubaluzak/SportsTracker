package org.example.sportstracker.snooker.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class FoulEvent extends SnookerMatchEvent {

    public FoulEvent(Competitor actor, int frameNumber, int pointsValue, String description) {
        super(null, null, actor, description, frameNumber, pointsValue, List.of());

        if (pointsValue < 4 || pointsValue > 7) {
            throw new IllegalArgumentException("Foul points must be between 4 and 7");
        }
    }

    @Override
    public void applyTo(SnookerMatch.SnookerScore score) {
        // Faul daje punkty przeciwnikowi.
        score.addFoulPointsAgainst(getActor(), getPointsValue());
    }

    @Override
    public void undoFrom(SnookerMatch.SnookerScore score) {
        // Cofa punkty przyznane przeciwnikowi za faul.
        score.removeFoulPointsAgainst(getActor(), getPointsValue());
    }

    @Override
    public boolean canBeUndone() {
        return true;
    }
}