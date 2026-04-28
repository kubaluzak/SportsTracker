package org.example.sportstracker.football.domain.table;

import lombok.Data;
import org.example.sportstracker.football.domain.competitor.Team;
import org.example.sportstracker.football.domain.match.FootballMatch;
import org.example.sportstracker.football.domain.score.FootballScore;

import java.util.ArrayList;
import java.util.List;

@Data
public class LeagueTable {
    private List<TableEntry> rows = new ArrayList<>();

    public void registerTeam(Team team) {
        rows.add(new TableEntry(team));
    }

    public void clearStats() {
        for (TableEntry row : rows) {
            row.resetStats();
        }
    }

    public void processMatch(FootballMatch match) {
        processMatch(match, 3);
    }

    public void processMatch(FootballMatch match, int pointsForWin) {
        FootballScore score = match.getScore();

        TableEntry homeEntry = getEntry(match.getHomeTeam());
        TableEntry awayEntry = getEntry(match.getAwayTeam());

        homeEntry.addResult(score.getHomeGoals(), score.getAwayGoals(), pointsForWin);
        awayEntry.addResult(score.getAwayGoals(), score.getHomeGoals(), pointsForWin);

        homeEntry.addCards(score.getHomeYellowCards(), score.getHomeRedCards());
        awayEntry.addCards(score.getAwayYellowCards(), score.getAwayRedCards());
    }

    public void sortByPointsAndGoalDifference() {
        rows.sort((r1, r2) -> {
            if (r1.getPoints() != r2.getPoints()) {
                return Integer.compare(r2.getPoints(), r1.getPoints());
            }

            if (r1.getGoalDifference() != r2.getGoalDifference()) {
                return Integer.compare(r2.getGoalDifference(), r1.getGoalDifference());
            }

            if (r1.getGoalsScored() != r2.getGoalsScored()) {
                return Integer.compare(r2.getGoalsScored(), r1.getGoalsScored());
            }

            return Integer.compare(r1.getFairPlayPenaltyPoints(), r2.getFairPlayPenaltyPoints());
        });
    }

    public void printTable() {
        System.out.println(String.format(
                "%-20s | %-3s | %-9s | %-7s | %-9s",
                "Drużyna", "Pkt", "Bramki", "Kartki", "FairPlay"
        ));

        System.out.println("----------------------------------------------------------------");

        for (TableEntry row : rows) {
            System.out.println(String.format(
                    "%-20s | %-3d | %d:%d (%+d) | Ż:%d C:%d | %d",
                    row.getTeam().getName(),
                    row.getPoints(),
                    row.getGoalsScored(),
                    row.getGoalsConceded(),
                    row.getGoalDifference(),
                    row.getYellowCards(),
                    row.getRedCards(),
                    row.getFairPlayPenaltyPoints()
            ));
        }
    }

    private TableEntry getEntry(Team team) {
        return rows.stream()
                .filter(r -> r.getTeam().equals(team))
                .findFirst()
                .orElseThrow();
    }
}