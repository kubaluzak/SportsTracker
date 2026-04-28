package org.example.sportstracker.football.domain.stage;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.Match;
import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.core.domain.tournament.AbstractTournamentStage;
import org.example.sportstracker.football.domain.bracket.BracketTree;
import org.example.sportstracker.football.domain.match.FootballMatch;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class FootballKnockoutStage extends AbstractTournamentStage {
    private BracketTree brackets = new BracketTree();
    private boolean isTwoLegged;

    public void advanceWinner(FootballMatch match) {
        if (match.getStatus() != MatchStatus.COMPLETED && match.getStatus() != MatchStatus.WALKOVER) {
            throw new IllegalStateException("Nie można awansować zwycięzcy niezakończonego meczu");
        }

        Competitor winner = match.getResult().getWinner();

        if (winner == null) {
            throw new IllegalStateException("Mecz pucharowy musi mieć zwycięzcę");
        }

        advancingTeams.add(winner);
    }

    @Override
    public void startStage() {
        System.out.println("\n>>> START FAZY PUCHAROWEJ <<<");
    }

    @Override
    protected void verifyMatchesCompleted() {
        for (Match match : stageMatches) {
            if (match.getStatus() != MatchStatus.COMPLETED && match.getStatus() != MatchStatus.WALKOVER) {
                throw new IllegalStateException("Nie wszystkie mecze pucharowe zostały zakończone");
            }
        }
    }

    @Override
    protected void processResults() {
        advancingTeams.clear();

        if (brackets.getRounds().isEmpty()) {
            for (Match match : stageMatches) {
                advanceWinner((FootballMatch) match);
            }
        }
    }

    @Override
    protected void determineAdvancingTeams() {
        if (!brackets.getRounds().isEmpty()) {
            List<FootballMatch> finalRound = brackets.getRounds().get(brackets.getRounds().size() - 1);

            if (finalRound.size() == 1 && (finalRound.get(0).getStatus() == MatchStatus.COMPLETED
                    || finalRound.get(0).getStatus() == MatchStatus.WALKOVER)) {
                advancingTeams.clear();
                advancingTeams.add(finalRound.get(0).getResult().getWinner());
            }
        }
    }

    @Override
    protected void printStageSummary() {
        brackets.printConsoleVisualization();
    }
}