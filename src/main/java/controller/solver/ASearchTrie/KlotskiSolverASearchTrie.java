package controller.solver.ASearchTrie; // New package

import controller.solver.BoardSerializer;
import controller.solver.BoardState;

import java.util.*;

// --- TrieNode definition (remains same, links to ElementNode) ---
class TrieNode {
    TrieNode[] children = new TrieNode[5];
    ElementNode elementNodeLink = null;
}

// --- ElementNode definition for A* (remains same) ---
class ElementNode implements Comparable<ElementNode> {
    BoardState state;
    ElementNode father;
    int g, h, f;

    public ElementNode(BoardState state, ElementNode father, int g, int h) {
        this.state = state; this.father = father; this.g = g; this.h = h; this.f = g + h;
    }
    @Override public int compareTo(ElementNode other) {
        if (this.f != other.f) return Integer.compare(this.f, other.f);
        return Integer.compare(this.h, other.h); // Tie-break on h
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true; if (!(o instanceof ElementNode)) return false;
        return state.equals(((ElementNode) o).state);
    }
    @Override public int hashCode() { return state.hashCode(); }
}


/**
 * Optimized A* Search with Trie and Symmetry Checking.
 * Uses direct layout calculation for symmetry.
 */
public class KlotskiSolverASearchTrie {

    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};
    private TrieNode trieRoot; // Reset for each solve call
    private int nodesExplored = 0;

    public int getNodesExplored() {
        return nodesExplored;
    }

    /**
     * Calculates symmetric layout using bit manipulation.
     * (Identical to BiBFSSymmetryOptV2)
     */
    private long getSymmetricLayout(long layout) {
        long symmetricLayout = 0L;
        final int BITS_PER_CELL = 3;
        final int COLS = BoardSerializer.COLS;
        final int ROWS = BoardSerializer.ROWS;
        final long CELL_MASK = (1L << BITS_PER_CELL) - 1L;

        for (int r = 0; r < ROWS; r++) {
            long cell0 = (layout >> (r * COLS * BITS_PER_CELL + 0 * BITS_PER_CELL)) & CELL_MASK;
            long cell1 = (layout >> (r * COLS * BITS_PER_CELL + 1 * BITS_PER_CELL)) & CELL_MASK;
            long cell2 = (layout >> (r * COLS * BITS_PER_CELL + 2 * BITS_PER_CELL)) & CELL_MASK;
            long cell3 = (layout >> (r * COLS * BITS_PER_CELL + 3 * BITS_PER_CELL)) & CELL_MASK;

            symmetricLayout |= (cell3 << (r * COLS * BITS_PER_CELL + 0 * BITS_PER_CELL));
            symmetricLayout |= (cell2 << (r * COLS * BITS_PER_CELL + 1 * BITS_PER_CELL));
            symmetricLayout |= (cell1 << (r * COLS * BITS_PER_CELL + 2 * BITS_PER_CELL));
            symmetricLayout |= (cell0 << (r * COLS * BITS_PER_CELL + 3 * BITS_PER_CELL));
        }
        return symmetricLayout;
    }


    /**
     * Checks if the given board state represents the winning condition.
     * (Identical)
     */
    private boolean isGoalState(BoardState state) {
        int[][] board = state.getBoardArray();
        return board[3][1] == BoardSerializer.CAO_CAO &&
                board[3][2] == BoardSerializer.CAO_CAO &&
                board[4][1] == BoardSerializer.CAO_CAO &&
                board[4][2] == BoardSerializer.CAO_CAO;
    }

    /**
     * Calculates the heuristic (h) value: Manhattan distance.
     * (Identical)
     */
    private int calculateHeuristic(BoardState state) {
        int[][] board = state.getBoardArray(); // Still needs array for this simple heuristic
        for (int r = 0; r < BoardSerializer.ROWS - 1; r++) {
            for (int c = 0; c < BoardSerializer.COLS - 1; c++) {
                if (board[r][c] == BoardSerializer.CAO_CAO) {
                    return Math.abs(r - 3) + Math.abs(c - 1);
                }
            }
        }
        return Integer.MAX_VALUE / 2; // Should not happen
    }


    /**
     * Inserts/Updates a board state in the Trie using its canonical layout.
     * Links the final node to the corresponding ElementNode.
     * If the canonical path already exists, it *updates* the link if the new
     * element provides a shorter path (lower g value).
     * Traverses Trie using bit manipulation on the layout.
     *
     * @param root The root of the Trie.
     * @param state The board state to insert (used to get layout).
     * @param elementToLink The ElementNode representing this state in A*.
     * @return The existing ElementNode if found and not updated, or null otherwise.
     *         (Used to know if we need to update the PriorityQueue)
     */
    private ElementNode trieInsertOrUpdate(TrieNode root, BoardState state, ElementNode elementToLink) {
        long layout = state.getLayout();
        long symmetricLayout = getSymmetricLayout(layout);
        long canonicalLayout = Math.min(layout, symmetricLayout);

        TrieNode current = root;
        final int BITS_PER_CELL = 3;
        final int CELLS_COUNT = BoardSerializer.ROWS * BoardSerializer.COLS;

        for (int i = 0; i < CELLS_COUNT; i++) {
            int pieceType = (int)((canonicalLayout >> (i * BITS_PER_CELL)) & ((1L << BITS_PER_CELL) - 1L));
            if (pieceType < 0 || pieceType > 4) throw new IllegalArgumentException("Invalid piece type " + pieceType);
            if (current.children[pieceType] == null) current.children[pieceType] = new TrieNode();
            current = current.children[pieceType];
        }

        if (current.elementNodeLink == null) {
            // First time seeing this state (or its symmetric)
            current.elementNodeLink = elementToLink;
            return null; // Inserted new
        } else {
            // State (or symmetric) already exists, check if new path is shorter
            if (elementToLink.g < current.elementNodeLink.g) {
                // Update the link in the Trie to the better node
                ElementNode oldElement = current.elementNodeLink;
                current.elementNodeLink = elementToLink;
                return oldElement; // Return the old element that was replaced
            } else {
                // Existing path is shorter or equal, do nothing
                return current.elementNodeLink; // Return existing element (not updated)
            }
        }
    }

    /**
     * Looks up a board state in the Trie using its canonical layout.
     * Traverses Trie using bit manipulation on the layout.
     * (Identical to BFS Opt version)
     */
    private ElementNode trieLookup(TrieNode root, BoardState state) {
        long layout = state.getLayout();
        long symmetricLayout = getSymmetricLayout(layout);
        long canonicalLayout = Math.min(layout, symmetricLayout);

        TrieNode current = root;
        final int BITS_PER_CELL = 3;
        final int CELLS_COUNT = BoardSerializer.ROWS * BoardSerializer.COLS;

        for (int i = 0; i < CELLS_COUNT; i++) {
            int pieceType = (int)((canonicalLayout >> (i * BITS_PER_CELL)) & ((1L << BITS_PER_CELL) - 1L));
            if (pieceType < 0 || pieceType > 4) return null;
            if (current.children[pieceType] == null) return null;
            current = current.children[pieceType];
        }
        return current.elementNodeLink;
    }


    /**
     * Solves the Klotski puzzle using Optimized A* with Trie and symmetry checking.
     */
    public List<BoardState> solve(BoardState initialState) {
        this.trieRoot = new TrieNode(); // Reset Trie
        this.nodesExplored = 0;

        if (isGoalState(initialState)) return Collections.singletonList(initialState);

        PriorityQueue<ElementNode> openSet = new PriorityQueue<>();
        int initialH = calculateHeuristic(initialState);
        ElementNode initialElement = new ElementNode(initialState, null, 0, initialH);

        // Insert initial state - trieInsertOrUpdate handles canonical form
        trieInsertOrUpdate(trieRoot, initialState, initialElement);
        openSet.add(initialElement);

        long statesAdded = 1;

        while (!openSet.isEmpty()) {
            ElementNode currentNode = openSet.poll();
            nodesExplored++;

            // Goal check when polling (standard A*)
            if (isGoalState(currentNode.state)) {
                LinkedList<BoardState> path = new LinkedList<>();
                ElementNode trace = currentNode;
                while (trace != null) { path.addFirst(trace.state); trace = trace.father; }
                return path;
            }

            // Check if a shorter path to this node was found *after* it was added
            // to the open set but *before* it was polled. The Trie link would have
            // been updated, but the version in the queue is outdated.
            ElementNode currentElementInTrie = trieLookup(trieRoot, currentNode.state);
            if (currentElementInTrie != currentNode && currentElementInTrie.g < currentNode.g) {
                // A shorter path was found and updated in the Trie. Skip processing this outdated node.
                // System.out.println("Skipping outdated node from queue: " + currentNode.state.layout);
                continue;
            }


            if (nodesExplored % 100000 == 0) {
                System.out.println("A*-TrieOpt Nodes: " + nodesExplored + ", OpenSet: " + openSet.size() + ", f=" + currentNode.f + " (g=" + currentNode.g + ", h=" + currentNode.h + ")");
            }

            List<BoardState> successors = generateSuccessors(currentNode.state);

            for (BoardState successor : successors) {
                int g_new = currentNode.g + 1;
                int h_new = calculateHeuristic(successor); // Recalculate heuristic for successor
                ElementNode newElement = new ElementNode(successor, currentNode, g_new, h_new);

                // Try to insert/update using canonical layout
                ElementNode existingElement = trieInsertOrUpdate(trieRoot, successor, newElement);

                if (existingElement == null) {
                    // New state (considering symmetry) was inserted
                    openSet.add(newElement);
                    statesAdded++;
                } else if (existingElement != newElement) {
                    // Update occurred: newElement replaced existingElement in Trie
                    // Need to update the priority queue: remove old, add new
                    if (openSet.remove(existingElement)) { // Try removing the old element
                        openSet.add(newElement); // Add the new element with better g value
                        // System.out.println("Updated path in openSet for layout " + successor.getLayout());
                    } else {
                        // Old element wasn't in openSet (already processed/polled).
                        // Some A* variants might re-add the newElement here (re-opening),
                        // but standard practice is often not to. We follow that here.
                        // System.out.println("Shorter path found, but old node not in openSet for layout " + successor.getLayout());
                    }
                }
                // else: No update occurred, existing path was better or equal. Do nothing.

            } // End processing successors
        } // End A* loop

        System.out.println("A*-TrieOpt: No solution found after processing " + nodesExplored + " states.");
        return Collections.emptyList();
    }


    /**
     * Generates successors (Identical)
     */
    private List<BoardState> generateSuccessors(BoardState currentState) {
        // --- Identical code as before ---
        List<BoardState> successors = new ArrayList<>();
        final int ROWS = BoardSerializer.ROWS;
        final int COLS = BoardSerializer.COLS;
        final int EMPTY = BoardSerializer.EMPTY;
        int[][] board = currentState.getBoardArray();
        boolean[][] processed = new boolean[ROWS][COLS];

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int pieceType = board[r][c];
                if (pieceType == EMPTY || processed[r][c]) continue;

                List<int[]> pieceCells = new ArrayList<>();
                boolean validPiece = true;
                switch (pieceType) {
                    case BoardSerializer.SOLDIER: pieceCells.add(new int[]{r, c}); processed[r][c] = true; break;
                    case BoardSerializer.HORIZONTAL: if (c + 1 < COLS && board[r][c+1] == pieceType) { pieceCells.add(new int[]{r, c}); pieceCells.add(new int[]{r, c + 1}); processed[r][c] = true; processed[r][c + 1] = true; } else validPiece = false; break;
                    case BoardSerializer.VERTICAL: if (r + 1 < ROWS && board[r+1][c] == pieceType) { pieceCells.add(new int[]{r, c}); pieceCells.add(new int[]{r + 1, c}); processed[r][c] = true; processed[r+1][c] = true; } else validPiece = false; break;
                    case BoardSerializer.CAO_CAO: if (r + 1 < ROWS && c + 1 < COLS && board[r][c+1] == pieceType && board[r+1][c] == pieceType && board[r+1][c+1] == pieceType) { pieceCells.add(new int[]{r, c}); pieceCells.add(new int[]{r, c + 1}); pieceCells.add(new int[]{r + 1, c}); pieceCells.add(new int[]{r + 1, c + 1}); processed[r][c] = true; processed[r][c+1] = true; processed[r+1][c] = true; processed[r+1][c+1] = true; } else validPiece = false; break;
                    default: validPiece = false; break;
                }
                if (!validPiece) continue;

                for (int dir = 0; dir < 4; dir++) {
                    int dr = DR[dir], dc = DC[dir];
                    boolean canMove = true;
                    List<int[]> targetCells = new ArrayList<>();
                    for (int[] cell : pieceCells) { int nr = cell[0] + dr, nc = cell[1] + dc; if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS) { canMove = false; break; } targetCells.add(new int[]{nr, nc}); }
                    if (!canMove) continue;
                    for (int[] targetCell : targetCells) { int tr = targetCell[0], tc = targetCell[1]; boolean isPartOfCurrentPiece = false; for(int[] originalCell : pieceCells) if (tr == originalCell[0] && tc == originalCell[1]) { isPartOfCurrentPiece = true; break; } if (!isPartOfCurrentPiece && board[tr][tc] != EMPTY) { canMove = false; break; } }
                    if (canMove) {
                        int[][] nextBoard = new int[ROWS][COLS];
                        for (int i = 0; i < ROWS; i++) nextBoard[i] = Arrays.copyOf(board[i], COLS);
                        for (int[] cell : pieceCells) nextBoard[cell[0]][cell[1]] = EMPTY;
                        for (int[] cell : targetCells) nextBoard[cell[0]][cell[1]] = pieceType;
                        successors.add(new BoardState(nextBoard));
                    }
                }
            }
        }
        return successors;
        // --- End identical code ---
    }

    // --- Main method for testing (Similar setup) ---
    public static void main(String[] args) {
        System.out.println("Klotski Solver with Optimized A*-Trie and Symmetry Checking Test");
        try {
            int[][] initialArray = {
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER}
            };
            BoardState initialState = new BoardState(initialArray);
            System.out.println("Initial State:");
            BoardSerializer.printBoard(initialState.getBoardArray());

            KlotskiSolverASearchTrie solver = new KlotskiSolverASearchTrie();

            System.out.println("\nStarting Optimized A*-Trie solve...");
            long startTime = System.currentTimeMillis();
            List<BoardState> path = solver.solve(initialState);
            long endTime = System.currentTimeMillis();
            System.out.println("\nSolve finished in " + (endTime - startTime) + " ms.");
            System.out.println("Nodes explored: " + solver.getNodesExplored());

            if (path.isEmpty()) {
                System.out.println("No solution found.");
            } else {
                System.out.println("Solution found with " + (path.size() - 1) + " steps.");
                System.out.println("\nFinal State (Step " + (path.size() - 1) + "):");
                BoardSerializer.printBoard(path.get(path.size() - 1).getBoardArray());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}