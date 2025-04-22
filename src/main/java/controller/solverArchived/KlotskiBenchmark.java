package controller.solverArchived;

import controller.solver.BoardSerializer;
import controller.solver.BoardState;
import controller.solver.KlotskiSolver; // 添加导入新的KlotskiSolver类
import controller.solverArchived.ASearchHashMap.KlotskiSolverASearchHashMap;
import controller.solverArchived.ASearchPDB.KlotskiSolverPDB; // 添加PDB求解器的导入
import controller.solverArchived.ASearchTrie.KlotskiSolverASearchTrie;
import controller.solverArchived.BFS.KlotskiSolverBFS;
import controller.solverArchived.BiBFS.KlotskiSolverBiBFS;
import controller.solverArchived.BiBFSSymmetry.KlotskiSolverBiBFSSymmetry; // 添加BiBFSTrie求解器的导入
import controller.solverArchived.TireTree.KlotskiSolverTrieTree;
import controller.solverArchived.BiBFSOptLayoutGen.KlotskiSolverBiBFSOptLayoutGen; // 添加新的优化布局生成求解器导入
import controller.solverArchived.BFSTireOptLayoutGen.KlotskiSolverBFSTrieOptLayoutGen; // 添加BFS Trie优化布局生成求解器导入
import controller.solverArchived.ASearchTrieOptLayoutGen.KlotskiSolverASearchTrieOptLayoutGen; // 添加 A* Trie 优化布局生成求解器导入

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
    private KlotskiSolverASearchHashMap lastSolver; // 保存最后一个求解器实例

    @Override
    public List<BoardState> solve(BoardState i) {
        lastSolver = new KlotskiSolverASearchHashMap();
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

// 添加 A*+Trie+OptLayoutGen 包装类
class ASearchTrieOptLayoutGenSolverWrapper implements KlotskiSolverInterface {
    private KlotskiSolverASearchTrieOptLayoutGen lastSolver; // 保存最后一个求解器实例

    @Override
    public List<BoardState> solve(BoardState i) {
        lastSolver = new KlotskiSolverASearchTrieOptLayoutGen();
        return lastSolver.solve(i);
    }

    @Override
    public String getName() {
        return "A*+Trie+OptLG";
    }

    @Override
    public int getNodesExplored() {
        return lastSolver != null ? lastSolver.getNodesExplored() : -1;
    }
}

// 修改 NewKlotskiSolverWrapper 类，正确实现新算法的复用测试
class NewKlotskiSolverWrapper implements KlotskiSolverInterface {
    private controller.solver.KlotskiSolver solver; // 保存求解器实例
    private boolean initialSolveCompleted = false;
    private int nodesExplored = -1;

    public NewKlotskiSolverWrapper(controller.solver.KlotskiSolver solver) {
        this.solver = solver;
    }

    @Override
    public List<BoardState> solve(BoardState state) {
        List<BoardState> path;
        
        if (!initialSolveCompleted) {
            // 第一次运行：执行完整初始化求解（Phase 1）
            path = solver.initialSolve(state);
            initialSolveCompleted = true;
            System.out.println("KlotskiSolver: Phase 1完成，全局数据已构建");
        } else {
            // 后续运行：从非最优路径状态测试复用能力（模拟Phase 3）
            path = solver.findPathFrom(state);
        }
        
        nodesExplored = solver.getNodesExploredTotal();
        return path;
    }

    @Override
    public String getName() {
        return "Hybrid PrBFS+A*";
    }

    @Override
    public int getNodesExplored() {
        return nodesExplored;
    }
    
    // 重置求解器的首次运行状态，用于新布局测试
    public void resetSolver() {
        initialSolveCompleted = false;
    }
}

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
    private static final int RUNS_PER_SOLVER = 10; // <<< Number of runs for averaging

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
        //solvers.add(new AStarTrieSolverWrapper());
        //solvers.add(new BFSSolverWrapper());
        //solvers.add(new BiBFSSolverWrapper(targetState));
        //solvers.add(new TrieBFSSolverWrapper());
        //solvers.add(new BiBFSSymmetrySolverWrapper(targetState));
        solvers.add(new BiBFSOptLayoutGenSolverWrapper(targetState));
        solvers.add(new BFSTrieOptLayoutGenSolverWrapper());
        //solvers.add(new ASearchTrieOptLayoutGenSolverWrapper()); // 添加新的 A*+Trie+OptLG 求解器
        
        // 将新的求解器添加到最后，使其单独处理
        solvers.add(new NewKlotskiSolverWrapper(new controller.solver.KlotskiSolver()));
        
        //solvers.add(new PDBSolverWrapper());
        return solvers;
    }

    /**
     * Runs a single benchmark instance with timeout. (same as before)
     */
    private static BenchmarkResult runSingleBenchmark(KlotskiSolverInterface solver, BoardState initialState) {
        BenchmarkResult result = new BenchmarkResult();
        ExecutorService executor = Executors.newSingleThreadExecutor();

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
            
            // 获取求解器列表，每个布局只定义一次
            List<KlotskiSolverInterface> solvers = defineSolvers(targetState);
            
            results.put(layoutName, new LinkedHashMap<>());

            System.out.println("\n--- Benchmarking Layout: " + layoutName + " ---");
            System.out.println("Initial state:");
            BoardSerializer.printBoard(initialState.getBoardArray());
            System.out.println("Target state:");
            BoardSerializer.printBoard(targetState.getBoardArray());

            // 找到NewKlotskiSolverWrapper实例以便特殊处理
            NewKlotskiSolverWrapper newSolverWrapper = null;
            for (KlotskiSolverInterface solver : solvers) {
                if (solver instanceof NewKlotskiSolverWrapper) {
                    newSolverWrapper = (NewKlotskiSolverWrapper) solver;
                    break;
                }
            }

            // 对于常规求解器的测试，保持不变
            for (KlotskiSolverInterface solver : solvers) {
                // 跳过KlotskiSolver，稍后单独测试
                if (solver instanceof NewKlotskiSolverWrapper) {
                    continue;
                }

                String solverName = solver.getName();
                results.get(layoutName).put(solverName, new ArrayList<>()); // 初始化此求解器的列表
                
                System.out.println("  Running solver: " + solverName + " (" + RUNS_PER_SOLVER + " times)");

                for (int run = 1; run <= RUNS_PER_SOLVER; run++) {
                    BenchmarkResult singleRunResult = runSingleBenchmark(solver, initialState);
                    results.get(layoutName).get(solverName).add(singleRunResult);
                }
            }
            
            // 单独测试KlotskiSolver（Phase 1 + Phase 3）
            if (newSolverWrapper != null) {
                String solverName = newSolverWrapper.getName();
                results.get(layoutName).put(solverName, new ArrayList<>()); // 初始化此求解器的列表

                System.out.println("  Running KlotskiSolver Phase 1 + Phase 3 tests:");

                // 第一次运行（Phase 1）- 初始化全局数据，结果不计入统计
                System.out.println("    Phase 1: 初始化全局数据...");
                BenchmarkResult initialResult = runSingleBenchmark(newSolverWrapper, initialState);
                System.out.println("    Phase 1 完成: " + initialResult.status +
                    " (" + initialResult.timeMillis + " ms, 探索 " +
                    initialResult.nodesExplored + " 节点)");

                // 创建非最优路径状态 - 根据初始状态修改
                System.out.println("    生成非最优路径状态用于Phase 3测试...");
                BoardState nonOptimalState = createNonOptimalState(initialState);

                if (nonOptimalState != null) {
                    System.out.println("    Phase 3: 从非最优路径状态测试 (" + RUNS_PER_SOLVER + " times)");
                    for (int run = 1; run <= RUNS_PER_SOLVER; run++) {
                        // 使用非最优路径状态测试findPathFrom方法
                        BenchmarkResult runResult = runSingleBenchmark(newSolverWrapper, nonOptimalState);
                        results.get(layoutName).get(solverName).add(runResult);
                    }
                } else {
                    System.out.println("    无法创建非最优路径状态，使用原始状态测试...");
                    for (int run = 1; run <= RUNS_PER_SOLVER; run++) {
                        BenchmarkResult runResult = runSingleBenchmark(newSolverWrapper, initialState);
                        results.get(layoutName).get(solverName).add(runResult);
                    }
                }

                // 布局测试完成后，重置KlotskiSolver的初始化状态
                newSolverWrapper.resetSolver();
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

    /**
     * 创建一个非最优路径状态，用于测试KlotskiSolver的Phase 3
     * 直接使用KlotskiSolver中Phase 3的测试方法实现
     */
    private static BoardState createNonOptimalState(BoardState initialState) {
        try {
            System.out.println("    使用KlotskiSolver中的Phase 3测试逻辑创建非最优路径状态...");

            // 初始化一个临时求解器生成初始解
            KlotskiSolver tempSolver = new KlotskiSolver();
            List<BoardState> initialSolution = tempSolver.initialSolve(initialState);

            if (initialSolution == null || initialSolution.size() <= 1) {
                System.out.println("    无法获取初始解，无法创建非最优路径状态");
                return null;
            }

            // 获取最优路径中的第一步
            BoardState firstOptimalStep = initialSolution.get(1);

            // 获取初始状态的所有后继状态
            List<Long> successors = generateSuccessorLayouts(initialState.getLayout());
            if (successors.isEmpty()) {
                System.out.println("    初始状态没有后继状态，无法创建非最优路径状态");
                return null;
            }

            // 找到一个与最优第一步不同的后继状态
            for (long succLayout : successors) {
                if (getCanonicalLayout(succLayout) != getCanonicalLayout(firstOptimalStep.getLayout())) {
                    System.out.println("    成功创建非最优路径状态");
                    return new BoardState(succLayout);
                }
            }

            // 如果所有后继状态都是最优的（不太可能），则使用第一个后继状态
            System.out.println("    所有后继状态都与最优路径一致，使用第一个后继状态");
            return new BoardState(successors.get(0));
        } catch (Exception e) {
            System.err.println("    创建非最优路径状态时出错: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 生成后继布局 - 使用KlotskiSolver中的方法
     */
    private static List<Long> generateSuccessorLayouts(long currentLayout) {
        // 重用KlotskiSolver中的同名方法，但将其复制过来以避免依赖问题

        final int ROWS = BoardSerializer.ROWS;
        final int COLS = BoardSerializer.COLS;
        final int BITS_PER_CELL = 3;
        final int TOTAL_CELLS = ROWS * COLS;
        final int[] DR = {-1, 1, 0, 0};
        final int[] DC = {0, 0, -1, 1};
        final long CELL_MASK_3BIT = (1L << BITS_PER_CELL) - 1L;

        List<Long> successorLayouts = new ArrayList<>();
        boolean[] processedCell = new boolean[TOTAL_CELLS];

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int cellIndex = r * COLS + c;
                if (processedCell[cellIndex]) continue;

                long pieceCode = getCellCode(currentLayout, r, c);
                processedCell[cellIndex] = true;

                if (pieceCode == BoardSerializer.CODE_EMPTY) continue;

                List<int[]> pieceCellsCoords = new ArrayList<>();
                pieceCellsCoords.add(new int[]{r, c});

                if (pieceCode == BoardSerializer.CODE_SOLDIER) {
                    // 单兵，已经添加到pieceCellsCoords
                } else if (pieceCode == BoardSerializer.CODE_HORIZONTAL) {
                    if (c + 1 < COLS && getCellCode(currentLayout, r, c + 1) == pieceCode) {
                        pieceCellsCoords.add(new int[]{r, c + 1});
                        processedCell[cellIndex + 1] = true;
                    } else continue;
                } else if (pieceCode == BoardSerializer.CODE_VERTICAL) {
                    if (r + 1 < ROWS && getCellCode(currentLayout, r + 1, c) == pieceCode) {
                        pieceCellsCoords.add(new int[]{r + 1, c});
                        processedCell[cellIndex + COLS] = true;
                    } else continue;
                } else if (pieceCode == BoardSerializer.CODE_CAO_CAO) {
                    boolean rOk = c + 1 < COLS && getCellCode(currentLayout, r, c + 1) == pieceCode;
                    boolean bOk = r + 1 < ROWS && getCellCode(currentLayout, r + 1, c) == pieceCode;
                    boolean brOk = c + 1 < COLS && r + 1 < ROWS && getCellCode(currentLayout, r + 1, c + 1) == pieceCode;

                    if (rOk && bOk && brOk) {
                        pieceCellsCoords.add(new int[]{r, c + 1});
                        processedCell[cellIndex + 1] = true;
                        pieceCellsCoords.add(new int[]{r + 1, c});
                        processedCell[cellIndex + COLS] = true;
                        pieceCellsCoords.add(new int[]{r + 1, c + 1});
                        processedCell[cellIndex + COLS + 1] = true;
                    } else continue;
                } else {
                    continue;
                }

                // 尝试四个方向移动
                for (int dir = 0; dir < 4; dir++) {
                    int dr = DR[dir], dc = DC[dir];
                    boolean canMove = true;
                    List<int[]> targetCellsCoords = new ArrayList<>();

                    for (int[] cellCoord : pieceCellsCoords) {
                        int nr = cellCoord[0] + dr;
                        int nc = cellCoord[1] + dc;

                        if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS) {
                            canMove = false;
                            break;
                        }

                        targetCellsCoords.add(new int[]{nr, nc});

                        boolean targetIsOriginal = false;
                        for (int[] originalCoord : pieceCellsCoords) {
                            if (nr == originalCoord[0] && nc == originalCoord[1]) {
                                targetIsOriginal = true;
                                break;
                            }
                        }

                        if (!targetIsOriginal && getCellCode(currentLayout, nr, nc) != BoardSerializer.CODE_EMPTY) {
                            canMove = false;
                            break;
                        }
                    }

                    if (canMove) {
                        long newLayout = currentLayout;
                        long clearMask = 0L;
                        long setMask = 0L;

                        for (int[] cellCoord : pieceCellsCoords) {
                            clearMask |= (CELL_MASK_3BIT << ((cellCoord[0] * COLS + cellCoord[1]) * BITS_PER_CELL));
                        }

                        for (int[] targetCoord : targetCellsCoords) {
                            setMask |= (pieceCode << ((targetCoord[0] * COLS + targetCoord[1]) * BITS_PER_CELL));
                        }

                        newLayout = (newLayout & ~clearMask) | setMask;
                        successorLayouts.add(newLayout);
                    }
                }
            }
        }

        return successorLayouts;
    }

    /**
     * 获取单元格代码
     */
    private static long getCellCode(long layout, int r, int c) {
        final int ROWS = BoardSerializer.ROWS;
        final int COLS = BoardSerializer.COLS;
        final int BITS_PER_CELL = 3;
        final long CELL_MASK_3BIT = (1L << BITS_PER_CELL) - 1L;

        if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return -1; // 无效坐标
        int shift = (r * COLS + c) * BITS_PER_CELL;
        return (layout >> shift) & CELL_MASK_3BIT;
    }

    /**
     * 获取对称布局
     */
    private static long getSymmetricLayout(long layout) {
        final int ROWS = BoardSerializer.ROWS;
        final int COLS = BoardSerializer.COLS;
        final int BITS_PER_CELL = 3;

        long symmetricLayout = 0L;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                long cellCode = getCellCode(layout, r, c);
                int mirroredCol = COLS - 1 - c;
                int shift = (r * COLS + mirroredCol) * BITS_PER_CELL;
                symmetricLayout |= (cellCode << shift);
            }
        }
        return symmetricLayout;
    }

    /**
     * 获取规范化布局（取原始布局与对称布局中的较小值）
     */
    private static long getCanonicalLayout(long layout) {
        return Math.min(layout, getSymmetricLayout(layout));
    }
}
