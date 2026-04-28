package org.example.sportstracker.core.domain.tournament;

import lombok.Getter;
import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.Match;

import java.util.ArrayList;
import java.util.List;
@Getter
public abstract class AbstractTournamentStage implements TournamentStage {
    protected List<Match> stageMatches = new ArrayList<>();
    protected List<Competitor> advancingTeams = new ArrayList<>();

    @Override
    public void addMatch(Match match) {
        stageMatches.add(match);
    }

    @Override
    public List<Competitor> getAdvancingCompetitors() {
        return advancingTeams;
    }

    // WZORZEC: TEMPLATE METHOD (Metoda Szablonowa)
    @Override
    public final void endStage() {
        verifyMatchesCompleted();
        processResults();
        determineAdvancingTeams();
        printStageSummary();
    }

    protected abstract void verifyMatchesCompleted();
    protected abstract void processResults();
    protected abstract void determineAdvancingTeams();
    protected abstract void printStageSummary();
}