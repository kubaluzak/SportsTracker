package org.example.sportstracker.snooker.domain.bracket;

import lombok.Getter;
import org.example.sportstracker.core.domain.match.MatchFactory;
import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.snooker.domain.competitor.Player;
import org.example.sportstracker.snooker.domain.match.SnookerMatch;
import org.example.sportstracker.snooker.domain.match.SnookerMatchFactory;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SnookerBracketTree {
    private final List<List<SnookerMatch>> rounds = new ArrayList<>();
    private final MatchFactory factory;

    public SnookerBracketTree(int defaultBestOfFrames) {
        this.factory = new SnookerMatchFactory(defaultBestOfFrames);
    }

    public void setFirstRound(List<SnookerMatch> initialMatches) {
        if (initialMatches == null || initialMatches.isEmpty()) {
            throw new IllegalArgumentException("First round of the bracket cannot be empty");
        }
        rounds.clear();
        rounds.add(initialMatches);
    }

    public void generateNextRound() {
        if (rounds.isEmpty()) {
            throw new IllegalStateException("First round has not been set");
        }

        List<SnookerMatch> currentRound = rounds.get(rounds.size() - 1);

        if (currentRound.size() % 2 != 0) {
            throw new IllegalStateException("A knockout round must have an even number of matches");
        }

        for (SnookerMatch match : currentRound) {
            if (match.getStatus() != MatchStatus.COMPLETED && match.getStatus() != MatchStatus.WALKOVER) {
                throw new IllegalStateException("Not all matches in the current round are completed!");
            }
            // Draws are prohibited.
            if (match.getResult().getWinner() == null) {
                throw new IllegalStateException("Draws are prohibited. Every match must have a winner.");
            }
        }

        List<SnookerMatch> nextRound = new ArrayList<>();
        for (int i = 0; i < currentRound.size(); i += 2) {
            Player winner1 = (Player) currentRound.get(i).getResult().getWinner();
            Player winner2 = (Player) currentRound.get(i + 1).getResult().getWinner();

            SnookerMatch nextMatch = (SnookerMatch) factory.createKnockoutMatch(winner1, winner2);
            nextRound.add(nextMatch);
        }
        rounds.add(nextRound);
    }

    public void printConsoleVisualization() {
        System.out.println("\n============= SNOOKER BRACKET =============");
        for (List<SnookerMatch> round : rounds) {
            System.out.println("\n--- " + getRoundName(round.size()) + " ---");
            for (SnookerMatch match : round) {
                String p1 = match.getPlayer1() != null ? match.getPlayer1().getName() : "TBD";
                String p2 = match.getPlayer2() != null ? match.getPlayer2().getName() : "TBD";

                if (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.WALKOVER) {
                    String score = match.getScore().display();
                    Player winner = (Player) match.getResult().getWinner();
                    System.out.printf("[%s vs %s] -> Score: %s (Advances: %s)%n", p1, p2, score, winner.getName());
                } else {
                    System.out.printf("[%s vs %s] -> [ Scheduled ]%n", p1, p2);
                }
            }
        }
        System.out.println("===========================================\n");
    }

    private String getRoundName(int matchCount) {
        return switch (matchCount) {
            case 1 -> "FINAL";
            case 2 -> "SEMI-FINALS";
            case 4 -> "QUARTER-FINALS";
            case 8 -> "LAST 16";
            case 16 -> "LAST 32";
            default -> "ROUND OF " + (matchCount * 2);
        };
    }
}