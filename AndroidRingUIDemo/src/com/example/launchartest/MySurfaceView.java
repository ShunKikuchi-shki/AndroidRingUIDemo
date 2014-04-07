package com.example.launchartest;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import com.example.launchartest.MyRing.MyIcon;

public class MySurfaceView extends SurfaceView implements Callback,Runnable{
	//このViewの描画範囲の左上が(0.0f,0.0f),右下が(1.0f,1.0f)になるように描画する。
	float mDrawOffsetX, mDrawOffsetY, mDrawWidth, mDrawHeight;
	private boolean mRunning;
	private Thread mThread;
	private SurfaceHolder mHolder;
	private MyRing myRing = new MyRing();
	private GestureDetector mGestureDetector;
	MySurfaceViewCallback mMySurfaceViewCallback;

	interface MySurfaceViewCallback{
		void onIconTouched(MyIcon myIcon);
	}

	public MySurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public MySurfaceView(Context context) {
		super(context);
		init(context);

		mMySurfaceViewCallback = (MySurfaceViewCallback) context;
	}
	private void init(Context context){
		try {
			mMySurfaceViewCallback = (MySurfaceViewCallback) context;		
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " must implement MySurfaceViewCallback");
		}
		mRunning = true;
		mGestureDetector = new GestureDetector(getContext(),new MyGestureListener(this));
		mHolder = getHolder();
		mHolder.setFormat(PixelFormat.TRANSLUCENT);
		mHolder.addCallback(this);
		setZOrderOnTop(true);

	};
	/**
	 * 表示するIconを追加する。
	 * MyIconは複製して格納されるが、bitmap自体は複製されず、引数と共有されるので注意
	 * @param myIcon
	 */
	public void addIcon(MyIcon myIcon){
		synchronized(mHolder){
			myRing.addIcon(myIcon);
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mRunning = true;
		mThread = new Thread(this);
		mThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mRunning = false;
		boolean retry = true;
		while(retry){
			try {
				mThread.join();
				retry = false;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		mDrawOffsetX = 0;
		mDrawOffsetY = 0;
		//viewの形でとれる最大の正方形を真ん中にとる。その領域がMyRing,内部のmyIconから見て左上(0.0f,0.0f),右下(1.0f,1.0f)の描画空間になる。
		if (width > height){
			mDrawOffsetX = (width - height) / 2;
			mDrawWidth = height;
			mDrawHeight = height;
		}else{
			mDrawOffsetY = (height - width) / 2;
			mDrawWidth = width;
			mDrawHeight = width;
		}
	}	
	@Override
	public boolean onTouchEvent(MotionEvent event){
		synchronized(mHolder){
			mGestureDetector.onTouchEvent(event);
		}
		return true;
	}

	@Override
	public void run() {
		Canvas c;
		long lastUpdateTime = System.currentTimeMillis();
		while(mRunning){
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long nowTime = System.currentTimeMillis();
			long difference = nowTime - lastUpdateTime;
			while(difference >= 17){
				difference -= 17;
				synchronized (mHolder){
					doUpdate();
				}
			}			
			lastUpdateTime = nowTime - difference;
			//描画
			c = null;
			try{
				synchronized (mHolder){
					c = mHolder.lockCanvas(null); 
					if(c != null){
						doDraw(c);
					}
				}
			}finally{
				if(c != null){
					mHolder.unlockCanvasAndPost(c);
				}
			}
		}
	}
	/**
	 *  各種更新処理
	 *  この処理は描画スレッド上で呼ばれている(直接ActivityのUIはいじれないので注意)。
	 */
	private void doUpdate(){
		myRing.update();
	}

	/**
	 *  各種描画処理
	 *  この処理は描画スレッド上で呼ばれている。
	 */
	private void doDraw(Canvas canvas){
		canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
		myRing.doDraw(canvas, this);
	}

	private static class MyGestureListener extends SimpleOnGestureListener{
		private WeakReference<MySurfaceView> mViewRef;
		MyGestureListener(MySurfaceView view){
			mViewRef = new WeakReference<MySurfaceView>(view);
		}
		@Override
		public boolean onDown(MotionEvent event) {
			mViewRef.get().myRing.mRadSpeed = 0.0f;
			return false;
		} 
		@Override
		public boolean onSingleTapConfirmed(MotionEvent event) {
			mViewRef.get().myRing.onTouchDown(event.getX(), event.getY(), mViewRef.get());
			return super.onSingleTapConfirmed(event);
		}

		@Override
		public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
			MyRing myRing = mViewRef.get().myRing;
			//下行の値を適当に調節すると感度が変わります。
			myRing.mRad += distanceX/200;
			myRing.mRadSpeed = 0.0f;
			return super.onScroll(event1, event2, distanceX, distanceY);
		}

		@Override 
		public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
			//下行の値を適当に調節すると感度が変わります。
			mViewRef.get().myRing.mRadSpeed = -velocityX/2000;
			return super.onFling(event1, event2, velocityX, velocityY); 
		} 

	}
}


