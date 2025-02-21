package org.acme.opt.mappers;

import org.acme.opt.models.SolverProject;
import org.acme.opt.models.SolverResource;
import resourceallocation.AllocationResponse;
import resourceallocation.AllocationStatus;
import resourceallocation.ProjectAllocation;
import resourceallocation.ResourceAllocation;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AllocationResponseMapper {

    public AllocationResponse buildAllocationResponse(Map<SolverProject, List<SolverResource>> result) {
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
}
