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
import org.example.sportstracker.football.domain.score.FootballScore;

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

        validateTwoLeggedPair(firstLeg, secondLeg);

        Team winner = resolveTwoLeggedWinner(firstLeg, secondLeg);
        advancingTeams.add(winner);
    }

    private void validateTwoLeggedPair(FootballMatch firstLeg, FootballMatch secondLeg) {
        boolean firstCompleted = firstLeg.getStatus() == MatchStatus.COMPLETED
                || firstLeg.getStatus() == MatchStatus.WALKOVER;

        boolean secondCompleted = secondLeg.getStatus() == MatchStatus.COMPLETED
                || secondLeg.getStatus() == MatchStatus.WALKOVER;

        if (!firstCompleted || !secondCompleted) {
            throw new IllegalStateException("Oba mecze dwumeczu muszą być zakończone");
        }

        boolean samePairReversed = firstLeg.getHomeTeam().equals(secondLeg.getAwayTeam())
                && firstLeg.getAwayTeam().equals(secondLeg.getHomeTeam());

        if (!samePairReversed) {
            throw new IllegalStateException("Dwumecz musi składać się z tych samych drużyn w odwróconej kolejności");
        }
    }

    private Team resolveTwoLeggedWinner(FootballMatch firstLeg, FootballMatch secondLeg) {
        FootballScore firstScore = firstLeg.getScore();
        FootballScore secondScore = secondLeg.getScore();

        Team teamA = firstLeg.getHomeTeam();
        Team teamB = firstLeg.getAwayTeam();

        int teamAGoals = firstScore.getHomeGoals() + secondScore.getAwayGoals();
        int teamBGoals = firstScore.getAwayGoals() + secondScore.getHomeGoals();

        if (teamAGoals > teamBGoals) {
            return teamA;
        }

        if (teamBGoals > teamAGoals) {
            return teamB;
        }

        int teamAPenalties = secondLeg.getAwayTeam().equals(teamA)
                ? secondScore.getAwayPenalties()
                : secondScore.getHomePenalties();

        int teamBPenalties = secondLeg.getHomeTeam().equals(teamB)
                ? secondScore.getHomePenalties()
                : secondScore.getAwayPenalties();

        if (teamAPenalties > teamBPenalties) {
            return teamA;
        }

        if (teamBPenalties > teamAPenalties) {
            return teamB;
        }

        throw new IllegalStateException("Dwumecz musi mieć zwycięzcę po agregacie lub karnych");
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