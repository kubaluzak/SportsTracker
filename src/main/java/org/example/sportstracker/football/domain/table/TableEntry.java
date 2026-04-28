package org.example.sportstracker.football.domain.table;

import lombok.Data;
import org.example.sportstracker.football.domain.competitor.Team;

@Data
public class TableEntry {
    private Team team;
    private int points = 0;
    private int goalsScored = 0;
    private int goalsConceded = 0;
    private int yellowCards = 0;
    private int redCards = 0;
    private int fairPlayPenaltyPoints = 0;

    public TableEntry(Team team) {
        this.team = team;
    }

    public int getGoalDifference() {
        return goalsScored - goalsConceded;
    }

    public void addResult(int scored, int conceded) {
        addResult(scored, conceded, 3);
    }

    public void addResult(int scored, int conceded, int pointsForWin) {
        goalsScored += scored;
        goalsConceded += conceded;

        if (scored > conceded) {
            points += pointsForWin;
        } else if (scored == conceded) {
            points += 1;
        }
    }

    public void addCards(int yellowCards, int redCards) {
        this.yellowCards += yellowCards;
        this.redCards += redCards;
        this.fairPlayPenaltyPoints += yellowCards + redCards * 3;
    }

    public void resetStats() {
        points = 0;
        goalsScored = 0;
        goalsConceded = 0;
        yellowCards = 0;
        redCards = 0;
        fairPlayPenaltyPoints = 0;
    }
}