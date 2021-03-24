package com.banuba.sdk.manager;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ScaleGestureDetector;
import java.lang.Math;

import com.banuba.sdk.effect_player.EffectPlayer;
import com.banuba.sdk.effect_player.InputManager;
import com.banuba.sdk.effect_player.Touch;

import java.util.HashMap;

/**
 * Add this to your view to pass touch event in BanubaSdk:
 * `view.setOnTouchListener(new BanubaSdkTouchListener());`
 */
public class BanubaSdkTouchListener implements OnTouchListener {
    public BanubaSdkTouchListener(@NonNull Context context, @NonNull EffectPlayer effectPlayer) {
        mEffectPlayer = effectPlayer;
        mGestureHandler = new GesturesHandler(context, this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        HashMap<Long, Touch> touch = event2Touch(view, motionEvent);

        mGestureHandler.onTouch(view, motionEvent);

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                getInputManager().onTouchesBegan(touch);
                break;
            case MotionEvent.ACTION_UP:
                if (misLongTapOccurs) {
                    mGestureHandler.onLongTapEnded();
                    misLongTapOccurs = false;
                }
            case MotionEvent.ACTION_POINTER_UP:
                getInputManager().onTouchesEnded(touch);
                break;
            case MotionEvent.ACTION_CANCEL:
                getInputManager().onTouchesCancelled(touch);
                break;
            case MotionEvent.ACTION_MOVE:
                getInputManager().onTouchesMoved(touch);
                break;
            default:
                return false;
        }
        return true;
    }

    private InputManager getInputManager() {
        return mEffectPlayer.getInputManager();
    }

    private void longTapOccurs() {
        misLongTapOccurs = true;
    }

    @SuppressLint("UseSparseArrays")
    private static HashMap<Long, Touch> map = new HashMap<>(1);

    private static HashMap<Long, Touch> event2Touch(View view, MotionEvent event) {
        int pointerCount;
        int pointerIndex;

        // in case of move we need to send all touches with new coords
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            pointerIndex = 0;
            pointerCount = event.getPointerCount();
        } else {
            pointerIndex = event.getActionIndex();
            pointerCount = pointerIndex + 1;
        }

        map.clear();

        for (; pointerIndex < pointerCount; pointerIndex++) {
            float x = -1 + 2 * (event.getX(pointerIndex) / view.getWidth());
            float y = -1 + 2 * ((view.getHeight() - event.getY(pointerIndex)) / view.getHeight());
            long id = event.getPointerId(pointerIndex);

            map.put(id, new Touch(x, y, id));
        }

