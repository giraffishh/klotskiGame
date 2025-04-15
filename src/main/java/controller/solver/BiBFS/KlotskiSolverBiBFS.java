package controller.solver.BiBFS;

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
 * Solves the Klotski (Hua Rong Dao) puzzle using Bidirectional Breadth-First Search (Bi-BFS).
 * 使用双向广度优先搜索（Bi-BFS）解决华容道问题。
 */
public class KlotskiSolverBiBFS {

    // Constants for directions (optional, but can make code clearer)
    // 方向常量（可选，但能让代码更清晰）
    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;
    private static final int[] DR = {-1, 1, 0, 0}; // Change in row for UP, DOWN, LEFT, RIGHT 行变化
    private static final int[] DC = {0, 0, -1, 1}; // Change in col for UP, DOWN, LEFT, RIGHT 列变化

    private int nodesExplored = 0; // 添加节点计数变量

    /**
     * 返回探索的节点数量
     * @return 算法执行过程中探索的节点数量
     */
    public int getNodesExplored() {
        return nodesExplored;
    }

    /**
     * Solves the Klotski puzzle using Bidirectional BFS.
     * 使用双向 BFS 解决华容道难题。
     *
     * @param initialState The starting board configuration. 初始棋盘配置。
     * @param targetState The target board configuration (must be specific for Bi-BFS). 目标棋盘配置（对于 Bi-BFS 必须是具体的）。
     * @return A list of BoardStateTrieTree objects representing the shortest path from
     *         the initial state to the target state, or an empty list if no solution is found.
     *         代表从初始状态到目标状态最短路径的 BoardStateTrieTree 对象列表，如果找不到解则返回空列表。
     */
    public List<BoardState> solve(BoardState initialState, BoardState targetState) {
        if (initialState.equals(targetState)) {
            return Collections.singletonList(initialState);
        }

        Queue<BoardState> forwardQueue = new ArrayDeque<>();
        Map<BoardState, BoardState> forwardVisited = new HashMap<>(); // state -> predecessor

        Queue<BoardState> backwardQueue = new ArrayDeque<>();
        Map<BoardState, BoardState> backwardVisited = new HashMap<>(); // state -> predecessor (from target's perspective)

        forwardQueue.offer(initialState);
        forwardVisited.put(initialState, null); // Mark initial state visited from forward

        backwardQueue.offer(targetState);
        backwardVisited.put(targetState, null); // Mark target state visited from backward

        BoardState meetingNode = null;
        nodesExplored = 0; // 重置节点计数

        while (!forwardQueue.isEmpty() && !backwardQueue.isEmpty()) {
            // Expand forward search by one level
            int forwardLevelSize = forwardQueue.size();
            for (int i = 0; i < forwardLevelSize; i++) {
                BoardState currentForward = forwardQueue.poll();
                nodesExplored++; // 增加节点计数（正向搜索）

                // Check for intersection before generating successors
                if (backwardVisited.containsKey(currentForward)) {
                    meetingNode = currentForward;
                    //System.out.println("Intersection found from forward search!");
                    break; // Exit inner loop
                }

                List<BoardState> successors = generateSuccessors(currentForward);
                for (BoardState successor : successors) {
                    if (!forwardVisited.containsKey(successor)) {
                        forwardVisited.put(successor, currentForward);
                        forwardQueue.offer(successor);

                        // Check for intersection immediately after adding
                        // (Alternative: check at the beginning of the loop)
                        // if (backwardVisited.containsKey(successor)) {
                        //    meetingNode = successor;
                        //    System.out.println("Intersection found after forward step!");
                        //    break; // Exit inner loop (successors)
                        // }
                    }
                }
                // if (meetingNode != null) break; // Exit outer loop (level)
            }
            if (meetingNode != null) break; // Exit while loop


            // Expand backward search by one level
            int backwardLevelSize = backwardQueue.size();
            for (int i = 0; i < backwardLevelSize; i++) {
                BoardState currentBackward = backwardQueue.poll();
                nodesExplored++; // 增加节点计数（反向搜索）

                // Check for intersection before generating successors
                if (forwardVisited.containsKey(currentBackward)) {
                    meetingNode = currentBackward;
                    //System.out.println("Intersection found from backward search!");
                    break; // Exit inner loop
                }

                List<BoardState> predecessors = generateSuccessors(currentBackward); // Successors are predecessors in backward search
                for (BoardState predecessor : predecessors) {
                    if (!backwardVisited.containsKey(predecessor)) {
                        backwardVisited.put(predecessor, currentBackward);
                        backwardQueue.offer(predecessor);

                        // Check for intersection immediately after adding
                        // if (forwardVisited.containsKey(predecessor)) {
                        //    meetingNode = predecessor;
                        //    System.out.println("Intersection found after backward step!");
                        //    break; // Exit inner loop (predecessors)
                        // }
                    }
                }
                // if (meetingNode != null) break; // Exit outer loop (level)
            }
            if (meetingNode != null) break; // Exit while loop

            if (nodesExplored % 50000 == 0) { // Print progress occasionally
                //System.out.println("Levels processed (approx): " + nodesExplored +
                       // ", FwdQ: " + forwardQueue.size() + ", BwdQ: " + backwardQueue.size() +
                        //", FwdV: " + forwardVisited.size() + ", BwdV: " + backwardVisited.size());
            }
        }


        if (meetingNode != null) {
            //System.out.println("Solution found! Reconstructing path via meeting node: " + meetingNode);
            return reconstructBiBFSPath(forwardVisited, backwardVisited, meetingNode);
        } else {
            //System.out.println("No solution found after processing levels (approx): " + nodesExplored);
            return Collections.emptyList();
        }
    }

