package org.example.sportstracker.snooker.domain;

import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.snooker.domain.bracket.SnookerBracketTree;
import org.example.sportstracker.snooker.domain.competitor.Player;
import org.example.sportstracker.snooker.domain.match.SnookerEventType;
import org.example.sportstracker.snooker.domain.match.SnookerMatch;
import org.example.sportstracker.snooker.domain.match.SnookerMatchEvent;
import org.example.sportstracker.snooker.domain.match.SnookerMatchFactory;
import org.example.sportstracker.snooker.domain.stage.SnookerKnockoutStage;
import org.example.sportstracker.snooker.domain.tournament.SnookerTournament;
import org.example.sportstracker.snooker.domain.match.SnookerMatch.SnookerScore; // Poprawny import klasy zagnieżdżonej
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class SnookerTournamentTest {

    private Player ronnie;
    private Player judd;
    private Player john;
    private Player mark;

    private SnookerMatchFactory bestOf3Factory;

    @BeforeEach
    void setUp() {
        ronnie = Player.builder().id("1").name("Ronnie O'Sullivan").build();
        judd = Player.builder().id("2").name("Judd Trump").build();
        john = Player.builder().id("3").name("John Higgins").build();
        mark = Player.builder().id("4").name("Mark Selby").build();

        // Best of 3 means 2 frames are needed to win
        bestOf3Factory = new SnookerMatchFactory(3);
    }

    @Test
    void shouldRunCompleteSnookerTournamentEndToEnd() {
        // 1. Create the Tournament
        SnookerTournament tournament = new SnookerTournament();
        tournament.setName("Masters Snooker 2026");

        // 2. Create the Knockout Stage (Best of 3)
        SnookerKnockoutStage knockoutStage = new SnookerKnockoutStage(3);
        tournament.addStage(knockoutStage);
        tournament.startTournament();

        // 3. Create Semi-Final Matches
        SnookerMatch semi1 = (SnookerMatch) bestOf3Factory.createKnockoutMatch(ronnie, judd);
        SnookerMatch semi2 = (SnookerMatch) bestOf3Factory.createKnockoutMatch(john, mark);

        knockoutStage.addMatch(semi1);
        knockoutStage.addMatch(semi2);

        // 4. Setup Bracket Tree
        SnookerBracketTree bracket = knockoutStage.getBrackets();
        bracket.setFirstRound(Arrays.asList(semi1, semi2));

        // 5. Play Semi 1: Ronnie wins 2-0 against Judd
        semi1.startMatch();
        winFrame(semi1, ronnie);
        semi1.startNextFrame();
        winFrame(semi1, ronnie);
        semi1.endMatch();

        assertEquals(ronnie, semi1.getResult().getWinner());

        // 6. Play Semi 2: Mark wins 2-1 against John
        semi2.startMatch();
        winFrame(semi2, john); // Frame 1 to John
        semi2.startNextFrame();
        winFrame(semi2, mark); // Frame 2 to Mark
        semi2.startNextFrame();
        winFrame(semi2, mark); // Frame 3 to Mark
        semi2.endMatch();

        assertEquals(mark, semi2.getResult().getWinner());

        // 7. Generate Final Round
        bracket.generateNextRound();
        assertEquals(2, bracket.getRounds().size());
        assertEquals(1, bracket.getRounds().get(1).size());

        SnookerMatch finalMatch = bracket.getRounds().get(1).get(0);
        assertEquals(ronnie, finalMatch.getPlayer1());
        assertEquals(mark, finalMatch.getPlayer2());

        // 8. Play Final: Ronnie wins 2-1
        finalMatch.startMatch();
        winFrame(finalMatch, mark);
        finalMatch.startNextFrame();
        winFrame(finalMatch, ronnie);
        finalMatch.startNextFrame();
        winFrame(finalMatch, ronnie);
        finalMatch.endMatch();

        // 9. End Stage and Tournament
        knockoutStage.endStage();
        tournament.endTournament();

        // 10. Assertions
        assertEquals(1, knockoutStage.getAdvancingCompetitors().size());
        assertEquals(ronnie, knockoutStage.getAdvancingCompetitors().get(0));
        assertEquals(ronnie, tournament.getOverallWinner(), "Ronnie should be the overall tournament winner");
    }

    @Test
    void shouldHandleWalkoversInTournamentBracket() {
        SnookerKnockoutStage knockoutStage = new SnookerKnockoutStage(3);
        SnookerMatch semi1 = (SnookerMatch) bestOf3Factory.createKnockoutMatch(ronnie, judd);
        SnookerMatch semi2 = (SnookerMatch) bestOf3Factory.createKnockoutMatch(john, mark);

        SnookerBracketTree bracket = knockoutStage.getBrackets();
        bracket.setFirstRound(Arrays.asList(semi1, semi2));

        // Play Semi 1 to completion (Ronnie wins 2-0)
        semi1.startMatch();
        winFrame(semi1, ronnie);
        semi1.startNextFrame();
        winFrame(semi1, ronnie);
        semi1.endMatch();

        // Semi 2: John Higgins withdraws, Mark Selby gets a walkover (2-0)
        semi2.assignWalkover(mark, 2, 0);

        // This should NOT throw an exception, as WALKOVER is a valid resolution state
        assertDoesNotThrow(bracket::generateNextRound);

        assertEquals(2, bracket.getRounds().size());
        SnookerMatch finalMatch = bracket.getRounds().get(1).get(0);

        assertEquals(ronnie, finalMatch.getPlayer1());
        assertEquals(mark, finalMatch.getPlayer2(), "Mark should advance to the final via walkover");
        assertEquals(MatchStatus.SCHEDULED, finalMatch.getStatus());
    }

    @Test
    void shouldProcessSimpleKnockoutStageWithWalkoverWithoutBracketTree() {
        SnookerKnockoutStage knockoutStage = new SnookerKnockoutStage(3);

        SnookerMatch match1 = (SnookerMatch) bestOf3Factory.createKnockoutMatch(ronnie, judd);
        SnookerMatch match2 = (SnookerMatch) bestOf3Factory.createKnockoutMatch(john, mark);

        knockoutStage.addMatch(match1);
        knockoutStage.addMatch(match2);

        // Ronnie gets a walkover
        match1.assignWalkover(ronnie, 2, 0);

        // John gets a walkover
        match2.assignWalkover(john, 2, 0);

        // This triggers the fallback `advanceWinner` loop which must handle WALKOVER
        assertDoesNotThrow(knockoutStage::endStage);

        assertEquals(2, knockoutStage.getAdvancingCompetitors().size());
        assertTrue(knockoutStage.getAdvancingCompetitors().contains(ronnie));
        assertTrue(knockoutStage.getAdvancingCompetitors().contains(john));
    }

    @Test
    void shouldThrowExceptionIfTryingToGenerateNextRoundWithIncompleteMatches() {
        SnookerKnockoutStage knockoutStage = new SnookerKnockoutStage(3);
        SnookerMatch semi1 = (SnookerMatch) bestOf3Factory.createKnockoutMatch(ronnie, judd);
        SnookerMatch semi2 = (SnookerMatch) bestOf3Factory.createKnockoutMatch(john, mark);

        SnookerBracketTree bracket = knockoutStage.getBrackets();
        bracket.setFirstRound(Arrays.asList(semi1, semi2));

        // Play Semi 1 to completion (Ronnie wins 2-0)
        semi1.startMatch();
        winFrame(semi1, ronnie);
        semi1.startNextFrame();
        winFrame(semi1, ronnie);
        semi1.endMatch();

        // Semi 2 is NOT played yet
        assertEquals(MatchStatus.SCHEDULED, semi2.getStatus());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                bracket::generateNextRound
        );

        assertEquals("Not all matches in the current round are completed!", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionIfEndingStageWithIncompleteMatches() {
        SnookerKnockoutStage knockoutStage = new SnookerKnockoutStage(3);
        SnookerMatch match1 = (SnookerMatch) bestOf3Factory.createKnockoutMatch(ronnie, judd);

        knockoutStage.addMatch(match1);

        // Match hasn't even started
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                knockoutStage::endStage
        );

        assertEquals("Not all snooker matches in this stage are completed", exception.getMessage());
    }

    @Test
    void shouldProcessSimpleKnockoutStageWithoutBracketTree() {
        // Fallback testing: If no bracket is explicitly managed, just advance winners from the list
        SnookerKnockoutStage knockoutStage = new SnookerKnockoutStage(3);

        SnookerMatch match1 = (SnookerMatch) bestOf3Factory.createKnockoutMatch(ronnie, judd);
        SnookerMatch match2 = (SnookerMatch) bestOf3Factory.createKnockoutMatch(john, mark);

        knockoutStage.addMatch(match1);
        knockoutStage.addMatch(match2);

        // Play matches
        match1.startMatch();
        winFrame(match1, judd);
        match1.startNextFrame();
        winFrame(match1, judd);
        match1.endMatch();

        match2.startMatch();
        winFrame(match2, john);
        match2.startNextFrame();
        winFrame(match2, john);
        match2.endMatch();

        // This triggers the fallback `advanceWinner` loop
        knockoutStage.endStage();

        assertEquals(2, knockoutStage.getAdvancingCompetitors().size());
        assertTrue(knockoutStage.getAdvancingCompetitors().contains(judd));
        assertTrue(knockoutStage.getAdvancingCompetitors().contains(john));
    }

    // --- Helper Methods ---

    private void winFrame(SnookerMatch match, Player player) {
        match.recordEvent(SnookerMatchEvent.builder()
                .actor(player)
                .eventType(SnookerEventType.POT_RED)
                .pointsValue(1)
                .build());

        match.recordEvent(SnookerMatchEvent.builder()
                .actor(player)
                .eventType(SnookerEventType.FRAME_WON)
                .build());
    }
}