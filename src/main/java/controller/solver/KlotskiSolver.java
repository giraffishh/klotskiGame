package controller.solver;

import java.util.*;

// --- TrieNode for V5 ---
// Stores only the minimum absolute g-cost found by the initial BFS
class TrieNodeV5 {
    TrieNodeV5[] children = new TrieNodeV5[5]; // 0..4 piece codes
    int minAbsolute_g = Integer.MAX_VALUE;
}

// --- ElementNode for BFS ---
// Used only during initialSolve
class ElementNodeBFSV5 {
    BoardState state;
    ElementNodeBFSV5 father; // Parent in BFS tree
    int moveCount;          // Moves from BFS start (relative depth)

    public ElementNodeBFSV5(BoardState state, ElementNodeBFSV5 father, int moveCount) {
        this.state = state;
        this.father = father;
        this.moveCount = moveCount;
    }
}

// --- ElementNode for A* ---
// Used only during runAStarSearch
class ElementNodeAStarV5 implements Comparable<ElementNodeAStarV5> {
    BoardState state;
    ElementNodeAStarV5 father; // Parent in A* search tree
    int g; // ABSOLUTE cost from original start 'O'
    int h; // Heuristic estimate
    int f; // f = g + h

    public ElementNodeAStarV5(BoardState s, ElementNodeAStarV5 fth, int absolute_g, int h) {
        this.state = s; this.father = fth; this.g = absolute_g; this.h = h; this.f = this.g + h;
    }
    @Override public int compareTo(ElementNodeAStarV5 o) { if (f != o.f) return Integer.compare(f, o.f); return Integer.compare(h, o.h); }
    @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; ElementNodeAStarV5 that = (ElementNodeAStarV5) o; return state.getLayout() == that.state.getLayout(); }
    @Override public int hashCode() { return state.hashCode(); }
}


public class KlotskiSolver { // Renamed class for clarity

    // ... (Keep constants and instance variables: persistentTrieRoot, optimalPath, etc.)
    // ... (Add initialSolveCompleted flag if not already present)
    private boolean initialSolveCompleted = false;
    private ElementNodeBFSV5 firstGoalNodeFound = null; // Store the first goal node

    // --- Constants ---
    private static final int[] DR = {-1, 1, 0, 0}; private static final int[] DC = {0, 0, -1, 1};
    private static final int ROWS = BoardSerializer.ROWS; private static final int COLS = BoardSerializer.COLS;
    private static final int BITS_PER_CELL = 3; private static final long CELL_MASK_3BIT = (1L << BITS_PER_CELL) - 1L;
    private static final int TOTAL_CELLS = ROWS * COLS;

    // --- Instance Variables ---
    private final TrieNodeV5 persistentTrieRoot = new TrieNodeV5(); // Stores global min_g from BFS
    private List<BoardState> optimalPath = null; // Stores the first optimal path found by BFS
    private long foundGoalCanonicalLayout = -1L;   // Stores the canonical hash of the goal found by BFS
    private int nodesExploredBFS = 0;
    private int nodesExploredAStar = 0;

    public int getNodesExploredBFS() { return nodesExploredBFS; }
    public int getNodesExploredAStar() { return nodesExploredAStar; }
    public int getNodesExploredTotal() { return nodesExploredBFS + nodesExploredAStar; } // Combined metric

    // --- Static Helper Methods (Ensure correctness and static nature) ---
    /**
     * Gets the 3-bit code for a cell directly from the layout long.
     * Returns -1 if coordinates are out of bounds.
     */
    private static long getCellCode(long layout, int r, int c) {
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return -1; // Invalid coordinate
        int shift = (r * COLS + c) * BITS_PER_CELL;
        return (layout >> shift) & CELL_MASK_3BIT;
    }

