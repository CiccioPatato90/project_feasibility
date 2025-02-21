package org.acme.opt.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class SolverResource {
    private final String id;
    private final String name;
    private final int availableCapacity;
    private final int cost;
}
