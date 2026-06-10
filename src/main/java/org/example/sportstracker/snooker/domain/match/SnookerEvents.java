package org.example.sportstracker.snooker.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public final class SnookerEvents {

    private SnookerEvents() {
        // Klasa narzędziowa, nie tworzymy obiektów.
    }

    public static SnookerMatchEvent potRed(Competitor actor, int frameNumber, String description) {
        return new PotRedEvent(actor, frameNumber, description);
    }

    public static SnookerMatchEvent potColor(
            Competitor actor,
            int frameNumber,
            int pointsValue,
            String description
    ) {
        return new PotColorEvent(actor, frameNumber, pointsValue, description);
    }

    public static SnookerMatchEvent foul(
            Competitor actor,
            int frameNumber,
            int pointsValue,
            String description
    ) {
        return new FoulEvent(actor, frameNumber, pointsValue, description);
    }

    public static SnookerMatchEvent missCalled(Competitor actor, int frameNumber, String description) {
        return new MissCalledEvent(actor, frameNumber, description);
    }

    public static SnookerMatchEvent frameWon(Competitor actor, int frameNumber, String description) {
        return new FrameWonEvent(actor, frameNumber, description);
    }

    public static SnookerMatchEvent eventInvalidated(
            Competitor actor,
            int frameNumber,
            String description,
            List<String> relatedEventIds
    ) {
        return new SnookerEventInvalidatedEvent(actor, frameNumber, description, relatedEventIds);
    }
}