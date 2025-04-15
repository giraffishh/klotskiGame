package controller.solver;

import java.util.*;
import java.io.*; // For persistence

/**
 * Generates a Pattern Database (PDB) for the Klotski puzzle,
 * focusing on the pattern of Cao Cao (CC) and the four Soldiers (S).
 * It calculates the minimum number of moves required to get the pattern pieces
 * from any reachable configuration to a defined target configuration, ignoring
 * other pieces (Vertical, Horizontal). This is done using a backward Breadth-First Search (BFS)
 * starting from the target state.
 */
public class PatternDatabaseGenerator {

    // Define the pieces included in this PDB pattern
    private static final Set<Integer> PATTERN_PIECES = new HashSet<>(Arrays.asList(
            BoardSerializer.CAO_CAO,
            BoardSerializer.SOLDIER,
            BoardSerializer.HORIZONTAL,
            BoardSerializer.VERTICAL
            ));

    // Define a canonical target state *for the pattern*
    // CC at goal (3,1), Soldiers placed arbitrarily in valid spots (e.g., top corners).
    // The exact placement of Soldiers in the target doesn't matter for correctness,
    // as long as CC is at the goal and it's a valid pattern configuration.
    private static final int[][] PDB_TARGET_ARRAY = {
            {BoardSerializer.VERTICAL, BoardSerializer.VERTICAL,   BoardSerializer.VERTICAL,   BoardSerializer.VERTICAL},
            {BoardSerializer.VERTICAL, BoardSerializer.VERTICAL,   BoardSerializer.VERTICAL,   BoardSerializer.VERTICAL},
            {BoardSerializer.EMPTY,   BoardSerializer.EMPTY,   BoardSerializer.HORIZONTAL,   BoardSerializer.HORIZONTAL},
            {BoardSerializer.EMPTY,   BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.SOLDIER},
            {BoardSerializer.EMPTY,   BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.SOLDIER}
    };
    // Serialize the target pattern state once
    public static final long PDB_TARGET_LAYOUT = BoardSerializer.serialize(PDB_TARGET_ARRAY);

    // Constants for move generation
    private static final int[] DR = {-1, 1, 0, 0}; // UP, DOWN, LEFT, RIGHT
    private static final int[] DC = {0, 0, -1, 1};

    // --- PDB State Representation ---

    /**
     * Extracts the pattern state (CC and S blocks only) from a full board state
     * and serializes it into a long. Treats non-pattern pieces (V, H) as EMPTY.
     *
     * @param fullState The complete BoardState.
     * @return A long representing the layout of only the pattern pieces. Returns -1 if input is null.
     */
    public static long getPatternLayout(BoardState fullState) {
        if (fullState == null) return -1L; // Handle null input
        // Optimization: Reuse board array if already deserialized by the BoardState instance
        return getPatternLayout(fullState.getBoardArray());
    }

    /**
     * Extracts the pattern state (CC and S blocks only) from a full board array
     * and serializes it into a long. Treats non-pattern pieces (V, H) as EMPTY.
     *
     * @param fullBoardArray The int[][] array of the complete board state.
     * @return A long representing the layout of only the pattern pieces. Returns -1 if input is invalid.
     */
    public static long getPatternLayout(int[][] fullBoardArray) {
        if (fullBoardArray == null || fullBoardArray.length != BoardSerializer.ROWS ||
                fullBoardArray[0] == null || fullBoardArray[0].length != BoardSerializer.COLS) {
            // System.err.println("Warning: Invalid board array passed to getPatternLayout."); // Can be noisy
            return -1L; // Indicate error
        }

        int[][] patternBoard = new int[BoardSerializer.ROWS][BoardSerializer.COLS];
        // Initialize patternBoard to EMPTY implicitly (default int value is 0)

        for (int r = 0; r < BoardSerializer.ROWS; r++) {
            for (int c = 0; c < BoardSerializer.COLS; c++) {
                // Ensure row is valid before accessing column
                if (fullBoardArray[r] == null || fullBoardArray[r].length != BoardSerializer.COLS) {
                    // System.err.println("Warning: Invalid row in board array at index " + r); // Can be noisy
                    return -1L;
                }
                int pieceType = fullBoardArray[r][c];
                if (PATTERN_PIECES.contains(pieceType)) {
                    // Only copy pattern pieces to the pattern board
                    patternBoard[r][c] = pieceType;
                }
                // Non-pattern pieces remain EMPTY in patternBoard
            }
        }
        // Serialize the board containing *only* pattern pieces
        try {
            return BoardSerializer.serialize(patternBoard);
        } catch (IllegalArgumentException e) {
            System.err.println("Error serializing pattern board: " + e.getMessage());
            // BoardSerializer.printBoard(patternBoard); // Print the problematic board for debugging
            return -1L; // Indicate error
        }
    }

