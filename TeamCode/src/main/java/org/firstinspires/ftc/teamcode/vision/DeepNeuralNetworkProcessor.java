package org.firstinspires.ftc.teamcode.vision;

import org.firstinspires.ftc.teamcode.vision.vizTurret.Constants;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;



public class DeepNeuralNetworkProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(DeepNeuralNetworkProcessor.class);
    private Net net;
    private final String model = "FtcRobotController/assets/rubix-ball.onnx";

    private final String[] classNames = {"rubix","ball"};


    public DeepNeuralNetworkProcessor() {
        this.net = Dnn.readNetFromONNX(model);
    }

    public int getObjectCount(Mat frame, String objectName, double threshold) {

        int inWidth = Constants.CAMERA_WIDTH;
        int inHeight = Constants.CAMERA_HEIGHT;
        //0.007843
        double inScaleFactor = 0.07843;
        double thresholdDnn =  threshold;
        double meanVal = 127.5;

        int personObjectCount = 0;
        Mat blob = null;
        Mat detections = null;


        try {
            blob = Dnn.blobFromImage(frame, inScaleFactor,
                    new Size(inWidth, inHeight),
                    new Scalar(meanVal, meanVal, meanVal),
                    false, false);
            net.setInput(blob);
            detections = net.forward();
            detections = detections.reshape(1, (int) detections.total() / 7);
            for (int i = 0; i < detections.rows(); ++i) {
                double confidence = detections.get(i, 2)[0];

                if (confidence < thresholdDnn)
                    continue;

                int classId = (int) detections.get(i, 1)[0];
                if (classNames[classId].toString() != objectName.toLowerCase()) {
                    continue;
                }
                personObjectCount++;
            }
        } catch (Exception ex) {
            LOGGER.error("An error occurred DNN: ", ex);
        }
        return personObjectCount;
    }

    public List<DnnObject> getObjectsInFrame(Mat frame, double threshold) {

        int inWidth = 320;
        int inHeight = 240;
        double inScaleFactor = 0.007843;
        double thresholdDnn =  0.2;
        double meanVal = 127.5;

        Mat blob = null;
        Mat detections = null;
        List<DnnObject> objectList = new ArrayList<>();

        int cols = frame.cols();
        int rows = frame.rows();

        try {

            blob = Dnn.blobFromImage(frame, inScaleFactor,
                    new Size(inWidth, inHeight),
                    new Scalar(meanVal, meanVal, meanVal),
                    false, false);

            net.setInput(blob);
            detections = net.forward();
            detections = detections.reshape(1, (int) detections.total() / 7);

            //all detected objects
            for (int i = 0; i < detections.rows(); ++i) {
                double confidence = detections.get(i, 2)[0];

                if (confidence < thresholdDnn)
                    continue;

                int classId = (int) detections.get(i, 1)[0];

                //calculate position
                int xLeftBottom = (int) (detections.get(i, 3)[0] * cols);
                int yLeftBottom = (int) (detections.get(i, 4)[0] * rows);
                Point leftPosition = new Point(xLeftBottom, yLeftBottom);

                int xRightTop = (int) (detections.get(i, 5)[0] * cols);
                int yRightTop = (int) (detections.get(i, 6)[0] * rows);
                Point rightPosition = new Point(xRightTop, yRightTop);

                float centerX = (float) (xLeftBottom + xRightTop) / 2;
                float centerY = (float) (yLeftBottom - yRightTop) / 2;
                Point centerPoint = new Point(centerX, centerY);


                DnnObject dnnObject = new DnnObject(classId, classNames[classId].toString(), leftPosition, rightPosition, centerPoint);
                objectList.add(dnnObject);
            }

        } catch (Exception ex) {
            LOGGER.error("An error occurred DNN: ", ex);
        }
        return objectList;
    }


}