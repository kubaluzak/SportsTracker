package org.example.sportstracker.snooker.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class PotColorEvent extends SnookerMatchEvent {

    public PotColorEvent(Competitor actor, int frameNumber, int pointsValue, String description) {
        super(null, null, actor, description, frameNumber, pointsValue, List.of());

        if (pointsValue < 2 || pointsValue > 7) {
            throw new IllegalArgumentException("Color ball points must be between 2 and 7");
        }
    }

    @Override
    public void applyTo(SnookerMatch.SnookerScore score) {
        // Dodaje punkty za kolor.
        score.addPointsFor(getActor(), getPointsValue());
    }

    @Override
    public void undoFrom(SnookerMatch.SnookerScore score) {
        // Cofa punkty za kolor.
        score.removePointsFor(getActor(), getPointsValue());
    }

    @Override
    public boolean canBeUndone() {
        return true;
    }
}