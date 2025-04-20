package controller.solverArchived.ASearchPDB;

import controller.solver.BoardSerializer;
import controller.solver.BoardState;
import controller.solverArchived.PatternDatabaseGenerator;

import java.util.*;
import java.io.File; // For checking file existence

/**
 * Node for the Trie structure.
 */
class TrieNode {
    TrieNode[] children = new TrieNode[5];
    ElementNode elementNodeLink = null;
}

/**
 * Node representing a state in the A* search tree.
 * Implements Comparable for use in PriorityQueue.
 */
class ElementNode implements Comparable<ElementNode> {
    BoardState state;
    ElementNode father; // Predecessor in the search path

    int g; // Cost from start to this node (move count)
    int h; // Heuristic estimate from this node to goal
    int f; // Estimated total cost (f = g + h)

    public ElementNode(BoardState state, ElementNode father, int g, int h) {
        this.state = state;
        this.father = father;
        this.g = g;
        this.h = h;
        this.f = g + h;
    }

    @Override
    public int compareTo(ElementNode other) {
        // Primary sort by f value (lower is better)
        if (this.f != other.f) {
            return Integer.compare(this.f, other.f);
        }
        // Tie-breaking: prefer lower h value (closer to goal heuristically)
        return Integer.compare(this.h, other.h);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElementNode that = (ElementNode) o;
        // Equality for PriorityQueue removal purposes should be based on state
        return state.equals(that.state);
    }

    @Override
    public int hashCode() {
        // HashCode for PriorityQueue removal purposes should be based on state
        return state.hashCode();
    }
}

public class KlotskiSolverPDB {

    private static final int[] DR = {-1, 1, 0, 0}; // UP, DOWN, LEFT, RIGHT
    private static final int[] DC = {0, 0, -1, 1};
    private TrieNode trieRoot; // 改为非静态，每次求解都重置
    private int nodesExplored = 0; // 节点计数变量

    // --- PDB Related Fields ---
    private static Map<Long, Integer> pdb_cc_4s = null; // Example: CC + 4 Soldiers PDB
    private static Map<Long, Integer> pdb_cc_2h_2v = null;  // Example: CC + Horizontal Block PDB
    private static final String PDB_CC_2H_4V_4S_FILENAME = "klotski_pdb_cc_2h_4v_4s.ser";
    private static final String PDB_CC_2H_2V_FILENAME = "klotski_pdb_cc_4s.ser"; // Adjust filename if needed
    private static boolean pdbsLoaded = false; // 标记PDB是否已加载

    // 静态初始化块，程序加载类时执行一次
    static {
        loadAllPDBs();
    }

    /**
     * 静态方法：加载所有PDB数据
     */
    private static void loadAllPDBs() {
        if (pdbsLoaded) return; // 如果已加载，直接返回

        System.out.println("Initializing KlotskiSolver PDBs (loading only once)...");
        PatternDatabaseGenerator pdbLoader = new PatternDatabaseGenerator(); // Helper for loading

        // Load CC+4S PDB
        if (new File(PDB_CC_2H_4V_4S_FILENAME).exists()) {
            pdb_cc_4s = pdbLoader.loadPDB(PDB_CC_2H_4V_4S_FILENAME);
            if (pdb_cc_4s == null) {
                System.err.println("Warning: Failed to load PDB: " + PDB_CC_2H_4V_4S_FILENAME);
            }
        } else {
            System.out.println("Info: PDB file not found, skipping: " + PDB_CC_2H_4V_4S_FILENAME);
        }

        // Load CC+H PDB
        if (new File(PDB_CC_2H_2V_FILENAME).exists()) {
            pdb_cc_2h_2v = pdbLoader.loadPDB(PDB_CC_2H_2V_FILENAME);
            if (pdb_cc_2h_2v == null) {
                System.err.println("Warning: Failed to load PDB: " + PDB_CC_2H_2V_FILENAME);
            }
        } else {
            System.out.println("Info: PDB file not found, skipping: " + PDB_CC_2H_2V_FILENAME);
        }

        pdbsLoaded = true; // 标记PDB已加载
        System.out.println("PDB loading complete!");
    }

    /**
     * Constructor: PDBs are now loaded via static initialization.
     */
    public KlotskiSolverPDB() {
        // PDB已通过静态初始化块加载，此处无需重复加载
        if (!pdbsLoaded) {
            System.out.println("Warning: PDBs were not loaded by static initializer, loading now...");
            loadAllPDBs();
        }
        // 确保每个实例都有自己的Trie树
        this.trieRoot = new TrieNode();
        this.nodesExplored = 0;
    }

    /**
     * Checks if the given board state represents the winning condition.
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
     */
    private BoardState getSymmetricState(BoardState state) {
        int[][] originalBoard = state.getBoardArray();
        int[][] symmetricBoard = new int[BoardSerializer.ROWS][BoardSerializer.COLS];
        for (int r = 0; r < BoardSerializer.ROWS; r++) {
            for (int c = 0; c < BoardSerializer.COLS; c++) {
                symmetricBoard[r][BoardSerializer.COLS - 1 - c] = originalBoard[r][c];
            }
        }
        return new BoardState(symmetricBoard);
    }

