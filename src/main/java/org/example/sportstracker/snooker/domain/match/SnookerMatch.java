package org.example.sportstracker.snooker.domain.match;

import lombok.Data;
import org.example.sportstracker.core.domain.match.*;
import org.example.sportstracker.core.domain.result.Result;
import org.example.sportstracker.snooker.domain.competitor.Player;
import org.example.sportstracker.snooker.domain.result.SnookerResult;
import org.example.sportstracker.snooker.domain.score.SnookerScore;

import java.util.ArrayList;
import java.util.List;

@Data
public class SnookerMatch implements Match {
    private MatchStatus status = MatchStatus.SCHEDULED;
    private SnookerScore score;
    private List<SnookerMatchEvent> events = new ArrayList<>();

    private Player player1;
    private Player player2;

    private int totalFrames;
    private int currentFrame = 1;

    private MatchResolutionStrategy resolutionStrategy;
    private List<MatchEventListener> listeners = new ArrayList<>();
    private String abandonReason;
    private SnookerResult manualResult;

    public SnookerMatch(Player p1, Player p2, int totalFrames, MatchResolutionStrategy strategy) {
        this.player1 = p1;
        this.player2 = p2;
        this.totalFrames = totalFrames;
        this.resolutionStrategy = strategy;
        this.score = new SnookerScore(p1, p2);
    }

    @Override
    public void addMatchEventListener(MatchEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void startMatch() {
        if (status != MatchStatus.SCHEDULED) throw new IllegalStateException("Match is not scheduled.");
        status = MatchStatus.IN_PROGRESS;
    }

    @Override
    public void endMatch() {
        if (status != MatchStatus.IN_PROGRESS) throw new IllegalStateException("Can only end an in-progress match.");
        status = MatchStatus.COMPLETED;
    }

    @Override
    public void abandonMatch(String reason) {
        if (status != MatchStatus.IN_PROGRESS) throw new IllegalStateException("Can only abandon an in-progress match.");
        this.abandonReason = reason;
        status = MatchStatus.ABANDONED;
    }

    public void startNextFrame() {
        if (currentFrame >= totalFrames) {
            throw new IllegalStateException("All frames have been played.");
        }

        int framesNeededToWin = (totalFrames / 2) + 1;
        if (score.getPlayer1Frames() >= framesNeededToWin || score.getPlayer2Frames() >= framesNeededToWin) {
            throw new IllegalStateException("Cannot start next frame; the match is already won.");
        }

        int completedFrames = score.getPlayer1Frames() + score.getPlayer2Frames();
        if (completedFrames < currentFrame) {
            throw new IllegalStateException("Cannot start next frame; the current frame is not finished yet.");
        }

        currentFrame++;
    }

    @Override
    public void recordEvent(MatchEvent event) {
        if (status != MatchStatus.IN_PROGRESS) throw new IllegalStateException("Match is not in progress.");
        if (!(event instanceof SnookerMatchEvent se)) throw new IllegalArgumentException("Invalid event type for Snooker.");

        events.add(se);
        score.update(se);

        for (MatchEventListener listener : listeners) {
            listener.onEventRecorded(se, score);
        }
    }

    @Override
    public SnookerScore getScore() { return score; }

    public void assignWalkover(Player winner, int framesWinner, int framesLoser) {
        if (!winner.equals(player1) && !winner.equals(player2)) {
            throw new IllegalArgumentException("Walkover winner must be a participant in the match");
        }

        Player loser = winner.equals(player1) ? player2 : player1;

        if (winner.equals(player1)) {
            score.setPlayer1Frames(framesWinner);
            score.setPlayer2Frames(framesLoser);
        } else {
            score.setPlayer2Frames(framesWinner);
            score.setPlayer1Frames(framesLoser);
        }

        manualResult = SnookerResult.builder()
                .winner(winner)
                .completedNormally(false)
                .walkover(true)
                .walkoverLoser(loser)
                .build();

        status = MatchStatus.WALKOVER;
    }

    @Override
    public Result getResult() {
        if (manualResult != null) {
            return manualResult;
        }
        return resolutionStrategy.resolve(score, player1, player2);
    }

    @Override
    public MatchStatus getStatus() { return status; }
}