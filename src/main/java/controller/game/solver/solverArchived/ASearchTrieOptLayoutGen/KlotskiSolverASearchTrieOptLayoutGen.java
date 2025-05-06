package controller.game.solver.solverArchived.ASearchTrieOptLayoutGen; // Can reuse package or add V2

import controller.util.BoardSerializer;
import controller.game.solver.BoardState;

import java.util.*;

// --- TrieNode definition ---
class TrieNode { /* ... same ... */
    TrieNode[] children = new TrieNode[5];
    ElementNode elementNodeLink = null;
}

// --- ElementNode definition for A* ---
class ElementNode implements Comparable<ElementNode> { /* ... same ... */
    BoardState state; ElementNode father; int g, h, f;
    public ElementNode(BoardState s, ElementNode fth, int g, int h) { state=s; father=fth; this.g=g; this.h=h; f=g+h; }
    @Override public int compareTo(ElementNode o) { if (f != o.f) return Integer.compare(f, o.f); return Integer.compare(h, o.h); }
    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof ElementNode)) return false; return state.equals(((ElementNode) o).state); }
    @Override public int hashCode() { return state.hashCode(); }
}

/**
 * V2: Optimized A* Search with Trie, Symmetry, LayoutGen, and DirectGoalCheck (using constant code).
 */
public class KlotskiSolverASearchTrieOptLayoutGen {

