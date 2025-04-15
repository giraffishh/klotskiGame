package controller.solver.TireTree; // New package

import controller.solver.BoardSerializer;
import controller.solver.BoardState;

import java.util.*;

// --- TrieNode definition (remains same, links to ElementNode) ---
class TrieNode {
    TrieNode[] children = new TrieNode[5];
    ElementNode elementNodeLink = null;
}

// --- ElementNode definition for BFS (remains same) ---
class ElementNode {
    BoardState state;
    ElementNode father;
    ElementNode nextInLevel; // Still used for level-by-level BFS
    int moveCount;

    ElementNode(BoardState state, ElementNode father, int moveCount) {
        this.state = state;
        this.father = father;
        this.moveCount = moveCount;
        this.nextInLevel = null;
    }
}

/**
 * Optimized BFS with Trie and Symmetry Checking.
 * Uses direct layout calculation for symmetry.
 */
public class KlotskiSolverTrieTree {

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
        // Optimization: Check layout directly if target layout is known and fixed
        // if (state.getLayout() == TARGET_LAYOUT) return true;
        // Otherwise, deserialize (or cache)
        int[][] board = state.getBoardArray(); // Uses cache if available
        return board[3][1] == BoardSerializer.CAO_CAO &&
                board[3][2] == BoardSerializer.CAO_CAO &&
                board[4][1] == BoardSerializer.CAO_CAO &&
                board[4][2] == BoardSerializer.CAO_CAO;
    }


    /**
     * Inserts a board state into the Trie using its canonical layout.
     * Links the final node to the corresponding ElementNode.
     * Traverses Trie using bit manipulation on the layout.
     *
     * @param root The root of the Trie.
     * @param state The board state to insert (used to get layout).
     * @param elementToLink The ElementNode representing this state in the BFS.
     */
    private void trieInsert(TrieNode root, BoardState state, ElementNode elementToLink) {
        long layout = state.getLayout();
        long symmetricLayout = getSymmetricLayout(layout);
        long canonicalLayout = Math.min(layout, symmetricLayout);

        TrieNode current = root;
        final int BITS_PER_CELL = 3;
        final int CELLS_COUNT = BoardSerializer.ROWS * BoardSerializer.COLS; // 20

        for (int i = 0; i < CELLS_COUNT; i++) {
            int pieceType = (int)((canonicalLayout >> (i * BITS_PER_CELL)) & ((1L << BITS_PER_CELL) - 1L));
            if (pieceType < 0 || pieceType > 4) { // Should not happen
                throw new IllegalArgumentException("Invalid piece type " + pieceType + " derived from layout during Trie insert.");
            }
            if (current.children[pieceType] == null) {
                current.children[pieceType] = new TrieNode();
            }
            current = current.children[pieceType];
        }
        // Link only if not already linked (first encounter is shortest in BFS)
        if (current.elementNodeLink == null) {
            current.elementNodeLink = elementToLink;
        }
    }

    /**
     * Looks up a board state in the Trie using its canonical layout.
     * Traverses Trie using bit manipulation on the layout.
     *
     * @param root The root of the Trie.
     * @param state The board state to look up.
     * @return The linked ElementNode if the canonical form exists, otherwise null.
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
            if (pieceType < 0 || pieceType > 4) return null; // Invalid path derived
            if (current.children[pieceType] == null) return null; // Path does not exist
            current = current.children[pieceType];
        }
        return current.elementNodeLink;
    }


    /**
     * Solves the Klotski puzzle using Optimized BFS with Trie and symmetry checking.
     */
    public List<BoardState> solve(BoardState initialState) {
        // Reset Trie for this solve instance
        this.trieRoot = new TrieNode();
        this.nodesExplored = 0;

        if (isGoalState(initialState)) {
            return Collections.singletonList(initialState);
        }

        ElementNode initialElement = new ElementNode(initialState, null, 0);
        trieInsert(trieRoot, initialState, initialElement); // Insert initial state using canonical layout

        ElementNode currentLevelHead = initialElement;
        ElementNode nextLevelHead = null;
        ElementNode nextLevelTail = null;
        int currentDepth = 0;
        long statesAdded = 1;

        while (currentLevelHead != null) {
            ElementNode currentNode = currentLevelHead;

            while (currentNode != null) {
                nodesExplored++;

                List<BoardState> successors = generateSuccessors(currentNode.state);

                for (BoardState successor : successors) {
                    if (isGoalState(successor)) {
                        // Reconstruct path
                        LinkedList<BoardState> path = new LinkedList<>();
                        path.addFirst(successor);
                        ElementNode trace = currentNode;
                        while (trace != null) {
                            path.addFirst(trace.state);
                            trace = trace.father;
                        }
                        return path;
                    }

                    // Optimized Visited Check using canonical layout lookup
                    ElementNode existingElement = trieLookup(trieRoot, successor);

                    // If Not Visited (canonical form not in Trie)
                    if (existingElement == null) {
                        statesAdded++;
                        ElementNode newElement = new ElementNode(successor, currentNode, currentDepth + 1);
                        trieInsert(trieRoot, successor, newElement); // Insert using canonical layout

                        // Add to next level's linked list
                        if (nextLevelHead == null) {
                            nextLevelHead = newElement;
                            nextLevelTail = newElement;
                        } else {
                            nextLevelTail.nextInLevel = newElement;
                            nextLevelTail = newElement;
                        }
                    }
                } // End processing successors

                currentNode = currentNode.nextInLevel;
            } // End processing current level

            currentLevelHead = nextLevelHead;
            nextLevelHead = null;
            nextLevelTail = null;
            currentDepth++;

            if (nodesExplored % 200000 == 0) { // Progress indicator
                System.out.println("BFS-TrieOpt Nodes: " + nodesExplored + ", Depth: " + currentDepth + ", Added: " + statesAdded);
            }

        } // End BFS Loop

        System.out.println("BFS-TrieOpt: No solution found after processing " + nodesExplored + " states.");
        return Collections.emptyList();
    }


    /**
     * Generates successors (Identical - still needs BoardState creation)
     */
    private List<BoardState> generateSuccessors(BoardState currentState) {
        // --- Identical code as before ---
        List<BoardState> successors = new ArrayList<>();
        final int ROWS = BoardSerializer.ROWS;
        final int COLS = BoardSerializer.COLS;
        final int EMPTY = BoardSerializer.EMPTY;
        int[][] board = currentState.getBoardArray(); // Still needs array for move logic
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
                        successors.add(new BoardState(nextBoard)); // Creates new state
                    }
                }
            }
        }
        return successors;
        // --- End identical code ---
    }

    // --- Main method for testing (Similar setup) ---
    public static void main(String[] args) {
        System.out.println("Klotski Solver with Optimized Trie-BFS and Symmetry Checking Test");
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

            KlotskiSolverTrieTree solver = new KlotskiSolverTrieTree();

            System.out.println("\nStarting Optimized Trie-BFS solve...");
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