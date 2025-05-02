package controller.solver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import controller.util.BoardSerializer;

// --- TrieNode for V5 ---
// Stores only the minimum absolute g-cost found by the initial BFS
class TrieNode {

    TrieNode[] children = new TrieNode[5]; // 0..4 piece codes
    int minAbsolute_g = Integer.MAX_VALUE;
}

// --- ElementNode for BFS ---
class ElementNodeBFS {

    BoardState state;
    ElementNodeBFS father; // Parent in BFS tree
    int moveCount;          // Moves from BFS start (relative depth)

    public ElementNodeBFS(BoardState state, ElementNodeBFS father, int moveCount) {
        this.state = state;
        this.father = father;
        this.moveCount = moveCount;
    }
}

// --- ElementNode for A* ---
class ElementNodeAStar implements Comparable<ElementNodeAStar> {

    BoardState state;
    ElementNodeAStar father;
    int g_local; // Cost from Phase 3 start state 'S'
    int h;       // Enhanced heuristic value
    int f;       // f = g_local + h

    public ElementNodeAStar(BoardState s, ElementNodeAStar fth, int g_local, int h) {
        this.state = s;
        this.father = fth;
        this.g_local = g_local;
        this.h = h;
        this.f = this.g_local + h;
    }

    @Override
    public int compareTo(ElementNodeAStar o) {
        if (f != o.f) {
            return Integer.compare(f, o.f);
        }
        return Integer.compare(h, o.h); // Tie-breaking using heuristic
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ElementNodeAStar that = (ElementNodeAStar) o;
        // Equality based on the board state's layout's canonical form
        // Assumes BoardState.getLayout() exists and getCanonicalLayout is static/accessible
        return KlotskiSolver.getCanonicalLayout(state.getLayout()) == KlotskiSolver.getCanonicalLayout(that.state.getLayout());
    }

    @Override
    public int hashCode() {
        // Hash code based on the board state's layout's canonical form
        // Assumes BoardState.getLayout() exists and getCanonicalLayout is static/accessible
        return Long.hashCode(KlotskiSolver.getCanonicalLayout(state.getLayout()));
    }
}

public class KlotskiSolver {

    // ... (Add initialSolveCompleted flag if not already present)
    private boolean initialSolveCompleted = false;
    private ElementNodeBFS firstGoalNodeFound = null; // Store the first goal node

