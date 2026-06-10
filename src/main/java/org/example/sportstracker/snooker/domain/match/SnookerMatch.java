package org.example.sportstracker.snooker.domain.match;

import lombok.Data;
import lombok.Getter;
import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.Match;
import org.example.sportstracker.core.domain.match.MatchEvent;
import org.example.sportstracker.core.domain.match.MatchEventListener;
import org.example.sportstracker.core.domain.match.MatchResolutionStrategy;
import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.core.domain.result.Result;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.snooker.domain.competitor.Player;
import org.example.sportstracker.snooker.domain.result.SnookerResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public SnookerMatch(Player player1, Player player2, int totalFrames, MatchResolutionStrategy resolutionStrategy) {
        this.player1 = player1;
        this.player2 = player2;
        this.totalFrames = totalFrames;
        this.resolutionStrategy = resolutionStrategy;
        this.score = new SnookerScore(player1, player2);
    }

    @Override
    public void addMatchEventListener(MatchEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void startMatch() {
        if (status != MatchStatus.SCHEDULED) {
            throw new IllegalStateException("Match is not scheduled");
        }

        status = MatchStatus.IN_PROGRESS;
    }

    @Override
    public void endMatch() {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only an in-progress match can be completed");
        }

        status = MatchStatus.COMPLETED;
    }

    @Override
    public void abandonMatch(String reason) {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only an in-progress match can be abandoned");
        }

        abandonReason = reason;
        status = MatchStatus.ABANDONED;
    }

    public void startNextFrame() {
        if (currentFrame >= totalFrames) {
            throw new IllegalStateException("All frames have already been started");
        }

        int framesNeededToWin = (totalFrames / 2) + 1;

        if (score.getPlayer1Frames() >= framesNeededToWin || score.getPlayer2Frames() >= framesNeededToWin) {
            throw new IllegalStateException("Cannot start next frame; match is already won");
        }

        int completedFrames = score.getPlayer1Frames() + score.getPlayer2Frames();

        if (completedFrames < currentFrame) {
            throw new IllegalStateException("Cannot start next frame; current frame is not finished yet");
        }

        currentFrame++;
    }

    @Override
    public void recordEvent(MatchEvent event) {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Events can only be recorded during an in-progress match");
        }

        if (!(event instanceof SnookerMatchEvent snookerEvent)) {
            throw new IllegalArgumentException("Invalid event type for a snooker match");
        }

        validateEventId(snookerEvent);
        validateRelatedEvents(snookerEvent);

        events.add(snookerEvent);
        score.update(snookerEvent);

        for (MatchEventListener listener : listeners) {
            listener.onEventRecorded(snookerEvent, score);
        }
    }

    private void validateEventId(SnookerMatchEvent event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            throw new IllegalArgumentException("Event must have an eventId");
        }

        boolean eventIdAlreadyExists = events.stream()
                .anyMatch(previousEvent -> previousEvent.getEventId().equals(event.getEventId()));

        if (eventIdAlreadyExists) {
            throw new IllegalArgumentException("Event with this eventId already exists in the match");
        }
    }

    private void validateRelatedEvents(SnookerMatchEvent event) {
        if (!event.requiresRelatedEvents()) {
            return;
        }

        if (event.getRelatedEventIds() == null || event.getRelatedEventIds().isEmpty()) {
            throw new IllegalArgumentException("Event requires at least one relatedEventId");
        }

        List<SnookerMatchEvent> relatedEvents = new ArrayList<>();

        for (String relatedEventId : event.getRelatedEventIds()) {
            if (relatedEventId == null || relatedEventId.isBlank()) {
                throw new IllegalArgumentException("relatedEventId cannot be empty");
            }

            SnookerMatchEvent relatedEvent = events.stream()
                    .filter(previousEvent -> previousEvent.getEventId().equals(relatedEventId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "relatedEventId does not point to an existing snooker event: " + relatedEventId
                    ));

            relatedEvents.add(relatedEvent);
        }

        if (!event.canReferTo(relatedEvents)) {
            throw new IllegalArgumentException("Event cannot refer to the provided related events");
        }
    }

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

    public SnookerScore replayScore() {
        // Odtwarza wynik od zera na podstawie eventów.
        SnookerScore replayedScore = new SnookerScore(player1, player2);

        for (SnookerMatchEvent event : events) {
            replayedScore.update(event);
        }

        return replayedScore;
    }

    public List<SnookerScoreSnapshot> replayTimeline() {
        // Tworzy historię wyniku po każdym evencie.
        SnookerScore replayedScore = new SnookerScore(player1, player2);
        List<SnookerScoreSnapshot> timeline = new ArrayList<>();

        for (SnookerMatchEvent event : events) {
            replayedScore.update(event);

            timeline.add(new SnookerScoreSnapshot(
                    event.getFrameNumber(),
                    event.getEventId(),
                    event.getEventName(),
                    event.getDescription(),
                    event.getPointsValue(),
                    event.getRelatedEventIds(),
                    replayedScore.getPlayer1Frames(),
                    replayedScore.getPlayer2Frames(),
                    replayedScore.getPlayer1CurrentFramePoints(),
                    replayedScore.getPlayer2CurrentFramePoints(),
                    replayedScore.getHighestBreak(),
                    replayedScore.getHighestBreakPlayer() == null
                            ? null
                            : replayedScore.getHighestBreakPlayer().getName(),
                    replayedScore.display()
            ));
        }

        return timeline;
    }

    public void printTimeline() {
        // Wypisuje timeline meczu po angielsku.
        for (SnookerScoreSnapshot snapshot : replayTimeline()) {
            System.out.println(
                    "Frame "
                            + snapshot.frameNumber()
                            + " | "
                            + snapshot.eventName()
                            + " | "
                            + snapshot.description()
                            + " | Score: "
                            + snapshot.display()
                            + relatedInfo(snapshot.relatedEventIds())
            );
        }
    }

    private String relatedInfo(List<String> relatedEventIds) {
        if (relatedEventIds == null || relatedEventIds.isEmpty()) {
            return "";
        }

        return " | Related events: " + relatedEventIds;
    }

    @Override
    public SnookerScore getScore() {
        return score;
    }

    @Override
    public Result getResult() {
        if (manualResult != null) {
            return manualResult;
        }

        return resolutionStrategy.resolve(score, player1, player2);
    }

    @Override
    public MatchStatus getStatus() {
        return status;
    }

    @Getter
    public static class SnookerScore implements Score {
        private int player1Frames = 0;
        private int player2Frames = 0;

        private int player1CurrentFramePoints = 0;
        private int player2CurrentFramePoints = 0;

        private int highestBreak = 0;
        private Player highestBreakPlayer = null;

        private int currentBreak = 0;
        private Player currentBreaker = null;

        private final Player player1;
        private final Player player2;

        private final List<SnookerMatchEvent> eventHistory = new ArrayList<>();
        private final Map<String, SnookerMatchEvent> appliedEvents = new HashMap<>();
        private final Map<String, SnookerMatchEvent> undoingEvents = new HashMap<>();

        private boolean rebuilding = false;

        public SnookerScore(Player player1, Player player2) {
            this.player1 = player1;
            this.player2 = player2;
        }

        void applyWalkover(int player1Frames, int player2Frames) {
            // Walkower ustawia wynik ręcznie.
            this.player1Frames = player1Frames;
            this.player2Frames = player2Frames;
        }

        @Override
        public String display() {
            return String.format(
                    "%s %d(%d) - %d(%d) %s",
                    player1.getName(),
                    player1Frames,
                    player1CurrentFramePoints,
                    player2Frames,
                    player2CurrentFramePoints,
                    player2.getName()
            );
        }

        @Override
        public void update(MatchEvent event) {
            if (!(event instanceof SnookerMatchEvent snookerEvent)) {
                return;
            }

            applyEvent(snookerEvent);
        }

        void applyEvent(SnookerMatchEvent event) {
            // Event sam wie, jak zmienić score.
            if (!rebuilding) {
                eventHistory.add(event);
            }

            event.applyTo(this);

            if (event.canBeUndone() && !undoingEvents.containsKey(event.getEventId())) {
                appliedEvents.put(event.getEventId(), event);
            }
        }

        void undoEvent(String relatedEventId, SnookerMatchEvent undoingEvent) {
            // Cofamy konkretny wcześniejszy event.
            if (relatedEventId == null || relatedEventId.isBlank()) {
                return;
            }

            if (undoingEvents.containsKey(relatedEventId)) {
                return;
            }

            SnookerMatchEvent eventToUndo = appliedEvents.get(relatedEventId);

            if (eventToUndo == null) {
                return;
            }

            eventToUndo.undoFrom(this);
            undoingEvents.put(relatedEventId, undoingEvent);

            rebuildFromHistory();
        }

        void undoEvents(List<String> relatedEventIds, SnookerMatchEvent undoingEvent) {
            // Cofamy kilka eventów naraz.
            if (relatedEventIds == null) {
                return;
            }

            for (String relatedEventId : relatedEventIds) {
                undoEvent(relatedEventId, undoingEvent);
            }
        }

        private void rebuildFromHistory() {
            // Przelicza score od zera po cofnięciu eventów.
            List<SnookerMatchEvent> historyCopy = new ArrayList<>(eventHistory);

            resetScoreState();

            rebuilding = true;

            for (SnookerMatchEvent event : historyCopy) {
                if (undoingEvents.containsKey(event.getEventId())) {
                    continue;
                }

                if (event instanceof SnookerEventInvalidatedEvent) {
                    continue;
                }

                event.applyTo(this);

                if (event.canBeUndone()) {
                    appliedEvents.put(event.getEventId(), event);
                }
            }

            rebuilding = false;
        }

        private void resetScoreState() {
            // Czyści stan wyniku, ale nie czyści informacji o tym, co zostało cofnięte.
            player1Frames = 0;
            player2Frames = 0;

            player1CurrentFramePoints = 0;
            player2CurrentFramePoints = 0;

            highestBreak = 0;
            highestBreakPlayer = null;

            currentBreak = 0;
            currentBreaker = null;

            appliedEvents.clear();
        }

        void addPointsFor(Competitor actor, int points) {
            Player player = resolvePlayer(actor);

            if (player.equals(player1)) {
                player1CurrentFramePoints += points;
            } else {
                player2CurrentFramePoints += points;
            }

            trackBreak(player, points);
        }

        void removePointsFor(Competitor actor, int points) {
            Player player = resolvePlayer(actor);

            if (player.equals(player1)) {
                player1CurrentFramePoints = Math.max(0, player1CurrentFramePoints - points);
            } else {
                player2CurrentFramePoints = Math.max(0, player2CurrentFramePoints - points);
            }

            resetBreak();
        }

        void addFoulPointsAgainst(Competitor actor, int points) {
            Player foulingPlayer = resolvePlayer(actor);

            if (foulingPlayer.equals(player1)) {
                player2CurrentFramePoints += points;
            } else {
                player1CurrentFramePoints += points;
            }

            resetBreak();
        }

        void removeFoulPointsAgainst(Competitor actor, int points) {
            Player foulingPlayer = resolvePlayer(actor);

            if (foulingPlayer.equals(player1)) {
                player2CurrentFramePoints = Math.max(0, player2CurrentFramePoints - points);
            } else {
                player1CurrentFramePoints = Math.max(0, player1CurrentFramePoints - points);
            }

            resetBreak();
        }

        void awardFrameTo(Competitor actor) {
            Player player = resolvePlayer(actor);
            boolean isPlayer1 = player.equals(player1);

            if (isPlayer1 && player1CurrentFramePoints >= player2CurrentFramePoints) {
                player1Frames++;
            } else if (!isPlayer1 && player2CurrentFramePoints >= player1CurrentFramePoints) {
                player2Frames++;
            } else {
                if (isPlayer1) {
                    player1Frames++;
                } else {
                    player2Frames++;
                }
            }

            resetFrame();
        }

        void removeFrameFor(Competitor actor) {
            Player player = resolvePlayer(actor);

            if (player.equals(player1) && player1Frames > 0) {
                player1Frames--;
            } else if (player.equals(player2) && player2Frames > 0) {
                player2Frames--;
            }
        }

        void resetBreak() {
            // Przerywa aktualnego breaka.
            currentBreak = 0;
            currentBreaker = null;
        }

        private void resetFrame() {
            // Po zakończeniu frame'a punkty aktualnego frame'a wracają do zera.
            player1CurrentFramePoints = 0;
            player2CurrentFramePoints = 0;
            resetBreak();
        }

        private void trackBreak(Player player, int points) {
            // Liczy aktualnego breaka i highest break.
            if (player.equals(currentBreaker)) {
                currentBreak += points;
            } else {
                currentBreaker = player;
                currentBreak = points;
            }

            if (currentBreak > highestBreak) {
                highestBreak = currentBreak;
                highestBreakPlayer = currentBreaker;
            }
        }

        private Player resolvePlayer(Competitor actor) {
            // Sprawdza, czy actor jest jednym z graczy meczu.
            if (actor == null) {
                throw new IllegalArgumentException("Event actor cannot be null");
            }

            if (actor.equals(player1)) {
                return player1;
            }

            if (actor.equals(player2)) {
                return player2;
            }

            throw new IllegalArgumentException("Event actor is not part of this snooker match");
        }
    }
}