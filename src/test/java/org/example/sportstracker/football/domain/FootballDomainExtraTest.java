package org.example.sportstracker.football.domain;

import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.MatchEvent;
import org.example.sportstracker.core.domain.match.MatchFactory;
import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.football.domain.bracket.BracketTree;
import org.example.sportstracker.football.domain.competitor.Team;
import org.example.sportstracker.football.domain.match.FootballEventType;
import org.example.sportstracker.football.domain.match.FootballMatch;
import org.example.sportstracker.football.domain.match.FootballMatchEvent;
import org.example.sportstracker.football.domain.match.FootballMatchFactory;
import org.example.sportstracker.football.domain.result.FootballResult;
import org.example.sportstracker.football.domain.score.FootballScore;
import org.example.sportstracker.football.domain.stage.FootballKnockoutStage;
import org.example.sportstracker.football.domain.stage.FootballLeagueStage;
import org.example.sportstracker.football.domain.table.LeagueTable;
import org.example.sportstracker.football.domain.table.TableEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FootballDomainExtraTest {

    @Test
    void shouldUseTimestampAndRelatedEventIdForVarCorrection() {
        Team home = team("1", "Home");
        Team away = team("2", "Away");

        MatchFactory factory = new FootballMatchFactory();
        FootballMatch match = (FootballMatch) factory.createLeagueMatch(home, away);

        FootballMatchEvent goal = FootballMatchEvent.builder()
                .eventType(FootballEventType.GOAL_SCORED)
                .actor(away)
                .minute(20)
                .description("Away scores")
                .build();

        String relatedId = goal.getTimestamp().toString();

        FootballMatchEvent varReview = FootballMatchEvent.builder()
                .eventType(FootballEventType.VAR_REVIEW)
                .actor(away)
                .minute(21)
                .description("VAR checks the goal")
                .relatedEventId(relatedId)
                .build();

        FootballMatchEvent disallowedGoal = FootballMatchEvent.builder()
                .eventType(FootballEventType.GOAL_DISALLOWED)
                .actor(away)
                .minute(22)
                .description("Goal disallowed")
                .relatedEventId(relatedId)
                .build();

        assertNotNull(goal.getTimestamp());
        assertNotNull(varReview.getTimestamp());
        assertNotNull(disallowedGoal.getTimestamp());

        assertEquals(relatedId, varReview.getRelatedEventId());
        assertEquals(relatedId, disallowedGoal.getRelatedEventId());

        match.startMatch();
        match.recordEvent(goal);
        match.recordEvent(varReview);
        match.recordEvent(disallowedGoal);
        match.endMatch();

        assertEquals(0, match.getScore().getHomeGoals());
        assertEquals(0, match.getScore().getAwayGoals());
        assertTrue(match.getResult().isDraw());
        assertEquals(MatchStatus.COMPLETED, match.getStatus());
    }

    @Test
    void shouldCountAllFootballScoreStatistics() {
        Team home = team("1", "Home");
        Team away = team("2", "Away");

        FootballScore score = new FootballScore(home, away);

        score.update(event(FootballEventType.GOAL_SCORED, home, 10, "Home goal"));
        score.update(event(FootballEventType.GOAL_SCORED, away, 20, "Away goal"));
        score.update(event(FootballEventType.PENALTY_SCORED, home, 30, "Home penalty goal"));
        score.update(event(FootballEventType.YELLOW_CARD, home, 40, "Home yellow"));
        score.update(event(FootballEventType.YELLOW_CARD, away, 41, "Away yellow"));
        score.update(event(FootballEventType.RED_CARD, away, 60, "Away red"));

        assertEquals(2, score.getHomeGoals());
        assertEquals(1, score.getAwayGoals());

        assertEquals(1, score.getHomeYellowCards());
        assertEquals(1, score.getAwayYellowCards());
        assertEquals(0, score.getHomeRedCards());
        assertEquals(1, score.getAwayRedCards());

        assertEquals(1, score.getHomeFairPlayPenaltyPoints());
        assertEquals(4, score.getAwayFairPlayPenaltyPoints());

        assertEquals(1, score.getGoalDifference());
    }

    @Test
    void shouldNotDecreaseGoalsBelowZeroWhenGoalIsDisallowedWithoutExistingGoal() {
        Team home = team("1", "Home");
        Team away = team("2", "Away");

        FootballScore score = new FootballScore(home, away);

        score.update(event(FootballEventType.GOAL_DISALLOWED, home, 5, "Invalid disallow"));
        score.update(event(FootballEventType.GOAL_DISALLOWED, away, 6, "Invalid disallow"));

        assertEquals(0, score.getHomeGoals());
        assertEquals(0, score.getAwayGoals());
    }

    @Test
    void shouldResolveLeagueDrawAndKnockoutDrawDifferently() {
        Team home = team("1", "Home");
        Team away = team("2", "Away");

        MatchFactory factory = new FootballMatchFactory();

        FootballMatch leagueMatch = (FootballMatch) factory.createLeagueMatch(home, away);
        playMatch(
                leagueMatch,
                event(FootballEventType.GOAL_SCORED, home, 10, "Home goal"),
                event(FootballEventType.GOAL_SCORED, away, 20, "Away goal")
        );

        assertTrue(leagueMatch.getResult().isDraw());
        assertNull(leagueMatch.getResult().getWinner());

        FootballMatch knockoutMatch = (FootballMatch) factory.createKnockoutMatch(home, away);
        playMatch(
                knockoutMatch,
                event(FootballEventType.GOAL_SCORED, home, 10, "Home goal"),
                event(FootballEventType.GOAL_SCORED, away, 20, "Away goal")
        );

        FootballResult knockoutResult = (FootballResult) knockoutMatch.getResult();

        assertFalse(knockoutResult.isDraw());
        assertTrue(knockoutResult.isWonViaPenalties());
        assertEquals(away, knockoutResult.getWinner());
    }

    @Test
    void shouldSortLeagueTableByPointsGoalDifferenceGoalsScoredAndFairPlay() {
        Team cleanTeam = team("1", "Clean Team");
        Team dirtyTeam = team("2", "Dirty Team");
        Team weakTeam = team("3", "Weak Team");

        LeagueTable table = new LeagueTable();

        table.registerTeam(cleanTeam);
        table.registerTeam(dirtyTeam);
        table.registerTeam(weakTeam);

        MatchFactory factory = new FootballMatchFactory();

        FootballMatch cleanWin = (FootballMatch) factory.createLeagueMatch(cleanTeam, weakTeam);
        playMatch(
                cleanWin,
                event(FootballEventType.GOAL_SCORED, cleanTeam, 10, "Clean 1"),
                event(FootballEventType.GOAL_SCORED, cleanTeam, 20, "Clean 2")
        );

        FootballMatch dirtyWin = (FootballMatch) factory.createLeagueMatch(dirtyTeam, weakTeam);
        playMatch(
                dirtyWin,
                event(FootballEventType.GOAL_SCORED, dirtyTeam, 10, "Dirty 1"),
                event(FootballEventType.GOAL_SCORED, dirtyTeam, 20, "Dirty 2"),
                event(FootballEventType.YELLOW_CARD, dirtyTeam, 30, "Dirty yellow"),
                event(FootballEventType.RED_CARD, dirtyTeam, 40, "Dirty red")
        );

        table.processMatch(cleanWin);
        table.processMatch(dirtyWin);
        table.sortByPointsAndGoalDifference();

        List<TableEntry> rows = table.getRows();

        assertEquals(cleanTeam, rows.get(0).getTeam());
        assertEquals(dirtyTeam, rows.get(1).getTeam());
        assertEquals(weakTeam, rows.get(2).getTeam());

        assertEquals(0, rows.get(0).getFairPlayPenaltyPoints());
        assertEquals(4, rows.get(1).getFairPlayPenaltyPoints());
    }

    @Test
    void shouldThrowWhenLeagueStageEndsWithUnfinishedMatch() {
        Team home = team("1", "Home");
        Team away = team("2", "Away");

        MatchFactory factory = new FootballMatchFactory();

        FootballLeagueStage stage = new FootballLeagueStage(Arrays.asList(home, away));
        FootballMatch unfinishedMatch = (FootballMatch) factory.createLeagueMatch(home, away);

        stage.addMatch(unfinishedMatch);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                stage::endStage
        );

        assertEquals("Nie wszystkie mecze ligowe zostały zakończone", exception.getMessage());
    }

    @Test
    void shouldGenerateNextBracketRoundFromCompletedMatches() {
        Team a = team("1", "A");
        Team b = team("2", "B");
        Team c = team("3", "C");
        Team d = team("4", "D");

        MatchFactory factory = new FootballMatchFactory();

        FootballMatch semi1 = (FootballMatch) factory.createKnockoutMatch(a, b);
        FootballMatch semi2 = (FootballMatch) factory.createKnockoutMatch(c, d);

        playMatch(semi1, event(FootballEventType.GOAL_SCORED, a, 10, "A goal"));
        playMatch(semi2, event(FootballEventType.GOAL_SCORED, d, 10, "D goal"));

        BracketTree bracket = new BracketTree();
        bracket.setFirstRound(Arrays.asList(semi1, semi2));

        bracket.generateNextRound();

        assertEquals(2, bracket.getRounds().size());
        assertEquals(1, bracket.getRounds().get(1).size());

        FootballMatch finalMatch = bracket.getRounds().get(1).get(0);

        assertEquals(a, finalMatch.getHomeTeam());
        assertEquals(d, finalMatch.getAwayTeam());
        assertEquals(MatchStatus.SCHEDULED, finalMatch.getStatus());
    }

    @Test
    void shouldThrowWhenGeneratingBracketRoundBeforeAllMatchesAreCompleted() {
        Team a = team("1", "A");
        Team b = team("2", "B");
        Team c = team("3", "C");
        Team d = team("4", "D");

        MatchFactory factory = new FootballMatchFactory();

        FootballMatch completed = (FootballMatch) factory.createKnockoutMatch(a, b);
        FootballMatch unfinished = (FootballMatch) factory.createKnockoutMatch(c, d);

        playMatch(completed, event(FootballEventType.GOAL_SCORED, a, 10, "A goal"));

        BracketTree bracket = new BracketTree();
        bracket.setFirstRound(Arrays.asList(completed, unfinished));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                bracket::generateNextRound
        );

        assertEquals("Nie wszystkie mecze w rundzie zostały zakończone!", exception.getMessage());
    }

    @Test
    void shouldRejectWrongEventTypeForFootballMatch() {
        Team home = team("1", "Home");
        Team away = team("2", "Away");

        MatchFactory factory = new FootballMatchFactory();
        FootballMatch match = (FootballMatch) factory.createLeagueMatch(home, away);

        MatchEvent wrongEvent = new FakeMatchEvent(home);

        match.startMatch();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> match.recordEvent(wrongEvent)
        );

        assertEquals("Nieprawidłowy typ zdarzenia dla meczu piłkarskiego", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAbandoningMatchThatIsNotInProgress() {
        Team home = team("1", "Home");
        Team away = team("2", "Away");

        MatchFactory factory = new FootballMatchFactory();

        FootballMatch scheduledMatch = (FootballMatch) factory.createLeagueMatch(home, away);

        IllegalStateException scheduledException = assertThrows(
                IllegalStateException.class,
                () -> scheduledMatch.abandonMatch("Rain")
        );

        assertEquals("Można przerwać tylko trwający mecz", scheduledException.getMessage());

        FootballMatch completedMatch = (FootballMatch) factory.createLeagueMatch(home, away);
        playMatch(completedMatch);

        IllegalStateException completedException = assertThrows(
                IllegalStateException.class,
                () -> completedMatch.abandonMatch("Too late")
        );

        assertEquals("Można przerwać tylko trwający mecz", completedException.getMessage());
    }

    @Test
    void shouldCreateTeamsWithBuilderAndExposeCompetitorContract() {
        Team team = Team.builder()
                .id("10")
                .name("Builder FC")
                .players(List.of("Player A", "Player B"))
                .manager("Manager X")
                .build();

        Competitor competitor = team;

        assertEquals("10", competitor.getId());
        assertEquals("Builder FC", competitor.getName());
        assertEquals(2, team.getPlayers().size());
        assertEquals("Manager X", team.getManager());
    }

    @Test
    void shouldAdvanceWinnerFromTwoLeggedKnockoutTieByAggregateScore() {
        Team lech = team("1", "Lech Poznań");
        Team legia = team("2", "Legia Warszawa");

        MatchFactory factory = new FootballMatchFactory();

        FootballMatch firstLeg = (FootballMatch) factory.createKnockoutMatch(lech, legia);
        FootballMatch secondLeg = (FootballMatch) factory.createKnockoutMatch(legia, lech);

        playMatch(
                firstLeg,
                event(FootballEventType.GOAL_SCORED, lech, 10, "Lech 1:0"),
                event(FootballEventType.GOAL_SCORED, legia, 50, "Legia 1:1")
        );

        playMatch(
                secondLeg,
                event(FootballEventType.GOAL_SCORED, lech, 70, "Lech wygrywa rewanż 0:1")
        );

        FootballKnockoutStage stage = new FootballKnockoutStage();
        stage.setTwoLegged(true);

        stage.addMatch(firstLeg);
        stage.addMatch(secondLeg);

        stage.endStage();

        assertEquals(1, stage.getAdvancingCompetitors().size());
        assertEquals(lech, stage.getAdvancingCompetitors().get(0));
    }

    @Test
    void shouldAdvanceWinnerFromTwoLeggedKnockoutTieByPenaltiesWhenAggregateIsDrawn() {
        Team lech = team("1", "Lech Poznań");
        Team legia = team("2", "Legia Warszawa");

        MatchFactory factory = new FootballMatchFactory();

        FootballMatch firstLeg = (FootballMatch) factory.createKnockoutMatch(lech, legia);
        FootballMatch secondLeg = (FootballMatch) factory.createKnockoutMatch(legia, lech);

        playMatch(
                firstLeg,
                event(FootballEventType.GOAL_SCORED, lech, 10, "Lech 1:0")
        );

        secondLeg.startMatch();

        secondLeg.recordEvent(event(FootballEventType.GOAL_SCORED, legia, 30, "Legia 1:0"));

        secondLeg.goToExtraTime();
        secondLeg.startPenaltyShootout();

        secondLeg.recordEvent(event(FootballEventType.SHOOTOUT_PENALTY_SCORED, legia, 121, "Legia trafia karnego"));
        secondLeg.recordEvent(event(FootballEventType.SHOOTOUT_PENALTY_SCORED, legia, 122, "Legia trafia drugiego karnego"));
        secondLeg.recordEvent(event(FootballEventType.SHOOTOUT_PENALTY_SCORED, lech, 123, "Lech trafia karnego"));

        secondLeg.endMatch();

        FootballKnockoutStage stage = new FootballKnockoutStage();
        stage.setTwoLegged(true);

        stage.addMatch(firstLeg);
        stage.addMatch(secondLeg);

        stage.endStage();

        assertEquals(1, stage.getAdvancingCompetitors().size());
        assertEquals(legia, stage.getAdvancingCompetitors().get(0));
    }

    private Team team(String id, String name) {
        return Team.builder()
                .id(id)
                .name(name)
                .players(List.of("Player 1", "Player 2"))
                .manager("Manager " + name)
                .build();
    }

    private FootballMatchEvent event(FootballEventType type, Team actor, int minute, String description) {
        return FootballMatchEvent.builder()
                .eventType(type)
                .actor(actor)
                .minute(minute)
                .description(description)
                .build();
    }

    private void playMatch(FootballMatch match, FootballMatchEvent... events) {
        assertEquals(MatchStatus.SCHEDULED, match.getStatus());

        match.startMatch();

        for (FootballMatchEvent event : events) {
            match.recordEvent(event);
        }

        match.endMatch();

        assertEquals(MatchStatus.COMPLETED, match.getStatus());
    }

    private static class FakeMatchEvent implements MatchEvent {
        private final Competitor actor;
        private final LocalDateTime timestamp = LocalDateTime.now();

        private FakeMatchEvent(Competitor actor) {
            this.actor = actor;
        }

        @Override
        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public Competitor getActor() {
            return actor;
        }

        @Override
        public String getDescription() {
            return "Fake event";
        }

        @Override
        public String getRelatedEventId() {
            return null;
        }
    }
}