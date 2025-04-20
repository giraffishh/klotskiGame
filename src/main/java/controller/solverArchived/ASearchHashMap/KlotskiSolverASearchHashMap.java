package controller.solverArchived.ASearchHashMap; // Keeping original package name

import controller.solver.BoardSerializer; // Assuming these are in the correct location
import controller.solver.BoardState;

import java.util.*;

// --- ElementNode definition for A* --- (Keep as is)
class ElementNode implements Comparable<ElementNode> {
    BoardState state;
    ElementNode father;
    int g; // Cost from start node
    int h; // Heuristic cost to goal
    int f; // f = g + h

    public ElementNode(BoardState s, ElementNode fth, int g, int h) {
        this.state = s;
        this.father = fth;
        this.g = g;
        this.h = h;
        this.f = g + h;
    }

    @Override
    public int compareTo(ElementNode o) {
        if (f != o.f) return Integer.compare(f, o.f);
        return Integer.compare(h, o.h); // Tie-breaking using heuristic
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElementNode that = (ElementNode) o;
        // Equality based on the board state's layout
        return state.getLayout() == that.state.getLayout();
    }

    @Override
    public int hashCode() {
        // Hash code based on the board state's layout
        return state.hashCode();
    }
}


/**
 * V4.1: A* Solver using HashMap + Direct Layout Operations.
 * generateSuccessorLayouts now operates directly on the long layout.
 */
public class KlotskiSolverASearchHashMap {

    // --- Constants ---
    private static final int[] DR = {-1, 1, 0, 0}; // Up, Down, Left, Right
    private static final int[] DC = {0, 0, -1, 1};
    private static final int ROWS = BoardSerializer.ROWS;
    private static final int COLS = BoardSerializer.COLS;
    private static final int BITS_PER_CELL = 3;
    private static final long CELL_MASK_3BIT = (1L << BITS_PER_CELL) - 1L; // 0b111 = 7L

    // Use codes directly from BoardSerializer for consistency
    // These codes match the ones used in the optimized generateSuccessorLayouts
    private static final long CODE_EMPTY = BoardSerializer.arrayToCodeMap.get(BoardSerializer.EMPTY);         // 0
    private static final long CODE_SOLDIER = BoardSerializer.arrayToCodeMap.get(BoardSerializer.SOLDIER);     // 1
    private static final long CODE_VERTICAL = BoardSerializer.arrayToCodeMap.get(BoardSerializer.VERTICAL);   // 2
    private static final long CODE_HORIZONTAL = BoardSerializer.arrayToCodeMap.get(BoardSerializer.HORIZONTAL); // 3
    private static final long CODE_CAO_CAO = BoardSerializer.arrayToCodeMap.get(BoardSerializer.CAO_CAO);       // 4
    // Constant used specifically for goal check and heuristic calculation
    private static final long CODE_CAO_CAO_TARGET = CODE_CAO_CAO; // = 4L

    // --- Instance Variables ---
    private int nodesExplored = 0;

    public int getNodesExplored() { return nodesExplored; }

    // --- Static Helper Methods ---

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
     * Returns the canonical layout (minimum of layout and its symmetric).
     */
    private static long getCanonicalLayout(long layout) {
        return Math.min(layout, getSymmetricLayout(layout));
    }


