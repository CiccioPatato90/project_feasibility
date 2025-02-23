package org.acme.opt.solvers;

import lombok.AllArgsConstructor;
import org.acme.opt.models.SolverProject;
import org.acme.opt.models.SolverResource;
import org.acme.opt.models.SolverStrategy;
import org.acme.opt.models.enums.GreedyOrder;
import org.acme.opt.models.enums.GreedyStrategy;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
public class GreedyAssignmentSolver implements BaseSolver{

    private final List<SolverResource> resources;
    private final List<SolverProject> projects;
    private final SolverStrategy strategy;

    @Override
    public Map<SolverProject, List<SolverResource>> solve() {
        // Determine the comparator based on the chosen criteria
        Comparator<SolverProject> comparator = getProjectComparator(strategy.strategy());

        // Apply the order if it's known
        if (strategy.order() == GreedyOrder.LARGEST_FIRST) {
            comparator = comparator.reversed();
        } // SMALLEST_FIRST is default ascending order

        // Sort projects using the comparator
        List<SolverProject> sortedProjects = projects.stream()
                .sorted(comparator)
                .toList();

        // Initialize result map and available resources
        Map<SolverProject, List<SolverResource>> allocation = new HashMap<>();

        Map<String, Integer> availableResources = new HashMap<>(resources.stream().collect(Collectors.groupingBy(SolverResource::getId, Collectors.summingInt(SolverResource::getAvailableCapacity))));
//        List<SolverResource> availableResources = new ArrayList<>(resources);

        // Try to allocate resources to each project in order
        for (SolverProject project : sortedProjects) {
            List<SolverResource> assignedResources = findResourcesForProject(project, availableResources);
            if (!assignedResources.isEmpty()) {
                allocation.put(project, assignedResources);
            }
        }

        for (Map.Entry<SolverProject, List<SolverResource>> entry : allocation.entrySet()) {
            SolverProject project = entry.getKey();
            List<SolverResource> assigned = entry.getValue();
            double completion = calculateProjectCompletion(project, assigned);
            project.setCompletionRate(String.valueOf(completion));
            allocation.put(project, assigned);
        }

        return allocation;
    }

    private List<SolverResource> findResourcesForProject(SolverProject project, Map<String, Integer> availableResources) {
        List<SolverResource> assignedResources = new ArrayList<>();
        Map<String, Integer> projectRequirements = new HashMap<>(project.getRequirements());


        for (Map.Entry<String, Integer> requirement : projectRequirements.entrySet()) {
            String neededResourceId = requirement.getKey();

            // Find matching resources
            var availableQuantity = availableResources.get(neededResourceId);

            if(availableQuantity != null){
                if (requirement.getValue() > 0 && availableQuantity >= requirement.getValue()) {
                    assignedResources.add(new SolverResource(neededResourceId, "", requirement.getValue(), 0));
                    availableResources.put(neededResourceId, availableQuantity - requirement.getValue());
                }else if(availableQuantity <= requirement.getValue()){
                    assignedResources.add(new SolverResource(neededResourceId, "", availableQuantity, 0));
                    availableResources.remove(neededResourceId);
                }
            }

        }

        return assignedResources;
    }

    private Comparator<SolverProject> getProjectComparator(GreedyStrategy strategy) {
        return switch (strategy) {
            case PROJECT_SIZE -> Comparator.comparingInt(p ->
                    p.getRequirements().values().stream().mapToInt(Integer::intValue).sum());
            case ASSOCIATION_ACTIVITY -> Comparator.comparing(SolverProject::getPriority,
                    Comparator.nullsLast(Comparator.naturalOrder()));
//            case CREATION_DATE -> Comparator.comparing(SolverProject::getCreationDate,
//                    Comparator.nullsLast(Comparator.naturalOrder()));
            case CREATION_DATE -> (p1, p2) -> 0;
            case UNKNOWN -> (p1, p2) -> 0; // No sorting for unknown strategy
        };
    }
}
