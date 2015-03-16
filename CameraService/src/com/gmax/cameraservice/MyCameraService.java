package com.gmax.cameraservice;

import java.io.IOException;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class MyCameraService extends Service {

	Camera mCamera;
	SurfaceTexture mSurfaceTexture;
	public final static int TEXTURE_ID = 0x000025;
	public final static int CHANGE_OFFSET = 0x000026;
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	

	@Override
	public void onCreate() {
		super.onCreate();
		Log.e("ServiceCamer","create the service now");
		mSurfaceTexture = new SurfaceTexture(TEXTURE_ID);
		try{
			mCamera = Camera.open(1);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	static public void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) 
	{
		final int frameSize = width * height;

		for (int j = 0, yp = 0; j < height; j++) {
			int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
			for (int i = 0; i < width; i++, yp++) {
				int y = (0xff & ((int) yuv420sp[yp])) - 16;
				if (y < 0)
					y = 0;
				if ((i & 1) == 0) {
					v = (0xff & yuv420sp[uvp++]) - 128;
					u = (0xff & yuv420sp[uvp++]) - 128;
				}
				int y1192 = 1192 * y;
				int r = (y1192 + 1634 * v);
				int g = (y1192 - 833 * v - 400 * u);
				int b = (y1192 + 2066 * u);
				if (r < 0)
					r = 0;
				else if (r > 262143)
					r = 262143;
				if (g < 0)
					g = 0;
				else if (g > 262143)
					g = 262143;
				if (b < 0)
					b = 0;
				else if (b > 262143)
					b = 262143;
				rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
			}
		}
	}
	
	int[] faceinfo = new int[3];
	public  int[] getDetectedInfo(){
		return faceinfo;
	}
	
	FaceDetector faceDetector = null;
	FaceDetector.Face[] face;
	Bitmap srcFace = null;
	int nub = 3;
	float dis = 0f;
	int dd = 0;
	public Bitmap detectFace(Bitmap srcImg , int[] xyr)
	{
		srcFace = srcImg.copy(Config.RGB_565, true);
		int w = srcFace.getWidth();
		int h = srcFace.getHeight();
		faceDetector = new FaceDetector(w , h , nub);
		face = new FaceDetector.Face[nub];
		
		int nFace = faceDetector.findFaces(srcFace , face);
		Log.e("nFace" , "ºÏ≤‚µΩ»À¡≥£∫n = " + nFace);
		
		for(int i = 0; i < nFace; i++)
		{
			Face f  = face[i];
			PointF midPoint = new PointF();
			dis = f.eyesDistance();
			f.getMidPoint(midPoint);
			dd = (int)(dis);
			Point eyeLeft = new Point( (int)(midPoint.x - dis/2) , (int)midPoint.y );
			Point eyeRight = new Point( (int)(midPoint.x + dis/2) , (int)midPoint.y );
			Rect faceRect = new Rect( (int)(midPoint.x - dd) , (int)(midPoint.y - dd) , (int)(midPoint.x + dd) , (int)(midPoint.y + dd) );
			
			Log.e("left" , "◊Û—€◊¯±Í x = " + eyeLeft.x + "y = " + eyeLeft.y);
			Log.e("eyeDis" , Integer.toString(dd));
			Log.e("midPoint" , "midPoint x = " + midPoint.x + "y = " + midPoint.y);
			Log.e("right" , "”“—€◊¯±Í x = " + eyeRight.x + "y = " + eyeRight.y);
			if((int)midPoint.x==0){}else{
			xyr[0] = (int)midPoint.x;
			xyr[1] = (int)midPoint.y;
			xyr[2] = dd;
			}

		}

		//Ω´ªÊ÷∆ÕÍ≥…∫ÛµƒfaceBitmap∑µªÿ
		return srcFace;
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		mCamera.stopPreview();
		mCamera.setPreviewCallback(null);
		mCamera.release();
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		try {
			mCamera.setPreviewTexture(mSurfaceTexture);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mCamera.startPreview();
		mCamera.setPreviewCallback(new PreviewCallback() {
			
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				// TODO Auto-generated method stub
//				Log.e("ServiceCamer","get data from callback"+data.length);
				int imageWidth = mCamera.getParameters().getPreviewSize().width;
    			int imageHeight = mCamera.getParameters().getPreviewSize().height;
				int RGBData[] = new int[imageWidth * imageHeight];
    			
    			decodeYUV420SP(RGBData, data, imageWidth, imageHeight); //ÔøΩÔøΩÔøΩÔøΩ
    			
    			Bitmap bitmap = Bitmap.createBitmap(RGBData , imageWidth, imageHeight, Config.ARGB_8888);
    			detectFace(bitmap, faceinfo);
    			
    			Intent intent = new Intent();  
                intent.setAction("android.intent.action.MY_RECEIVER");  
                intent.putExtra("left", (float)faceinfo[1]);
                intent.putExtra("right", (float)faceinfo[2]);
                sendBroadcast(intent); 
			}
		});
		return super.onStartCommand(intent, flags, startId);
	}
	
	

}