    /**
     * Inserts/Updates a board state layout in the Trie, linking to the ElementNode.
     */
    private void trieInsert(TrieNode root, BoardState state, ElementNode elementToLink) {
        TrieNode current = root;
        int[][] board = state.getBoardArray();
        for (int r = 0; r < BoardSerializer.ROWS; r++) {
            for (int c = 0; c < BoardSerializer.COLS; c++) {
                int pieceType = board[r][c];
                if (pieceType < 0 || pieceType > 4) throw new IllegalArgumentException("Invalid piece type " + pieceType);
                if (current.children[pieceType] == null) current.children[pieceType] = new TrieNode();
                current = current.children[pieceType];
            }
        }
        current.elementNodeLink = elementToLink;
    }

    /**
     * Looks up a board state layout in the Trie.
     */
    private ElementNode trieLookup(TrieNode root, BoardState state) {
        TrieNode current = root;
        int[][] board = state.getBoardArray();
        for (int r = 0; r < BoardSerializer.ROWS; r++) {
            for (int c = 0; c < BoardSerializer.COLS; c++) {
                int pieceType = board[r][c];
                if (pieceType < 0 || pieceType > 4) return null;
                if (current.children[pieceType] == null) return null;
                current = current.children[pieceType];
            }
        }
        return current.elementNodeLink;
    }
    
    /**
     * 返回探索的节点数量
     * @return 算法执行过程中探索的节点数量
     */
    public int getNodesExplored() {
        return nodesExplored;
    }

    /**
     * Calculates the heuristic (h) value for a state using loaded PDBs and Manhattan distance.
     * Returns the maximum value found among all applicable heuristics.
     */
    private int calculateHeuristic(BoardState state) {
        int h_manhattan = 0;
        // Calculate basic Manhattan distance (Cao Cao top-left to goal (3,1))
        int[][] board = state.getBoardArray(); // Getting array only once
        boolean caoCaoFound = false;
        for (int r = 0; r < BoardSerializer.ROWS - 1 && !caoCaoFound; r++) {
            for (int c = 0; c < BoardSerializer.COLS - 1; c++) {
                if (board[r][c] == BoardSerializer.CAO_CAO) {
                    h_manhattan = Math.abs(r - 3) + Math.abs(c - 1);
                    caoCaoFound = true;
                    break;
                }
            }
        }
        if (!caoCaoFound) h_manhattan = Integer.MAX_VALUE / 2; // Should not happen for valid states

        int max_h = h_manhattan; // Start with Manhattan distance

        // --- PDB Lookup: CC + 4S ---
        if (this.pdb_cc_4s != null) {
            // Use the correct pattern extraction method from PatternDatabaseGenerator
            long patternLayout_4s = PatternDatabaseGenerator.getPatternLayout(board); // Assuming this extracts CC+4S
            if (patternLayout_4s != -1L) { // Check if extraction was valid
                int h_pdb_4s = this.pdb_cc_4s.getOrDefault(patternLayout_4s, 0);
                max_h = Math.max(max_h, h_pdb_4s);
                //System.out.printf("  State %d: h_man=%d, h_4s=%d -> max_h=%d\n", state.getLayout(), h_manhattan, h_pdb_4s, max_h);
            }
        }

        // --- PDB Lookup: CC + H ---
        if (this.pdb_cc_2h_2v != null) {
            long patternLayout_h = getPatternLayout_CC_H(board); 
            if (patternLayout_h != -1L) {
                int h_pdb_h = this.pdb_cc_2h_2v.getOrDefault(patternLayout_h, 0);
                max_h = Math.max(max_h, h_pdb_h);
                //System.out.printf("  State %d: h_man=%d, h_h=%d -> max_h=%d\n", state.getLayout(), h_manhattan, h_pdb_h, max_h);
            }
        }

        // --- Add lookups for other PDBs similarly ---

        return max_h; // Return the overall maximum heuristic value
    }

    // --- Method to extract CC + H pattern ---
    private static long getPatternLayout_CC_H(int[][] fullBoardArray) {
        Set<Integer> patternPieces = new HashSet<>(Arrays.asList(BoardSerializer.CAO_CAO, BoardSerializer.HORIZONTAL));
        int[][] patternBoard = new int[BoardSerializer.ROWS][BoardSerializer.COLS];
        for (int r = 0; r < BoardSerializer.ROWS; r++) {
            for (int c = 0; c < BoardSerializer.COLS; c++) {
                if (fullBoardArray[r] != null && fullBoardArray[r].length == BoardSerializer.COLS) { // Basic check
                    int pieceType = fullBoardArray[r][c];
                    if (patternPieces.contains(pieceType)) {
                        patternBoard[r][c] = pieceType;
                    }
                } else return -1L; // Invalid input array
            }
        }
        try {
            return BoardSerializer.serialize(patternBoard);
        } catch (IllegalArgumentException e) { return -1L; }
    }

