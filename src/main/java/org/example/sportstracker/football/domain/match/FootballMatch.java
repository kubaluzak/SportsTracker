package org.example.sportstracker.football.domain.match;

import lombok.Data;
import lombok.Getter;
import org.example.sportstracker.core.domain.match.Match;
import org.example.sportstracker.core.domain.match.MatchEvent;
import org.example.sportstracker.core.domain.match.MatchEventListener;
import org.example.sportstracker.core.domain.match.MatchResolutionStrategy;
import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.core.domain.result.Result;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.football.domain.competitor.Team;
import org.example.sportstracker.football.domain.result.FootballResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class FootballMatch implements Match {
    private MatchStatus status = MatchStatus.SCHEDULED;
    private FootballScore score;
    private List<FootballMatchEvent> events = new ArrayList<>();

    private Team homeTeam;
    private Team awayTeam;

    private MatchResolutionStrategy resolutionStrategy;
    private List<MatchEventListener> listeners = new ArrayList<>();

    private String abandonReason;
    private boolean extraTimePlayed;
    private boolean penaltyShootoutStarted;
    private FootballResult manualResult;

    public FootballMatch(Team homeTeam, Team awayTeam, MatchResolutionStrategy resolutionStrategy) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.resolutionStrategy = resolutionStrategy;
        this.score = new FootballScore(homeTeam, awayTeam);
    }

    @Override
    public void addMatchEventListener(MatchEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void startMatch() {
        if (status != MatchStatus.SCHEDULED) {
            throw new IllegalStateException("Mecz nie jest zaplanowany");
        }
        status = MatchStatus.IN_PROGRESS;
    }

    @Override
    public void endMatch() {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Tylko trwający mecz można zakończyć");
        }
        status = MatchStatus.COMPLETED;
    }

    @Override
    public void abandonMatch(String reason) {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Można przerwać tylko trwający mecz");
        }
        abandonReason = reason;
        status = MatchStatus.ABANDONED;
    }

    public void assignWalkover(Team winner, int winnerGoals, int loserGoals) {
        if (!winner.equals(homeTeam) && !winner.equals(awayTeam)) {
            throw new IllegalArgumentException("Zwycięzca walkowera musi być uczestnikiem meczu");
        }

        Team loser = winner.equals(homeTeam) ? awayTeam : homeTeam;

        // Użycie prywatnej metody wewn. klasy score - z zewnątrz nikt nie ustawi goli ręcznie!
        if (winner.equals(homeTeam)) {
            score.applyWalkover(winnerGoals, loserGoals);
        } else {
            score.applyWalkover(loserGoals, winnerGoals);
        }

        manualResult = FootballResult.builder()
                .winner(winner)
                .isDraw(false)
                .wonViaPenalties(false)
                .completedNormally(false)
                .walkover(true)
                .walkoverLoser(loser)
                .build();

        status = MatchStatus.WALKOVER;
    }

    public void goToExtraTime() {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Dogrywkę można rozpocząć tylko w trwającym meczu");
        }
        extraTimePlayed = true;
    }

    public void startPenaltyShootout() {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Karne można rozpocząć tylko w trwającym meczu");
        }
        penaltyShootoutStarted = true;
    }

    @Override
    public void recordEvent(MatchEvent event) {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Zdarzenia można dodawać tylko podczas trwania meczu");
        }

        if (!(event instanceof FootballMatchEvent fbEvent)) {
            throw new IllegalArgumentException("Nieprawidłowy typ zdarzenia dla meczu piłkarskiego");
        }

        validateEventId(fbEvent);
        validateRelatedEvent(fbEvent);

        events.add(fbEvent);
        score.update(event);

        for (MatchEventListener listener : listeners) {
            listener.onEventRecorded(event, score);
        }
    }

    private void validateEventId(FootballMatchEvent event) { /* (bez zmian) */
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            throw new IllegalArgumentException("Zdarzenie musi mieć eventId");
        }
        boolean eventIdAlreadyExists = events.stream()
                .anyMatch(previousEvent -> previousEvent.getEventId().equals(event.getEventId()));
        if (eventIdAlreadyExists) {
            throw new IllegalArgumentException("Zdarzenie o takim eventId już istnieje w meczu");
        }
    }

    private void validateRelatedEvent(FootballMatchEvent event) { /* (bez zmian) */
        FootballEventType type = event.getEventType();
        boolean requiresRelatedEvent = type == FootballEventType.VAR_REVIEW
                || type == FootballEventType.GOAL_DISALLOWED
                || type == FootballEventType.PENALTY_DISALLOWED
                || type == FootballEventType.EVENT_INVALIDATED;

        if (!requiresRelatedEvent) return;

        if (event.getRelatedEventId() == null || event.getRelatedEventId().isBlank()) {
            throw new IllegalArgumentException("Zdarzenie wymaga relatedEventId");
        }
        FootballMatchEvent relatedEvent = events.stream()
                .filter(previousEvent -> previousEvent.getEventId().equals(event.getRelatedEventId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("relatedEventId nie wskazuje na istniejące zdarzenie meczu"));

        validateRelatedEventType(event, relatedEvent);
    }

    private void validateRelatedEventType(FootballMatchEvent event, FootballMatchEvent relatedEvent) { /* (bez zmian) */
        FootballEventType eventType = event.getEventType();
        FootballEventType relatedEventType = relatedEvent.getEventType();

        if (eventType == FootballEventType.VAR_REVIEW) {
            boolean canReview = relatedEventType == FootballEventType.GOAL_SCORED || relatedEventType == FootballEventType.PENALTY_SCORED
                    || relatedEventType == FootballEventType.SHOOTOUT_PENALTY_SCORED || relatedEventType == FootballEventType.FOUL
                    || relatedEventType == FootballEventType.YELLOW_CARD || relatedEventType == FootballEventType.RED_CARD;
            if (!canReview) throw new IllegalArgumentException("VAR nie może odnosić się do tego typu zdarzenia");
        }
        if (eventType == FootballEventType.GOAL_DISALLOWED) {
            boolean canDisallowGoal = relatedEventType == FootballEventType.GOAL_SCORED || relatedEventType == FootballEventType.PENALTY_SCORED;
            if (!canDisallowGoal) throw new IllegalArgumentException("Anulowanie gola musi odnosić się do zdarzenia bramkowego");
        }
        if (eventType == FootballEventType.PENALTY_DISALLOWED) {
            if (relatedEventType != FootballEventType.SHOOTOUT_PENALTY_SCORED)
                throw new IllegalArgumentException("Anulowanie karnego musi odnosić się do trafionego karnego w serii");
        }
        if (eventType == FootballEventType.EVENT_INVALIDATED) {
            boolean canInvalidate = relatedEventType == FootballEventType.GOAL_SCORED || relatedEventType == FootballEventType.PENALTY_SCORED
                    || relatedEventType == FootballEventType.SHOOTOUT_PENALTY_SCORED || relatedEventType == FootballEventType.YELLOW_CARD || relatedEventType == FootballEventType.RED_CARD;
            if (!canInvalidate) throw new IllegalArgumentException("Nie można unieważnić tego typu zdarzenia");
        }
    }

    @Override
    public FootballScore getScore() {
        return score;
    }

    @Override
    public Result getResult() {
        if (manualResult != null) return manualResult;
        return resolutionStrategy.resolve(score, homeTeam, awayTeam);
    }

    @Override
    public MatchStatus getStatus() {
        return status;
    }

    @Getter
    public static class FootballScore implements Score {
        private int homeGoals = 0;
        private int awayGoals = 0;
        private int homePenalties = 0;
        private int awayPenalties = 0;

        private int homeYellowCards = 0;
        private int awayYellowCards = 0;
        private int homeRedCards = 0;
        private int awayRedCards = 0;

        private final Team homeTeam;
        private final Team awayTeam;

        private final Map<String, ScoreAffectingEventRecord> scoreAffectingEvents = new HashMap<>();

        public FootballScore(Team homeTeam, Team awayTeam) {
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
        }

        private void applyWalkover(int homeGoals, int awayGoals) {
            this.homeGoals = homeGoals;
            this.awayGoals = awayGoals;
        }

        public int getGoalDifference() {
            return Math.abs(homeGoals - awayGoals);
        }

        public int getSignedGoalDifferenceFor(Team team) {
            if (team.equals(homeTeam)) return homeGoals - awayGoals;
            if (team.equals(awayTeam)) return awayGoals - homeGoals;
            throw new IllegalArgumentException("Drużyna nie bierze udziału w meczu");
        }

        public int getHomeFairPlayPenaltyPoints() {
            return homeYellowCards + homeRedCards * 3;
        }

        public int getAwayFairPlayPenaltyPoints() {
            return awayYellowCards + awayRedCards * 3;
        }

        @Override
        public String display() {
            String baseScore = String.format("%s %d - %d %s", homeTeam.getName(), homeGoals, awayGoals, awayTeam.getName());
            String penalties = "";
            if (homePenalties > 0 || awayPenalties > 0) {
                penalties = String.format(" (Karne: %d - %d)", homePenalties, awayPenalties);
            }
            String cards = String.format(" | Kartki: %s Ż:%d C:%d, %s Ż:%d C:%d",
                    homeTeam.getName(), homeYellowCards, homeRedCards,
                    awayTeam.getName(), awayYellowCards, awayRedCards);

            return baseScore + penalties + cards;
        }

        @Override
        public void update(MatchEvent event) {
            if (!(event instanceof FootballMatchEvent fbEvent)) return;

            FootballEventType type = fbEvent.getEventType();

            if (type == FootballEventType.GOAL_SCORED || type == FootballEventType.PENALTY_SCORED) {
                addGoal(fbEvent);
            } else if (type == FootballEventType.GOAL_DISALLOWED) {
                invalidateGoal(fbEvent.getRelatedEventId());
            } else if (type == FootballEventType.YELLOW_CARD) {
                addYellowCard(fbEvent);
            } else if (type == FootballEventType.RED_CARD) {
                addRedCard(fbEvent);
            } else if (type == FootballEventType.SHOOTOUT_PENALTY_SCORED) {
                addShootoutPenalty(fbEvent);
            } else if (type == FootballEventType.PENALTY_DISALLOWED) {
                invalidateShootoutPenalty(fbEvent.getRelatedEventId());
            } else if (type == FootballEventType.EVENT_INVALIDATED) {
                invalidateAnyScoreAffectingEvent(fbEvent.getRelatedEventId());
            }
        }

        private void addGoal(FootballMatchEvent event) {
            if (event.getActor().equals(homeTeam)) {
                homeGoals++;
                registerScoreAffectingEvent(event, homeTeam, ScoreAffectingEventType.GOAL);
            } else if (event.getActor().equals(awayTeam)) {
                awayGoals++;
                registerScoreAffectingEvent(event, awayTeam, ScoreAffectingEventType.GOAL);
            }
        }

        private void invalidateGoal(String relatedEventId) {
            if (relatedEventId == null) return;
            ScoreAffectingEventRecord record = scoreAffectingEvents.get(relatedEventId);
            if (record == null || !record.active || record.type != ScoreAffectingEventType.GOAL) return;

            if (record.team.equals(homeTeam) && homeGoals > 0) homeGoals--;
            else if (record.team.equals(awayTeam) && awayGoals > 0) awayGoals--;
            record.active = false;
        }

        private void addYellowCard(FootballMatchEvent event) {
            if (event.getActor().equals(homeTeam)) {
                homeYellowCards++;
                registerScoreAffectingEvent(event, homeTeam, ScoreAffectingEventType.YELLOW_CARD);
            } else if (event.getActor().equals(awayTeam)) {
                awayYellowCards++;
                registerScoreAffectingEvent(event, awayTeam, ScoreAffectingEventType.YELLOW_CARD);
            }
        }

        private void addRedCard(FootballMatchEvent event) {
            if (event.getActor().equals(homeTeam)) {
                homeRedCards++;
                registerScoreAffectingEvent(event, homeTeam, ScoreAffectingEventType.RED_CARD);
            } else if (event.getActor().equals(awayTeam)) {
                awayRedCards++;
                registerScoreAffectingEvent(event, awayTeam, ScoreAffectingEventType.RED_CARD);
            }
        }

        private void addShootoutPenalty(FootballMatchEvent event) {
            if (event.getActor().equals(homeTeam)) {
                homePenalties++;
                registerScoreAffectingEvent(event, homeTeam, ScoreAffectingEventType.SHOOTOUT_PENALTY);
            } else if (event.getActor().equals(awayTeam)) {
                awayPenalties++;
                registerScoreAffectingEvent(event, awayTeam, ScoreAffectingEventType.SHOOTOUT_PENALTY);
            }
        }

        private void invalidateShootoutPenalty(String relatedEventId) {
            if (relatedEventId == null) return;
            ScoreAffectingEventRecord record = scoreAffectingEvents.get(relatedEventId);
            if (record == null || !record.active || record.type != ScoreAffectingEventType.SHOOTOUT_PENALTY) return;

            if (record.team.equals(homeTeam) && homePenalties > 0) homePenalties--;
            else if (record.team.equals(awayTeam) && awayPenalties > 0) awayPenalties--;
            record.active = false;
        }

        private void invalidateAnyScoreAffectingEvent(String relatedEventId) {
            if (relatedEventId == null) return;
            ScoreAffectingEventRecord record = scoreAffectingEvents.get(relatedEventId);
            if (record == null || !record.active) return;

            if (record.type == ScoreAffectingEventType.GOAL) invalidateGoal(relatedEventId);
            else if (record.type == ScoreAffectingEventType.SHOOTOUT_PENALTY) invalidateShootoutPenalty(relatedEventId);
            else if (record.type == ScoreAffectingEventType.YELLOW_CARD) invalidateYellowCard(record);
            else if (record.type == ScoreAffectingEventType.RED_CARD) invalidateRedCard(record);
        }

        private void invalidateYellowCard(ScoreAffectingEventRecord record) {
            if (record.team.equals(homeTeam) && homeYellowCards > 0) homeYellowCards--;
            else if (record.team.equals(awayTeam) && awayYellowCards > 0) awayYellowCards--;
            record.active = false;
        }

        private void invalidateRedCard(ScoreAffectingEventRecord record) {
            if (record.team.equals(homeTeam) && homeRedCards > 0) homeRedCards--;
            else if (record.team.equals(awayTeam) && awayRedCards > 0) awayRedCards--;
            record.active = false;
        }

        private void registerScoreAffectingEvent(FootballMatchEvent event, Team team, ScoreAffectingEventType type) {
            scoreAffectingEvents.put(event.getEventId(), new ScoreAffectingEventRecord(team, type, true));
        }

        private enum ScoreAffectingEventType { GOAL, SHOOTOUT_PENALTY, YELLOW_CARD, RED_CARD }

        private static class ScoreAffectingEventRecord {
            private final Team team;
            private final ScoreAffectingEventType type;
            private boolean active;

            private ScoreAffectingEventRecord(Team team, ScoreAffectingEventType type, boolean active) {
                this.team = team;
                this.type = type;
                this.active = active;
            }
        }
    }
}