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
	public static float TOUCH_POINT_RADIUS_RATE = 0.7f;//�e�A�C�R���̃^�b�`�͈͂̑傫��(0.5f�̎��A���傤�ǃA�C�R���摜�ɓ��ڂ���~�̑傫���ɂȂ�B)
	float mX = 0.5f;
	float mY = 0.5f;//(SurfaceView�̍����(0.0f,0.0f),�E����(1.0f,1.0f)�Ƃ��āA�����O�̒��S���W�̈ʒu
	float mRingSizeX = 1.4f;
	float mRingSizeY = 0.3f;//�����O�̑傫���̑��΍��W
	float mRad = 0.0f;//�őO�ʂ�180�x,�Ŕw�ʂ�0�x�Ƃ����ꍇ�̊p�x�BIconList�̍ŏ���Icon�̕`�悳���p�x�ɂȂ�B
	float mRadSpeed = 0.0f;//���݂̃����O�̉�]���x�B�t���b�N�ő��x���ς��B
	float mRadOffset = 0.0f;//���̃A�C�R���Ƃ̊p�x
	float mIconSize = 0.2f;//�e�A�C�R���̃T�C�Y
	float mIconMaxSize = 0.2f;//�őO�ʂł̃A�C�R���̑傫��
	float mIconMinSize = 0.1f;//�Ō�ʂł̃A�C�R���̑傫��
	private List<MyIcon> mIconList = new ArrayList<MyIcon>();//�A�C�R���̃��X�g
	private List<MyIcon> mDispIconList = new ArrayList<MyIcon>();//�`�揇(touch�m�F�͋t��)�ɐ��񂵂�mIconList
	private MyIconComparator mMyIconComparator = new MyIconComparator();
	/**
	 * Icon��ǉ�����B
	 * ������Icon�͕�������ēo�^����邪�Abitmap�͋��L�����̂Œ���
	 * �܂��A��������t�B�[���h�𑝂₵���ꍇ�́A�����Œl���R�s�[����悤�ɕύX���邱�ƁB
	 * @param myIcon �o�^����Icon
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
		//���Ԃ̌o�߂ɂ��]���x������������B
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
				//Icon�𑗂邱�Ƃɂ����̂ŁAIcon��Id�����R�s�[���āA�����Ă݂�B
				//�����`���[�Ƃ��ċN���������ꍇ�́AMyIcon�ɋN������A�v���̏����ێ����đ��邱�ƂɂȂ�B
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

	//�A�C�R���N���X
	static class MyIcon{
		float mX;
		float mY;
		float mSize;//�A�C�R���̃T�C�Y�B�����ł͐����`����
		int mId;
		Bitmap mBitmap;//�A�C�R���̃r�b�g�}�b�v
		public void update(MyRing myRing, float workRad) {
			//myRing�̒��S���W�ƃT�C�Y�A�^����ꂽ�p�x����AmSize��mX,mY���v�Z
			float radiusX = myRing.mRingSizeX * 0.5f;
			float radiusY = myRing.mRingSizeY * 0.5f;
			float minY = myRing.mY - radiusY;
			float maxY = myRing.mY + radiusY;
			mX = (float) (myRing.mX + Math.sin(workRad)* radiusX);
			mY = (float) (myRing.mY - Math.cos(workRad)* radiusY);
			mSize = (mY - minY)/(maxY - minY) * (myRing.mIconMaxSize - myRing.mIconMinSize) + myRing.mIconMinSize;
			
		}
		void doDraw(Canvas canvas, MySurfaceView view){
			//canvas�ɕ`��B
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
		 * �^�b�`��Icon�̃N���b�N�\�|�C���g���ɂ��邩���~�Ŕ���
		 * @param x x���S���W
		 * @param y y���S���W
		 * @return �N���b�N����Ă����true, ����Ă��Ȃ�������false
		 */
		boolean isPointInside(float touchX, float touchY, MySurfaceView view){
			//�W�I�ƃ^�b�`���ꂽ�|�C���g�Ƃ̃A�C�R�����S����̋������v�Z����(�����`�̑O��)
			float offsetX = view.mDrawOffsetX;
			float offsetY = view.mDrawOffsetY;
			float mapWidth = view.mDrawWidth;
			float mapHeight = view.mDrawHeight;
			float dx = ((touchX-offsetX) / mapWidth)- mX;
			float dy = ((touchY-offsetY) / mapHeight) - mY;
			//���[�g�̌v�Z�͏d���炵�̂ŁA�����͓��Ŕ���
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
