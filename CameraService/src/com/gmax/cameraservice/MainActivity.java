package com.gmax.cameraservice;


import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity  {

	MyReceiver myReceiver;
	Intent i;
	TextView showPositonTV;
	private static final int REFRESHPOSTION = 1;
	public  Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			switch (msg.what) {
			case REFRESHPOSTION:
				float[] eyes = msg.getData().getFloatArray("eye");
				if(showPositonTV!=null){
					showPositonTV.setText("left eye position is :"+eyes[0]+"    right eye position is "+eyes[1]);
				}
				break;
			default:
				
				break;
			}
		}
		
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		showPositonTV = (TextView)findViewById(R.id.showposition);
		 i = new Intent(MainActivity.this,MyCameraService.class);
//		bindService(i,conn,BIND_AUTO_CREATE);
		startService(i);
		myReceiver = new MyReceiver();
		IntentFilter filter = new IntentFilter();  
        filter.addAction("android.intent.action.MY_RECEIVER");  
        registerReceiver(myReceiver, filter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	ServiceConnection conn = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
		}
	};
	

	private void changeOffset(float offset){
		Log.e("ServiceCamer", "change the offset now "+offset);
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		stopService(i);
		unregisterReceiver(myReceiver);
	}
	
	private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            float lefteye = bundle.getFloat("left");
            float righteye = bundle.getFloat("right");
            float[] eyesPosition = {lefteye,righteye};
//            changeOffset(offset);
            Bundle eyeBundle = new Bundle();
            eyeBundle.putFloatArray("eye", eyesPosition);
            Message msg = handler.obtainMessage();
            msg.what = REFRESHPOSTION;
            msg.setData(eyeBundle);
            handler.sendMessage(msg);
        }  
    }
}
