package org.example.sportstracker.core.domain.match;

import org.example.sportstracker.core.domain.result.Result;
import org.example.sportstracker.core.domain.score.Score;

public interface Match {
    void startMatch();
    void endMatch();
    void abandonMatch(String reason);
    void recordEvent(MatchEvent event);
    Score getScore();
    Result getResult();
    MatchStatus getStatus();

    void addMatchEventListener(MatchEventListener listener);
}