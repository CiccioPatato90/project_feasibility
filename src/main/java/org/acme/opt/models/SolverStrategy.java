package org.acme.opt.models;

import org.acme.opt.models.enums.GreedyOrder;
import org.acme.opt.models.enums.GreedyStrategy;
import resourceallocation.AllocationStrategy;

public record SolverStrategy (GreedyStrategy strategy, GreedyOrder order) {
    public static SolverStrategy fromProto(AllocationStrategy message) {
        return new SolverStrategy(GreedyStrategy.fromProto(message.getCriteria()), GreedyOrder.fromProto(message.getOrder()));
    }
}