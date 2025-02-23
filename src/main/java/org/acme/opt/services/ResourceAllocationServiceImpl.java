package org.acme.opt.services;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import org.acme.opt.mappers.AllocationResponseMapper;
import org.acme.opt.models.SolverProject;
import org.acme.opt.models.SolverResource;
import org.acme.opt.models.SolverStrategy;
import org.acme.opt.solvers.GreedyAssignmentSolver;
import org.acme.opt.solvers.MaximizeResourceUsage;
import org.acme.opt.stats.ResourceAllocationStats;
import resourceallocation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@GrpcService
public class ResourceAllocationServiceImpl implements ResourceAllocationService {

    @Override
    public Uni<AllocationResponse> allocateResourcesLinearProgramming(AllocationRequest request) {
        List<SolverResource> resources = request.getResourcesList().stream()
                .map(r -> new SolverResource(r.getId(), r.getName(), r.getCapacity(), (int) r.getCost()))
                .toList();
        List<SolverProject> projects = request.getProjectsList().stream()
                .map(p -> new SolverProject("", p.getId(), p.getName(), p.getRequirementsMap(), p.getPriority()))
                .toList();
        // Call the algorithm.

        MaximizeResourceUsage solver = new MaximizeResourceUsage(resources, projects);
        Map<SolverProject, List<SolverResource>> result = solver.solve();

        var stats = new ResourceAllocationStats(resources, projects, solver);

//        stats.exportAllocationStatsToCsv(result, "linear_programming.csv");
//        stats.printAllStats(result);

        var mapper = new AllocationResponseMapper();
        var res_metadata = mapper.buildAllocationResponseMetadata(result, resources, projects, solver);
        return Uni.createFrom().item(res_metadata);
    }

    @Override
    public Uni<AllocationResponse> allocateResourcesGreedy(AllocationRequest request) {
        List<SolverResource> resources = request.getResourcesList().stream()
                .map(r -> new SolverResource(r.getId(), r.getName(), r.getCapacity(), (int) r.getCost()))
                .toList();
        List<SolverProject> projects = request.getProjectsList().stream()
                .map(p -> new SolverProject("", p.getId(), p.getName(), p.getRequirementsMap(), p.getPriority()))
                .toList();
        SolverStrategy strategy = SolverStrategy.fromProto(request.getStrategy());

        GreedyAssignmentSolver solver = new GreedyAssignmentSolver(resources, projects, strategy);

        Map<SolverProject, List<SolverResource>> result = solver.solve();

        var stats = new ResourceAllocationStats(resources, projects, solver);

//        stats.printAllStats(result);
//        stats.exportAllocationStatsToCsv(result, "greedy-"+strategy.strategy().name() +"-"+strategy.order().name()+".csv");

        var mapper = new AllocationResponseMapper();
        var res_metadata = mapper.buildAllocationResponseMetadata(result, resources, projects, solver);

        return Uni.createFrom().item(res_metadata);
    }
}
