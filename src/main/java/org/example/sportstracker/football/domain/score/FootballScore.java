package org.example.sportstracker.football.domain.score;

import lombok.Data;
import org.example.sportstracker.core.domain.match.MatchEvent;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.football.domain.competitor.Team;
import org.example.sportstracker.football.domain.match.FootballEventType;
import org.example.sportstracker.football.domain.match.FootballMatchEvent;

import java.util.HashMap;
import java.util.Map;

@Data
public class FootballScore implements Score {
    private int homeGoals = 0;
    private int awayGoals = 0;
    private int homePenalties = 0;
    private int awayPenalties = 0;

    private int homeYellowCards = 0;
    private int awayYellowCards = 0;
    private int homeRedCards = 0;
    private int awayRedCards = 0;

    private Team homeTeam;
    private Team awayTeam;

    /*
     * Zapamiętujemy konkretne zdarzenia, które zmieniły wynik albo statystyki.
     * Dzięki temu GOAL_DISALLOWED, PENALTY_DISALLOWED i EVENT_INVALIDATED
     * mogą unieważnić konkretny wcześniejszy event po relatedEventId.
     */
    private Map<String, ScoreAffectingEventRecord> scoreAffectingEvents = new HashMap<>();

    public FootballScore(Team homeTeam, Team awayTeam) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }

    public int getGoalDifference() {
        return Math.abs(homeGoals - awayGoals);
    }

    public int getSignedGoalDifferenceFor(Team team) {
        if (team.equals(homeTeam)) {
            return homeGoals - awayGoals;
        }

        if (team.equals(awayTeam)) {
            return awayGoals - homeGoals;
        }

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
        String baseScore = String.format("%s %d - %d %s",
                homeTeam.getName(), homeGoals, awayGoals, awayTeam.getName());

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
        if (!(event instanceof FootballMatchEvent fbEvent)) {
            return;
        }

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

        /*
         * Zdarzenia takie jak:
         * FOUL,
         * SUBSTITUTION,
         * VAR_REVIEW,
         * PENALTY_MISSED,
         * SHOOTOUT_PENALTY_MISSED
         * nie zmieniają wyniku ani statystyk przechowywanych w FootballScore.
         */
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
        if (relatedEventId == null) {
            return;
        }

        ScoreAffectingEventRecord record = scoreAffectingEvents.get(relatedEventId);

        if (record == null || !record.active || record.type != ScoreAffectingEventType.GOAL) {
            return;
        }

        if (record.team.equals(homeTeam) && homeGoals > 0) {
            homeGoals--;
        } else if (record.team.equals(awayTeam) && awayGoals > 0) {
            awayGoals--;
        }

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
        if (relatedEventId == null) {
            return;
        }

        ScoreAffectingEventRecord record = scoreAffectingEvents.get(relatedEventId);

        if (record == null || !record.active || record.type != ScoreAffectingEventType.SHOOTOUT_PENALTY) {
            return;
        }

        if (record.team.equals(homeTeam) && homePenalties > 0) {
            homePenalties--;
        } else if (record.team.equals(awayTeam) && awayPenalties > 0) {
            awayPenalties--;
        }

        record.active = false;
    }

    private void invalidateAnyScoreAffectingEvent(String relatedEventId) {
        if (relatedEventId == null) {
            return;
        }

        ScoreAffectingEventRecord record = scoreAffectingEvents.get(relatedEventId);

        if (record == null || !record.active) {
            return;
        }

        if (record.type == ScoreAffectingEventType.GOAL) {
            invalidateGoal(relatedEventId);
        } else if (record.type == ScoreAffectingEventType.SHOOTOUT_PENALTY) {
            invalidateShootoutPenalty(relatedEventId);
        } else if (record.type == ScoreAffectingEventType.YELLOW_CARD) {
            invalidateYellowCard(record);
        } else if (record.type == ScoreAffectingEventType.RED_CARD) {
            invalidateRedCard(record);
        }
    }

    private void invalidateYellowCard(ScoreAffectingEventRecord record) {
        if (record.team.equals(homeTeam) && homeYellowCards > 0) {
            homeYellowCards--;
        } else if (record.team.equals(awayTeam) && awayYellowCards > 0) {
            awayYellowCards--;
        }

        record.active = false;
    }

    private void invalidateRedCard(ScoreAffectingEventRecord record) {
        if (record.team.equals(homeTeam) && homeRedCards > 0) {
            homeRedCards--;
        } else if (record.team.equals(awayTeam) && awayRedCards > 0) {
            awayRedCards--;
        }

        record.active = false;
    }

    private void registerScoreAffectingEvent(
            FootballMatchEvent event,
            Team team,
            ScoreAffectingEventType type
    ) {
        scoreAffectingEvents.put(
                event.getEventId(),
                new ScoreAffectingEventRecord(team, type, true)
        );
    }

    private enum ScoreAffectingEventType {
        GOAL,
        SHOOTOUT_PENALTY,
        YELLOW_CARD,
        RED_CARD
    }

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