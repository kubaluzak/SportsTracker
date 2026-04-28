package org.example.sportstracker.football.domain.bracket;

import lombok.Getter;
import org.example.sportstracker.core.domain.match.MatchFactory;
import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.football.domain.competitor.Team;
import org.example.sportstracker.football.domain.match.FootballMatch;
import org.example.sportstracker.football.domain.match.FootballMatchFactory;

import java.util.ArrayList;
import java.util.List;

@Getter
public class BracketTree {
    private final List<List<FootballMatch>> rounds = new ArrayList<>();

    private final MatchFactory factory = new FootballMatchFactory();

    public void setFirstRound(List<FootballMatch> initialMatches) {
        rounds.clear();
        rounds.add(initialMatches);
    }

    public void generateNextRound() {
        List<FootballMatch> currentRound = rounds.get(rounds.size() - 1);

        for (FootballMatch match : currentRound) {
            if (match.getStatus() != MatchStatus.COMPLETED) {
                throw new IllegalStateException("Nie wszystkie mecze w rundzie zostały zakończone!");
            }
        }

        List<FootballMatch> nextRound = new ArrayList<>();

        for (int i = 0; i < currentRound.size(); i += 2) {
            Team winner1 = (Team) currentRound.get(i).getResult().getWinner();
            Team winner2 = (Team) currentRound.get(i + 1).getResult().getWinner();

            FootballMatch nextMatch = (FootballMatch) factory.createKnockoutMatch(winner1, winner2);
            nextRound.add(nextMatch);
        }

        rounds.add(nextRound);
    }

    public void printConsoleVisualization() {
        System.out.println("\n============= DRABINKA PUCHAROWA =============");
        for (List<FootballMatch> round : rounds) {
            System.out.println("\n--- " + getRoundName(round.size()) + " ---");

            for (FootballMatch match : round) {
                String t1 = match.getHomeTeam() != null ? match.getHomeTeam().getName() : "TBD";
                String t2 = match.getAwayTeam() != null ? match.getAwayTeam().getName() : "TBD";

                if (match.getStatus() == MatchStatus.COMPLETED) {
                    String score = match.getScore().display();
                    Team winner = (Team) match.getResult().getWinner();
                    System.out.printf("[%s vs %s] -> Wynik: %s (Awansuje: %s)%n", t1, t2, score, winner.getName());
                } else {
                    System.out.printf("[%s vs %s] -> [ Mecz Zaplanowany ]%n", t1, t2);
                }
            }
        }
        System.out.println("==============================================\n");
    }

    private String getRoundName(int matchCount) {
        return switch (matchCount) {
            case 1 -> "FINAŁ";
            case 2 -> "PÓŁFINAŁY";
            case 4 -> "ĆWIERĆFINAŁY";
            case 8 -> "1/8 FINAŁU";
            default -> "RUNDA";
        };
    }
}