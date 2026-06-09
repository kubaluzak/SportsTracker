package org.example.sportstracker.snooker.domain.match;

import lombok.Data;
import lombok.Getter;
import org.example.sportstracker.core.domain.match.*;
import org.example.sportstracker.core.domain.result.Result;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.snooker.domain.competitor.Player;
import org.example.sportstracker.snooker.domain.result.SnookerResult;

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
            score.applyWalkover(framesWinner, framesLoser);
        } else {
            score.applyWalkover(framesLoser, framesWinner);
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

    @Getter
    public static class SnookerScore implements Score {
        private int player1Frames = 0;
        private int player2Frames = 0;
        private int p1CurrentFramePoints = 0;
        private int p2CurrentFramePoints = 0;

        private int highestBreak = 0;
        private Player highestBreakPlayer = null;

        private int currentBreak = 0;
        private Player currentBreaker = null;

        private final Player player1;
        private final Player player2;

        public SnookerScore(Player player1, Player player2) {
            this.player1 = player1;
            this.player2 = player2;
        }

        private void applyWalkover(int p1Frames, int p2Frames) {
            this.player1Frames = p1Frames;
            this.player2Frames = p2Frames;
        }

        void setFramesForTesting(int p1Frames, int p2Frames) {
            this.player1Frames = p1Frames;
            this.player2Frames = p2Frames;
        }

        @Override
        public String display() {
            return String.format("%s %d(%d) - %d(%d) %s",
                    player1.getName(), player1Frames, p1CurrentFramePoints,
                    player2Frames, p2CurrentFramePoints, player2.getName());
        }

        @Override
        public void update(MatchEvent event) {
            if (!(event instanceof SnookerMatchEvent snookerEvent)) return;

            Player actor = (Player) snookerEvent.getActor();
            boolean isPlayer1 = actor.equals(player1);

            switch (snookerEvent.getEventType()) {
                case POT_RED:
                case POT_COLOR:
                    if (isPlayer1) p1CurrentFramePoints += snookerEvent.getPointsValue();
                    else p2CurrentFramePoints += snookerEvent.getPointsValue();

                    trackBreak(actor, snookerEvent.getPointsValue());
                    break;
                case FOUL:
                    if (isPlayer1) p2CurrentFramePoints += snookerEvent.getPointsValue();
                    else p1CurrentFramePoints += snookerEvent.getPointsValue();
                    resetBreak();
                    break;
                case MISS_CALLED:
                    resetBreak();
                    break;
                case FRAME_WON:
                    if (isPlayer1 && p1CurrentFramePoints >= p2CurrentFramePoints) {
                        player1Frames++;
                    } else if (!isPlayer1 && p2CurrentFramePoints >= p1CurrentFramePoints) {
                        player2Frames++;
                    } else {
                        if (isPlayer1) player1Frames++;
                        else player2Frames++;
                    }
                    resetFrame();
                    break;
                case EVENT_INVALIDATED:
                    if (isPlayer1) {
                        p1CurrentFramePoints = Math.max(0, p1CurrentFramePoints - snookerEvent.getPointsValue());
                    } else {
                        p2CurrentFramePoints = Math.max(0, p2CurrentFramePoints - snookerEvent.getPointsValue());
                    }
                    resetBreak();
                    break;
            }
        }

        private void trackBreak(Player actor, int points) {
            if (actor.equals(currentBreaker)) {
                currentBreak += points;
            } else {
                currentBreaker = actor;
                currentBreak = points;
            }

            if (currentBreak > highestBreak) {
                highestBreak = currentBreak;
                highestBreakPlayer = currentBreaker;
            }
        }

        private void resetBreak() {
            currentBreak = 0;
            currentBreaker = null;
        }

        private void resetFrame() {
            p1CurrentFramePoints = 0;
            p2CurrentFramePoints = 0;
            resetBreak();
        }
    }
}