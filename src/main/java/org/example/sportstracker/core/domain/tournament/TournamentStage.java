package org.example.sportstracker.core.domain.tournament;

import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.Match;

import java.util.List;

public interface TournamentStage {
    void startStage();
    void endStage();
    List<Competitor> getAdvancingCompetitors();
    void addMatch(Match match);
}