    /**
     * Calculates the symmetric layout (mirrored horizontally).
     */
    private static long getSymmetricLayout(long layout) {
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
     * Generates successor layouts - Uses BoardSerializer codes RIGOROUSLY.
     */
    private static long getCanonicalLayout(long layout) { return Math.min(layout, getSymmetricLayout(layout)); }
    private static List<Long> generateSuccessorLayouts(long currentLayout) { /* ... Same robust implementation ... */
        List<Long> successorLayouts = new ArrayList<>(); boolean[] processedCell = new boolean[TOTAL_CELLS];
        for (int r = 0; r < ROWS; r++) { for (int c = 0; c < COLS; c++) { int cellIndex = r * COLS + c; if (processedCell[cellIndex]) continue; long pieceCode = getCellCode(currentLayout, r, c); processedCell[cellIndex] = true; if (pieceCode == BoardSerializer.CODE_EMPTY) continue;
            List<int[]> pieceCellsCoords = new ArrayList<>(); pieceCellsCoords.add(new int[]{r, c});
            if (pieceCode == BoardSerializer.CODE_SOLDIER) {}
            else if (pieceCode == BoardSerializer.CODE_HORIZONTAL) { if (c + 1 < COLS && getCellCode(currentLayout, r, c + 1) == pieceCode) { pieceCellsCoords.add(new int[]{r, c + 1}); processedCell[cellIndex + 1] = true; } else continue; }
            else if (pieceCode == BoardSerializer.CODE_VERTICAL) { if (r + 1 < ROWS && getCellCode(currentLayout, r + 1, c) == pieceCode) { pieceCellsCoords.add(new int[]{r + 1, c}); processedCell[cellIndex + COLS] = true; } else continue; }
            else if (pieceCode == BoardSerializer.CODE_CAO_CAO) { boolean rOk = c + 1 < COLS && getCellCode(currentLayout, r, c + 1) == pieceCode; boolean bOk = r + 1 < ROWS && getCellCode(currentLayout, r + 1, c) == pieceCode; boolean brOk = c + 1 < COLS && r + 1 < ROWS && getCellCode(currentLayout, r + 1, c + 1) == pieceCode; if (rOk && bOk && brOk) { pieceCellsCoords.add(new int[]{r, c + 1}); processedCell[cellIndex + 1] = true; pieceCellsCoords.add(new int[]{r + 1, c}); processedCell[cellIndex + COLS] = true; pieceCellsCoords.add(new int[]{r + 1, c + 1}); processedCell[cellIndex + COLS + 1] = true; } else continue; } else { continue; }
            for (int dir = 0; dir < 4; dir++) { int dr = DR[dir], dc = DC[dir]; boolean canMove = true; List<int[]> targetCellsCoords = new ArrayList<>();
                for (int[] cellCoord : pieceCellsCoords) { int nr = cellCoord[0] + dr; int nc = cellCoord[1] + dc; if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS) { canMove = false; break; } targetCellsCoords.add(new int[]{nr, nc}); boolean targetIsOriginal = false; for (int[] originalCoord : pieceCellsCoords) { if (nr == originalCoord[0] && nc == originalCoord[1]) { targetIsOriginal = true; break; } } if (!targetIsOriginal && getCellCode(currentLayout, nr, nc) != BoardSerializer.CODE_EMPTY) { canMove = false; break; } }
                if (canMove) { long newLayout = currentLayout; long clearMask = 0L; long setMask = 0L; for (int[] cellCoord : pieceCellsCoords) { clearMask |= (CELL_MASK_3BIT << ((cellCoord[0] * COLS + cellCoord[1]) * BITS_PER_CELL)); } for (int[] targetCoord : targetCellsCoords) { setMask |= (pieceCode << ((targetCoord[0] * COLS + targetCoord[1]) * BITS_PER_CELL)); } newLayout = (newLayout & ~clearMask) | setMask; successorLayouts.add(newLayout); }
            } } } return successorLayouts;
    }
    private static int calculateHeuristic(long layout) { /* ... Same A* heuristic ... */
        for (int r = 0; r < ROWS - 1; r++) { for (int c = 0; c < COLS - 1; c++) { if (getCellCode(layout, r, c) == BoardSerializer.CODE_CAO_CAO && getCellCode(layout, r, c + 1) == BoardSerializer.CODE_CAO_CAO && getCellCode(layout, r + 1, c) == BoardSerializer.CODE_CAO_CAO && getCellCode(layout, r + 1, c + 1) == BoardSerializer.CODE_CAO_CAO) { int goalR = 3; int goalC = 1; return Math.abs(r - goalR) + Math.abs(c - goalC); } } } return Integer.MAX_VALUE / 2;
    }
    private static boolean isGoalLayout(long layout) { /* ... Same goal check ... */
        boolean cell17_ok = getCellCode(layout, 4, 1) == BoardSerializer.CODE_CAO_CAO; boolean cell18_ok = getCellCode(layout, 4, 2) == BoardSerializer.CODE_CAO_CAO; return cell17_ok && cell18_ok;
    }

