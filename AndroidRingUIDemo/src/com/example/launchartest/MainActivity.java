package com.example.launchartest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import com.example.launchartest.MyRing.MyIcon;
import com.example.launchartest.MySurfaceView.MySurfaceViewCallback;

public class MainActivity extends Activity implements MySurfaceViewCallback{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		MySurfaceView mySurfaceView = (MySurfaceView) findViewById(R.id.mySurfaceView);
		Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		for(int i = 0; i < 7; i++){
			MyIcon myIcon = new MyIcon();
			myIcon.mId = i;
			myIcon.mBitmap = bitmap;
			mySurfaceView.addIcon(myIcon);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onIconTouched(MyIcon myIcon) {
		Toast.makeText(this, myIcon.mId +" is touched.", Toast.LENGTH_SHORT).show();
	}
}
