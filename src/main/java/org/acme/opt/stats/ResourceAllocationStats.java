package org.acme.opt.stats;

import lombok.AllArgsConstructor;
import org.acme.opt.models.SolverProject;
import org.acme.opt.models.SolverResource;
import org.acme.opt.solvers.BaseSolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@AllArgsConstructor
public class ResourceAllocationStats {
    private final List<SolverResource> resources;
    private final List<SolverProject> projects;
    private final BaseSolver solver;


    public void printPerProjectStats(Map<SolverProject, List<SolverResource>> result) {
        System.out.println("Per-Project Stats:");
        for (Map.Entry<SolverProject, List<SolverResource>> entry : result.entrySet()) {
            SolverProject project = entry.getKey();
            List<SolverResource> assigned = entry.getValue();
            double completion = solver.calculateProjectCompletion(project, assigned);

            // Count total capacity assigned per resource ID
            Map<String, Integer> assignedCapacity = assigned.stream()
                    .collect(Collectors.groupingBy(
                            SolverResource::getId,
                            Collectors.summingInt(SolverResource::getAvailableCapacity)
                    ));

            // Calculate missing resources to complete project's requirements
            Map<String, Integer> missingResources = new HashMap<>();
            Map<String, Integer> requirements = project.getRequirements();
            for (Map.Entry<String, Integer> reqEntry : requirements.entrySet()) {
                String resourceId = reqEntry.getKey();
                int required = reqEntry.getValue();
                int assignedAmount = assignedCapacity.getOrDefault(resourceId, 0);
                if (assignedAmount < required) {
                    missingResources.put(resourceId, required - assignedAmount);
                }
            }

            System.out.printf("Project %s (Priority: %s): Completion = %.2f%%%n",
                    project.getName(), project.getPriority(), completion);
            System.out.println("   Assigned Resources (by capacity):");
            assignedCapacity.forEach((res, capacity) ->
                    System.out.printf("      %s: %d units%n", res, capacity));

            System.out.println("   Required Resources:");
            requirements.forEach((res, required) ->
                    System.out.printf("      %s: %d units%n", res, required));

            if (!missingResources.isEmpty()) {
                System.out.println("   Missing Resources:");
                missingResources.forEach((res, missing) ->
                        System.out.printf("      %s: %d units%n", res, missing));
            } else {
                System.out.println("   All resource requirements met.");
            }
        }
    }

    public void printGlobalStats(Map<SolverProject, List<SolverResource>> result) {
        // Calculate total available capacity per resource ID
        Map<String, Integer> totalCapacityById = resources.stream()
                .collect(Collectors.groupingBy(
                        SolverResource::getId,
                        Collectors.summingInt(SolverResource::getAvailableCapacity)
                ));

        // Calculate total used capacity per resource ID
        Map<String, Integer> usedCapacityById = result.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(
                        SolverResource::getId,
                        Collectors.summingInt(SolverResource::getAvailableCapacity)
                ));

        int totalAvailableCapacity = totalCapacityById.values().stream().mapToInt(Integer::intValue).sum();
        int totalUsedCapacity = usedCapacityById.values().stream().mapToInt(Integer::intValue).sum();
        double utilizationRate = totalAvailableCapacity > 0 ?
                (double) totalUsedCapacity / totalAvailableCapacity * 100 : 0;

