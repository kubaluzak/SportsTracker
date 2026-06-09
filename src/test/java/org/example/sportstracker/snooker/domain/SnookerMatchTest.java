package org.example.sportstracker.snooker.domain;

import org.example.sportstracker.core.domain.match.MatchEvent;
import org.example.sportstracker.core.domain.match.MatchEventListener;
import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.snooker.domain.competitor.Player;
import org.example.sportstracker.snooker.domain.match.*;
import org.example.sportstracker.snooker.domain.result.SnookerResult;
import org.example.sportstracker.snooker.domain.match.SnookerMatch.SnookerScore; // Poprawny import zagnieżdżonej klasy wyniku
import org.example.sportstracker.snooker.domain.strategy.SnookerMatchResolutionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnookerMatchTest {

    private Player p1_osullivan;
    private Player p2_trump;
    private SnookerMatchFactory bestOf5Factory;

    @BeforeEach
    void setUp() {
        p1_osullivan = Player.builder().id("1").name("Ronnie").build();
        p2_trump = Player.builder().id("2").name("Judd").build();
        // A Best-of-5 match requires 3 frames to win
        bestOf5Factory = new SnookerMatchFactory(5);
    }

    @Test
    void shouldTrackPerfect147MaximumBreak() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);
        match.startMatch();

        // Simulate 15 Reds and 15 Blacks
        for (int i = 0; i < 15; i++) {
            match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_RED, 1));
            match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_COLOR, 7)); // Black
        }

        // Simulate the final colors clearance
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_COLOR, 2)); // Yellow
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_COLOR, 3)); // Green
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_COLOR, 4)); // Brown
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_COLOR, 5)); // Blue
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_COLOR, 6)); // Pink
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_COLOR, 7)); // Black

        SnookerScore score = match.getScore();

        assertEquals(147, score.getP1CurrentFramePoints(), "Player 1 should have exactly 147 points");
        assertEquals(0, score.getP2CurrentFramePoints(), "Player 2 should have 0 points");
        assertEquals(147, score.getHighestBreak(), "Highest break should be recorded as 147");
        assertEquals(p1_osullivan, score.getHighestBreakPlayer(), "Highest break player should be accurately tracked as Ronnie");
    }

    @Test
    void shouldCorrectlyAwardFoulPointsToOpponentAndResetBreaks() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);
        match.startMatch();

        // Ronnie pots a red (1 pt)
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_RED, 1));
        assertEquals(1, match.getScore().getHighestBreak());

        // Ronnie goes for a black but fouls (7 pts penalty)
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.FOUL, 7));

        assertEquals(1, match.getScore().getP1CurrentFramePoints());
        assertEquals(7, match.getScore().getP2CurrentFramePoints(), "Judd should receive 7 points from Ronnie's foul");
        assertEquals(0, match.getScore().getCurrentBreak(), "Break should be reset to 0 after a foul");

        // Judd comes to the table, calls a miss
        match.recordEvent(createEvent(p2_trump, SnookerEventType.MISS_CALLED, 0));

        // Judd fouls on the brown (4 pts penalty)
        match.recordEvent(createEvent(p2_trump, SnookerEventType.FOUL, 4));

        assertEquals(5, match.getScore().getP1CurrentFramePoints(), "Ronnie gets 4 points from Judd's foul (1 + 4 = 5)");
        assertEquals(7, match.getScore().getP2CurrentFramePoints());
    }

    @Test
    void shouldResolveBestOfFiveMatchCorrectly() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);
        match.startMatch();

        // Frame 1 -> Ronnie scores and wins
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_RED, 1));
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.FRAME_WON, 0));

        // Frame 2 -> Judd scores and wins
        match.startNextFrame();
        match.recordEvent(createEvent(p2_trump, SnookerEventType.POT_RED, 1));
        match.recordEvent(createEvent(p2_trump, SnookerEventType.FRAME_WON, 0));

        // Frame 3 -> Ronnie scores and wins
        match.startNextFrame();
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_RED, 1));
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.FRAME_WON, 0));

        // Frame 4 -> Ronnie scores and wins (Score is now 3-1, match should be over)
        match.startNextFrame();
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_RED, 1));
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.FRAME_WON, 0));

        match.endMatch();

        SnookerResult result = (SnookerResult) match.getResult();

        assertEquals(MatchStatus.COMPLETED, match.getStatus());
        assertEquals(3, match.getScore().getPlayer1Frames());
        assertEquals(1, match.getScore().getPlayer2Frames());
        assertEquals(p1_osullivan, result.getWinner(), "Ronnie should win the Best of 5 match");
        assertFalse(result.isDraw(), "Snooker matches cannot be drawn");
    }

    @Test
    void shouldPreventStartingNextFrameIfMatchAlreadyWon() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);
        match.startMatch();

        // Ronnie wins 3 frames in a row (Best of 5 requires 3 to win)
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_RED, 1));
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.FRAME_WON, 0));

        match.startNextFrame();
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_RED, 1));
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.FRAME_WON, 0));

        match.startNextFrame();
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_RED, 1));
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.FRAME_WON, 0));

        // Match is mathematically won (3-0). Starting a 4th frame should be prevented.
        assertThrows(IllegalStateException.class, match::startNextFrame, "Cannot start next frame; the match is already won.");
    }

    @Test
    void shouldThrowExceptionsForInvalidStateTransitions() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);

        // 1. Cannot end a match that hasn't started
        assertThrows(IllegalStateException.class, match::endMatch, "Cannot end a scheduled match");

        // 2. Cannot record events before starting
        assertThrows(IllegalStateException.class, () ->
                match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_RED, 1))
        );

        match.startMatch();

        // 3. Cannot start a match twice
        assertThrows(IllegalStateException.class, match::startMatch, "Cannot start an already in-progress match");

        match.endMatch();

        // 4. Cannot record events after completion
        assertThrows(IllegalStateException.class, () ->
                match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_RED, 1))
        );
    }

    @Test
    void shouldThrowExceptionIfTryingToStartMoreFramesThanAllowed() {
        // Best of 1 match (Only 1 frame allowed)
        SnookerMatchFactory bestOf1Factory = new SnookerMatchFactory(1);
        SnookerMatch match = (SnookerMatch) bestOf1Factory.createLeagueMatch(p1_osullivan, p2_trump);
        match.startMatch();

        // Currently in Frame 1. Trying to start Frame 2 should fail.
        assertThrows(IllegalStateException.class, match::startNextFrame, "Cannot exceed total frames");
    }

    @Test
    void shouldPreventResolutionIfFramesRequiredToWinAreNotMet() {
        SnookerMatchResolutionStrategy strategy = new SnookerMatchResolutionStrategy(5); // Requires 3 frames

        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);
        match.startMatch();

        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_RED, 1));
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.FRAME_WON, 0));

        match.startNextFrame();
        match.recordEvent(createEvent(p2_trump, SnookerEventType.POT_RED, 1));
        match.recordEvent(createEvent(p2_trump, SnookerEventType.FRAME_WON, 0));

        // Frame 3 -> Ronnie wygrywa (2-1)
        match.startNextFrame();
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_RED, 1));
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.FRAME_WON, 0));

        // Frame 4 -> Judd wygrywa (2-2)
        match.startNextFrame();
        match.recordEvent(createEvent(p2_trump, SnookerEventType.POT_RED, 1));
        match.recordEvent(createEvent(p2_trump, SnookerEventType.FRAME_WON, 0));

        // Pobieramy bezpieczny, szczelnie zamknięty wynik meczu (aktualny stan: 2-2)
        SnookerScore tieScore = match.getScore();

        // Nikt nie osiągnął wymaganych 3 frejmów, strategia powinna zgłosić wyjątek
        assertThrows(IllegalStateException.class, () ->
                        strategy.resolve(tieScore, p1_osullivan, p2_trump),
                "Strategy should refuse to resolve if nobody has reached the target frame count"
        );
    }

    @Test
    void shouldAbandonMatchAndRetainLiveScore() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);
        match.startMatch();

        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_RED, 1));
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_COLOR, 7));

        // Player falls ill, match is abandoned
        match.abandonMatch("Player illness");

        assertEquals(MatchStatus.ABANDONED, match.getStatus());
        assertEquals("Player illness", match.getAbandonReason());

        // Ensure score is preserved exactly as it was when abandoned
        assertEquals(8, match.getScore().getP1CurrentFramePoints());
    }

    @Test
    void shouldBroadcastEventsToListeners() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);
        TestMatchBroadcaster broadcaster = new TestMatchBroadcaster();
        match.addMatchEventListener(broadcaster);

        match.startMatch();
        match.recordEvent(createEvent(p1_osullivan, SnookerEventType.POT_RED, 1));

        assertEquals(1, broadcaster.getReceivedEvents().size());
        assertEquals(SnookerEventType.POT_RED, broadcaster.getReceivedEvents().get(0).getEventType());
        assertEquals(p1_osullivan, broadcaster.getReceivedEvents().get(0).getActor());
    }

    @Test
    void shouldAssignWalkoverCorrectly() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);

        // Ronnie wins via walkover, 3-0
        match.assignWalkover(p1_osullivan, 3, 0);

        assertEquals(MatchStatus.WALKOVER, match.getStatus());
        assertEquals(3, match.getScore().getPlayer1Frames());
        assertEquals(0, match.getScore().getPlayer2Frames());

        SnookerResult result = (SnookerResult) match.getResult();
        assertEquals(p1_osullivan, result.getWinner());
        assertTrue(result.isWalkover());
        assertFalse(result.isCompletedNormally());
        assertEquals(p2_trump, result.getWalkoverLoser());
    }

    @Test
    void shouldThrowExceptionIfWalkoverWinnerNotParticipating() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);
        Player randomPlayer = Player.builder().id("99").name("Random").build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> match.assignWalkover(randomPlayer, 3, 0)
        );

        assertEquals("Walkover winner must be a participant in the match", exception.getMessage());
    }

    // Helper method
    private SnookerMatchEvent createEvent(Player actor, SnookerEventType type, int points) {
        return SnookerMatchEvent.builder()
                .actor(actor)
                .eventType(type)
                .pointsValue(points)
                .build();
    }

    // Helper class for listener verification
    private static class TestMatchBroadcaster implements MatchEventListener {
        private final List<SnookerMatchEvent> receivedEvents = new ArrayList<>();

        @Override
        public void onEventRecorded(MatchEvent event, Score currentScore) {
            receivedEvents.add((SnookerMatchEvent) event);
        }

        public List<SnookerMatchEvent> getReceivedEvents() {
            return receivedEvents;
        }
    }
}