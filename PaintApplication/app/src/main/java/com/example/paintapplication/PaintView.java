package com.example.paintapplication;

import android.view.View;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import java.util.ArrayList;


//This class represents the basic building block for user interface components.
// A View occupies a rectangular area on the screen and is responsible for drawing
// and event handling. View is the base class for widgets, which are used to create
// interactive UI components (buttons, text fields, etc.).
public class PaintView extends View {

    public static int BRUSH_SIZE = 10;
    public static final int DEFAULT_COLOR = Color.BLACK;
    public static final int DEFAULT_BG_COLOR = Color.WHITE;
    private static final float TOUCH_TOLERANCE = 4;
    private float mX, mY;
    private Path mPath;
    private Paint mPaint;
    //The PaintView will store several FingerPath objects inside an ArrayList field
    private ArrayList<FingerPath> paths = new ArrayList<>();
    private int currentColor;
    private int backgroundColor = DEFAULT_BG_COLOR;
    private int strokeWidth;
    private boolean emboss;
    private boolean blur;
    private MaskFilter mEmboss;
    private MaskFilter mBlur;
    private Bitmap mBitmap;
    //Besides, the PaintView object has a field for the Canvas used
    // to draw the paths represented on the screen to the user.
    private Canvas mCanvas;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);

    public PaintView(Context context) {
        this(context, null);
    }

    //In the constructor of the PaintView, we initialize the Paint Object used to draw
    // the paths on the Canvas. We need to define the style of the Paint Object as STROKE.
    // Then, we set the stroke join and stroke cap to ROUND. Finally, we create an
    // EmbossMaskFilter Object for the emboss mode and an BlurMaskFilter for the blur mode.
    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(DEFAULT_COLOR);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setXfermode(null);
        mPaint.setAlpha(0xff);

        mEmboss = new EmbossMaskFilter(new float[]{1, 1, 1}, 6.4f, 0.81f, 50.5f);
        mBlur = new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL);
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    //we add an init method on the PaintView class.
    //This method will take a DisplayMetrics Object in parameter and will be
    //responsible to define the height and width of the PaintView.
    //Furthermore, we initialize the Canvas and its underlying Bitmap used to draw paths on the screen.
    //A structure describing general information about a display,
    // such as its size, density, and font scaling.
    public void init(DisplayMetrics metrics) {
        int height = metrics.heightPixels;
        int width = metrics.widthPixels;

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        currentColor = DEFAULT_COLOR;
        strokeWidth = BRUSH_SIZE;
    }

    //The PaintView will expose three methods to switch between the three modes of
    // drawing : normal, emboss and blur. The clear method will let us to clear the
    // PaintView by clearing the list of Finger Paths.

    public void normal() {
        emboss = false;
        blur = false;
    }

    public void emboss() {
        emboss = true;
        blur = false;
    }

    public void blur() {
        emboss = false;
        blur = true;
    }

    public void clear() {
        backgroundColor = DEFAULT_BG_COLOR;
        paths.clear();
        normal();
        invalidate();
    }

    //Then, we override the onDraw method of the View class. First we save the
    // current state of the Canvas instance before to draw the background color
    // of the PaintView. We iterate on the list of Finger Paths and we draw
    // the Path Object contained in the current FingerPath Object on the Canvas.
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        mCanvas.drawColor(backgroundColor);

        for (FingerPath fp : paths) {
            mPaint.setColor(fp.color);
            mPaint.setStrokeWidth(fp.strokeWidth);
            mPaint.setMaskFilter(null);

            //To apply the specific effect defined by the user, like emboss or blur,
            //we set the corresponding mask filter on the Paint Object used to draw
            //the FingerPath if needed. Finally, we draw all these elements on the
            //Canvas of the PaintView and we restore the current Canvas.

            if (fp.emboss)
                mPaint.setMaskFilter(mEmboss);
            else if (fp.blur)
                mPaint.setMaskFilter(mBlur);

            mCanvas.drawPath(fp.path, mPaint);

        }

        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.restore();
    }


    //In the touchStart method, we start by creating a new Path and a new FingerPath Objects.
    // We save the current coordinates (x,y) of the finger of the user and we call
    // the moveTo method of the Path object.

    private void touchStart(float x, float y) {
        mPath = new Path();
        FingerPath fp = new FingerPath(currentColor, emboss, blur, strokeWidth, mPath);
        paths.add(fp);

        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    //In the touchMove method, we check if the move on the screen if greater
    //than the touch tolerance defined as a constant previously. If yes,
    //we call the quadTo method of the Path Object starting from the last point
    //touched and going to the average position between the first position and the current position.

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    //In the touchUp method, we end the line by calling the lineTo method of
    //the Path object with the last position touched on the screen.
    private void touchUp() {
        mPath.lineTo(mX, mY);
    }


    //Now, we need to manager the users’ touches on the screen to save the
    // paths drawn on the screen with their fingers. We override the onTouchEvent
    // method and we define three methods to manage the three following actions :
    //ACTION_DOWN with a touchStart method
    //ACTION_MOVE with a touchMove method
    //ACTION_UP with a touchUp method


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                //method of View class
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                break;
        }

        return true;
    }
}