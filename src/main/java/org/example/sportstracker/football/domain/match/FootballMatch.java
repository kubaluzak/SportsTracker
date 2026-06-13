package org.example.sportstracker.football.domain.match;


import lombok.Getter;
import org.example.sportstracker.core.domain.competitor.Competitor;
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

import static java.util.Collections.unmodifiableList;

public class FootballMatch implements Match {
    private MatchStatus status = MatchStatus.SCHEDULED;
    private final FootballScore score;

    private final List<FootballMatchEvent> events = new ArrayList<>();

    @Getter
    private Team homeTeam;
    @Getter
    private Team awayTeam;

    private final MatchResolutionStrategy resolutionStrategy;
    private final List<MatchEventListener> listeners = new ArrayList<>();

    @Getter
    private boolean extraTimePlayed;
    @Getter
    private boolean penaltyShootoutStarted;
    private FootballResult manualResult;

    public FootballMatch(Team homeTeam, Team awayTeam, MatchResolutionStrategy resolutionStrategy) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.resolutionStrategy = resolutionStrategy;
        this.score = new FootballScore(homeTeam, awayTeam);
    }

    public List<FootballMatchEvent> getEvents() {
        return unmodifiableList(events);
    }

    @Override
    public void addMatchEventListener(MatchEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void startMatch() {
        if (status != MatchStatus.SCHEDULED) {
            throw new IllegalStateException("Match is not scheduled");
        }

        status = MatchStatus.IN_PROGRESS;
    }

    @Override
    public void endMatch() {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only an in-progress match can be completed");
        }

        status = MatchStatus.COMPLETED;
    }

    @Override
    public void abandonMatch(String reason) {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only an in-progress match can be abandoned");
        }

        status = MatchStatus.ABANDONED;
    }

    public void assignWalkover(Team winner, int winnerGoals, int loserGoals) {
        if (!winner.equals(homeTeam) && !winner.equals(awayTeam)) {
            throw new IllegalArgumentException("Walkover winner must be one of the match teams");
        }

        Team loser = winner.equals(homeTeam) ? awayTeam : homeTeam;

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
            throw new IllegalStateException("Extra time can only start during an in-progress match");
        }

        extraTimePlayed = true;
    }

    public void startPenaltyShootout() {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Penalty shootout can only start during an in-progress match");
        }

        penaltyShootoutStarted = true;
    }

    @Override
    public void recordEvent(MatchEvent event) {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Events can only be recorded during an in-progress match");
        }

        if (!(event instanceof FootballMatchEvent footballEvent)) {
            throw new IllegalArgumentException("Invalid event type for a football match");
        }

        validateEventId(footballEvent);
        validateRelatedEvents(footballEvent);

        events.add(footballEvent);
        score.update(footballEvent);

        for (MatchEventListener listener : listeners) {
            listener.onEventRecorded(footballEvent, score);
        }
    }

    private void validateEventId(FootballMatchEvent event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            throw new IllegalArgumentException("Event must have an eventId");
        }

        boolean eventIdAlreadyExists = events.stream()
                .anyMatch(previousEvent -> previousEvent.getEventId().equals(event.getEventId()));

        if (eventIdAlreadyExists) {
            throw new IllegalArgumentException("Event with this eventId already exists in the match");
        }
    }

    private void validateRelatedEvents(FootballMatchEvent event) {
        if (!event.requiresRelatedEvents()) {
            return;
        }

        if (event.getRelatedEventIds() == null || event.getRelatedEventIds().isEmpty()) {
            throw new IllegalArgumentException("Event requires at least one relatedEventId");
        }

        List<FootballMatchEvent> relatedEvents = new ArrayList<>();

        for (String relatedEventId : event.getRelatedEventIds()) {
            if (relatedEventId == null || relatedEventId.isBlank()) {
                throw new IllegalArgumentException("relatedEventId cannot be empty");
            }

            FootballMatchEvent relatedEvent = events.stream()
                    .filter(previousEvent -> previousEvent.getEventId().equals(relatedEventId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "relatedEventId does not point to an existing match event: " + relatedEventId
                    ));

            relatedEvents.add(relatedEvent);
        }

        if (!event.canReferTo(relatedEvents)) {
            throw new IllegalArgumentException("Event cannot refer to the provided related events");
        }
    }

    public FootballScore replayScore() {
        // Odtwarza aktualny wynik od zera na podstawie eventów.
        FootballScore replayedScore = new FootballScore(homeTeam, awayTeam);

        for (FootballMatchEvent event : events) {
            replayedScore.update(event);
        }

        return replayedScore;
    }

    public List<ScoreSnapshot> replayTimeline() {
        FootballScore replayedScore = new FootballScore(homeTeam, awayTeam);
        List<ScoreSnapshot> timeline = new ArrayList<>();

        for (FootballMatchEvent event : events) {
            replayedScore.update(event);

            timeline.add(new ScoreSnapshot(
                    event.getMinute(),
                    event.getEventId(),
                    event.getEventName(),
                    event.getDescription(),
                    event.getRelatedEventIds(),
                    replayedScore.getHomeGoals(),
                    replayedScore.getAwayGoals(),
                    replayedScore.getHomePenalties(),
                    replayedScore.getAwayPenalties(),
                    replayedScore.getHomeYellowCards(),
                    replayedScore.getAwayYellowCards(),
                    replayedScore.getHomeRedCards(),
                    replayedScore.getAwayRedCards(),
                    replayedScore.display()
            ));
        }

        return timeline;
    }

    public void printTimeline() {
        for (ScoreSnapshot snapshot : replayTimeline()) {
            System.out.println(
                    snapshot.minute()
                            + "' | "
                            + snapshot.eventName()
                            + " | "
                            + snapshot.description()
                            + " | Score: "
                            + snapshot.display()
                            + relatedInfo(snapshot.relatedEventIds())
            );
        }
    }

    private String relatedInfo(List<String> relatedEventIds) {
        if (relatedEventIds == null || relatedEventIds.isEmpty()) {
            return "";
        }

        return " | Related events: " + relatedEventIds;
    }

    @Override
    public FootballScore getScore() {
        return new FootballScore(score);
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

        private final Map<String, FootballMatchEvent> appliedEvents = new HashMap<>();
        private final Map<String, FootballMatchEvent> undoingEvents = new HashMap<>();

        public FootballScore(Team homeTeam, Team awayTeam) {
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
        }

        // Copy constructor
        public FootballScore(FootballScore source) {
            this.homeGoals = source.homeGoals;
            this.awayGoals = source.awayGoals;
            this.homePenalties = source.homePenalties;
            this.awayPenalties = source.awayPenalties;
            this.homeYellowCards = source.homeYellowCards;
            this.awayYellowCards = source.awayYellowCards;
            this.homeRedCards = source.homeRedCards;
            this.awayRedCards = source.awayRedCards;

            this.homeTeam = deepCopyTeam(source.homeTeam);
            this.awayTeam = deepCopyTeam(source.awayTeam);

        }


        private Team deepCopyTeam(Team original) {
            if (original == null) return null;

            return Team.builder()
                    .id(original.getId())
                    .name(original.getName())
                    .manager(original.getManager())
                    .players(original.getPlayers() != null ? new ArrayList<>(original.getPlayers()) : null)
                    .build();
        }

        void applyWalkover(int homeGoals, int awayGoals) {
            // Walkower ustawia wynik ręcznie.
            this.homeGoals = homeGoals;
            this.awayGoals = awayGoals;
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

            throw new IllegalArgumentException("Team is not part of this match");
        }

        public int getHomeFairPlayPenaltyPoints() {
            return homeYellowCards + homeRedCards * 3;
        }

        public int getAwayFairPlayPenaltyPoints() {
            return awayYellowCards + awayRedCards * 3;
        }

        @Override
        public String display() {
            String baseScore = String.format(
                    "%s %d - %d %s",
                    homeTeam.getName(),
                    homeGoals,
                    awayGoals,
                    awayTeam.getName()
            );

            String penalties = "";
            if (homePenalties > 0 || awayPenalties > 0) {
                penalties = String.format(" (Penalties: %d - %d)", homePenalties, awayPenalties);
            }

            String cards = String.format(
                    " | Cards: %s Y:%d R:%d, %s Y:%d R:%d",
                    homeTeam.getName(),
                    homeYellowCards,
                    homeRedCards,
                    awayTeam.getName(),
                    awayYellowCards,
                    awayRedCards
            );

            return baseScore + penalties + cards;
        }

        @Override
        public void update(MatchEvent event) {
            if (!(event instanceof FootballMatchEvent footballEvent)) {
                return;
            }

            applyEvent(footballEvent);
        }

        void applyEvent(FootballMatchEvent event) {
            // Event sam wie, jak zmienić wynik.
            event.applyTo(this);

            if (event.canBeUndone()) {
                appliedEvents.put(event.getEventId(), event);
            }
        }

        void undoEvent(String relatedEventId, FootballMatchEvent undoingEvent) {
            // Cofamy konkretny wcześniejszy event.
            if (relatedEventId == null || relatedEventId.isBlank()) {
                return;
            }

            if (undoingEvents.containsKey(relatedEventId)) {
                return;
            }

            FootballMatchEvent eventToUndo = appliedEvents.get(relatedEventId);

            if (eventToUndo == null) {
                return;
            }

            eventToUndo.undoFrom(this);
            undoingEvents.put(relatedEventId, undoingEvent);
        }

        void undoEvents(List<String> relatedEventIds, FootballMatchEvent undoingEvent) {
            // Cofamy kilka eventów naraz.
            if (relatedEventIds == null) {
                return;
            }

            for (String relatedEventId : relatedEventIds) {
                undoEvent(relatedEventId, undoingEvent);
            }
        }

        public boolean isEventUndone(String eventId) {
            return undoingEvents.containsKey(eventId);
        }

        public FootballMatchEvent getUndoingEvent(String eventId) {
            return undoingEvents.get(eventId);
        }

        void addGoalFor(Competitor actor) {
            Team team = resolveTeam(actor);

            if (team.equals(homeTeam)) {
                homeGoals++;
            } else {
                awayGoals++;
            }
        }

        void removeGoalFor(Competitor actor) {
            Team team = resolveTeam(actor);

            if (team.equals(homeTeam) && homeGoals > 0) {
                homeGoals--;
            } else if (team.equals(awayTeam) && awayGoals > 0) {
                awayGoals--;
            }
        }

        void addShootoutPenaltyFor(Competitor actor) {
            Team team = resolveTeam(actor);

            if (team.equals(homeTeam)) {
                homePenalties++;
            } else {
                awayPenalties++;
            }
        }

        void removeShootoutPenaltyFor(Competitor actor) {
            Team team = resolveTeam(actor);

            if (team.equals(homeTeam) && homePenalties > 0) {
                homePenalties--;
            } else if (team.equals(awayTeam) && awayPenalties > 0) {
                awayPenalties--;
            }
        }

        void addYellowCardFor(Competitor actor) {
            Team team = resolveTeam(actor);

            if (team.equals(homeTeam)) {
                homeYellowCards++;
            } else {
                awayYellowCards++;
            }
        }

        void removeYellowCardFor(Competitor actor) {
            Team team = resolveTeam(actor);

            if (team.equals(homeTeam) && homeYellowCards > 0) {
                homeYellowCards--;
            } else if (team.equals(awayTeam) && awayYellowCards > 0) {
                awayYellowCards--;
            }
        }

        void addRedCardFor(Competitor actor) {
            Team team = resolveTeam(actor);

            if (team.equals(homeTeam)) {
                homeRedCards++;
            } else {
                awayRedCards++;
            }
        }

        void removeRedCardFor(Competitor actor) {
            Team team = resolveTeam(actor);

            if (team.equals(homeTeam) && homeRedCards > 0) {
                homeRedCards--;
            } else if (team.equals(awayTeam) && awayRedCards > 0) {
                awayRedCards--;
            }
        }

        private Team resolveTeam(Competitor actor) {
            // Sprawdza, czy actor to jedna z drużyn meczu.
            if (actor == null) {
                throw new IllegalArgumentException("Event actor cannot be null");
            }

            if (actor.equals(homeTeam)) {
                return homeTeam;
            }

            if (actor.equals(awayTeam)) {
                return awayTeam;
            }

            throw new IllegalArgumentException("Event actor is not part of this match");
        }
    }
}