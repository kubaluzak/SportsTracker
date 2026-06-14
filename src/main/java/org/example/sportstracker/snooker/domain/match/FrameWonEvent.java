package org.example.sportstracker.snooker.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class FrameWonEvent extends SnookerMatchEvent {

    public FrameWonEvent(Competitor actor, int frameNumber, String description) {
        super(null, null, actor, description, frameNumber, 0, List.of());
    }

    @Override
    public SnookerEventTypeId getEventTypeId() {
        return SnookerEventTypeId.FRAME_WON;
    }

    @Override
    public void applyTo(SnookerMatch.SnookerScore score) {
        score.awardFrameTo(getActor());
    }

    @Override
    public void undoFrom(SnookerMatch.SnookerScore score) {
        score.removeFrameFor(getActor());
    }

    @Override
    public boolean canBeUndone() {
        return true;
    }
}