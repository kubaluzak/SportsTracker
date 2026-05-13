package org.example.sportstracker.snooker.domain.stage;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.Match;
import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.core.domain.tournament.AbstractTournamentStage;
import org.example.sportstracker.snooker.domain.bracket.SnookerBracketTree;
import org.example.sportstracker.snooker.domain.match.SnookerMatch;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SnookerKnockoutStage extends AbstractTournamentStage {

    private SnookerBracketTree brackets;

    public SnookerKnockoutStage(int defaultBestOfFrames) {
        this.brackets = new SnookerBracketTree(defaultBestOfFrames);
    }

    public void advanceWinner(SnookerMatch match) {
        if (match.getStatus() != MatchStatus.COMPLETED && match.getStatus() != MatchStatus.WALKOVER) {
            throw new IllegalStateException("Cannot advance a winner from an incomplete match");
        }

        if (match.getResult().isDraw()) {
            throw new IllegalStateException("Draws are explicitly not allowed in this snooker tournament stage.");
        }

        Competitor winner = match.getResult().getWinner();
        if (winner == null) {
            throw new IllegalStateException("Snooker knockout match must have a defined winner");
        }

        advancingTeams.add(winner);
    }

    @Override
    public void startStage() {
        System.out.println("\n>>> START SNOOKER KNOCKOUT STAGE <<<");
    }

    @Override
    protected void verifyMatchesCompleted() {
        for (Match match : stageMatches) {
            if (match.getStatus() != MatchStatus.COMPLETED && match.getStatus() != MatchStatus.WALKOVER) {
                throw new IllegalStateException("Not all snooker matches in this stage are completed");
            }
            if (match.getResult().getWinner() == null) {
                throw new IllegalStateException("A match ended without a winner. Draws are prohibited.");
            }
        }
    }

    @Override
    protected void processResults() {
        advancingTeams.clear();

        // If a bracket is actively managing this stage, the manual advancing list is ignored
        if (!brackets.getRounds().isEmpty()) {
            return;
        }

        // Fallback for simple single-round knockout stages without full bracket tracking
        for (Match match : stageMatches) {
            advanceWinner((SnookerMatch) match);
        }
    }

    @Override
    protected void determineAdvancingTeams() {
        if (!brackets.getRounds().isEmpty()) {
            List<SnookerMatch> finalRound = brackets.getRounds().get(brackets.getRounds().size() - 1);

            if (finalRound.size() == 1 && (finalRound.get(0).getStatus() == MatchStatus.COMPLETED
                    || finalRound.get(0).getStatus() == MatchStatus.WALKOVER)) {

                advancingTeams.clear();
                advancingTeams.add(finalRound.get(0).getResult().getWinner());
            }
        }
    }

    @Override
    protected void printStageSummary() {
        if (!brackets.getRounds().isEmpty()) {
            brackets.printConsoleVisualization();
        }
    }
}