package org.example.sportstracker.snooker.domain.observer;

import org.example.sportstracker.core.domain.match.MatchEvent;
import org.example.sportstracker.core.domain.match.MatchEventListener;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.snooker.domain.match.SnookerMatchEvent;

public class SnookerConsoleMatchBroadcaster implements MatchEventListener {
    @Override
    public void onEventRecorded(MatchEvent event, Score currentScore) {
        if (event instanceof SnookerMatchEvent se) {
            String pointsInfo = se.getPointsValue() > 0 ? " (+" + se.getPointsValue() + " pts)" : "";
            System.out.println("[LIVE] " + se.getActor().getName() + " | "
                    + se.getEventType() + pointsInfo + " | "
                    + "Score: " + currentScore.display());
        }
    }
}