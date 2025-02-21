package org.acme.opt.stats;

import lombok.AllArgsConstructor;
import org.acme.opt.models.SolverProject;
import org.acme.opt.models.SolverResource;
import org.acme.opt.solvers.MaximizeResourceUsage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ResourceAllocationStats {
    private final List<SolverResource> resources;
    private final List<SolverProject> projects;
    private final MaximizeResourceUsage solver;

    public void printPerProjectStats(Map<SolverProject, List<SolverResource>> result) {
        System.out.println("Per-Project Stats:");
        for (Map.Entry<SolverProject, List<SolverResource>> entry : result.entrySet()) {
            SolverProject project = entry.getKey();
            List<SolverResource> assigned = entry.getValue();
            double completion = solver.calculateProjectCompletion(project, assigned);

            Map<String, Integer> assignmentCount = assigned.stream()
                    .collect(Collectors.groupingBy(
                            SolverResource::getId,
                            Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                    ));

            // Calculate missing resources to complete project's requirements
            Map<String, Integer> missingResources = new HashMap<>();
            Map<String, Integer> requirements = project.getRequirements();
            for (Map.Entry<String, Integer> reqEntry : requirements.entrySet()) {
                String resourceId = reqEntry.getKey();
                int required = reqEntry.getValue();
                int assignedCount = assignmentCount.getOrDefault(resourceId, 0);
                if (assignedCount < required) {
                    missingResources.put(resourceId, required - assignedCount);
                }
            }

            System.out.printf("Project %s: Completion = %.2f%%, Resources Assigned = %d%n",
                    project.getName(), completion, assigned.size());
            System.out.println("   Assigned Resources:");
            assignmentCount.forEach((res, count) -> System.out.printf("      %s: %d%n", res, count));

            System.out.println("   Required Resources:");
            requirements.forEach((res, count) -> System.out.printf("      %s: %d%n", res, count));

            if (!missingResources.isEmpty()) {
                System.out.println("   Missing Resources:");
                missingResources.forEach((res, count) -> System.out.printf("      %s: %d%n", res, count));
            } else {
                System.out.println("   All resource requirements met.");
            }
        }
    }

    public void printGlobalStats(Map<SolverProject, List<SolverResource>> result) {
        int totalAvailable = resources.stream().mapToInt(SolverResource::getAvailableCapacity).sum();
        int totalUsed = result.values().stream().mapToInt(List::size).sum();
        double avgUsed = (double) totalUsed / projects.size();

        System.out.println("\nGlobal Stats:");
        System.out.printf("Total Resources Available: %d%n", totalAvailable);
        System.out.printf("Total Resources Used: %d%n", totalUsed);
        System.out.printf("Average Resources per Project: %.2f%n", avgUsed);
        System.out.printf("Unused Resources: %d%n", totalAvailable - totalUsed);
    }

    public void printGlobalResourceAssignmentBreakdown(Map<SolverProject, List<SolverResource>> result) {
        Map<String, Integer> globalAssignment = result.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(
                        SolverResource::getName,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
        System.out.println("\nGlobal Resource Assignment Breakdown:");
        globalAssignment.forEach((res, count) -> System.out.printf("Resource %s: Assigned %d times%n", res, count));

        Optional<Map.Entry<String, Integer>> mostAssigned = globalAssignment.entrySet().stream()
                .max(Map.Entry.comparingByValue());
        Optional<Map.Entry<String, Integer>> leastAssigned = globalAssignment.entrySet().stream()
                .min(Map.Entry.comparingByValue());

        mostAssigned.ifPresent(entry ->
                System.out.printf("\nMost Assigned Resource: %s (%d times)%n", entry.getKey(), entry.getValue()));
        leastAssigned.ifPresent(entry ->
                System.out.printf("Least Assigned Resource: %s (%d times)%n", entry.getKey(), entry.getValue()));
    }

    public void printPerResourceStats(Map<SolverProject, List<SolverResource>> result) {
        Map<String, Integer> globalAssignment = result.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(
                        SolverResource::getName,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
        System.out.println("\nPer Resource Stats:");
        for (SolverResource resource : resources) {
            System.out.printf("Resource %s: Capacity = %d, Cost = %d, Assigned Count = %d%n",
                    resource.getName(),
                    resource.getAvailableCapacity(),
                    resource.getCost(),
                    globalAssignment.getOrDefault(resource.getName(), 0));
        }
    }

    public void printAllStats(Map<SolverProject, List<SolverResource>> result) {
        System.out.println("\n=== FEASIBILITY STATS SUMMARY ===\n");
        printPerProjectStats(result);
        printGlobalStats(result);
        printGlobalResourceAssignmentBreakdown(result);
        printPerResourceStats(result);
    }
}
