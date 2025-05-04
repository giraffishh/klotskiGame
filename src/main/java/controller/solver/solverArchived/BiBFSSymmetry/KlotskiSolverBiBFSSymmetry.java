package controller.solver.solverArchived.BiBFSSymmetry; // Can keep same package or make V2

import controller.util.BoardSerializer;
import controller.solver.BoardState;

import java.util.*;

/**
 * Optimized version of Bi-BFS with HashMaps and Symmetry Checking.
 * Avoids creating full BoardState objects for symmetry checks by calculating
 * the symmetric layout directly using bit manipulation.
 * V2: Refined path reconstruction logic.
 */
public class KlotskiSolverBiBFSSymmetry {

    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};

    private int nodesExplored = 0;

    public int getNodesExplored() {
        return nodesExplored;
    }

    /**
     * Calculates the layout long of the horizontally symmetric board state
     * directly from the original layout long using bit manipulation.
     * (Identical to previous version)
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
     * Solves the Klotski puzzle using Optimized Bidirectional BFS with HashMaps and Symmetry.
     */
    public List<BoardState> solve(BoardState initialState, BoardState targetState) {
        if (initialState.equals(targetState)) {
            return Collections.singletonList(initialState);
        }

        Queue<BoardState> forwardQueue = new ArrayDeque<>();
        Map<Long, BoardState> forwardVisited = new HashMap<>(); // layout -> parent State

        Queue<BoardState> backwardQueue = new ArrayDeque<>();
        Map<Long, BoardState> backwardVisited = new HashMap<>(); // layout -> parent State

        long initialLayout = initialState.getLayout();
        forwardQueue.offer(initialState);
        forwardVisited.put(initialLayout, null);

        long targetLayout = targetState.getLayout();
        backwardQueue.offer(targetState);
        backwardVisited.put(targetLayout, null);

        BoardState meetingNode = null; // The state object where intersection occurred
        boolean foundByForwardSearch = false; // Flag to indicate which search found it
        boolean foundIntersection = false;
        nodesExplored = 0;

        while (!forwardQueue.isEmpty() && !backwardQueue.isEmpty() && !foundIntersection) {

            // --- Expand forward search by one level ---
            int forwardLevelSize = forwardQueue.size();
            for (int i = 0; i < forwardLevelSize && !foundIntersection; i++) {
                BoardState currentForward = forwardQueue.poll();
                long currentForwardLayout = currentForward.getLayout();
                nodesExplored++;

                // Check for intersection *before* generating successors (can sometimes happen)
                long currentForwardSymmetricLayout = getSymmetricLayout(currentForwardLayout);
                if (backwardVisited.containsKey(currentForwardLayout) || backwardVisited.containsKey(currentForwardSymmetricLayout)) {
                    meetingNode = currentForward; // Intersection is at currentForward
                    foundByForwardSearch = true; // Found during forward check
                    foundIntersection = true;
                    //System.out.println("Intersection found by Forward before generating!");
                    break;
                }

                List<BoardState> successors = generateSuccessors(currentForward);
                for (BoardState successor : successors) {
                    long successorLayout = successor.getLayout();
                    long successorSymmetricLayout = getSymmetricLayout(successorLayout);

                    // Check if visited by forward search (using layouts)
                    if (!forwardVisited.containsKey(successorLayout) && !forwardVisited.containsKey(successorSymmetricLayout)) {
                        forwardVisited.put(successorLayout, currentForward); // Store layout -> parent state
                        forwardQueue.offer(successor);

                        // Check for intersection immediately after adding (using layouts)
                        if (backwardVisited.containsKey(successorLayout) || backwardVisited.containsKey(successorSymmetricLayout)) {
                            meetingNode = successor; // Intersection is at the successor state
                            foundByForwardSearch = true; // Found after forward step added it
                            foundIntersection = true;
                            //System.out.println("Intersection found by Forward after adding!");
                            break; // Exit successor loop
                        }
                    }
                }
            }
            if (foundIntersection) break;

            // --- Expand backward search by one level ---
            int backwardLevelSize = backwardQueue.size();
            for (int i = 0; i < backwardLevelSize && !foundIntersection; i++) {
                BoardState currentBackward = backwardQueue.poll();
                long currentBackwardLayout = currentBackward.getLayout();
                nodesExplored++;

                // Check for intersection *before* generating successors
                long currentBackwardSymmetricLayout = getSymmetricLayout(currentBackwardLayout);
                if (forwardVisited.containsKey(currentBackwardLayout) || forwardVisited.containsKey(currentBackwardSymmetricLayout)) {
                    meetingNode = currentBackward; // Intersection is at currentBackward
                    foundByForwardSearch = false; // Found during backward check
                    foundIntersection = true;
                    //System.out.println("Intersection found by Backward before generating!");
                    break;
                }

                List<BoardState> predecessors = generateSuccessors(currentBackward);
                for (BoardState predecessor : predecessors) {
                    long predecessorLayout = predecessor.getLayout();
                    long predecessorSymmetricLayout = getSymmetricLayout(predecessorLayout);

                    // Check if visited by backward search (using layouts)
                    if (!backwardVisited.containsKey(predecessorLayout) && !backwardVisited.containsKey(predecessorSymmetricLayout)) {
                        backwardVisited.put(predecessorLayout, currentBackward); // Store layout -> parent state
                        backwardQueue.offer(predecessor);

                        // Check for intersection immediately after adding (using layouts)
                        if (forwardVisited.containsKey(predecessorLayout) || forwardVisited.containsKey(predecessorSymmetricLayout)) {
                            meetingNode = predecessor; // Intersection is at the predecessor state
                            foundByForwardSearch = false; // Found after backward step added it
                            foundIntersection = true;
                            //System.out.println("Intersection found by Backward after adding!");
                            break; // Exit predecessor loop
                        }
                    }
                }
            }
            // No need for extra check, while loop condition handles it

            if (nodesExplored % 100000 == 0) {
                //System.out.println("BiBFSSymOptV2 Nodes: " + nodesExplored +
                        //", FwdQ: " + forwardQueue.size() + ", BwdQ: " + backwardQueue.size() +
                        //", FwdV: " + forwardVisited.size() + ", BwdV: " + backwardVisited.size());
            }
        }

        // --- Path Reconstruction ---
        if (foundIntersection) {
            if (meetingNode == null) {
                // Should not happen if foundIntersection is true, but defensive check
                System.err.println("Error: Intersection detected but meeting node is null!");
                return Collections.emptyList();
            }
            //System.out.println("Solution found! Reconstructing path via meeting node (Layout: " + meetingNode.getLayout() + ")");
            return reconstructPathV2(forwardVisited, backwardVisited, meetingNode, foundByForwardSearch);
        } else {
            System.out.println("No solution found after processing nodes: " + nodesExplored);
            return Collections.emptyList();
        }
    }

    /**
     * Generates successors (Identical)
     */
    private List<BoardState> generateSuccessors(BoardState currentState) {
        List<BoardState> successors = new ArrayList<>();
        final int ROWS = BoardSerializer.ROWS;
        final int COLS = BoardSerializer.COLS;
        final int EMPTY = BoardSerializer.EMPTY;
        // Cache the array to avoid repeated calls if BoardState caches internally
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
                        // Creation of BoardState still happens here
                        successors.add(new BoardState(nextBoard));
                    }
                }
            }
        }
        return successors;
    }


    /**
     * Reconstructs the path using the meeting node and visited maps. V2.
     *
     * @param forwardVisited Map tracking forward path (layout -> parent State)
     * @param backwardVisited Map tracking backward path (layout -> parent State)
     * @param meetingNode The actual BoardState object where the two searches met.
     * @param foundByForwardSearch True if the forward search detected the intersection.
     * @return The complete path from initial to target state.
     */
    private List<BoardState> reconstructPathV2(Map<Long, BoardState> forwardVisited,
                                               Map<Long, BoardState> backwardVisited,
                                               BoardState meetingNode,
                                               boolean foundByForwardSearch) {

        LinkedList<BoardState> path = new LinkedList<>();
        BoardState current;
        long meetingLayout = meetingNode.getLayout();
        long meetingSymmetricLayout = getSymmetricLayout(meetingLayout); // Needed for lookup in opposite map

        // --- Trace Forward Path Segment ---
        // Start from the meeting node itself and go back to initial state
        current = meetingNode;
        while (current != null) {
            path.addFirst(current);
            // Find the parent using the layout stored in forwardVisited map
            current = forwardVisited.get(current.getLayout());
        }

        // --- Trace Backward Path Segment ---
        // Find the state *before* the meeting node in the backward search path
        BoardState parentBackward = backwardVisited.containsKey(meetingLayout)
                ? backwardVisited.get(meetingLayout)
                : backwardVisited.get(meetingSymmetricLayout);
        current = parentBackward;
        while (current != null) {
            path.addLast(current); // Append the rest of the backward path
            // Find the next parent using the layout stored in backwardVisited map
            current = backwardVisited.get(current.getLayout());
        }

        // Correction: The logic above duplicates the meeting node if not careful.
        // Let's refine: Build forward path up to meeting node, build backward path up to parent of meeting node, combine.

        path.clear(); // Reset path
        LinkedList<BoardState> forwardSegment = new LinkedList<>();
        LinkedList<BoardState> backwardSegmentReversed = new LinkedList<>();

        // Build forward path up to and including meeting node
        current = meetingNode;
        while(current != null) {
            forwardSegment.addFirst(current);
            current = forwardVisited.get(current.getLayout());
        }

        // Build backward path up to *parent* of meeting node (reversed order)
        parentBackward = backwardVisited.containsKey(meetingLayout)
                ? backwardVisited.get(meetingLayout)
                : backwardVisited.get(meetingSymmetricLayout);
        current = parentBackward;
        while(current != null) {
            backwardSegmentReversed.addFirst(current); // Adds parent, then grandparent...
            current = backwardVisited.get(current.getLayout());
        }

        // Combine: Forward segment + Backward segment (reversed)
        path.addAll(forwardSegment);
        while(!backwardSegmentReversed.isEmpty()){
            path.addLast(backwardSegmentReversed.removeFirst()); // Append parent, grandparent... to target
        }


        //System.out.println("Reconstructed Path Length V2: " + (path.isEmpty() ? 0 : path.size() - 1) + " steps");
        return path;
    }


    // --- Main method for testing (Identical setup) ---
    public static void main(String[] args) {
        System.out.println("Klotski Solver with Optimized Bi-BFS V2 (HashMap, Symmetry Layout) Test");

        try {
            // HengDaoLiMa initial state
            int[][] initialArray = {
                    {BoardSerializer.EMPTY, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.EMPTY},
                    {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.SOLDIER},
                    {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.SOLDIER}
            };
            int[][] targetArray = {
                    {BoardSerializer.VERTICAL, BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER},
                    {BoardSerializer.VERTICAL, BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                    {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.EMPTY},
                    {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.EMPTY}
            };

            BoardState initialState = new BoardState(initialArray);
            BoardState targetState = new BoardState(targetArray);

            System.out.println("Initial State:");
            BoardSerializer.printBoard(initialState.getBoardArray());
            System.out.println("\nTarget State:");
            BoardSerializer.printBoard(targetState.getBoardArray());

            KlotskiSolverBiBFSSymmetry solver = new KlotskiSolverBiBFSSymmetry();

            System.out.println("\nStarting Optimized BiBFS-HashMap-Symmetry V2 solve...");
            long startTime = System.currentTimeMillis();
            List<BoardState> path = solver.solve(initialState, targetState);
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

        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}