        return map;
    }

    private final EffectPlayer mEffectPlayer;
    private GesturesHandler mGestureHandler;
    private boolean misLongTapOccurs;

    private class GesturesHandler {
        GesturesHandler(Context context, BanubaSdkTouchListener listener) {
            mTouchListener = listener;
            mGestureDetector = new GestureDetector(new TouchGesturesHandler(this));
            mRotationDetector = new RotationGestureDetector(this);
            mScaleDetector = new ScaleGestureDetector(context, new ScaleGestureHandler(this));
        }

        public void onTouch(View view, MotionEvent event) {
            mCurrentView = view;
            mRotationDetector.onTouchEvent(event);
            mScaleDetector.onTouchEvent(event);
            mGestureDetector.onTouchEvent(event);
        }

        public void onFling(MotionEvent start, MotionEvent curr, float vx, float vy) {
            mTouchListener.getInputManager().onSwipeGesture(Math.signum(vx), -Math.signum(vy));

            if (curr.getActionMasked() == MotionEvent.ACTION_UP) {
                mTouchListener.getInputManager().onGestureEnded("Swipe");
            }
        }

        public void onLongTap(MotionEvent event) {
            HashMap<Long, Touch> touch = event2Touch(mCurrentView, event);
            long id = event.getPointerId(event.getActionIndex());
            Touch currTouch = touch.get(id);

            if (currTouch != null) {
                mTouchListener.getInputManager().onLongTapGesture(currTouch);
                mTouchListener.longTapOccurs();
            }
        }

        public void onLongTapEnded() {
            mTouchListener.getInputManager().onGestureEnded("LongTap");
        }

        public void onDoubleTap(MotionEvent event) {
            HashMap<Long, Touch> touch = event2Touch(mCurrentView, event);
            long id = event.getPointerId(event.getActionIndex());
            Touch currTouch = touch.get(id);

            if (currTouch != null) {
                mTouchListener.getInputManager().onDoubleTapGesture(currTouch);
            }
        }

        public void onDoubleTapEnded() {
            mTouchListener.getInputManager().onGestureEnded("DoubleTap");
        }

        public void onScale(float scale) {
            mTouchListener.getInputManager().onScaleGesture(scale);
        }

        public void onScaleEnded() {
            mTouchListener.getInputManager().onGestureEnded("Scale");
        }

        public void onRotation(float angle) {
            mTouchListener.getInputManager().onRotationGesture(-angle);
        }

        public void onRotationEnded() {
            mTouchListener.getInputManager().onGestureEnded("Rotation");
        }

        private GestureDetector mGestureDetector;
        private RotationGestureDetector mRotationDetector;
        private ScaleGestureDetector mScaleDetector;
        private BanubaSdkTouchListener mTouchListener;
        private View mCurrentView;
    }


    private class TouchGesturesHandler extends GestureDetector.SimpleOnGestureListener {
        TouchGesturesHandler(GesturesHandler handler) {
            mGestureHandler = handler;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            mGestureHandler.onLongTap(e);
            mIsLongPressOccurs = true;
            super.onLongPress(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            mGestureHandler.onFling(e1, e2, velocityX, velocityY);
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            mGestureHandler.onDoubleTap(e);
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                mGestureHandler.onDoubleTapEnded();
            }
            return super.onDoubleTapEvent(e);
        }

        private GesturesHandler mGestureHandler;
        private boolean mIsLongPressOccurs;
    }


    private class ScaleGestureHandler extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        ScaleGestureHandler(GesturesHandler handler) {
            mGestureHandler = handler;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mGestureHandler.onScale(detector.getScaleFactor());
            return super.onScale(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mGestureHandler.onScaleEnded();
            super.onScaleEnd(detector);
        }

        private GesturesHandler mGestureHandler;
    }


    private class RotationGestureDetector {
        RotationGestureDetector(GesturesHandler handler) {
            mGestureHandler = handler;
        }

        void onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mFirstFingerID = event.getPointerId(event.getActionIndex());
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    mSecondFingerID = event.getPointerId(event.getActionIndex());
                    mSecondFingerX = event.getX(event.findPointerIndex(mFirstFingerID));
                    mSecondFingerY = event.getY(event.findPointerIndex(mFirstFingerID));
                    mFirstFingerX = event.getX(event.findPointerIndex(mSecondFingerID));
                    mFirstFingerY = event.getY(event.findPointerIndex(mSecondFingerID));
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mFirstFingerID >= 0 && mSecondFingerID >= 0) {
                        float newFirstFingerX, newFirstFingerY, newSecondFingerX, newSecondFingerY;

                        newSecondFingerX = event.getX(event.findPointerIndex(mFirstFingerID));
                        newSecondFingerY = event.getY(event.findPointerIndex(mFirstFingerID));
                        newFirstFingerX = event.getX(event.findPointerIndex(mSecondFingerID));
                        newFirstFingerY = event.getY(event.findPointerIndex(mSecondFingerID));

                        mCurrAngle = getAngle(
                            mFirstFingerX - mSecondFingerX,
                            mFirstFingerY - mSecondFingerY,
                            newFirstFingerX - newSecondFingerX,
                            newFirstFingerY - newSecondFingerY);

                        mGestureHandler.onRotation(mCurrAngle);
                    }

                    break;
                case MotionEvent.ACTION_UP:
                    mFirstFingerID = -1;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    mSecondFingerID = -1;
                    mGestureHandler.onRotationEnded();
                    break;

                case MotionEvent.ACTION_CANCEL:
                    mFirstFingerID = -1;
                    mSecondFingerID = -1;
            }
        }

        float getAngle(float firstX, float firstY, float secondX, float secondY) {
            float angle = (float) Math.toDegrees((Math.atan2(firstX, firstY) - Math.atan2(secondX, secondY))) % 360;
            if (angle < -180.f)
                angle += 360.0f;
            if (angle > 180.f)
                angle -= 360.0f;
            return angle;
        }

        private float mFirstFingerX, mFirstFingerY, mSecondFingerX, mSecondFingerY;
        private int mFirstFingerID = -1, mSecondFingerID = -1;
        private float mCurrAngle = 0;

        private GesturesHandler mGestureHandler;
    }
}
