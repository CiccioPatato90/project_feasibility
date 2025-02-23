package org.acme.opt.models.enums;

import resourceallocation.GreedyCriteriaOrder;

public enum GreedyOrder {
    LARGEST_FIRST,SMALLEST_FIRST, UNKNOWN;

    public static GreedyOrder fromProto(GreedyCriteriaOrder protoEnum) {
        return switch (protoEnum) {
            case LARGEST_FIRST -> LARGEST_FIRST;
            case SMALLEST_FIRST -> SMALLEST_FIRST;
            default -> UNKNOWN;
        };
    }
}