package com.goalwise.ai;

import com.goalwise.model.Budget;
import com.goalwise.model.Goal;
import com.goalwise.service.BudgetService;
import com.goalwise.service.ExpenseService;
import com.goalwise.service.GoalService;
import com.goalwise.service.IncomeService;

import java.time.LocalDate;
import java.util.List;

/**
 * Computes a single 0-100 "financial health score" from four weighted
 * signals: savings ratio, budget adherence, goal progress and spending
 * stability. Purely rule-based - no external ML dependency required.
 */
public class FinancialHealthScore {

    public static class Result {
        public final int score;
        public final String rating;
        public final String summary;

        public Result(int score, String rating, String summary) {
            this.score = score;
            this.rating = rating;
            this.summary = summary;
        }
    }

    public static Result calculate(int userId) {
        double savingsScore = savingsRatioScore(userId);      // 0-40
        double budgetScore = budgetAdherenceScore(userId);    // 0-25
        double goalScore = goalProgressScore(userId);         // 0-20
        double stabilityScore = spendingStabilityScore(userId); // 0-15

        int total = (int) Math.round(savingsScore + budgetScore + goalScore + stabilityScore);
        total = Math.max(0, Math.min(100, total));

        String rating;
        if (total >= 80) {
            rating = "Excellent";
        } else if (total >= 60) {
            rating = "Good";
        } else if (total >= 40) {
            rating = "Fair";
        } else {
            rating = "Needs Attention";
        }

        String summary = String.format(
                "Savings %.0f/40, Budget adherence %.0f/25, Goal progress %.0f/20, Stability %.0f/15",
                savingsScore, budgetScore, goalScore, stabilityScore);

        return new Result(total, rating, summary);
    }

    /** Rewards a healthy income-to-expense ratio. */
    private static double savingsRatioScore(int userId) {
        LocalDate now = LocalDate.now();
        double income = IncomeService.getIncomeForMonth(userId, now.getMonthValue(), now.getYear());
        if (income <= 0) {
            income = IncomeService.getTotalIncome(userId);
        }
        double expense = ExpenseService.getTotalExpensesForMonth(userId, now.getMonthValue(), now.getYear());

        if (income <= 0) {
            return 10; // not enough data - neutral-low default
        }
        double savingsRatio = (income - expense) / income; // can be negative
        double normalized = Math.max(0, Math.min(1, (savingsRatio + 0.2) / 0.7));
        return normalized * 40;
    }

    /** Rewards staying under (not over) declared category budgets. */
    private static double budgetAdherenceScore(int userId) {
        LocalDate now = LocalDate.now();
        List<Budget> budgets = BudgetService.getBudgetsForMonth(userId, now.getMonthValue(), now.getYear());
        if (budgets.isEmpty()) {
            return 15; // neutral default when no budgets have been set yet
        }
        double totalUsage = 0;
        for (Budget b : budgets) {
            totalUsage += Math.min(BudgetService.getUsagePercent(b), 150); // cap outliers
        }
        double avgUsage = totalUsage / budgets.size();
        double normalized = Math.max(0, Math.min(1, (110 - avgUsage) / 110));
        return normalized * 25;
    }

    /** Rewards active progress toward savings goals. */
    private static double goalProgressScore(int userId) {
        List<Goal> goals = GoalService.getGoalsByUser(userId);
        if (goals.isEmpty()) {
            return 10; // neutral default
        }
        double avgProgress = goals.stream()
                .mapToDouble(Goal::getProgressPercent)
                .average()
                .orElse(0);
        return (avgProgress / 100.0) * 20;
    }

    /** Rewards consistent (non-spiky) month-to-month spending. */
    private static double spendingStabilityScore(int userId) {
        double overspend = ExpensePredictor.getOverspendPercent(userId);
        double normalized = Math.max(0, Math.min(1, (50 - overspend) / 50));
        return normalized * 15;
    }
}
