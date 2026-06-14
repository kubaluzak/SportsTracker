package org.example.sportstracker.football.domain;

import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.MatchEvent;
import org.example.sportstracker.core.domain.match.MatchEventListener;
import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.football.domain.bracket.BracketTree;
import org.example.sportstracker.football.domain.competitor.Team;
import org.example.sportstracker.football.domain.match.FootballEvents;
import org.example.sportstracker.football.domain.match.FootballMatch;
import org.example.sportstracker.football.domain.match.FootballMatchEvent;
import org.example.sportstracker.football.domain.match.FootballMatchFactory;
import org.example.sportstracker.football.domain.match.ScoreSnapshot;
import org.example.sportstracker.football.domain.observer.ConsoleMatchBroadcaster;
import org.example.sportstracker.football.domain.result.FootballResult;
import org.example.sportstracker.football.domain.stage.FootballKnockoutStage;
import org.example.sportstracker.football.domain.stage.FootballLeagueStage;
import org.example.sportstracker.football.domain.table.TableEntry;
import org.example.sportstracker.football.domain.tournament.FootballTournament;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FullFootballTournamentWithEventReplayTest {

    @Test
    void shouldRunWholeTournamentWithEventClassesUndoReplayTimelineLeagueAndBracket() {
        // =========================
        // 1. TWORZENIE DRUŻYN
        // =========================

        Team lech = team("1", "Lech Poznan");
        Team legia = team("2", "Legia Warszawa");
        Team rakow = team("3", "Rakow Czestochowa");
        Team jagiellonia = team("4", "Jagiellonia");
        Team widzew = team("5", "Widzew Lodz");
        Team pogon = team("6", "Pogon Szczecin");
        Team slask = team("7", "Slask Wroclaw");
        Team gornik = team("8", "Gornik Zabrze");

        assertEquals("1", lech.getId());
        assertEquals("Lech Poznan", lech.getName());
        assertEquals(3, lech.getPlayers().size());
        assertEquals("Manager Lech Poznan", lech.getManager());

        // =========================
        // 2. FABRYKA I TURNIEJ
        // =========================

        FootballMatchFactory factory = new FootballMatchFactory();

        FootballTournament tournament = new FootballTournament();
        tournament.setName("Event Replay Football Cup 2026");

        assertNull(tournament.getOverallWinner());

        tournament.startTournament();

        // =========================
        // 3. FAZA LIGOWA
        // =========================

        FootballLeagueStage leagueStage = new FootballLeagueStage(
                Arrays.asList(lech, legia, rakow, jagiellonia, widzew, pogon, slask, gornik),
                3
        );

        tournament.addStage(leagueStage);

        assertEquals(1, tournament.getStages().size());
        assertEquals(3, leagueStage.getPointsForWin());

        leagueStage.startStage();

        // =========================
        // 4. MECZE LIGOWE
        // =========================

        FootballMatch m1 = (FootballMatch) factory.createLeagueMatch(lech, legia);
        FootballMatch m2 = (FootballMatch) factory.createLeagueMatch(rakow, jagiellonia);
        FootballMatch m3 = (FootballMatch) factory.createLeagueMatch(widzew, pogon);
        FootballMatch m4 = (FootballMatch) factory.createLeagueMatch(slask, gornik);
        FootballMatch m5 = (FootballMatch) factory.createLeagueMatch(lech, rakow);
        FootballMatch m6 = (FootballMatch) factory.createLeagueMatch(legia, jagiellonia);
        FootballMatch m7 = (FootballMatch) factory.createLeagueMatch(pogon, slask);
        FootballMatch m8 = (FootballMatch) factory.createLeagueMatch(gornik, widzew);

        TestMatchBroadcaster testBroadcaster = new TestMatchBroadcaster();

        m1.addMatchEventListener(testBroadcaster);
        m1.addMatchEventListener(new ConsoleMatchBroadcaster());


        List<FootballMatch> leagueMatches = List.of(m1, m2, m3, m4, m5, m6, m7, m8);
        leagueMatches.forEach(leagueStage::addMatch);

        assertEquals(8, leagueStage.getStageMatches().size());

        // =========================
        // 5. MECZ LECH VS LEGIA
        // Testujemy:
        // - gole
        // - VAR
        // - anulowanie gola
        // - anulowanie kilku kartek naraz
        // - replay score
        // - timeline
        // =========================

        FootballMatchEvent lechFirstGoal = FootballEvents.goalScored(
                lech,
                12,
                "Lech scores the opening goal"
        );

        FootballMatchEvent lechSecondGoal = FootballEvents.goalScored(
                lech,
                20,
                "Lech scores again"
        );

        FootballMatchEvent legiaYellowCard = FootballEvents.yellowCard(
                legia,
                23,
                "Legia receives a yellow card"
        );

        FootballMatchEvent varReview = FootballEvents.varReview(
                legia,
                24,
                "VAR checks Lech second goal",
                List.of(lechSecondGoal.getEventId())
        );

        FootballMatchEvent disallowedGoal = FootballEvents.goalDisallowed(
                legia,
                26,
                "VAR disallows Lech second goal",
                List.of(lechSecondGoal.getEventId())
        );

        FootballMatchEvent legiaRedCard = FootballEvents.redCard(
                legia,
                40,
                "Legia receives a red card"
        );

        FootballMatchEvent invalidatedCards = FootballEvents.eventInvalidated(
                lech,
                45,
                "Referee cancels Legia cards after review",
                List.of(legiaYellowCard.getEventId(), legiaRedCard.getEventId())
        );

        FootballMatchEvent legiaPenaltyGoal = FootballEvents.penaltyScored(
                legia,
                55,
                "Legia scores from penalty"
        );

        FootballMatchEvent lechWinningGoal = FootballEvents.goalScored(
                lech,
                88,
                "Lech scores the winning goal"
        );

        assertNotNull(lechFirstGoal.getEventId());
        assertNotNull(lechFirstGoal.getTimestamp());

        assertEquals(List.of(lechSecondGoal.getEventId()), varReview.getRelatedEventIds());
        assertEquals(List.of(lechSecondGoal.getEventId()), disallowedGoal.getRelatedEventIds());

        assertEquals(
                List.of(legiaYellowCard.getEventId(), legiaRedCard.getEventId()),
                invalidatedCards.getRelatedEventIds()
        );

        playMatch(
                m1,
                lechFirstGoal,
                lechSecondGoal,
                legiaYellowCard,
                varReview,
                disallowedGoal,
                legiaRedCard,
                invalidatedCards,
                legiaPenaltyGoal,
                lechWinningGoal
        );

        assertEquals(9, m1.getEvents().size());
        assertEquals(9, testBroadcaster.getReceivedMessages().size());

        // Po anulowaniu drugiego gola Lecha i późniejszym golu na 88' wynik to 2:1.
        assertEquals(2, m1.getScore().getHomeGoals());
        assertEquals(1, m1.getScore().getAwayGoals());

        // Kartki Legii zostały cofnięte.
        assertEquals(0, m1.getScore().getAwayYellowCards());
        assertEquals(0, m1.getScore().getAwayRedCards());
        assertEquals(0, m1.getScore().getAwayFairPlayPenaltyPoints());

        assertEquals(1, m1.getScore().getGoalDifference());
        assertEquals(1, m1.getScore().getSignedGoalDifferenceFor(lech));
        assertEquals(-1, m1.getScore().getSignedGoalDifferenceFor(legia));

        assertEquals(
                "Lech Poznan 2 - 1 Legia Warszawa | Cards: Lech Poznan Y:0 R:0, Legia Warszawa Y:0 R:0",
                m1.getScore().display()
        );

        FootballResult m1Result = (FootballResult) m1.getResult();

        assertEquals(lech, m1Result.getWinner());
        assertFalse(m1Result.isDraw());
        assertTrue(m1Result.isCompletedNormally());
        assertFalse(m1Result.isWalkover());

        // =========================
        // 6. REPLAY SCORE DLA M1
        // =========================

        FootballMatch.FootballScore replayedM1Score = m1.replayScore();

        assertEquals(m1.getScore().getHomeGoals(), replayedM1Score.getHomeGoals());
        assertEquals(m1.getScore().getAwayGoals(), replayedM1Score.getAwayGoals());
        assertEquals(m1.getScore().getHomeYellowCards(), replayedM1Score.getHomeYellowCards());
        assertEquals(m1.getScore().getAwayYellowCards(), replayedM1Score.getAwayYellowCards());
        assertEquals(m1.getScore().getHomeRedCards(), replayedM1Score.getHomeRedCards());
        assertEquals(m1.getScore().getAwayRedCards(), replayedM1Score.getAwayRedCards());
        assertEquals(m1.getScore().display(), replayedM1Score.display());

        // =========================
        // 7. TIMELINE DLA M1
        // =========================

        List<ScoreSnapshot> m1Timeline = m1.replayTimeline();

        assertEquals(9, m1Timeline.size());

        // 12' Lech 1:0
        assertEquals(12, m1Timeline.get(0).minute());
        assertEquals("GOAL_SCORED", m1Timeline.get(0).eventName());
        assertEquals(1, m1Timeline.get(0).homeGoals());
        assertEquals(0, m1Timeline.get(0).awayGoals());

        // 20' Lech 2:0
        assertEquals(20, m1Timeline.get(1).minute());
        assertEquals(2, m1Timeline.get(1).homeGoals());
        assertEquals(0, m1Timeline.get(1).awayGoals());

        // 23' kartka Legii
        assertEquals(23, m1Timeline.get(2).minute());
        assertEquals(1, m1Timeline.get(2).awayYellowCards());

        // 24' VAR nic nie zmienia
        assertEquals(24, m1Timeline.get(3).minute());
        assertEquals("VAR_REVIEW", m1Timeline.get(3).eventName());
        assertEquals(2, m1Timeline.get(3).homeGoals());
        assertEquals(0, m1Timeline.get(3).awayGoals());

        // 26' anulowanie gola, wynik wraca z 2:0 na 1:0
        assertEquals(26, m1Timeline.get(4).minute());
        assertEquals("GOAL_DISALLOWED", m1Timeline.get(4).eventName());
        assertEquals(1, m1Timeline.get(4).homeGoals());
        assertEquals(0, m1Timeline.get(4).awayGoals());
        assertEquals(List.of(lechSecondGoal.getEventId()), m1Timeline.get(4).relatedEventIds());

        // 40' czerwona kartka Legii
        assertEquals(40, m1Timeline.get(5).minute());
        assertEquals(1, m1Timeline.get(5).awayYellowCards());
        assertEquals(1, m1Timeline.get(5).awayRedCards());

        // 45' anulowanie dwóch kartek
        assertEquals(45, m1Timeline.get(6).minute());
        assertEquals("EVENT_INVALIDATED", m1Timeline.get(6).eventName());
        assertEquals(0, m1Timeline.get(6).awayYellowCards());
        assertEquals(0, m1Timeline.get(6).awayRedCards());

        // 55' Legia 1:1
        assertEquals(55, m1Timeline.get(7).minute());
        assertEquals("PENALTY_SCORED", m1Timeline.get(7).eventName());
        assertEquals(1, m1Timeline.get(7).homeGoals());
        assertEquals(1, m1Timeline.get(7).awayGoals());

        // 88' Lech 2:1
        assertEquals(88, m1Timeline.get(8).minute());
        assertEquals(2, m1Timeline.get(8).homeGoals());
        assertEquals(1, m1Timeline.get(8).awayGoals());

        m1.printTimeline();

        // =========================
        // 8. POZOSTAŁE MECZE LIGOWE
        // =========================

        // Raków remisuje z Jagiellonią 1:1.
        playMatch(
                m2,
                FootballEvents.goalScored(rakow, 10, "Rakow scores first"),
                FootballEvents.goalScored(jagiellonia, 75, "Jagiellonia equalizes"),
                FootballEvents.yellowCard(rakow, 80, "Rakow receives a yellow card")
        );

        assertTrue(m2.getResult().isDraw());
        assertNull(m2.getResult().getWinner());
        assertEquals(1, m2.getScore().getHomeYellowCards());

        // Pogoń wygrywa z Widzewem 2:0, nietrafiony karny nie zmienia wyniku.
        playMatch(
                m3,
                FootballEvents.goalScored(pogon, 20, "Pogon scores"),
                FootballEvents.goalScored(pogon, 62, "Pogon scores again"),
                FootballEvents.penaltyMissed(widzew, 70, "Widzew misses a penalty")
        );

        assertEquals(pogon, m3.getResult().getWinner());
        assertEquals(0, m3.getScore().getHomeGoals());
        assertEquals(2, m3.getScore().getAwayGoals());

        // Śląsk wygrywa z Górnikiem 1:0 i dostaje dwie żółte kartki.
        playMatch(
                m4,
                FootballEvents.goalScored(slask, 44, "Slask scores"),
                FootballEvents.yellowCard(slask, 60, "Slask receives a yellow card"),
                FootballEvents.yellowCard(slask, 65, "Slask receives another yellow card")
        );

        assertEquals(slask, m4.getResult().getWinner());
        assertEquals(2, m4.getScore().getHomeYellowCards());
        assertEquals(2, m4.getScore().getHomeFairPlayPenaltyPoints());

        // Lech wygrywa z Rakowem 2:1.
        playMatch(
                m5,
                FootballEvents.goalScored(lech, 15, "Lech scores"),
                FootballEvents.penaltyScored(lech, 33, "Lech scores from penalty"),
                FootballEvents.goalScored(rakow, 80, "Rakow scores")
        );

        assertEquals(lech, m5.getResult().getWinner());

        // Jagiellonia wygrywa z Legią 1:0.
        playMatch(
                m6,
                FootballEvents.goalScored(jagiellonia, 55, "Jagiellonia scores")
        );

        assertEquals(jagiellonia, m6.getResult().getWinner());

        // Pogoń remisuje ze Śląskiem 1:1.
        playMatch(
                m7,
                FootballEvents.goalScored(pogon, 5, "Pogon scores"),
                FootballEvents.goalScored(slask, 90, "Slask equalizes")
        );

        assertTrue(m7.getResult().isDraw());

        // Górnik remisuje z Widzewem 1:1.
        playMatch(
                m8,
                FootballEvents.goalScored(gornik, 18, "Gornik scores"),
                FootballEvents.goalScored(widzew, 78, "Widzew equalizes")
        );

        assertTrue(m8.getResult().isDraw());

        // =========================
        // 9. WALIDACJA RELATED EVENTS
        // GoalDisallowedEvent nie może cofnąć kartki.
        // =========================

        FootballMatch invalidRelatedMatch = (FootballMatch) factory.createLeagueMatch(lech, legia);

        FootballMatchEvent cardToWronglyDisallow = FootballEvents.yellowCard(
                legia,
                10,
                "Legia receives a yellow card"
        );

        FootballMatchEvent invalidDisallowedGoal = FootballEvents.goalDisallowed(
                lech,
                11,
                "Invalid attempt to disallow a yellow card",
                List.of(cardToWronglyDisallow.getEventId())
        );

        invalidRelatedMatch.startMatch();
        invalidRelatedMatch.recordEvent(cardToWronglyDisallow);

        IllegalArgumentException invalidRelatedException = assertThrows(
                IllegalArgumentException.class,
                () -> invalidRelatedMatch.recordEvent(invalidDisallowedGoal)
        );

        assertEquals(
                "Event cannot refer to the provided related events",
                invalidRelatedException.getMessage()
        );

        // =========================
        // 10. PODWÓJNE UNDO TEGO SAMEGO EVENTU
        // =========================

        FootballMatch doubleUndoMatch = (FootballMatch) factory.createLeagueMatch(lech, legia);

        FootballMatchEvent goalToUndo = FootballEvents.goalScored(
                lech,
                10,
                "Lech scores"
        );

        FootballMatchEvent firstUndo = FootballEvents.goalDisallowed(
                legia,
                20,
                "Goal is disallowed first time",
                List.of(goalToUndo.getEventId())
        );

        FootballMatchEvent secondUndo = FootballEvents.goalDisallowed(
                legia,
                25,
                "Goal is disallowed second time",
                List.of(goalToUndo.getEventId())
        );

        doubleUndoMatch.startMatch();
        doubleUndoMatch.recordEvent(goalToUndo);
        assertEquals(1, doubleUndoMatch.getScore().getHomeGoals());

        doubleUndoMatch.recordEvent(firstUndo);
        assertEquals(0, doubleUndoMatch.getScore().getHomeGoals());

        doubleUndoMatch.recordEvent(secondUndo);
        assertEquals(0, doubleUndoMatch.getScore().getHomeGoals());

        List<ScoreSnapshot> doubleUndoTimeline = doubleUndoMatch.replayTimeline();

        assertEquals(3, doubleUndoTimeline.size());
        assertEquals(1, doubleUndoTimeline.get(0).homeGoals());
        assertEquals(0, doubleUndoTimeline.get(1).homeGoals());
        assertEquals(0, doubleUndoTimeline.get(2).homeGoals());

        doubleUndoMatch.endMatch();

        // =========================
        // 11. WALKOWER W LIDZE
        // =========================

        FootballMatch walkoverMatch = (FootballMatch) factory.createLeagueMatch(slask, legia);

        walkoverMatch.assignWalkover(slask, 3, 0);

        assertEquals(MatchStatus.WALKOVER, walkoverMatch.getStatus());
        assertEquals(3, walkoverMatch.getScore().getHomeGoals());
        assertEquals(0, walkoverMatch.getScore().getAwayGoals());

        FootballResult walkoverResult = (FootballResult) walkoverMatch.getResult();

        assertEquals(slask, walkoverResult.getWinner());
        assertTrue(walkoverResult.isWalkover());
        assertFalse(walkoverResult.isCompletedNormally());
        assertEquals(legia, walkoverResult.getWalkoverLoser());

        leagueStage.addMatch(walkoverMatch);

        // =========================
        // 12. KONIEC FAZY LIGOWEJ I TABELA
        // =========================

        leagueStage.endStage();

        List<Competitor> advanced = leagueStage.getAdvancingCompetitors();

        assertEquals(4, advanced.size());

        assertTrue(advanced.contains(lech));
        assertTrue(advanced.contains(pogon));
        assertTrue(advanced.contains(slask));
        assertTrue(advanced.contains(jagiellonia));

        TableEntry topEntry = leagueStage.getTable().getRows().get(0);

        assertTrue(topEntry.getPoints() > 0);
        assertTrue(advanced.contains(topEntry.getTeam()));

        // =========================
        // 13. FAZA PUCHAROWA I BRACKET
        // =========================

        FootballKnockoutStage knockoutStage = new FootballKnockoutStage();

        tournament.addStage(knockoutStage);

        assertEquals(2, tournament.getStages().size());

        knockoutStage.startStage();

        Team semi1Home = (Team) advanced.get(0);
        Team semi1Away = (Team) advanced.get(3);
        Team semi2Home = (Team) advanced.get(1);
        Team semi2Away = (Team) advanced.get(2);

        FootballMatch semi1 = (FootballMatch) factory.createKnockoutMatch(semi1Home, semi1Away);
        FootballMatch semi2 = (FootballMatch) factory.createKnockoutMatch(semi2Home, semi2Away);

        knockoutStage.addMatch(semi1);
        knockoutStage.addMatch(semi2);

        BracketTree bracket = knockoutStage.getBrackets();

        bracket.setFirstRound(Arrays.asList(semi1, semi2));

        assertEquals(1, bracket.getRounds().size());
        assertEquals(2, bracket.getRounds().get(0).size());

        // =========================
        // 14. PÓŁFINAŁ 1
        // Testujemy normalny mecz pucharowy.
        // =========================

        playMatch(
                semi1,
                FootballEvents.goalScored(semi1Home, 22, "Semi-final home team scores"),
                FootballEvents.goalScored(semi1Home, 81, "Semi-final home team scores again")
        );

        FootballResult semi1Result = (FootballResult) semi1.getResult();

        assertEquals(semi1Home, semi1Result.getWinner());
        assertFalse(semi1Result.isWonViaPenalties());
        assertEquals(2, semi1.getScore().getHomeGoals());
        assertEquals(0, semi1.getScore().getAwayGoals());

        // =========================
        // 15. PÓŁFINAŁ 2 Z DOGRYWKĄ I KARNYMI
        // Testujemy:
        // - remis po golach
        // - dogrywkę
        // - serię karnych
        // - anulowanie trafionego karnego w serii
        // =========================

        semi2.startMatch();

        assertEquals(MatchStatus.IN_PROGRESS, semi2.getStatus());

        semi2.recordEvent(FootballEvents.goalScored(
                semi2Home,
                20,
                "Semi-final home team scores"
        ));

        semi2.recordEvent(FootballEvents.goalScored(
                semi2Away,
                90,
                "Semi-final away team equalizes"
        ));

        semi2.goToExtraTime();

        assertTrue(semi2.isExtraTimePlayed());

        semi2.startPenaltyShootout();

        assertTrue(semi2.isPenaltyShootoutStarted());

        FootballMatchEvent homePenalty1 = FootballEvents.shootoutPenaltyScored(
                semi2Home,
                121,
                "Home team scores first shootout penalty"
        );

        FootballMatchEvent awayPenalty1 = FootballEvents.shootoutPenaltyScored(
                semi2Away,
                122,
                "Away team scores first shootout penalty"
        );

        FootballMatchEvent awayPenalty2 = FootballEvents.shootoutPenaltyScored(
                semi2Away,
                123,
                "Away team scores second shootout penalty"
        );

        FootballMatchEvent homePenaltyMiss = FootballEvents.shootoutPenaltyMissed(
                semi2Home,
                124,
                "Home team misses second shootout penalty"
        );

        FootballMatchEvent wrongAwayPenalty = FootballEvents.shootoutPenaltyScored(
                semi2Away,
                125,
                "Away team scores third shootout penalty"
        );

        FootballMatchEvent disallowedShootoutPenalty = FootballEvents.penaltyDisallowed(
                semi2Home,
                126,
                "Referee disallows away third shootout penalty",
                List.of(wrongAwayPenalty.getEventId())
        );

        semi2.recordEvent(homePenalty1);
        semi2.recordEvent(awayPenalty1);
        semi2.recordEvent(awayPenalty2);
        semi2.recordEvent(homePenaltyMiss);
        semi2.recordEvent(wrongAwayPenalty);
        semi2.recordEvent(disallowedShootoutPenalty);

        semi2.endMatch();

        assertEquals(MatchStatus.COMPLETED, semi2.getStatus());

        assertEquals(1, semi2.getScore().getHomeGoals());
        assertEquals(1, semi2.getScore().getAwayGoals());

        // Away miało 3 trafione karne, ale trzeci został cofnięty, więc zostaje 2.
        assertEquals(1, semi2.getScore().getHomePenalties());
        assertEquals(2, semi2.getScore().getAwayPenalties());

        FootballResult semi2Result = (FootballResult) semi2.getResult();

        assertFalse(semi2Result.isDraw());
        assertTrue(semi2Result.isWonViaPenalties());
        assertEquals(semi2Away, semi2Result.getWinner());

        List<ScoreSnapshot> semi2Timeline = semi2.replayTimeline();

        assertEquals(8, semi2Timeline.size());

        ScoreSnapshot lastSemi2Snapshot = semi2Timeline.get(semi2Timeline.size() - 1);

        assertEquals(126, lastSemi2Snapshot.minute());
        assertEquals("PENALTY_DISALLOWED", lastSemi2Snapshot.eventName());
        assertEquals(1, lastSemi2Snapshot.homePenalties());
        assertEquals(2, lastSemi2Snapshot.awayPenalties());
        assertEquals(List.of(wrongAwayPenalty.getEventId()), lastSemi2Snapshot.relatedEventIds());

        // =========================
        // 16. GENEROWANIE FINAŁU Z BRACKETA
        // =========================

        bracket.generateNextRound();

        assertEquals(2, bracket.getRounds().size());
        assertEquals(1, bracket.getRounds().get(1).size());

        FootballMatch finalMatch = bracket.getRounds().get(1).get(0);

        assertEquals(semi1.getResult().getWinner(), finalMatch.getHomeTeam());
        assertEquals(semi2.getResult().getWinner(), finalMatch.getAwayTeam());
        assertEquals(MatchStatus.SCHEDULED, finalMatch.getStatus());

        // =========================
        // 17. FINAŁ
        // Testujemy:
        // - gola anulowanego
        // - zwykłe gole
        // - replay finału
        // =========================

        FootballMatchEvent finalHomeGoal = FootballEvents.goalScored(
                finalMatch.getHomeTeam(),
                35,
                "Final home team scores first"
        );

        FootballMatchEvent finalAwayDisallowedGoal = FootballEvents.goalScored(
                finalMatch.getAwayTeam(),
                60,
                "Final away team scores but it will be disallowed"
        );

        FootballMatchEvent finalVarReview = FootballEvents.varReview(
                finalMatch.getAwayTeam(),
                61,
                "VAR checks away team goal",
                List.of(finalAwayDisallowedGoal.getEventId())
        );

        FootballMatchEvent finalGoalDisallowed = FootballEvents.goalDisallowed(
                finalMatch.getHomeTeam(),
                62,
                "VAR disallows away team goal",
                List.of(finalAwayDisallowedGoal.getEventId())
        );

        FootballMatchEvent finalAwayEqualizer = FootballEvents.goalScored(
                finalMatch.getAwayTeam(),
                89,
                "Final away team equalizes"
        );

        FootballMatchEvent finalAwayWinner = FootballEvents.goalScored(
                finalMatch.getAwayTeam(),
                118,
                "Final away team scores the winning goal"
        );

        playMatch(
                finalMatch,
                finalHomeGoal,
                finalAwayDisallowedGoal,
                finalVarReview,
                finalGoalDisallowed,
                FootballEvents.substitution(finalMatch.getAwayTeam(), 105, "Away team makes a substitution"),
                finalAwayEqualizer,
                finalAwayWinner
        );

        assertEquals(MatchStatus.COMPLETED, finalMatch.getStatus());
        assertFalse(finalMatch.getResult().isDraw());
        assertEquals(finalMatch.getAwayTeam(), finalMatch.getResult().getWinner());

        assertEquals(1, finalMatch.getScore().getHomeGoals());
        assertEquals(2, finalMatch.getScore().getAwayGoals());

        FootballMatch.FootballScore replayedFinalScore = finalMatch.replayScore();

        assertEquals(finalMatch.getScore().display(), replayedFinalScore.display());

        List<ScoreSnapshot> finalTimeline = finalMatch.replayTimeline();

        assertEquals(7, finalTimeline.size());

        // 35' 1:0
        assertEquals(1, finalTimeline.get(0).homeGoals());
        assertEquals(0, finalTimeline.get(0).awayGoals());

        // 60' 1:1 chwilowo
        assertEquals(1, finalTimeline.get(1).homeGoals());
        assertEquals(1, finalTimeline.get(1).awayGoals());

        // 61' VAR, nadal 1:1
        assertEquals("VAR_REVIEW", finalTimeline.get(2).eventName());
        assertEquals(1, finalTimeline.get(2).homeGoals());
        assertEquals(1, finalTimeline.get(2).awayGoals());

        // 62' anulowanie, wraca na 1:0
        assertEquals("GOAL_DISALLOWED", finalTimeline.get(3).eventName());
        assertEquals(1, finalTimeline.get(3).homeGoals());
        assertEquals(0, finalTimeline.get(3).awayGoals());

        // 89' 1:1
        assertEquals(1, finalTimeline.get(5).homeGoals());
        assertEquals(1, finalTimeline.get(5).awayGoals());

        // 118' 1:2
        assertEquals(1, finalTimeline.get(6).homeGoals());
        assertEquals(2, finalTimeline.get(6).awayGoals());

        finalMatch.printTimeline();

        // =========================
        // 18. KONIEC FAZY PUCHAROWEJ
        // =========================

        knockoutStage.endStage();

        assertEquals(1, knockoutStage.getAdvancingCompetitors().size());
        assertEquals(finalMatch.getResult().getWinner(), knockoutStage.getAdvancingCompetitors().get(0));

        // =========================
        // 19. KONIEC TURNIEJU
        // =========================

        tournament.setOverallWinner(finalMatch.getResult().getWinner());

        tournament.endTournament();

        assertNotNull(tournament.getOverallWinner());
        assertEquals(finalMatch.getAwayTeam(), tournament.getOverallWinner());

        // Zwycięzca turnieju musi być zwycięzcą finału.
        assertEquals(finalMatch.getResult().getWinner(), tournament.getOverallWinner());
    }

    private Team team(String id, String name) {
        return Team.builder()
                .id(id)
                .name(name)
                .players(List.of("Player 1", "Player 2", "Player 3"))
                .manager("Manager " + name)
                .build();
    }

    private void playMatch(FootballMatch match, FootballMatchEvent... events) {
        assertEquals(MatchStatus.SCHEDULED, match.getStatus());

        match.startMatch();

        assertEquals(MatchStatus.IN_PROGRESS, match.getStatus());

        for (FootballMatchEvent event : events) {
            match.recordEvent(event);
        }

        match.endMatch();

        assertEquals(MatchStatus.COMPLETED, match.getStatus());
    }

    private static class TestMatchBroadcaster implements MatchEventListener {
        private final List<String> receivedMessages = new ArrayList<>();

        @Override
        public void onEventRecorded(MatchEvent event, Score currentScore) {
            // Zbieramy komunikaty observera do późniejszych asercji.
            receivedMessages.add(event.getDescription() + " | " + currentScore.display());
        }

        public List<String> getReceivedMessages() {
            return receivedMessages;
        }
    }
}