package controller.solver;

import java.util.*;

// --- TrieNode definition (Unchanged) ---
class TrieNode {
    TrieNode[] children = new TrieNode[5];
    int minAbsolute_g = Integer.MAX_VALUE;
    long parentCanonicalLayout = -1L;
}

// --- ElementNode definition (Unchanged) ---
class ElementNode implements Comparable<ElementNode> {
    BoardState state; ElementNode father; int g; int h; int f;
    public ElementNode(BoardState s, ElementNode fth, int absolute_g, int h) { this.state = s; this.father = fth; this.g = absolute_g; this.h = h; this.f = this.g + h; }
    @Override public int compareTo(ElementNode o) { if (f != o.f) return Integer.compare(f, o.f); return Integer.compare(h, o.h); }
    @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; ElementNode that = (ElementNode) o; return state.getLayout() == that.state.getLayout(); }
    @Override public int hashCode() { return state.hashCode(); }
}

/**
 * V4.3.3: A* Solver using Trie - Final Cleanup.
 * - Uses Trie for persistence (min_g, parent).
 * - Core A* uses only Dequeue Global Pruning (>) and Local Pruning (>=).
 * - NO Enqueue Global Pruning.
 * - Fast path reconstruction optimization ONLY applies when start_g == 0.
 */
public class KlotskiSolver {

    // --- Constants (Unchanged) ---
    private static final int[] DR = {-1, 1, 0, 0}; private static final int[] DC = {0, 0, -1, 1};
    private static final int ROWS = BoardSerializer.ROWS; private static final int COLS = BoardSerializer.COLS;
    private static final int BITS_PER_CELL = 3; private static final long CELL_MASK_3BIT = (1L << BITS_PER_CELL) - 1L;
    private static final int TOTAL_CELLS = ROWS * COLS;

    // --- Instance Variables ---
    private int nodesExplored = 0;
    private final TrieNode persistentTrieRoot;
    private long foundGoalCanonicalLayout = -1L; // Store the canonical hash of the first found goal

    public KlotskiSolver() { this.persistentTrieRoot = new TrieNode(); }
    public void setFoundGoalLayout(long layout) { this.foundGoalCanonicalLayout = getCanonicalLayout(layout); }
    public int getNodesExplored() { return nodesExplored; }

