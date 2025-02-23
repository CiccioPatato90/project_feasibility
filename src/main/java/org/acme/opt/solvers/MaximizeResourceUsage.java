package org.acme.opt.solvers;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import lombok.AllArgsConstructor;
import org.acme.opt.models.SolverProject;
import org.acme.opt.models.SolverResource;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
public class MaximizeResourceUsage implements BaseSolver {
    private final List<SolverResource> solverResources;
    private final List<SolverProject> solverProjects;


    @Override
    public Map<SolverProject, List<SolverResource>> solve() {
        Loader.loadNativeLibraries();
        MPSolver solver = MPSolver.createSolver("GLOP");

        // First, aggregate resources by ID to match greedy approach
        Map<String, Integer> totalResourceCapacity = solverResources.stream()
                .collect(Collectors.groupingBy(
                        SolverResource::getId,
                        Collectors.summingInt(SolverResource::getAvailableCapacity)
                ));

        // Create decision variables x[resourceId][projectId] representing the quantity assigned
        Map<String, Map<SolverProject, MPVariable>> x = new HashMap<>();

        for (String resourceId : totalResourceCapacity.keySet()) {
            Map<SolverProject, MPVariable> projectVars = new HashMap<>();
            for (SolverProject project : solverProjects) {
                String varName = String.format("x_%s_%s", resourceId, project.getId());
                int requirement = project.getRequirements().getOrDefault(resourceId, 0);
                // Upper bound is the minimum between total resource capacity and project requirement
                int upperBound = Math.min(totalResourceCapacity.get(resourceId), requirement);
                projectVars.put(project, solver.makeIntVar(0, upperBound, varName));
            }
            x.put(resourceId, projectVars);
        }

        // Objective: Maximize resource utilization
        MPObjective objective = solver.objective();
        for (String resourceId : totalResourceCapacity.keySet()) {
            for (SolverProject project : solverProjects) {
                // Add priority weight if project has priority
                double weight = 1.0;
                if (project.getPriority() >= 0) {
                    weight += project.getPriority();
                }
                objective.setCoefficient(x.get(resourceId).get(project), weight);
            }
        }
        objective.setMaximization();

        // Constraint 1: Don't exceed resource capacity
        for (String resourceId : totalResourceCapacity.keySet()) {
            MPConstraint capacityConstraint = solver.makeConstraint(
                    0,
                    totalResourceCapacity.get(resourceId),
                    "capacity_" + resourceId
            );
            for (SolverProject project : solverProjects) {
                capacityConstraint.setCoefficient(x.get(resourceId).get(project), 1);
            }
        }

        // Solve the problem
        MPSolver.ResultStatus status = solver.solve();

        // Process results
        Map<SolverProject, List<SolverResource>> assignments = new HashMap<>();
        if (status == MPSolver.ResultStatus.OPTIMAL || status == MPSolver.ResultStatus.FEASIBLE) {
            for (SolverProject project : solverProjects) {
                List<SolverResource> assignedResources = new ArrayList<>();

                for (String resourceId : totalResourceCapacity.keySet()) {
                    double quantity = x.get(resourceId).get(project).solutionValue();
                    if (quantity > 0) {
                        // Create a single resource instance with the total assigned quantity
                        SolverResource originalResource = solverResources.stream()
                                .filter(r -> r.getId().equals(resourceId))
                                .findFirst()
                                .orElseThrow();

                        assignedResources.add(new SolverResource(
                                resourceId,
                                originalResource.getName(),
                                (int)quantity,
                                originalResource.getCost()
                        ));
                    }
                }

                if (!assignedResources.isEmpty()) {
                    double completion = calculateProjectCompletion(project, assignedResources);
                    project.setCompletionRate(String.valueOf(completion));
                    assignments.put(project, assignedResources);
                    System.out.printf("Project %s: Completion = %.2f%%, Resources Assigned = %d%n",
                            project.getName(), completion, assignedResources.size());
                }
            }
        } else {
            System.out.println("No solution found");
        }

        return assignments;
    }

//    @Override
//    public Map<SolverProject, List<SolverResource>> solve() {
//        Loader.loadNativeLibraries();
//        MPSolver solver = MPSolver.createSolver("GLOP");
//
//        // Create decision variables x[i][j] representing the quantity of resource i assigned to project j
//        Map<SolverResource, Map<SolverProject, MPVariable>> x = new HashMap<>();
//        for (SolverResource solverResource : solverResources) {
//            Map<SolverProject, MPVariable> projectVars = new HashMap<>();
//            for (SolverProject solverProject : solverProjects) {
//                String varName = String.format("x_%s_%s", solverResource.getId(), solverProject.getId());
//                // Upper bound is the minimum between resource capacity and project requirement
//                int upperBound = Math.min(
//                        solverResource.getAvailableCapacity(),
//                        solverProject.getRequirements().getOrDefault(solverResource.getId(), 0)
//                );
//                projectVars.put(solverProject, solver.makeIntVar(0, upperBound, varName));
//            }
//            x.put(solverResource, projectVars);
//        }
//
//        // Objective: Minimize cost while maximizing resource utilization
//        MPObjective objective = solver.objective();
//
//        // Cost minimization component
//        for (SolverResource solverResource : solverResources) {
//            for (SolverProject solverProject : solverProjects) {
//                // Assuming Resource class has a getCost() method that returns cost per unit
//                objective.setCoefficient(x.get(solverResource).get(solverProject), solverResource.getCost());
//            }
//        }
//
//        // Resource utilization component (negative coefficient to maximize)
//        for (SolverResource solverResource : solverResources) {
//            for (SolverProject solverProject : solverProjects) {
//                // Add a small negative weight to encourage resource utilization
//                objective.setCoefficient(x.get(solverResource).get(solverProject), -0.1);
//            }
//        }
//
//        objective.setMinimization();
//
//        // Constraint 1: Don't exceed resource capacity
//        for (SolverResource solverResource : solverResources) {
//            MPConstraint capacityConstraint = solver.makeConstraint(
//                    0,
//                    solverResource.getAvailableCapacity(),
//                    "capacity_" + solverResource.getId()
//            );
//            for (SolverProject solverProject : solverProjects) {
//                capacityConstraint.setCoefficient(x.get(solverResource).get(solverProject), 1);
//            }
//        }
//
//        // Constraint 2: Don't exceed project requirements
//        for (SolverProject solverProject : solverProjects) {
//            for (SolverResource solverResource : solverResources) {
//                int requirement = solverProject.getRequirements().getOrDefault(solverResource.getId(), 0);
//                if (requirement > 0) {
//                    MPConstraint requirementConstraint = solver.makeConstraint(
//                            0,
//                            requirement,
//                            String.format("requirement_%s_%s", solverProject.getId(), solverResource.getId())
//                    );
//                    requirementConstraint.setCoefficient(x.get(solverResource).get(solverProject), 1);
//                }
//            }
//        }
//
//        // Solve the problem
//        MPSolver.ResultStatus status = solver.solve();
//
//        // Process results
//        Map<SolverProject, List<SolverResource>> assignments = new HashMap<>();
//        if (status == MPSolver.ResultStatus.OPTIMAL || status == MPSolver.ResultStatus.FEASIBLE) {
//            for (SolverProject solverProject : solverProjects) {
//                List<SolverResource> assignedSolverResources = new ArrayList<>();
//                for (SolverResource solverResource : solverResources) {
//                    double quantity = x.get(solverResource).get(solverProject).solutionValue();
//                    // If any quantity of this resource is assigned to this project
//                    if (quantity > 0) {
//                        // Create new resource instances with the assigned quantity
//                        for (int i = 0; i < (int)quantity; i++) {
//                            assignedSolverResources.add(new SolverResource(
//                                    solverResource.getId(),
//                                    solverResource.getName(),
//                                    1,  // One unit per instance
//                                    solverResource.getCost()
//                            ));
//                        }
//                    }
//                }
//                if (!assignedSolverResources.isEmpty()) {
//                    double completion = calculateProjectCompletion(solverProject, assignedSolverResources);
//                    solverProject.setCompletionRate(String.valueOf(completion));
//                    assignments.put(solverProject, assignedSolverResources);
//                }
//            }
//        }
//
//        return assignments;
//    }

    // Helper method to calculate project completion percentage
}
