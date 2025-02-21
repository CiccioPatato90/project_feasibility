package org.acme.opt.generators;

import org.acme.opt.models.SolverResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class ResourceGenerator {
    public enum CapacityDistribution {
        UNIFORM,           // Even distribution between min and max
        NORMAL,           // Gaussian distribution around mean
        PARETO,           // Power law distribution (few high, many low)
        EXPONENTIAL,      // Exponentially decreasing capacities
        CLUSTERED         // Resources clustered around certain capacity levels
    }

    private final int numResources;
    private final int minCapacity;
    private final int maxCapacity;
    private final CapacityDistribution distribution;
    private final Random random;
    private final List<Integer> clusters;  // For CLUSTERED distribution

    private ResourceGenerator(Builder builder) {
        this.numResources = builder.numResources;
        this.minCapacity = builder.minCapacity;
        this.maxCapacity = builder.maxCapacity;
        this.distribution = builder.distribution;
        this.random = new Random(builder.seed);
        this.clusters = builder.clusters;
    }

    public List<SolverResource> generate() {
        List<SolverResource> solverResources = new ArrayList<>();
        Function<Integer, Integer> capacityGenerator = getCapacityGenerator();

        for (int i = 0; i < numResources; i++) {
            int capacity = capacityGenerator.apply(i);
            solverResources.add(new SolverResource("res"+i,"Resource" + i, capacity, capacity % (i+1)));
        }

        return solverResources;
    }

    private Function<Integer, Integer> getCapacityGenerator() {
        return switch (distribution) {
            case UNIFORM -> (i) -> minCapacity + random.nextInt(maxCapacity - minCapacity + 1);
            case NORMAL -> (i) -> {
                double mean = (minCapacity + maxCapacity) / 2.0;
                double stdDev = (maxCapacity - minCapacity) / 6.0;  // 99.7% within range
                int capacity;
                do {
                    capacity = (int) Math.round(random.nextGaussian() * stdDev + mean);
                } while (capacity < minCapacity || capacity > maxCapacity);
                return capacity;
            };
            case PARETO -> (i) -> {
                double alpha = 1.16;  // Shape parameter
                int capacity;
                do {
                    capacity = (int) (minCapacity / Math.pow(random.nextDouble(), 1 / alpha));
                } while (capacity > maxCapacity);
                return capacity;
            };
            case EXPONENTIAL -> (i) -> {
                double lambda = 1.0 / ((maxCapacity - minCapacity) / 4.0);
                return minCapacity + (int) (-Math.log(1 - random.nextDouble()) / lambda);
            };
            case CLUSTERED -> (i) -> {
                int cluster = clusters.get(random.nextInt(clusters.size()));
                int variation = (int) (cluster * 0.2);  // 20% variation around cluster
                return Math.max(minCapacity,
                        Math.min(maxCapacity,
                                cluster + random.nextInt(2 * variation + 1) - variation));
            };
        };
    }

    public static class Builder {
        private int numResources = 100;
        private int minCapacity = 10;
        private int maxCapacity = 100;
        private CapacityDistribution distribution = CapacityDistribution.UNIFORM;
        private long seed = 42;
        private List<Integer> clusters = Arrays.asList(20, 50, 80);  // Default clusters

        public Builder numResources(int val) {
            numResources = val;
            return this;
        }

        public Builder minCapacity(int val) {
            minCapacity = val;
            return this;
        }

        public Builder maxCapacity(int val) {
            maxCapacity = val;
            return this;
        }

        public Builder distribution(CapacityDistribution val) {
            distribution = val;
            return this;
        }

        public Builder seed(long val) {
            seed = val;
            return this;
        }

        public Builder clusters(List<Integer> val) {
            clusters = val;
            return this;
        }

        public ResourceGenerator build() {
            return new ResourceGenerator(this);
        }
    }

}
