package org.acme.opt.services;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import org.acme.opt.mappers.AllocationResponseMapper;
import org.acme.opt.models.SolverProject;
import org.acme.opt.models.SolverResource;
import org.acme.opt.solvers.MaximizeResourceUsage;
import org.acme.opt.stats.ResourceAllocationStats;
import resourceallocation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@GrpcService
public class ResourceAllocationServiceImpl implements ResourceAllocationService {

    @Override
    public Uni<AllocationResponse> allocateResources(AllocationRequest request) {

        List<SolverResource> resources = request.getResourcesList().stream()
                .map(r -> new SolverResource(r.getId(), r.getName(), r.getCapacity(), (int) r.getCost()))
                .toList();
        List<SolverProject> projects = request.getProjectsList().stream()
                .map(p -> new SolverProject("", p.getId(), p.getName(), p.getRequirementsMap()))
                .toList();
        // Call the algorithm.
        MaximizeResourceUsage solver = new MaximizeResourceUsage(resources, projects);
        Map<SolverProject, List<SolverResource>> result = solver.solve();

        var stats = new ResourceAllocationStats(resources, projects, solver);

        stats.printAllStats(result);
//
//        // Build response.
//        SolveResponse.Builder responseBuilder = SolveResponse.newBuilder();
//        for (Map.Entry<Project, List<Resource>> entry : result.entrySet()) {
//            Assignment.Builder assignBuilder = Assignment.newBuilder()
//                    .setProjectName(entry.getKey().getName())
//                    .setCompletion(solver.calculateProjectCompletion(entry.getKey(), entry.getValue()));
//            for (Resource res : entry.getValue()) {
//                ResourceMessage resMsg = ResourceMessage.newBuilder()
//                        .setName(res.getName())
//                        .setAvailableCapacity(res.getAvailableCapacity())
//                        .setCost(res.getCost())
//                        .build();
//                assignBuilder.addAssignedResources(resMsg);
//            }
//            responseBuilder.addAssignments(assignBuilder.build());
//        }
//
//        responseObserver.onNext(responseBuilder.build());
//        responseObserver.onCompleted();


        var mapper = new AllocationResponseMapper();
//        var res = mapper.buildAllocationResponseNoMetadata(result);
        var res_metadata = mapper.buildAllocationResponseMetadata(result, resources, projects, solver);

        return Uni.createFrom().item(res_metadata);
    }
}
