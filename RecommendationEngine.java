package com.goalwise.ai;

import com.goalwise.model.Goal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Turns raw analysis (from ExpensePredictor / GoalAdvisor) into plain-English,
 * actionable recommendations, e.g.
 * "Reduce shopping expenses by Rs.700 and entertainment by Rs.300 to hit your goal."
 */
public class RecommendationEngine {

    /**
     * Builds a list of human-readable recommendations for a specific goal,
     * based on the user's top spending categories and the shortfall (if any)
     * between required monthly saving and current surplus.
     */
    public static List<String> recommendForGoal(Goal goal) {
        List<String> tips = new ArrayList<>();
        GoalAdvisor.Assessment assessment = GoalAdvisor.assess(goal);

        if (assessment.verdict == GoalAdvisor.Verdict.ALREADY_ACHIEVED) {
            tips.add("Goal achieved! Consider setting a new goal to keep building momentum.");
            return tips;
        }

        double shortfall = assessment.requiredMonthlySaving - assessment.availableMonthlySurplus;
        Map<String, Double> categoryTotals = com.goalwise.service.ExpenseService
                .getCategoryTotalsAllTime(goal.getUserId());

        if (shortfall <= 0 || categoryTotals.isEmpty()) {
            tips.add(String.format(
                    "Keep saving about Rs.%.0f per month and you'll reach \"%s\" by %s.",
                    assessment.requiredMonthlySaving, goal.getGoalName(), goal.getTargetDate()));
            return tips;
        }

        // Rank categories by spend, largest first, and suggest trims that
        // together close the shortfall - largest categories first.
        List<Map.Entry<String, Double>> ranked = categoryTotals.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        double remainingToClose = shortfall;
        List<String> cuts = new ArrayList<>();

        for (Map.Entry<String, Double> entry : ranked) {
            if (remainingToClose <= 0) {
                break;
            }
            // Suggest trimming up to 25% of what's spent in that category,
            // capped at whatever is still needed to close the gap.
            double maxTrim = entry.getValue() * 0.25;
            double trim = Math.min(maxTrim, remainingToClose);
            if (trim >= 50) { // ignore tiny, not-worth-mentioning trims
                cuts.add(String.format("%s by Rs.%.0f", entry.getKey(), trim));
                remainingToClose -= trim;
            }
        }

        if (!cuts.isEmpty()) {
            tips.add("Reduce " + String.join(" and ", cuts)
                    + " per month to achieve your \"" + goal.getGoalName() + "\" goal on time.");
        }

        if (remainingToClose > 0) {
            tips.add(String.format(
                    "Even with those cuts you're about Rs.%.0f/month short - "
                            + "consider adding income or extending the target date.",
                    remainingToClose));
        }

        return tips;
    }

    /**
     * General-purpose tips based on overspending and budget alerts, not
     * tied to a specific goal.
     */
    public static List<String> generalTips(int userId) {
        List<String> tips = new ArrayList<>();

        double overspend = ExpensePredictor.getOverspendPercent(userId);
        if (overspend > 15) {
            tips.add(String.format(
                    "You're spending about %.0f%% more than your recent average this month - "
                            + "review your biggest categories before month end.", overspend));
        }

        List<com.goalwise.model.Budget> alerts =
                com.goalwise.service.BudgetService.getOverBudgetAlerts(userId);
        for (com.goalwise.model.Budget b : alerts) {
            tips.add(String.format("You're near or over your %s budget for this month (limit Rs.%.0f).",
                    b.getCategory(), b.getLimitAmount()));
        }

        if (tips.isEmpty()) {
            tips.add("Your spending looks steady - no red flags this month.");
        }

        return tips;
    }
}
