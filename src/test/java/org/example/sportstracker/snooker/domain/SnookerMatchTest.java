package org.example.sportstracker.snooker.domain;

import org.example.sportstracker.core.domain.match.MatchEvent;
import org.example.sportstracker.core.domain.match.MatchEventListener;
import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.snooker.domain.competitor.Player;
import org.example.sportstracker.snooker.domain.match.SnookerEvents;
import org.example.sportstracker.snooker.domain.match.SnookerMatch;
import org.example.sportstracker.snooker.domain.match.SnookerMatchEvent;
import org.example.sportstracker.snooker.domain.match.SnookerMatchFactory;
import org.example.sportstracker.snooker.domain.result.SnookerResult;
import org.example.sportstracker.snooker.domain.match.SnookerMatch.SnookerScore;
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
        p1_osullivan = Player.builder()
                .id("1")
                .name("Ronnie")
                .build();

        p2_trump = Player.builder()
                .id("2")
                .name("Judd")
                .build();

        // A Best-of-5 match requires 3 frames to win.
        bestOf5Factory = new SnookerMatchFactory(5);
    }

    @Test
    void shouldTrackPerfect147MaximumBreak() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);

        match.startMatch();

        // Simulate 15 Reds and 15 Blacks.
        for (int i = 0; i < 15; i++) {
            match.recordEvent(potRed(match, p1_osullivan));
            match.recordEvent(potColor(match, p1_osullivan, 7)); // Black
        }

        // Simulate the final colors clearance.
        match.recordEvent(potColor(match, p1_osullivan, 2)); // Yellow
        match.recordEvent(potColor(match, p1_osullivan, 3)); // Green
        match.recordEvent(potColor(match, p1_osullivan, 4)); // Brown
        match.recordEvent(potColor(match, p1_osullivan, 5)); // Blue
        match.recordEvent(potColor(match, p1_osullivan, 6)); // Pink
        match.recordEvent(potColor(match, p1_osullivan, 7)); // Black

        SnookerScore score = match.getScore();

        assertEquals(147, score.getPlayer1CurrentFramePoints(), "Player 1 should have exactly 147 points");
        assertEquals(0, score.getPlayer2CurrentFramePoints(), "Player 2 should have 0 points");
        assertEquals(147, score.getHighestBreak(), "Highest break should be recorded as 147");
        assertEquals(p1_osullivan, score.getHighestBreakPlayer(), "Highest break player should be accurately tracked as Ronnie");
    }

    @Test
    void shouldCorrectlyAwardFoulPointsToOpponentAndResetBreaks() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);

        match.startMatch();

        // Ronnie pots a red (1 pt).
        match.recordEvent(potRed(match, p1_osullivan));

        assertEquals(1, match.getScore().getHighestBreak());

        // Ronnie goes for a black but fouls (7 pts penalty).
        match.recordEvent(foul(match, p1_osullivan, 7));

        assertEquals(1, match.getScore().getPlayer1CurrentFramePoints());
        assertEquals(7, match.getScore().getPlayer2CurrentFramePoints(), "Judd should receive 7 points from Ronnie's foul");
        assertEquals(0, match.getScore().getCurrentBreak(), "Break should be reset to 0 after a foul");

        // Judd comes to the table, calls a miss.
        match.recordEvent(missCalled(match, p2_trump));

        // Judd fouls on the brown (4 pts penalty).
        match.recordEvent(foul(match, p2_trump, 4));

        assertEquals(5, match.getScore().getPlayer1CurrentFramePoints(), "Ronnie gets 4 points from Judd's foul (1 + 4 = 5)");
        assertEquals(7, match.getScore().getPlayer2CurrentFramePoints());
    }

    @Test
    void shouldResolveBestOfFiveMatchCorrectly() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);

        match.startMatch();

        // Frame 1 -> Ronnie scores and wins.
        winFrame(match, p1_osullivan);

        // Frame 2 -> Judd scores and wins.
        match.startNextFrame();
        winFrame(match, p2_trump);

        // Frame 3 -> Ronnie scores and wins.
        match.startNextFrame();
        winFrame(match, p1_osullivan);

        // Frame 4 -> Ronnie scores and wins. Score is now 3-1.
        match.startNextFrame();
        winFrame(match, p1_osullivan);

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

        // Ronnie wins 3 frames in a row. Best of 5 requires 3 to win.
        winFrame(match, p1_osullivan);

        match.startNextFrame();
        winFrame(match, p1_osullivan);

        match.startNextFrame();
        winFrame(match, p1_osullivan);

        // Match is mathematically won. Starting a 4th frame should be prevented.
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                match::startNextFrame
        );

        assertEquals("Cannot start next frame; match is already won", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionsForInvalidStateTransitions() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);

        // 1. Cannot end a match that has not started.
        IllegalStateException endBeforeStartException = assertThrows(
                IllegalStateException.class,
                match::endMatch
        );

        assertEquals("Only an in-progress match can be completed", endBeforeStartException.getMessage());

        // 2. Cannot record events before starting.
        IllegalStateException eventBeforeStartException = assertThrows(
                IllegalStateException.class,
                () -> match.recordEvent(potRed(match, p1_osullivan))
        );

        assertEquals("Events can only be recorded during an in-progress match", eventBeforeStartException.getMessage());

        match.startMatch();

        // 3. Cannot start a match twice.
        IllegalStateException startTwiceException = assertThrows(
                IllegalStateException.class,
                match::startMatch
        );

        assertEquals("Match is not scheduled", startTwiceException.getMessage());

        match.endMatch();

        // 4. Cannot record events after completion.
        IllegalStateException eventAfterEndException = assertThrows(
                IllegalStateException.class,
                () -> match.recordEvent(potRed(match, p1_osullivan))
        );

        assertEquals("Events can only be recorded during an in-progress match", eventAfterEndException.getMessage());
    }

    @Test
    void shouldThrowExceptionIfTryingToStartMoreFramesThanAllowed() {
        // Best of 1 match: only 1 frame allowed.
        SnookerMatchFactory bestOf1Factory = new SnookerMatchFactory(1);
        SnookerMatch match = (SnookerMatch) bestOf1Factory.createLeagueMatch(p1_osullivan, p2_trump);

        match.startMatch();

        // Currently in Frame 1. Trying to start Frame 2 should fail.
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                match::startNextFrame
        );

        assertEquals("All frames have already been started", exception.getMessage());
    }

    @Test
    void shouldPreventResolutionIfFramesRequiredToWinAreNotMet() {
        SnookerMatchResolutionStrategy strategy = new SnookerMatchResolutionStrategy(5); // Requires 3 frames.

        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);

        match.startMatch();

        // Frame 1 -> Ronnie wins.
        winFrame(match, p1_osullivan);

        // Frame 2 -> Judd wins.
        match.startNextFrame();
        winFrame(match, p2_trump);

        // Frame 3 -> Ronnie wins. Score 2-1.
        match.startNextFrame();
        winFrame(match, p1_osullivan);

        // Frame 4 -> Judd wins. Score 2-2.
        match.startNextFrame();
        winFrame(match, p2_trump);

        SnookerScore tieScore = match.getScore();

        // Nobody has reached 3 frames, so strategy should refuse to resolve.
        assertThrows(
                IllegalStateException.class,
                () -> strategy.resolve(tieScore, List.of(p1_osullivan, p2_trump)),
                "Strategy should refuse to resolve if nobody has reached the target frame count"
        );
    }

    @Test
    void shouldAbandonMatchAndRetainLiveScore() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);

        match.startMatch();

        match.recordEvent(potRed(match, p1_osullivan));
        match.recordEvent(potColor(match, p1_osullivan, 7));

        // Player falls ill, match is abandoned.
        match.abandonMatch("Player illness");

        assertEquals(MatchStatus.ABANDONED, match.getStatus());
        assertEquals("Player illness", match.getAbandonReason());

        // Ensure score is preserved exactly as it was when abandoned.
        assertEquals(8, match.getScore().getPlayer1CurrentFramePoints());
    }

    @Test
    void shouldBroadcastEventsToListeners() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);
        TestMatchBroadcaster broadcaster = new TestMatchBroadcaster();

        match.addMatchEventListener(broadcaster);

        match.startMatch();

        SnookerMatchEvent red = potRed(match, p1_osullivan);

        match.recordEvent(red);

        assertEquals(1, broadcaster.getReceivedEvents().size());
        assertEquals("POT_RED", broadcaster.getReceivedEvents().get(0).getEventName());
        assertEquals(p1_osullivan, broadcaster.getReceivedEvents().get(0).getActor());
        assertEquals(red.getEventId(), broadcaster.getReceivedEvents().get(0).getEventId());
    }

    @Test
    void shouldAssignWalkoverCorrectly() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);

        // Ronnie wins via walkover, 3-0.
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
        Player randomPlayer = Player.builder()
                .id("99")
                .name("Random")
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> match.assignWalkover(randomPlayer, 3, 0)
        );

        assertEquals("Walkover winner must be a participant in the match", exception.getMessage());
    }

    @Test
    void shouldReplayScoreAndTimelineWithInvalidatedEvent() {
        SnookerMatch match = (SnookerMatch) bestOf5Factory.createKnockoutMatch(p1_osullivan, p2_trump);

        match.startMatch();

        SnookerMatchEvent red = potRed(match, p1_osullivan);
        SnookerMatchEvent black = potColor(match, p1_osullivan, 7);
        SnookerMatchEvent juddFoul = foul(match, p2_trump, 4);

        match.recordEvent(red);       // Ronnie 1
        match.recordEvent(black);     // Ronnie 8
        match.recordEvent(juddFoul);  // Ronnie 12

        SnookerMatchEvent invalidatedBlack = SnookerEvents.eventInvalidated(
                p2_trump,
                match.getCurrentFrame(),
                "Black pot is invalidated",
                List.of(black.getEventId())
        );

        match.recordEvent(invalidatedBlack); // Ronnie should go back from 12 to 5.

        assertEquals(5, match.getScore().getPlayer1CurrentFramePoints());
        assertEquals(0, match.getScore().getPlayer2CurrentFramePoints());

        SnookerScore replayedScore = match.replayScore();

        assertEquals(match.getScore().display(), replayedScore.display());
        assertEquals(5, replayedScore.getPlayer1CurrentFramePoints());

        var timeline = match.replayTimeline();

        assertEquals(4, timeline.size());

        assertEquals(1, timeline.get(0).player1CurrentFramePoints());
        assertEquals(8, timeline.get(1).player1CurrentFramePoints());
        assertEquals(12, timeline.get(2).player1CurrentFramePoints());
        assertEquals(5, timeline.get(3).player1CurrentFramePoints());

        assertEquals("EVENT_INVALIDATED", timeline.get(3).eventName());
        assertEquals(List.of(black.getEventId()), timeline.get(3).relatedEventIds());
    }

    private SnookerMatchEvent potRed(SnookerMatch match, Player actor) {
        return SnookerEvents.potRed(
                actor,
                match.getCurrentFrame(),
                actor.getName() + " pots a red"
        );
    }

    private SnookerMatchEvent potColor(SnookerMatch match, Player actor, int points) {
        return SnookerEvents.potColor(
                actor,
                match.getCurrentFrame(),
                points,
                actor.getName() + " pots a color worth " + points
        );
    }

    private SnookerMatchEvent foul(SnookerMatch match, Player actor, int points) {
        return SnookerEvents.foul(
                actor,
                match.getCurrentFrame(),
                points,
                actor.getName() + " commits a foul worth " + points
        );
    }

    private SnookerMatchEvent missCalled(SnookerMatch match, Player actor) {
        return SnookerEvents.missCalled(
                actor,
                match.getCurrentFrame(),
                "Miss called on " + actor.getName()
        );
    }

    private SnookerMatchEvent frameWon(SnookerMatch match, Player actor) {
        return SnookerEvents.frameWon(
                actor,
                match.getCurrentFrame(),
                actor.getName() + " wins frame " + match.getCurrentFrame()
        );
    }

    private void winFrame(SnookerMatch match, Player player) {
        // Zachowujemy starą logikę testów: wbicie czerwonej i zakończenie frame'a.
        match.recordEvent(potRed(match, player));
        match.recordEvent(frameWon(match, player));
    }

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