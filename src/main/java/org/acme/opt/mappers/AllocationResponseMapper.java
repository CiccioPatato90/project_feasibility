package org.acme.opt.mappers;

import org.acme.opt.models.SolverProject;
import org.acme.opt.models.SolverResource;
import org.acme.opt.solvers.MaximizeResourceUsage;
import resourceallocation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AllocationResponseMapper {

    public AllocationResponse buildAllocationResponseNoMetadata(Map<SolverProject, List<SolverResource>> result) {
        AllocationResponse.Builder allocationResponseBuilder = AllocationResponse.newBuilder();

        // Generate a unique allocation ID.
        allocationResponseBuilder.setAllocationId(UUID.randomUUID().toString());

        // For each project, build a ProjectAllocation.
        result.forEach((project, resources) -> {
            // Group resources by resourceId and count their occurrences.
            Map<String, Long> groupedResources = resources.stream()
                    .collect(Collectors.groupingBy(SolverResource::getId, Collectors.counting()));

            ProjectAllocation.Builder projectAllocationBuilder = ProjectAllocation.newBuilder();
            // Use project name or project ID as needed.
            projectAllocationBuilder.setProjectId(project.getId());

            // Build ResourceAllocation messages.
            groupedResources.forEach((resourceId, count) -> {
                ResourceAllocation resourceAllocation = ResourceAllocation.newBuilder()
                        .setResourceId(resourceId)
                        .setAllocatedAmount(count.intValue())
                        .build();
                projectAllocationBuilder.addResourceAllocations(resourceAllocation);
            });

            // Add the project allocation to the response map.
            allocationResponseBuilder.putProjectAllocations(project.getCompletionRate(), projectAllocationBuilder.build());
        });

        // Set the overall allocation status.
        allocationResponseBuilder.setStatus(AllocationStatus.COMPLETED);

        return allocationResponseBuilder.build();
    }

    public AllocationResponse buildAllocationResponseMetadata(Map<SolverProject, List<SolverResource>> result, List<SolverResource> resources, List<SolverProject> projects, MaximizeResourceUsage solver) {
        // First build the base response without metadata
        AllocationResponse.Builder responseBuilder = AllocationResponse.newBuilder(buildAllocationResponseNoMetadata(result));

        // Build global stats
        AllocationStats.Builder globalStatsBuilder = AllocationStats.newBuilder();

        // Calculate global resource metrics
        int totalAvailable = resources.stream().mapToInt(SolverResource::getAvailableCapacity).sum();
        int totalUsed = result.values().stream().mapToInt(List::size).sum();
        double avgUsed = (double) totalUsed / projects.size();

        globalStatsBuilder.setTotalResourcesAvailable(totalAvailable)
                .setTotalResourcesUsed(totalUsed)
                .setAverageResourcesPerProject(avgUsed)
                .setUnusedResources(totalAvailable - totalUsed);

        // Calculate most/least assigned resources
        Map<String, Integer> globalAssignment = result.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(
                        SolverResource::getName,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        globalAssignment.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> {
                    AllocationStats.ResourceUsage mostAssigned = AllocationStats.ResourceUsage.newBuilder()
                            .setResourceId(entry.getKey())
                            .setUsageCount(entry.getValue())
                            .build();
                    globalStatsBuilder.setMostAssignedResource(mostAssigned);
                });

        globalAssignment.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .ifPresent(entry -> {
                    AllocationStats.ResourceUsage leastAssigned = AllocationStats.ResourceUsage.newBuilder()
                            .setResourceId(entry.getKey())
                            .setUsageCount(entry.getValue())
                            .build();
                    globalStatsBuilder.setLeastAssignedResource(leastAssigned);
                });

        // Add global stats to response
        responseBuilder.setGlobalStats(globalStatsBuilder.build());

        // Build per-project stats
        for (Map.Entry<SolverProject, List<SolverResource>> entry : result.entrySet()) {
            SolverProject project = entry.getKey();
            List<SolverResource> assigned = entry.getValue();

            ProjectStats.Builder projectStatsBuilder = ProjectStats.newBuilder();

            // Calculate completion percentage
            double completion = solver.calculateProjectCompletion(project, assigned);
            projectStatsBuilder.setCompletionPercentage(completion);

            // Set assigned resource count
            projectStatsBuilder.setAssignedResourceCount(assigned.size());

            // Calculate missing resources
            Map<String, Integer> assignmentCount = assigned.stream()
                    .collect(Collectors.groupingBy(
                            SolverResource::getId,
                            Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                    ));

            // Add missing resources to project stats
            Map<String, Integer> requirements = project.getRequirements();
            requirements.forEach((resourceId, required) -> {
                int assignedCount = assignmentCount.getOrDefault(resourceId, 0);
                if (assignedCount < required) {
                    projectStatsBuilder.putMissingResources(resourceId, required - assignedCount);
                }
            });

            // Add project stats to response
            responseBuilder.putProjectStats(project.getId(), projectStatsBuilder.build());
        }

        return responseBuilder.build();
    }
}
