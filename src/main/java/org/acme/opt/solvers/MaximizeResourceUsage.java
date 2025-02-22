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

@AllArgsConstructor
public class MaximizeResourceUsage {
    private final List<SolverResource> solverResources;
    private final List<SolverProject> solverProjects;

    public Map<SolverProject, List<SolverResource>> solve() {
        // Set a fixed seed for reproducibility
        Random random = new Random(42L);

        Loader.loadNativeLibraries();
        MPSolver solver = MPSolver.createSolver("GLOP");

        // Create decision variables x[i][j] representing the quantity of resource i assigned to project j
        Map<SolverResource, Map<SolverProject, MPVariable>> x = new HashMap<>();
        for (SolverResource solverResource : solverResources) {
            Map<SolverProject, MPVariable> projectVars = new HashMap<>();
            for (SolverProject solverProject : solverProjects) {
                String varName = String.format("x_%s_%s", solverResource.getId(), solverProject.getId());
                // Upper bound is the minimum between resource capacity and project requirement
                int upperBound = Math.min(
                        solverResource.getAvailableCapacity(),
                        solverProject.getRequirements().getOrDefault(solverResource.getId(), 0)
                );
                projectVars.put(solverProject, solver.makeIntVar(0, upperBound, varName));
            }
            x.put(solverResource, projectVars);
        }

        // Objective: Minimize cost while maximizing resource utilization
        MPObjective objective = solver.objective();

        // Cost minimization component
        for (SolverResource solverResource : solverResources) {
            for (SolverProject solverProject : solverProjects) {
                // Assuming Resource class has a getCost() method that returns cost per unit
                objective.setCoefficient(x.get(solverResource).get(solverProject), solverResource.getCost());
            }
        }

        // Resource utilization component (negative coefficient to maximize)
        for (SolverResource solverResource : solverResources) {
            for (SolverProject solverProject : solverProjects) {
                // Add a small negative weight to encourage resource utilization
                objective.setCoefficient(x.get(solverResource).get(solverProject), -0.1);
            }
        }

        objective.setMinimization();

        // Constraint 1: Don't exceed resource capacity
        for (SolverResource solverResource : solverResources) {
            MPConstraint capacityConstraint = solver.makeConstraint(
                    0,
                    solverResource.getAvailableCapacity(),
                    "capacity_" + solverResource.getId()
            );
            for (SolverProject solverProject : solverProjects) {
                capacityConstraint.setCoefficient(x.get(solverResource).get(solverProject), 1);
            }
        }

        // Constraint 2: Don't exceed project requirements
        for (SolverProject solverProject : solverProjects) {
            for (SolverResource solverResource : solverResources) {
                int requirement = solverProject.getRequirements().getOrDefault(solverResource.getId(), 0);
                if (requirement > 0) {
                    MPConstraint requirementConstraint = solver.makeConstraint(
                            0,
                            requirement,
                            String.format("requirement_%s_%s", solverProject.getId(), solverResource.getId())
                    );
                    requirementConstraint.setCoefficient(x.get(solverResource).get(solverProject), 1);
                }
            }
        }

        // Solve the problem
        MPSolver.ResultStatus status = solver.solve();

        // Process results
        Map<SolverProject, List<SolverResource>> assignments = new HashMap<>();
        if (status == MPSolver.ResultStatus.OPTIMAL || status == MPSolver.ResultStatus.FEASIBLE) {
            for (SolverProject solverProject : solverProjects) {
                List<SolverResource> assignedSolverResources = new ArrayList<>();
                for (SolverResource solverResource : solverResources) {
                    double quantity = x.get(solverResource).get(solverProject).solutionValue();
                    // If any quantity of this resource is assigned to this project
                    if (quantity > 0) {
                        // Create new resource instances with the assigned quantity
                        for (int i = 0; i < (int)quantity; i++) {
                            assignedSolverResources.add(new SolverResource(
                                    solverResource.getId(),
                                    solverResource.getName(),
                                    1,  // One unit per instance
                                    solverResource.getCost()
                            ));
                        }
                    }
                }
                if (!assignedSolverResources.isEmpty()) {
                    double completion = calculateProjectCompletion(solverProject, assignedSolverResources);
                    solverProject.setCompletionRate(String.valueOf(completion));
                    assignments.put(solverProject, assignedSolverResources);
                }
            }
        }

        return assignments;
    }

    // Helper method to calculate project completion percentage
    public double calculateProjectCompletion(SolverProject solverProject, List<SolverResource> assignedSolverResources) {
        Map<String, Integer> requirements = solverProject.getRequirements();
        Map<String, Integer> fulfilled = new HashMap<>();

        // Count assigned resources
        for (SolverResource solverResource : assignedSolverResources) {
            fulfilled.merge(solverResource.getId(), 1, Integer::sum);
        }

        // Calculate completion percentage
        double totalRequirements = 0;
        double totalFulfilled = 0;

        for (Map.Entry<String, Integer> requirement : requirements.entrySet()) {
            String resourceName = requirement.getKey();
            int required = requirement.getValue();
            int fulfilled_amount = fulfilled.getOrDefault(resourceName, 0);

            totalRequirements += required;
            totalFulfilled += Math.min(fulfilled_amount, required);
        }

        double result = totalRequirements > 0 ? (totalFulfilled / totalRequirements) * 100 : 100.0;
        return new BigDecimal(result, new MathContext(3, RoundingMode.HALF_UP)).doubleValue();

//        return totalRequirements > 0 ? (totalFulfilled / totalRequirements) * 100 : 100.0;
    }
}