    // --- Constants ---
    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};
    private static final int ROWS = BoardSerializer.ROWS;
    private static final int COLS = BoardSerializer.COLS;
    private static final int BITS_PER_CELL = 3;
    private static final long CELL_MASK_3BIT = (1L << BITS_PER_CELL) - 1L;
    private static final int TOTAL_CELLS = ROWS * COLS;

    // --- Instance Variables ---
    private final TrieNode persistentTrieRoot = new TrieNode(); // Stores global min_g from BFS
    private List<BoardState> optimalPath = null; // Stores the first optimal path found by BFS
    private int nodesExploredBFS = 0;
    private int nodesExploredAStar = 0;

    public int getNodesExploredBFS() {
        return nodesExploredBFS;
    }

    public int getNodesExploredAStar() {
        return nodesExploredAStar;
    }
    private int globalOptimalCost = Integer.MAX_VALUE; // Store the global optimal cost

    public int getNodesExploredTotal() {
        return nodesExploredBFS + nodesExploredAStar;
    } // Combined metric
    // --- Static Helper Methods (Ensure correctness and static nature) ---

    /**
     * Gets the 3-bit code for a cell directly from the layout long. Returns -1
     * if coordinates are out of bounds.
     */
    private static long getCellCode(long layout, int r, int c) {
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS) {
            return -1; // Invalid coordinate

        }
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
    static long getCanonicalLayout(long layout) {
        return Math.min(layout, getSymmetricLayout(layout));
    }

    private static List<Long> generateSuccessorLayouts(long currentLayout) {
        /* ... Same robust implementation ... */
        List<Long> successorLayouts = new ArrayList<>();
        boolean[] processedCell = new boolean[TOTAL_CELLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int cellIndex = r * COLS + c;
                if (processedCell[cellIndex]) {
                    continue;

                }
                long pieceCode = getCellCode(currentLayout, r, c);
                processedCell[cellIndex] = true;
                if (pieceCode == BoardSerializer.CODE_EMPTY) {
                    continue;
                }
                List<int[]> pieceCellsCoords = new ArrayList<>();
                pieceCellsCoords.add(new int[]{r, c});
                if (pieceCode == BoardSerializer.CODE_SOLDIER) {
                } else if (pieceCode == BoardSerializer.CODE_HORIZONTAL) {
                    if (c + 1 < COLS && getCellCode(currentLayout, r, c + 1) == pieceCode) {
                        pieceCellsCoords.add(new int[]{r, c + 1});
                        processedCell[cellIndex + 1] = true;
                    } else {
                        continue;

                    }
                } else if (pieceCode == BoardSerializer.CODE_VERTICAL) {
                    if (r + 1 < ROWS && getCellCode(currentLayout, r + 1, c) == pieceCode) {
                        pieceCellsCoords.add(new int[]{r + 1, c});
                        processedCell[cellIndex + COLS] = true;
                    } else {
                        continue;

                    }
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
                    } else {
                        continue;

                    }
                } else {
                    continue;
                }
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
     * Calculates an enhanced heuristic value for A*. h(I) = max(Manhattan(I),
     * globalOptimalCost - trieGetMin_g(I)) Handles cases where global info is
     * unavailable. 1. 利用全局最优路径信息: 我们知道从起点 O 到状态 I 的全局最短成本是 g_global(I) =
     * trieGetMin_g(I) 我们知道从起点 O 到目标 G 的全局最短成本是 g_global(G) = globalOptimalCost
     * 如果状态 I 位于某条从 O 到 G 的全局最优路径上，那么理论上 g_global(I) + h*(I) = g_global(G)，其中
     * h*(I) 是从 I 到 G 的真实最短成本 因此，h*(I) = g_global(G) - g_global(I)。 因此， h*(I) =
     * g_global(G) - g_global(I) 虽然状态 I 不一定在全局最优路径上，但 g_global(I) + h*(I) 总是大于等于
     * g_global(G)（因为 g_global(I) + h*(I) 是某条 O->G 路径的成本，而 g_global(G) 是最短成本）
     * 所以，h*(I) >= g_global(G) - g_global(I)，这意味着 diff = g_global(G) -
     * g_global(I) 是真实剩余成本 h*(I) 的一个下界（当 diff >= 0 时） 2. 定义增强启发式 h_enhanced(I):
     * 我们可以结合现有的启发式和这个新的下界： h_enhanced(I) = max(h_manhattan(I),
     * globalOptimalCost - trieGetMin_g(I)) 重要: 这个计算只在 globalOptimalCost 有效（不是
     * MAX_VALUE）且 trieGetMin_g(I) 有效（不是 MAX_VALUE）并且 globalOptimalCost >=
     * trieGetMin_g(I) 时才有意义 在其他情况下，我们应该只使用 h_manhattan(I) 3. 保持 A* 结构正确: A*
     * 搜索本身仍然使用局部 g 值 (g_local)，从 Phase 3 的起始状态 S 开始计数（g_local(S) = 0） 优先级队列使用 f
     * = g_local + h_enhanced(I) 只使用标准的本地剪枝：基于 visitedInThisSearch 中的 g_local
     * (new_g_local >= visited_g_local) 不使用任何基于绝对 g 值或全局成本的额外剪枝规则
     */
    private int calculateHeuristic(long layout) {
        int h_manhattan = calculateManhattanHeuristic(layout); // Assuming this exists or implement it

        int h_diff = 0; // Default value for the difference part
        if (this.globalOptimalCost != Integer.MAX_VALUE) {
            long canonicalLayout = getCanonicalLayout(layout);
            int g_global_I = trieGetMin_g(canonicalLayout);

            if (g_global_I != Integer.MAX_VALUE && this.globalOptimalCost >= g_global_I) {
                h_diff = this.globalOptimalCost - g_global_I;
            }
            // If g_global_I is MAX_VALUE or globalOptimalCost < g_global_I, h_diff remains 0
            // or could be set to a negative value, max will handle it.
        }

        return Math.max(h_manhattan, h_diff);
    }

    // --- Helper for basic Manhattan distance (or your existing heuristic) ---
    private int calculateManhattanHeuristic(long layout) {
        // Find top-left of CaoCao
        for (int r = 0; r < ROWS - 1; r++) {
            for (int c = 0; c < COLS - 1; c++) {
                if (getCellCode(layout, r, c) == BoardSerializer.CODE_CAO_CAO
                        && getCellCode(layout, r, c + 1) == BoardSerializer.CODE_CAO_CAO
                        && getCellCode(layout, r + 1, c) == BoardSerializer.CODE_CAO_CAO
                        && getCellCode(layout, r + 1, c + 1) == BoardSerializer.CODE_CAO_CAO) {
                    int goalR = 3;
                    int goalC = 1; // Target for top-left corner
                    return Math.abs(r - goalR) + Math.abs(c - goalC);
                }
            }
        }
        // Should not happen in valid states reachable from start if goal is reachable
        return Integer.MAX_VALUE / 2; // High cost if CaoCao not found
    }

    private static boolean isGoalLayout(long layout) {
        /* ... Same goal check ... */
        boolean cell17_ok = getCellCode(layout, 4, 1) == BoardSerializer.CODE_CAO_CAO;
        boolean cell18_ok = getCellCode(layout, 4, 2) == BoardSerializer.CODE_CAO_CAO;
        return cell17_ok && cell18_ok;
    }
    // --- Trie Helper Methods (V5 - Operate on TrieNodeV5) ---

    private TrieNode getOrCreateTrieNode(long canonicalLayout) {
        TrieNode current = persistentTrieRoot;
        for (int i = 0; i < TOTAL_CELLS; i++) {
            int shift = i * BITS_PER_CELL;
            int cellCode = (int) ((canonicalLayout >> shift) & CELL_MASK_3BIT);
            if (cellCode < 0 || cellCode >= current.children.length) {
                throw new ArrayIndexOutOfBoundsException("Invalid cell code " + cellCode);
            }
            if (current.children[cellCode] == null) {
                current.children[cellCode] = new TrieNode();
            }
            current = current.children[cellCode];
        }
        return current;
    }

    /**
     * Updates Trie node ONLY if new_g is better (Used by BFS).
     */
    private boolean trieUpdate_g(long canonicalLayout, int newAbsolute_g) {
        TrieNode node = getOrCreateTrieNode(canonicalLayout);
        if (newAbsolute_g < node.minAbsolute_g) {
            node.minAbsolute_g = newAbsolute_g;
            return true;
        }
        return false;
    }

    /**
     * Gets the minimum absolute g-cost from the Trie (Read-only).
     */
    private int trieGetMin_g(long canonicalLayout) {
        TrieNode current = persistentTrieRoot;
        for (int i = 0; i < TOTAL_CELLS; i++) {
            int shift = i * BITS_PER_CELL;
            int cellCode = (int) ((canonicalLayout >> shift) & CELL_MASK_3BIT);
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
    private List<BoardState> reconstructBFSPath(ElementNodeBFS endNode) {
        LinkedList<BoardState> path = new LinkedList<>();
        ElementNodeBFS trace = endNode;
        while (trace != null) {
            path.addFirst(trace.state);
            trace = trace.father;
        }
        return path;
    }

    private List<BoardState> reconstructAStarPath(ElementNodeAStar endNode) {
        LinkedList<BoardState> path = new LinkedList<>();
        ElementNodeAStar trace = endNode;
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

        Queue<ElementNodeBFS> queue = new ArrayDeque<>();
        ElementNodeBFS initialElement = new ElementNodeBFS(initialState, null, 0);

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
            // No need to set firstGoalNodeFound here as the path is trivial
            this.initialSolveCompleted = true;
            return this.optimalPath;
        }

        while (!queue.isEmpty()) {
            ElementNodeBFS currentNode = queue.poll();
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
                    ElementNodeBFS newElement = new ElementNodeBFS(successorState, currentNode, new_g);

                    // Check if it's a goal state
                    if (isGoalLayout(successorLayout)) {
                        // If this is the *first* time we found a goal, store this node
                        if (this.firstGoalNodeFound == null || new_g < this.firstGoalNodeFound.moveCount) {
                            System.out.println("[initialSolve] Goal found or improved at step " + new_g + "!");
                            this.firstGoalNodeFound = newElement;
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
        this.initialSolveCompleted = true;

        if (this.firstGoalNodeFound != null) {
            System.out.println("[initialSolve] Reconstructing path from first goal found at step " + this.firstGoalNodeFound.moveCount);
            this.optimalPath = reconstructBFSPath(this.firstGoalNodeFound);
            this.globalOptimalCost = this.firstGoalNodeFound.moveCount; // *** STORE GLOBAL OPTIMAL COST ***
            return this.optimalPath;
        } else {
            System.err.println("[initialSolve] Full BFS completed without finding any goal!");
            this.optimalPath = Collections.emptyList();
            this.globalOptimalCost = Integer.MAX_VALUE; // Indicate no solution found
            return this.optimalPath;
        }
    }

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
            //System.out.println("[findPathFrom] Cache Miss: Current state not on optimal path. Running A*...");
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

    /**
     * Runs a standard A* search using an ENHANCED heuristic informed by global
     * data. Uses local 'g' and standard local pruning. Global info influences
     * 'h' only.
     */
    private List<BoardState> runAStarSearch(BoardState startState, int start_g_abs) { // start_g_abs is informational
        this.nodesExploredAStar = 0;
        long startLayout = startState.getLayout();
        long canonicalStartLayout = getCanonicalLayout(startLayout);

        System.out.println("[runAStarSearch EnhancedH] Starting A* for state " + canonicalStartLayout + " (origin_abs_g=" + start_g_abs + ")");

        if (isGoalLayout(startLayout)) {
            this.nodesExploredAStar = 1;
            //System.out.println("[runAStarSearch EnhancedH] Start state is already goal.");
            return Collections.singletonList(startState);
        }

        PriorityQueue<ElementNodeAStar> openSet = new PriorityQueue<>();
        Map<Long, Integer> visitedInThisSearch = new HashMap<>(); // Stores min LOCAL g

        // *** Uses the ENHANCED heuristic ***
        int initialH = calculateHeuristic(startLayout);
        ElementNodeAStar initialElement = new ElementNodeAStar(startState, null, 0, initialH); // local g = 0

        openSet.add(initialElement);
        visitedInThisSearch.put(canonicalStartLayout, 0);

        while (!openSet.isEmpty()) {
            ElementNodeAStar currentNode = openSet.poll();
            this.nodesExploredAStar++;
            long currentLayout = currentNode.state.getLayout();
            long canonicalCurrentLayout = getCanonicalLayout(currentLayout);
            int current_g_local = currentNode.g_local;

            // Goal Check
            if (isGoalLayout(currentLayout)) {
                System.out.println("[runAStarSearch RelativeG] Goal found!");
                return reconstructAStarPath(currentNode); // Path reconstruction uses father pointers
            }

            // --- Local Dequeue Pruning (based on local g) ---
            int visited_g_local = visitedInThisSearch.getOrDefault(canonicalCurrentLayout, Integer.MAX_VALUE);
            if (current_g_local > visited_g_local) {
                continue;
            }
            // --- NO GLOBAL PRUNING ---

            List<Long> successorLayouts = generateSuccessorLayouts(currentLayout);

            for (long successorLayout : successorLayouts) {
                int new_g_local = current_g_local + 1;
                long canonicalSuccessorLayout = getCanonicalLayout(successorLayout);
                boolean pruned = false;

                // --- Local Enqueue Pruning (based on local g, standard >=) ---
                int successor_visited_g_local = visitedInThisSearch.getOrDefault(canonicalSuccessorLayout, Integer.MAX_VALUE);
                if (new_g_local >= successor_visited_g_local) {
                    pruned = true;
                }
                // --- NO GLOBAL PRUNING ---

                if (!pruned) {
                    // *** Uses the ENHANCED heuristic ***
                    int h_new = calculateHeuristic(successorLayout);
                    BoardState successorState = new BoardState(successorLayout);
                    ElementNodeAStar newElement = new ElementNodeAStar(successorState, currentNode, new_g_local, h_new);

                    visitedInThisSearch.put(canonicalSuccessorLayout, new_g_local);
                    openSet.add(newElement);
                }
            }
        }

        // If loop finishes without finding the goal
        System.err.println("[runAStarSearch RelativeG] A* completed without finding goal from state " + canonicalStartLayout
                + " (abs_g=" + start_g_abs + ")");
        return Collections.emptyList();
    }

    // ... (Keep main method and test sequence runner - update class name if needed)
    public static void main(String[] args) {
        System.out.println("Klotski Solver V5.0 (Full BFS Hybrid) Test");
        try {
            int[][] initialArray = new int[][]{
                {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                {BoardSerializer.SOLDIER, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.SOLDIER},
                {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER}
            };
            long initialLayout = BoardSerializer.serialize(initialArray);
            BoardState initialState = new BoardState(initialLayout);
            System.out.println("Initial State (Layout: " + initialState.getLayout() + "):");
            BoardSerializer.printBoard(initialState.getBoardArray());

            KlotskiSolver solver = new KlotskiSolver(); // Use Full BFS version
            runTestSequence(solver, initialState, "Layout1_FullBFS"); // Pass the new solver instance
        } catch (Exception e) {
            System.err.println("An error occurred in main:");
            e.printStackTrace();
        }
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
                int phase2AStarNodes = solver.nodesExploredAStar;
                solver.nodesExploredAStar = 0;
                System.out.println("[" + layoutName + "] Guidance (Optimal) finished in " + (endTime - startTime) + " ms.");
                System.out.println("[" + layoutName + "] Guidance (Optimal) BFS nodes: " + solver.getNodesExploredBFS() + ", A* nodes: " + phase2AStarNodes);

                if (pathFromOptimal.isEmpty()) {
                    System.err.println("[" + layoutName + "] >>>>> FAILURE: No path found from optimal intermediate!");
                } else {
                    /* ... validation ... */ }
            } else {
                System.out.println("[" + layoutName + "] Skipping Phase 2 (Initial solution too short).");
            }

            // Phase 3
            System.out.println("\n[" + layoutName + "] Phase 3: findPathFrom Non-Optimal Intermediate ---");
            // Find a non-optimal successor from initial state (same logic as before)
            List<Long> successors = generateSuccessorLayouts(initialState.getLayout());
            if (!successors.isEmpty() && initialSolution.size() > 1) {
                long firstOptimalStepLayout = initialSolution.get(1).getLayout();
                long chosenLayout = -1;
                for (long succLayout : successors) {
                    if (getCanonicalLayout(succLayout) != getCanonicalLayout(firstOptimalStepLayout)) {
                        chosenLayout = succLayout;
                        break;
                    }
                }
                if (chosenLayout == -1 && !successors.isEmpty()) {
                    chosenLayout = successors.get(0);
                }
                // --- Add this for debugging Phase 3 ---

                long layoutForPhase3 = -1;
                if (chosenLayout != -1) {
                    layoutForPhase3 = chosenLayout;
                    BoardState stateToDebug = new BoardState(layoutForPhase3);
                    //System.out.println("\nDEBUG: Board layout for Phase 3 start (Layout: " + layoutForPhase3 + ", Canonical: " + getCanonicalLayout(layoutForPhase3) + ")");
                    try {
                        BoardSerializer.printBoard(stateToDebug.getBoardArray());
                    } catch (Exception e) {
                        System.out.println("Error printing board: " + e.getMessage());
                    }
                }

                if (layoutForPhase3 != -1) {

                    nonOptimalState = new BoardState(layoutForPhase3);
                    System.out.println("[" + layoutName + "] Finding path from potentially non-optimal state after 1 move (Layout: " + nonOptimalState.getLayout() + ")");
                    startTime = System.currentTimeMillis();
                    List<BoardState> pathFromNonOptimal = solver.findPathFrom(nonOptimalState); // Should run A*
                    endTime = System.currentTimeMillis();
                    int phase3AStarNodes = solver.nodesExploredAStar;
                    solver.nodesExploredAStar = 0;
                    System.out.println("[" + layoutName + "] Guidance (Non-Optimal) finished in " + (endTime - startTime) + " ms.");
                    System.out.println("[" + layoutName + "] Guidance (Non-Optimal) BFS nodes: " + solver.getNodesExploredBFS() + ", A* nodes: " + phase3AStarNodes);

                    if (pathFromNonOptimal.isEmpty()) {
                        System.err.println("[" + layoutName + "] >>>>> FAILURE: No path found from non-optimal intermediate!");
                    } else {
                        /* ... validation ... */ }
                } else {
                    System.out.println("[" + layoutName + "] Skipping Phase 3 (Could not find suitable non-optimal successor).");
                }
            } else {
                System.out.println("[" + layoutName + "] Skipping Phase 3 (No successors or initial solution too short).");
            }

            // Phase 4
            System.out.println("\n[" + layoutName + "] Phase 4: findPathFrom Initial State (Re-Solve) ---");
            startTime = System.currentTimeMillis();
            List<BoardState> reSolveSolution = solver.findPathFrom(initialState);
            endTime = System.currentTimeMillis();
            int phase4AStarNodes = solver.nodesExploredAStar;
            solver.nodesExploredAStar = 0;
            System.out.println("[" + layoutName + "] Re-solve finished in " + (endTime - startTime) + " ms.");
            System.out.println("[" + layoutName + "] Re-solve BFS nodes: " + solver.getNodesExploredBFS() + ", A* nodes: " + phase4AStarNodes);

            if (reSolveSolution.isEmpty()) {
                System.err.println("[" + layoutName + "] >>>>> FAILURE: Re-solve failed to find a solution!");
            } else {
                /* ... validation ... */ }

        } catch (Exception e) {
            System.err.println("\n[" + layoutName + "] An error occurred during test sequence:");
            e.printStackTrace();
        }
    }

}
