package controller.solver; // Ensure this matches BoardSerializer's package

// Assuming BoardSerializer and BoardState are in this package

import java.util.*;

// --- ElementNode definition (Unchanged from V4.2.1) ---
class ElementNode implements Comparable<ElementNode> {
    BoardState state;
    ElementNode father;
    int g; // ABSOLUTE cost from original start 'O'
    int h;
    int f; // f = g + h

    public ElementNode(BoardState s, ElementNode fth, int absolute_g, int h) {
        this.state = s;
        this.father = fth;
        this.g = absolute_g;
        this.h = h;
        this.f = this.g + h;
    }
    @Override public int compareTo(ElementNode o) { if (f != o.f) return Integer.compare(f, o.f); return Integer.compare(h, o.h); }
    @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; ElementNode that = (ElementNode) o; return state.getLayout() == that.state.getLayout(); }
    @Override public int hashCode() { return state.hashCode(); }
}

/**
 * V4.2.3: A* Solver - Rigorously uses BoardSerializer codes.
 * - generateSuccessorLayouts, calculateHeuristic, isGoalLayout use BoardSerializer.CODE_*
 * - Other logic remains from V4.2.1 (Removed Enqueue Global Prune).
 */
public class KlotskiSolver {

    // --- Constants ---
    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};
    // Use constants from BoardSerializer directly
    private static final int ROWS = BoardSerializer.ROWS;
    private static final int COLS = BoardSerializer.COLS;
    private static final int BITS_PER_CELL = 3; // Should match BoardSerializer if used internally
    private static final long CELL_MASK_3BIT = (1L << BITS_PER_CELL) - 1L; // Should match BoardSerializer

    // --- Instance Variables ---
    private int nodesExplored = 0;
    private final Map<Long, Integer> globalBest_g; // canonicalLayout -> best absolute g

    public KlotskiSolver() {
        this.globalBest_g = new HashMap<>();
    }

    public int getNodesExplored() { return nodesExplored; }

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
    private static long getCanonicalLayout(long layout) { return Math.min(layout, getSymmetricLayout(layout)); }

    /**
     * Generates successor layouts - Uses BoardSerializer codes RIGOROUSLY.
     */
    private static List<Long> generateSuccessorLayouts(long currentLayout) {
        List<Long> successorLayouts = new ArrayList<>();
        boolean[] processedCell = new boolean[ROWS * COLS];

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int cellIndex = r * COLS + c;
                if (processedCell[cellIndex]) continue;

                // Use codes from BoardSerializer
                long pieceCode = getCellCode(currentLayout, r, c);
                processedCell[cellIndex] = true;

                if (pieceCode == BoardSerializer.CODE_EMPTY) continue; // CORRECT CODE

                List<int[]> pieceCellsCoords = new ArrayList<>();
                pieceCellsCoords.add(new int[]{r, c});

                // Identify full piece using BoardSerializer codes
                if (pieceCode == BoardSerializer.CODE_SOLDIER) {} // CORRECT CODE
                else if (pieceCode == BoardSerializer.CODE_HORIZONTAL) { // CORRECT CODE (checks for 3)
                    if (c + 1 < COLS && getCellCode(currentLayout, r, c + 1) == pieceCode) {
                        pieceCellsCoords.add(new int[]{r, c + 1}); processedCell[cellIndex + 1] = true;
                    } else continue; // Invalid state
                } else if (pieceCode == BoardSerializer.CODE_VERTICAL) { // CORRECT CODE (checks for 2)
                    if (r + 1 < ROWS && getCellCode(currentLayout, r + 1, c) == pieceCode) {
                        pieceCellsCoords.add(new int[]{r + 1, c}); processedCell[cellIndex + COLS] = true;
                    } else continue; // Invalid state
                } else if (pieceCode == BoardSerializer.CODE_CAO_CAO) { // CORRECT CODE (checks for 4)
                    boolean rOk = c + 1 < COLS && getCellCode(currentLayout, r, c + 1) == pieceCode;
                    boolean bOk = r + 1 < ROWS && getCellCode(currentLayout, r + 1, c) == pieceCode;
                    boolean brOk = c + 1 < COLS && r + 1 < ROWS && getCellCode(currentLayout, r + 1, c + 1) == pieceCode;
                    if (rOk && bOk && brOk) {
                        pieceCellsCoords.add(new int[]{r, c + 1}); processedCell[cellIndex + 1] = true;
                        pieceCellsCoords.add(new int[]{r + 1, c}); processedCell[cellIndex + COLS] = true;
                        pieceCellsCoords.add(new int[]{r + 1, c + 1}); processedCell[cellIndex + COLS + 1] = true;
                    } else continue; // Invalid state
                } else {
                    System.err.println("Warning: generateSuccessorLayouts unexpected piece code " + pieceCode);
                    continue;
                }

                // Try moving in 4 directions
                for (int dir = 0; dir < 4; dir++) {
                    int dr = DR[dir], dc = DC[dir];
                    boolean canMove = true;
                    List<int[]> targetCellsCoords = new ArrayList<>();

                    for (int[] cellCoord : pieceCellsCoords) {
                        int nr = cellCoord[0] + dr; int nc = cellCoord[1] + dc;
                        if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS) { canMove = false; break; }
                        targetCellsCoords.add(new int[]{nr, nc});
                        boolean targetIsOriginal = false;
                        for (int[] originalCoord : pieceCellsCoords) { if (nr == originalCoord[0] && nc == originalCoord[1]) { targetIsOriginal = true; break; } }
                        // Check against BoardSerializer.CODE_EMPTY
                        if (!targetIsOriginal && getCellCode(currentLayout, nr, nc) != BoardSerializer.CODE_EMPTY) { canMove = false; break; }
                    }

                    if (canMove) {
                        long newLayout = currentLayout; long clearMask = 0L; long setMask = 0L;
                        for (int[] cellCoord : pieceCellsCoords) { clearMask |= (CELL_MASK_3BIT << ((cellCoord[0] * COLS + cellCoord[1]) * BITS_PER_CELL)); }
                        // Set with the correct pieceCode (which IS the layout code)
                        for (int[] targetCoord : targetCellsCoords) { setMask |= (pieceCode << ((targetCoord[0] * COLS + targetCoord[1]) * BITS_PER_CELL)); }
                        newLayout = (newLayout & ~clearMask) | setMask;
                        successorLayouts.add(newLayout);
                    }
                }
            }
        }
        return successorLayouts;
    }

    /**
     * Calculates heuristic - Uses BoardSerializer codes.
     */
    private static int calculateHeuristic(long layout) {
        for (int r = 0; r < ROWS - 1; r++) {
            for (int c = 0; c < COLS - 1; c++) {
                // Use BoardSerializer.CODE_CAO_CAO
                if (getCellCode(layout, r, c) == BoardSerializer.CODE_CAO_CAO &&
                        getCellCode(layout, r, c + 1) == BoardSerializer.CODE_CAO_CAO &&
                        getCellCode(layout, r + 1, c) == BoardSerializer.CODE_CAO_CAO &&
                        getCellCode(layout, r + 1, c + 1) == BoardSerializer.CODE_CAO_CAO) {
                    int goalR = 3; int goalC = 1; return Math.abs(r - goalR) + Math.abs(c - goalC);
                }
            }
        }
        return Integer.MAX_VALUE / 2; // Or check isGoalLayout first
    }

    /**
     * Checks goal layout - Uses BoardSerializer codes.
     */
    private boolean isGoalLayout(long layout) {
        // Use BoardSerializer.CODE_CAO_CAO
        boolean cell17_ok = getCellCode(layout, 4, 1) == BoardSerializer.CODE_CAO_CAO;
        boolean cell18_ok = getCellCode(layout, 4, 2) == BoardSerializer.CODE_CAO_CAO;
        return cell17_ok && cell18_ok;
    }

    private boolean updateGlobalBest(long canonicalLayout, int newAbsolute_g) {
        int currentBest = globalBest_g.getOrDefault(canonicalLayout, Integer.MAX_VALUE);
        if (newAbsolute_g < currentBest) { globalBest_g.put(canonicalLayout, newAbsolute_g); return true; }
        return false;
    }

    /**
     * Solves using A* (V4.2.3 - Fixed piece codes)
     */
    public List<BoardState> solve(BoardState startState, int start_g) {
        // --- Logic unchanged from V4.2.1 ---
        this.nodesExplored = 0;
        long startLayout = startState.getLayout();
        long canonicalStartLayout = getCanonicalLayout(startLayout);

        updateGlobalBest(canonicalStartLayout, start_g);

        if (isGoalLayout(startLayout)) { // Uses corrected isGoalLayout
            this.nodesExplored = 1;
            return Collections.singletonList(startState);
        }

        PriorityQueue<ElementNode> openSet = new PriorityQueue<>();
        Map<Long, Integer> visitedInThisSearch = new HashMap<>();

        int initialH = calculateHeuristic(startLayout); // Uses corrected heuristic
        ElementNode initialElement = new ElementNode(startState, null, start_g, initialH);

        openSet.add(initialElement);
        visitedInThisSearch.put(canonicalStartLayout, start_g);

        while (!openSet.isEmpty()) {
            ElementNode currentNode = openSet.poll();
            this.nodesExplored++;
            long currentLayout = currentNode.state.getLayout();
            long canonicalCurrentLayout = getCanonicalLayout(currentLayout);
            int currentAbsolute_g = currentNode.g;

            if (isGoalLayout(currentLayout)) { return reconstructPath(currentNode); } // Uses corrected isGoalLayout

            // Dequeue Pruning
            if (currentAbsolute_g > visitedInThisSearch.getOrDefault(canonicalCurrentLayout, Integer.MAX_VALUE)) continue;
            if (currentAbsolute_g > globalBest_g.getOrDefault(canonicalCurrentLayout, Integer.MAX_VALUE)) continue;

            // Expand Successors
            List<Long> successorLayouts = generateSuccessorLayouts(currentLayout); // Uses corrected generator

            for (long successorLayout : successorLayouts) {
                int newAbsolute_g = currentAbsolute_g + 1;
                long canonicalSuccessorLayout = getCanonicalLayout(successorLayout);

                // Enqueue Local Pruning
                if (newAbsolute_g >= visitedInThisSearch.getOrDefault(canonicalSuccessorLayout, Integer.MAX_VALUE)) continue;

                // Process successor
                int h_new = calculateHeuristic(successorLayout); // Uses corrected heuristic
                BoardState successorState = new BoardState(successorLayout);
                ElementNode newElement = new ElementNode(successorState, currentNode, newAbsolute_g, h_new);

                visitedInThisSearch.put(canonicalSuccessorLayout, newAbsolute_g);
                updateGlobalBest(canonicalSuccessorLayout, newAbsolute_g);
                openSet.add(newElement);
            }
        }
        return Collections.emptyList(); // No solution found
    }

    // --- findOptimalPathFromCurrent (Unchanged from V4.2.1) ---
    public List<BoardState> findOptimalPathFromCurrent(BoardState currentState) {
        long currentLayout = currentState.getLayout();
        long canonicalLayout = getCanonicalLayout(currentLayout);
        int currentAbsolute_g = globalBest_g.getOrDefault(canonicalLayout, Integer.MAX_VALUE);

        if (currentAbsolute_g == Integer.MAX_VALUE) {
            System.err.println("Error V4.2.3: Cannot find optimal path from state with unknown absolute g-cost: " + currentLayout + ". Assuming start_g = 0.");
            currentAbsolute_g = 0; // Fallback
        }
        return solve(currentState, currentAbsolute_g);
    }

    // --- reconstructPath (Unchanged) ---
    private List<BoardState> reconstructPath(ElementNode endNode) {
        LinkedList<BoardState> path = new LinkedList<>();
        ElementNode trace = endNode;
        while (trace != null) { path.addFirst(trace.state); trace = trace.father; }
        return path;
    }

    // --- Main method and runTestSequence (Unchanged from V4.2.1) ---
    public static void main(String[] args) {
        System.out.println("Klotski Solver A* V4.2.3 (Corrected Piece Codes) Test");
        try {
            // Use BoardSerializer constants to define initial array
            int[][] initialArray = {
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER}
            };
            long initialLayout = BoardSerializer.serialize(initialArray); // Serialize correctly
            BoardState initialState = new BoardState(initialLayout);

            System.out.println("Initial State (Layout: " + initialState.getLayout() + "):");
            BoardSerializer.printBoard(initialState.getBoardArray()); // Use BoardSerializer's print

            KlotskiSolver solver = new KlotskiSolver();
            runTestSequence(solver, initialState, "Layout1");
        } catch (Exception e) {
            System.err.println("An error occurred in main:"); e.printStackTrace();
        }
        System.out.println("\n\n===== Test Complete =====");
    }

    // runTestSequence needs access to BoardSerializer methods like generateSuccessorLayouts
    private static void runTestSequence(KlotskiSolver solver, BoardState initialState, String layoutName) {
        // (Implementation from V4.2.1 - should work with V4.2.3 solve)
        List<BoardState> initialSolution = null;
        BoardState intermediateStateOptimal = null;
        BoardState nonOptimalState = null;
        int stepForGuidanceOptimal = 5;

        try {
            // Phase 1
            System.out.println("\n[" + layoutName + "] Phase 1: Initial Solve (start_g=0) ---");
            long startTime = System.currentTimeMillis();
            initialSolution = solver.solve(initialState, 0);
            long endTime = System.currentTimeMillis();
            System.out.println("[" + layoutName + "] Initial solve finished in " + (endTime - startTime) + " ms.");
            System.out.println("[" + layoutName + "] Initial solve explored nodes: " + solver.getNodesExplored());
            if (initialSolution.isEmpty()) { System.out.println("[" + layoutName + "] Initial solve found no solution."); return; }
            else { System.out.println("[" + layoutName + "] Initial solution found with " + (initialSolution.size() - 1) + " steps."); }

            // Phase 2
            System.out.println("\n[" + layoutName + "] Phase 2: Solve from Optimal Intermediate ---");
            if (initialSolution.size() > stepForGuidanceOptimal) {
                intermediateStateOptimal = initialSolution.get(stepForGuidanceOptimal);
                System.out.println("[" + layoutName + "] Solving from state after " + stepForGuidanceOptimal + " optimal steps (Layout: " + intermediateStateOptimal.getLayout() + ")");
                startTime = System.currentTimeMillis();
                List<BoardState> pathFromOptimal = solver.findOptimalPathFromCurrent(intermediateStateOptimal);
                endTime = System.currentTimeMillis();
                System.out.println("[" + layoutName + "] Guidance (Optimal) finished in " + (endTime - startTime) + " ms.");
                System.out.println("[" + layoutName + "] Guidance (Optimal) explored nodes: " + solver.getNodesExplored());
                if(pathFromOptimal.isEmpty()) System.err.println("[" + layoutName + "] >>>>> FAILURE: No path found from optimal intermediate!");
                else { /* ... Validation checks ... */
                    System.out.println("[" + layoutName + "] Guidance (Optimal) found path with " + (pathFromOptimal.size() - 1) + " steps.");
                    int expectedRemaining = initialSolution.size() - 1 - stepForGuidanceOptimal;
                    if (pathFromOptimal.size() - 1 != expectedRemaining) { System.err.println("[" + layoutName + "] >>>>> WARNING: Optimal guidance step count mismatch"); }
                    else { System.out.println("[" + layoutName + "] Optimal guidance step count matches expected remaining."); }
                }
            } else { System.out.println("[" + layoutName + "] Skipping Phase 2."); }

            // Phase 3
            System.out.println("\n[" + layoutName + "] Phase 3: Solve from Non-Optimal Intermediate ---");
            // Use the generateSuccessorLayouts from this class (which uses BoardSerializer codes)
            List<Long> successors = generateSuccessorLayouts(initialState.getLayout());
            if (!successors.isEmpty() && initialSolution.size() > 1) {
                long firstOptimalStepLayout = initialSolution.get(1).getLayout();
                long chosenLayout = -1;
                for (long succLayout : successors) { if (succLayout != firstOptimalStepLayout) { chosenLayout = succLayout; break; } }
                if (chosenLayout == -1 && !successors.isEmpty()) { chosenLayout = successors.get(0); }

                if (chosenLayout != -1) {
                    nonOptimalState = new BoardState(chosenLayout);
                    System.out.println("[" + layoutName + "] Solving from potentially non-optimal state after 1 move (Layout: " + nonOptimalState.getLayout() + ")");
                    startTime = System.currentTimeMillis();
                    List<BoardState> pathFromNonOptimal = solver.findOptimalPathFromCurrent(nonOptimalState);
                    endTime = System.currentTimeMillis();
                    System.out.println("[" + layoutName + "] Guidance (Non-Optimal) finished in " + (endTime - startTime) + " ms.");
                    System.out.println("[" + layoutName + "] Guidance (Non-Optimal) explored nodes: " + solver.getNodesExplored());
                    if(pathFromNonOptimal.isEmpty()) System.err.println("[" + layoutName + "] >>>>> FAILURE: No path found from non-optimal intermediate!");
                    else { System.out.println("[" + layoutName + "] Guidance (Non-Optimal) found path with " + (pathFromNonOptimal.size() - 1) + " steps."); }
                } else { System.out.println("[" + layoutName + "] Skipping Phase 3 (No non-optimal successor)."); }
            } else { System.out.println("[" + layoutName + "] Skipping Phase 3 (No successors/short solution)."); }

            // Phase 4
            System.out.println("\n[" + layoutName + "] Phase 4: Re-Solve Initial (start_g=0, After Intermediates) ---");
            startTime = System.currentTimeMillis();
            List<BoardState> reSolveSolution = solver.solve(initialState, 0);
            endTime = System.currentTimeMillis();
            System.out.println("[" + layoutName + "] Re-solve finished in " + (endTime - startTime) + " ms.");
            System.out.println("[" + layoutName + "] Re-solve explored nodes: " + solver.getNodesExplored());
            if (reSolveSolution.isEmpty()) { System.err.println("[" + layoutName + "] >>>>> FAILURE: Re-solve failed to find a solution!"); }
            else { /* ... Validation checks ... */
                System.out.println("[" + layoutName + "] Re-solve solution found with " + (reSolveSolution.size() - 1) + " steps.");
                if (initialSolution.size() != reSolveSolution.size()) { System.err.println("[" + layoutName + "] >>>>> WARNING: Re-solve step count mismatch"); }
                else { System.out.println("[" + layoutName + "] Re-solve step count matches initial solve."); }
            }
        } catch (Exception e) {
            System.err.println("\n[" + layoutName + "] An error occurred during test sequence:"); e.printStackTrace();
        }
    }
}