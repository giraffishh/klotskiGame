package controller.solver;

import controller.solver.ASearchHashMap.KlotskiSolverAStarHashMap;
import controller.solver.ASearchPDB.KlotskiSolverPDB; // 添加PDB求解器的导入
import controller.solver.ASearchTrie.KlotskiSolverASearchTrie;
import controller.solver.BFS.KlotskiSolverBFS;
import controller.solver.BiBFS.KlotskiSolverBiBFS;
import controller.solver.BiBFSSymmetry.KlotskiSolverBiBFSSymmetry; // 添加BiBFSTrie求解器的导入
import controller.solver.TireTree.KlotskiSolverTrieTree;
import controller.solver.BiBFSOptLayoutGen.KlotskiSolverBiBFSOptLayoutGen; // 添加新的优化布局生成求解器导入
import controller.solver.BFSTireOptLayoutGen.KlotskiSolverBFSTrieOptLayoutGen; // 添加BFS Trie优化布局生成求解器导入


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

// 添加新的BiBFSTrie包装类
class BiBFSSymmetrySolverWrapper implements KlotskiSolverInterface {
    private final BoardState targetState;
    private KlotskiSolverBiBFSSymmetry lastSolver; // 保存最后一个求解器实例

    public BiBFSSymmetrySolverWrapper(BoardState t){
        this.targetState = t;
    }

    @Override
    public List<BoardState> solve(BoardState i) {
        lastSolver = new KlotskiSolverBiBFSSymmetry();
        return lastSolver.solve(i, targetState);
    }

    @Override
    public String getName() {
        return "BiBFS+Sym";
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
        return "BFS+Trie+Sym";
    }

    @Override
    public int getNodesExplored() {
        return lastSolver != null ? lastSolver.getNodesExplored() : -1;
    }
}

class AStarTrieSolverWrapper implements KlotskiSolverInterface {
    private KlotskiSolverASearchTrie lastSolver; // 保存最后一个求解器实例