    // --- Constants ---
    // --- Identical ---
    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};
    private static final int ROWS = BoardSerializer.ROWS;
    private static final int COLS = BoardSerializer.COLS;
    private static final int BITS_PER_CELL = 3;
    private static final long CELL_MASK_3BIT = (1L << BITS_PER_CELL) - 1L;
    private static final long CODE_EMPTY = 0L;
    // *** Direct Constant for CaoCao's 3-bit code ***
    private static final long CODE_CAO_CAO_DIRECT = 4L; // 0b100L - Use this directly!

    private TrieNode trieRoot;
    private int nodesExplored = 0;

    public int getNodesExplored() { return nodesExplored; }

    /** Calculates symmetric layout. (static) */
    private static long getSymmetricLayout(long layout) { /* ... same static code ... */
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

    /** Gets cell code. (static) */
     private static long getCellCode(long layout, int r, int c) { /* ... same static code ... */
         if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return -1;
         int shift = (r * COLS + c) * BITS_PER_CELL;
         return (layout >> shift) & CELL_MASK_3BIT;
      }

    /** Generates successor layouts. (static) */
    private static List<Long> generateSuccessorLayouts(long currentLayout) { /* ... same static code ... */
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
                long pieceCode = BoardSerializer.arrayToCodeMap.get(pieceType); // Still need map for general code
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

    /** Checks if the given layout is a goal state using direct comparison and constant code. */
    private boolean isGoalLayout(long layout) {
        // OPTIMIZED: Use direct constant CODE_CAO_CAO_DIRECT (4L)
        final long mask = CELL_MASK_3BIT;
        // Check (4,1) - index 17 -> shift 51
        boolean cell3_ok = ((layout >> (17 * BITS_PER_CELL)) & mask) == CODE_CAO_CAO_DIRECT;
        // Check (4,2) - index 18 -> shift 54
        boolean cell4_ok = ((layout >> (18 * BITS_PER_CELL)) & mask) == CODE_CAO_CAO_DIRECT;
        return cell3_ok && cell4_ok;
    }

    /** Calculates heuristic (Manhattan distance) using layout. (static) */
    private static int calculateHeuristic(long layout) { // Made static
        // Need to find CaoCao's top-left corner from layout
        // Use direct constant CODE_CAO_CAO_DIRECT
        for (int r = 0; r < ROWS - 1; r++) {
            for (int c = 0; c < COLS - 1; c++) {
                // Check if (r,c) is top-left of CaoCao using getCellCode (static version)
                if (getCellCode(layout, r, c) == CODE_CAO_CAO_DIRECT &&
                    getCellCode(layout, r, c+1) == CODE_CAO_CAO_DIRECT &&
                    getCellCode(layout, r+1, c) == CODE_CAO_CAO_DIRECT &&
                    getCellCode(layout, r+1, c+1) == CODE_CAO_CAO_DIRECT) {
                    return Math.abs(r - 3) + Math.abs(c - 1); // Manhattan distance to (3,1)
                }
            }
        }
        return Integer.MAX_VALUE / 2; // Should not happen
    }

    /** Inserts/Updates layout in Trie. (static) */
    private static ElementNode trieInsertOrUpdateLayout(TrieNode root, long layoutToInsert, ElementNode elementToLink) { // Made static
        long symmetricLayout = getSymmetricLayout(layoutToInsert); // Use static version
        long canonicalLayout = Math.min(layoutToInsert, symmetricLayout);
        TrieNode current = root;
        final int CELLS_COUNT = ROWS * COLS;
        for (int i = 0; i < CELLS_COUNT; i++) {
            int pieceType = (int)((canonicalLayout >> (i * BITS_PER_CELL)) & CELL_MASK_3BIT);
            if (current.children[pieceType] == null) current.children[pieceType] = new TrieNode();
            current = current.children[pieceType];
        }
        if (current.elementNodeLink == null) { current.elementNodeLink = elementToLink; return null; }
        else {
            if (elementToLink.g < current.elementNodeLink.g) {
                 ElementNode oldElement = current.elementNodeLink; current.elementNodeLink = elementToLink; return oldElement;
            } else { return current.elementNodeLink; }
        }
    }

     /** Looks up layout in Trie. (static) */
    private static ElementNode trieLookupLayout(TrieNode root, long layoutToLookup) { // Made static
        long symmetricLayout = getSymmetricLayout(layoutToLookup); // Use static version
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
     * Solves using Optimized A* + Trie + Symmetry + LayoutGen + DirectGoalCheck V2.
     */
    public List<BoardState> solve(BoardState initialState) {
        this.trieRoot = new TrieNode(); // Instance specific root
        this.nodesExplored = 0;
        long initialLayout = initialState.getLayout();

        // Check initial state with optimized goal check
        if (isGoalLayout(initialLayout)) return Collections.singletonList(initialState);

        PriorityQueue<ElementNode> openSet = new PriorityQueue<>();
        int initialH = calculateHeuristic(initialLayout); // Use static version
        ElementNode initialElement = new ElementNode(initialState, null, 0, initialH);

        // Use static version, pass instance root
        trieInsertOrUpdateLayout(this.trieRoot, initialLayout, initialElement);
        openSet.add(initialElement);
        long statesAdded = 1;

        while (!openSet.isEmpty()) {
            ElementNode currentNode = openSet.poll();
            nodesExplored++;
            long currentLayout = currentNode.state.getLayout();

            // *** Optimized Goal Check V2 (when polling) ***
            if (isGoalLayout(currentLayout)) {
                LinkedList<BoardState> path = new LinkedList<>(); ElementNode trace = currentNode;
                while (trace != null) { path.addFirst(trace.state); trace = trace.father; } return path;
            }

            // Check for outdated node (use static lookup)
            ElementNode currentElementInTrie = trieLookupLayout(this.trieRoot, currentLayout);
            if (currentElementInTrie == null || currentElementInTrie.g < currentNode.g) {
                 continue; // Skip outdated node
            }

            if (nodesExplored % 100000 == 0) {
                 System.out.println("A*-TrieOptLGV2 Nodes: " + nodesExplored + ", OpenSet: " + openSet.size() + ", f=" + currentNode.f + " (g=" + currentNode.g + ", h=" + currentNode.h + ")");
            }

            // Use static version
            List<Long> successorLayouts = generateSuccessorLayouts(currentLayout);

            for (long successorLayout : successorLayouts) {
                int g_new = currentNode.g + 1;
                int h_new = calculateHeuristic(successorLayout); // Use static version
                BoardState successorState = null; // Lazy creation
                ElementNode newElement = null; // Lazy creation

                // Create state/node needed for Trie insertion/update check
                successorState = new BoardState(successorLayout);
                newElement = new ElementNode(successorState, currentNode, g_new, h_new);

                // Use static version, pass instance root
                ElementNode existingOrOldElement = trieInsertOrUpdateLayout(this.trieRoot, successorLayout, newElement);

                if (existingOrOldElement == null) {
                    openSet.add(newElement); statesAdded++;
                } else if (existingOrOldElement != newElement && existingOrOldElement.g > newElement.g) {
                    if (openSet.remove(existingOrOldElement)) { openSet.add(newElement); }
                }
            } // End processing successors
        } // End A* loop

        System.out.println("A*-TrieOptLGV2: No solution found after processing " + nodesExplored + " states.");
        return Collections.emptyList();
    }

     // --- Main method for testing (Similar setup) ---
     public static void main(String[] args) {
         System.out.println("Klotski Solver V2 (Opt A*-Trie, Symmetry, LayoutGen, DirectGoalConst) Test");
        try {
             int[][] initialArray = { /* ... same ... */
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER}
            };
            BoardState initialState = new BoardState(initialArray);
            System.out.println("Initial State:"); BoardSerializer.printBoard(initialState.getBoardArray());

            KlotskiSolverASearchTrieOptLayoutGen solver = new KlotskiSolverASearchTrieOptLayoutGen();

            System.out.println("\nStarting Optimized A*-Trie-LG V2 solve...");
            long startTime = System.currentTimeMillis();
            List<BoardState> path = solver.solve(initialState);
            long endTime = System.currentTimeMillis();
            System.out.println("\nSolve finished in " + (endTime - startTime) + " ms.");
             System.out.println("Nodes explored: " + solver.getNodesExplored());

            if (!path.isEmpty()) {
                System.out.println("Solution found with " + (path.size() - 1) + " steps.");
            } else System.out.println("No solution found.");
        } catch (Exception e) { e.printStackTrace(); }
    }
}