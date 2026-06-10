package org.example.sportstracker.snooker.domain.observer;

import org.example.sportstracker.core.domain.match.MatchEvent;
import org.example.sportstracker.core.domain.match.MatchEventListener;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.snooker.domain.match.SnookerMatchEvent;

public class SnookerConsoleMatchBroadcaster implements MatchEventListener {

    @Override
    public void onEventRecorded(MatchEvent event, Score currentScore) {
        if (!(event instanceof SnookerMatchEvent snookerEvent)) {
            return;
        }

        String pointsInfo = snookerEvent.getPointsValue() > 0
                ? " (+" + snookerEvent.getPointsValue() + " pts)"
                : "";

        System.out.println(
                "LIVE | Frame "
                        + snookerEvent.getFrameNumber()
                        + " | "
                        + snookerEvent.getActor().getName()
                        + " | "
                        + snookerEvent.getEventName()
                        + pointsInfo
                        + " | Score: "
                        + currentScore.display()
        );
    }
}