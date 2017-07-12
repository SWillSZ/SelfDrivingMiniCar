package com.example.williamroyle.selfdrivingminicar;

/**
 * Created by WilliamRoyle on 5/29/17.
 */
        import org.opencv.core.Core;
        import org.opencv.core.Point;
        import java.util.Random;
        import org.opencv.core.Scalar;
        import org.opencv.core.CvType;
        import org.opencv.core.Mat;
        import org.opencv.core.MatOfPoint;
        import org.opencv.core.*;
        import org.opencv.core.Size;
        import org.opencv.imgproc.Imgproc;

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
        import org.opencv.features2d.*;

public class UIHandler
{

    public boolean firstClick = true;
    public Mat avgRGB = null;
    public int[] avgRGBarray = null;
    public Mat avgHSV = null;
    public int[] avgHSVarray = null;




    public static long lastRight;
    public static long lastLeft;
    public static boolean movingForwards = false;
    public final long WAIT_FOR_REPEAT_TURN_MS = 3500;
    public final long TURN_DURATION_MS= 2000;

    Activity mainActivity;

    public UIHandler(Activity toSave)
    {
        mainActivity = toSave;
        lastRight = 0;
        lastLeft = 0;
    }


    public static Bitmap editImageControllersColors(Mat tmp, UIHandler controller)
    {
        Bitmap bitmap = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
        if (controller.firstClick)
        {
            // Find and save ground color
            controller.firstClick = false;
            Imgproc.cvtColor(tmp,tmp, Imgproc.COLOR_RGBA2RGB);
            controller.avgRGB = ImageMethods.avgRGB(tmp,tmp.width()/3,2*tmp.width()/3,2*tmp.height()/3,tmp.height());
            controller.avgRGBarray = ImageMethods.pixMatToIntArray(controller.avgRGB);
            controller.avgHSV = new Mat(1,1,CvType.CV_8UC3);
            Imgproc.cvtColor(controller.avgRGB,controller.avgHSV,Imgproc.COLOR_RGB2HSV);
            controller.avgHSVarray = ImageMethods.pixMatToIntArray(controller.avgHSV);


            /* Display ground color
            controller.setMessage(controller.avgRGB.dump());
            final int[] rgb = ImageMethods.pixMatToIntArray(controller.avgRGB);
            final ImageView toChange = (ImageView)controller.mainActivity.findViewById(R.id.control4);
            toChange.post(new Runnable() {
                public void run()
                {
                    toChange.setBackgroundColor(Color.argb(255,rgb[0],rgb[1],rgb[2]));
                }
            });*/
            Utils.matToBitmap(tmp, bitmap);
        }
        else
        {
            // Resize & Apply blurring filters
            Size sz = new Size(60,40);
            Mat startImage = new Mat((int)sz.height,(int)sz.width, tmp.type());
            Imgproc.resize( tmp, startImage, sz );
            Imgproc.cvtColor(startImage,startImage, Imgproc.COLOR_RGBA2RGB);
            Imgproc.blur(startImage, startImage, new Size(5, 5));
            Mat mediumImage = startImage.clone();
            Imgproc.bilateralFilter(startImage,mediumImage,7,80,200);


            //find + render local average color, debugging tool
            Mat localAvgRGB = ImageMethods.avgRGBSample(startImage,0,startImage.width(),0,startImage.height(),200);
            final int[] localAvgRGBarray = ImageMethods.pixMatToIntArray(localAvgRGB);
            final ImageView toChange = (ImageView)controller.mainActivity.findViewById(R.id.control5);
            toChange.post(new Runnable() {
                public void run()
                {
                    toChange.setBackgroundColor(Color.argb(255,localAvgRGBarray[0],localAvgRGBarray[1],localAvgRGBarray[2]));
                }
            });


            // We compute and save the distance map of the pixels from controller.avgHSV
            Imgproc.cvtColor(mediumImage,mediumImage, Imgproc.COLOR_RGB2HSV);
            mediumImage.convertTo(mediumImage,CvType.CV_64FC3);
            int size = (int) (mediumImage.total() * mediumImage.channels());
            double[] picArr = new double[size];
            mediumImage.get(0,0,picArr);
            int width = mediumImage.width();
            int height = mediumImage.height();
            double[] avgHSV = controller.avgHSV.get(0,0);
            for (int row = 0; row < height;row++)
            {
                for (int column = 0; column <width;column++)
                {
                    int count = (row * width + column) * 3;
                    //picArr[count] = (picArr[count] + 90) % 180;
                    double vDiff = Math.abs(picArr[count+2]-avgHSV[2])/255;
                    double vMin = Math.min(picArr[count+2],avgHSV[2])/255;
                    double sDiff = Math.abs(picArr[count+1]-avgHSV[1])/255;
                    double sMin = Math.min(picArr[count+1],avgHSV[1])/255;
                    double diffH1 = Math.abs(picArr[count]-(avgHSV[0]-180));
                    double diffH2 = Math.abs(picArr[count]-avgHSV[0]);
                    double diffH3 = Math.abs(picArr[count]-(avgHSV[0]+180));
                    double hDiff = Math.min(Math.min(diffH1,diffH2), diffH3)/179;
                    picArr[count] = 0;
                    picArr[count+1] = 0;
                    picArr[count+2] = 255 - (int)((vDiff/2+vMin*sDiff*1.5+vMin*Math.sqrt(sMin)*hDiff*4)*42);
                }

            }



            // We find average distance value
            int average = 0;
            Random rand = new Random();
            int samples = 400;
            for (int count = 0; count<samples; count ++)
            {
                int n = rand.nextInt(width*height);
                average+=picArr[3*n+2];
            }
            average = average/samples;
            controller.setMessage2(String.valueOf(average));



            // We label pixels black or white based on distance
            Log.d("Average = ",String.valueOf(average));
            for (int row = 0; row < height;row++)
            {
                for (int column = 0; column < width; column++)
                {
                    int count = (row * width + column) * 3;
                    if (picArr[count+2] > (average-2) && picArr[count+2] > 244)
                    {
                        picArr[count] = 0;
                        picArr[count+1] = 0;
                        picArr[count+2] = 255;
                    }
                    else if (picArr[count+2] > (average-2) && picArr[count+2] > 235)
                    {
                        //picArr[count+2] = 100;
                        picArr[count+2] = 0;
                    }
                    else
                    {
                        picArr[count+2] = 0;
                    }
                }
            }




            // We save our results, re-using startImage
            startImage.put(0,0,picArr);
            Imgproc.cvtColor(startImage,startImage,Imgproc.COLOR_HSV2RGB);
            Imgproc.cvtColor(startImage,startImage,Imgproc.COLOR_RGB2GRAY);


            // We use contors to remove noise
            ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            ArrayList<MatOfPoint> smallContours = new ArrayList<MatOfPoint>();
            Imgproc.findContours(startImage.clone(), contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            int i;
            for (MatOfPoint shape : contours)
            {
                Log.d("SuperPointW",String.valueOf(tmp.width()));
                Log.d("SuperPointH",String.valueOf(tmp.height()));
                if (Imgproc.contourArea(shape)<200)
                {
                    smallContours.add(shape);
                }

            }
            Imgproc.cvtColor(startImage,startImage,Imgproc.COLOR_GRAY2RGB);
            Imgproc.fillPoly(startImage, smallContours, new Scalar(255,255,255));


            // We decide whether to move the car forwards
            Mat avgColor = ImageMethods.avgRGBSample(startImage,width/3,2*width/3,6*height/8,height,300);
            Log.d("averageColor",avgColor.dump());
            if (ImageMethods.pixMatToIntArray(avgColor)[0]>235)
            {
                Mat avgUpperColorLeft = ImageMethods.avgRGBSample(startImage,0,width/2,3*height/8,7*height/8,200);
                int leftAvg = ImageMethods.pixMatToIntArray(avgUpperColorLeft)[0];
                Mat avgUpperColorRight = ImageMethods.avgRGBSample(startImage,width/2,width,3*height/8,7*height/8,200);
                int rightAvg = ImageMethods.pixMatToIntArray(avgUpperColorRight)[0];
                int toSet = 0;
                if (leftAvg>rightAvg)
                {
                    toSet = 2;
                }
                else
                {
                    toSet = 3;
                }

                controller.setControllerWhite(toSet);
                controller.goForwards();
                controller.setControllerBlack(toSet);

            }
            else
            {
            }





            // Resize and return
            Mat endImage = new Mat((int)sz.height,(int)sz.width, tmp.type());
            sz = new Size(240,160);
            Imgproc.resize(startImage,endImage, sz );
            Utils.matToBitmap(endImage, bitmap);

        }


        return bitmap;
    }

    public static Bitmap editImageControllersTest(Mat tmp, UIHandler controller)
    {
        if (controller.firstClick)
        {

            ImageMethods.sleep(5000);
            // Angle wheels right
            for (int count=0; count<40;count++)
            {
                controller.setControllerWhite(3);
                ImageMethods.sleep(300);
                controller.setControllerBlack(3);
                ImageMethods.sleep(100);
                // Move forwards
                controller.backMotorOn();
                ImageMethods.sleep(400);
                controller.backMotorOff();
                ImageMethods.sleep(300);
            }
            ImageMethods.sleep(10000);

        }
        Bitmap b = null;
        Utils.matToBitmap(tmp, b);
        return b;
    }

    public static Bitmap editImageControllersEmpty(Mat tmp, UIHandler controller)
    {
        Bitmap b = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(tmp, b);
        return b;
    }

    public static Bitmap editImageControllersEmpty(Mat tmp)
    {
        Bitmap b = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(tmp, b);
        return b;

    }


    public static Bitmap editImageControllers(Mat tmp, UIHandler controller)
    {
        if (tmp == null)
        {
            Log.d("NullHandler","tmp was Empty");
        }
        Bitmap bmp = null;
        try
        {
            Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_BGRA2RGBA, 4);
            Mat tmp2 = new Mat (tmp.rows(), tmp.cols(), CvType.CV_8UC1);
            Imgproc.cvtColor(tmp, tmp2, Imgproc.COLOR_BGRA2GRAY, 4);
            Mat tmp3 = new Mat (tmp.rows(), tmp.cols(), CvType.CV_8UC1);
            Imgproc.GaussianBlur(tmp2, tmp3, new Size(9, 9), 2, 2 );
            Mat circles = new Mat();
            Imgproc.HoughCircles(tmp3, circles, Imgproc.CV_HOUGH_GRADIENT, 1, tmp2.rows()/8, 100, 22, 0, 200 );

            Mat tmp4 = new Mat (tmp.rows(), tmp.cols(), CvType.CV_8UC4);
            Imgproc.cvtColor(tmp3, tmp4, Imgproc.COLOR_GRAY2RGBA, 4);
            if (circles!=null && (circles.rows()*circles.cols())>0) // if circles is non-empty
            {
                controller.backMotorOn();
                double radius = 0;
                double x = 0 ;
                double y = 0;
                Log.d("HandlerException",String.valueOf(circles.rows())+" "+String.valueOf(circles.cols()));
                double vCircle[] = circles.get(0, 0);
                x = vCircle[0];
                y = vCircle[1];
                radius = vCircle[2];
                Imgproc.circle(tmp4, new Point(x,y),((int)radius),new Scalar(0,255,0,100),4);
                // We now turn the car
                if (x > (tmp.width()/2))
                {
                    //controller.turnRight();
                    Log.d("","");
                }
                else
                {
                    //controller.turnLeft();
                }
            }
            else
            {
                controller.backMotorOff();
                controller.setControllerBlack(2);
                controller.setControllerBlack(3);
            }
            bmp = Bitmap.createBitmap(tmp4.cols(), tmp4.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(tmp4, bmp);
            Log.d("Handler","Converted to Bitmap");

        }
        catch (Exception e)
        {
            Log.d("HandlerException",e.toString());
        }
        return bmp;
    }


    public static Bitmap editImageControllersContors(Mat tmp, UIHandler controller)
    {
        Mat tst = tmp.clone();
        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_BGRA2GRAY);
        //Imgproc.cvtColor(tmp2,tmp2, Imgproc.COLOR_RGB2HSV);
        Mat mask = new Mat (tmp.rows(), tmp.cols(), tmp.type());
        Core.inRange(tmp ,new Scalar(0),new Scalar(150),mask);
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        ArrayList<MatOfPoint> badContours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        for (MatOfPoint shape : contours)
        {
            Log.d("SuperPointW",String.valueOf(tmp.width()));
            Log.d("SuperPointH",String.valueOf(tmp.height()));
            if (Imgproc.contourArea(shape)<20)
            {
                badContours.add(shape);
            }


        }
        contours.removeAll(badContours);
        Log.d("SuperPoint","DONE");
        Imgproc.drawContours(tst, contours, -1, new Scalar(255,255,0));
        Bitmap b = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(tst, b);
        return b;
    }

