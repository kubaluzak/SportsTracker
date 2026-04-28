package org.example.sportstracker.core.domain.tournament;

import org.example.sportstracker.core.domain.competitor.Competitor;

public interface Tournament {
    void startTournament();
    void endTournament();
    Competitor getOverallWinner();
}