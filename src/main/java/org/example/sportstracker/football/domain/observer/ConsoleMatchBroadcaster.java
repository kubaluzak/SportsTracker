package org.example.sportstracker.football.domain.observer;

import org.example.sportstracker.core.domain.match.MatchEvent;
import org.example.sportstracker.core.domain.match.MatchEventListener;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.football.domain.match.FootballMatchEvent;

public class ConsoleMatchBroadcaster implements MatchEventListener {
    @Override
    public void onEventRecorded(MatchEvent event, Score currentScore) {
        FootballMatchEvent fbEvent = (FootballMatchEvent) event;
        System.out.println("📺 [LIVE] " + fbEvent.getMinute() + "' | " 
                + fbEvent.getDescription() + " | Wynik: " + currentScore.display());
    }
}