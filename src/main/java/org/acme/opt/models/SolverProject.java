package org.acme.opt.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Map;
@AllArgsConstructor
@Getter
@Setter
public class SolverProject {
    private String completionRate;
    private final String id;
    private final String name;
    private final Map<String, Integer> resourceRequirements;
    private final int priority;

    public Map<String, Integer> getRequirements() { return Collections.unmodifiableMap(resourceRequirements); }
}
