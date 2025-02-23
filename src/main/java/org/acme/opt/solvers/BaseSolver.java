package org.acme.opt.solvers;

import org.acme.opt.models.SolverProject;
import org.acme.opt.models.SolverResource;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface BaseSolver {
    Map<SolverProject, List<SolverResource>> solve();

    default double calculateProjectCompletion(SolverProject solverProject, List<SolverResource> assignedSolverResources) {
        Map<String, Integer> requirements = solverProject.getRequirements();
        Map<String, Double> fulfilled = new HashMap<>();

//        for each project requirement, check if it is fulfilled by the assigned resources
        var assignedMap = assignedSolverResources.stream()
                .collect(Collectors.groupingBy(SolverResource::getId, Collectors.summingInt(SolverResource::getAvailableCapacity)));

        for (Map.Entry<String, Integer> requirement : requirements.entrySet()) {
            String resourceName = requirement.getKey();

            if(!assignedMap.containsKey(resourceName)){
                fulfilled.put(resourceName, 0.0);
            }else{
                double fulfilled_percentage = ((double) assignedMap.get(resourceName) / requirement.getValue()) * 100;
                fulfilled.put(resourceName, fulfilled_percentage);
            }

        }

        // Calculate completion percentage
        double totalRequirements = 0;
        double totalWeight = 0;
        for (Map.Entry<String, Double> completion_entry : fulfilled.entrySet()) {
            totalRequirements += completion_entry.getValue() * requirements.get(completion_entry.getKey());
            totalWeight += requirements.get(completion_entry.getKey());
        }

        System.out.println("Weighted Percentage: " + totalRequirements / totalWeight);

        return Math.round(totalRequirements / totalWeight);


//        for (Map.Entry<String, Integer> requirement : requirements.entrySet()) {
//            String resourceName = requirement.getKey();
//            int required = requirement.getValue();
////            int fulfilled_amount = fulfilled.getOrDefault(resourceName, 0);
//
//            totalRequirements += required;
//            totalFulfilled += Math.min(0.0, required);
//        }
//
//        double result = totalRequirements > 0 ? (totalFulfilled / totalRequirements) * 100 : 100.0;
//        return new BigDecimal(result, new MathContext(3, RoundingMode.HALF_UP)).doubleValue();

//        return totalRequirements > 0 ? (totalFulfilled / totalRequirements) * 100 : 100.0;
    }
}
