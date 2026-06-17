package org.example.sportstracker.football.domain.strategy;

import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.MatchResolutionStrategy;
import org.example.sportstracker.core.domain.result.Result;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.football.domain.competitor.Team;
import org.example.sportstracker.football.domain.result.FootballResult;
import org.example.sportstracker.football.domain.match.FootballMatch.FootballScore;

import java.util.List;

public class LeagueMatchResolutionStrategy implements MatchResolutionStrategy {
    @Override
    public Result resolve(Score score, List<Competitor> competitorList) {
        Competitor homeTeam = competitorList.get(0);
        Competitor awayTeam = competitorList.get(1);
        FootballScore fs = (FootballScore) score;
        boolean isDraw = fs.getHomeGoals() == fs.getAwayGoals();

        Team winner = isDraw ? null : (fs.getHomeGoals() > fs.getAwayGoals() ? (Team) homeTeam : (Team) awayTeam);

        return FootballResult.builder()
                .isDraw(isDraw)
                .winner(winner)
                .wonViaPenalties(false)
                .completedNormally(true)
                .walkover(false)
                .build();
    }
}