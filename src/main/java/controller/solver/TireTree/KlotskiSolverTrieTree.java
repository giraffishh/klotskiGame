package controller.solver.TireTree;

import controller.solver.BoardSerializer;
import controller.solver.BoardState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
// No longer using Queue, Map, Set from java.util in the core solver logic


/**
 * Node for the Trie structure. Each path of depth 20 represents a board layout.
 * Uses piece types 0-4 from BoardSerializer as indices.
 */
class TrieNode {
    // Children nodes corresponding to piece types 0 (EMPTY) to 4 (CAO_CAO)
    TrieNode[] children = new TrieNode[5];
    // Link to the ElementNode if this path represents a visited state in the BFS
    ElementNode elementNodeLink = null;
}

/**
 * Node representing a state in the BFS search tree (Element Node from literature).
 */
class ElementNode {
    BoardState state;
    ElementNode father; // Predecessor in the BFS path
    ElementNode nextInLevel; // The 'lever' pointer for the BFS level linked list
    int moveCount; // Depth in the BFS

    ElementNode(BoardState state, ElementNode father, int moveCount) {
        this.state = state;
        this.father = father;
        this.moveCount = moveCount;
        this.nextInLevel = null;
    }
}


/**
 * Solves the Klotski (Hua Rong Dao) puzzle using BFS with a Trie for visited set
 * and symmetry checking, based on literature description.
 */
public class KlotskiSolverTrieTree {

    private static final int[] DR = {-1, 1, 0, 0}; // UP, DOWN, LEFT, RIGHT
    private static final int[] DC = {0, 0, -1, 1};
    private final TrieNode trieRoot = new TrieNode(); // Root of the visited state Trie
    private int nodesExplored = 0; // 添加节点计数变量

    /**
     * 返回探索的节点数量
     * @return 算法执行过程中探索的节点数量
     */
    public int getNodesExplored() {
        return nodesExplored;
    }

    /**
     * Checks if the given board state represents the winning condition.
     * (Identical to previous versions)
     */
    private boolean isGoalState(BoardState state) {
        int[][] board = state.getBoardArray();
        return board[3][1] == BoardSerializer.CAO_CAO &&
                board[3][2] == BoardSerializer.CAO_CAO &&
                board[4][1] == BoardSerializer.CAO_CAO &&
                board[4][2] == BoardSerializer.CAO_CAO;
    }

    /**
     * Generates the horizontally symmetric board state.
     * @param state Original board state.
     * @return The symmetric board state.
     */
    private BoardState getSymmetricState(BoardState state) {
        int[][] originalBoard = state.getBoardArray();
        int[][] symmetricBoard = new int[BoardSerializer.ROWS][BoardSerializer.COLS];

        for (int r = 0; r < BoardSerializer.ROWS; r++) {
            for (int c = 0; c < BoardSerializer.COLS; c++) {
                symmetricBoard[r][BoardSerializer.COLS - 1 - c] = originalBoard[r][c];
                // Note: Piece types themselves don't change value upon reflection
            }
        }
        // Need to create a new BoardStateTrieTree, which recalculates the layout 'long'
        return new BoardState(symmetricBoard);
    }


    /**
     * Inserts a board state layout into the Trie and links the final node
     * to the corresponding ElementNode.
     * @param root The root of the Trie.
     * @param state The board state to insert.
     * @param elementToLink The ElementNode representing this state in the BFS.
     */
    private void trieInsert(TrieNode root, BoardState state, ElementNode elementToLink) {
        TrieNode current = root;
        int[][] board = state.getBoardArray(); // Use array for sequence

        for (int r = 0; r < BoardSerializer.ROWS; r++) {
            for (int c = 0; c < BoardSerializer.COLS; c++) {
                int pieceType = board[r][c];
                if (pieceType < 0 || pieceType > 4) {
                    // Should not happen with valid BoardSerializer constants
                    throw new IllegalArgumentException("Invalid piece type " + pieceType + " during Trie insert.");
                }
                if (current.children[pieceType] == null) {
                    current.children[pieceType] = new TrieNode();
                }
                current = current.children[pieceType];
            }
        }
        // Link the final node in the path to the ElementNode
        current.elementNodeLink = elementToLink;
    }

    /**
     * Looks up a board state layout in the Trie.
     * @param root The root of the Trie.
     * @param state The board state to look up.
     * @return The linked ElementNode if the state exists in the Trie, otherwise null.
     */
    private ElementNode trieLookup(TrieNode root, BoardState state) {
        TrieNode current = root;
        int[][] board = state.getBoardArray(); // Use array for sequence

        for (int r = 0; r < BoardSerializer.ROWS; r++) {
            for (int c = 0; c < BoardSerializer.COLS; c++) {
                int pieceType = board[r][c];
                if (pieceType < 0 || pieceType > 4) {
                    // Defensive check
                    return null; // Invalid path
                }
                if (current.children[pieceType] == null) {
                    return null; // Path does not exist
                }
                current = current.children[pieceType];
            }
        }
        // Return the linked ElementNode at the end of the path
        return current.elementNodeLink;
    }


