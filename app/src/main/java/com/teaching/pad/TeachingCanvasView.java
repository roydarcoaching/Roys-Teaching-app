package com.teaching.pad;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TeachingCanvasView extends View {

    private Bitmap backgroundBitmap = null;
    private Bitmap insertedImageBitmap = null;
    private Bitmap drawBitmap;
    private Canvas drawCanvas;
    private Path drawPath = new Path();
    private Paint drawPaint = new Paint();
    private Paint canvasPaint = new Paint(Paint.DITHER_FLAG);

    private boolean isEraser = false;
    private float currentX, currentY;

    public TeachingCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaint();
    }

    private void initPaint() {
        drawPaint.setColor(Color.RED);
        drawPaint.setAntiAlias(true);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        drawPaint.setStrokeWidth(5f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            drawBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            drawCanvas = new Canvas(drawBitmap);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }
        if (insertedImageBitmap != null) {
            canvas.drawBitmap(insertedImageBitmap, 50, 50, null);
        }
        if (drawBitmap != null) {
            canvas.drawBitmap(drawBitmap, 0, 0, canvasPaint);
        }
        canvas.drawPath(drawPath, drawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int toolType = event.getToolType(0);
        if (toolType != MotionEvent.TOOL_TYPE_STYLUS && toolType != MotionEvent.TOOL_TYPE_MOUSE) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();
        float pressure = event.getPressure();
        if (pressure <= 0) pressure = 0.5f;

        if (isEraser) {
            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            drawPaint.setStrokeWidth(40f);
        } else {
            drawPaint.setXfermode(null);
            drawPaint.setStrokeWidth(6f * (pressure * 2.5f));
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.reset();
                drawPath.moveTo(x, y);
                currentX = x;
                currentY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.quadTo(currentX, currentY, (x + currentX) / 2, (y + currentY) / 2);
                currentX = x;
                currentY = y;
                break;
            case MotionEvent.ACTION_UP:
                drawPath.lineTo(currentX, currentY);
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();
                break;
        }
        invalidate();
        return true;
    }

    public void setEraser(boolean eraser) {
        this.isEraser = eraser;
    }

    public void clearCanvas() {
        if (drawCanvas != null) {
            drawCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            invalidate();
        }
    }

    public void setPageBackground(Bitmap bmp) {
        this.backgroundBitmap = bmp;
        this.insertedImageBitmap = null;
        clearCanvas();
    }

    public void insertImage(Bitmap bmp) {
        this.insertedImageBitmap = Bitmap.createScaledBitmap(bmp, 400, 400, true);
        invalidate();
    }
}