    // --- Trie Helper Methods (V5 - Operate on TrieNodeV5) ---
    private TrieNodeV5 getOrCreateTrieNode(long canonicalLayout) {
        TrieNodeV5 current = persistentTrieRoot;
        for (int i = 0; i < TOTAL_CELLS; i++) {
            int shift = i * BITS_PER_CELL; int cellCode = (int)((canonicalLayout >> shift) & CELL_MASK_3BIT);
            if (cellCode < 0 || cellCode >= current.children.length) throw new ArrayIndexOutOfBoundsException("Invalid cell code "+cellCode);
            if (current.children[cellCode] == null) current.children[cellCode] = new TrieNodeV5();
            current = current.children[cellCode];
        }
        return current;
    }

    /** Updates Trie node ONLY if new_g is better (Used by BFS). */
    private boolean trieUpdate_g(long canonicalLayout, int newAbsolute_g) {
        TrieNodeV5 node = getOrCreateTrieNode(canonicalLayout);
        if (newAbsolute_g < node.minAbsolute_g) {
            node.minAbsolute_g = newAbsolute_g;
            return true;
        }
        return false;
    }

    /** Gets the minimum absolute g-cost from the Trie (Read-only). */
    private int trieGetMin_g(long canonicalLayout) {
        TrieNodeV5 current = persistentTrieRoot;
        for (int i = 0; i < TOTAL_CELLS; i++) {
            int shift = i * BITS_PER_CELL; int cellCode = (int)((canonicalLayout >> shift) & CELL_MASK_3BIT);
            // Need to handle cases where path doesn't exist during lookup
            if (current == null || cellCode < 0 || cellCode >= current.children.length || current.children[cellCode] == null) {
                return Integer.MAX_VALUE; // State not found in Trie
            }
            current = current.children[cellCode];
        }
        // If loop completes, current points to the node for the layout
        return current.minAbsolute_g;
    }


    // --- Path Reconstruction ---
    private List<BoardState> reconstructBFSPath(ElementNodeBFSV5 endNode) {
        LinkedList<BoardState> path = new LinkedList<>();
        ElementNodeBFSV5 trace = endNode;
        while (trace != null) {
            path.addFirst(trace.state);
            trace = trace.father;
        }
        return path;
    }

    private List<BoardState> reconstructAStarPath(ElementNodeAStarV5 endNode) {
        LinkedList<BoardState> path = new LinkedList<>();
        ElementNodeAStarV5 trace = endNode;
        while (trace != null) {
            path.addFirst(trace.state);
            trace = trace.father;
        }
        return path;
    }


