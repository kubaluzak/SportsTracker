package org.example.sportstracker.football.domain.stage;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.Match;
import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.core.domain.tournament.AbstractTournamentStage;
import org.example.sportstracker.football.domain.bracket.BracketTree;
import org.example.sportstracker.football.domain.competitor.Team;
import org.example.sportstracker.football.domain.match.FootballMatch;
import org.example.sportstracker.football.domain.strategy.KnockoutMatchResolutionStrategy;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class FootballKnockoutStage extends AbstractTournamentStage {
    private BracketTree brackets = new BracketTree();

    /*
     * Jeśli false, każdy mecz pucharowy daje jednego zwycięzcę.
     * Jeśli true, mecze są analizowane parami jako dwumecz.
     */
    private boolean isTwoLegged;

    private KnockoutMatchResolutionStrategy knockoutResolutionStrategy =
            new KnockoutMatchResolutionStrategy();

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

    public void advanceTwoLeggedWinner(FootballMatch firstLeg, FootballMatch secondLeg) {
        if (!isTwoLegged) {
            throw new IllegalStateException("Faza nie jest skonfigurowana jako dwumecz");
        }

        Team winner = knockoutResolutionStrategy.resolveTwoLeggedWinner(firstLeg, secondLeg);
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

        if (isTwoLegged && brackets.getRounds().isEmpty() && stageMatches.size() % 2 != 0) {
            throw new IllegalStateException("Faza dwumeczowa musi mieć parzystą liczbę meczów");
        }
    }

    @Override
    protected void processResults() {
        advancingTeams.clear();

        if (!brackets.getRounds().isEmpty()) {
            return;
        }

        if (isTwoLegged) {
            for (int i = 0; i < stageMatches.size(); i += 2) {
                FootballMatch firstLeg = (FootballMatch) stageMatches.get(i);
                FootballMatch secondLeg = (FootballMatch) stageMatches.get(i + 1);

                advanceTwoLeggedWinner(firstLeg, secondLeg);
            }
        } else {
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
        if (!brackets.getRounds().isEmpty()) {
            brackets.printConsoleVisualization();
        }

        if (isTwoLegged) {
            System.out.println("\n>>> KONIEC FAZY DWUMECZOWEJ <<<");
            for (Competitor team : advancingTeams) {
                System.out.println("Awansuje: " + team.getName());
            }
        }
    }
}