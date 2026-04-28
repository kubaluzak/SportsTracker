package org.example.sportstracker.football.domain.strategy;

import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.MatchResolutionStrategy;
import org.example.sportstracker.core.domain.result.Result;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.football.domain.competitor.Team;
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
            winner = (Team) awayTeam;
            wonViaPenalties = true;
        }

        return FootballResult.builder()
                .isDraw(false)
                .winner(winner)
                .wonViaPenalties(wonViaPenalties)
                .completedNormally(true)
                .walkover(false)
                .build();
    }
}