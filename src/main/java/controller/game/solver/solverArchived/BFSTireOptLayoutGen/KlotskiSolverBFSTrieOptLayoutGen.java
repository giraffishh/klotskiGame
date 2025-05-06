package controller.game.solver.solverArchived.BFSTireOptLayoutGen; // Consistent package

import controller.util.BoardSerializer;
import controller.game.solver.BoardState;

import java.util.*;

// --- TrieNode definition --- (Keep as is)
class TrieNode {
    TrieNode[] children = new TrieNode[5]; // 0:Empty, 1:Soldier, 2:Vertical, 3:Horizontal, 4:CaoCao (Based on CODE_*)
    ElementNode elementNodeLink = null;
}

// --- ElementNode definition --- (Keep as is)
class ElementNode {
    BoardState state;
    ElementNode father;
    int moveCount;

    public ElementNode(BoardState state, ElementNode father, int moveCount) {
        this.state = state;
        this.father = father;
        this.moveCount = moveCount;
    }
}

/**
 * V3.3: Optimized BFS + Trie + Symmetry + Direct Layout Operations.
 * Guidance function directly calls the core solve method.
 * generateSuccessorLayouts now operates directly on the long layout.
 */
public class KlotskiSolverBFSTrieOptLayoutGen {

    // --- Constants ---
    private static final int[] DR = {-1, 1, 0, 0}; // Up, Down, Left, Right
    private static final int[] DC = {0, 0, -1, 1};
    private static final int ROWS = BoardSerializer.ROWS;
    private static final int COLS = BoardSerializer.COLS;
    private static final int BITS_PER_CELL = 3;
    private static final long CELL_MASK_3BIT = (1L << BITS_PER_CELL) - 1L; // 0b111 = 7L

