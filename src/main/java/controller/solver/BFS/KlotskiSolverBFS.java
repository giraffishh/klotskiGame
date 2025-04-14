package controller.solver.BFS;

import controller.solver.BoardSerializer;
import controller.solver.BoardState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;


/**
 * Solves the Klotski (Hua Rong Dao) puzzle using Breadth-First Search (BFS).
 * 使用广度优先搜索（BFS）解决华容道问题。
 */
public class KlotskiSolverBFS {

    // Constants for directions (optional, but can make code clearer)
    // 方向常量（可选，但能让代码更清晰）
    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;
    private static final int[] DR = {-1, 1, 0, 0}; // Change in row for UP, DOWN, LEFT, RIGHT 行变化
    private static final int[] DC = {0, 0, -1, 1}; // Change in col for UP, DOWN, LEFT, RIGHT 列变化

    // 添加节点计数器
    private int nodesExplored = 0;

    // 添加获取节点数的getter方法
    public int getNodesExplored() {
        return nodesExplored;
    }

    /**
     * Checks if the given board state represents the winning condition.
     * The winning condition is defined as the 2x2 Cao Cao block
     * occupying the bottom center position (cells [3][1], [3][2], [4][1], [4][2]).
     *
     * 检查给定的棋盘状态是否满足获胜条件。
     * 获胜条件定义为 2x2 的曹操方块占据底部中央位置
     * （单元格 [3][1], [3][2], [4][1], [4][2]）。
     *
     * @param state The board state to check. 要检查的棋盘状态。
     * @return true if the state is a winning state, false otherwise. 如果是获胜状态则返回 true，否则返回 false。
     */
    private boolean isGoalState(BoardState state) {
        int[][] board = state.getBoardArray(); // Get the board representation
        // Check the four cells for Cao Cao (value 4)
        return board[3][1] == BoardSerializer.CAO_CAO &&
                board[3][2] == BoardSerializer.CAO_CAO &&
                board[4][1] == BoardSerializer.CAO_CAO &&
                board[4][2] == BoardSerializer.CAO_CAO;
    }

    /**
     * Solves the Klotski puzzle starting from the initial state using BFS.
     * 使用 BFS 从初始状态开始解决华容道难题。
     *
     * @param initialState The starting board configuration. 初始棋盘配置。
     * @return A list of BoardState objects representing the shortest path from
     *         the initial state to a goal state, or an empty list if no solution is found.
     *         代表从初始状态到目标状态最短路径的 BoardState 对象列表，如果找不到解则返回空列表。
     */
    public List<BoardState> solve(BoardState initialState) { // Removed targetState parameter
        // 重置节点计数器
        nodesExplored = 0;

        Queue<BoardState> queue = new ArrayDeque<>();
        // Using a HashMap with potentially millions of entries benefits greatly
        // from a good hashCode distribution.
        // 对于可能有数百万条目的 HashMap，良好的 hashCode 分布会带来很大好处。
        Map<BoardState, BoardState> visitedMap = new HashMap<>(); // Stores state -> predecessor

        queue.offer(initialState);
        visitedMap.put(initialState, null); // Initial state has no predecessor
        nodesExplored++; // 计数初始状态

        int statesProcessed = 0; // Counter for debugging/performance check 状态处理计数器

        while (!queue.isEmpty()) {
            BoardState currentState = queue.poll();
            statesProcessed++;
            if (statesProcessed % 100000 == 0) { // Print progress less frequently
                System.out.println("States processed: " + statesProcessed + ", Queue size: " + queue.size() + ", Visited size: " + visitedMap.size());
            }

            // Check if the current state is a goal state using the new method
            // 使用新方法检查当前状态是否为目标状态
            if (isGoalState(currentState)) {
                // System.out.println("Solution found after processing " + statesProcessed + " states! Reconstructing path...");
                // Reconstruct path ending at the winning currentState
                // 重建以获胜的 currentState 结尾的路径
                return reconstructPath(visitedMap, currentState);
            }

            // Generate successors using the newly implemented method
            // 使用新实现的方法生成后继状态
            List<BoardState> successors = generateSuccessors(currentState);

            for (BoardState successor : successors) {
                // The containsKey check now uses the folding hashCode.
                // containsKey 检查现在使用折叠哈希码。
                if (!visitedMap.containsKey(successor)) {
                    visitedMap.put(successor, currentState); // Store successor and its predecessor
                    queue.offer(successor);
                    nodesExplored++; // 计数每个加入队列的新节点
                }
            }
        }

        System.out.println("No solution found after processing " + statesProcessed + " states.");
        return Collections.emptyList(); // Return empty list if no solution
    }

