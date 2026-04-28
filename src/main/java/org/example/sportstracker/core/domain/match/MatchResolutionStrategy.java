package org.example.sportstracker.core.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.result.Result;
import org.example.sportstracker.core.domain.score.Score;

public interface MatchResolutionStrategy {
    Result resolve(Score score, Competitor homeTeam, Competitor awayTeam);
}