    /**
     * Phase 1 (MODIFIED): Performs FULL BFS to find the globally optimal path
     * and populate the Trie with min_g values for ALL reachable states.
     */
    public List<BoardState> initialSolve(BoardState initialState) {
        if (initialSolveCompleted) {
            System.out.println("[initialSolve] Already completed. Returning cached optimal path.");
            // Ensure optimalPath is not null before returning
            return (this.optimalPath != null) ? this.optimalPath : Collections.emptyList();
        }
        System.out.println("[initialSolve] Starting FULL BFS (explores all reachable)...");
        this.nodesExploredBFS = 0;
        this.firstGoalNodeFound = null; // Reset goal node tracker
        long initialLayout = initialState.getLayout();
        long canonicalInitialLayout = getCanonicalLayout(initialLayout);

        Queue<ElementNodeBFSV5> queue = new ArrayDeque<>();
        ElementNodeBFSV5 initialElement = new ElementNodeBFSV5(initialState, null, 0);

        // Update Trie for the start state
        if (trieUpdate_g(canonicalInitialLayout, 0)) { // Check if update happened (first time)
            queue.offer(initialElement);
        } else {
            // This case should ideally not happen if called fresh, but handles re-entry possibility
            System.out.println("[initialSolve] Warning: Initial state already had a g-value in Trie.");
            if (trieGetMin_g(canonicalInitialLayout) == 0) {
                queue.offer(initialElement); // Still start the BFS
            } else {
                System.err.println("[initialSolve] Error: Initial state has non-zero g-value. Aborting.");
                this.initialSolveCompleted = true; // Mark as completed (failed)
                return Collections.emptyList();
            }
        }


        if (isGoalLayout(initialLayout)) {
            System.out.println("[initialSolve] Initial state is the goal.");
            this.nodesExploredBFS = 1;
            this.optimalPath = Collections.singletonList(initialState);
            this.foundGoalCanonicalLayout = canonicalInitialLayout;
            // No need to set firstGoalNodeFound here as the path is trivial
            this.initialSolveCompleted = true;
            return this.optimalPath;
        }


        while (!queue.isEmpty()) {
            ElementNodeBFSV5 currentNode = queue.poll();
            this.nodesExploredBFS++;
            long currentLayout = currentNode.state.getLayout();
            int currentDepth = currentNode.moveCount; // Absolute g = currentDepth

            List<Long> successorLayouts = generateSuccessorLayouts(currentLayout);

            for (long successorLayout : successorLayouts) {
                int new_g = currentDepth + 1;
                long canonicalSuccessorLayout = getCanonicalLayout(successorLayout);

                // Check if this state is globally better than known paths OR first time visiting
                if (new_g < trieGetMin_g(canonicalSuccessorLayout)) {
                    // Update Trie with the new best g
                    trieUpdate_g(canonicalSuccessorLayout, new_g);

                    BoardState successorState = new BoardState(successorLayout);
                    ElementNodeBFSV5 newElement = new ElementNodeBFSV5(successorState, currentNode, new_g);

                    // Check if it's a goal state
                    if (isGoalLayout(successorLayout)) {
                        // If this is the *first* time we found a goal, store this node
                        if (this.firstGoalNodeFound == null || new_g < this.firstGoalNodeFound.moveCount) {
                            System.out.println("[initialSolve] Goal found or improved at step " + new_g + "!");
                            this.firstGoalNodeFound = newElement;
                            this.foundGoalCanonicalLayout = canonicalSuccessorLayout; // Store canonical layout of goal
                        }
                        // *** DO NOT RETURN HERE - CONTINUE BFS ***
                    }

                    // Add to queue for further exploration (regardless of goal status)
                    queue.offer(newElement);
                }
                // If new_g >= known global best, prune this path (standard BFS optimization with Trie)
            }
        }

        // --- BFS Loop Finished ---
        System.out.println("[initialSolve] Full BFS completed exploration.");
        this.initialSolveCompleted = true; // Mark BFS as done

        if (this.firstGoalNodeFound != null) {
            System.out.println("[initialSolve] Reconstructing path from first goal found at step " + this.firstGoalNodeFound.moveCount);
            this.optimalPath = reconstructBFSPath(this.firstGoalNodeFound);
            return this.optimalPath;
        } else {
            System.err.println("[initialSolve] Full BFS completed without finding any goal!");
            this.optimalPath = Collections.emptyList();
            return this.optimalPath;
        }
    }

    // --- findPathFrom (No changes needed) ---
    public List<BoardState> findPathFrom(BoardState currentState) {
        // ... (Existing V5.0 logic remains the same)
        // It relies on initialSolve having been completed and the Trie being populated.
        if (!initialSolveCompleted) {
            System.out.println("[findPathFrom] Initial solve not completed. Running initialSolve first...");
            // IMPORTANT: Ensure initialSolve is called with the *actual* game start state,
            // not necessarily currentState if this is the very first call.
            // Assuming it was called correctly before, or handle error.
            // For this context, we assume initialSolve was run appropriately.
            if (this.optimalPath == null) { // Check if initial solve actually ran and succeeded
                System.err.println("[findPathFrom] Initial solve likely failed or wasn't run correctly. Cannot find path.");
                return Collections.emptyList();
            }
        }

        long currentLayout = currentState.getLayout();
        long canonicalCurrentLayout = getCanonicalLayout(currentLayout);

        // Phase 2/4 Check
        int optimalPathIndex = -1;
        if (this.optimalPath != null) {
            for (int i = this.optimalPath.size() - 1; i >= 0; i--) {
                if (getCanonicalLayout(this.optimalPath.get(i).getLayout()) == canonicalCurrentLayout) {
                    optimalPathIndex = i;
                    break;
                }
            }
        }

        if (optimalPathIndex != -1) {
            System.out.println("[findPathFrom] Cache Hit: Current state is on the optimal path at index " + optimalPathIndex + ".");
            return this.optimalPath.subList(optimalPathIndex, this.optimalPath.size());
        } else {
            // Phase 3
            System.out.println("[findPathFrom] Cache Miss: Current state not on optimal path. Running A*...");
            int start_g = trieGetMin_g(canonicalCurrentLayout);

            if (start_g == Integer.MAX_VALUE) {
                // This state was not reachable by the full BFS from the initial state.
                // This implies no path exists from the original start to this state,
                // OR there's an issue. A path to the goal from here is impossible
                // if we assume the initial state *could* reach the goal.
                System.err.println("[findPathFrom] Error: State " + canonicalCurrentLayout + " was not found in the completed BFS Trie. It should be unreachable or an error occurred.");
                return Collections.emptyList(); // Declare no solution from this state
            }
            // Proceed with A* using the accurate start_g from the full BFS
            return runAStarSearch(currentState, start_g);
        }
    }