    /**
     * Generates all valid next board states from the current state by moving each piece.
     * 通过移动每个棋子，从当前状态生成所有有效的下一步棋盘状态。
     *
     * @param currentState The current board state. 当前棋盘状态。
     * @return A list of valid successor board states. 有效后继棋盘状态的列表。
     */
    private List<BoardState> generateSuccessors(BoardState currentState) {
        List<BoardState> successors = new ArrayList<>();
        // Ensure BoardSerializer constants are accessible
        // 确保 BoardSerializer 常量可访问
        final int ROWS = BoardSerializer.ROWS;
        final int COLS = BoardSerializer.COLS;
        final int EMPTY = BoardSerializer.EMPTY;

        int[][] board = currentState.getBoardArray(); // Get the board representation 获取棋盘表示
        boolean[][] processed = new boolean[ROWS][COLS]; // Track processed top-left corners 跟踪已处理的左上角

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int pieceType = board[r][c];

                // Skip empty cells or already processed pieces
                // 跳过空格或已处理的棋子
                if (pieceType == EMPTY || processed[r][c]) {
                    continue;
                }

                // Identify the piece and its dimensions based on its top-left corner (r, c)
                // 根据左上角 (r, c) 识别棋子及其尺寸
                int pieceHeight = 1;
                int pieceWidth = 1;
                List<int[]> pieceCells = new ArrayList<>(); // Store cells occupied by this piece 存储此棋子占用的单元格

                // Use constants from BoardSerializer
                // 使用 BoardSerializer 中的常量
                switch (pieceType) {
                    case BoardSerializer.SOLDIER: // 1x1
                        pieceCells.add(new int[]{r, c});
                        processed[r][c] = true;
                        break;
                    case BoardSerializer.HORIZONTAL: // 1x2
                        // Basic boundary check before accessing c+1
                        // 访问 c+1 前的基本边界检查
                        if (c + 1 < COLS && board[r][c+1] == pieceType) {
                            pieceWidth = 2;
                            pieceCells.add(new int[]{r, c});
                            pieceCells.add(new int[]{r, c + 1});
                            processed[r][c] = true;
                            processed[r][c + 1] = true;
                        } else { continue; } // Invalid state or piece already processed part 状态无效或棋子部分已处理
                        break;
                    case BoardSerializer.VERTICAL: // 2x1
                        // Basic boundary check before accessing r+1
                        // 访问 r+1 前的基本边界检查
                        if (r + 1 < ROWS && board[r+1][c] == pieceType) {
                            pieceHeight = 2;
                            pieceCells.add(new int[]{r, c});
                            pieceCells.add(new int[]{r + 1, c});
                            processed[r][c] = true;
                            processed[r+1][c] = true;
                        } else { continue; } // Invalid state or piece already processed part 状态无效或棋子部分已处理
                        break;
                    case BoardSerializer.CAO_CAO: // 2x2
                        // Basic boundary checks before accessing neighbors
                        // 访问邻居前的基本边界检查
                        if (r + 1 < ROWS && c + 1 < COLS &&
                                board[r][c+1] == pieceType &&
                                board[r+1][c] == pieceType &&
                                board[r+1][c+1] == pieceType) {
                            pieceHeight = 2;
                            pieceWidth = 2;
                            pieceCells.add(new int[]{r, c});
                            pieceCells.add(new int[]{r, c + 1});
                            pieceCells.add(new int[]{r + 1, c});
                            pieceCells.add(new int[]{r + 1, c + 1});
                            processed[r][c] = true;
                            processed[r][c+1] = true;
                            processed[r+1][c] = true;
                            processed[r+1][c+1] = true;
                        } else { continue; } // Invalid state or piece already processed part 状态无效或棋子部分已处理
                        break;
                    default:
                        // Should not happen with valid input from BoardSerializer
                        // 使用来自 BoardSerializer 的有效输入时不应发生
                        System.err.println("Warning: Unknown piece type " + pieceType + " at [" + r + "," + c + "]");
                        continue;
                }

                // Try moving this piece in all 4 directions
                // 尝试将此棋子向4个方向移动
                for (int dir = 0; dir < 4; dir++) {
                    int dr = DR[dir]; // Delta row
                    int dc = DC[dir]; // Delta col

                    boolean canMove = true;
                    List<int[]> targetCells = new ArrayList<>(); // Cells the piece would occupy after moving 移动后棋子将占用的单元格

                    // Determine target cells and check boundaries
                    // 确定目标单元格并检查边界
                    for (int[] cell : pieceCells) {
                        int nr = cell[0] + dr;
                        int nc = cell[1] + dc;

                        // Boundary check
                        // 边界检查
                        if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS) {
                            canMove = false;
                            break;
                        }
                        targetCells.add(new int[]{nr, nc});
                    }

                    if (!canMove) continue; // Cannot move in this direction due to boundary 由于边界无法朝此方向移动

                    // Check if target cells are either empty or part of the current piece itself
                    // 检查目标单元格是否为空或是当前棋子本身的一部分
                    for (int[] targetCell : targetCells) {
                        int tr = targetCell[0];
                        int tc = targetCell[1];
                        boolean isPartOfCurrentPiece = false;
                        // Check if the target cell is one of the original cells of the piece
                        // 检查目标单元格是否是棋子的原始单元格之一
                        for(int[] originalCell : pieceCells) {
                            if (tr == originalCell[0] && tc == originalCell[1]) {
                                isPartOfCurrentPiece = true;
                                break;
                            }
                        }
                        // If the target cell is not part of the moving piece, it must be empty
                        // 如果目标单元格不是移动棋子的一部分，则必须为空
                        if (!isPartOfCurrentPiece && board[tr][tc] != EMPTY) {
                            canMove = false;
                            break;
                        }
                    }


                    if (canMove) {
                        // Create a new board state for the valid move
                        // 为有效移动创建新的棋盘状态
                        int[][] nextBoard = new int[ROWS][COLS];
                        for (int i = 0; i < ROWS; i++) {
                            nextBoard[i] = Arrays.copyOf(board[i], COLS);
                        }

                        // Clear original piece position
                        // 清空原始棋子位置
                        for (int[] cell : pieceCells) {
                            nextBoard[cell[0]][cell[1]] = EMPTY;
                        }
                        // Place piece in new position
                        // 将棋子放置在新位置
                        for (int[] cell : targetCells) {
                            nextBoard[cell[0]][cell[1]] = pieceType;
                        }

                        // Create new BoardState and add to successors
                        // 创建新的 BoardState 并添加到后继列表中
                        successors.add(new BoardState(nextBoard)); // Constructor serializes the array 构造函数序列化数组
                    }
                }
            }
        }
        return successors;
    }


    /**
     * Reconstructs the path from the initial state to the target state using the visited map.
     * 使用 visitedMap 从初始状态重建到目标状态的路径。
     *
     * @param visitedMap A map where keys are states and values are their predecessors. 键是状态，值是其前驱的映射。
     * @param targetState The final state reached. 到达的最终状态。
     * @return A list of BoardState objects representing the path, from initial to target. 代表路径的 BoardState 对象列表，从初始到目标。
     */
    private List<BoardState> reconstructPath(Map<BoardState, BoardState> visitedMap, BoardState targetState) {
        LinkedList<BoardState> path = new LinkedList<>();
        BoardState current = targetState;
        while (current != null) {
            path.addFirst(current); // Add to the beginning to reverse the path
            current = visitedMap.get(current); // Move to the predecessor
        }
        return path;
    }

    // --- Main method for testing ---
    public static void main(String[] args) {
        System.out.println("Klotski Solver with Folding Hash and Dynamic Goal Test");
        System.out.println("NOTE: Requires BoardSerializer class to be fully implemented and accessible.");
        System.out.println("      Goal is Cao Cao at bottom center.");

        // --- Example Setup ---
        try {
            // "横刀立马" initial state
            // “横刀立马” 初始状态
            int[][] initialArray = {
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER}
            };


            // Create BoardState objects (constructor now handles serialization)
            // 创建 BoardState 对象（构造函数现在处理序列化）
            BoardState initialState = new BoardState(initialArray);
            // Target state is no longer predefined, the isGoalState method handles the check.
            // 目标状态不再预定义，isGoalState 方法处理检查。

            System.out.println("Initial State (Layout: " + initialState.getLayout() + ", Hash: " + initialState.hashCode() + "):");
            BoardSerializer.printBoard(initialState.getBoardArray()); // Assumes printBoard exists 假设 printBoard 存在

            // Removed target state printing as it's now dynamic

            KlotskiSolverBFS solver = new KlotskiSolverBFS();

            System.out.println("\nStarting solve...");
            long startTime = System.currentTimeMillis();
            // Call solve without the targetState argument
            List<BoardState> path = solver.solve(initialState);
            long endTime = System.currentTimeMillis();
            System.out.println("\nSolve finished in " + (endTime - startTime) + " ms.");


            if (path.isEmpty()) {
                System.out.println("No solution found.");
            } else {
                System.out.println("Solution found with " + (path.size() - 1) + " steps.");
                // Optionally print the path (can be very long)
                // 可选：打印路径（可能非常长）
                /* // Uncomment to print the full path
                int step = 0;
                for (BoardState state : path) {
                    System.out.println("\nStep " + step++);
                    BoardSerializer.printBoard(state.getBoardArray());
                    System.out.println("  Layout: " + state.getLayout());
                }
                */
                // Print only the final state
                System.out.println("\nFinal State (Step " + (path.size() - 1) + "):");
                BoardSerializer.printBoard(path.get(path.size() - 1).getBoardArray());
                System.out.println("  Layout: " + path.get(path.size() - 1).getLayout());

            }

        } catch (Exception e) {
            System.err.println("An error occurred during the test setup or solve: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
