package org.example.sportstracker.snooker.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class MissCalledEvent extends SnookerMatchEvent {

    public MissCalledEvent(Competitor actor, int frameNumber, String description) {
        super(null, null, actor, description, frameNumber, 0, List.of());
    }

    @Override
    public SnookerEventTypeId getEventTypeId() {
        return SnookerEventTypeId.MISS_CALLED;
    }

    @Override
    public void applyTo(SnookerMatch.SnookerScore score) {
        score.resetBreak();
    }
}