    public void turnRight()
    {
        if (System.currentTimeMillis() > (WAIT_FOR_REPEAT_TURN_MS + lastRight))
        {
            setControllerBlack(3);
            setControllerWhite(2);
            lastRight = System.currentTimeMillis();
        }
        else if (System.currentTimeMillis() > (TURN_DURATION_MS + lastRight))
        {
            setControllerBlack(2);
        }
    }

    // Runs for approx one second, makes car go forwards
    public void goForwards()
    {

        backMotorOn();
        ImageMethods.sleep(400);
        backMotorOff();
        ImageMethods.sleep(300);
        setControllerWhite(4);
        ImageMethods.sleep(100);
        setControllerBlack(4);
        ImageMethods.sleep(10);
    }

    public void frontRight()
    {
        setControllerWhite(3);
        ImageMethods.sleep(300);
        setControllerBlack(3);
        ImageMethods.sleep(100);
    }

    public void frontLeft()
    {
        setControllerWhite(2);
        ImageMethods.sleep(300);
        setControllerBlack(2);
        ImageMethods.sleep(100);
    }


    public void backMotorOn()
    {

        setControllerWhite(1);

    }

    public void backMotorOff()
    {
        setControllerBlack(1);
    }


    public void setControllerWhite(int number)
    {
        final ImageView toChange = (ImageView)mainActivity.findViewById(getControlInt(number));
        toChange.post(new Runnable() {
            public void run() {
                toChange.setBackgroundColor(Color.WHITE);
            }
        });

    }