    /**
     * Solves the Klotski puzzle using BFS with Trie and symmetry checking.
     *
     * @param initialState The starting board configuration.
     * @return A list representing the shortest path, or empty list if no solution found.
     */
    public List<BoardState> solve(BoardState initialState) {
        // 1. Initialization
        if (isGoalState(initialState)) {
            return Collections.singletonList(initialState);
        }

        ElementNode initialElement = new ElementNode(initialState, null, 0);
        trieInsert(trieRoot, initialState, initialElement); // Insert initial state

        ElementNode currentLevelHead = initialElement;
        ElementNode nextLevelHead = null;
        ElementNode nextLevelTail = null;
        int currentDepth = 0;
        nodesExplored = 0; // 重置节点计数
        long statesAdded = 1;


        // 2. BFS Loop (Level by Level)
        while (currentLevelHead != null) {
            // System.out.println("Trie-BFS: Processing Depth: " + currentDepth + ", States in level: " + (statesAdded - statesProcessed) + ", Total added: " + statesAdded);
            ElementNode currentNode = currentLevelHead;

            while (currentNode != null) {
                nodesExplored++; // 增加节点计数

                // 3. Generate Successors
                List<BoardState> successors = generateSuccessors(currentNode.state);

                // 4. Process Successors
                for (BoardState successor : successors) {
                    // 4a. Goal Check
                    if (isGoalState(successor)) {
                        // System.out.println("Trie-BFS: Solution found at depth " + (currentDepth + 1) + " after processing " + statesProcessed + " states.");
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

                    // 4b. Visited Check (Original and Symmetric)
                    ElementNode existingElement = trieLookup(trieRoot, successor);
                    ElementNode symmetricExistingElement = null;
                    if (existingElement == null) { // Only check symmetry if original not found
                        BoardState symmetricSuccessor = getSymmetricState(successor);
                        // Avoid looking up self-symmetric states twice (though unlikely for full board)
                        if (!successor.equals(symmetricSuccessor)) {
                            symmetricExistingElement = trieLookup(trieRoot, symmetricSuccessor);
                        }
                    }

                    // 4c. If Not Visited (Neither original nor symmetric)
                    if (existingElement == null && symmetricExistingElement == null) {
                        statesAdded++;
                        // Create new ElementNode
                        ElementNode newElement = new ElementNode(successor, currentNode, currentDepth + 1);
                        // Insert into Trie
                        trieInsert(trieRoot, successor, newElement);
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

                // Move to the next node in the current level
                currentNode = currentNode.nextInLevel;
            } // End processing current level

            // 5. Move to Next Level
            currentLevelHead = nextLevelHead;
            nextLevelHead = null;
            nextLevelTail = null;
            currentDepth++;

        } // End BFS Loop

        System.out.println("Trie-BFS: No solution found after processing " + nodesExplored + " states.");
        return Collections.emptyList();
    }


    /**
     * Generates all valid next board states from the current state.
     * (Code is identical to the previous versions)
     */
    private List<BoardState> generateSuccessors(BoardState currentState) {
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

                // --- Piece identification logic (identical) ---
                int pieceHeight = 1, pieceWidth = 1;
                List<int[]> pieceCells = new ArrayList<>();
                boolean validPiece = true;
                switch (pieceType) {
                    case BoardSerializer.SOLDIER: pieceCells.add(new int[]{r, c}); processed[r][c] = true; break;
                    case BoardSerializer.HORIZONTAL: if (c + 1 < COLS && board[r][c+1] == pieceType) { pieceWidth = 2; pieceCells.add(new int[]{r, c}); pieceCells.add(new int[]{r, c + 1}); processed[r][c] = true; processed[r][c + 1] = true; } else validPiece = false; break;
                    case BoardSerializer.VERTICAL: if (r + 1 < ROWS && board[r+1][c] == pieceType) { pieceHeight = 2; pieceCells.add(new int[]{r, c}); pieceCells.add(new int[]{r + 1, c}); processed[r][c] = true; processed[r+1][c] = true; } else validPiece = false; break;
                    case BoardSerializer.CAO_CAO: if (r + 1 < ROWS && c + 1 < COLS && board[r][c+1] == pieceType && board[r+1][c] == pieceType && board[r+1][c+1] == pieceType) { pieceHeight = 2; pieceWidth = 2; pieceCells.add(new int[]{r, c}); pieceCells.add(new int[]{r, c + 1}); pieceCells.add(new int[]{r + 1, c}); pieceCells.add(new int[]{r + 1, c + 1}); processed[r][c] = true; processed[r][c+1] = true; processed[r+1][c] = true; processed[r+1][c+1] = true; } else validPiece = false; break;
                    default: validPiece = false; break;
                }
                if (!validPiece) continue;
                // --- End Piece identification ---


                // --- Move generation logic (identical) ---
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
                        successors.add(new BoardState(nextBoard)); // Creates new state (and layout long)
                    }
                }
                // --- End Move generation ---
            }
        }
        return successors;
    }

    // --- Main method for testing ---
    public static void main(String[] args) {
        System.out.println("Klotski Solver with Trie-BFS and Symmetry Checking Test");

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

            KlotskiSolverTrieTree solver = new KlotskiSolverTrieTree(); // Creates a new solver with an empty Trie

            System.out.println("\nStarting Trie-BFS solve...");
            long startTime = System.currentTimeMillis();
            List<BoardState> path = solver.solve(initialState);
            long endTime = System.currentTimeMillis();
            System.out.println("\nSolve finished in " + (endTime - startTime) + " ms.");

            if (path.isEmpty()) {
                System.out.println("No solution found.");
            } else {
                System.out.println("Solution found with " + (path.size() - 1) + " steps.");
                System.out.println("\nFinal State (Step " + (path.size() - 1) + "):");
                BoardSerializer.printBoard(path.get(path.size() - 1).getBoardArray());
            }

            /*
            int step = 0;
            for (BoardStateTrieTree state : path) {
                System.out.println("\nStep " + step++);
                BoardSerializer.printBoard(state.getBoardArray());
                // System.out.println("  Layout: " + state.getLayout());
            }
            */

        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