    // --- runAStarSearch (No changes needed) ---
    private List<BoardState> runAStarSearch(BoardState startState, int start_g) {
        this.nodesExploredAStar = 0; // 重置计数器
        long startLayout = startState.getLayout();
        long canonicalStartLayout = getCanonicalLayout(startLayout);

        // 添加日志表明全局剪枝已禁用
        System.out.println("[runAStarSearch DEBUG] Starting A* for state " + canonicalStartLayout
                + " with start_g=" + start_g + " (GLOBAL PRUNING DISABLED)");

        if (isGoalLayout(startLayout)) {
            this.nodesExploredAStar = 1;
            return Collections.singletonList(startState);
        }

        PriorityQueue<ElementNodeAStarV5> openSet = new PriorityQueue<>();
        // visitedInThisSearch 仍然存储绝对 g 值
        Map<Long, Integer> visitedInThisSearch = new HashMap<>(); // 本地访问集

        int initialH = calculateHeuristic(startLayout);
        // 起始 g 值仍然使用来自 Trie 的全局最小值
        ElementNodeAStarV5 initialElement = new ElementNodeAStarV5(startState, null, start_g, initialH);

        openSet.add(initialElement);
        visitedInThisSearch.put(canonicalStartLayout, start_g);

        while (!openSet.isEmpty()) {
            ElementNodeAStarV5 currentNode = openSet.poll();
            this.nodesExploredAStar++;
            long currentLayout = currentNode.state.getLayout();
            long canonicalCurrentLayout = getCanonicalLayout(currentLayout);
            int currentAbsolute_g = currentNode.g; // 当前路径的绝对 g 值

            // 目标检查
            if (isGoalLayout(currentLayout)) {
                System.out.println("[runAStarSearch DEBUG] Goal found! (GLOBAL PRUNING DISABLED)");
                return reconstructAStarPath(currentNode);
            }

            // --- 出队剪枝 ---
            // 1. 本地剪枝 (保留)
            int visited_g = visitedInThisSearch.getOrDefault(canonicalCurrentLayout, Integer.MAX_VALUE);
            if (currentAbsolute_g > visited_g) {
                // System.out.println("[A* DEBUG Prune Dequeue LocalVisited] Node " + canonicalCurrentLayout + " (g=" + currentAbsolute_g + ") already visited with better g=" + visited_g);
                continue;
            }
            // 2. 全局剪枝 (在某些布局会出错）

            int globalMinG = trieGetMin_g(canonicalCurrentLayout);
            if (globalMinG != Integer.MAX_VALUE && currentAbsolute_g > globalMinG) {
                 // System.out.println("[A* DEBUG Prune Dequeue GlobalG] Node " + canonicalCurrentLayout + " (g=" + currentAbsolute_g + ") worse than global min_g=" + globalMinG);
                 continue; // 全局剪枝被注释掉
            }

            // --- 出队剪枝结束 ---

            List<Long> successorLayouts = generateSuccessorLayouts(currentLayout);

            for (long successorLayout : successorLayouts) {
                int newAbsolute_g = currentAbsolute_g + 1; // 新路径的绝对 g 值
                long canonicalSuccessorLayout = getCanonicalLayout(successorLayout);
                boolean pruned = false;

                // --- 入队剪枝 ---
                // 1. 本地剪枝 (保留, 使用标准 >=)
                int successor_visited_g = visitedInThisSearch.getOrDefault(canonicalSuccessorLayout, Integer.MAX_VALUE);
                if (newAbsolute_g >= successor_visited_g) {
                    // System.out.println("[A* DEBUG EnqueuePrune LocalVisited] Successor " + canonicalSuccessorLayout + " (new_g=" + newAbsolute_g + ") already visited better/equal g=" + successor_visited_g);
                    pruned = true;
                }

                // 2. 全局剪枝 (在某些布局会出错）

                int successorGlobalMinG = trieGetMin_g(canonicalSuccessorLayout);
                if (!pruned && successorGlobalMinG != Integer.MAX_VALUE && newAbsolute_g > successorGlobalMinG) {
                    // System.out.println("[A* DEBUG EnqueuePrune GlobalG] Successor " + canonicalSuccessorLayout + " (new_g=" + newAbsolute_g + ") worse than global min_g=" + successorGlobalMinG);
                    pruned = true;
                }

                // --- 入队剪枝结束 ---

                if (!pruned) {
                    int h_new = calculateHeuristic(successorLayout);
                    BoardState successorState = new BoardState(successorLayout);
                    ElementNodeAStarV5 newElement = new ElementNodeAStarV5(successorState, currentNode, newAbsolute_g, h_new);

                    // 更新本地访问集
                    visitedInThisSearch.put(canonicalSuccessorLayout, newAbsolute_g);
                    openSet.add(newElement);
                }
            }
        }

        // 如果循环结束仍未找到目标
        System.err.println("[runAStarSearch] A* completed without finding goal from state " + canonicalStartLayout
                + " (start_g=" + start_g + ") (GLOBAL PRUNING DISABLED)");
        return Collections.emptyList();
    }


