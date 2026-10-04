# GoalWise

AI-Powered Personal Finance and Goal-Based Savings Management System (Java + Swing).

## What it does

Tracks income and expenses, lets you set category budgets, and helps you hit
savings goals with an AI-style assistant (rule-based, no external ML library
required) that:

- Predicts next month's spending, overall and per category
- Flags overspending and near-limit budgets
- Assesses whether a savings goal is achievable by its target date
- Recommends specific category cuts to close any shortfall
- Computes an overall 0-100 Financial Health Score

## Running it

The app stores data **in memory** by default, so it runs with zero setup -
no database installation required. Data resets each time you restart it.

### Option A: IntelliJ IDEA (recommended)

1. `File > Open` and select the `GoalWise` folder.
2. If prompted, let IntelliJ import it as a Maven project (or just mark
   `src` as the Sources Root if you don't want to use Maven at all).
3. Run `com.goalwise.Main`.

### Option B: Command line (plain javac, no Maven needed)

```bash
cd GoalWise
find src -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out com.goalwise.Main
```

### Option C: Maven

```bash
cd GoalWise
mvn package
java -jar target/GoalWise.jar
```

## Enabling real MySQL persistence (optional)

The app ships with an in-memory data layer (see the `service` package) so it
"just works" out of the box. If you want data to persist across restarts:

1. Install MySQL and run `src/com/goalwise/db/schema.sql` against it:
   ```bash
   mysql -u root -p < src/com/goalwise/db/schema.sql
   ```
2. Update the username/password/host constants in
   `src/com/goalwise/db/DatabaseConnection.java` to match your setup.
3. Make sure `mysql-connector-j` is on your classpath (already declared in
   `pom.xml` — `mvn package` pulls it automatically).
4. Update the `service` classes to call `DatabaseConnection.getConnection()`
   and run SQL against the tables in `schema.sql` instead of the in-memory
   lists. This is intentionally left as the next step so you can decide how
   much of the app you want backed by the database for your submission.

## Project structure

```
GoalWise/
├── pom.xml
├── README.md
└── src/com/goalwise/
    ├── Main.java              entry point
    ├── db/
    │   ├── DatabaseConnection.java
    │   └── schema.sql
    ├── model/                 User, Income, Expense, Budget, Goal
    ├── service/                business logic (currently in-memory storage)
    ├── ai/                    ExpensePredictor, GoalAdvisor,
    │                          RecommendationEngine, FinancialHealthScore
    └── ui/                    Swing screens (Login, Register, Dashboard,
                               AddIncome, AddExpense, BudgetScreen,
                               GoalScreen, AIInsights, Reports)
```

## Demo flow

1. Register an account with a monthly income.
2. Add a few income and expense entries (try different categories).
3. Set a budget for a category you spend a lot in.
4. Create a goal, e.g. "Buy a Dress", target Rs.6000, already saved Rs.1000,
   2 months out.
5. Open **AI Insights** for the health score and spending predictions.
6. In **Goals**, select your goal and click **Check Achievability & Tips**
   to see the AI's recommendation.
