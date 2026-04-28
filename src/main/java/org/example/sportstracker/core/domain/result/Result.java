package org.example.sportstracker.core.domain.result;

import org.example.sportstracker.core.domain.competitor.Competitor;

public interface Result {
    Competitor getWinner();
    boolean isDraw();
    boolean isCompletedNormally();
}