    // ... (Keep main method and test sequence runner - update class name if needed)
    public static void main(String[] args) {
        System.out.println("Klotski Solver V5.0 (Full BFS Hybrid) Test");
        try {
            int[][] initialArray = new int[][]{
                    {BoardSerializer.EMPTY, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.EMPTY},
                    {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.SOLDIER},
                    {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.SOLDIER}

            };
            long initialLayout = BoardSerializer.serialize(initialArray);
            BoardState initialState = new BoardState(initialLayout);
            System.out.println("Initial State (Layout: " + initialState.getLayout() + "):");
            BoardSerializer.printBoard(initialState.getBoardArray());

            KlotskiSolver solver = new KlotskiSolver(); // Use Full BFS version
            runTestSequence(solver, initialState, "Layout1_FullBFS"); // Pass the new solver instance
        } catch (Exception e) { System.err.println("An error occurred in main:"); e.printStackTrace(); }
        System.out.println("\n\n===== Test Complete =====");
    }

    // Modify runTestSequence to accept the correct solver type
    private static void runTestSequence(KlotskiSolver solver, BoardState initialState, String layoutName) {
        // ... (The rest of the test sequence logic can remain largely the same)
        // Just ensure it uses the passed 'solver' object of type KlotskiSolver
        List<BoardState> initialSolution = null;
        BoardState intermediateStateOptimal = null;
        BoardState nonOptimalState = null;
        int stepForGuidanceOptimal = 3;

        try {
            // Phase 1
            System.out.println("\n[" + layoutName + "] Phase 1: Initial FULL BFS Solve ---");
            long startTime = System.currentTimeMillis();
            initialSolution = solver.initialSolve(initialState); // Call the modified initialSolve
            long endTime = System.currentTimeMillis();
            System.out.println("[" + layoutName + "] Initial FULL BFS finished in " + (endTime - startTime) + " ms.");
            System.out.println("[" + layoutName + "] Initial FULL BFS explored nodes: " + solver.getNodesExploredBFS());
            if (initialSolution == null || initialSolution.isEmpty()) {
                System.out.println("[" + layoutName + "] Initial FULL BFS found no solution.");
                return;
            } else {
                System.out.println("[" + layoutName + "] Initial solution found with " + (initialSolution.size() - 1) + " steps.");
            }

            // Phase 2
            System.out.println("\n[" + layoutName + "] Phase 2: findPathFrom Optimal Intermediate (step " + stepForGuidanceOptimal + ") ---");
            if (initialSolution.size() > stepForGuidanceOptimal) {
                intermediateStateOptimal = initialSolution.get(stepForGuidanceOptimal);
                System.out.println("[" + layoutName + "] Finding path from state after " + stepForGuidanceOptimal + " optimal steps (Layout: " + intermediateStateOptimal.getLayout() + ")");
                startTime = System.currentTimeMillis();
                List<BoardState> pathFromOptimal = solver.findPathFrom(intermediateStateOptimal);
                endTime = System.currentTimeMillis();
                int phase2AStarNodes = solver.nodesExploredAStar; solver.nodesExploredAStar = 0;
                System.out.println("[" + layoutName + "] Guidance (Optimal) finished in " + (endTime - startTime) + " ms.");
                System.out.println("[" + layoutName + "] Guidance (Optimal) BFS nodes: " + solver.getNodesExploredBFS() + ", A* nodes: " + phase2AStarNodes);

                if(pathFromOptimal.isEmpty()) System.err.println("[" + layoutName + "] >>>>> FAILURE: No path found from optimal intermediate!");
                else { /* ... validation ... */ }
            } else { System.out.println("[" + layoutName + "] Skipping Phase 2 (Initial solution too short)."); }


            // Phase 3
            System.out.println("\n[" + layoutName + "] Phase 3: findPathFrom Non-Optimal Intermediate ---");
            // Find a non-optimal successor from initial state (same logic as before)
            List<Long> successors = generateSuccessorLayouts(initialState.getLayout());
            if (!successors.isEmpty() && initialSolution.size() > 1) {
                long firstOptimalStepLayout = initialSolution.get(1).getLayout(); long chosenLayout = -1;
                for (long succLayout : successors) { if (getCanonicalLayout(succLayout) != getCanonicalLayout(firstOptimalStepLayout)) { chosenLayout = succLayout; break; } }
                if (chosenLayout == -1 && !successors.isEmpty()) { chosenLayout = successors.get(0); }
                // --- Add this for debugging Phase 3 ---

                long layoutForPhase3 = -1;
                if (chosenLayout != -1) {
                    layoutForPhase3 = chosenLayout;
                    BoardState stateToDebug = new BoardState(layoutForPhase3);
                    System.out.println("\nDEBUG: Board layout for Phase 3 start (Layout: " + layoutForPhase3 + ", Canonical: " + getCanonicalLayout(layoutForPhase3) + ")");
                    try {
                        BoardSerializer.printBoard(stateToDebug.getBoardArray());
                    } catch (Exception e) { System.out.println("Error printing board: " + e.getMessage());}
                }


                if (layoutForPhase3  != -1) {

                    nonOptimalState = new BoardState(layoutForPhase3);
                    System.out.println("[" + layoutName + "] Finding path from potentially non-optimal state after 1 move (Layout: " + nonOptimalState.getLayout() + ")");
                    startTime = System.currentTimeMillis();
                    List<BoardState> pathFromNonOptimal = solver.findPathFrom(nonOptimalState); // Should run A*
                    endTime = System.currentTimeMillis();
                    int phase3AStarNodes = solver.nodesExploredAStar; solver.nodesExploredAStar = 0;
                    System.out.println("[" + layoutName + "] Guidance (Non-Optimal) finished in " + (endTime - startTime) + " ms.");
                    System.out.println("[" + layoutName + "] Guidance (Non-Optimal) BFS nodes: " + solver.getNodesExploredBFS() + ", A* nodes: " + phase3AStarNodes);

                    if(pathFromNonOptimal.isEmpty()) System.err.println("[" + layoutName + "] >>>>> FAILURE: No path found from non-optimal intermediate!");
                    else { /* ... validation ... */ }
                } else { System.out.println("[" + layoutName + "] Skipping Phase 3 (Could not find suitable non-optimal successor)."); }
            } else { System.out.println("[" + layoutName + "] Skipping Phase 3 (No successors or initial solution too short)."); }


            // Phase 4
            System.out.println("\n[" + layoutName + "] Phase 4: findPathFrom Initial State (Re-Solve) ---");
            startTime = System.currentTimeMillis();
            List<BoardState> reSolveSolution = solver.findPathFrom(initialState);
            endTime = System.currentTimeMillis();
            int phase4AStarNodes = solver.nodesExploredAStar; solver.nodesExploredAStar = 0;
            System.out.println("[" + layoutName + "] Re-solve finished in " + (endTime - startTime) + " ms.");
            System.out.println("[" + layoutName + "] Re-solve BFS nodes: " + solver.getNodesExploredBFS() + ", A* nodes: " + phase4AStarNodes);

            if (reSolveSolution.isEmpty()) { System.err.println("[" + layoutName + "] >>>>> FAILURE: Re-solve failed to find a solution!"); }
            else { /* ... validation ... */ }

        } catch (Exception e) { System.err.println("\n[" + layoutName + "] An error occurred during test sequence:"); e.printStackTrace(); }
    }

}