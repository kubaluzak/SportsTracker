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
    void shouldRunWholeFootballTournamentFromLeagueStageToFinal() {
        // =========================
        // 1. TWORZENIE DRUŻYN
        // =========================

        Team lech = team("1", "Lech Poznań");
        Team legia = team("2", "Legia Warszawa");
        Team rakow = team("3", "Raków Częstochowa");
        Team jagiellonia = team("4", "Jagiellonia");
        Team widzew = team("5", "Widzew Łódź");
        Team pogon = team("6", "Pogoń Szczecin");
        Team slask = team("7", "Śląsk Wrocław");
        Team gornik = team("8", "Górnik Zabrze");

        // Sprawdzamy, czy drużyna została poprawnie utworzona przez buildera.
        assertEquals("1", lech.getId());
        assertEquals("Lech Poznań", lech.getName());
        assertEquals(3, lech.getPlayers().size());
        assertEquals("Manager Lech Poznań", lech.getManager());

        // =========================
        // 2. TWORZENIE FABRYKI MECZÓW I TURNIEJU
        // =========================

        MatchFactory factory = new FootballMatchFactory();

        FootballTournament tournament = new FootballTournament();
        tournament.setName("Ultimate Football Library Cup 2026");

        // Na początku turniej nie ma zwycięzcy.
        assertNull(tournament.getOverallWinner());

        // Startujemy turniej.
        tournament.startTournament();

        // =========================
        // 3. TWORZENIE FAZY LIGOWEJ
        // =========================

        FootballLeagueStage leagueStage = new FootballLeagueStage(
                Arrays.asList(lech, legia, rakow, jagiellonia, widzew, pogon, slask, gornik),
                3
        );

        // Sprawdzamy, czy zwycięstwo w lidze daje 3 punkty.
        assertEquals(3, leagueStage.getPointsForWin());

        // Dodajemy fazę ligową do turnieju.
        tournament.addStage(leagueStage);
        assertEquals(1, tournament.getStages().size());

        // Startujemy fazę ligową.
        leagueStage.startStage();

        // =========================
        // 4. TWORZENIE MECZÓW LIGOWYCH
        // =========================

        FootballMatch m1 = (FootballMatch) factory.createLeagueMatch(lech, legia);
        FootballMatch m2 = (FootballMatch) factory.createLeagueMatch(rakow, jagiellonia);
        FootballMatch m3 = (FootballMatch) factory.createLeagueMatch(widzew, pogon);
        FootballMatch m4 = (FootballMatch) factory.createLeagueMatch(slask, gornik);
        FootballMatch m5 = (FootballMatch) factory.createLeagueMatch(lech, rakow);
        FootballMatch m6 = (FootballMatch) factory.createLeagueMatch(legia, jagiellonia);
        FootballMatch m7 = (FootballMatch) factory.createLeagueMatch(pogon, slask);
        FootballMatch m8 = (FootballMatch) factory.createLeagueMatch(gornik, widzew);

        // Dodajemy obserwatorów do pierwszego meczu.
        // TestMatchBroadcaster zbiera eventy do listy, a ConsoleMatchBroadcaster wypisuje relację live.
        TestMatchBroadcaster testBroadcaster = new TestMatchBroadcaster();

        m1.addMatchEventListener(testBroadcaster);
        m1.addMatchEventListener(new ConsoleMatchBroadcaster());

        // Dodajemy wszystkie mecze ligowe do fazy ligowej.
        List<FootballMatch> leagueMatches = List.of(m1, m2, m3, m4, m5, m6, m7, m8);
        leagueMatches.forEach(leagueStage::addMatch);

        assertEquals(8, leagueStage.getStageMatches().size());

        // =========================
        // 5. MECZ LIGOWY LECH VS LEGIA Z VAR
        // =========================

        // Lech strzela pierwszego gola.
        FootballMatchEvent lechGoal = event(
                FootballEventType.GOAL_SCORED,
                lech,
                12,
                "Lech strzela na 1:0"
        );

        // Legia strzela gola, który później zostanie sprawdzony przez VAR.
        FootballMatchEvent legiaGoal = event(
                FootballEventType.GOAL_SCORED,
                legia,
                30,
                "Legia wyrównuje"
        );

        // Zapamiętujemy ID gola Legii, żeby VAR i anulowanie gola mogły się do niego odnieść.
        String relatedGoalId = legiaGoal.getEventId();

        // VAR sprawdza konkretnego gola Legii.
        FootballMatchEvent varReview = eventWithRelatedId(
                FootballEventType.VAR_REVIEW,
                legia,
                31,
                "VAR sprawdza gola Legii",
                relatedGoalId
        );

        // Gol Legii zostaje anulowany po VAR.
        FootballMatchEvent disallowedGoal = eventWithRelatedId(
                FootballEventType.GOAL_DISALLOWED,
                legia,
                32,
                "Gol Legii anulowany",
                relatedGoalId
        );

        // Sprawdzamy, czy eventy dostały ID i timestamp.
        assertNotNull(lechGoal.getEventId());
        assertNotNull(legiaGoal.getEventId());
        assertNotNull(lechGoal.getTimestamp());
        assertNotNull(legiaGoal.getTimestamp());

        // Sprawdzamy, czy VAR i anulowanie gola wskazują dokładnie na gola Legii.
        assertEquals(relatedGoalId, varReview.getRelatedEventId());
        assertEquals(relatedGoalId, disallowedGoal.getRelatedEventId());

        // Rozgrywamy mecz Lech vs Legia.
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

        // W meczu było 9 eventów i każdy został zapisany oraz wysłany do obserwatora.
        assertEquals(9, m1.getEvents().size());
        assertEquals(9, testBroadcaster.getReceivedDescriptions().size());

        // Po anulowaniu gola Legii i drugim golu Lecha wynik to 2:0.
        assertEquals(2, m1.getScore().getHomeGoals());
        assertEquals(0, m1.getScore().getAwayGoals());

        // Sprawdzamy kartki i punkty fair play.
        assertEquals(1, m1.getScore().getHomeYellowCards());
        assertEquals(1, m1.getScore().getAwayRedCards());
        assertEquals(1, m1.getScore().getHomeFairPlayPenaltyPoints());
        assertEquals(3, m1.getScore().getAwayFairPlayPenaltyPoints());

        // Sprawdzamy bilans bramkowy z perspektywy obu drużyn.
        assertEquals(2, m1.getScore().getGoalDifference());
        assertEquals(2, m1.getScore().getSignedGoalDifferenceFor(lech));
        assertEquals(-2, m1.getScore().getSignedGoalDifferenceFor(legia));

        // Sprawdzamy tekstowe wyświetlanie wyniku.
        assertEquals(
                "Lech Poznań 2 - 0 Legia Warszawa | Kartki: Lech Poznań Ż:1 C:0, Legia Warszawa Ż:0 C:1",
                m1.getScore().display()
        );

        // Mecz ligowy kończy się normalnym zwycięstwem Lecha.
        assertEquals(lech, m1.getResult().getWinner());
        assertFalse(m1.getResult().isDraw());
        assertTrue(m1.getResult().isCompletedNormally());

        // =========================
        // 6. ROZGRYWANIE POZOSTAŁYCH MECZÓW LIGOWYCH
        // =========================

        // Raków remisuje z Jagiellonią.
        playMatch(m2,
                event(FootballEventType.GOAL_SCORED, rakow, 10, "Raków 1:0"),
                event(FootballEventType.GOAL_SCORED, jagiellonia, 75, "Jagiellonia 1:1"),
                event(FootballEventType.YELLOW_CARD, rakow, 80, "Kartka dla Rakowa")
        );

        assertTrue(m2.getResult().isDraw());
        assertNull(m2.getResult().getWinner());

        // Pogoń wygrywa z Widzewem 2:0. Nietrafiony karny Widzewa nie zmienia wyniku.
        playMatch(m3,
                event(FootballEventType.GOAL_SCORED, pogon, 20, "Pogoń 0:1"),
                event(FootballEventType.GOAL_SCORED, pogon, 62, "Pogoń 0:2"),
                event(FootballEventType.PENALTY_MISSED, widzew, 70, "Widzew nie trafia karnego")
        );

        assertEquals(pogon, m3.getResult().getWinner());

        // Śląsk wygrywa z Górnikiem i dostaje dwie żółte kartki.
        playMatch(m4,
                event(FootballEventType.GOAL_SCORED, slask, 44, "Śląsk 1:0"),
                event(FootballEventType.YELLOW_CARD, slask, 60, "Kartka Śląska"),
                event(FootballEventType.YELLOW_CARD, slask, 65, "Druga kartka Śląska")
        );

        // Lech wygrywa z Rakowem 2:1.
        playMatch(m5,
                event(FootballEventType.GOAL_SCORED, lech, 15, "Lech 1:0"),
                event(FootballEventType.PENALTY_SCORED, lech, 33, "Lech 2:0 z karnego w meczu"),
                event(FootballEventType.GOAL_SCORED, rakow, 80, "Raków 2:1")
        );

        // Jagiellonia wygrywa z Legią 1:0.
        playMatch(m6,
                event(FootballEventType.GOAL_SCORED, jagiellonia, 55, "Jagiellonia 0:1")
        );

        // Pogoń remisuje ze Śląskiem 1:1.
        playMatch(m7,
                event(FootballEventType.GOAL_SCORED, pogon, 5, "Pogoń 1:0"),
                event(FootballEventType.GOAL_SCORED, slask, 90, "Śląsk 1:1")
        );

        // Górnik remisuje z Widzewem 1:1.
        playMatch(m8,
                event(FootballEventType.GOAL_SCORED, gornik, 18, "Górnik 1:0"),
                event(FootballEventType.GOAL_SCORED, widzew, 78, "Widzew 1:1")
        );

        // =========================
        // 7. WALKOWER W FAZIE LIGOWEJ
        // =========================

        // Śląsk wygrywa z Legią walkowerem 3:0.
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

        // Dodajemy walkower do fazy ligowej, żeby był liczony w tabeli.
        leagueStage.addMatch(walkoverMatch);

        // =========================
        // 8. ZAKOŃCZENIE FAZY LIGOWEJ I WYŁONIENIE TOP 4
        // =========================

        // Kończymy fazę ligową.
        // endStage sprawdza mecze, liczy tabelę i wybiera 4 najlepsze drużyny.
        leagueStage.endStage();

        // Pobieramy drużyny, które awansowały do fazy pucharowej.
        List<Competitor> advanced = leagueStage.getAdvancingCompetitors();

        assertEquals(4, advanced.size());

        // W tym scenariuszu do fazy pucharowej awansują te drużyny.
        assertTrue(advanced.contains(lech));
        assertTrue(advanced.contains(pogon));
        assertTrue(advanced.contains(slask));
        assertTrue(advanced.contains(jagiellonia));

        // Sprawdzamy, że lider tabeli ma punkty i jest wśród awansujących.
        TableEntry topEntry = leagueStage.getTable().getRows().get(0);
        assertTrue(topEntry.getPoints() > 0);
        assertTrue(advanced.contains(topEntry.getTeam()));

        // =========================
        // 9. TWORZENIE FAZY PUCHAROWEJ
        // =========================

        FootballKnockoutStage knockoutStage = new FootballKnockoutStage();

        // Dodajemy fazę pucharową do turnieju.
        tournament.addStage(knockoutStage);
        assertEquals(2, tournament.getStages().size());

        // Startujemy fazę pucharową.
        knockoutStage.startStage();

        // Tworzymy pary półfinałowe na podstawie pozycji po fazie ligowej.
        Team semi1Home = (Team) advanced.get(0);
        Team semi1Away = (Team) advanced.get(3);
        Team semi2Home = (Team) advanced.get(1);
        Team semi2Away = (Team) advanced.get(2);

        FootballMatch semi1 = (FootballMatch) factory.createKnockoutMatch(semi1Home, semi1Away);
        FootballMatch semi2 = (FootballMatch) factory.createKnockoutMatch(semi2Home, semi2Away);

        // Dodajemy półfinały do etapu pucharowego.
        knockoutStage.addMatch(semi1);
        knockoutStage.addMatch(semi2);

        // Ustawiamy pierwszą rundę drabinki jako dwa półfinały.
        BracketTree bracket = knockoutStage.getBrackets();
        bracket.setFirstRound(Arrays.asList(semi1, semi2));

        assertEquals(1, bracket.getRounds().size());
        assertEquals(2, bracket.getRounds().get(0).size());

        // =========================
        // 10. PIERWSZY PÓŁFINAŁ
        // =========================

        // Pierwszy półfinał kończy się zwycięstwem gospodarza 2:0.
        playMatch(semi1,
                event(FootballEventType.GOAL_SCORED, semi1Home, 22, "Lider strzela w półfinale"),
                event(FootballEventType.GOAL_SCORED, semi1Home, 81, "Lider dobija rywala")
        );

        assertEquals(semi1Home, semi1.getResult().getWinner());
        assertFalse(((FootballResult) semi1.getResult()).isWonViaPenalties());

        // =========================
        // 11. DRUGI PÓŁFINAŁ Z DOGRYWKĄ I KARNYMI
        // =========================

        // Drugi półfinał rozgrywamy ręcznie, bo chcemy pokazać dogrywkę i rzuty karne.
        semi2.startMatch();

        assertEquals(MatchStatus.IN_PROGRESS, semi2.getStatus());

        // Po czasie regulaminowym jest 1:1.
        semi2.recordEvent(event(FootballEventType.GOAL_SCORED, semi2Home, 20, "Półfinał 1:0"));
        semi2.recordEvent(event(FootballEventType.GOAL_SCORED, semi2Away, 90, "Półfinał 1:1"));

        // Mecz przechodzi do dogrywki.
        semi2.goToExtraTime();
        assertTrue(semi2.isExtraTimePlayed());

        // Po dogrywce nadal potrzebne są karne.
        semi2.startPenaltyShootout();
        assertTrue(semi2.isPenaltyShootoutStarted());

        // Goście wygrywają serię karnych 2:1.
        semi2.recordEvent(event(FootballEventType.SHOOTOUT_PENALTY_SCORED, semi2Home, 121, "Gospodarze trafiają karnego"));
        semi2.recordEvent(event(FootballEventType.SHOOTOUT_PENALTY_SCORED, semi2Away, 122, "Goście trafiają karnego"));
        semi2.recordEvent(event(FootballEventType.SHOOTOUT_PENALTY_SCORED, semi2Away, 123, "Goście trafiają drugiego karnego"));
        semi2.recordEvent(event(FootballEventType.SHOOTOUT_PENALTY_MISSED, semi2Home, 124, "Gospodarze pudłują karnego"));

        semi2.endMatch();

        assertEquals(MatchStatus.COMPLETED, semi2.getStatus());

        FootballResult semi2Result = (FootballResult) semi2.getResult();

        assertFalse(semi2Result.isDraw());
        assertTrue(semi2Result.isWonViaPenalties());
        assertEquals(semi2Away, semi2Result.getWinner());
        assertEquals(1, semi2.getScore().getHomePenalties());
        assertEquals(2, semi2.getScore().getAwayPenalties());

        // =========================
        // 12. GENEROWANIE FINAŁU Z DRABINKI
        // =========================

        // Drabinka bierze zwycięzców półfinałów i tworzy finał.
        bracket.generateNextRound();

        assertEquals(2, bracket.getRounds().size());
        assertEquals(1, bracket.getRounds().get(1).size());

        FootballMatch finalMatch = bracket.getRounds().get(1).get(0);

        // Finał powinien być zaplanowany między zwycięzcami półfinałów.
        assertEquals(semi1.getResult().getWinner(), finalMatch.getHomeTeam());
        assertEquals(semi2.getResult().getWinner(), finalMatch.getAwayTeam());
        assertEquals(MatchStatus.SCHEDULED, finalMatch.getStatus());

        // =========================
        // 13. FINAŁ TURNIEJU
        // =========================

        // W finale goście wygrywają 2:1.
        playMatch(finalMatch,
                event(FootballEventType.GOAL_SCORED, finalMatch.getHomeTeam(), 35, "Finał 1:0"),
                event(FootballEventType.GOAL_SCORED, finalMatch.getAwayTeam(), 89, "Finał 1:1"),
                event(FootballEventType.SUBSTITUTION, finalMatch.getAwayTeam(), 105, "Zmiana przed dogrywką"),
                event(FootballEventType.GOAL_SCORED, finalMatch.getAwayTeam(), 118, "Gol na wagę pucharu")
        );

        assertEquals(MatchStatus.COMPLETED, finalMatch.getStatus());
        assertFalse(finalMatch.getResult().isDraw());
        assertEquals(finalMatch.getAwayTeam(), finalMatch.getResult().getWinner());

        // =========================
        // 14. ZAKOŃCZENIE FAZY PUCHAROWEJ
        // =========================

        // Faza pucharowa kończy się zwycięstwem drużyny, która wygrała finał.
        knockoutStage.endStage();

        assertEquals(1, knockoutStage.getAdvancingCompetitors().size());
        assertEquals(finalMatch.getResult().getWinner(), knockoutStage.getAdvancingCompetitors().get(0));

        // =========================
        // 15. ZAKOŃCZENIE TURNIEJU
        // =========================

        // Ustawiamy zwycięzcę turnieju na podstawie zwycięzcy finału.
        tournament.setOverallWinner(finalMatch.getResult().getWinner());

        // Kończymy turniej.
        tournament.endTournament();

        assertNotNull(tournament.getOverallWinner());
        assertEquals(finalMatch.getAwayTeam(), tournament.getOverallWinner());
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
}