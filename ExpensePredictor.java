package com.goalwise.ai;

import com.goalwise.model.Expense;
import com.goalwise.service.ExpenseService;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Predicts future spending based on the user's historical expense data.
 *
 * Uses a simple moving-average model over the last few months, which is
 * transparent, explainable and needs no external ML library - a good fit
 * for a Java-only project.
 */
public class ExpensePredictor {

    /** How many past months to average over when predicting. */
    private static final int LOOKBACK_MONTHS = 3;

    /**
     * Predicts total spend for next month based on the average of the last
     * {@link #LOOKBACK_MONTHS} months of actual spending.
     */
    public static double predictNextMonthTotal(int userId) {
        double sum = 0;
        int monthsWithData = 0;
        LocalDate cursor = LocalDate.now();

        for (int i = 1; i <= LOOKBACK_MONTHS; i++) {
            LocalDate month = cursor.minusMonths(i);
            double total = ExpenseService.getTotalExpensesForMonth(userId, month.getMonthValue(), month.getYear());
            if (total > 0) {
                sum += total;
                monthsWithData++;
            }
        }

        if (monthsWithData == 0) {
            // Not enough history - fall back to the current month's spend so far.
            LocalDate now = LocalDate.now();
            return ExpenseService.getTotalExpensesForMonth(userId, now.getMonthValue(), now.getYear());
        }

        return sum / monthsWithData;
    }

    /**
     * Predicts next month's spend broken down per category, using each
     * category's historical average over the lookback window.
     */
    public static Map<String, Double> predictNextMonthByCategory(int userId) {
        Map<String, Double> categorySums = new LinkedHashMap<>();
        Map<String, Integer> categoryMonthCounts = new LinkedHashMap<>();
        LocalDate cursor = LocalDate.now();

        for (int i = 1; i <= LOOKBACK_MONTHS; i++) {
            LocalDate month = cursor.minusMonths(i);
            Map<String, Double> monthTotals =
                    ExpenseService.getCategoryTotalsForMonth(userId, month.getMonthValue(), month.getYear());

            for (Map.Entry<String, Double> entry : monthTotals.entrySet()) {
                categorySums.merge(entry.getKey(), entry.getValue(), Double::sum);
                categoryMonthCounts.merge(entry.getKey(), 1, Integer::sum);
            }
        }

        Map<String, Double> predictions = new LinkedHashMap<>();
        for (String category : categorySums.keySet()) {
            double avg = categorySums.get(category) / categoryMonthCounts.get(category);
            predictions.put(category, avg);
        }
        return predictions;
    }

    /**
     * Detects whether the user is overspending this month relative to
     * their own recent-month average, and by how much (as a percentage).
     * Returns 0 if no overspending or not enough data.
     */
    public static double getOverspendPercent(int userId) {
        LocalDate now = LocalDate.now();
        double currentMonthSoFar = ExpenseService.getTotalExpensesForMonth(userId, now.getMonthValue(), now.getYear());
        double predicted = predictNextMonthTotal(userId);

        if (predicted <= 0) {
            return 0;
        }
        double diffPercent = ((currentMonthSoFar - predicted) / predicted) * 100.0;
        return Math.max(diffPercent, 0);
    }

    /** Convenience helper: all of a user's expenses, most recent first. */
    public static List<Expense> getHistorySortedByDateDesc(int userId) {
        List<Expense> list = ExpenseService.getExpensesByUser(userId);
        list.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return list;
    }
}