    /**
     * OPTIMIZED: Generates successor layouts by directly manipulating the long representation.
     * Avoids creating intermediate int[][] arrays.
     * (Copied and adapted from BFS version V3.3)
     */
    private static List<Long> generateSuccessorLayouts(long currentLayout) {
        List<Long> successorLayouts = new ArrayList<>();
        boolean[] processedCell = new boolean[ROWS * COLS]; // Track processed cells by index

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int cellIndex = r * COLS + c;
                if (processedCell[cellIndex]) continue;

                long pieceCode = getCellCode(currentLayout, r, c);
                processedCell[cellIndex] = true; // Mark current cell as processed

                if (pieceCode == CODE_EMPTY) {
                    continue; // Skip empty cells
                }

                List<int[]> pieceCellsCoords = new ArrayList<>(); // Stores {r, c} for the piece
                pieceCellsCoords.add(new int[]{r, c}); // Start with the current cell

                // --- Identify the full piece based on its code and neighbors ---
                if (pieceCode == CODE_SOLDIER) {
                    // Soldier occupies only one cell, already added.
                } else if (pieceCode == CODE_HORIZONTAL) {
                    // Check right neighbor
                    if (c + 1 < COLS && getCellCode(currentLayout, r, c + 1) == pieceCode) {
                        pieceCellsCoords.add(new int[]{r, c + 1});
                        processedCell[cellIndex + 1] = true;
                    } else continue; // Invalid state: Horizontal piece code without matching neighbor
                } else if (pieceCode == CODE_VERTICAL) {
                    // Check bottom neighbor
                    if (r + 1 < ROWS && getCellCode(currentLayout, r + 1, c) == pieceCode) {
                        pieceCellsCoords.add(new int[]{r + 1, c});
                        processedCell[cellIndex + COLS] = true;
                    } else continue; // Invalid state: Vertical piece code without matching neighbor
                } else if (pieceCode == CODE_CAO_CAO) {
                    // Check right, bottom, and bottom-right neighbors
                    boolean rightOk = c + 1 < COLS && getCellCode(currentLayout, r, c + 1) == pieceCode;
                    boolean bottomOk = r + 1 < ROWS && getCellCode(currentLayout, r + 1, c) == pieceCode;
                    boolean bottomRightOk = c + 1 < COLS && r + 1 < ROWS && getCellCode(currentLayout, r + 1, c + 1) == pieceCode;
                    if (rightOk && bottomOk && bottomRightOk) {
                        pieceCellsCoords.add(new int[]{r, c + 1});
                        pieceCellsCoords.add(new int[]{r + 1, c});
                        pieceCellsCoords.add(new int[]{r + 1, c + 1});
                        processedCell[cellIndex + 1] = true;
                        processedCell[cellIndex + COLS] = true;
                        processedCell[cellIndex + COLS + 1] = true;
                    } else continue; // Invalid state: CaoCao code without matching neighbors
                } else {
                    // Should not happen with valid codes defined in BoardSerializer
                    System.err.println("Warning: Encountered unexpected piece code " + pieceCode + " at [" + r + "," + c + "] in layout " + currentLayout);
                    continue;
                }

                // --- Try moving the identified piece in 4 directions ---
                for (int dir = 0; dir < 4; dir++) {
                    int dr = DR[dir], dc = DC[dir];
                    boolean canMove = true;
                    List<int[]> targetCellsCoords = new ArrayList<>(); // Store {nr, nc} for target cells

                    // Check if all target cells are valid and empty (or part of the original piece)
                    for (int[] cellCoord : pieceCellsCoords) {
                        int nr = cellCoord[0] + dr;
                        int nc = cellCoord[1] + dc;

                        // Check bounds
                        if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS) {
                            canMove = false;
                            break;
                        }
                        targetCellsCoords.add(new int[]{nr, nc});

                        // Check if target cell is occupied by a *different* piece
                        boolean targetIsOriginal = false;
                        for (int[] originalCoord : pieceCellsCoords) {
                            if (nr == originalCoord[0] && nc == originalCoord[1]) {
                                targetIsOriginal = true;
                                break;
                            }
                        }
                        if (!targetIsOriginal && getCellCode(currentLayout, nr, nc) != CODE_EMPTY) {
                            canMove = false;
                            break;
                        }
                    } // End checking target cells for one direction

                    // --- Generate new layout if move is valid ---
                    if (canMove) {
                        long newLayout = currentLayout;
                        long clearMask = 0L;
                        long setMask = 0L;

                        // Create mask to clear original piece cells
                        for (int[] cellCoord : pieceCellsCoords) {
                            clearMask |= (CELL_MASK_3BIT << ((cellCoord[0] * COLS + cellCoord[1]) * BITS_PER_CELL));
                        }
                        // Create mask to set new piece cells with the correct piece code
                        for (int[] targetCoord : targetCellsCoords) {
                            setMask |= (pieceCode << ((targetCoord[0] * COLS + targetCoord[1]) * BITS_PER_CELL));
                        }

                        // Apply masks: clear old positions, then set new positions
                        newLayout = (newLayout & ~clearMask) | setMask;
                        successorLayouts.add(newLayout);
                    }
                } // End loop through 4 directions
            } // End loop c
        } // End loop r
        return successorLayouts;
    }


    /**
     * Calculates the heuristic value (Manhattan distance of Cao Cao to goal).
     * Goal position is assumed to be top-left at (3, 1).
     */
    private static int calculateHeuristic(long layout) {
        // Find the top-left corner of Cao Cao
        for (int r = 0; r < ROWS - 1; r++) { // CaoCao is 2x2, so no need to check last row/col for top-left
            for (int c = 0; c < COLS - 1; c++) {
                // Check if the cell (r, c) contains CaoCao code AND it's the top-left corner
                if (getCellCode(layout, r, c) == CODE_CAO_CAO_TARGET &&
                        getCellCode(layout, r, c + 1) == CODE_CAO_CAO_TARGET &&
                        getCellCode(layout, r + 1, c) == CODE_CAO_CAO_TARGET &&
                        getCellCode(layout, r + 1, c + 1) == CODE_CAO_CAO_TARGET)
                {
                    // Target position for the top-left corner is (3, 1)
                    int goalR = 3;
                    int goalC = 1;
                    return Math.abs(r - goalR) + Math.abs(c - goalC);
                }
            }
        }
        // If Cao Cao is not found (should not happen in valid states) or layout is invalid
        // Return a large value, but avoid overflow issues with Integer.MAX_VALUE + g
        // System.err.println("Warning: Cao Cao not found in layout for heuristic calculation: " + layout);
        return Integer.MAX_VALUE / 2; // Indicate a very high cost/bad state
    }

    /**
     * Checks if the layout represents the goal state (Cao Cao at bottom center).
     * Goal: CaoCao at cells (3,1), (3,2), (4,1), (4,2)
     * Indices: 13, 14, 17, 18
     */
    private boolean isGoalLayout(long layout) {
        // Check the bottom-left and bottom-right cells occupied by CaoCao in goal state
        boolean cell17_ok = getCellCode(layout, 4, 1) == CODE_CAO_CAO_TARGET;
        boolean cell18_ok = getCellCode(layout, 4, 2) == CODE_CAO_CAO_TARGET;
        // Minimal check is sufficient if piece generation is correct
        return cell17_ok && cell18_ok;
    }


    /**
     * Solves using A* + HashMap + Symmetry + Direct Layout Operations.
     */
    public List<BoardState> solve(BoardState initialState) {
        this.nodesExplored = 0;
        long initialLayout = initialState.getLayout();

        if (isGoalLayout(initialLayout)) {
            System.out.println("Initial state is already the goal.");
            this.nodesExplored = 1;
            return Collections.singletonList(initialState);
        }

        PriorityQueue<ElementNode> openSet = new PriorityQueue<>();
        Map<Long, ElementNode> closedSet = new HashMap<>(); // Stores best node found for each canonical layout

        int initialH = calculateHeuristic(initialLayout);
        ElementNode initialElement = new ElementNode(initialState, null, 0, initialH);
        long canonicalInitialLayout = getCanonicalLayout(initialLayout);

        openSet.add(initialElement);
        closedSet.put(canonicalInitialLayout, initialElement);
        // long statesAdded = 1; // Counter if needed

        while (!openSet.isEmpty()) {
            ElementNode currentNode = openSet.poll();
            this.nodesExplored++;
            long currentLayout = currentNode.state.getLayout();
            long canonicalCurrentLayout = getCanonicalLayout(currentLayout);

            // Goal Check
            if (isGoalLayout(currentLayout)) {
                //System.out.println("Solve: Goal found!");
                List<BoardState> path = reconstructPath(currentNode);
                //System.out.println("Solve: Solution found! Processed " + this.nodesExplored + " states.");
                return path;
            }

            // Check if a better path (lower g) to this state has already been found and processed.
            // This check uses the canonical layout as the key in closedSet.
            ElementNode bestKnownNode = closedSet.get(canonicalCurrentLayout);
            // If the node we pulled from openSet has a higher g cost than the best one we recorded
            // in closedSet, it means we found a shorter path earlier. Skip this one.
            if (bestKnownNode == null || currentNode.g > bestKnownNode.g) {
                // Note: bestKnownNode should not be null here if currentNode came from openSet
                // and was put in closedSet before. The g > bestKnownNode.g check is the critical one.
                // This handles cases where the same state is reached via different paths and re-added
                // to openSet before the better path was processed.
                continue;
            }
            // If currentNode.g <= bestKnownNode.g, this is the best path found so far, process its neighbors.
            // (Equality case means this *is* the best node recorded in closedSet).


            // Logging (optional)
            if (this.nodesExplored % 100000 == 0) {
                //System.out.println("Solve Nodes: " + this.nodesExplored + ", OpenSet: " + openSet.size() + ", Current f=" + currentNode.f + " (g=" + currentNode.g + ", h=" + currentNode.h + ")");
            }

            // Use the optimized generator
            List<Long> successorLayouts = generateSuccessorLayouts(currentLayout);

            for (long successorLayout : successorLayouts) {
                int g_new = currentNode.g + 1;
                int h_new = calculateHeuristic(successorLayout);
                long canonicalSuccessorLayout = getCanonicalLayout(successorLayout);

                ElementNode existingBest = closedSet.get(canonicalSuccessorLayout);

                // If successor is new OR we found a strictly better path (lower g)
                if (existingBest == null || g_new < existingBest.g) {
                    BoardState successorState = new BoardState(successorLayout);
                    ElementNode newElement = new ElementNode(successorState, currentNode, g_new, h_new);

                    // Update closedSet with the new best node for this state
                    closedSet.put(canonicalSuccessorLayout, newElement);
                    // Add the new/improved node to the open set for exploration
                    openSet.add(newElement);
                    // if (existingBest == null) statesAdded++;
                }
                // If existingBest != null and g_new >= existingBest.g, do nothing.
                // We already have an equal or better path recorded in closedSet,
                // and if it's equal, the corresponding node is either already processed
                // or is in openSet waiting. If better, the current path is suboptimal.
            }
        }

        //System.out.println("Solve: No solution found after processing " + this.nodesExplored + " states.");
        return Collections.emptyList();
    }


    /**
     * Real-time guidance: Finds optimal path from current state using A*.
     * (This method is structurally similar to solve, using its own local sets)
     */
    public List<BoardState> findOptimalPathFromCurrent(BoardState currentState) {
        System.out.println("\n--- Finding optimal path from current state (Local A* - Direct Layout Ops) ---");
        this.nodesExplored = 0; // Reset counter for this specific guidance search
        long currentLayout = currentState.getLayout();

        if (isGoalLayout(currentLayout)) {
            System.out.println("Current state is already the goal.");
            this.nodesExplored = 1;
            return Collections.singletonList(currentState);
        }

        PriorityQueue<ElementNode> localOpenSet = new PriorityQueue<>();
        Map<Long, ElementNode> localBestNodes = new HashMap<>(); // Local closed set equivalent

        int initialH = calculateHeuristic(currentLayout);
        ElementNode startElement = new ElementNode(currentState, null, 0, initialH);
        long canonicalStartLayout = getCanonicalLayout(currentLayout);

        localOpenSet.add(startElement);
        localBestNodes.put(canonicalStartLayout, startElement);
        // long statesAddedLocal = 1;

        while (!localOpenSet.isEmpty()) {
            ElementNode localCurrentNode = localOpenSet.poll();
            this.nodesExplored++;
            long layout = localCurrentNode.state.getLayout();
            long canonicalLayout = getCanonicalLayout(layout);

            // Goal Check
            if (isGoalLayout(layout)) {
                //System.out.println("Local A*: Goal found!");
                List<BoardState> path = reconstructPath(localCurrentNode);
                //System.out.println("Local A*: Path found! Explored " + this.nodesExplored + " states.");
                return path;
            }

            // Outdated check (compare with localBestNodes)
            ElementNode bestKnownNode = localBestNodes.get(canonicalLayout);
            if (bestKnownNode == null || localCurrentNode.g > bestKnownNode.g) {
                continue; // Skip outdated or suboptimal node
            }

            // Logging (optional)
            if (this.nodesExplored % 50000 == 0) {
                //System.out.println("Local A* Nodes: " + this.nodesExplored + ", OpenSet: " + localOpenSet.size() + ", Current f=" + localCurrentNode.f);
            }

            // Use the optimized generator
            List<Long> successorLayouts = generateSuccessorLayouts(layout);

            for (long successorLayout : successorLayouts) {
                int g_new = localCurrentNode.g + 1;
                int h_new = calculateHeuristic(successorLayout);
                long successorCanonicalLayout = getCanonicalLayout(successorLayout);

                ElementNode existingBest = localBestNodes.get(successorCanonicalLayout);

                if (existingBest == null || g_new < existingBest.g) {
                    BoardState successorState = new BoardState(successorLayout);
                    ElementNode newElement = new ElementNode(successorState, localCurrentNode, g_new, h_new);
                    localBestNodes.put(successorCanonicalLayout, newElement);
                    localOpenSet.add(newElement);
                    // if(existingBest == null) statesAddedLocal++;
                }
            }
        }

        System.out.println("Local A*: No solution found from current state after exploring " + this.nodesExplored + " states.");
        return Collections.emptyList();
    }


    /**
     * Helper method: Reconstructs path by backtracking father pointers.
     */
    private List<BoardState> reconstructPath(ElementNode endNode) {
        LinkedList<BoardState> path = new LinkedList<>();
        ElementNode trace = endNode;
        while (trace != null) {
            path.addFirst(trace.state);
            trace = trace.father;
        }
        return path;
    }


    // --- Main method for testing ---
    public static void main(String[] args) {
        System.out.println("Klotski Solver A* V4.1 (HashMap + Direct Layout Ops) Test");
        try {
            // Standard initial state
            int[][] initialArray = {
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER}
            };

            BoardState initialState = new BoardState(initialArray);
            System.out.println("Initial State (Layout: " + initialState.getLayout() + "):");
            BoardSerializer.printBoard(initialState.getBoardArray());

            KlotskiSolverASearchHashMap solver = new KlotskiSolverASearchHashMap();

            // --- Phase 1: Initial Solve ---
            System.out.println("\n--- Phase 1: Initial Solve ---");
            long startTime = System.currentTimeMillis();
            List<BoardState> initialSolution = solver.solve(initialState);
            long endTime = System.currentTimeMillis();
            System.out.println("\nInitial solve finished in " + (endTime - startTime) + " ms.");
            System.out.println("Initial solve explored nodes: " + solver.getNodesExplored());

            if (initialSolution.isEmpty()) {
                System.out.println("Initial solve found no solution.");
                return;
            }
            System.out.println("Initial solution found with " + (initialSolution.size() - 1) + " steps.");


            // --- Phase 2: Guidance ---
            System.out.println("\n--- Phase 2: Simulate Gameplay & Request Guidance ---");
            BoardState intermediateState;
            int stepForGuidance = Math.min(10, initialSolution.size() / 2); // Example step
            if (initialSolution.size() > stepForGuidance && stepForGuidance > 0) {
                intermediateState = initialSolution.get(stepForGuidance);
                System.out.println("\nSimulating user reaching state after " + stepForGuidance + " optimal steps (Layout: " + intermediateState.getLayout() + "):");
                BoardSerializer.printBoard(intermediateState.getBoardArray());
            } else {
                intermediateState = initialState;
                System.out.println("\nUsing initial state for guidance test.");
            }

            startTime = System.currentTimeMillis();
            List<BoardState> optimalPathFromCurrent = solver.findOptimalPathFromCurrent(intermediateState);
            endTime = System.currentTimeMillis();
            System.out.println("\nGuidance calculation finished in " + (endTime - startTime) + " ms.");
            System.out.println("Guidance calculation explored nodes: " + solver.getNodesExplored());

            if (!optimalPathFromCurrent.isEmpty()) {
                System.out.println("Optimal path from current state has " + (optimalPathFromCurrent.size() - 1) + " remaining steps.");
                if (optimalPathFromCurrent.size() > 1) {
                    System.out.println("Suggested next optimal move leads to state (Layout: " + optimalPathFromCurrent.get(1).getLayout() + "):");
                    BoardSerializer.printBoard(optimalPathFromCurrent.get(1).getBoardArray());
                }
            } else {
                System.out.println("No path to goal found from the current state.");
            }

        } catch (Exception e) {
            System.err.println("An error occurred in main:");
            e.printStackTrace();
        }
    }
}