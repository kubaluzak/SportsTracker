package org.example.sportstracker.football.domain.match;

public enum FootballEventTypeId {
    GOAL_SCORED,
    PENALTY_SCORED,
    PENALTY_MISSED,

    SHOOTOUT_PENALTY_SCORED,
    SHOOTOUT_PENALTY_MISSED,

    GOAL_DISALLOWED,
    PENALTY_DISALLOWED,
    EVENT_INVALIDATED,

    FOUL,
    SUBSTITUTION,
    VAR_REVIEW,

    YELLOW_CARD,
    RED_CARD
}