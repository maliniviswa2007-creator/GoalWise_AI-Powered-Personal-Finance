package com.goalwise.ai;

import com.goalwise.model.Goal;
import com.goalwise.service.ExpenseService;
import com.goalwise.service.IncomeService;

import java.time.LocalDate;

/**
 * Assesses whether a savings goal is realistically achievable given the
 * user's current income and spending pattern.
 */
public class GoalAdvisor {

    public enum Verdict {
        ON_TRACK,
        TIGHT_BUT_POSSIBLE,
        NOT_ACHIEVABLE,
        ALREADY_ACHIEVED
    }

    public static class Assessment {
        public final Verdict verdict;
        public final double requiredMonthlySaving;
        public final double availableMonthlySurplus;
        public final String message;

        public Assessment(Verdict verdict, double requiredMonthlySaving,
                           double availableMonthlySurplus, String message) {
            this.verdict = verdict;
            this.requiredMonthlySaving = requiredMonthlySaving;
            this.availableMonthlySurplus = availableMonthlySurplus;
            this.message = message;
        }
    }

    /**
     * Evaluates whether the goal can be reached by its target date, comparing
     * the required monthly saving against the user's average monthly surplus
     * (income minus expenses).
     */
    public static Assessment assess(Goal goal) {
        if (goal.isAchieved()) {
            return new Assessment(Verdict.ALREADY_ACHIEVED, 0, 0,
                    "You've already reached this goal. Nice work!");
        }

        double requiredMonthly = goal.getRequiredMonthlySaving();
        double surplus = estimateMonthlySurplus(goal.getUserId());

        if (surplus <= 0) {
            return new Assessment(Verdict.NOT_ACHIEVABLE, requiredMonthly, surplus,
                    "Your expenses currently match or exceed your income, so there is no "
                            + "surplus to put toward this goal. Reduce spending first.");
        }

        if (requiredMonthly <= surplus * 0.7) {
            return new Assessment(Verdict.ON_TRACK, requiredMonthly, surplus,
                    "You're comfortably on track - your typical monthly surplus covers "
                            + "the required saving with room to spare.");
        } else if (requiredMonthly <= surplus) {
            return new Assessment(Verdict.TIGHT_BUT_POSSIBLE, requiredMonthly, surplus,
                    "This goal is achievable, but it will use most of your monthly surplus. "
                            + "Consider trimming a category or two to stay comfortable.");
        } else {
            double gap = requiredMonthly - surplus;
            return new Assessment(Verdict.NOT_ACHIEVABLE, requiredMonthly, surplus,
                    String.format("At your current pace you're short by about Rs.%.0f per month. "
                            + "Extend the deadline or cut expenses to close the gap.", gap));
        }
    }

    /**
     * Estimates the user's average monthly surplus using this month's
     * income so far minus average monthly expenses (from ExpensePredictor).
     */
    public static double estimateMonthlySurplus(int userId) {
        LocalDate now = LocalDate.now();
        double income = IncomeService.getTotalIncome(userId);
        long incomeEntries = IncomeService.getIncomeByUser(userId).size();

        double avgMonthlyIncome;
        if (incomeEntries == 0) {
            avgMonthlyIncome = 0;
        } else {
            // Approximate using total income recorded / distinct months of data (min 1).
            avgMonthlyIncome = IncomeService.getIncomeForMonth(userId, now.getMonthValue(), now.getYear());
            if (avgMonthlyIncome == 0) {
                avgMonthlyIncome = income; // fall back to whatever total is on record
            }
        }

        double avgMonthlyExpense = ExpensePredictor.predictNextMonthTotal(userId);
        return avgMonthlyIncome - avgMonthlyExpense;
    }
}