    // --- PDB Move Generation ---

    /**
     * Generates valid next pattern states reachable in one move within the PDB world.
     * Moves only pattern pieces (CC, S), treating other pattern pieces as obstacles
     * and all other space as empty. This is used to find predecessors in backward BFS.
     *
     * @param currentPatternLayout The layout (long) of the current pattern state.
     * @return A list of layouts (long) of the next possible pattern states.
     */
    private List<Long> generatePDBMoves(long currentPatternLayout) {
        List<Long> nextLayouts = new ArrayList<>();
        int[][] board = BoardSerializer.deserialize(currentPatternLayout); // This is the pattern board
        final int ROWS = BoardSerializer.ROWS;
        final int COLS = BoardSerializer.COLS;
        final int EMPTY = BoardSerializer.EMPTY;
        boolean[][] processed = new boolean[ROWS][COLS]; // Track processed piece origins

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int pieceType = board[r][c];
                // Only consider moving pattern pieces, skip empty or already processed
                if (!PATTERN_PIECES.contains(pieceType) || processed[r][c]) {
                    continue;
                }

                // Determine piece dimensions and cells (simplified for CC and S)
                List<int[]> pieceCells = new ArrayList<>();
                boolean isCaoCao = (pieceType == BoardSerializer.CAO_CAO);
                boolean validPiece = true; // Flag to check if piece formation is correct

                if (isCaoCao) {
                    // Verify and collect all 4 cells of CC
                    if (r + 1 < ROWS && c + 1 < COLS &&
                            board[r][c+1] == pieceType && board[r+1][c] == pieceType && board[r+1][c+1] == pieceType) {
                        pieceCells.add(new int[]{r, c}); pieceCells.add(new int[]{r, c + 1});
                        pieceCells.add(new int[]{r + 1, c}); pieceCells.add(new int[]{r + 1, c + 1});
                        // Mark all CC cells as processed
                        processed[r][c] = true; processed[r][c+1] = true;
                        processed[r+1][c] = true; processed[r+1][c+1] = true;
                    } else {
                        // Found a CC cell but it's not part of a valid 2x2 block
                        validPiece = false;
                        // This might indicate an issue if it happens often, but we just skip
                    }
                } else { // Must be Soldier (1x1)
                    pieceCells.add(new int[]{r, c});
                    processed[r][c] = true;
                }

                // If piece formation was invalid (e.g., broken CC), skip trying to move it
                if (!validPiece) continue;

                // Try moving the identified piece in 4 directions
                for (int dir = 0; dir < 4; dir++) {
                    int dr = DR[dir], dc = DC[dir];
                    boolean canMove = true;
                    List<int[]> targetCells = new ArrayList<>(); // Cells the piece will occupy after moving

                    // Check 1: Are all target cells within board bounds?
                    for (int[] cell : pieceCells) {
                        int nr = cell[0] + dr;
                        int nc = cell[1] + dc;
                        if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS) {
                            canMove = false;
                            break;
                        }
                        targetCells.add(new int[]{nr, nc});
                    }
                    if (!canMove) continue; // Skip direction if out of bounds

                    // Check 2: Are all target cells EMPTY in the *pattern board*?
                    // A target cell is valid if it's EMPTY OR if it's currently occupied by
                    // another part of the *same* piece that is moving.
                    for (int[] targetCell : targetCells) {
                        int tr = targetCell[0];
                        int tc = targetCell[1];
                        boolean isPartOfMovingPiece = false;
                        // Check if the target cell overlaps with the original position
                        for(int[] originalCell : pieceCells) {
                            if (tr == originalCell[0] && tc == originalCell[1]) {
                                isPartOfMovingPiece = true;
                                break;
                            }
                        }
                        // If the target cell is *not* part of the moving piece's original footprint,
                        // it MUST be EMPTY (occupied only by other pattern pieces counts as blocked).
                        if (!isPartOfMovingPiece && board[tr][tc] != EMPTY) {
                            canMove = false;
                            break;
                        }
                    }
                    if (!canMove) continue; // Skip direction if blocked by another pattern piece

                    // --- If the move is valid, create the new pattern board state ---
                    // Create a copy of the current pattern board
                    int[][] nextBoard = new int[ROWS][COLS];
                    for (int i = 0; i < ROWS; i++) {
                        nextBoard[i] = Arrays.copyOf(board[i], COLS); // Defensive copy of each row
                    }
                    // Empty the original piece cells
                    for (int[] cell : pieceCells) {
                        nextBoard[cell[0]][cell[1]] = EMPTY;
                    }
                    // Place the piece in the target cells
                    for (int[] cell : targetCells) {
                        nextBoard[cell[0]][cell[1]] = pieceType;
                    }
                    // Serialize the new pattern board and add its layout to the results
                    try {
                        nextLayouts.add(BoardSerializer.serialize(nextBoard));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Error serializing next PDB board state: " + e.getMessage());
                        // BoardSerializer.printBoard(nextBoard); // Debugging
                        // Optionally continue or stop generation based on severity
                    }
                } // End direction loop
            } // End column loop
        } // End row loop
        return nextLayouts;
    }

    // --- Backward BFS for PDB Generation ---

    /**
     * Generates the Pattern Database using backward BFS starting from the target state.
     * Calculates the minimum moves from any reachable pattern state TO the target state.
     *
     * @return A Map where keys are pattern state layouts (long) and values are
     *         the minimum distance (moves) to the PDB_TARGET_LAYOUT. Returns null if generation fails.
     */
    public Map<Long, Integer> generatePDB() {
        System.out.println("Starting PDB Generation (Pattern: CaoCao + 4 Soldiers)...");
        // HashMap is suitable for single-threaded generation.
        Map<Long, Integer> pdbMap = new HashMap<>();
        // ArrayDeque is generally preferred over LinkedList for queue operations.
        Queue<Long> queue = new ArrayDeque<>();

        // Validate target layout before starting
        if (PDB_TARGET_LAYOUT == -1L) {
            System.err.println("PDB Generation failed: Invalid target layout defined.");
            return null;
        }

        // Start BFS from the canonical target pattern state
        pdbMap.put(PDB_TARGET_LAYOUT, 0); // Distance from target to target is 0
        queue.add(PDB_TARGET_LAYOUT);

        long statesExplored = 0;
        int maxDistance = 0; // Track the maximum distance found
        long startTime = System.currentTimeMillis();
        long lastLogTime = startTime;

        while (!queue.isEmpty()) {
            long currentLayout = queue.poll();
            // Retrieve distance; should always exist for states in the queue
            int currentDistance = pdbMap.getOrDefault(currentLayout, -1);
            if (currentDistance == -1) {
                System.err.println("Error: State found in queue but not in pdbMap: " + currentLayout);
                continue; // Skip this state
            }

            maxDistance = Math.max(maxDistance, currentDistance);
            statesExplored++;

            // Progress indicator - Log every second or every N states
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastLogTime > 1000 || statesExplored % 250000 == 0) {
                System.out.printf("  [%.1f s] Explored: %.2f M, Queue: %d k, Current Dist: %d, Max Dist: %d\n",
                        (currentTime - startTime) / 1000.0,
                        statesExplored / 1_000_000.0,
                        queue.size() / 1000,
                        currentDistance,
                        maxDistance);
                lastLogTime = currentTime;
            }


            // Generate predecessor states (states from which currentLayout is reachable in one move)
            // This is done by generating successors *from* currentLayout in the PDB world.
            List<Long> predecessorLayouts = generatePDBMoves(currentLayout);

            for (long predLayout : predecessorLayouts) {
                // If this predecessor state hasn't been visited yet (i.e., not in pdbMap)
                if (!pdbMap.containsKey(predLayout)) {
                    // The distance from this predecessor to the target is one more than
                    // the distance from the current state to the target.
                    pdbMap.put(predLayout, currentDistance + 1);
                    queue.add(predLayout);
                }
                // If already visited, BFS guarantees the first time we reached it was via a shortest path
                // from the target, so we ignore subsequent encounters.
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("-----------------------------------------------------");
        System.out.println("PDB Generation Complete.");
        System.out.printf("  Total States Found: %,d\n", pdbMap.size());
        System.out.printf("  Maximum Distance (Heuristic Value): %d\n", maxDistance);
        System.out.printf("  Total Time: %.3f s\n", (endTime - startTime) / 1000.0);
        System.out.println("-----------------------------------------------------");


        return pdbMap;
    }

    // --- Persistence ---

    /**
     * Saves the generated PDB map to a file using Java serialization.
     * @param pdbMap The map to save. Should not be null.
     * @param filename The name of the file to save to.
     * @return true if saving was successful, false otherwise.
     */
    public boolean savePDB(Map<Long, Integer> pdbMap, String filename) {
        if (pdbMap == null) { // Check for null map
            System.err.println("PDB map is null. Nothing to save.");
            return false;
        }
        if (pdbMap.isEmpty()) {
            System.out.println("PDB map is empty. Saving an empty map.");
            // Allow saving empty map if needed, or return false based on requirements
        } else {
            System.out.printf("Saving PDB map (%,d entries) to %s...\n", pdbMap.size(), filename);
        }

        // Use try-with-resources for automatic stream closing
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream( // Add buffering for potentially better performance
                        new FileOutputStream(filename)))) {
            oos.writeObject(pdbMap);
            System.out.println("PDB saved successfully.");
            return true;
        } catch (IOException e) {
            System.err.println("Error saving PDB to file " + filename + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads a PDB map from a file.
     * @param filename The name of the file to load from.
     * @return The loaded Map<Long, Integer>, or null if loading fails or file not found.
     */
    @SuppressWarnings("unchecked") // Suppress warning for the cast from Object to Map<Long, Integer>
    public Map<Long, Integer> loadPDB(String filename) {
        System.out.println("Loading PDB map from " + filename + "...");
        File pdbFile = new File(filename);
        if (!pdbFile.exists()) {
            System.err.println("PDB file not found: " + filename);
            return null;
        }
        if (!pdbFile.isFile() || !pdbFile.canRead()) {
            System.err.println("Cannot read PDB file (not a file or insufficient permissions): " + filename);
            return null;
        }

        // Use try-with-resources for automatic stream closing
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream( // Add buffering
                        new FileInputStream(filename)))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                // Basic type check on the map instance itself
                Map<?, ?> rawMap = (Map<?, ?>) obj;
                // More robust check: verify key/value types of the first entry if map is not empty
                if (!rawMap.isEmpty()) {
                    Map.Entry<?, ?> entry = rawMap.entrySet().iterator().next();
                    if (!(entry.getKey() instanceof Long) || !(entry.getValue() instanceof Integer)) {
                        System.err.println("Error: PDB file contains unexpected key/value types (Expected Long -> Integer).");
                        return null;
                    }
                }
                System.out.printf("PDB loaded successfully (%,d entries).\n", rawMap.size());
                return (Map<Long, Integer>) obj; // Perform the cast
            } else {
                System.err.println("Error: Loaded object is not a Map.");
                return null;
            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) { // Catch potential ClassCastException too
            System.err.println("Error loading PDB from file " + filename + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    // --- Main method for running the generator and saving ---
    public static void main(String[] args) {
        PatternDatabaseGenerator generator = new PatternDatabaseGenerator();
        // Define a standard filename for this PDB
        String pdbFilename = "klotski_pdb_cc_2h_4v.ser"; // .ser is common for serialized Java objects

        // --- Generation Phase ---
        // This can take significant time and memory.
        Map<Long, Integer> pdb = generator.generatePDB();

        // --- Saving Phase ---
        if (pdb != null) { // Check if generation was successful (returned non-null map)
            if (!pdb.isEmpty()) {
                generator.savePDB(pdb, pdbFilename);
            } else {
                System.out.println("PDB generation resulted in an empty map. Check target state and generation logic.");
                // Decide whether to save an empty file or not. Saving might be okay.
                generator.savePDB(pdb, pdbFilename);
            }
        } else {
            System.err.println("PDB generation failed. Not saving.");
        }

        // --- Optional: Loading Test ---
        // You can uncomment this section to test if loading works immediately after saving.

        System.out.println("\n--- Testing PDB loading ---");
        Map<Long, Integer> loadedPdb = generator.loadPDB(pdbFilename);
        if (loadedPdb != null) {
            System.out.printf("Loaded PDB map size: %,d\n", loadedPdb.size());
            // Example lookup test (using the known target layout)
            long targetLayout = PDB_TARGET_LAYOUT;
            int distance = loadedPdb.getOrDefault(targetLayout, -1);
            System.out.println("Lookup distance for target layout (" + targetLayout + "): " + distance + " (Expected: 0)");

            // Add another test case if you have a known layout and its expected distance
        } else {
            System.out.println("PDB loading test failed.");
        }

    }
}