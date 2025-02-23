package org.acme.opt.generators;

import org.acme.opt.models.SolverProject;
import org.acme.opt.models.SolverResource;

import java.util.*;

public class ProjectGenerator {
    public enum RequirementProfile {
        BALANCED,          // Similar requirements across resources
        SPARSE,           // Few high requirements, many low/zero
        COMPLEMENTARY,    // Projects tend to need different resources
        COMPETITIVE,      // Projects compete for same resources
        SEASONAL          // Requirements follow a pattern across resources
    }

    private final int numProjects;
    private final List<SolverResource> solverResources;
    private final RequirementProfile profile;
    private final double utilizationTarget;  // Target resource utilization (0.0 to 1.0)
    private final Random random;

    private ProjectGenerator(Builder builder) {
        this.numProjects = builder.numProjects;
        this.solverResources = builder.solverResources;
        this.profile = builder.profile;
        this.utilizationTarget = builder.utilizationTarget;
        this.random = new Random(builder.seed);
    }

    public List<SolverProject> generate() {
        List<SolverProject> solverProjects = new ArrayList<>();

        switch (profile) {
            case BALANCED -> generateBalancedProjects(solverProjects);
            case SPARSE -> generateSparseProjects(solverProjects);
            case COMPLEMENTARY -> generateComplementaryProjects(solverProjects);
            case COMPETITIVE -> generateCompetitiveProjects(solverProjects);
            case SEASONAL -> generateSeasonalProjects(solverProjects);
        }

        return solverProjects;
    }

    private void generateBalancedProjects(List<SolverProject> solverProjects) {
        for (int i = 0; i < numProjects; i++) {
            Map<String, Integer> requirements = new HashMap<>();
            for (SolverResource solverResource : solverResources) {
                int capacity = solverResource.getAvailableCapacity();
                int maxReq = (int) (capacity * utilizationTarget / numProjects);
                requirements.put(solverResource.getName(), maxReq + random.nextInt(maxReq/2));
            }
            solverProjects.add(new SolverProject("", "proj"+i,"Project" + i, requirements, i));
        }
    }

    private void generateSparseProjects(List<SolverProject> solverProjects) {
        for (int i = 0; i < numProjects; i++) {
            Map<String, Integer> requirements = new HashMap<>();
            int numRequiredResources = Math.max(1, solverResources.size() / 5);  // Use 20% of resources

            List<SolverResource> shuffledSolverResources = new ArrayList<>(solverResources);
            Collections.shuffle(shuffledSolverResources, random);

            for (int j = 0; j < numRequiredResources; j++) {
                SolverResource solverResource = shuffledSolverResources.get(j);
                int capacity = solverResource.getAvailableCapacity();
                int maxReq = (int) (capacity * utilizationTarget);
                requirements.put(solverResource.getName(), maxReq + random.nextInt(maxReq/2));
            }
            solverProjects.add(new SolverProject("", "proj"+i,"Project" + i, requirements, i));
        }
    }

    private void generateComplementaryProjects(List<SolverProject> solverProjects) {
        // Divide resources into groups
        List<List<SolverResource>> resourceGroups = new ArrayList<>();
        List<SolverResource> remainingSolverResources = new ArrayList<>(solverResources);

        while (!remainingSolverResources.isEmpty()) {
            int groupSize = Math.min(remainingSolverResources.size(),
                    Math.max(1, solverResources.size() / numProjects));
            List<SolverResource> group = remainingSolverResources.subList(0, groupSize);
            resourceGroups.add(new ArrayList<>(group));
            remainingSolverResources = remainingSolverResources.subList(groupSize, remainingSolverResources.size());
        }

        // Generate projects that primarily use one group
        for (int i = 0; i < numProjects; i++) {
            Map<String, Integer> requirements = new HashMap<>();
            List<SolverResource> primaryGroup = resourceGroups.get(i % resourceGroups.size());

            // High requirements for primary group
            for (SolverResource solverResource : primaryGroup) {
                int capacity = solverResource.getAvailableCapacity();
                int maxReq = (int) (capacity * utilizationTarget);
                requirements.put(solverResource.getName(), maxReq + random.nextInt(maxReq/2));
            }

            // Low requirements for other resources
            for (SolverResource solverResource : solverResources) {
                if (!primaryGroup.contains(solverResource)) {
                    int capacity = solverResource.getAvailableCapacity();
                    int maxReq = (int) (capacity * utilizationTarget * 0.2);  // 20% of normal
                    requirements.put(solverResource.getName(), random.nextInt(maxReq));
                }
            }

            solverProjects.add(new SolverProject("", "proj"+i,"Project" + i, requirements, i));
        }
    }

