package org.example.sportstracker.snooker.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class PotColorEvent extends SnookerMatchEvent {

    public PotColorEvent(
            Competitor actor,
            int frameNumber,
            int pointsValue,
            String description
    ) {
        super(null, null, actor, description, frameNumber, pointsValue, List.of());
    }

    @Override
    public SnookerEventTypeId getEventTypeId() {
        return SnookerEventTypeId.POT_COLOR;
    }

    @Override
    public void applyTo(SnookerMatch.SnookerScore score) {
        score.addPointsFor(getActor(), getPointsValue());
    }

    @Override
    public void undoFrom(SnookerMatch.SnookerScore score) {
        score.removePointsFor(getActor(), getPointsValue());
    }

    @Override
    public boolean canBeUndone() {
        return true;
    }
}