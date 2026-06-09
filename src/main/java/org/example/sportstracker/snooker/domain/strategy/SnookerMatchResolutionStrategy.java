package org.example.sportstracker.snooker.domain.strategy;

import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.MatchResolutionStrategy;
import org.example.sportstracker.core.domain.result.Result;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.snooker.domain.competitor.Player;
import org.example.sportstracker.snooker.domain.result.SnookerResult;
import org.example.sportstracker.snooker.domain.match.SnookerMatch.SnookerScore;

public class SnookerMatchResolutionStrategy implements MatchResolutionStrategy {
    private final int framesNeededToWin;

    public SnookerMatchResolutionStrategy(int bestOfTotalFrames) {
        this.framesNeededToWin = (bestOfTotalFrames / 2) + 1;
    }

    @Override
    public Result resolve(Score score, Competitor player1, Competitor player2) {
        SnookerScore ss = (SnookerScore) score;
        Player winner = null;

        if (ss.getPlayer1Frames() >= framesNeededToWin) {
            winner = (Player) player1;
        } else if (ss.getPlayer2Frames() >= framesNeededToWin) {
            winner = (Player) player2;
        } else {
            throw new IllegalStateException("Match cannot be resolved; neither player reached the required frames.");
        }

        return SnookerResult.builder()
                .winner(winner)
                .completedNormally(true)
                .build();
    }
}