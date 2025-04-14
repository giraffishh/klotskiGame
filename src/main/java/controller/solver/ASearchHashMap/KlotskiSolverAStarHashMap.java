package controller.solver.ASearchHashMap;

import controller.solver.BoardSerializer;
import controller.solver.BoardState;

import java.util.*; // Import necessary classes


// ElementNode class remains the same as in the previous A* HashMap version

/**
 * Solves the Klotski puzzle using A* search with a HashMap for the visited set.
 * Returns List<BoardState> directly.
 */
public class KlotskiSolverAStarHashMap {

    // Constants DR/DC, isGoalState, calculateHeuristic, generateSuccessors remain the same

    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};

    private int nodesExplored = 0; // 添加节点计数变量

    private boolean isGoalState(BoardState state) { /* ... same ... */
        int[][] board = state.getBoardArray();
        return board[3][1] == BoardSerializer.CAO_CAO && board[3][2] == BoardSerializer.CAO_CAO &&
                board[4][1] == BoardSerializer.CAO_CAO && board[4][2] == BoardSerializer.CAO_CAO;
    }
    private int calculateHeuristic(BoardState state) { /* ... same ... */
        int[][] board = state.getBoardArray();
        for (int r = 0; r < BoardSerializer.ROWS - 1; r++) {
            for (int c = 0; c < BoardSerializer.COLS - 1; c++) {
                if (board[r][c] == BoardSerializer.CAO_CAO) {
                    return Math.abs(r - 3) + Math.abs(c - 1);
                }
            }
        }
        return Integer.MAX_VALUE / 2;
    }

    /**
     * 获取求解过程中探索的节点数量
     * @return 探索的节点数
     */
    public int getNodesExplored() {
        return nodesExplored;
    }

    /**
     * Solves the Klotski puzzle using A* search with HashMap.
     *
     * @param initialState The starting board configuration.
     * @return A List of BoardState representing the path, or empty list if no solution.
     */
    public List<BoardState> solve(BoardState initialState) { // <--- Return type changed
        nodesExplored = 0; // 重置节点计数
        PriorityQueue<ElementNode> openSet = new PriorityQueue<>();
        Map<BoardState, ElementNode> visitedMap = new HashMap<>();

        int initialH = calculateHeuristic(initialState);
        ElementNode initialElement = new ElementNode(initialState, null, 0, initialH);

        openSet.add(initialElement);
        visitedMap.put(initialState, initialElement);

        nodesExplored = 1; // 初始节点

        while (!openSet.isEmpty()) {
            ElementNode currentNode = openSet.poll();

            if (isGoalState(currentNode.state)) {
                // System.out.println("A* HashMap: Solved. Processed: " + statesProcessed); // Optional debug
                LinkedList<BoardState> path = new LinkedList<>();
                ElementNode trace = currentNode;
                while (trace != null) {
                    path.addFirst(trace.state);
                    trace = trace.father;
                }
                return path; // <--- Return the path directly
            }

            List<BoardState> successors = generateSuccessors(currentNode.state);

            for (BoardState successor : successors) {
                int g_new = currentNode.g + 1;
                ElementNode existingNode = visitedMap.get(successor);

                if (existingNode != null) {
                    if (g_new < existingNode.g) {
                        existingNode.father = currentNode;
                        existingNode.g = g_new;
                        existingNode.f = g_new + existingNode.h;
                        boolean removed = openSet.remove(existingNode);
                        if (removed) {
                            openSet.add(existingNode);
                        }
                    }
                } else {
                    nodesExplored++; // 每当遇到新节点时增加计数
                    int h_new = calculateHeuristic(successor);
                    ElementNode newElement = new ElementNode(successor, currentNode, g_new, h_new);
                    openSet.add(newElement);
                    visitedMap.put(successor, newElement);
                }
            }
        }

        // System.out.println("A* HashMap: No solution. Processed: " + statesProcessed); // Optional debug
        return Collections.emptyList(); // <--- Return empty list for no solution
    }

    private List<BoardState> generateSuccessors(BoardState currentState) { /* ... same ... */
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
    }

    // ElementNode inner class (or defined separately)
    static class ElementNode implements Comparable<ElementNode> {
        BoardState state; ElementNode father; int g; int h; int f;
        ElementNode(BoardState state, ElementNode father, int g, int h) { this.state = state; this.father = father; this.g = g; this.h = h; this.f = g + h; }
        @Override public int compareTo(ElementNode other) { if (this.f != other.f) return Integer.compare(this.f, other.f); return Integer.compare(this.h, other.h); }
        @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; ElementNode that = (ElementNode) o; return state.equals(that.state); }
        @Override public int hashCode() { return state.hashCode(); }
    }

    // Optional main method for direct testing can remain similar, but check path directly
    public static void main(String[] args) {
        // ... (setup initial state) ...
        int[][] initialArray = {
                {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
                {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER}
        };
        BoardState initialState = new BoardState(initialArray);
        KlotskiSolverAStarHashMap solver = new KlotskiSolverAStarHashMap();
        long startTime = System.currentTimeMillis();
        List<BoardState> path = solver.solve(initialState);
        long endTime = System.currentTimeMillis();
        System.out.println("\nSolve finished in " + (endTime - startTime) + " ms.");
        if (path.isEmpty()) { System.out.println("No solution found."); }
        else { System.out.println("Solution found with " + (path.size() - 1) + " steps."); }
    }
}
