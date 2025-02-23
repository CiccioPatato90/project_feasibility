package org.acme.opt.solvers;

import org.acme.opt.models.SolverProject;
import org.acme.opt.models.SolverResource;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface BaseSolver {
    Map<SolverProject, List<SolverResource>> solve();

    default double calculateProjectCompletion(SolverProject solverProject, List<SolverResource> assignedSolverResources) {
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
