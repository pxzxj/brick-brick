package io.github;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * https://www.cnblogs.com/hymws/p/17559563.html
 */
public class OpenCVImageSimilarity {

    static {
        OpenCV.loadShared();
    }

    public static double compare(String imageUrl1, String imageUrl2, boolean print) throws IOException {
        Mat image1 = readImageFromUrl(imageUrl1); // 调用方法读取网络图片
        Mat image2 = readImageFromUrl(imageUrl2);
        double rdb = rdbSimilar(image1, image2);
        double hash = meanHash(image1, image2);
        double hist = histogram(image1, image2);
        double result = 0;
        int count = 0;
        if (rdb > 0) {
            result += rdb;
            count++;
        }
        if (hash > 0) {
            result += hash;
            count++;
        }
        if (hist > 0) {
            result += hist;
            count++;
        }
        result = result / count;
        if (print) {
            System.out.println("RDB相似度：" + rdb);
            System.out.println("均值哈希算法计算相似度：" + hash);
            System.out.println("图片相似度(直方图): " + hist);
            System.out.println("平均值为：" + result);
        }
        return result;
    }

    private static Mat readImageFromUrl(String imageUrl) {
        Mat image = null;
        try {
            byte[] imageData = FileCopyUtils.copyToByteArray(new File(imageUrl));
            image = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_COLOR);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    static double histogram(Mat image1, Mat image2) {
        Mat hvs_1 = new Mat();
        Mat hvs_2 = new Mat();
        //图片转HSV
        Imgproc.cvtColor(image1, hvs_1,Imgproc.COLOR_BGR2HSV);
        Imgproc.cvtColor(image2, hvs_2,Imgproc.COLOR_BGR2HSV);

        Mat hist_1 = new Mat();
        Mat hist_2 = new Mat();

        //直方图计算
        Imgproc.calcHist(Stream.of(hvs_1).collect(Collectors.toList()),new MatOfInt(0),new Mat(),hist_1,new MatOfInt(255) ,new MatOfFloat(0,256));
        Imgproc.calcHist(Stream.of(hvs_2).collect(Collectors.toList()),new MatOfInt(0),new Mat(),hist_2,new MatOfInt(255) ,new MatOfFloat(0,256));

        //图片归一化
        Core.normalize(hist_1, hist_1, 1, hist_1.rows() , Core.NORM_MINMAX, -1, new Mat() );
        Core.normalize(hist_2, hist_2, 1, hist_2.rows() , Core.NORM_MINMAX, -1, new Mat() );

        //直方图比较
        double similarity = Imgproc.compareHist(hist_1,hist_2, Imgproc.CV_COMP_CORREL);
        return similarity;

    }

    // 计算均方差（MSE）
    private static double calculateHistogram(Mat image1, Mat image2) {
        // 计算直方图
        Mat hist1 = calculateHistogram(image1);
        Mat hist2 = calculateHistogram(image2);

        // 计算相似度
        final double similarity = Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_CORREL);
        return similarity;
    }


    // 计算均方差（MSE）
    private static double calculateMSE(Mat image1, Mat image2) {
        Mat diff = new Mat();
        Core.absdiff(image1, image2, diff);
        Mat squaredDiff = new Mat();
        Core.multiply(diff, diff, squaredDiff);
        Scalar mseScalar = Core.mean(squaredDiff);
        return mseScalar.val[0];
    }

