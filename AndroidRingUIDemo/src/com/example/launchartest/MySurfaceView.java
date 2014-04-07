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
	//����View�̕`��͈͂̍��オ(0.0f,0.0f),�E����(1.0f,1.0f)�ɂȂ�悤�ɕ`�悷��B
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
	 * �\������Icon��ǉ�����B
	 * MyIcon�͕������Ċi�[����邪�Abitmap���͕̂������ꂸ�A�����Ƌ��L�����̂Œ���
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
		//view�̌`�łƂ��ő�̐����`��^�񒆂ɂƂ�B���̗̈悪MyRing,������myIcon���猩�č���(0.0f,0.0f),�E��(1.0f,1.0f)�̕`���ԂɂȂ�B
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
			//�`��
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
	 *  �e��X�V����
	 *  ���̏����͕`��X���b�h��ŌĂ΂�Ă���(����Activity��UI�͂�����Ȃ��̂Œ���)�B
	 */
	private void doUpdate(){
		myRing.update();
	}

	/**
	 *  �e��`�揈��
	 *  ���̏����͕`��X���b�h��ŌĂ΂�Ă���B
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
			//���s�̒l��K���ɒ��߂���Ɗ��x���ς��܂��B
			myRing.mRad += distanceX/200;
			myRing.mRadSpeed = 0.0f;
			return super.onScroll(event1, event2, distanceX, distanceY);
		}

		@Override 
		public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
			//���s�̒l��K���ɒ��߂���Ɗ��x���ς��܂��B
			mViewRef.get().myRing.mRadSpeed = -velocityX/2000;
			return super.onFling(event1, event2, velocityX, velocityY); 
		} 

	}
}