    // Use codes directly from BoardSerializer for consistency
    private static final long CODE_EMPTY = BoardSerializer.arrayToCodeMap.get(BoardSerializer.EMPTY);         // 0
    private static final long CODE_SOLDIER = BoardSerializer.arrayToCodeMap.get(BoardSerializer.SOLDIER);     // 1
    private static final long CODE_VERTICAL = BoardSerializer.arrayToCodeMap.get(BoardSerializer.VERTICAL);   // 2
    private static final long CODE_HORIZONTAL = BoardSerializer.arrayToCodeMap.get(BoardSerializer.HORIZONTAL); // 3
    private static final long CODE_CAO_CAO = BoardSerializer.arrayToCodeMap.get(BoardSerializer.CAO_CAO);       // 4
    private static final long CODE_CAO_CAO_GOAL_CHECK = CODE_CAO_CAO; // Use the actual code for goal check

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
                // Place the code in the mirrored column c' = COLS - 1 - c
                int mirroredCol = COLS - 1 - c;
                int shift = (r * COLS + mirroredCol) * BITS_PER_CELL;
                symmetricLayout |= (cellCode << shift);
            }
        }
        return symmetricLayout;
    }

    /**
     * Generates successor layouts by directly manipulating the long representation.
     * Avoids creating intermediate int[][] arrays.
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
                    continue; // Should not happen with valid codes
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
     * Checks if the layout represents the goal state (Cao Cao at bottom center).
     * Uses direct bit checks.
     */
    private boolean isGoalLayout(long layout) {
        // Goal position for Cao Cao (2x2 block) is typically cells (3,1), (3,2), (4,1), (4,2)
        // Indices: (3*4+1)=13, (3*4+2)=14, (4*4+1)=17, (4*4+2)=18
        // Let's assume goal is CaoCao occupying bottom row, columns 1 and 2
        // Cell indices: row 4, col 1 => 4*4+1 = 17
        // Cell indices: row 4, col 2 => 4*4+2 = 18
        // We only need to check two cells uniquely identifying the goal position for CaoCao
        boolean cell17_ok = getCellCode(layout, 4, 1) == CODE_CAO_CAO_GOAL_CHECK;
        boolean cell18_ok = getCellCode(layout, 4, 2) == CODE_CAO_CAO_GOAL_CHECK;
        // Optionally, check the other two cells for robustness, though redundant if generation is correct
        // boolean cell13_ok = getCellCode(layout, 3, 1) == CODE_CAO_CAO_GOAL_CHECK;
        // boolean cell14_ok = getCellCode(layout, 3, 2) == CODE_CAO_CAO_GOAL_CHECK;
        // return cell13_ok && cell14_ok && cell17_ok && cell18_ok;
        return cell17_ok && cell18_ok; // Minimal check
    }

    /**
     * Inserts the canonical form of the layout into the Trie.
     */
    private static void trieInsertLayout(TrieNode root, long layoutToInsert, ElementNode elementToLink) {
        long symmetricLayout = getSymmetricLayout(layoutToInsert);
        long canonicalLayout = Math.min(layoutToInsert, symmetricLayout);
        TrieNode current = root;
        final int CELLS_COUNT = ROWS * COLS;

        for (int i = 0; i < CELLS_COUNT; i++) {
            int shift = i * BITS_PER_CELL;
            int pieceCodeIndex = (int)((canonicalLayout >> shift) & CELL_MASK_3BIT);

            // Ensure the index is valid for the children array
            if (pieceCodeIndex < 0 || pieceCodeIndex >= current.children.length) {
                System.err.println("Error: Invalid piece code index " + pieceCodeIndex + " derived from layout " + canonicalLayout + " at cell " + i);
                return; // Should not happen with valid layouts
            }

            if (current.children[pieceCodeIndex] == null) {
                current.children[pieceCodeIndex] = new TrieNode();
            }
            current = current.children[pieceCodeIndex];
        }
        // Only link if this state hasn't been reached before (or reached at same depth by BFS nature)
        if (current.elementNodeLink == null) {
            current.elementNodeLink = elementToLink;
        }
        // Optional: Could compare moveCount if allowing re-visiting, but BFS naturally finds shortest first.
        // else if (elementToLink.moveCount < current.elementNodeLink.moveCount) {
        //    current.elementNodeLink = elementToLink; // Update if a shorter path is found (relevant for non-BFS)
        // }
    }

    /**
     * Looks up the canonical form of the layout in the Trie.
     * Returns the linked ElementNode if found, otherwise null.
     */
    private static ElementNode trieLookupLayout(TrieNode root, long layoutToLookup) {
        long symmetricLayout = getSymmetricLayout(layoutToLookup);
        long canonicalLayout = Math.min(layoutToLookup, symmetricLayout);
        TrieNode current = root;
        final int CELLS_COUNT = ROWS * COLS;

        for (int i = 0; i < CELLS_COUNT; i++) {
            int shift = i * BITS_PER_CELL;
            int pieceCodeIndex = (int)((canonicalLayout >> shift) & CELL_MASK_3BIT);

            if (pieceCodeIndex < 0 || pieceCodeIndex >= current.children.length) return null; // Invalid code path
            if (current.children[pieceCodeIndex] == null) {
                return null; // State not found
            }
            current = current.children[pieceCodeIndex];
        }
        return current.elementNodeLink; // Return the node found at the end of the path
    }

    /**
     * Core solver: Finds the shortest path from the given startState to the goal
     * using BFS + Trie + Symmetry + Direct Layout Operations.
     */
    public List<BoardState> solve(BoardState startState) {
        this.nodesExplored = 0;
        long initialLayout = startState.getLayout();

        if (isGoalLayout(initialLayout)) {
            System.out.println("Start state is already the goal.");
            this.nodesExplored = 1;
            return Collections.singletonList(startState);
        }

        TrieNode localTrieRoot = new TrieNode();
        Queue<ElementNode> queue = new ArrayDeque<>();
        ElementNode initialElement = new ElementNode(startState, null, 0);

        trieInsertLayout(localTrieRoot, initialLayout, initialElement);
        queue.offer(initialElement);
        // long statesAdded = 1; // Not strictly needed for logic

        while (!queue.isEmpty()) {
            ElementNode currentNode = queue.poll();
            nodesExplored++;
            long currentLayout = currentNode.state.getLayout();
            int currentDepth = currentNode.moveCount;

            // Use the optimized successor generation
            List<Long> successorLayouts = generateSuccessorLayouts(currentLayout);

            for (long successorLayout : successorLayouts) {
                // Goal check *before* Trie lookup
                if (isGoalLayout(successorLayout)) {
                    BoardState goalState = new BoardState(successorLayout);
                    List<BoardState> path = reconstructPath(currentNode, goalState);
                    // System.out.println("Goal found! Path length: " + (path.size() -1) + ", Nodes explored: " + nodesExplored);
                    return path;
                }

                // Check if the successor (or its symmetric) is already visited in this search
                ElementNode existingElement = trieLookupLayout(localTrieRoot, successorLayout);

                if (existingElement == null) { // If not visited yet
                    // statesAdded++;
                    BoardState successorState = new BoardState(successorLayout);
                    ElementNode newElement = new ElementNode(successorState, currentNode, currentDepth + 1);
                    trieInsertLayout(localTrieRoot, successorLayout, newElement);
                    queue.offer(newElement);
                }
                // Else: Already visited via a path <= current path length. BFS ensures optimality. Ignore.
            }

            // Optional logging (adjust frequency as needed)
            if (nodesExplored % 500000 == 0) {
                System.out.println("BFS Search Nodes: " + nodesExplored + ", Queue: " + queue.size() + ", Approx Depth: " + currentDepth);
            }
        }

        System.out.println("BFS Search: No solution found after exploring " + nodesExplored + " states.");
        return Collections.emptyList();
    }

    // --- Guidance Function --- (Keep as is)
    public List<BoardState> findOptimalPathFromCurrent(BoardState currentState) {
        System.out.println("\n--- Requesting Guidance (calling core BFS solver) ---");
        return solve(currentState);
    }

    // --- Path Reconstruction --- (Keep as is)
    private List<BoardState> reconstructPath(ElementNode nodeBeforeGoal, BoardState goalState) {
        LinkedList<BoardState> path = new LinkedList<>();
        path.addFirst(goalState);
        ElementNode trace = nodeBeforeGoal;
        while (trace != null) {
            path.addFirst(trace.state);
            trace = trace.father;
        }
        return path;
    }

    // --- Main method for testing --- (Keep as is, uses BoardSerializer)
    public static void main(String[] args) {
        System.out.println("Klotski Solver V3.3 (BFS-Trie, Direct Layout Ops)");
        try {
            int[][] initialArray = {
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER}
            };
            BoardState initialState = new BoardState(initialArray);
            System.out.println("Initial State (Layout: " + initialState.getLayout() + "):");
            BoardSerializer.printBoard(initialState.getBoardArray()); // Still useful for visualization

            KlotskiSolverBFSTrieOptLayoutGen solver = new KlotskiSolverBFSTrieOptLayoutGen();

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
            // Optional: Print the solution path
            /*
            System.out.println("Solution Path:");
            for (int i = 0; i < initialSolution.size(); i++) {
                System.out.println("Step " + i + ":");
                BoardSerializer.printBoard(initialSolution.get(i).getBoardArray());
            }
            */

            // --- Phase 2: Guidance ---
            System.out.println("\n--- Phase 2: Simulate Gameplay & Request Guidance ---");
            BoardState intermediateState;
            int stepForGuidance = 10; // Example step
            if (initialSolution.size() > stepForGuidance) {
                intermediateState = initialSolution.get(stepForGuidance);
                System.out.println("\nSimulating user reaching state after " + stepForGuidance + " optimal steps (Layout: " + intermediateState.getLayout() + "):");
                BoardSerializer.printBoard(intermediateState.getBoardArray());
            } else {
                intermediateState = initialState;
                System.out.println("\nInitial solution too short, using initial state for guidance test.");
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
                System.out.println("No path to goal found from the current state (Should not happen if initial solution exists).");
            }

        } catch (Exception e) {
            System.err.println("An error occurred:");
            e.printStackTrace();
        }
    }
}