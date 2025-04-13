package controller.save;

/**
 * 负责处理游戏地图状态的序列化和反序列化
 * 提供将二维矩阵转换为字符串表示和将字符串解析回矩阵的方法
 */
public class MapStateSerializer {

    /**
     * 将二维矩阵转换为字符串表示
     * 
     * @param matrix 要转换的二维矩阵
     * @return 矩阵的字符串表示，格式为"[[a,b],[c,d]]"
     */
    public static String convertMatrixToString(int[][] matrix) {
        StringBuilder mapStateBuilder = new StringBuilder("[");
        for (int i = 0; i < matrix.length; i++) {
            mapStateBuilder.append("[");
            for (int j = 0; j < matrix[i].length; j++) {
                mapStateBuilder.append(matrix[i][j]);
                if (j < matrix[i].length - 1) {
                    mapStateBuilder.append(",");
                }
            }
            mapStateBuilder.append("]");
            if (i < matrix.length - 1) {
                mapStateBuilder.append(",");
            }
        }
        mapStateBuilder.append("]");
        return mapStateBuilder.toString();
    }

    /**
     * 将字符串表示转换回二维矩阵
     * 
     * @param mapState 矩阵的字符串表示，格式为"[[a,b],[c,d]]"
     * @return 解析后的二维矩阵
     */
    public static int[][] convertStringToMatrix(String mapState) {
        // 移除方括号
        mapState = mapState.substring(1, mapState.length() - 1);
        String[] rows = mapState.split("],\\[");

        // 修正第一行和最后一行的格式
        rows[0] = rows[0].substring(1);
        rows[rows.length - 1] = rows[rows.length - 1].substring(0, rows[rows.length - 1].length() - 1);

        int[][] newMatrix = new int[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            String[] cols = rows[i].split(",");
            newMatrix[i] = new int[cols.length];
            for (int j = 0; j < cols.length; j++) {
                newMatrix[i][j] = Integer.parseInt(cols[j].trim());
            }
        }
        return newMatrix;
    }
}