    public void setControllerBlack(int number)
    {
        final ImageView toChange = (ImageView)mainActivity.findViewById(getControlInt(number));
        toChange.post(new Runnable()
        {
            public void run()
            {
                toChange.setBackgroundColor(Color.BLACK);
            }
        });

    }

    public void setMessage(String message)
    {
        final String mes = message;
        final TextView toChange = (TextView)mainActivity.findViewById(R.id.textView);
        toChange.post(new Runnable() {
            public void run() {
                toChange.setText(mes);
            }
        });

    }


    public void setMessage2(String message2)
    {
        final String mes = message2;
        final TextView toChange = (TextView)mainActivity.findViewById(R.id.textView2);
        toChange.post(new Runnable() {
            public void run() {
                toChange.setText(mes);
            }
        });

    }


    public int getControlInt(int number)
    {
        int toReturn = 0;
        switch (number)
        {
            case 1:  toReturn = R.id.control1;
                break;
            case 2:  toReturn = R.id.control2;
                break;
            case 3:  toReturn = R.id.control3;
                break;
            case 4:  toReturn = R.id.control4;
                break;
            case 5:  toReturn = R.id.control5;
                break;
            default: toReturn = 0;
                break;
        }
        return toReturn;
    }

    //DynamicAdaptedFeatureDetector?
    // partially taken from https://stackoverflow.com/questions/24569386/opencv-filtering-orb-matches
    public static Bitmap findMatches(Mat newMat, Mat oldMat,  UIHandler controller)
    {
        int margin = 20;
        Size sz = new Size(newMat.width()+2*margin,newMat.height()+2*margin);
        //Size sz = new Size(newMat.width()*2,newMat.height()*2);
        Mat oldMatBig = new Mat((int)sz.height,(int)sz.width, oldMat.type());
        oldMatBig.setTo(new Scalar(0,0,0));
        Mat newMatBig = new Mat((int)sz.height,(int)sz.width, newMat.type());
        newMatBig.setTo(new Scalar(0,0,0));


        Mat submat = oldMatBig.submat(new Rect(margin,margin, oldMat.cols(), oldMat.rows()) );
        oldMat.copyTo(submat);

        submat = newMatBig.submat(new Rect(margin,margin, newMat.cols(), newMat.rows()) );
        newMat.copyTo(submat);


        //Imgproc.resize(oldMat,oldMatBig,sz);
        //Imgproc.resize(newMat,newMatBig,sz);


        FeatureDetector detector = FeatureDetector.create(FeatureDetector.AKAZE);
        DescriptorExtractor descriptor = DescriptorExtractor.create(DescriptorExtractor.AKAZE);
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        // First photo
        Imgproc.cvtColor(oldMatBig, oldMatBig, Imgproc.COLOR_RGB2GRAY);
        Mat descriptors1 = new Mat();
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();


        detector.detect(oldMatBig, keypoints1);
        descriptor.compute(oldMatBig, keypoints1, descriptors1);

        int size = (int) (keypoints1.total() * keypoints1.channels());
        //double[] keyPoints1Array = new double[size];
        //keypoints1.get(0,0,keyPoints1Array);

        //for (int i = 0; i < keyPoints1Array.length; i++)
        //{
           // Log.d("Describer","X = "+String.valueOf(keyPoints1Array[7*i])+"; Y = "+String.valueOf(keyPoints1Array[7*i+1]));

        //}


        // Second photo
        Imgproc.cvtColor(newMatBig, newMatBig, Imgproc.COLOR_RGB2GRAY);
        Mat descriptors2 = new Mat();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();

        detector.detect(newMatBig, keypoints2);
        descriptor.compute(newMatBig, keypoints2, descriptors2);

        // Matching
        MatOfDMatch matches = new MatOfDMatch();
        MatOfDMatch filteredMatches = new MatOfDMatch();
        matcher.match(descriptors1, descriptors2, matches);

        // Linking
        Scalar RED = new Scalar(255,0,0);
        Scalar GREEN = new Scalar(0,255,0);

        // Printing
        Log.d("Describer","New Data");
        Log.d("Describer",matches.size().toString());
        Log.d("Describer",keypoints1.dump());

        Mat outputImg = new Mat();
        MatOfByte drawnMatches = new MatOfByte();
        Features2d.drawMatches(oldMatBig, keypoints1, newMatBig, keypoints2, matches, outputImg, GREEN, RED, drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
        // input images width = 240, height  = 160
        // output image width = 480, height  = 160
        Bitmap b = Bitmap.createBitmap(outputImg.cols(), outputImg.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(outputImg, b);
        return b;


    }




}
