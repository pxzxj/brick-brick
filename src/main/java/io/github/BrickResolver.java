package io.github;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BrickResolver {

    public static void main(String[] args) throws IOException {
        String brickTable = FileCopyUtils.copyToString(new FileReader("C:/Users/pxzxj1/Desktop/brick/brick.txt"));
        String[] brickLines = brickTable.split("\r\n");
        Map<String, AtomicInteger> typeCountMap = new HashMap<>();
        int[][] cellArray = new int[14][10];
        for (int i = 0; i < 14; i++) {
            String[] strings = brickLines[i].replace("[", "").replace("]", "").split(",\\s*");
            for (int j = 0; j < 10; j++) {
                cellArray[i][j] = Integer.parseInt(strings[j]);
                typeCountMap.computeIfAbsent(strings[j], k -> new AtomicInteger()).incrementAndGet();
            }
        }
        for (Map.Entry<String, AtomicInteger> entry : typeCountMap.entrySet()) {
            Assert.isTrue(entry.getValue().get() % 2 == 0, entry.getKey() + "数量非偶数");
        }
        List<Operation> operations = new ArrayList<>();
        boolean result = match(cellArray, 0, operations);
        for (Operation operation : operations) {
            System.out.println(operation);
        }
        Assert.isTrue(result, "得不到结果");


    }

    private static boolean match(int[][] cellArray, int matchCount, List<Operation> operations) {
        if (matchCount == 70) {
            return true;
        }
        //消掉的元素赋值为-1
        //先看右边跟下面是不是有相同的有就消掉
        for (int i = 0; i < 14; i++) {
            for (int j = 0; j < 10; j++) {
                if (cellArray[i][j] == -1) {
                    continue;
                }
                int right = -1;
                for (int k = j + 1; k < 10; k++) {
                    if (cellArray[i][k] != -1) {
                        right = k;
                        break;
                    }
                }
                if (right != -1 && cellArray[i][right] == cellArray[i][j]) {
                    cellArray[i][j] = -1;
                    cellArray[i][right] = -1;
                    operations.add(new Click(i, j));
                    matchCount++;
                }
                int down = -1;
                for (int k = i + 1; k < 14; k++) {
                    if (cellArray[k][j] != -1) {
                        down = k;
                        break;
                    }
                }
                if (down != -1 && cellArray[down][j] == cellArray[i][j]) {
                    cellArray[i][j] = -1;
                    cellArray[down][j] = -1;
                    operations.add(new Click(i, j));
                    matchCount++;
                }
            }
        }
        if (matchCount == 70) {
            return true;
        }
        for (int i = 0; i < 14; i++) {
            for (int j = 0; j < 10; j++) {
                if (cellArray[i][j] == -1) {
                    continue;
                }
                boolean result = move(cellArray, operations, i, j);
                if (result) {
                    matchCount++;
                } else {
                    continue;
                }
                int [][] cellArraySnapshot = new int[14][10];
                copyArray(cellArray, cellArraySnapshot);
                List<Operation> operationsSnapshot = new ArrayList<>(operations);
                result = match(cellArray, matchCount, operations);
                if (result) {
                    return true;
                } else {
                    //使用快照还原数据
                    copyArray(cellArraySnapshot, cellArray);
                    operations.clear();
                    operations.addAll(operationsSnapshot);
                }
            }
        }
        return false;
    }

    private static void copyArray(int[][] arr1, int[][] arr2) {
        for (int i = 0; i < 14; i++) {
            for (int j = 0; j < 10; j++) {
                arr2[i][j] = arr1[i][j];
            }
        }
    }

    private static boolean move(int[][] cellArray, List<Operation> operations, int i, int j) {
        boolean result = moveLeft(cellArray, operations, i, j);
        if (!result) {
            result = moveRight(cellArray, operations, i, j);
        }
        if (!result) {
            result = moveUp(cellArray, operations, i, j);
        }
        if (!result) {
            result = moveDown(cellArray, operations, i, j);
        }
        return result;
    }

    private static boolean moveLeft(int[][] cellArray, List<Operation> operations, int i, int j) {
        int left = j;
        for (int k = j - 1; k >= 0; k--) {
            if (cellArray[i][k] == -1) {
                break;
            } else {
                left = k;
            }
        }
        int leftBoundary = left;
        for (int k = left - 1; k >= 0; k--) {
            if (cellArray[i][k] != -1) {
                break;
            } else {
                leftBoundary = k;
            }
        }
        if (leftBoundary < left) {
            int maxMoveCellCount = left - leftBoundary;
            int moveCount = -1;
            int matchIndex = -1;
            for (int k = 1; k <= maxMoveCellCount; k++) {
                int matchValue = -1;
                for (int ium = i - 1; ium >= 0; ium--) {
                    if (cellArray[ium][j - k] != -1) {
                        matchIndex = ium;
                        matchValue = cellArray[ium][j - k];
                        break;
                    }
                }
                if (cellArray[i][j] == matchValue) {
                    moveCount = k;
                    break;
                }
                for (int idm = i + 1; idm < 14; idm++) {
                    if (cellArray[idm][j - k] != -1) {
                        matchIndex = idm;
                        matchValue = cellArray[idm][j - k];
                        break;
                    }
                }
                if (cellArray[i][j] == matchValue) {
                    moveCount = k;
                    break;
                }
            }
            if (moveCount != -1) {
                for (int k = left; k <= j; k++) {
                    cellArray[i][k - moveCount] = cellArray[i][k];
                    cellArray[i][k] = -1;
                }
                cellArray[i][j - moveCount] = -1;
                cellArray[matchIndex][j - moveCount] = -1;
                operations.add(new Move(i, j, Direction.LEFT, moveCount));
                return true;
            }
        }
        return false;
    }

    private static boolean moveRight(int[][] cellArray, List<Operation> operations, int i, int j) {
        int right = j;
        for (int k = j + 1; k < 10; k++) {
            if (cellArray[i][k] == -1) {
                break;
            } else {
                right = k;
            }
        }
        int rightBoundary = right;
        for (int k = right + 1; k < 10; k++) {
            if (cellArray[i][k] != -1) {
                break;
            } else {
                rightBoundary = k;
            }
        }
        if (rightBoundary > right) {
            int maxMoveCellCount = rightBoundary - right;
            int moveCount = -1;
            int matchIndex = -1;
            for (int k = 1; k <= maxMoveCellCount; k++) {
                int matchValue = -1;
                for (int ium = i - 1; ium >= 0; ium--) {
                    if (cellArray[ium][j + k] != -1) {
                        matchIndex = ium;
                        matchValue = cellArray[ium][j + k];
                        break;
                    }
                }
                if (cellArray[i][j] == matchValue) {
                    moveCount = k;
                    break;
                }
                for (int idm = i + 1; idm < 14; idm++) {
                    if (cellArray[idm][j + k] != -1) {
                        matchIndex = idm;
                        matchValue = cellArray[idm][j + k];
                        break;
                    }
                }
                if (cellArray[i][j] == matchValue) {
                    moveCount = k;
                    break;
                }
            }
            if (moveCount != -1) {
                for (int k = right; k >= j; k--) {
                    cellArray[i][k + moveCount] = cellArray[i][k];
                    cellArray[i][k] = -1;
                }
                cellArray[i][j + moveCount] = -1;
                cellArray[matchIndex][j + moveCount] = -1;
                operations.add(new Move(i, j, Direction.RIGHT, moveCount));
                return true;
            }
        }
        return false;
    }

    private static boolean moveUp(int[][] cellArray, List<Operation> operations, int i, int j) {
        int up = i;
        for (int k = i - 1; k >= 0; k--) {
            if (cellArray[k][j] == -1) {
                break;
            } else {
                up = k;
            }
        }
        int upBoundary = up;
        for (int k = up - 1; k >= 0; k--) {
            if (cellArray[k][j] != -1) {
                break;
            } else {
                upBoundary = k;
            }
        }
        if (upBoundary < up) {
            int maxMoveCellCount = up - upBoundary;
            int moveCount = -1;
            int matchIndex = -1;
            for (int k = 1; k <= maxMoveCellCount; k++) {
                int matchValue = -1;
                for (int jlm = j - 1; jlm >= 0; jlm--) {
                    if (cellArray[i - k][jlm] != -1) {
                        matchIndex = jlm;
                        matchValue = cellArray[i - k][jlm];
                        break;
                    }
                }
                if (cellArray[i][j] == matchValue) {
                    moveCount = k;
                    break;
                }
                for (int jrm = j + 1; jrm < 10; jrm++) {
                    if (cellArray[i - k][jrm] != -1) {
                        matchIndex = jrm;
                        matchValue = cellArray[i - k][jrm];
                        break;
                    }
                }
                if (cellArray[i][j] == matchValue) {
                    moveCount = k;
                    break;
                }
            }
            if (moveCount != -1) {
                for (int k = up; k <= i; k++) {
                    cellArray[k - moveCount][j] = cellArray[k][j];
                    cellArray[k][j] = -1;
                }
                cellArray[i - moveCount][j] = -1;
                cellArray[i - moveCount][matchIndex] = -1;
                operations.add(new Move(i, j, Direction.UP, moveCount));
                return true;
            }
        }
        return false;
    }

    private static boolean moveDown(int[][] cellArray, List<Operation> operations, int i, int j) {
        int down = i;
        for (int k = i + 1; k < 14; k++) {
            if (cellArray[k][j] == -1) {
                break;
            } else {
                down = k;
            }
        }
        int downBoundary = down;
        for (int k = down + 1; k < 14; k++) {
            if (cellArray[k][j] != -1) {
                break;
            } else {
                downBoundary = k;
            }
        }
        if (downBoundary > down) {
            int maxMoveCellCount = downBoundary - down;
            int moveCount = -1;
            int matchIndex = -1;
            for (int k = 1; k <= maxMoveCellCount; k++) {
                int matchValue = -1;
                for (int jlm = j - 1; jlm >= 0; jlm--) {
                    if (cellArray[i + k][jlm] != -1) {
                        matchIndex = jlm;
                        matchValue = cellArray[i + k][jlm];
                        break;
                    }
                }
                if (cellArray[i][j] == matchValue) {
                    moveCount = k;
                    break;
                }
                for (int jrm = j + 1; jrm < 10; jrm++) {
                    if (cellArray[i + k][jrm] != -1) {
                        matchIndex = jrm;
                        matchValue = cellArray[i + k][jrm];
                        break;
                    }
                }
                if (cellArray[i][j] == matchValue) {
                    moveCount = k;
                    break;
                }
            }
            if (moveCount != -1) {
                for (int k = down; k >= i; k--) {
                    cellArray[k + moveCount][j] = cellArray[k][j];
                    cellArray[k][j] = -1;
                }
                cellArray[i + moveCount][j] = -1;
                cellArray[i + moveCount][matchIndex] = -1;
                operations.add(new Move(i, j, Direction.DOWN, moveCount));
                return true;
            }
        }
        return false;
    }

    interface Operation {


    }

    static class Click implements Operation {

        private final int x;
        private final int y;

        Click(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "点击第" + (x + 1) + "行第" + (y + 1) + "列";
        }
    }

    static class Move implements Operation {

        private final int x;
        private final int y;
        private final Direction direction;
        private final int count;


        Move(int x, int y, Direction direction, int count) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.count = count;
        }

        @Override
        public String toString() {
            return "拖动第\u001B[31m" + (x + 1) + "\u001B[0m行第\u001B[31m" + (y + 1) + "\u001B[0m列的格子向\u001B[31m" + direction.value + "\u001B[0m方向移动\u001B[31m" + count + "\u001B[0m个格子" ;
        }
    }

    static enum Direction {
        LEFT("左"), RIGHT("右"), UP("上"), DOWN("下");

        private final String value;

        Direction(String value) {
            this.value = value;
        }

    }
}
