package org.example.sportstracker.football.domain.match;

import lombok.Data;
import org.example.sportstracker.core.domain.match.Match;
import org.example.sportstracker.core.domain.match.MatchEvent;
import org.example.sportstracker.core.domain.match.MatchEventListener;
import org.example.sportstracker.core.domain.match.MatchResolutionStrategy;
import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.core.domain.result.Result;
import org.example.sportstracker.football.domain.competitor.Team;
import org.example.sportstracker.football.domain.result.FootballResult;
import org.example.sportstracker.football.domain.score.FootballScore;

import java.util.ArrayList;
import java.util.List;

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

        if (winner.equals(homeTeam)) {
            score.setHomeGoals(winnerGoals);
            score.setAwayGoals(loserGoals);
        } else {
            score.setAwayGoals(winnerGoals);
            score.setHomeGoals(loserGoals);
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

        events.add(fbEvent);
        score.update(event);

        for (MatchEventListener listener : listeners) {
            listener.onEventRecorded(event, score);
        }
    }

    @Override
    public FootballScore getScore() {
        return score;
    }

    @Override
    public Result getResult() {
        if (manualResult != null) {
            return manualResult;
        }

        return resolutionStrategy.resolve(score, homeTeam, awayTeam);
    }

    @Override
    public MatchStatus getStatus() {
        return status;
    }
}