    /**
     * Solves the Klotski puzzle using A* search with Trie, Symmetry, and PDB heuristics.
     */
    public List<BoardState> solve(BoardState initialState) {
        // 重置求解器状态
        this.trieRoot = new TrieNode();
        this.nodesExplored = 0;

        // 1. Initialization
        if (isGoalState(initialState)) {
            return Collections.singletonList(initialState);
        }

        PriorityQueue<ElementNode> openSet = new PriorityQueue<>();

        int initialH = calculateHeuristic(initialState);
        ElementNode initialElement = new ElementNode(initialState, null, 0, initialH);

        openSet.add(initialElement);
        trieInsert(trieRoot, initialState, initialElement); // Add initial state to Trie

        nodesExplored = 0; // 重置节点计数
        long statesAdded = 1;

        // 2. A* Search Loop
        while (!openSet.isEmpty()) {
            // Get node with lowest f value
            ElementNode currentNode = openSet.poll();
            nodesExplored++; // 增加已探索节点计数

            // 2a. Goal Check (Check when polling)
            if (isGoalState(currentNode.state)) {
                // Reconstruct path
                LinkedList<BoardState> path = new LinkedList<>();
                ElementNode trace = currentNode;
                while (trace != null) {
                    path.addFirst(trace.state);
                    trace = trace.father;
                }
                return path;
            }

            if (nodesExplored % 100000 == 0) { // Progress indicator
                System.out.println("A* PDB: Processed: " + nodesExplored + ", OpenSet size: " + openSet.size() + ", Current f=" + currentNode.f + " (g=" + currentNode.g + ", h=" + currentNode.h + ")");
            }

            // 3. Generate Successors
            List<BoardState> successors = generateSuccessors(currentNode.state);

            // 4. Process Successors
            for (BoardState successor : successors) {

                int g_new = currentNode.g + 1;

                // 4a. Visited Check (Original and Symmetric) using Trie
                ElementNode existingElement = trieLookup(trieRoot, successor);
                ElementNode symmetricExistingElement = null;
                if (existingElement == null) {
                    BoardState symmetricSuccessor = getSymmetricState(successor);
                    // Avoid redundant lookup if state is self-symmetric
                    if (!successor.equals(symmetricSuccessor)) {
                        symmetricExistingElement = trieLookup(trieRoot, symmetricSuccessor);
                    }
                }

                ElementNode elementToUpdate = null; // The element in the Trie (original or symmetric)
                boolean foundShorterPath = false;

                if (existingElement != null) { // Found original state
                    if (g_new < existingElement.g) {
                        elementToUpdate = existingElement;
                        foundShorterPath = true;
                    } else {
                        continue; // Found original but not a shorter path, skip
                    }
                } else if (symmetricExistingElement != null) { // Found symmetric state
                    if (g_new < symmetricExistingElement.g) {
                        // Treat finding a shorter path to the symmetric state
                        // as finding a shorter path to *this* state conceptually.
                        // Update the *symmetric* element's info in the Trie.
                        elementToUpdate = symmetricExistingElement;
                        foundShorterPath = true;
                    } else {
                        continue; // Found symmetric but not a shorter path, skip
                    }
                } else {
                    // 4b. Neither Original nor Symmetric Found: Add to open set and Trie
                    statesAdded++;
                    int h_new = calculateHeuristic(successor);
                    ElementNode newElement = new ElementNode(successor, currentNode, g_new, h_new);
                    openSet.add(newElement);
                    trieInsert(trieRoot, successor, newElement); // Insert the *original* state into Trie
                }

                // 4c. Shorter Path Found: Update node and queue
                if (foundShorterPath && elementToUpdate != null) {
                    // Update father and g cost
                    elementToUpdate.father = currentNode;
                    elementToUpdate.g = g_new;
                    elementToUpdate.f = g_new + elementToUpdate.h; // h remains the same for the state

                    // Re-prioritize in the open set: Remove and re-add
                    // This is crucial for A* correctness
                    boolean removed = openSet.remove(elementToUpdate); // remove uses equals() based on BoardState
                    if (removed) {
                        openSet.add(elementToUpdate); // Re-add with updated f value
                    }
                }

            } // End processing successors

        } // End A* Search Loop

        System.out.println("A* PDB: No solution found after processing " + nodesExplored + " states.");
        return Collections.emptyList();
    }

    /**
     * Generates all valid next board states from the current state.
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
    }

    // --- Main method for testing ---
    public static void main(String[] args) {
        System.out.println("Klotski Solver with A* Search, Trie, PDB, and Symmetry Checking Test");

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

            KlotskiSolverPDB solver = new KlotskiSolverPDB();

            System.out.println("\nStarting A* PDB solve...");
            long startTime = System.currentTimeMillis();
            List<BoardState> path = solver.solve(initialState);
            long endTime = System.currentTimeMillis();
            System.out.println("\nSolve finished in " + (endTime - startTime) + " ms.");

            if (path.isEmpty()) {
                System.out.println("No solution found.");
            } else {
                System.out.println("Solution found with " + (path.size() - 1) + " steps.");
                System.out.println("Nodes explored: " + solver.getNodesExplored());
                System.out.println("\nFinal State (Step " + (path.size() - 1) + "):");
                BoardSerializer.printBoard(path.get(path.size() - 1).getBoardArray());
            }

        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
