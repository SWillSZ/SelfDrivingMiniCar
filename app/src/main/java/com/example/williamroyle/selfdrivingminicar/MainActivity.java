package com.example.williamroyle.selfdrivingminicar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.widget.ImageView;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceView;
import android.widget.Button;
import android.os.Message;
import java.util.concurrent.locks.*;
import android.graphics.Bitmap;
import org.opencv.core.Core;

public class MainActivity extends Activity implements CvCameraViewListener2 {

    public Mat Mrgba;
    public Mat oldMrgba;
    Button button;
    public Handler bgThreadHandler;
    public Handler mainUIHandler;
    public ReentrantLock lock;
    public Thread runner;
    public Activity toShare;
    public UIHandler otherClass;
    //protected Camera mCamera = null;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                    //mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //


            /*try
            {
                mCamera = Camera.open();
            }
            catch (Exception e)
            {
                Log.e("Shit", "Camera is not available (in use or does not exist): " + e.getLocalizedMessage());
            }*/
        toShare = this;
        otherClass = new UIHandler(toShare);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.controlinterface);

        button=(Button)findViewById(R.id.button);
        button.setText("Start");

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        //mOpenCvCameraView.setMaxFrameSize(480, 9999);
        //mOpenCvCameraView.disableView();
        //mOpenCvCameraView.enableView();
        /*try
        {
            CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM;
            List<android.hardware.Camera.Size> Sizes = c.getParameters().getSupportedPictureSizes();
            Log.d("SizeInfo","Sizes");
        }
        catch (Exception E)
        {

        }*/// ugh
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        lock = new ReentrantLock();
        mainUIHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                Log.d("UIHandle","Message received by UI thread");
                Bitmap returned = (Bitmap)msg.obj;
                Log.d("UIHandle","NewWidth = " + String.valueOf(returned.getWidth()));
                ImageView toChange = (ImageView)findViewById(R.id.imageViewCamera);


                // We set controller1 and controller2
                lock.lock();
                ImageView picOld = (ImageView)findViewById(R.id.control1);
                picOld.setImageBitmap(UIHandler.editImageControllersEmpty(oldMrgba));
                ImageView picNew = (ImageView)findViewById(R.id.control2);
                picNew.setImageBitmap(UIHandler.editImageControllersEmpty(Mrgba));
                lock.unlock();





                toChange.setImageBitmap(returned);


                ImageMethods.sleep(100);
                Message mail = bgThreadHandler.obtainMessage();
                Mat[] matPics = new Mat[2];
                lock.lock();
                matPics[0]= oldMrgba;
                matPics[1]= Mrgba;
                mail.obj = matPics;
                lock.unlock();
                mail.sendToTarget();


            }
        };
        Log.d("UICreateHandle","UI Handler Created");

        runner = new BackgroundThread();
        runner.start();

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug())
        {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        }
        else
        {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy()
    {
        Log.d("UIHandle","Destroy Called");
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height)
    {

    }

    public void onCameraViewStopped() {
        //Mrgba.release();
    }


    public Mat onCameraFrame(CvCameraViewFrame inputFrame)
    {

        if (lock.tryLock())
        {
            this.oldMrgba = this.Mrgba;
            this.Mrgba = inputFrame.rgba();

            Core.flip(this.Mrgba, this.Mrgba, -1);
            //Imgproc.flip(const Mat& src, Mat& dst, int flipCode);
            lock.unlock();
        }
        return this.Mrgba;
    }


    public void doSomething(View view)
    {
        Message msg = bgThreadHandler.obtainMessage();
        Mat[] matPics = new Mat[2];
        lock.lock();
        matPics[0]= oldMrgba;
        matPics[1]= Mrgba;
        msg.obj = matPics;
        lock.unlock();
        msg.sendToTarget();
        // We then disable the button
        button=(Button)findViewById(R.id.button);
        button.setVisibility(View.GONE);
        //((Button)findViewById(R.id.button)).setVisibility(false);
    }


    class BackgroundThread extends Thread {
        public void run()
        {

            Log.d("Handle","Running Background thread!");
            Looper.prepare();
            bgThreadHandler = new Handler()
            {
                public void handleMessage(Message msg)
                {
                    Log.d("my arm","my arm");
                    Mat[] matPics = (Mat[])msg.obj;
                    Mat mRgba = matPics[0];
                    // Clever trick to get Mrgba non-null for sure
                    lock.lock();
                    while (mRgba==null||oldMrgba==null)
                    {
                        lock.unlock();
                        try{Thread.sleep(100);}catch(Exception E){Log.d("HandlerException","SleepError");}
                        Log.d("Handler","Waiting for Mrgba to be loaded");
                        lock.lock();
                    }
                    // At this stage, lock is locked and Mrgba is not null
                    String toLog = String.valueOf(mRgba.width());
                    Log.d("Handle","Width = "+toLog);
                    // edit image here
                    Mat tmp = mRgba.clone();
                    Mat oldTmp = matPics[1].clone();
                    lock.unlock();
                    String print = String.valueOf(mainUIHandler != null);
                    Log.d("Handler","Trying to send message from Background thread to mainUIHandler! mainUIHandler exists = "+print);
                    Message mail = mainUIHandler.obtainMessage();
                    //Bitmap toSend = UIHandler.editImageControllersEmpty(tmp,otherClass);
                    Bitmap toSend = UIHandler.findMatches(tmp,oldTmp,otherClass);
                    Log.d("Handler","NewWidth = " + String.valueOf(toSend.getWidth()));
                    mail.obj = toSend; // Put the string into Message, into "obj" field.
                    try{mainUIHandler.sendMessage(mail);}catch(Exception E){Log.d("Handler",E.toString());}
                    Log.d("Handler","Message sent from BackGround Thread!");

                }
            };
            Log.d("Handle","Starting Loop!");
            Looper.loop();
        }
    }







}