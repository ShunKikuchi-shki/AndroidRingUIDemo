package com.example.launchartest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

import com.example.launchartest.MyRing.MyIcon.MyIconComparator;

public class MyRing {
	public static float TOUCH_POINT_RADIUS_RATE = 0.7f;//各アイコンのタッチ範囲の大きさ(0.5fの時、ちょうどアイコン画像に内接する円の大きさになる。)
	float mX = 0.5f;
	float mY = 0.5f;//(SurfaceViewの左上を(0.0f,0.0f),右下を(1.0f,1.0f)として、リングの中心座標の位置
	float mRingSizeX = 1.4f;
	float mRingSizeY = 0.3f;//リングの大きさの相対座標
	float mRad = 0.0f;//最前面を180度,最背面を0度とした場合の角度。IconListの最初のIconの描画される角度になる。
	float mRadSpeed = 0.0f;//現在のリングの回転速度。フリックで速度が変わる。
	float mRadOffset = 0.0f;//次のアイコンとの角度
	float mIconSize = 0.2f;//各アイコンのサイズ
	float mIconMaxSize = 0.2f;//最前面でのアイコンの大きさ
	float mIconMinSize = 0.1f;//最後面でのアイコンの大きさ
	private List<MyIcon> mIconList = new ArrayList<MyIcon>();//アイコンのリスト
	private List<MyIcon> mDispIconList = new ArrayList<MyIcon>();//描画順(touch確認は逆順)に整列したmIconList
	private MyIconComparator mMyIconComparator = new MyIconComparator();
	/**
	 * Iconを追加する。
	 * 引数のIconは複製されて登録されるが、bitmapは共有されるので注意
	 * また、複製するフィールドを増やした場合は、内部で値をコピーするように変更すること。
	 * @param myIcon 登録するIcon
	 */
	public void addIcon(MyIcon myIcon) {
		MyIcon icon = new MyIcon();
		icon.mBitmap = myIcon.mBitmap;
		icon.mId = myIcon.mId;
		mIconList.add(icon);
		mDispIconList.add(icon);
		int size = mIconList.size();
		if (size > 0){
			mRadOffset = (float) (Math.PI * 2 / size);
		}
	}
	
	void update(){
		mRad += mRadSpeed;
		//時間の経過につれ回転速度を減衰させる。
		mRadSpeed *= 0.9f;
		float workRad = mRad;
		for(MyIcon myIcon : mIconList){
			myIcon.update(this,workRad);
			workRad += mRadOffset;
		}
		Collections.sort(mDispIconList,mMyIconComparator);
	}
	
	void doDraw(Canvas canvas, MySurfaceView view) {
		for(MyIcon myIcon : mDispIconList){
			myIcon.doDraw(canvas, view);
		}
		
	}
	public void onTouchDown(float x, float y, final MySurfaceView mySurfaceView) {
		for(int i = mDispIconList.size()-1;i >= 0; i--){
			MyIcon myIcon = mDispIconList.get(i);
			if(myIcon.isPointInside(x,y,mySurfaceView)){
				//Iconを送ることにしたので、IconにIdだけコピーして、送ってみる。
				//ランチャーとして起動したい場合は、MyIconに起動するアプリの情報も保持して送ることになる。
				final MyIcon sendIcon = new MyIcon();
				sendIcon.mId = myIcon.mId;
				mySurfaceView.getHandler().post(new Runnable() {
					
					@Override
					public void run() {
						mySurfaceView.mMySurfaceViewCallback.onIconTouched(sendIcon);
					}
				});
				break;
			}
		}
	}

	//アイコンクラス
	static class MyIcon{
		float mX;
		float mY;
		float mSize;//アイコンのサイズ。ここでは正方形限定
		int mId;
		Bitmap mBitmap;//アイコンのビットマップ
		public void update(MyRing myRing, float workRad) {
			//myRingの中心座標とサイズ、与えられた角度から、mSizeとmX,mYを計算
			float radiusX = myRing.mRingSizeX * 0.5f;
			float radiusY = myRing.mRingSizeY * 0.5f;
			float minY = myRing.mY - radiusY;
			float maxY = myRing.mY + radiusY;
			mX = (float) (myRing.mX + Math.sin(workRad)* radiusX);
			mY = (float) (myRing.mY - Math.cos(workRad)* radiusY);
			mSize = (mY - minY)/(maxY - minY) * (myRing.mIconMaxSize - myRing.mIconMinSize) + myRing.mIconMinSize;
			
		}
		void doDraw(Canvas canvas, MySurfaceView view){
			//canvasに描画。
			float offsetX = view.mDrawOffsetX;
			float offsetY = view.mDrawOffsetY;
			float mapWidth = view.mDrawWidth;
			float mapHeight = view.mDrawHeight;
			RectF dst = new RectF((float) ((mX- mSize * 0.5) * mapWidth+offsetX),
					(float)((mY-mSize * 0.5) * mapHeight + offsetY),
					(float)((mX+mSize* 0.5) * mapWidth + offsetX),
					(float)((mY+mSize * 0.5) * mapHeight + offsetY));

			canvas.drawBitmap(mBitmap, null, dst, null);

		};
		/**
		 * タッチがIconのクリック可能ポイント内にあるかを円で判定
		 * @param x x中心座標
		 * @param y y中心座標
		 * @return クリックされているとtrue, されていなかったらfalse
		 */
		boolean isPointInside(float touchX, float touchY, MySurfaceView view){
			//標的とタッチされたポイントとのアイコン中心からの距離を計算する(正方形の前提)
			float offsetX = view.mDrawOffsetX;
			float offsetY = view.mDrawOffsetY;
			float mapWidth = view.mDrawWidth;
			float mapHeight = view.mDrawHeight;
			float dx = ((touchX-offsetX) / mapWidth)- mX;
			float dy = ((touchY-offsetY) / mapHeight) - mY;
			//ルートの計算は重いらしので、距離は二乗で判定
			float distance = dx * dx + dy * dy;
			if (distance <= mSize * mSize * TOUCH_POINT_RADIUS_RATE * TOUCH_POINT_RADIUS_RATE){
				return true;
			}
			return false;
		}
		static class MyIconComparator implements Comparator<MyIcon>{

			@Override
			public int compare(MyIcon myIcon1, MyIcon myIcon2) {
				return (int) (myIcon1.mY - myIcon2.mY);
			}
			
		}
	}
}