    private void generateCompetitiveProjects(List<SolverProject> solverProjects) {
        // Select highly contested resources
        int numContested = Math.max(1, solverResources.size() / 3);  // 33% of resources are contested
        List<SolverResource> contestedSolverResources = new ArrayList<>(solverResources.subList(0, numContested));

        for (int i = 0; i < numProjects; i++) {
            Map<String, Integer> requirements = new HashMap<>();

            // High requirements for contested resources
            for (SolverResource solverResource : contestedSolverResources) {
                int capacity = solverResource.getAvailableCapacity();
                int maxReq = (int) (capacity * utilizationTarget);
                requirements.put(solverResource.getName(), maxReq + random.nextInt(maxReq/2));
            }

            // Normal requirements for other resources
            for (SolverResource solverResource : solverResources) {
                if (!contestedSolverResources.contains(solverResource)) {
                    int capacity = solverResource.getAvailableCapacity();
                    int maxReq = (int) (capacity * utilizationTarget / numProjects);
                    requirements.put(solverResource.getName(), random.nextInt(maxReq));
                }
            }

            solverProjects.add(new SolverProject("", "proj"+i,"Project" + i, requirements, i));
        }
    }

    private void generateSeasonalProjects(List<SolverProject> solverProjects) {
        // Create a seasonal pattern
        double[] seasonalPattern = new double[solverResources.size()];
        int seasonLength = Math.max(1, solverResources.size() / 4);  // Four seasons

        for (int i = 0; i < solverResources.size(); i++) {
            seasonalPattern[i] = 0.5 + 0.5 * Math.sin(2 * Math.PI * i / seasonLength);
        }

        for (int i = 0; i < numProjects; i++) {
            Map<String, Integer> requirements = new HashMap<>();

            for (int j = 0; j < solverResources.size(); j++) {
                SolverResource solverResource = solverResources.get(j);
                int capacity = solverResource.getAvailableCapacity();
                double seasonalFactor = seasonalPattern[(j + i) % solverResources.size()];
                int maxReq = (int) (capacity * utilizationTarget * seasonalFactor);
                requirements.put(solverResource.getName(), maxReq + random.nextInt(maxReq/2));
            }

            solverProjects.add(new SolverProject("", "proj"+i,"Project" + i, requirements, i % 9));
        }
    }

    public static class Builder {
        private int numProjects = 5;
        private List<SolverResource> solverResources = new ArrayList<>();
        private RequirementProfile profile = RequirementProfile.BALANCED;
        private double utilizationTarget = 0.7;
        private long seed = 42;

        public Builder numProjects(int val) {
            numProjects = val;
            return this;
        }

        public Builder resources(List<SolverResource> val) {
            solverResources = val;
            return this;
        }

        public Builder profile(RequirementProfile val) {
            profile = val;
            return this;
        }

        public Builder utilizationTarget(double val) {
            utilizationTarget = val;
            return this;
        }

        public Builder seed(long val) {
            seed = val;
            return this;
        }

        public ProjectGenerator build() {
            return new ProjectGenerator(this);
        }
    }
}
