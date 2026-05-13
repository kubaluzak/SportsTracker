package org.example.sportstracker.snooker.domain.score;

import lombok.Data;
import org.example.sportstracker.core.domain.match.MatchEvent;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.snooker.domain.competitor.Player;
import org.example.sportstracker.snooker.domain.match.SnookerEventType;
import org.example.sportstracker.snooker.domain.match.SnookerMatchEvent;

@Data
public class SnookerScore implements Score {
    private int player1Frames = 0;
    private int player2Frames = 0;
    private int p1CurrentFramePoints = 0;
    private int p2CurrentFramePoints = 0;

    private int highestBreak = 0;
    private Player highestBreakPlayer = null;

    // Internal break tracking
    private int currentBreak = 0;
    private Player currentBreaker = null;

    private Player player1;
    private Player player2;

    public SnookerScore(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
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
                // Fouls award points to the opponent in Snooker
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
                    // Fallback in case of a legitimate concession where the trailing player concedes
                    if (isPlayer1) player1Frames++;
                    else player2Frames++;
                }
                resetFrame();
                break;
            case EVENT_INVALIDATED:
                // Deduct the points that were incorrectly awarded.
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