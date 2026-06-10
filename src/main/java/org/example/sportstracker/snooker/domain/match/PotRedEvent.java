package org.example.sportstracker.snooker.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class PotRedEvent extends SnookerMatchEvent {

    public PotRedEvent(Competitor actor, int frameNumber, String description) {
        super(null, null, actor, description, frameNumber, 1, List.of());
    }

    @Override
    public void applyTo(SnookerMatch.SnookerScore score) {
        // Dodaje 1 punkt za czerwoną.
        score.addPointsFor(getActor(), getPointsValue());
    }

    @Override
    public void undoFrom(SnookerMatch.SnookerScore score) {
        // Cofa punkty za czerwoną.
        score.removePointsFor(getActor(), getPointsValue());
    }

    @Override
    public boolean canBeUndone() {
        return true;
    }
}