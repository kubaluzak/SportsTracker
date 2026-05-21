package org.example.sportstracker.football.domain.strategy;

import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.MatchResolutionStrategy;
import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.core.domain.result.Result;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.football.domain.competitor.Team;
import org.example.sportstracker.football.domain.match.FootballMatch;
import org.example.sportstracker.football.domain.result.FootballResult;
import org.example.sportstracker.football.domain.score.FootballScore;

public class KnockoutMatchResolutionStrategy implements MatchResolutionStrategy {

    @Override
    public Result resolve(Score score, Competitor homeTeam, Competitor awayTeam) {
        FootballScore fs = (FootballScore) score;

        Team winner;
        boolean wonViaPenalties = false;

        if (fs.getHomeGoals() > fs.getAwayGoals()) {
            winner = (Team) homeTeam;
        } else if (fs.getAwayGoals() > fs.getHomeGoals()) {
            winner = (Team) awayTeam;
        } else if (fs.getHomePenalties() > fs.getAwayPenalties()) {
            winner = (Team) homeTeam;
            wonViaPenalties = true;
        } else if (fs.getAwayPenalties() > fs.getHomePenalties()) {
            winner = (Team) awayTeam;
            wonViaPenalties = true;
        } else {
            throw new IllegalStateException("Mecz pucharowy musi mieć zwycięzcę po dogrywce lub karnych");
        }

        return FootballResult.builder()
                .isDraw(false)
                .winner(winner)
                .wonViaPenalties(wonViaPenalties)
                .completedNormally(true)
                .walkover(false)
                .build();
    }

    public Team resolveTwoLeggedWinner(FootballMatch firstLeg, FootballMatch secondLeg) {
        validateTwoLeggedPair(firstLeg, secondLeg);

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
}