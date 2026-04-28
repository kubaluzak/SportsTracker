package org.example.sportstracker.football.domain.score;

import lombok.Data;
import org.example.sportstracker.core.domain.match.MatchEvent;
import org.example.sportstracker.core.domain.score.Score;
import org.example.sportstracker.football.domain.competitor.Team;
import org.example.sportstracker.football.domain.match.FootballEventType;
import org.example.sportstracker.football.domain.match.FootballMatchEvent;

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
            if (fbEvent.getActor().equals(homeTeam)) {
                homeGoals++;
            } else if (fbEvent.getActor().equals(awayTeam)) {
                awayGoals++;
            }
        } else if (type == FootballEventType.GOAL_DISALLOWED) {
            if (fbEvent.getActor().equals(homeTeam) && homeGoals > 0) {
                homeGoals--;
            } else if (fbEvent.getActor().equals(awayTeam) && awayGoals > 0) {
                awayGoals--;
            }
        } else if (type == FootballEventType.YELLOW_CARD) {
            if (fbEvent.getActor().equals(homeTeam)) {
                homeYellowCards++;
            } else if (fbEvent.getActor().equals(awayTeam)) {
                awayYellowCards++;
            }
        } else if (type == FootballEventType.RED_CARD) {
            if (fbEvent.getActor().equals(homeTeam)) {
                homeRedCards++;
            } else if (fbEvent.getActor().equals(awayTeam)) {
                awayRedCards++;
            }
        } else if (type == FootballEventType.SHOOTOUT_PENALTY_SCORED) {
            if (fbEvent.getActor().equals(homeTeam)) {
                homePenalties++;
            } else if (fbEvent.getActor().equals(awayTeam)) {
                awayPenalties++;
            }
        } else if (type == FootballEventType.PENALTY_DISALLOWED) {
            if (fbEvent.getActor().equals(homeTeam) && homePenalties > 0) {
                homePenalties--;
            } else if (fbEvent.getActor().equals(awayTeam) && awayPenalties > 0) {
                awayPenalties--;
            }
        }
    }
}