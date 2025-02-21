package org.acme.opt.solvers;

import org.acme.opt.generators.ProjectGenerator;
import org.acme.opt.generators.ResourceGenerator;
import org.acme.opt.models.SolverProject;
import org.acme.opt.models.SolverResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MaximizeResourceUsageEntrypoint {
    public static void main(String[] args) {
        ResourceGenerator resourceGen = new ResourceGenerator.Builder()
                .numResources(1000)
                .minCapacity(70)
                .distribution(ResourceGenerator.CapacityDistribution.NORMAL)
                .build();

        ProjectGenerator projectGen = new ProjectGenerator.Builder()
                .numProjects(800)
                .resources(resourceGen.generate())
                .profile(ProjectGenerator.RequirementProfile.BALANCED)
                .utilizationTarget(100)
                .build();

        List<SolverResource> solverResources = resourceGen.generate();
        List<SolverProject> solverProjects = projectGen.generate();

        // Solve feasibility
        var solver = new MaximizeResourceUsage(solverResources, solverProjects);
        var result = solver.solve();

        // Added Code: Unified Statistics Summary
        System.out.println("\n=== FEASIBILITY STATS SUMMARY ===\n");

        // Per-Project Stats
        System.out.println("Per-Project Stats:");
        for (Map.Entry<SolverProject, List<SolverResource>> entry : result.entrySet()) {
            SolverProject solverProject = entry.getKey();
            List<SolverResource> assigned = entry.getValue();
            double completion = solver.calculateProjectCompletion(solverProject, assigned);

            Map<String, Integer> assignmentCount = assigned.stream()
                    .collect(Collectors.groupingBy(
                            SolverResource::getName,
                            Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                    ));
            // Calculate missing resources to complete the project's requirements.
            Map<String, Integer> missingResources = new HashMap<>();
            Map<String, Integer> requirements = solverProject.getRequirements();
            for (Map.Entry<String, Integer> reqEntry : requirements.entrySet()) {
                String resourceName = reqEntry.getKey();
                int required = reqEntry.getValue();
                int assignedCount = assignmentCount.getOrDefault(resourceName, 0);
                if (assignedCount < required) {
                    missingResources.put(resourceName, required - assignedCount);
                }
            }

            System.out.printf("Project %s: Completion = %.2f%%, Resources Assigned = %d%n",
                    solverProject.getName(), completion, assigned.size());
            System.out.println("   Assigned Resources:");
            assignmentCount.forEach((res, count) ->
                    System.out.printf("      %s: %d%n", res, count));

            System.out.println("   Required Resources:");
            requirements.forEach((res, count) ->
                    System.out.printf("      %s: %d%n", res, count));

            if (!missingResources.isEmpty()) {
                System.out.println("   Missing Resources:");
                missingResources.forEach((res, count) ->
                        System.out.printf("      %s: %d%n", res, count));
            } else {
                System.out.println("   All resource requirements met.");
            }
        }

        // Global Stats
        int totalAvailable = solverResources.stream().mapToInt(SolverResource::getAvailableCapacity).sum();
        int totalUsed = result.values().stream().mapToInt(List::size).sum();
        double avgUsed = (double) totalUsed / solverProjects.size();
        System.out.println("\nGlobal Stats:");
        System.out.printf("Total Resources Available: %d%n", totalAvailable);
        System.out.printf("Total Resources Used: %d%n", totalUsed);
        System.out.printf("Average Resources per Project: %.2f%n", avgUsed);
        System.out.printf("Unused Resources: %d%n", totalAvailable - totalUsed);

        // Global Resource Assignment Breakdown
        Map<String, Integer> globalAssignment = result.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(
                        SolverResource::getName,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
        System.out.println("\nGlobal Resource Assignment Breakdown:");
        globalAssignment.forEach((res, count) ->
                System.out.printf("Resource %s: Assigned %d times%n", res, count));

        // Additional Interesting Stats: Most/Least Assigned Resources
        Optional<Map.Entry<String, Integer>> mostAssigned = globalAssignment.entrySet().stream()
                .max(Map.Entry.comparingByValue());
        Optional<Map.Entry<String, Integer>> leastAssigned = globalAssignment.entrySet().stream()
                .min(Map.Entry.comparingByValue());

        mostAssigned.ifPresent(entry ->
                System.out.printf("\nMost Assigned Resource: %s (%d times)%n", entry.getKey(), entry.getValue()));
        leastAssigned.ifPresent(entry ->
                System.out.printf("Least Assigned Resource: %s (%d times)%n", entry.getKey(), entry.getValue()));

        // Per Resource Stats
        System.out.println("\nPer Resource Stats:");
        for (SolverResource solverResource : solverResources) {
            System.out.printf("Resource %s: Capacity = %d, Cost = %d, Assigned Count = %d%n",
                    solverResource.getName(),
                    solverResource.getAvailableCapacity(),
                    solverResource.getCost(),
                    globalAssignment.getOrDefault(solverResource.getName(), 0));
        }

    }
}
