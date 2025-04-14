package controller.solver;

import controller.solver.ASearchHashMap.KlotskiSolverAStarHashMap; // 添加导入
import controller.solver.ASearchTrie.KlotskiSolverAsearch;
import controller.solver.BFS.KlotskiSolverBFS;
import controller.solver.BiBFS.KlotskiSolverBiBFS;
import controller.solver.TireTree.KlotskiSolverTrieTree;


import java.util.ArrayList;
import java.util.Collections; // Added for Collections.emptyList() in placeholders
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors; // For stream calculations

/**
 * Represents the result of a single benchmark run.
 * (No changes needed here for basic averaging, calculation done later)
 */
class BenchmarkResult {
    long timeMillis = -1;
    int steps = -1;
    String status = "Not Run";
    int nodesExplored = -1; // 添加节点探索数量字段

    @Override
    public String toString() {
        if (status.equals("Solved")) {
            return String.format("%5d ms, %3d steps, %,d nodes", timeMillis, steps, nodesExplored);
        } else {
            return String.format("%5s ms, %3s steps, %s nodes [%s]",
                    timeMillis == -1 ? "N/A" : String.valueOf(timeMillis),
                    steps == -1 ? "N/A" : String.valueOf(steps),
                    nodesExplored == -1 ? "N/A" : String.format("%,d", nodesExplored),
                    status);
        }
    }
}

// --- KlotskiSolverInterface and Wrapper classes remain the same ---
// Make sure these wrappers correctly instantiate *new* solver instances inside their solve() method!
interface KlotskiSolverInterface {
    List<BoardState> solve(BoardState initialState);
    String getName();
    int getNodesExplored(); // 添加获取节点数的方法
}

class BFSSolverWrapper implements KlotskiSolverInterface {
    private KlotskiSolverBFS lastSolver; // 保存最后一个求解器实例

    @Override
    public List<BoardState> solve(BoardState i) {
        lastSolver = new KlotskiSolverBFS();
        return lastSolver.solve(i);
    }

    @Override
    public String getName() {
        return "BFS (Original)";
    }

    @Override
    public int getNodesExplored() {
        return lastSolver != null ? lastSolver.getNodesExplored() : -1;
    }
}

class BiBFSSolverWrapper implements KlotskiSolverInterface {
    private final BoardState targetState;
    private KlotskiSolverBiBFS lastSolver; // 保存最后一个求解器实例

    public BiBFSSolverWrapper(BoardState t){
        this.targetState=t;
    }

    @Override
    public List<BoardState> solve(BoardState i) {
        lastSolver = new KlotskiSolverBiBFS();
        return lastSolver.solve(i, targetState);
    }

    @Override
    public String getName() {
        return "Bi-BFS";
    }

    @Override
    public int getNodesExplored() {
        return lastSolver != null ? lastSolver.getNodesExplored() : -1;
    }
}

class TrieBFSSolverWrapper implements KlotskiSolverInterface {
    private KlotskiSolverTrieTree lastSolver; // 保存最后一个求解器实例

    @Override
    public List<BoardState> solve(BoardState i) {
        lastSolver = new KlotskiSolverTrieTree();
        return lastSolver.solve(i);
    }

    @Override
    public String getName() {
        return "Trie+Sym BFS";
    }

    @Override
    public int getNodesExplored() {
        return lastSolver != null ? lastSolver.getNodesExplored() : -1;
    }
}

class AStarTrieSolverWrapper implements KlotskiSolverInterface {
    private KlotskiSolverAsearch lastSolver; // 保存最后一个求解器实例

    @Override
    public List<BoardState> solve(BoardState i) {
        lastSolver = new KlotskiSolverAsearch();
        return lastSolver.solve(i);
    }

    @Override
    public String getName() {
        return "A*+Trie+Sym";
    }

    @Override
    public int getNodesExplored() {
        return lastSolver != null ? lastSolver.getNodesExplored() : -1;
    }
}

// 添加A* HashMap求解器的包装类
class AStarHashMapSolverWrapper implements KlotskiSolverInterface {
    private KlotskiSolverAStarHashMap lastSolver; // 保存最后一个求解器实例

    @Override
    public List<BoardState> solve(BoardState i) {
        lastSolver = new KlotskiSolverAStarHashMap();
        return lastSolver.solve(i);
    }

    @Override
    public String getName() {
        return "A* HashMap";
    }

    @Override
    public int getNodesExplored() {
        return lastSolver != null ? lastSolver.getNodesExplored() : -1;
    }
}
// --- End Wrappers ---

/**
 * Benchmarking program for Klotski solvers with multiple runs and averaging.
 */
public class KlotskiBenchmark {

    private static final long SOLVE_TIMEOUT_MS = 60 * 1000; // 60 seconds timeout
    private static final int RUNS_PER_SOLVER = 10; // <<< Number of runs for averaging