    // --- Static Helper Methods (Unchanged) ---
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
    private static List<Long> generateSuccessorLayouts(long currentLayout) { /* ... V4.3.1 implementation ... */
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
    private static int calculateHeuristic(long layout) { /* ... V4.3.1 ... */
        for (int r = 0; r < ROWS - 1; r++) { for (int c = 0; c < COLS - 1; c++) { if (getCellCode(layout, r, c) == BoardSerializer.CODE_CAO_CAO && getCellCode(layout, r, c + 1) == BoardSerializer.CODE_CAO_CAO && getCellCode(layout, r + 1, c) == BoardSerializer.CODE_CAO_CAO && getCellCode(layout, r + 1, c + 1) == BoardSerializer.CODE_CAO_CAO) { int goalR = 3; int goalC = 1; return Math.abs(r - goalR) + Math.abs(c - goalC); } } } return Integer.MAX_VALUE / 2;
    }
    private boolean isGoalLayout(long layout) { /* ... V4.3.1 ... */
        boolean cell17_ok = getCellCode(layout, 4, 1) == BoardSerializer.CODE_CAO_CAO; boolean cell18_ok = getCellCode(layout, 4, 2) == BoardSerializer.CODE_CAO_CAO; return cell17_ok && cell18_ok;
    }

    // --- Trie Helper Methods (Unchanged) ---
    private TrieNode getOrCreateTrieNode(long canonicalLayout) { /* ... V4.3.1 ... */ TrieNode current = persistentTrieRoot; for (int i = 0; i < TOTAL_CELLS; i++) { int shift = i * BITS_PER_CELL; int cellCode = (int)((canonicalLayout >> shift) & CELL_MASK_3BIT); if (cellCode < 0 || cellCode >= current.children.length) throw new ArrayIndexOutOfBoundsException("Invalid cell code "+cellCode); if (current.children[cellCode] == null) current.children[cellCode] = new TrieNode(); current = current.children[cellCode]; } return current; }
    private boolean trieUpdate(long canonicalLayout, int newAbsolute_g, long parentCanonicalLayout) { /* ... V4.3.1 ... */ TrieNode node = getOrCreateTrieNode(canonicalLayout); if (newAbsolute_g < node.minAbsolute_g) { node.minAbsolute_g = newAbsolute_g; node.parentCanonicalLayout = parentCanonicalLayout; return true; } return false; }
    private int trieGetMin_g(long canonicalLayout) { /* ... V4.3.1 ... */ TrieNode current = persistentTrieRoot; for (int i = 0; i < TOTAL_CELLS; i++) { int shift = i * BITS_PER_CELL; int cellCode = (int)((canonicalLayout >> shift) & CELL_MASK_3BIT); if (current == null || cellCode < 0 || cellCode >= current.children.length || current.children[cellCode] == null) return Integer.MAX_VALUE; current = current.children[cellCode]; } return current.minAbsolute_g; }
    private long trieGetParent(long canonicalLayout) { /* ... V4.3.1 ... */ TrieNode current = persistentTrieRoot; for (int i = 0; i < TOTAL_CELLS; i++) { int shift = i * BITS_PER_CELL; int cellCode = (int)((canonicalLayout >> shift) & CELL_MASK_3BIT); if (current == null || cellCode < 0 || cellCode >= current.children.length || current.children[cellCode] == null) return -1L; current = current.children[cellCode]; } return current.parentCanonicalLayout; }

    // --- Solver Methods ---

    /**
     * Solves using A* with Trie persistence and cleaned optimization (V4.3.3)
     */
    public List<BoardState> solve(BoardState startState, int start_g) {
        this.nodesExplored = 0;
        long startLayout = startState.getLayout();
        long canonicalStartLayout = getCanonicalLayout(startLayout);

        // --- Optimization Check ONLY for start_g == 0 (Re-Solve Case) ---
        if (start_g == 0 && this.foundGoalCanonicalLayout != -1L && trieGetMin_g(this.foundGoalCanonicalLayout) != Integer.MAX_VALUE) {
            int knownStart_g = trieGetMin_g(canonicalStartLayout);
            if (knownStart_g == 0) { // Double check it's the known start node
                System.out.println("[Cache Hit] Re-solving known optimal path. Reconstructing...");
                this.nodesExplored = 1;
                List<BoardState> reconstructedPath = reconstructGlobalPath(this.foundGoalCanonicalLayout);
                if (!reconstructedPath.isEmpty() && getCanonicalLayout(reconstructedPath.getFirst().getLayout()) == canonicalStartLayout) {
                    return reconstructedPath;
                } else {
                    System.err.println("Warning: Global path reconstruction failed validation (start_g=0). Proceeding with search.");
                    // Fall through to regular search
                }
            }
        }
        // --- End Optimization Check ---

        // Update Trie for the start of this search segment (only if better)
        // Note: Parent is -1L because it's the start *for this search call*
        trieUpdate(canonicalStartLayout, start_g, -1L);

        if (isGoalLayout(startLayout)) {
            this.nodesExplored = 1;
            if (this.foundGoalCanonicalLayout == -1L) setFoundGoalLayout(startLayout);
            return Collections.singletonList(startState);
        }

        PriorityQueue<ElementNode> openSet = new PriorityQueue<>();
        Map<Long, Integer> visitedInThisSearch = new HashMap<>();

        int initialH = calculateHeuristic(startLayout);
        ElementNode initialElement = new ElementNode(startState, null, start_g, initialH);

        openSet.add(initialElement);
        visitedInThisSearch.put(canonicalStartLayout, start_g);

        while (!openSet.isEmpty()) {
            ElementNode currentNode = openSet.poll();
            this.nodesExplored++;
            long currentLayout = currentNode.state.getLayout();
            long canonicalCurrentLayout = getCanonicalLayout(currentLayout);
            int currentAbsolute_g = currentNode.g;

            if (isGoalLayout(currentLayout)) {
                if (this.foundGoalCanonicalLayout == -1L) setFoundGoalLayout(currentLayout);
                return reconstructPath(currentNode); // Use relative path reconstruction
            }

            // Dequeue Pruning (Local and Global)
            if (currentAbsolute_g > visitedInThisSearch.getOrDefault(canonicalCurrentLayout, Integer.MAX_VALUE)) continue;
            if (currentAbsolute_g > trieGetMin_g(canonicalCurrentLayout)) continue; // Global check (>)

            // Expand Successors
            List<Long> successorLayouts = generateSuccessorLayouts(currentLayout);

            for (long successorLayout : successorLayouts) {
                int newAbsolute_g = currentAbsolute_g + 1;
                long canonicalSuccessorLayout = getCanonicalLayout(successorLayout);

                // Enqueue Local Pruning (within this search)
                if (newAbsolute_g >= visitedInThisSearch.getOrDefault(canonicalSuccessorLayout, Integer.MAX_VALUE)) continue;

                // NO Enqueue Global Pruning based on trieGetMin_g

                // Process successor
                int h_new = calculateHeuristic(successorLayout);
                BoardState successorState = new BoardState(successorLayout);
                ElementNode newElement = new ElementNode(successorState, currentNode, newAbsolute_g, h_new);

                visitedInThisSearch.put(canonicalSuccessorLayout, newAbsolute_g);
                // Update Trie only if this path is globally better
                trieUpdate(canonicalSuccessorLayout, newAbsolute_g, canonicalCurrentLayout);
                openSet.add(newElement);
            }
        }
        return Collections.emptyList(); // No solution found
    }

    // --- findOptimalPathFromCurrent (Unchanged from V4.3.1) ---
    public List<BoardState> findOptimalPathFromCurrent(BoardState currentState) {
        long currentLayout = currentState.getLayout();
        long canonicalLayout = getCanonicalLayout(currentLayout);
        int currentAbsolute_g = trieGetMin_g(canonicalLayout); // Use Trie
        if (currentAbsolute_g == Integer.MAX_VALUE) {
            System.err.println("Error V4.3.3: Cannot find optimal path from state with unknown absolute g-cost: " + currentLayout + ". Assuming start_g = 0.");
            currentAbsolute_g = 0; // Fallback
        }
        return solve(currentState, currentAbsolute_g); // Will NOT hit cache check if start_g != 0
    }

    // --- reconstructPath (Unchanged - relative path) ---
    private List<BoardState> reconstructPath(ElementNode endNode) { /* ... V4.3.1 ... */ LinkedList<BoardState> path = new LinkedList<>(); ElementNode trace = endNode; while (trace != null) { path.addFirst(trace.state); trace = trace.father; } return path; }

    // --- reconstructGlobalPath (Unchanged - uses Trie parents) ---
    private List<BoardState> reconstructGlobalPath(long targetCanonicalLayout) { /* ... V4.3.1 ... */ LinkedList<BoardState> path = new LinkedList<>(); long currentLayout = targetCanonicalLayout; int safetyBreak = TOTAL_CELLS * 1000; int count = 0; while (currentLayout != -1L && count++ < safetyBreak) { path.addFirst(new BoardState(currentLayout)); currentLayout = trieGetParent(currentLayout); } if (count >= safetyBreak) System.err.println("Warning: reconstructGlobalPath hit safety break."); if (path.isEmpty() && targetCanonicalLayout != -1L) System.err.println("Error: reconstructGlobalPath resulted in empty path for target " + targetCanonicalLayout); return path; }

    // --- Main method (Unchanged) ---
    public static void main(String[] args) { /* ... V4.3.1 ... */ System.out.println("Klotski Solver A* V4.3.3 (Trie Persistence - Cleaned Optimization) Test"); try { int[][] initialArray = { {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL}, {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL}, {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL}, {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL}, {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER} }; long initialLayout = BoardSerializer.serialize(initialArray); BoardState initialState = new BoardState(initialLayout); System.out.println("Initial State (Layout: " + initialState.getLayout() + "):"); BoardSerializer.printBoard(initialState.getBoardArray()); KlotskiSolver solver = new KlotskiSolver(); runTestSequence(solver, initialState, "Layout1"); } catch (Exception e) { System.err.println("An error occurred in main:"); e.printStackTrace(); } System.out.println("\n\n===== Test Complete ====="); }

    // --- runTestSequence (Unchanged from V4.3.2 - still sets goal hash) ---
    private static void runTestSequence(KlotskiSolver solver, BoardState initialState, String layoutName) { /* ... V4.3.2 implementation ... */
        List<BoardState> initialSolution = null; BoardState intermediateStateOptimal = null; BoardState nonOptimalState = null; int stepForGuidanceOptimal = 3; // KEEP step 3 for testing
        try {
            // Phase 1
            System.out.println("\n[" + layoutName + "] Phase 1: Initial Solve (start_g=0) ---"); long startTime = System.currentTimeMillis(); initialSolution = solver.solve(initialState, 0); long endTime = System.currentTimeMillis();
            System.out.println("[" + layoutName + "] Initial solve finished in " + (endTime - startTime) + " ms."); System.out.println("[" + layoutName + "] Initial solve explored nodes: " + solver.getNodesExplored());
            if (initialSolution.isEmpty()) { System.out.println("[" + layoutName + "] Initial solve found no solution."); return; } else { System.out.println("[" + layoutName + "] Initial solution found with " + (initialSolution.size() - 1) + " steps."); solver.setFoundGoalLayout(initialSolution.getLast().getLayout()); }
            // Phase 2
            System.out.println("\n[" + layoutName + "] Phase 2: Solve from Optimal Intermediate ---");
            if (initialSolution.size() > stepForGuidanceOptimal) {
                intermediateStateOptimal = initialSolution.get(stepForGuidanceOptimal); System.out.println("[" + layoutName + "] Solving from state after " + stepForGuidanceOptimal + " optimal steps (Layout: " + intermediateStateOptimal.getLayout() + ")"); startTime = System.currentTimeMillis(); List<BoardState> pathFromOptimal = solver.findOptimalPathFromCurrent(intermediateStateOptimal); endTime = System.currentTimeMillis();
                System.out.println("[" + layoutName + "] Guidance (Optimal) finished in " + (endTime - startTime) + " ms."); System.out.println("[" + layoutName + "] Guidance (Optimal) explored nodes: " + solver.getNodesExplored()); // Expect similar to V4.3.1
                if(pathFromOptimal.isEmpty()) System.err.println("[" + layoutName + "] >>>>> FAILURE: No path found from optimal intermediate!"); else { /* Validation */ System.out.println("[" + layoutName + "] Guidance (Optimal) found path with " + (pathFromOptimal.size() - 1) + " steps."); int expectedRemaining = initialSolution.size() - 1 - stepForGuidanceOptimal; if (pathFromOptimal.size() - 1 != expectedRemaining) { System.err.println("[" + layoutName + "] >>>>> WARNING: Optimal guidance step count mismatch ("+(pathFromOptimal.size() - 1)+" vs "+expectedRemaining+")"); } else { System.out.println("[" + layoutName + "] Optimal guidance step count matches expected remaining."); } }
            } else { System.out.println("[" + layoutName + "] Skipping Phase 2."); }
            // Phase 3
            System.out.println("\n[" + layoutName + "] Phase 3: Solve from Non-Optimal Intermediate ---"); List<Long> successors = generateSuccessorLayouts(initialState.getLayout());
            if (!successors.isEmpty() && initialSolution.size() > 1) {
                long firstOptimalStepLayout = initialSolution.get(1).getLayout(); long chosenLayout = -1; for (long succLayout : successors) { if (succLayout != firstOptimalStepLayout) { chosenLayout = succLayout; break; } } if (chosenLayout == -1 && !successors.isEmpty()) { chosenLayout = successors.get(0); }
                if (chosenLayout != -1) {
                    nonOptimalState = new BoardState(chosenLayout); System.out.println("[" + layoutName + "] Solving from potentially non-optimal state after 1 move (Layout: " + nonOptimalState.getLayout() + ")"); startTime = System.currentTimeMillis(); List<BoardState> pathFromNonOptimal = solver.findOptimalPathFromCurrent(nonOptimalState); endTime = System.currentTimeMillis();
                    System.out.println("[" + layoutName + "] Guidance (Non-Optimal) finished in " + (endTime - startTime) + " ms."); System.out.println("[" + layoutName + "] Guidance (Non-Optimal) explored nodes: " + solver.getNodesExplored()); // Expect similar to V4.3.1
                    if(pathFromNonOptimal.isEmpty()) System.err.println("[" + layoutName + "] >>>>> FAILURE: No path found from non-optimal intermediate!"); else { System.out.println("[" + layoutName + "] Guidance (Non-Optimal) found path with " + (pathFromNonOptimal.size() - 1) + " steps."); }
                } else { System.out.println("[" + layoutName + "] Skipping Phase 3 (No non-optimal successor)."); }
            } else { System.out.println("[" + layoutName + "] Skipping Phase 3 (No successors/short solution)."); }
            // Phase 4
            System.out.println("\n[" + layoutName + "] Phase 4: Re-Solve Initial (start_g=0, After Intermediates) ---"); startTime = System.currentTimeMillis(); List<BoardState> reSolveSolution = solver.solve(initialState, 0); endTime = System.currentTimeMillis();
            System.out.println("[" + layoutName + "] Re-solve finished in " + (endTime - startTime) + " ms."); System.out.println("[" + layoutName + "] Re-solve explored nodes: " + solver.getNodesExplored()); // Expect 1
            if (reSolveSolution.isEmpty()) { System.err.println("[" + layoutName + "] >>>>> FAILURE: Re-solve failed to find a solution!"); } else { /* Validation */ System.out.println("[" + layoutName + "] Re-solve solution found with " + (reSolveSolution.size() - 1) + " steps."); if (initialSolution.size() != reSolveSolution.size()) { System.err.println("[" + layoutName + "] >>>>> WARNING: Re-solve step count mismatch"); } else { System.out.println("[" + layoutName + "] Re-solve step count matches initial solve."); } }
        } catch (Exception e) { System.err.println("\n[" + layoutName + "] An error occurred during test sequence:"); e.printStackTrace(); }
    }
}