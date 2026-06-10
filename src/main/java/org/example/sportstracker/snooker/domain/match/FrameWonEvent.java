package org.example.sportstracker.snooker.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class FrameWonEvent extends SnookerMatchEvent {

    public FrameWonEvent(Competitor actor, int frameNumber, String description) {
        super(null, null, actor, description, frameNumber, 0, List.of());
    }

    @Override
    public void applyTo(SnookerMatch.SnookerScore score) {
        // Dodaje frame zwycięzcy i zeruje punkty aktualnego frame'a.
        score.awardFrameTo(getActor());
    }

    @Override
    public void undoFrom(SnookerMatch.SnookerScore score) {
        // Cofa wygranego frame'a.
        score.removeFrameFor(getActor());
    }

    @Override
    public boolean canBeUndone() {
        return true;
    }
}