    // Define a standard target state for Bi-BFS (same as before)
    private static final BoardState TARGET_STATE = new BoardState(new int[][]{
            {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
            {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
            {BoardSerializer.HORIZONTAL, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.HORIZONTAL},
            {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
            {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL}
    });

    /**
     * Defines the initial board layouts to test. (same as before)
     * @return A map of layout names to their 2D array representations.
     */
    private static Map<String, int[][]> defineLayouts() { /* ... same as before ... */
        Map<String, int[][]> layouts = new LinkedHashMap<>();
        // Layout 1
        layouts.put("HengDaoLiMa", new int[][]{
                {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
                {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER}
        });

        // Layout 2
        layouts.put("BingLinChengXia", new int[][]{
                {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.SOLDIER},
                {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.SOLDIER},
                {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                {BoardSerializer.EMPTY, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.EMPTY}
        });

        // Layout 3
        layouts.put("FengHuiLuZhuan", new int[][]{
                {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.HORIZONTAL},
                {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.SOLDIER},
                {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                {BoardSerializer.SOLDIER, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.SOLDIER},
                {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER}
        });

        // Layout 4
        layouts.put("Start4", new int[][]{
                {BoardSerializer.EMPTY, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.EMPTY},
                {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.SOLDIER},
                {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                {BoardSerializer.SOLDIER, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.SOLDIER}
        });

        // Layout 5
        layouts.put("Start5", new int[][]{
                {BoardSerializer.SOLDIER, BoardSerializer.VERTICAL, BoardSerializer.VERTICAL, BoardSerializer.VERTICAL},
                {BoardSerializer.SOLDIER, BoardSerializer.VERTICAL, BoardSerializer.VERTICAL, BoardSerializer.VERTICAL},
                {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER},
                {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO},
                {BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO}
        });
        return layouts;
    }

    /**
     * Defines the solver algorithms to test. (same as before)
     * @return A list of solver wrappers.
     */
    private static List<KlotskiSolverInterface> defineSolvers() { /* ... same as before ... */
        List<KlotskiSolverInterface> solvers = new ArrayList<>();
        solvers.add(new BFSSolverWrapper());
        //solvers.add(new BiBFSSolverWrapper(TARGET_STATE));
        solvers.add(new AStarHashMapSolverWrapper());
        solvers.add(new TrieBFSSolverWrapper());
        solvers.add(new AStarTrieSolverWrapper());
        return solvers;
    }

    /**
     * Runs a single benchmark instance with timeout. (same as before)
     */
    private static BenchmarkResult runSingleBenchmark(KlotskiSolverInterface solver, BoardState initialState) {
        BenchmarkResult result = new BenchmarkResult();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // IMPORTANT: The lambda ensures a new solver instance is created via the wrapper's solve method
        Future<List<BoardState>> future = executor.submit(() -> solver.solve(initialState));
        long startTime = System.nanoTime();
        try {
            List<BoardState> path = future.get(SOLVE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            long endTime = System.nanoTime();
            result.timeMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            result.nodesExplored = solver.getNodesExplored(); // 获取节点探索数量

            if (path == null || path.isEmpty()) {
                result.status = "No Solution";
            } else {
                result.status = "Solved";
                result.steps = path.size() - 1;
            }
        } catch (TimeoutException e) {
            future.cancel(true);
            result.status = "Timeout";
            result.timeMillis = SOLVE_TIMEOUT_MS;
            // 尝试获取超时时已探索的节点数
            result.nodesExplored = solver.getNodesExplored();
        }
        catch (InterruptedException | ExecutionException e) {
            future.cancel(true);
            result.status = "Error";
            result.timeMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            // 尝试获取出错时已探索的节点数
            result.nodesExplored = solver.getNodesExplored();
            System.err.println("Error during solve for " + solver.getName() + ": " + e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
        finally { executor.shutdownNow(); }
        return result;
    }


    public static void main(String[] args) {
        System.out.println("Starting Klotski Solver Benchmark...");

        Map<String, int[][]> layouts = defineLayouts();
        List<KlotskiSolverInterface> solvers = defineSolvers();
        // Store results: Layout -> Algorithm -> List of individual run results
        Map<String, Map<String, List<BenchmarkResult>>> results = new LinkedHashMap<>();

        System.out.println("Defined " + layouts.size() + " layouts and " + solvers.size() + " solvers.");
        System.out.println("Runs per solver/layout combination: " + RUNS_PER_SOLVER);
        System.out.println("Timeout per run: " + SOLVE_TIMEOUT_MS + " ms");

        // Run benchmarks
        for (Map.Entry<String, int[][]> layoutEntry : layouts.entrySet()) {
            String layoutName = layoutEntry.getKey();
            BoardState initialState = new BoardState(layoutEntry.getValue());
            results.put(layoutName, new LinkedHashMap<>());

            //System.out.println("\n--- Benchmarking Layout: " + layoutName + " ---");
            //BoardSerializer.printBoard(initialState.getBoardArray());

            for (KlotskiSolverInterface solver : solvers) {
                String solverName = solver.getName();
                results.get(layoutName).put(solverName, new ArrayList<>()); // Initialize list for this solver
                //System.out.println("  Running solver: " + solverName + " (" + RUNS_PER_SOLVER + " times)");

                for (int run = 1; run <= RUNS_PER_SOLVER; run++) {
                    //System.out.print("    Run " + run + "/" + RUNS_PER_SOLVER + "... ");
                    // Run the benchmark for this single run
                    BenchmarkResult singleRunResult = runSingleBenchmark(solver, initialState);
                    results.get(layoutName).get(solverName).add(singleRunResult);
                    //System.out.println(singleRunResult.status + " (" + singleRunResult.timeMillis + " ms)");

                    // Optional: Add a small delay or GC call between runs if needed, though often not necessary
                    // try { Thread.sleep(50); } catch (InterruptedException e) {}
                    // System.gc();
                }
            }
        }

        // Print results table with averages
        System.out.println("\n\n--- Benchmark Results (Averages over " + RUNS_PER_SOLVER + " runs) ---");

        int maxAlgNameLen = solvers.stream().mapToInt(s -> s.getName().length()).max().orElse(15);
        // Adjust format strings for average results
        String headerFormat = "%-" + maxAlgNameLen + "s";
        String resultFormat = "%-" + maxAlgNameLen + "s | %s%n"; // Algorithm | Result String

        for (Map.Entry<String, Map<String, List<BenchmarkResult>>> layoutResultEntry : results.entrySet()) {
            String layoutName = layoutResultEntry.getKey();
            Map<String, List<BenchmarkResult>> algoResultsMap = layoutResultEntry.getValue();

            System.out.println("\nLayout: " + layoutName);
            System.out.printf(headerFormat, "Algorithm");
            System.out.println(" | Avg Time (ms), Steps, Success Rate, Nodes Explored [Status]");
            System.out.println("-".repeat(maxAlgNameLen) + "-|-" + "-".repeat(50)); // Separator line

            // Print results for each algorithm for this layout
            for (KlotskiSolverInterface solver : solvers) {
                String solverName = solver.getName();
                List<BenchmarkResult> runResults = algoResultsMap.getOrDefault(solverName, Collections.emptyList());

                // Calculate statistics
                List<BenchmarkResult> solvedRuns = runResults.stream()
                        .filter(r -> r.status.equals("Solved"))
                        .collect(Collectors.toList());

                long totalSuccessfulTime = solvedRuns.stream().mapToLong(r -> r.timeMillis).sum();
                int successfulRuns = solvedRuns.size();
                double averageTime = (successfulRuns > 0) ? (double) totalSuccessfulTime / successfulRuns : -1.0;
                int steps = (successfulRuns > 0) ? solvedRuns.get(0).steps : -1; // Assume steps are consistent for optimal solvers
                String successRate = String.format("%d/%d", successfulRuns, runResults.size());

                // 计算平均探索节点数
                double avgNodesExplored = -1;
                if (successfulRuns > 0) {
                    avgNodesExplored = solvedRuns.stream()
                            .mapToInt(r -> r.nodesExplored)
                            .average()
                            .orElse(-1);
                }

                // Determine overall status
                String overallStatus;
                if (successfulRuns == runResults.size()) {
                    overallStatus = "OK";
                } else if (successfulRuns > 0) {
                    overallStatus = "Partial Success";
                } else if (runResults.stream().anyMatch(r -> r.status.equals("Timeout"))) {
                    overallStatus = "Timeout";
                } else if (runResults.stream().anyMatch(r -> r.status.equals("No Solution"))) {
                    overallStatus = "No Solution";
                } else if (runResults.stream().anyMatch(r -> r.status.equals("Error"))) {
                    overallStatus = "Error";
                } else {
                    overallStatus = "Unknown";
                }

                // Format output string
                String resultString;
                if (averageTime >= 0) {
                    resultString = String.format("%8.1f ms, %3d steps, %s, %,.0f nodes [%s]",
                            averageTime, steps, successRate, avgNodesExplored, overallStatus);
                } else {
                    resultString = String.format("%8s ms, %3s steps, %s, %s nodes [%s]",
                            "N/A", "N/A", successRate, "N/A", overallStatus);
                }

                System.out.printf(resultFormat, solverName, resultString);
            }
        }

        System.out.println("\nBenchmark finished.");
    }

    // --- Placeholder/Dummy Solver Classes (Ensure these match your actual classes) ---
    static class KlotskiSolver_BFS_Original { List<BoardState> solve(BoardState i) { System.err.println("BFS Not Implemented"); return Collections.emptyList(); } }
    static class KlotskiSolver_BiBFS { List<BoardState> solve(BoardState i, BoardState t) { System.err.println("BiBFS Not Implemented"); return Collections.emptyList(); } }
    static class KlotskiSolver_IDDFS { List<BoardState> solve(BoardState i) { System.err.println("IDDFS Not Implemented"); return Collections.emptyList(); } }
    static class KlotskiSolver_TrieBFS { List<BoardState> solve(BoardState i) { System.err.println("TrieBFS Not Implemented"); return Collections.emptyList(); } }
    // KlotskiSolver (for A*) is assumed to exist
}

