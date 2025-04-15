package controller.solver.BFSTireOptLayoutGen; // New package

import controller.solver.BoardSerializer;
import controller.solver.BoardState;

import java.util.*;

// --- TrieNode definition (links to ElementNode) ---
class TrieNode {
    TrieNode[] children = new TrieNode[5];
    ElementNode elementNodeLink = null;
}

// --- ElementNode definition for BFS (remains same) ---
class ElementNode {
    BoardState state; // Still store the state object for path reconstruction
    ElementNode father;
    ElementNode nextInLevel;
    int moveCount;

    ElementNode(BoardState state, ElementNode father, int moveCount) {
        this.state = state; this.father = father; this.moveCount = moveCount; this.nextInLevel = null;
    }
}

/**
 * Optimized BFS with Trie, Symmetry Checking, and Direct Layout Generation.
 */
public class KlotskiSolverBFSTrieOptLayoutGen {


    // --- Constants (DR, DC, ROWS, COLS, BITS_PER_CELL, CELL_MASK_3BIT, CODE_EMPTY) ---
    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};
    private static final int ROWS = BoardSerializer.ROWS;
    private static final int COLS = BoardSerializer.COLS;
    private static final int BITS_PER_CELL = 3;
    private static final long CELL_MASK_3BIT = (1L << BITS_PER_CELL) - 1L;
    private static final long CODE_EMPTY = 0L;

    private TrieNode trieRoot;
    private int nodesExplored = 0;

    public int getNodesExplored() {
        return nodesExplored;
    }

    /** Calculates symmetric layout using bit manipulation. (Identical) */
    private long getSymmetricLayout(long layout) {
        long symmetricLayout = 0L;
        for (int r = 0; r < ROWS; r++) {
            long rowLayout = 0L;
            for(int c=0; c<COLS; c++){ rowLayout |= ((layout >> ((r * COLS + c) * BITS_PER_CELL)) & CELL_MASK_3BIT) << (c * BITS_PER_CELL); }
            long reversedRowLayout = 0L;
            reversedRowLayout |= (rowLayout & CELL_MASK_3BIT) << (3 * BITS_PER_CELL);
            reversedRowLayout |= ((rowLayout >> BITS_PER_CELL) & CELL_MASK_3BIT) << (2 * BITS_PER_CELL);
            reversedRowLayout |= ((rowLayout >> (2 * BITS_PER_CELL)) & CELL_MASK_3BIT) << (1 * BITS_PER_CELL);
            reversedRowLayout |= ((rowLayout >> (3 * BITS_PER_CELL)) & CELL_MASK_3BIT);
            symmetricLayout |= (reversedRowLayout << (r * COLS * BITS_PER_CELL));
        }
        return symmetricLayout;
    }

    /** Gets the 3-bit code for a specific cell directly from the layout. (Identical) */
     private long getCellCode(long layout, int r, int c) {
         if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return -1;
         int shift = (r * COLS + c) * BITS_PER_CELL;
         return (layout >> shift) & CELL_MASK_3BIT;
     }

    /** Generates successor layouts directly from the current layout. (Identical) */
    private List<Long> generateSuccessorLayouts(long currentLayout) {
        List<Long> successorLayouts = new ArrayList<>();
        int[][] board = BoardSerializer.deserialize(currentLayout); // Deserialize ONCE
        boolean[][] processed = new boolean[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int pieceType = board[r][c];
                if (pieceType == BoardSerializer.EMPTY || processed[r][c]) continue;
                List<int[]> pieceCellsCoords = new ArrayList<>();
                boolean validPiece = true;
                 switch (pieceType) { // Simplified piece identification logic for brevity
                    case 1: pieceCellsCoords.add(new int[]{r, c}); processed[r][c] = true; break;
                    case 2: if (c + 1 < COLS && board[r][c+1] == pieceType) { pieceCellsCoords.add(new int[]{r, c}); pieceCellsCoords.add(new int[]{r, c + 1}); processed[r][c] = true; processed[r][c + 1] = true; } else validPiece = false; break;
                    case 3: if (r + 1 < ROWS && board[r+1][c] == pieceType) { pieceCellsCoords.add(new int[]{r, c}); pieceCellsCoords.add(new int[]{r + 1, c}); processed[r][c] = true; processed[r+1][c] = true; } else validPiece = false; break;
                    case 4: if (r + 1 < ROWS && c + 1 < COLS && board[r][c+1] == 4 && board[r+1][c] == 4 && board[r+1][c+1] == 4) { pieceCellsCoords.add(new int[]{r, c}); pieceCellsCoords.add(new int[]{r, c + 1}); pieceCellsCoords.add(new int[]{r + 1, c}); pieceCellsCoords.add(new int[]{r + 1, c + 1}); processed[r][c] = true; processed[r][c+1] = true; processed[r+1][c] = true; processed[r+1][c+1] = true; } else validPiece = false; break;
                    default: validPiece = false; break;
                }
                if (!validPiece) continue;
                long pieceCode = BoardSerializer.arrayToCodeMap.get(pieceType);
                for (int dir = 0; dir < 4; dir++) {
                    int dr = DR[dir], dc = DC[dir]; boolean canMove = true; List<int[]> targetCellsCoords = new ArrayList<>();
                    for (int[] cellCoord : pieceCellsCoords) {
                        int nr = cellCoord[0] + dr, nc = cellCoord[1] + dc;
                        if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS) { canMove = false; break; }
                        targetCellsCoords.add(new int[]{nr, nc});
                        boolean targetIsOriginal = false; for(int[] o : pieceCellsCoords) if(nr == o[0] && nc == o[1]) {targetIsOriginal = true; break;}
                        if (!targetIsOriginal && getCellCode(currentLayout, nr, nc) != CODE_EMPTY) { canMove = false; break; }
                    }
                    if (canMove) {
                        long newLayout = currentLayout; long clearMask = 0L; long setMask = 0L;
                        for (int[] cellCoord : pieceCellsCoords) clearMask |= (CELL_MASK_3BIT << ((cellCoord[0] * COLS + cellCoord[1]) * BITS_PER_CELL));
                        for (int[] targetCoord : targetCellsCoords) setMask |= (pieceCode << ((targetCoord[0] * COLS + targetCoord[1]) * BITS_PER_CELL));
                        newLayout = (newLayout & ~clearMask) | setMask;
                        successorLayouts.add(newLayout);
                    }
                }
            }
        }
        return successorLayouts;
    }

    /** Checks if the given layout represents the winning condition. */
    private boolean isGoalLayout(long layout) {
        // Direct check using known goal pattern (CaoCao at bottom center)
        // Goal cells: (3,1)=13, (3,2)=14, (4,1)=17, (4,2)=18
        // Assuming CAO_CAO code is 4 (0b100)
        long caoCaoCode = BoardSerializer.arrayToCodeMap.get(BoardSerializer.CAO_CAO);
        return getCellCode(layout, 3, 1) == caoCaoCode &&
               getCellCode(layout, 3, 2) == caoCaoCode &&
               getCellCode(layout, 4, 1) == caoCaoCode &&
               getCellCode(layout, 4, 2) == caoCaoCode;
    }


    /**
     * Inserts a layout into the Trie using its canonical form.
     * Links the final node to the corresponding ElementNode.
     * Traverses Trie using bit manipulation on the layout.
     *
     * @param root The root of the Trie.
     * @param layoutToInsert The layout long to insert.
     * @param elementToLink The ElementNode representing this state in the BFS.
     */
    private void trieInsertLayout(TrieNode root, long layoutToInsert, ElementNode elementToLink) {
        long symmetricLayout = getSymmetricLayout(layoutToInsert);
        long canonicalLayout = Math.min(layoutToInsert, symmetricLayout);

        TrieNode current = root;
        final int CELLS_COUNT = ROWS * COLS;

        for (int i = 0; i < CELLS_COUNT; i++) {
            int pieceType = (int)((canonicalLayout >> (i * BITS_PER_CELL)) & CELL_MASK_3BIT);
            if (current.children[pieceType] == null) current.children[pieceType] = new TrieNode();
            current = current.children[pieceType];
        }
        if (current.elementNodeLink == null) current.elementNodeLink = elementToLink;
    }

    /**
     * Looks up a layout in the Trie using its canonical form.
     * Traverses Trie using bit manipulation on the layout.
     *
     * @param root The root of the Trie.
     * @param layoutToLookup The layout long to look up.
     * @return The linked ElementNode if the canonical form exists, otherwise null.
     */
    private ElementNode trieLookupLayout(TrieNode root, long layoutToLookup) {
        long symmetricLayout = getSymmetricLayout(layoutToLookup);
        long canonicalLayout = Math.min(layoutToLookup, symmetricLayout);

        TrieNode current = root;
        final int CELLS_COUNT = ROWS * COLS;

        for (int i = 0; i < CELLS_COUNT; i++) {
             int pieceType = (int)((canonicalLayout >> (i * BITS_PER_CELL)) & CELL_MASK_3BIT);
             if (pieceType < 0 || pieceType > 4) return null;
             if (current.children[pieceType] == null) return null;
             current = current.children[pieceType];
        }
        return current.elementNodeLink;
    }


    /**
     * Solves the Klotski puzzle using Optimized BFS with Trie, Symmetry, and Layout Generation.
     */
    public List<BoardState> solve(BoardState initialState) {
        this.trieRoot = new TrieNode();
        this.nodesExplored = 0;
        long initialLayout = initialState.getLayout();

        if (isGoalLayout(initialLayout)) return Collections.singletonList(initialState);

        ElementNode initialElement = new ElementNode(initialState, null, 0);
        trieInsertLayout(trieRoot, initialLayout, initialElement); // Insert initial state

        ElementNode currentLevelHead = initialElement;
        ElementNode nextLevelHead = null;
        ElementNode nextLevelTail = null;
        int currentDepth = 0;
        long statesAdded = 1;

        while (currentLevelHead != null) {
            ElementNode currentNode = currentLevelHead;

            while (currentNode != null) {
                nodesExplored++;
                long currentLayout = currentNode.state.getLayout(); // Get layout from state object

                List<Long> successorLayouts = generateSuccessorLayouts(currentLayout);

                for (long successorLayout : successorLayouts) {
                    if (isGoalLayout(successorLayout)) {
                        // Create final state object for path
                        BoardState goalState = new BoardState(successorLayout);
                        LinkedList<BoardState> path = new LinkedList<>();
                        path.addFirst(goalState);
                        ElementNode trace = currentNode;
                        while (trace != null) { path.addFirst(trace.state); trace = trace.father; }
                        return path;
                    }

                    // Optimized Visited Check using canonical layout lookup
                    ElementNode existingElement = trieLookupLayout(trieRoot, successorLayout);

                    if (existingElement == null) {
                        statesAdded++;
                        // Create BoardState ONLY when adding new node
                        BoardState successorState = new BoardState(successorLayout);
                        ElementNode newElement = new ElementNode(successorState, currentNode, currentDepth + 1);
                        trieInsertLayout(trieRoot, successorLayout, newElement); // Insert using layout

                        if (nextLevelHead == null) { nextLevelHead = newElement; nextLevelTail = newElement; }
                        else { nextLevelTail.nextInLevel = newElement; nextLevelTail = newElement; }
                    }
                } // End processing successors

                currentNode = currentNode.nextInLevel;
            } // End processing current level

            currentLevelHead = nextLevelHead; nextLevelHead = null; nextLevelTail = null;
            currentDepth++;

             if (nodesExplored % 200000 == 0) {
                System.out.println("BFS-TrieOptLG Nodes: " + nodesExplored + ", Depth: " + currentDepth + ", Added: " + statesAdded);
            }
        } // End BFS Loop

        System.out.println("BFS-TrieOptLG: No solution found after processing " + nodesExplored + " states.");
        return Collections.emptyList();
    }

     // --- Main method for testing (Similar setup) ---
     public static void main(String[] args) {
        System.out.println("Klotski Solver with Optimized Trie-BFS, Symmetry, Layout Generation Test");
        try {
             int[][] initialArray = {
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER}
            };
            BoardState initialState = new BoardState(initialArray);
            System.out.println("Initial State:"); BoardSerializer.printBoard(initialState.getBoardArray());

            KlotskiSolverBFSTrieOptLayoutGen solver = new KlotskiSolverBFSTrieOptLayoutGen();

            System.out.println("\nStarting Optimized Trie-BFS-LG solve...");
            long startTime = System.currentTimeMillis();
            List<BoardState> path = solver.solve(initialState);
            long endTime = System.currentTimeMillis();
            System.out.println("\nSolve finished in " + (endTime - startTime) + " ms.");
            System.out.println("Nodes explored: " + solver.getNodesExplored());

            if (!path.isEmpty()) {
                System.out.println("Solution found with " + (path.size() - 1) + " steps.");
                System.out.println("\nFinal State (Step " + (path.size() - 1) + "):"); BoardSerializer.printBoard(path.get(path.size() - 1).getBoardArray());
            } else System.out.println("No solution found.");
        } catch (Exception e) { e.printStackTrace(); }
    }
}