        System.out.println("\nGlobal Stats:");
        System.out.printf("Total Resource Capacity Available: %d units%n", totalAvailableCapacity);
        System.out.printf("Total Resource Capacity Used: %d units%n", totalUsedCapacity);
        System.out.printf("Resource Utilization Rate: %.2f%%%n", utilizationRate);
        System.out.printf("Unused Resource Capacity: %d units%n", totalAvailableCapacity - totalUsedCapacity);
    }

    public void printGlobalResourceAssignmentBreakdown(Map<SolverProject, List<SolverResource>> result) {
        // Group by resource ID and sum capacities
        Map<String, Integer> usedCapacityById = result.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(
                        SolverResource::getId,
                        Collectors.summingInt(SolverResource::getAvailableCapacity)
                ));

        System.out.println("\nGlobal Resource Assignment Breakdown:");
        for (String resourceId : usedCapacityById.keySet()) {
            int totalCapacity = resources.stream()
                    .filter(r -> r.getId().equals(resourceId))
                    .mapToInt(SolverResource::getAvailableCapacity)
                    .sum();
            int usedCapacity = usedCapacityById.get(resourceId);
            double utilizationRate = (double) usedCapacity / totalCapacity * 100;

            System.out.printf("Resource %s: Used %d/%d units (%.2f%% utilization)%n",
                    resourceId, usedCapacity, totalCapacity, utilizationRate);
        }
    }

    public void printPerResourceStats(Map<SolverProject, List<SolverResource>> result) {
        // Group by resource ID and sum capacities
        Map<String, Integer> usedCapacityById = result.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(
                        SolverResource::getId,
                        Collectors.summingInt(SolverResource::getAvailableCapacity)
                ));

        System.out.println("\nPer Resource Stats:");
        Map<String, Integer> totalCapacityById = resources.stream()
                .collect(Collectors.groupingBy(
                        SolverResource::getId,
                        Collectors.summingInt(SolverResource::getAvailableCapacity)
                ));

        totalCapacityById.forEach((resourceId, totalCapacity) -> {
            int usedCapacity = usedCapacityById.getOrDefault(resourceId, 0);
            int cost = resources.stream()
                    .filter(r -> r.getId().equals(resourceId))
                    .findFirst()
                    .map(SolverResource::getCost)
                    .orElse(0);

            System.out.printf("Resource %s: Total Capacity = %d, Used = %d, Available = %d, Cost = %d%n",
                    resourceId,
                    totalCapacity,
                    usedCapacity,
                    totalCapacity - usedCapacity,
                    cost);
        });
    }

    public void exportAllocationStatsToCsv(Map<SolverProject, List<SolverResource>> result, String filePath) {
        // Extract distinct resource IDs from our resources list.
        // Using LinkedHashSet to maintain insertion order.
        Set<String> resourceIds = resources.stream()
                .map(SolverResource::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Extract projects from the result map and sort them (by name here, adjust as needed).
        List<SolverProject> projectList = result.keySet().stream()
                .sorted(Comparator.comparing(SolverProject::getName))
                .collect(Collectors.toList());

        // Prepare a mapping: for each project, map resource IDs to their assignment counts.
        Map<SolverProject, Map<String, Integer>> projectResourceCount = new HashMap<>();
        for (SolverProject project : projectList) {
            List<SolverResource> assigned = result.get(project);
            Map<String, Integer> countMap = new HashMap<>();
            for (SolverResource resource : assigned) {
                countMap.put(resource.getId(), countMap.getOrDefault(resource.getId(), 0) + 1);
            }
            projectResourceCount.put(project, countMap);
        }

        List<String> lines = new ArrayList<>();
        // Build header: "Resource", then one column per project (using project name), then "Row Sum"
        StringBuilder header = new StringBuilder("Resource");
        for (SolverProject project : projectList) {
            header.append(",").append(project.getName());
        }
        header.append(",Row Sum");
        lines.add(header.toString());

        // Prepare a map for column sums keyed by project name.
        Map<String, Integer> columnSums = new HashMap<>();

        // For each resource, build a row with counts per project.
        for (String resourceId : resourceIds) {
            StringBuilder row = new StringBuilder(resourceId);
            int rowSum = 0;
            for (SolverProject project : projectList) {
                int count = projectResourceCount.getOrDefault(project, Collections.emptyMap())
                        .getOrDefault(resourceId, 0);
                rowSum += count;
                columnSums.put(project.getName(), columnSums.getOrDefault(project.getName(), 0) + count);
                row.append(",").append(count);
            }
            row.append(",").append(rowSum);
            lines.add(row.toString());
        }

        // Build the final row for column sums.
        StringBuilder sumRow = new StringBuilder("Column Sum");
        int totalSum = 0;
        for (SolverProject project : projectList) {
            int colSum = columnSums.getOrDefault(project.getName(), 0);
            totalSum += colSum;
            sumRow.append(",").append(colSum);
        }
        sumRow.append(",").append(totalSum);
        lines.add(sumRow.toString());

        // Write the CSV file to the specified filePath.
        try {
            Path path = Paths.get(filePath);
            Files.write(path, lines);
            System.out.println("CSV file written to " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
    }

    public void printAllStats(Map<SolverProject, List<SolverResource>> result) {
        System.out.println("\n=== RESOURCE ALLOCATION STATS SUMMARY ===\n");
        printPerProjectStats(result);
        printGlobalStats(result);
        printGlobalResourceAssignmentBreakdown(result);
        printPerResourceStats(result);
    }
}
