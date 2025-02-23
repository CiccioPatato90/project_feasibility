package org.acme.opt.models.enums;

import resourceallocation.GreedyCriteria;
import resourceallocation.GreedyCriteriaOrder;

public enum GreedyStrategy {
    PROJECT_SIZE,ASSOCIATION_ACTIVITY,CREATION_DATE, UNKNOWN;

    public static GreedyStrategy fromProto(GreedyCriteria protoEnum) {
        return switch (protoEnum) {
            case PROJECT_SIZE -> PROJECT_SIZE;
            case ASSOCIATION_ACTIVITY -> ASSOCIATION_ACTIVITY;
            case CREATION_DATE -> CREATION_DATE;
            default -> UNKNOWN;
        };
    }
}
