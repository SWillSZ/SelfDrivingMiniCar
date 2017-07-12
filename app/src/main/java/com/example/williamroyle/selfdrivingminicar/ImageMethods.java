package com.example.williamroyle.selfdrivingminicar;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * Created by WilliamRoyle on 6/24/17.
 */

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Point;
import java.util.Random;
import org.opencv.core.Scalar;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import android.os.Looper;
import android.os.*;
import java.util.concurrent.locks.*;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import org.opencv.android.Utils;
import android.os.Handler;
import java.lang.Math;
import java.lang.System;
import org.opencv.calib3d.StereoBM;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.*;
import java.util.*;

public class ImageMethods
{
    // returns angle of a vector
    public static float getAngle(double x, double y)
    {
        float angle = (float) Math.toDegrees(Math.atan2(y,x));
        if(angle < 0)
        {
            angle += 360;
        }
        return angle;
    }


    /*
    Finds the average RGB values by iterating over all pixels in range. Returns the result in
    [r,g,b] format, with r in [0,255]. Note that the range for bounds is exclusive, so a topBound of
    0 and a bottomBound of 240 will average over the range [0,239] for vertical position.
    Assumes Mat is in RGB format, with storage format CvType.CV_8UC3. Returns Mat with the same format, but just one pixel
     */
    public static Mat avgRGB(Mat image, int leftBound, int rightBound, int topBound, int bottomBound)
    {
        image.convertTo(image, CvType.CV_64FC3);
        Mat avgRGB = new Mat(1,1,CvType.CV_8UC3);
        int avgR = 0;
        int avgG = 0;
        int avgB = 0;
        int numPixels = (rightBound-leftBound)*(bottomBound-topBound);
        double[] doubleArray = new double[image.height()*image.width()*3];
        image.get(0,0,doubleArray);
        for (int row = topBound; row < bottomBound;row++)
        {
            for (int column = leftBound; column <rightBound;column++)
            {
                int count = (row * rightBound + column) * 3;
                avgR += (double)doubleArray[count];
                avgG += (double)doubleArray[count+1];
                avgB += (double)doubleArray[count+2];
            }
        }
        avgR = avgR/numPixels;
        avgG = avgG/numPixels;
        avgB = avgB/numPixels;
        avgRGB.put(0,0,new double[]{avgR,avgG,avgB});
        image.convertTo(image,CvType.CV_8UC3);
        return avgRGB;
    }


    /*
Finds the average RGB values by randomly sampling pixels. Returns the result in
[r,g,b] format, with r in [0,255]. Note that the range for bounds is exclusive, so a topBound of
0 and a bottomBound of 240 will average over the range [0,239] for vertical position.
Assumes Mat is in RGB format, with storage format CvType.CV_8UC3. Returns Mat with the same format, but just one pixel
 */
    public static Mat avgRGBSample(Mat image, int leftBound, int rightBound, int topBound, int bottomBound, int samples)
    {
        image.convertTo(image,CvType.CV_64FC3);
        Mat avgRGB = new Mat(1,1,CvType.CV_8UC3);
        int avgR = 0;
        int avgG = 0;
        int avgB = 0;
        int numPixels = (rightBound-leftBound)*(bottomBound-topBound);
        double[] doubleArray = new double[image.height()*image.width()*3];
        image.get(0,0,doubleArray);
        Random rand = new Random();
        for (int count = 0; count<samples; count++)
        {
            int x = leftBound + rand.nextInt(rightBound-leftBound);
            int y = topBound + rand.nextInt(bottomBound-topBound);
            int pos = (y * image.width() + x) * 3;
            avgR += (int)doubleArray[pos];
            avgG += (int)doubleArray[pos+1];
            avgB += (int)doubleArray[pos+2];
        }
        avgR = avgR/samples;
        avgG = avgG/samples;
        avgB = avgB/samples;
        avgRGB.put(0,0,new double[]{avgR,avgG,avgB});
        image.convertTo(image,CvType.CV_8UC3);
        return avgRGB;
    }

    public static int[] pixMatToIntArray(Mat toConvert)
    {
        int toReturn[] = new int[3];
        toReturn[0] = (int)toConvert.get(0,0)[0];
        toReturn[1] = (int)toConvert.get(0,0)[1];
        toReturn[2] = (int)toConvert.get(0,0)[2];
        return toReturn;
    }

    public static void sleep(int time)
    {
        try
        {
            Thread.sleep(time);
        }
        catch (Exception E)
        {

        }

    }


}