    /**
     * Generates all valid next board states from the current state by moving each piece.
     * 通过移动每个棋子，从当前状态生成所有有效的下一步棋盘状态。
     * (Code is identical to the previous version)
     * (代码与上一版本相同)
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

                        // Create new BoardStateTrieTree and add to successors
                        // 创建新的 BoardStateTrieTree 并添加到后继列表中
                        successors.add(new BoardState(nextBoard)); // Constructor serializes the array 构造函数序列化数组
                    }
                }
            }
        }
        return successors;
    }


    /**
     * Reconstructs the path found by Bi-BFS.
     * 重建 Bi-BFS 找到的路径。
     *
     * @param forwardVisited Map tracking predecessors from the initial state. 从初始状态跟踪前驱的 Map。
     * @param backwardVisited Map tracking predecessors from the target state. 从目标状态跟踪前驱的 Map。
     * @param meetingNode The state where the two searches met. 两个搜索相遇的状态。
     * @return The complete path from initial to target state. 从初始到目标状态的完整路径。
     */
    private List<BoardState> reconstructBiBFSPath(Map<BoardState, BoardState> forwardVisited,
                                                  Map<BoardState, BoardState> backwardVisited,
                                                  BoardState meetingNode) {
        LinkedList<BoardState> path = new LinkedList<>();

        // Reconstruct path from meetingNode back to initialState
        BoardState current = meetingNode;
        while (current != null) {
            path.addFirst(current);
            current = forwardVisited.get(current);
        }

        // Reconstruct path from meetingNode (predecessor in backward search) back to targetState
        current = backwardVisited.get(meetingNode); // Start from the node *before* the meeting node in the backward path
        while (current != null) {
            path.addLast(current); // Append to the end
            current = backwardVisited.get(current);
        }

        return path;
    }

    // --- Main method for testing ---
    public static void main(String[] args) {
        System.out.println("Klotski Solver with Bidirectional BFS Test");
        System.out.println("NOTE: Requires BoardSerializer class to be fully implemented and accessible.");
        System.out.println("      Requires a SPECIFIC target state for backward search.");

        // --- Example Setup ---
        try {
            // "横刀立马" initial state
            int[][] initialArray = {
                    {BoardSerializer.EMPTY, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.EMPTY},
                    {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.SOLDIER},
                    {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.SOLDIER}
            };


            // Define a SPECIFIC target state for Bi-BFS
            // 为 Bi-BFS 定义一个具体的目标状态
            // Example: Cao Cao at bottom center, a common known solvable end state.
            // 示例：曹操在底部中央，一个常见的已知可解最终状态。
            int[][] targetArray = {
                    {BoardSerializer.VERTICAL, BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER},
                    {BoardSerializer.VERTICAL, BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                    {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.EMPTY},
                    {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.EMPTY}
            };
            // Alternative simpler target (might be unreachable from some starts)
            /* int[][] targetArray = {
                    {1, 2, 2, 1},
                    {1, 2, 2, 1},
                    {3, 0, 0, 3},
                    {3, 4, 4, 3},
                    {0, 4, 4, 0} // Example target, might need adjustment
            };*/


            // Create BoardStateTrieTree objects
            BoardState initialState = new BoardState(initialArray);
            BoardState targetState = new BoardState(targetArray);

            System.out.println("Initial State (Layout: " + initialState.getLayout() + ", Hash: " + initialState.hashCode() + "):");
            BoardSerializer.printBoard(initialState.getBoardArray());

            System.out.println("\nTarget State (Layout: " + targetState.getLayout() + ", Hash: " + targetState.hashCode() + "):");
            BoardSerializer.printBoard(targetState.getBoardArray());


            KlotskiSolverBiBFS solver = new KlotskiSolverBiBFS();

            System.out.println("\nStarting Bi-BFS solve...");
            long startTime = System.currentTimeMillis();
            // Call solve with both initial and target states
            List<BoardState> path = solver.solve(initialState, targetState);
            long endTime = System.currentTimeMillis();
            System.out.println("\nSolve finished in " + (endTime - startTime) + " ms.");


            if (path.isEmpty()) {
                System.out.println("No solution found (or path reconstruction failed).");
            } else {
                System.out.println("Solution found with " + (path.size() - 1) + " steps.");
                // Optionally print the path (can be very long)
                /* // Uncomment to print the full path
                int step = 0;
                for (BoardStateTrieTree state : path) {
                    System.out.println("\nStep " + step++);
                    BoardSerializer.printBoard(state.getBoardArray());
                    // System.out.println("  Layout: " + state.getLayout());
                }
                */
                // Print only the final state
                System.out.println("\nFinal State (Step " + (path.size() - 1) + "):");
                BoardSerializer.printBoard(path.get(path.size() - 1).getBoardArray());
                // System.out.println("  Layout: " + path.get(path.size() - 1).getLayout());

            }

        } catch (Exception e) {
            System.err.println("An error occurred during the test setup or solve: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