    @Override
    public List<BoardState> solve(BoardState i) {
        lastSolver = new KlotskiSolverASearchTrie();
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

// 修改PDB求解器的包装类
class PDBSolverWrapper implements KlotskiSolverInterface {
    // 使用一个共享实例，不再需要多次创建
    private static final KlotskiSolverPDB SOLVER = new KlotskiSolverPDB();

    @Override
    public List<BoardState> solve(BoardState i) {
        return SOLVER.solve(i);
    }

    @Override
    public String getName() {
        return "A*+PDB";
    }

    @Override
    public int getNodesExplored() {
        return SOLVER.getNodesExplored();
    }
}

// 添加新的BiBFSOptLayoutGen包装类
class BiBFSOptLayoutGenSolverWrapper implements KlotskiSolverInterface {
    private final BoardState targetState;
    private KlotskiSolverBiBFSOptLayoutGen lastSolver; // 保存最后一个求解器实例

    public BiBFSOptLayoutGenSolverWrapper(BoardState t){
        this.targetState = t;
    }

    @Override
    public List<BoardState> solve(BoardState i) {
        lastSolver = new KlotskiSolverBiBFSOptLayoutGen();
        return lastSolver.solve(i, targetState);
    }

    @Override
    public String getName() {
        return "BiBFS+OptLG";
    }

    @Override
    public int getNodesExplored() {
        return lastSolver != null ? lastSolver.getNodesExplored() : -1;
    }
}

// 添加新的BFSTrieOptLayoutGen包装类
class BFSTrieOptLayoutGenSolverWrapper implements KlotskiSolverInterface {
    private KlotskiSolverBFSTrieOptLayoutGen lastSolver; // 保存最后一个求解器实例

    @Override
    public List<BoardState> solve(BoardState i) {
        lastSolver = new KlotskiSolverBFSTrieOptLayoutGen();
        return lastSolver.solve(i);
    }

    @Override
    public String getName() {
        return "BFS+Trie+OptLG";
    }

    @Override
    public int getNodesExplored() {
        return lastSolver != null ? lastSolver.getNodesExplored() : -1;
    }
}

// --- End Wrappers ---

// 修改LayoutConfig类，移除手动设置的目标状态
class LayoutConfig {
    final String name;
    final BoardState initialState;
    BoardState targetState; // 不再为final，将通过求解器计算获得

    public LayoutConfig(String name, int[][] initialArray) {
        this.name = name;
        this.initialState = new BoardState(initialArray);
        this.targetState = null; // 初始时为null，稍后计算
    }
}

/**
 * Benchmarking program for Klotski solvers with multiple runs and averaging.
 */
public class KlotskiBenchmark {

    private static final long SOLVE_TIMEOUT_MS = 60 * 1000;
    private static final int RUNS_PER_SOLVER = 100; // <<< Number of runs for averaging

    /**
     * 定义测试布局的初始状态
     * @return 布局配置列表
     */
    private static List<LayoutConfig> defineLayouts() {
        List<LayoutConfig> layouts = new ArrayList<>();
        
        // Layout 1 横刀立马
        layouts.add(new LayoutConfig("HengDaoLiMa", 
            new int[][]{
                {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
                {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER}
            }
        ));

        // Layout 2 兵临城下
        layouts.add(new LayoutConfig("BingLinChengXia", 
            new int[][]{
                {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.SOLDIER},
                {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.SOLDIER},
                {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                {BoardSerializer.EMPTY, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.EMPTY}
            }
        ));

        // Layout 3 峰回路转
        layouts.add(new LayoutConfig("FengHuiLuZhuan", 
            new int[][]{
                {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                {BoardSerializer.SOLDIER, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.SOLDIER},
                {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER}
            }
        ));

        // Layout 4
        layouts.add(new LayoutConfig("Start4", 
            new int[][]{
                {BoardSerializer.EMPTY, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.EMPTY},
                {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.SOLDIER},
                {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                {BoardSerializer.SOLDIER, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.SOLDIER}
            }
        ));

        // Layout 5
        layouts.add(new LayoutConfig("Start5", 
            new int[][]{
                {BoardSerializer.SOLDIER, BoardSerializer.VERTICAL, BoardSerializer.VERTICAL, BoardSerializer.VERTICAL},
                {BoardSerializer.SOLDIER, BoardSerializer.VERTICAL, BoardSerializer.VERTICAL, BoardSerializer.VERTICAL},
                {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER},
                {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO},
                {BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO}
            }
        ));
        layouts.add(new LayoutConfig("Start6",
                new int[][]{
                        {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                        {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                        {BoardSerializer.SOLDIER, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.SOLDIER},
                        {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
                        {BoardSerializer.VERTICAL, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.VERTICAL}
                }
        ));
        
        return layouts;
    }

    /**
     * 定义求解器算法
     * @param targetState 目标状态
     * @return 求解器包装器列表
     */
    private static List<KlotskiSolverInterface> defineSolvers(BoardState targetState) {
        List<KlotskiSolverInterface> solvers = new ArrayList<>();
        solvers.add(new AStarHashMapSolverWrapper());
        solvers.add(new AStarTrieSolverWrapper());
        solvers.add(new BFSSolverWrapper());
        solvers.add(new BiBFSSolverWrapper(targetState));
        solvers.add(new TrieBFSSolverWrapper());
        solvers.add(new BiBFSSymmetrySolverWrapper(targetState));
        solvers.add(new BiBFSOptLayoutGenSolverWrapper(targetState));
        solvers.add(new BFSTrieOptLayoutGenSolverWrapper());
        //solvers.add(new PDBSolverWrapper());
        return solvers;
    }

    /**
     * Runs a single benchmark instance with timeout. (same as before)
     */
    private static BenchmarkResult runSingleBenchmark(KlotskiSolverInterface solver, BoardState initialState) {
        BenchmarkResult result = new BenchmarkResult();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // 不再需要每次创建新求解器，直接使用传入的实例
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

        List<LayoutConfig> layoutConfigs = defineLayouts();
        
        // 结果存储: 布局名称 -> 算法名称 -> 运行结果列表
        Map<String, Map<String, List<BenchmarkResult>>> results = new LinkedHashMap<>();

        System.out.println("Defined " + layoutConfigs.size() + " layouts");
        System.out.println("Runs per solver/layout combination: " + RUNS_PER_SOLVER);
        System.out.println("Timeout per run: " + SOLVE_TIMEOUT_MS + " ms");

        // 添加计算目标状态的代码
        System.out.println("\nCalculating target states using BFS solver...");
        for (LayoutConfig config : layoutConfigs) {
            System.out.println("Calculating target state for layout: " + config.name);
            
            // 创建BFS求解器实例计算目标状态
            KlotskiSolverBFS targetFinder = new KlotskiSolverBFS();
            List<BoardState> solution = targetFinder.solve(config.initialState);
            
            if (solution.isEmpty()) {
                System.out.println("WARNING: 无法为 " + config.name + " 找到目标状态。跳过此布局。");
                continue;
            }
            
            // 使用解决方案的最后一个状态作为目标状态
            config.targetState = solution.get(solution.size() - 1);
            System.out.println("目标状态已计算，共 " + (solution.size() - 1) + " 步，探索了 " 
                              + targetFinder.getNodesExplored() + " 个节点");
        }

        // 运行基准测试
        for (LayoutConfig config : layoutConfigs) {
            // 跳过没有目标状态的布局
            if (config.targetState == null) {
                continue;
            }
            
            String layoutName = config.name;
            BoardState initialState = config.initialState;
            BoardState targetState = config.targetState;
            
            // 为每个布局生成定制的求解器
            List<KlotskiSolverInterface> solvers = defineSolvers(targetState);
            
            results.put(layoutName, new LinkedHashMap<>());

            System.out.println("\n--- Benchmarking Layout: " + layoutName + " ---");
            System.out.println("Initial state:");
            BoardSerializer.printBoard(initialState.getBoardArray());
            System.out.println("Target state:");
            BoardSerializer.printBoard(targetState.getBoardArray());

            for (KlotskiSolverInterface solver : solvers) {
                String solverName = solver.getName();
                results.get(layoutName).put(solverName, new ArrayList<>()); // 初始化此求解器的列表
                //System.out.println("  Running solver: " + solverName + " (" + RUNS_PER_SOLVER + " times)");

                for (int run = 1; run <= RUNS_PER_SOLVER; run++) {
                    //System.out.print("    Run " + run + "/" + RUNS_PER_SOLVER + "... ");
                    // 运行单次基准测试
                    BenchmarkResult singleRunResult = runSingleBenchmark(solver, initialState);
                    results.get(layoutName).get(solverName).add(singleRunResult);
                    //System.out.println(singleRunResult.status + " (" + singleRunResult.timeMillis + " ms)");
                }
            }
        }

        // 打印结果表格和平均值
        System.out.println("\n\n--- Benchmark Results (Averages over " + RUNS_PER_SOLVER + " runs) ---");

        int maxAlgNameLen = results.values().stream()
                .flatMap(m -> m.keySet().stream())
                .mapToInt(String::length)
                .max().orElse(15);
        
        // 调整平均结果的格式字符串
        String headerFormat = "%-" + maxAlgNameLen + "s";
        String resultFormat = "%-" + maxAlgNameLen + "s | %s%n"; // 算法 | 结果字符串

        for (Map.Entry<String, Map<String, List<BenchmarkResult>>> layoutResultEntry : results.entrySet()) {
            String layoutName = layoutResultEntry.getKey();
            Map<String, List<BenchmarkResult>> algoResultsMap = layoutResultEntry.getValue();

            System.out.println("\nLayout: " + layoutName);
            System.out.printf(headerFormat, "Algorithm");
            System.out.println(" | Avg Time (ms), Steps, Success Rate, Nodes Explored [Status]");
            System.out.println("-".repeat(maxAlgNameLen) + "-|-" + "-".repeat(50)); // 分隔线

            // 打印此布局的每个算法的结果
            for (Map.Entry<String, List<BenchmarkResult>> algoEntry : algoResultsMap.entrySet()) {
                String solverName = algoEntry.getKey();
                List<BenchmarkResult> runResults = algoEntry.getValue();

                // 计算统计数据
                List<BenchmarkResult> solvedRuns = runResults.stream()
                        .filter(r -> r.status.equals("Solved"))
                        .collect(Collectors.toList());

                long totalSuccessfulTime = solvedRuns.stream().mapToLong(r -> r.timeMillis).sum();
                int successfulRuns = solvedRuns.size();
                double averageTime = (successfulRuns > 0) ? (double) totalSuccessfulTime / successfulRuns : -1.0;
                int steps = (successfulRuns > 0) ? solvedRuns.get(0).steps : -1; // 假设最优解的步数一致
                String successRate = String.format("%d/%d", successfulRuns, runResults.size());

                // 计算平均探索节点数
                double avgNodesExplored = -1;
                if (successfulRuns > 0) {
                    avgNodesExplored = solvedRuns.stream()
                            .mapToInt(r -> r.nodesExplored)
                            .average()
                            .orElse(-1);
                }

                // 确定整体状态
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

                // 格式化输出字符串
                String resultString;
                if (averageTime >= 0) {
                    resultString = String.format("%8.2f ms, %3d steps, %s, %,.0f nodes [%s]",
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

