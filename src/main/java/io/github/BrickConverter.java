package io.github;

import nu.pattern.OpenCV;
import org.springframework.util.FileCopyUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BrickConverter {

    private final static String PATH = "C:/Users/pxzxj1/Desktop/brick/";

    static {
        OpenCV.loadShared();
    }

    public static void main(String[] args) throws IOException {
        splitImage();
        int[][] imageArray = convertImagesToArray();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            stringBuilder.append(Arrays.toString(imageArray[i])).append("\r\n");
        }
        FileCopyUtils.copy(stringBuilder.toString(), new FileWriter(PATH + "brick.txt"));
    }

    static int[][] convertImagesToArray() throws IOException {
        int[][] imageArray = new int[14][10];
        List<String> typeSamples = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            for (int j = 0; j < 10; j++) {
                String filename = PATH + "brick_" + i + "_" + j + ".jpg";
                int matchType = -1;
                for (int k = 0; k < typeSamples.size(); k++) {
                    if (compareImage(filename, typeSamples.get(k))) {
                        matchType = k;
                        break;
                    }
                }
                if (matchType == -1) {
                    matchType = typeSamples.size();
                    typeSamples.add(filename);
                }
                imageArray[i][j] = matchType;
            }
        }
        System.out.println("typeSamples = " + typeSamples.size());
        System.out.println("typeSamples = " + typeSamples);
        return imageArray;
    }

    static void splitImage() throws IOException {
        // 读取原始图像
        BufferedImage originalImage = ImageIO.read(new File(PATH + "brick.png"));

        double cellWidth = originalImage.getWidth() / 10.0;
        double cellHeight = originalImage.getHeight() / 14.0;

        // 遍历原始图像，分割并保存每个图像
        for (int i = 0; i < 14; i++) {
            for (int j = 0; j < 10; j++) {
                // 创建分割图像的子图像
                BufferedImage tileImage = originalImage.getSubimage(
                        (int) (j * cellWidth),
                        (int) (i * cellHeight),
                        (int) cellWidth,
                        (int) cellHeight
                );
                BufferedImage scaledImage = new BufferedImage(400, 400, originalImage.getType());
                Image scaledInstance = tileImage.getScaledInstance(400, 400, Image.SCALE_SMOOTH);
                scaledImage.getGraphics().drawImage(scaledInstance, 0, 0, null);
                // 保存分割后的图像
                String fileName = PATH + "brick_" + i + "_" + j + ".jpg";
                ImageIO.write(scaledImage, "jpg", new File(fileName));
            }
        }
    }

    /**
     * OpenCV-4.0.0 直方图比较
     *
     */
    static boolean compareImage(String file1, String file2) throws IOException {
        double similarity = OpenCVImageSimilarity.compare(file1, file2, false);
        return similarity > 0.92;
    }
}
