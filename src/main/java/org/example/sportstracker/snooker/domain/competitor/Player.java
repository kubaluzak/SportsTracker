package org.example.sportstracker.snooker.domain.competitor;

import lombok.Builder;
import lombok.Data;
import org.example.sportstracker.core.domain.competitor.Competitor;

@Data
@Builder
public class Player implements Competitor {
    private String id;
    private String name;

    private int rank;
    private String nationality;
}