    // 计算结构相似性指数（SSIM）
    private static double calculateSSIM(Mat image1, Mat image2) {
        Mat image1Gray = new Mat();
        Mat image2Gray = new Mat();
        Imgproc.cvtColor(image1, image1Gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(image2, image2Gray, Imgproc.COLOR_BGR2GRAY);
        MatOfFloat ssimMat = new MatOfFloat();
        Imgproc.matchTemplate(image1Gray, image2Gray, ssimMat, Imgproc.CV_COMP_CORREL);
        Scalar ssimScalar = Core.mean(ssimMat);
        return ssimScalar.val[0];
    }

    // 计算峰值信噪比（PSNR）
    private static double calculatePSNR(Mat image1, Mat image2) {
        Mat diff = new Mat();
        Core.absdiff(image1, image2, diff);
        Mat squaredDiff = new Mat();
        Core.multiply(diff, diff, squaredDiff);
        Scalar mseScalar = Core.mean(squaredDiff);
        double mse = mseScalar.val[0];
        double psnr = 10.0 * Math.log10(255.0 * 255.0 / mse);
        return psnr;
    }

    private static Mat calculateHistogram(Mat image) {
        Mat hist = new Mat();

// 设置直方图参数
        MatOfInt histSize = new MatOfInt(256);
        MatOfFloat ranges = new MatOfFloat(0, 256);
        MatOfInt channels = new MatOfInt(0);
        List<Mat> images = new ArrayList<>();
        images.add(image);

// 计算直方图
        Imgproc.calcHist(images, channels, new Mat(), hist, histSize, ranges);

        return hist;
    }

    static double rdbSimilar(Mat img1, Mat img2) {

        // 创建ORB特征检测器和描述子提取器
        ORB orb = ORB.create();

        // 检测图像中的特征点并计算描述子
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat();
        Mat descriptors2 = new Mat();
        orb.detectAndCompute(img1, new Mat(), keypoints1, descriptors1);
        orb.detectAndCompute(img2, new Mat(), keypoints2, descriptors2);

        // 创建描述子匹配器
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        // 匹配描述子
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(descriptors1, descriptors2, matches);
        // 计算匹配结果的相似度
        double maxDist = 0;
        double minDist = 100;
        DMatch[] matchArray = matches.toArray();
        for (int i = 0; i < matchArray.length; i++) {
            double dist = matchArray[i].distance;
            if (dist < minDist) {
                minDist = dist;
            }
            if (dist > maxDist) {
                maxDist = dist;
            }
        }

        // 输出相似度
        double v = 1 - minDist / maxDist;
        return v;
    }

    static double meanHash(Mat img1, Mat img2) {
        Mat grayImg1 = new Mat();
        Mat grayImg2 = new Mat();
        Imgproc.cvtColor(img1, grayImg1, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(img2, grayImg2, Imgproc.COLOR_BGR2GRAY);
        // 缩放图像至固定大小
        Size targetSize = new Size(8, 8);
        Mat resizedImg1 = new Mat();
        Mat resizedImg2 = new Mat();
        Imgproc.resize(grayImg1, resizedImg1, targetSize);
        Imgproc.resize(grayImg2, resizedImg2, targetSize);

        // 计算均值哈希值
        String hash1 = calculateMeanHash(resizedImg1);
        String hash2 = calculateMeanHash(resizedImg2);

        // 计算汉明距离并计算相似度
        int hammingDistance = calculateHammingDistance(hash1, hash2);

        return 1 - (double) hammingDistance / (targetSize.width * targetSize.height);
    }

    private static String calculateMeanHash(Mat image) {
        double sum = 0;
        int totalPixels = image.rows() * image.cols();

        for (int i = 0; i < image.rows(); i++) {
            for (int j = 0; j < image.cols(); j++) {
                double pixelValue = image.get(i, j)[0];
                sum += pixelValue;
            }
        }

        double mean = sum / totalPixels;
        StringBuilder hash = new StringBuilder();

        for (int i = 0; i < image.rows(); i++) {
            for (int j = 0; j < image.cols(); j++) {
                double pixelValue = image.get(i, j)[0];
                if (pixelValue >= mean) {
                    hash.append("1");
                } else {
                    hash.append("0");
                }
            }
        }

        return hash.toString();
    }

    private static int calculateHammingDistance(String hash1, String hash2) {
        int distance = 0;

        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) {
                distance++;
            }
        }
        return distance;
    }

}