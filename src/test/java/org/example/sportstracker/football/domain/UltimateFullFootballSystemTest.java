package org.example.sportstracker.football.domain;

import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.MatchEvent;
import org.example.sportstracker.core.domain.match.MatchEventListener;
import org.example.sportstracker.core.domain.match.MatchFactory;
import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.football.domain.bracket.BracketTree;
import org.example.sportstracker.football.domain.competitor.Team;
import org.example.sportstracker.football.domain.match.FootballEventType;
import org.example.sportstracker.football.domain.match.FootballMatch;
import org.example.sportstracker.football.domain.match.FootballMatchEvent;
import org.example.sportstracker.football.domain.match.FootballMatchFactory;
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

class UltimateFullFootballSystemTest {

    @Test
    void shouldTestWholeFootballSystemEndToEndWithEverythingUsed() {
        /*
         * 1. Tworzymy uczestników turnieju.
         * Team implementuje core interface Competitor.
         * Dzięki temu core systemu zna tylko getId() i getName(),
         * a szczegóły piłkarskie siedzą w module football.
         */
        Team lech = team("1", "Lech Poznań");
        Team legia = team("2", "Legia Warszawa");
        Team rakow = team("3", "Raków Częstochowa");
        Team jagiellonia = team("4", "Jagiellonia");
        Team widzew = team("5", "Widzew Łódź");
        Team pogon = team("6", "Pogoń Szczecin");
        Team slask = team("7", "Śląsk Wrocław");
        Team gornik = team("8", "Górnik Zabrze");

        Competitor competitor = lech;

        assertEquals("1", competitor.getId());
        assertEquals("Lech Poznań", competitor.getName());
        assertEquals(3, lech.getPlayers().size());
        assertEquals("Manager Lech Poznań", lech.getManager());

        /*
         * 2. Tworzymy fabrykę meczów.
         * Factory Method ukrywa tworzenie FootballMatch.
         * Dla ligi fabryka daje LeagueMatchResolutionStrategy.
         * Dla pucharu fabryka daje KnockoutMatchResolutionStrategy.
         */
        MatchFactory factory = new FootballMatchFactory();

        /*
         * 3. Tworzymy turniej.
         * FootballTournament implementuje core interface Tournament.
         * Turniej będzie miał dwa etapy: ligowy i pucharowy.
         */
        FootballTournament tournament = new FootballTournament();
        tournament.setName("Ultimate Football Library Cup 2026");

        assertNull(tournament.getOverallWinner());

        tournament.startTournament();

        /*
         * 4. Tworzymy fazę ligową.
         * FootballLeagueStage dziedziczy po AbstractTournamentStage.
         * AbstractTournamentStage daje Template Method w endStage().
         *
         * pointsForWin = 3 oznacza klasyczną punktację piłkarską.
         */
        FootballLeagueStage leagueStage = new FootballLeagueStage(
                Arrays.asList(lech, legia, rakow, jagiellonia, widzew, pogon, slask, gornik),
                3
        );

        assertEquals(3, leagueStage.getPointsForWin());

        tournament.addStage(leagueStage);
        assertEquals(1, tournament.getStages().size());

        leagueStage.startStage();

        /*
         * 5. Tworzymy mecze ligowe przez fabrykę.
         * Każdy z tych meczów pozwala na remis.
         */
        FootballMatch m1 = (FootballMatch) factory.createLeagueMatch(lech, legia);
        FootballMatch m2 = (FootballMatch) factory.createLeagueMatch(rakow, jagiellonia);
        FootballMatch m3 = (FootballMatch) factory.createLeagueMatch(widzew, pogon);
        FootballMatch m4 = (FootballMatch) factory.createLeagueMatch(slask, gornik);
        FootballMatch m5 = (FootballMatch) factory.createLeagueMatch(lech, rakow);
        FootballMatch m6 = (FootballMatch) factory.createLeagueMatch(legia, jagiellonia);
        FootballMatch m7 = (FootballMatch) factory.createLeagueMatch(pogon, slask);
        FootballMatch m8 = (FootballMatch) factory.createLeagueMatch(gornik, widzew);

        /*
         * 6. Podpinamy observerów do meczu.
         * TestMatchBroadcaster zapisuje powiadomienia do listy.
         * ConsoleMatchBroadcaster wypisuje live update w konsoli.
         */
        TestMatchBroadcaster testBroadcaster = new TestMatchBroadcaster();

        m1.addMatchEventListener(testBroadcaster);
        m1.addMatchEventListener(new ConsoleMatchBroadcaster());

        /*
         * 7. Dodajemy wszystkie mecze do etapu ligowego.
         */
        List<FootballMatch> leagueMatches = List.of(m1, m2, m3, m4, m5, m6, m7, m8);
        leagueMatches.forEach(leagueStage::addMatch);

        assertEquals(8, leagueStage.getStageMatches().size());

        /*
         * 8. Przygotowujemy mecz z VAR.
         * Używamy timestamp jako prostego identyfikatora eventu.
         * relatedEventId łączy VAR i anulowanie gola z wcześniejszym golem.
         */
        FootballMatchEvent lechGoal = event(FootballEventType.GOAL_SCORED, lech, 12, "Lech strzela na 1:0");
        FootballMatchEvent legiaGoal = event(FootballEventType.GOAL_SCORED, legia, 30, "Legia wyrównuje");

        String relatedGoalId = legiaGoal.getTimestamp().toString();

        FootballMatchEvent varReview = eventWithRelatedId(
                FootballEventType.VAR_REVIEW,
                legia,
                31,
                "VAR sprawdza gola Legii",
                relatedGoalId
        );

        FootballMatchEvent disallowedGoal = eventWithRelatedId(
                FootballEventType.GOAL_DISALLOWED,
                legia,
                32,
                "Gol Legii anulowany",
                relatedGoalId
        );

        assertNotNull(lechGoal.getTimestamp());
        assertNotNull(legiaGoal.getTimestamp());
        assertEquals(relatedGoalId, varReview.getRelatedEventId());
        assertEquals(relatedGoalId, disallowedGoal.getRelatedEventId());

        /*
         * 9. Rozgrywamy pierwszy mecz.
         * Sprawdzamy gole, VAR, anulowanie gola, kartki, faul,
         * zmianę, observerów i wynik live.
         */
        playMatch(m1,
                lechGoal,
                legiaGoal,
                varReview,
                disallowedGoal,
                event(FootballEventType.YELLOW_CARD, lech, 50, "Żółta kartka dla Lecha"),
                event(FootballEventType.RED_CARD, legia, 70, "Czerwona kartka dla Legii"),
                event(FootballEventType.FOUL, legia, 75, "Faul taktyczny Legii"),
                event(FootballEventType.SUBSTITUTION, lech, 80, "Zmiana w Lechu"),
                event(FootballEventType.GOAL_SCORED, lech, 88, "Lech zamyka mecz")
        );

        assertEquals(9, testBroadcaster.getReceivedDescriptions().size());
        assertEquals(9, m1.getEvents().size());

        assertEquals(
                "Lech Poznań 2 - 0 Legia Warszawa | Kartki: Lech Poznań Ż:1 C:0, Legia Warszawa Ż:0 C:1",
                m1.getScore().display()
        );

        assertEquals(2, m1.getScore().getHomeGoals());
        assertEquals(0, m1.getScore().getAwayGoals());
        assertEquals(1, m1.getScore().getHomeYellowCards());
        assertEquals(1, m1.getScore().getAwayRedCards());
        assertEquals(1, m1.getScore().getHomeFairPlayPenaltyPoints());
        assertEquals(3, m1.getScore().getAwayFairPlayPenaltyPoints());
        assertEquals(2, m1.getScore().getGoalDifference());
        assertEquals(2, m1.getScore().getSignedGoalDifferenceFor(lech));
        assertEquals(-2, m1.getScore().getSignedGoalDifferenceFor(legia));

        assertEquals(lech, m1.getResult().getWinner());
        assertFalse(m1.getResult().isDraw());
        assertTrue(m1.getResult().isCompletedNormally());

        /*
         * 10. Rozgrywamy pozostałe mecze ligi.
         * Są tu remisy, wygrane, kartki i niewykorzystany karny.
         */
        playMatch(m2,
                event(FootballEventType.GOAL_SCORED, rakow, 10, "Raków 1:0"),
                event(FootballEventType.GOAL_SCORED, jagiellonia, 75, "Jagiellonia 1:1"),
                event(FootballEventType.YELLOW_CARD, rakow, 80, "Kartka dla Rakowa")
        );

        assertTrue(m2.getResult().isDraw());
        assertNull(m2.getResult().getWinner());

        playMatch(m3,
                event(FootballEventType.GOAL_SCORED, pogon, 20, "Pogoń 0:1"),
                event(FootballEventType.GOAL_SCORED, pogon, 62, "Pogoń 0:2"),
                event(FootballEventType.PENALTY_MISSED, widzew, 70, "Widzew nie trafia karnego")
        );

        assertEquals(pogon, m3.getResult().getWinner());

        playMatch(m4,
                event(FootballEventType.GOAL_SCORED, slask, 44, "Śląsk 1:0"),
                event(FootballEventType.YELLOW_CARD, slask, 60, "Kartka Śląska"),
                event(FootballEventType.YELLOW_CARD, slask, 65, "Druga kartka Śląska")
        );

        playMatch(m5,
                event(FootballEventType.GOAL_SCORED, lech, 15, "Lech 1:0"),
                event(FootballEventType.PENALTY_SCORED, lech, 33, "Lech 2:0 z karnego w meczu"),
                event(FootballEventType.GOAL_SCORED, rakow, 80, "Raków 2:1")
        );

        playMatch(m6,
                event(FootballEventType.GOAL_SCORED, jagiellonia, 55, "Jagiellonia 0:1")
        );

        playMatch(m7,
                event(FootballEventType.GOAL_SCORED, pogon, 5, "Pogoń 1:0"),
                event(FootballEventType.GOAL_SCORED, slask, 90, "Śląsk 1:1")
        );

        playMatch(m8,
                event(FootballEventType.GOAL_SCORED, gornik, 18, "Górnik 1:0"),
                event(FootballEventType.GOAL_SCORED, widzew, 78, "Widzew 1:1")
        );

        /*
         * 11. Dodajemy walkower.
         * To pokazuje rozdzielenie live Score od końcowego Result.
         * Wynik jest ustawiony administracyjnie, a mecz dostaje status WALKOVER.
         */
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

        /*
         * 12. Liczymy tabelę ręcznie przez calculateStandings(),
         * a potem zamykamy etap przez endStage().
         * endStage() używa Template Method.
         */
        leagueStage.calculateStandings();
        assertFalse(leagueStage.getTable().getRows().isEmpty());

        leagueStage.endStage();

        List<Competitor> advanced = leagueStage.getAdvancingCompetitors();

        assertEquals(4, advanced.size());
        assertTrue(advanced.contains(lech));
        assertTrue(advanced.contains(pogon));
        assertTrue(advanced.contains(slask));
        assertTrue(advanced.contains(jagiellonia));

        /*
         * 13. Sprawdzamy spadkowiczów.
         * Nie sprawdzamy dokładnej kolejności, bo zależy od wyników i tie-breakerów.
         */
        List<Team> relegatedTeams = leagueStage.getRelegatedTeams();

        assertEquals(2, relegatedTeams.size());
        assertTrue(relegatedTeams.contains(widzew) || relegatedTeams.contains(gornik) || relegatedTeams.contains(legia));

        TableEntry topEntry = leagueStage.getTable().getRows().get(0);
        assertTrue(topEntry.getPoints() > 0);
        assertTrue(advanced.contains(topEntry.getTeam()));

        /*
         * 14. Osobno sprawdzamy custom pointsForWin.
         * Tu zwycięstwo daje 2 punkty zamiast 3.
         */
        FootballLeagueStage customPointsStage = new FootballLeagueStage(List.of(lech, legia), 2);
        FootballMatch customPointsMatch = (FootballMatch) factory.createLeagueMatch(lech, legia);

        playMatch(customPointsMatch, event(FootballEventType.GOAL_SCORED, lech, 10, "Lech wins"));

        customPointsStage.addMatch(customPointsMatch);
        customPointsStage.endStage();

        assertEquals(2, customPointsStage.getPointsForWin());
        assertEquals(2, customPointsStage.getTable().getRows().get(0).getPoints());

        /*
         * 15. Negatywny test etapu pucharowego.
         * Nie można zamknąć fazy, jeśli mecz nie jest zakończony.
         */
        FootballKnockoutStage unfinishedStage = new FootballKnockoutStage();
        FootballMatch unfinished = (FootballMatch) factory.createKnockoutMatch(lech, legia);

        unfinishedStage.addMatch(unfinished);

        IllegalStateException unfinishedStageException = assertThrows(
                IllegalStateException.class,
                unfinishedStage::endStage
        );

        assertEquals("Nie wszystkie mecze pucharowe zostały zakończone", unfinishedStageException.getMessage());

        /*
         * 16. Tworzymy fazę pucharową.
         */
        FootballKnockoutStage knockoutStage = new FootballKnockoutStage();

        tournament.addStage(knockoutStage);
        knockoutStage.startStage();

        assertEquals(2, tournament.getStages().size());

        Team semi1Home = (Team) advanced.get(0);
        Team semi1Away = (Team) advanced.get(3);
        Team semi2Home = (Team) advanced.get(1);
        Team semi2Away = (Team) advanced.get(2);

        FootballMatch semi1 = (FootballMatch) factory.createKnockoutMatch(semi1Home, semi1Away);
        FootballMatch semi2 = (FootballMatch) factory.createKnockoutMatch(semi2Home, semi2Away);

        knockoutStage.addMatch(semi1);
        knockoutStage.addMatch(semi2);

        /*
         * 17. Tworzymy drabinkę.
         * Pierwsza runda to dwa półfinały.
         */
        BracketTree bracket = knockoutStage.getBrackets();
        bracket.setFirstRound(Arrays.asList(semi1, semi2));

        assertEquals(1, bracket.getRounds().size());
        assertEquals(2, bracket.getRounds().get(0).size());

        /*
         * 18. Pierwszy półfinał kończy się zwykłym zwycięstwem.
         */
        playMatch(semi1,
                event(FootballEventType.GOAL_SCORED, semi1Home, 22, "Lider strzela w półfinale"),
                event(FootballEventType.GOAL_SCORED, semi1Home, 81, "Lider dobija rywala")
        );

        assertEquals(semi1Home, semi1.getResult().getWinner());
        assertFalse(((FootballResult) semi1.getResult()).isWonViaPenalties());

        /*
         * 19. Drugi półfinał kończy się remisem,
         * więc używamy dogrywki i serii rzutów karnych.
         */
        semi2.startMatch();

        semi2.recordEvent(event(FootballEventType.GOAL_SCORED, semi2Home, 20, "Półfinał 1:0"));
        semi2.recordEvent(event(FootballEventType.GOAL_SCORED, semi2Away, 90, "Półfinał 1:1"));

        semi2.goToExtraTime();
        assertTrue(semi2.isExtraTimePlayed());

        semi2.startPenaltyShootout();
        assertTrue(semi2.isPenaltyShootoutStarted());

        semi2.recordEvent(event(FootballEventType.SHOOTOUT_PENALTY_SCORED, semi2Home, 121, "Gospodarze trafiają karnego"));
        semi2.recordEvent(event(FootballEventType.SHOOTOUT_PENALTY_SCORED, semi2Away, 122, "Goście trafiają karnego"));
        semi2.recordEvent(event(FootballEventType.SHOOTOUT_PENALTY_SCORED, semi2Away, 123, "Goście trafiają drugiego karnego"));
        semi2.recordEvent(event(FootballEventType.SHOOTOUT_PENALTY_MISSED, semi2Home, 124, "Gospodarze pudłują karnego"));

        semi2.endMatch();

        FootballResult semi2Result = (FootballResult) semi2.getResult();

        assertFalse(semi2Result.isDraw());
        assertTrue(semi2Result.isWonViaPenalties());
        assertEquals(semi2Away, semi2Result.getWinner());
        assertEquals(1, semi2.getScore().getHomePenalties());
        assertEquals(2, semi2.getScore().getAwayPenalties());

        /*
         * 20. Generujemy finał z drabinki.
         * BracketTree bierze zwycięzców półfinałów i tworzy kolejny mecz.
         */
        bracket.generateNextRound();

        assertEquals(2, bracket.getRounds().size());
        assertEquals(1, bracket.getRounds().get(1).size());

        FootballMatch finalMatch = bracket.getRounds().get(1).get(0);

        assertEquals(semi1.getResult().getWinner(), finalMatch.getHomeTeam());
        assertEquals(semi2.getResult().getWinner(), finalMatch.getAwayTeam());
        assertEquals(MatchStatus.SCHEDULED, finalMatch.getStatus());

        /*
         * 21. Rozgrywamy finał.
         */
        playMatch(finalMatch,
                event(FootballEventType.GOAL_SCORED, finalMatch.getHomeTeam(), 35, "Finał 1:0"),
                event(FootballEventType.GOAL_SCORED, finalMatch.getAwayTeam(), 89, "Finał 1:1"),
                event(FootballEventType.SUBSTITUTION, finalMatch.getAwayTeam(), 105, "Zmiana przed dogrywką"),
                event(FootballEventType.GOAL_SCORED, finalMatch.getAwayTeam(), 118, "Gol na wagę pucharu")
        );

        assertEquals(MatchStatus.COMPLETED, finalMatch.getStatus());
        assertFalse(finalMatch.getResult().isDraw());
        assertEquals(finalMatch.getAwayTeam(), finalMatch.getResult().getWinner());

        /*
         * 22. Zamykamy fazę pucharową.
         * Zwycięzca finału staje się zwycięzcą etapu.
         */
        knockoutStage.endStage();

        assertEquals(1, knockoutStage.getAdvancingCompetitors().size());
        assertEquals(finalMatch.getResult().getWinner(), knockoutStage.getAdvancingCompetitors().get(0));

        /*
         * 23. Sprawdzamy advanceWinner bez drabinki.
         * Jeśli stage nie ma ustawionej drabinki, bierze zwycięzców z listy meczów.
         */
        FootballKnockoutStage directKnockoutStage = new FootballKnockoutStage();
        FootballMatch directMatch = (FootballMatch) factory.createKnockoutMatch(lech, gornik);

        playMatch(directMatch, event(FootballEventType.GOAL_SCORED, lech, 10, "Lech direct win"));

        directKnockoutStage.addMatch(directMatch);
        directKnockoutStage.endStage();

        assertEquals(1, directKnockoutStage.getAdvancingCompetitors().size());
        assertEquals(lech, directKnockoutStage.getAdvancingCompetitors().get(0));

        /*
         * 24. Kończymy cały turniej.
         */
        tournament.setOverallWinner(finalMatch.getResult().getWinner());
        tournament.endTournament();

        assertNotNull(tournament.getOverallWinner());
        assertEquals(finalMatch.getAwayTeam(), tournament.getOverallWinner());
        /*
         * 25. Testujemy przerwany mecz.
         * Status przechodzi na ABANDONED, ale live score zostaje.
         */
        FootballMatch abandonedMatch = (FootballMatch) factory.createLeagueMatch(lech, gornik);

        abandonedMatch.startMatch();
        abandonedMatch.recordEvent(event(FootballEventType.GOAL_SCORED, lech, 10, "Gol przed przerwaniem"));
        abandonedMatch.abandonMatch("Burza i zalane boisko");

        assertEquals(MatchStatus.ABANDONED, abandonedMatch.getStatus());
        assertEquals("Burza i zalane boisko", abandonedMatch.getAbandonReason());
        assertEquals(1, abandonedMatch.getScore().getHomeGoals());

        /*
         * 26. Walidacja maszyny stanów i błędnych operacji.
         */
        FootballMatch invalidStart = (FootballMatch) factory.createLeagueMatch(lech, legia);

        invalidStart.startMatch();

        IllegalStateException startAgainException = assertThrows(
                IllegalStateException.class,
                invalidStart::startMatch
        );

        assertEquals("Mecz nie jest zaplanowany", startAgainException.getMessage());

        FootballMatch invalidEnd = (FootballMatch) factory.createLeagueMatch(lech, legia);

        IllegalStateException endScheduledException = assertThrows(
                IllegalStateException.class,
                invalidEnd::endMatch
        );

        assertEquals("Tylko trwający mecz można zakończyć", endScheduledException.getMessage());

        FootballMatch invalidEventBeforeStart = (FootballMatch) factory.createLeagueMatch(lech, legia);

        IllegalStateException eventBeforeStartException = assertThrows(
                IllegalStateException.class,
                () -> invalidEventBeforeStart.recordEvent(
                        event(FootballEventType.GOAL_SCORED, lech, 1, "Nielegalny gol")
                )
        );

        assertEquals("Zdarzenia można dodawać tylko podczas trwania meczu", eventBeforeStartException.getMessage());

        FootballMatch invalidEventAfterEnd = (FootballMatch) factory.createLeagueMatch(lech, legia);

        playMatch(invalidEventAfterEnd);

        IllegalStateException eventAfterEndException = assertThrows(
                IllegalStateException.class,
                () -> invalidEventAfterEnd.recordEvent(
                        event(FootballEventType.GOAL_SCORED, lech, 99, "Za późno")
                )
        );

        assertEquals("Zdarzenia można dodawać tylko podczas trwania meczu", eventAfterEndException.getMessage());

        FootballMatch invalidAbandon = (FootballMatch) factory.createLeagueMatch(lech, legia);

        IllegalStateException invalidAbandonException = assertThrows(
                IllegalStateException.class,
                () -> invalidAbandon.abandonMatch("Too early")
        );

        assertEquals("Można przerwać tylko trwający mecz", invalidAbandonException.getMessage());

        FootballMatch invalidExtraTime = (FootballMatch) factory.createKnockoutMatch(lech, legia);

        IllegalStateException invalidExtraTimeException = assertThrows(
                IllegalStateException.class,
                invalidExtraTime::goToExtraTime
        );

        assertEquals("Dogrywkę można rozpocząć tylko w trwającym meczu", invalidExtraTimeException.getMessage());

        FootballMatch invalidShootout = (FootballMatch) factory.createKnockoutMatch(lech, legia);

        IllegalStateException invalidShootoutException = assertThrows(
                IllegalStateException.class,
                invalidShootout::startPenaltyShootout
        );

        assertEquals("Karne można rozpocząć tylko w trwającym meczu", invalidShootoutException.getMessage());

        /*
         * 27. Test złego typu eventu.
         * FootballMatch przyjmuje tylko FootballMatchEvent.
         */
        FootballMatch wrongEventMatch = (FootballMatch) factory.createLeagueMatch(lech, legia);
        wrongEventMatch.startMatch();

        IllegalArgumentException wrongEventException = assertThrows(
                IllegalArgumentException.class,
                () -> wrongEventMatch.recordEvent(new FakeMatchEvent(lech))
        );

        assertEquals("Nieprawidłowy typ zdarzenia dla meczu piłkarskiego", wrongEventException.getMessage());

        /*
         * 28. Test złego zwycięzcy walkowera.
         * Walkower może wygrać tylko drużyna grająca w meczu.
         */
        FootballMatch invalidWalkoverWinner = (FootballMatch) factory.createLeagueMatch(lech, legia);

        IllegalArgumentException invalidWalkoverException = assertThrows(
                IllegalArgumentException.class,
                () -> invalidWalkoverWinner.assignWalkover(rakow, 3, 0)
        );

        assertEquals("Zwycięzca walkowera musi być uczestnikiem meczu", invalidWalkoverException.getMessage());

        /*
         * 29. Test błędnego zapytania o bilans bramek.
         * Nie można pytać o drużynę, która nie grała w meczu.
         */
        assertThrows(
                IllegalArgumentException.class,
                () -> m1.getScore().getSignedGoalDifferenceFor(rakow)
        );
    }

    private Team team(String id, String name) {
        return Team.builder()
                .id(id)
                .name(name)
                .players(List.of("Player 1", "Player 2", "Player 3"))
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

    private FootballMatchEvent eventWithRelatedId(
            FootballEventType type,
            Team actor,
            int minute,
            String description,
            String relatedEventId
    ) {
        return FootballMatchEvent.builder()
                .eventType(type)
                .actor(actor)
                .minute(minute)
                .description(description)
                .relatedEventId(relatedEventId)
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
        private final List<String> receivedDescriptions = new ArrayList<>();

        @Override
        public void onEventRecorded(MatchEvent event, Score currentScore) {
            receivedDescriptions.add(event.getDescription() + " | " + currentScore.display());
        }

        public List<String> getReceivedDescriptions() {
            return receivedDescriptions;
        }
    }

    private static class FakeMatchEvent implements MatchEvent {
        private final Competitor actor;

        private FakeMatchEvent(Competitor actor) {
            this.actor = actor;
        }

        @Override
        public java.time.LocalDateTime getTimestamp() {
            return java.time.LocalDateTime.now();
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