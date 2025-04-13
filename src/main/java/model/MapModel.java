package model;

/**
 * This class is to record the map of one game. For example:
 */
public class MapModel {
    int[][] matrix;
    int[][] initialMatrix; // 保存初始地图状态

    public MapModel(int[][] matrix) {
        this.matrix = matrix;
        // 保存初始地图状态的深拷贝
        this.initialMatrix = new int[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            System.arraycopy(matrix[i], 0, initialMatrix[i], 0, matrix[i].length);
        }
    }

    public int getWidth() {
        return this.matrix[0].length;
    }

    public int getHeight() {
        return this.matrix.length;
    }

    public int getId(int row, int col) {
        return matrix[row][col];
    }

    public int[][] getMatrix() {
        return matrix;
    }

    public boolean checkInWidthSize(int col) {
        return col >= 0 && col < matrix[0].length;
    }

    public boolean checkInHeightSize(int row) {
        return row >= 0 && row < matrix.length;
    }

    /**
     * 重置地图到初始状态
     */
    public void resetToInitialState() {
        for (int i = 0; i < matrix.length; i++) {
            System.arraycopy(initialMatrix[i], 0, matrix[i], 0, matrix[i].length);
        }
    }
}
