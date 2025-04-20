package controller.solverArchived.BiBFSOptLayoutGen; // New package

import controller.solver.BoardSerializer;
import controller.solver.BoardState;

import java.util.*;

/**
 * Further Optimized Bi-BFS with HashMaps and Symmetry Checking.
 * Generates successor layouts directly using bit manipulation,
 * minimizing BoardState object creation and serialization calls.
 */
public class KlotskiSolverBiBFSOptLayoutGen {

    // --- Constants (DR, DC, ROWS, COLS, BITS_PER_CELL, CELL_MASK, piece codes) ---
    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};
    private static final int ROWS = BoardSerializer.ROWS;
    private static final int COLS = BoardSerializer.COLS;
    private static final int BITS_PER_CELL = 3;
    private static final long CELL_MASK_3BIT = (1L << BITS_PER_CELL) - 1L; // 0x7
    private static final long CODE_EMPTY = 0L; // Assuming BoardSerializer.CODE_EMPTY is 0

    private int nodesExplored = 0;

    public int getNodesExplored() {
        return nodesExplored;
    }

    /**
     * Calculates symmetric layout using bit manipulation.
     * (Identical)
     */
    private long getSymmetricLayout(long layout) {
        long symmetricLayout = 0L;
        for (int r = 0; r < ROWS; r++) {
            long rowLayout = 0L;
            // Extract 4 cells for the row
            for(int c=0; c<COLS; c++){
                rowLayout |= ((layout >> ((r * COLS + c) * BITS_PER_CELL)) & CELL_MASK_3BIT) << (c * BITS_PER_CELL);
            }
            // Reverse the cells within the 12-bit row layout
            long reversedRowLayout = 0L;
            reversedRowLayout |= (rowLayout & CELL_MASK_3BIT) << (3 * BITS_PER_CELL); // cell 0 -> pos 3
            reversedRowLayout |= ((rowLayout >> BITS_PER_CELL) & CELL_MASK_3BIT) << (2 * BITS_PER_CELL); // cell 1 -> pos 2
            reversedRowLayout |= ((rowLayout >> (2 * BITS_PER_CELL)) & CELL_MASK_3BIT) << (1 * BITS_PER_CELL); // cell 2 -> pos 1
            reversedRowLayout |= ((rowLayout >> (3 * BITS_PER_CELL)) & CELL_MASK_3BIT) << (0 * BITS_PER_CELL); // cell 3 -> pos 0
            // Place reversed row into symmetric layout
            symmetricLayout |= (reversedRowLayout << (r * COLS * BITS_PER_CELL));
        }
        return symmetricLayout;
    }

     /**
      * Gets the 3-bit code for a specific cell directly from the layout.
      */
     private long getCellCode(long layout, int r, int c) {
         if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return -1; // Invalid coords
         int shift = (r * COLS + c) * BITS_PER_CELL;
         return (layout >> shift) & CELL_MASK_3BIT;
     }

    /**
     * Generates successor layouts directly from the current layout using bit manipulation.
     */
    private List<Long> generateSuccessorLayouts(long currentLayout) {
        List<Long> successorLayouts = new ArrayList<>();
        int[][] board = BoardSerializer.deserialize(currentLayout); // Deserialize ONCE
        boolean[][] processed = new boolean[ROWS][COLS];

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int pieceType = board[r][c]; // Use deserialized board to find pieces
                if (pieceType == BoardSerializer.EMPTY || processed[r][c]) continue;

                // --- Piece identification using deserialized board (same logic as before) ---
                List<int[]> pieceCellsCoords = new ArrayList<>(); // Store {row, col}
                boolean validPiece = true;
                 switch (pieceType) {
                    case BoardSerializer.SOLDIER: pieceCellsCoords.add(new int[]{r, c}); processed[r][c] = true; break;
                    case BoardSerializer.HORIZONTAL: if (c + 1 < COLS && board[r][c+1] == pieceType) { pieceCellsCoords.add(new int[]{r, c}); pieceCellsCoords.add(new int[]{r, c + 1}); processed[r][c] = true; processed[r][c + 1] = true; } else validPiece = false; break;
                    case BoardSerializer.VERTICAL: if (r + 1 < ROWS && board[r+1][c] == pieceType) { pieceCellsCoords.add(new int[]{r, c}); pieceCellsCoords.add(new int[]{r + 1, c}); processed[r][c] = true; processed[r+1][c] = true; } else validPiece = false; break;
                    case BoardSerializer.CAO_CAO: if (r + 1 < ROWS && c + 1 < COLS && board[r][c+1] == pieceType && board[r+1][c] == pieceType && board[r+1][c+1] == pieceType) { pieceCellsCoords.add(new int[]{r, c}); pieceCellsCoords.add(new int[]{r, c + 1}); pieceCellsCoords.add(new int[]{r + 1, c}); pieceCellsCoords.add(new int[]{r + 1, c + 1}); processed[r][c] = true; processed[r][c+1] = true; processed[r+1][c] = true; processed[r+1][c+1] = true; } else validPiece = false; break;
                    default: validPiece = false; break;
                }
                if (!validPiece) continue;
                // --- End Piece identification ---

                long pieceCode = BoardSerializer.arrayToCodeMap.get(pieceType); // Get the 3-bit code

                // Try moving in 4 directions
                for (int dir = 0; dir < 4; dir++) {
                    int dr = DR[dir], dc = DC[dir];
                    boolean canMove = true;
                    List<int[]> targetCellsCoords = new ArrayList<>();
                    long clearMask = 0L;
                    long setMask = 0L;

                    // 1. Check boundaries and validity using bitwise checks on currentLayout
                    for (int[] cellCoord : pieceCellsCoords) {
                        int nr = cellCoord[0] + dr;
                        int nc = cellCoord[1] + dc;

                        // Boundary check
                        if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS) {
                            canMove = false;
                            break;
                        }
                        targetCellsCoords.add(new int[]{nr, nc});

                        // Check if target cell is empty (using layout) UNLESS it's part of the original piece
                        boolean targetIsOriginal = false;
                        for(int[] originalCoord : pieceCellsCoords) {
                            if(nr == originalCoord[0] && nc == originalCoord[1]) {
                                targetIsOriginal = true;
                                break;
                            }
                        }
                        if (!targetIsOriginal) {
                            if (getCellCode(currentLayout, nr, nc) != CODE_EMPTY) {
                                canMove = false;
                                break;
                            }
                        }
                    } // End checking target cells

                    if (canMove) {
                        // 2. Generate newLayout using bitwise operations
                        long newLayout = currentLayout;

                        // Create clearMask for original positions
                        for (int[] cellCoord : pieceCellsCoords) {
                            int pos = cellCoord[0] * COLS + cellCoord[1];
                            clearMask |= (CELL_MASK_3BIT << (pos * BITS_PER_CELL));
                        }

                        // Create setMask for target positions
                        for (int[] targetCoord : targetCellsCoords) {
                             int pos = targetCoord[0] * COLS + targetCoord[1];
                             setMask |= (pieceCode << (pos * BITS_PER_CELL));
                        }

                        // Apply masks
                        newLayout = (newLayout & ~clearMask) | setMask;
                        successorLayouts.add(newLayout);
                    }
                } // End directions loop
            }
        } // End board iteration
        return successorLayouts;
    }


    /**
     * Solves the Klotski puzzle using the most optimized Bi-BFS.
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

        BoardState meetingNode = null;
        boolean foundByForwardSearch = false;
        boolean foundIntersection = false;
        nodesExplored = 0;

        while (!forwardQueue.isEmpty() && !backwardQueue.isEmpty() && !foundIntersection) {

            // --- Expand forward search by one level ---
            int forwardLevelSize = forwardQueue.size();
            for (int i = 0; i < forwardLevelSize && !foundIntersection; i++) {
                BoardState currentForward = forwardQueue.poll();
                long currentForwardLayout = currentForward.getLayout();
                nodesExplored++;

                long currentForwardSymmetricLayout = getSymmetricLayout(currentForwardLayout);
                if (backwardVisited.containsKey(currentForwardLayout) || backwardVisited.containsKey(currentForwardSymmetricLayout)) {
                    meetingNode = currentForward;
                    foundByForwardSearch = true;
                    foundIntersection = true;
                    break;
                }

                // Generate LAYOUTS directly
                List<Long> successorLayouts = generateSuccessorLayouts(currentForwardLayout);
                for (long successorLayout : successorLayouts) {
                    long successorSymmetricLayout = getSymmetricLayout(successorLayout);

                    if (!forwardVisited.containsKey(successorLayout) && !forwardVisited.containsKey(successorSymmetricLayout)) {
                        forwardVisited.put(successorLayout, currentForward);
                        // Create BoardState ONLY when adding to queue
                        forwardQueue.offer(new BoardState(successorLayout));

                        if (backwardVisited.containsKey(successorLayout) || backwardVisited.containsKey(successorSymmetricLayout)) {
                            // Create the meeting BoardState object now
                            meetingNode = new BoardState(successorLayout);
                            foundByForwardSearch = true;
                            foundIntersection = true;
                            break;
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

                long currentBackwardSymmetricLayout = getSymmetricLayout(currentBackwardLayout);
                 if (forwardVisited.containsKey(currentBackwardLayout) || forwardVisited.containsKey(currentBackwardSymmetricLayout)) {
                    meetingNode = currentBackward;
                    foundByForwardSearch = false;
                    foundIntersection = true;
                    break;
                }

                // Generate LAYOUTS directly
                List<Long> predecessorLayouts = generateSuccessorLayouts(currentBackwardLayout);
                for (long predecessorLayout : predecessorLayouts) {
                    long predecessorSymmetricLayout = getSymmetricLayout(predecessorLayout);

                    if (!backwardVisited.containsKey(predecessorLayout) && !backwardVisited.containsKey(predecessorSymmetricLayout)) {
                         backwardVisited.put(predecessorLayout, currentBackward);
                         // Create BoardState ONLY when adding to queue
                         backwardQueue.offer(new BoardState(predecessorLayout));

                        if (forwardVisited.containsKey(predecessorLayout) || forwardVisited.containsKey(predecessorSymmetricLayout)) {
                            // Create the meeting BoardState object now
                            meetingNode = new BoardState(predecessorLayout);
                            foundByForwardSearch = false;
                            foundIntersection = true;
                            break;
                        }
                    }
                }
            }

            if (nodesExplored % 200000 == 0) {
                 System.out.println("BiBFSOptLG Nodes: " + nodesExplored +
                        ", FwdQ: " + forwardQueue.size() + ", BwdQ: " + backwardQueue.size() +
                        ", FwdV: " + forwardVisited.size() + ", BwdV: " + backwardVisited.size());
            }
        }

        // --- Path Reconstruction (Identical to V2) ---
        if (foundIntersection) {
             if (meetingNode == null) {
                 System.err.println("Error: Intersection detected but meeting node is null!");
                 return Collections.emptyList();
             }
            return reconstructPathV2(forwardVisited, backwardVisited, meetingNode, foundByForwardSearch);
        } else {
            System.out.println("No solution found after processing nodes: " + nodesExplored);
            return Collections.emptyList();
        }
    }

    /**
     * Reconstructs the path using the meeting node and visited maps.
     * (Identical to V2)
     */
    private List<BoardState> reconstructPathV2(Map<Long, BoardState> forwardVisited,
                                              Map<Long, BoardState> backwardVisited,
                                              BoardState meetingNode,
                                              boolean foundByForwardSearch) {
        LinkedList<BoardState> path = new LinkedList<>();
        BoardState current;
        long meetingLayout = meetingNode.getLayout();
        long meetingSymmetricLayout = getSymmetricLayout(meetingLayout);

        path.clear();
        LinkedList<BoardState> forwardSegment = new LinkedList<>();
        LinkedList<BoardState> backwardSegmentReversed = new LinkedList<>();

        current = meetingNode;
        while(current != null) {
             forwardSegment.addFirst(current);
             current = forwardVisited.get(current.getLayout());
        }

        BoardState parentBackward = backwardVisited.containsKey(meetingLayout)
                                   ? backwardVisited.get(meetingLayout)
                                   : backwardVisited.get(meetingSymmetricLayout);
        current = parentBackward;
         while(current != null) {
             backwardSegmentReversed.addFirst(current);
             current = backwardVisited.get(current.getLayout());
         }

         path.addAll(forwardSegment);
         while(!backwardSegmentReversed.isEmpty()){
             path.addLast(backwardSegmentReversed.removeFirst());
         }

        //System.out.println("Reconstructed Path Length LG: " + (path.isEmpty() ? 0 : path.size() - 1) + " steps");
        return path;
    }


    // --- Main method for testing (Identical setup) ---
    public static void main(String[] args) {
         System.out.println("Klotski Solver with Layout Generation Bi-BFS (HashMap, Symmetry Layout) Test");
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

            KlotskiSolverBiBFSOptLayoutGen solver = new KlotskiSolverBiBFSOptLayoutGen();

            System.out.println("\nStarting Layout Generation BiBFS solve...");
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

        } catch (Exception e) { e.printStackTrace(); }
    }
}