/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors: Allan Marube
 *
 *******************************************************************************/
package com.example.SmartBoard;


import android.content.Context;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;

import android.util.AttributeSet;

import android.util.SparseArray;
import android.view.View;

import android.view.MotionEvent;
import android.widget.*;
import org.eclipse.paho.android.service.MqttAndroidClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.UUID;


/**
 * Created by Allan Marube on 7/15/2014.
 * Main View that contains canvas to draw upon, handles all touch Events for
 * draw, drag, erase, inserting/removing objects.
 *
 */
public class DrawingView extends View {
    private String textBoxData; //Insert Text data
    private Path drawPath; //user drawPath
    private Path drawPathRecv; //drawPath for other users

    //drawing and canvas paint
    private Paint drawPaint, drawPaintSender, canvasPaint;
    private int paintColor= Color.BLACK; //system color
    private int strokeWidth = 5; //width of pencil
    private String mode = "pencil"; //draw mode
    //canvas
    private Canvas drawCanvas; //canvas
    public int width = 0; //canvas width
    public int height = 0;//canvas height
    //canvas bitmap
    private Bitmap canvasBitmap;
    private double mX; //current point touched in freedraw Mode
    private double mY; //current point touched in freedraw Mode

    //Contains Path for each client who draws to the board
    private Hashtable<String, Path> clientPaths = new Hashtable<String, Path>();

    // private boolean mPoint = false;
    private MqttAndroidClient client; //Mqtt Client

    //holds instance of MQTTHandler
    private MQTTHandler mqtt = MQTTHandler.getInstance();

    //erase flag
    // private boolean erase = false;

    //Drawing modes. default mode is free draw mode
    /******************************************************/
    public boolean rectMode = false;
    public boolean circleMode = false;
    public boolean lineMode = false;
    public boolean textMode = false;
    public boolean dragMode = false;
    public boolean removeObjectMode = false;
    public boolean colorDropperMode = false;
    public int dropperColor = Color.BLACK; //dropper color
    int dropperX, dropperY; //dropper position

    public boolean textSizeMode = false;
    public int textViewSize = 15; //textView size
    int textResizePosX, textResizePosY; //text positon

    //points define position of the blue draw handles for object being drawn or resized
    Point [] points = new Point[4];
    boolean finished = false; //marked if object being drawn has been finished
    int groupId = -1;
    private ArrayList<ColorBall> colorballs = new ArrayList<ColorBall>(); //balls used as handles for drawn objects
    private int balID = 0;


    public Hashtable<String, JSONObject> objectDrawables = new Hashtable<String, JSONObject>(); //holds objects drawn on screen
    public Hashtable<String, JSONObject> textObjects = new Hashtable<String, JSONObject>(); //holds text objects
    private SparseArray<JSONObject> mObjectPointers = new SparseArray<JSONObject>(); //for drag events


    /*****************************************************/
    //clear screen action flag
    public static final int CLEAR_SCREEN = (int)Double.NEGATIVE_INFINITY;

    private boolean isCircle = false;

    public DrawingView( Context ctx, AttributeSet attrs){
        super (ctx, attrs);
        setupDrawing();

    }

    //initializes canvas and drawing classes
    private void setupDrawing() {
        //get drawing area setup for interaction
        drawPath = new Path();
        drawPathRecv = new Path();
        drawPaint = new Paint();
        drawPaint.setDither(true);
        drawPaint.setPathEffect(new CornerPathEffect(10));
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(strokeWidth);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        canvasPaint = new Paint(Paint.DITHER_FLAG);

        drawPaintSender = new Paint();
        drawPaintSender.setDither(true);
        drawPaintSender.setPathEffect(new CornerPathEffect(10));
        drawPaintSender.setColor(paintColor);
        drawPaintSender.setAntiAlias(true);
        drawPaintSender.setStrokeWidth(strokeWidth);
        drawPaintSender.setStyle(Paint.Style.STROKE);
        drawPaintSender.setStrokeJoin(Paint.Join.ROUND);
        drawPaintSender.setStrokeCap(Paint.Cap.ROUND);
        client = MQTTHandler.getInstance().getClientHandle();
    }


    //sets system color and pencil width to default values
    private void reset() {
        drawPaint.setColor(paintColor);
        drawPaint.setStrokeWidth(strokeWidth);
    }

    //sets Paint properties for points being received
    private void setDrawPaintRecv(int color, String mode, int brushSize) {
        drawPaintSender.setColor(color);
        drawPaintSender.setStrokeWidth(brushSize);
        if (mode.compareTo("erase") == 0) {
            drawPaintSender.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }
        else {
            drawPaintSender.setXfermode(null);
        }
    }

    //sets rectangle mode
    public void rectMode(boolean val) {
        rectMode = val; circleMode = false;lineMode = false; textMode = false; dragMode = false; removeObjectMode = false; colorDropperMode = false;}
    //Sets circle mode
    public void circleMode(boolean val){
        circleMode = val; rectMode = false;lineMode = false; textMode = false;dragMode = false; removeObjectMode = false; colorDropperMode = false;}
    //sets line mode
    public void lineMode(boolean val) {
        lineMode = val; rectMode = false;circleMode = false; textMode = false;dragMode = false; removeObjectMode = false; colorDropperMode = false;}
    //sets text mode
    public void textMode(boolean val, String text) {
        textMode = val; rectMode = false; circleMode = false; lineMode = false; textBoxData = text; dragMode = false; removeObjectMode = false;colorDropperMode = false;}
    //sets drag mode
    public void dragMode(boolean val){
        dragMode = val; rectMode = false; circleMode = false;lineMode = false; textMode = false; removeObjectMode = false; colorDropperMode = false;}
    //sets object delete mode
    public void removeObjectMode(boolean val){
        removeObjectMode = val; circleMode = false; rectMode = false;lineMode = false; textMode = false;dragMode = false; colorDropperMode = false;}
    //sets color dropper mode
    public void colorDropperMode(boolean val, int color) {
        dropperColor = color; colorDropperMode = val;removeObjectMode = false; circleMode = false; rectMode = false;lineMode = false; textMode = false;dragMode = false;
    }
    //sets text resize mode
    public void textSizeMode(boolean val, int size){
        textViewSize = size ; textSizeMode= val;colorDropperMode = false;removeObjectMode = false; circleMode = false; rectMode = false;lineMode = false; textMode = false;dragMode = false;
    }

    //gets current mqttHandler instance
    public void initializeHandler(MQTTHandler mqtt){
        this.mqtt = mqtt;
    }

    //clears view
    public void clearView() {
        recreateCanvas();
        mqtt.publishClearScreen();

        objectDrawables.clear();
        textObjects.clear();
        invalidate();
    }

    //removes object with key key.
    public void removeObject(String key) {
        if (key == null)
            throw new NullPointerException("Invalid key");
        objectDrawables.remove(key);
        textObjects.remove(key);
        invalidate();
    }

    //clear View in response to clear message from another user
    public void updateClearScreen() {
        recreateCanvas();
        objectDrawables.clear();
        invalidate();
    }

    //Changes color locally
    public void changeColor(int color) {
        paintColor = color;
        drawPaint.setColor(color);
    }

    //updates system color on receiving message from broker
    public void updateColor(int color) {
        drawPaint.setColor(color);
    }

    //turns off object related modes
    public void objectModeOff() {
        rectMode = false;
        circleMode = false;
        lineMode = false;
        textMode = false;
        dragMode = false;
        removeObjectMode = false;
        colorDropperMode = false;
        textSizeMode = false;
    }

    //changes the brush size locally
    public void changeBrushSize(int size) {
        mode = "pencil";
        objectModeOff();

        //exit rectangle draw mode
        strokeWidth = size; //setStrokewidth to be sent
        drawPaint.setXfermode(null); //removes erase
        drawPaint.setStrokeWidth(size);
    }

    //changes the brush size in response to message.
    public void updateBrushSize(float size) {
        mode = "pencil";
        objectModeOff();

        drawPaint.setXfermode(null);
        drawPaint.setStrokeWidth(size);
    }

    //changes to erase mode locally
    public void changeEraseSize(float size) {
        mode = "erase";
        strokeWidth = (int)size;
        objectModeOff();
        drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        drawPaint.setStrokeWidth(size);
        // mqtt.publishBrushSize(size, "Eraser");
    }

    //updates mode to erase and sets size of eraser to size
    public void updateEraseSize(float size) {
        mode = "erase";
        objectModeOff();
        drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        drawPaint.setStrokeWidth(size);
    }

    //recreates the canvas
    public void recreateCanvas() {
        canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }



    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //view given size
        width = w;
        height = h;
        super.onSizeChanged(w,h,oldw,oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        drawCanvas.drawPath(drawPath, drawPaint);
        drawCanvas.drawPath(drawPathRecv, drawPaintSender);

        if (rectMode) {
            //draw rectangle
            drawPaint.setXfermode(null);
            onDrawRectangle(canvas);
        }
        else if (circleMode) {
            drawPaint.setXfermode(null);
            onDrawCircle(canvas);
        }
        else if (lineMode) {
            drawPaint.setXfermode(null);
            onDrawLine(canvas);
        }
        else if (textMode) {
            drawPaint.setXfermode(null);
            onDrawText(canvas);
        } else if (dragMode && !dragFinished) {
            drawPaint.setXfermode(null);
            onDragDraw(canvas, dragX, dragY);
        } else if (colorDropperMode ) {
            drawPaint.setXfermode(null);
            onDrawColorDropper(canvas, dropperX, dropperY);
        } else if (textSizeMode) {
            drawPaint.setXfermode(null);
        }

        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);

        //redraw objects

        Paint tempPaint = new Paint();
        tempPaint.setStyle(Paint.Style.STROKE);

        for (String key : textObjects.keySet()) {
            if (key.compareTo("")==0) {
                continue;
            }
            JSONObject o = textObjects.get(key);

            tempPaint.setColor(o.optInt("color"));
            tempPaint.setStrokeWidth(o.optInt("size"));

            canvas.drawBitmap(mqtt.stringToBitmap(o.optString("textBitmap")), o.optInt("x"), o.optInt("y"), tempPaint);

        }

        for (String key: objectDrawables.keySet()){
            //hashtable problems no time to explain. But it creates a duplicate of last item I add to the table.
            //So dont print duplicates which have empty string key values
            if (key.compareTo("")==0) {
                continue;
            }
            JSONObject o = objectDrawables.get(key);

            String objectType = o.optString("type");
            tempPaint.setColor(o.optInt("color"));
            tempPaint.setStrokeWidth(o.optInt("size"));


            if(objectType.compareTo("Circle") == 0) {
                canvas.drawCircle(o.optInt("x"), o.optInt("y"), o.optInt("radius"), tempPaint);

            } else if (objectType.compareTo("Line") == 0) {
                canvas.drawLine(o.optInt("startx"), o.optInt("starty"), o.optInt("stopx"), o.optInt("stopy"), tempPaint);

            } else if (objectType.compareTo("Rectangle") == 0) {
                canvas.drawRect(Rect.unflattenFromString(o.optString("dimens")), tempPaint);
            } else if (objectType.compareTo("Text") == 0) {
                //canvas.drawRect(Rect.unflattenFromString(o.optString("region")), drawPaint);
                canvas.drawBitmap(mqtt.stringToBitmap(o.optString("textBitmap")), o.optInt("x"), o.optInt("y"), tempPaint);


            }
        }

    }




    //draws points based on messages received
    public boolean drawPoint(float x,float y, int action, int color, String mode, int brushSize, String clientID) {

        mX = x;
        mY = y;


        Path path = clientPaths.get(clientID);
        if (path == null) {
            path = new Path();
            clientPaths.put(clientID, path);
        }
        drawPathRecv = path;

        setDrawPaintRecv(color, mode, brushSize);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                drawPathRecv.moveTo(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPathRecv.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(drawPathRecv, drawPaintSender);
                drawPathRecv.reset();
                break;
            case CLEAR_SCREEN:
                drawCanvas.drawColor(Color.WHITE);
                break;
            default:
                //draw nothing
        }

        invalidate();
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //detect user touch
        float touchX = event.getX();
        float touchY = event.getY();
        //System.out.println("X"+touchX);
        // System.out.println("Y"+touchY);


        if (rectMode) {
            onTouchRectangleMode(event);
            return true;
        } else if (circleMode) {
            onTouchCircleMode(event);
            return true;
        } else if (lineMode) {
            onTouchLineMode(event);
            return true;
        } else if(textMode) {
            onTouchTextMode(event);
            return true;
        } else if (dragMode) {
            onTouchDragEvent(event);
            return true;
        } else if (removeObjectMode) {
            onRemoveObjectEvent(event);
            return true;
        } else if (colorDropperMode) {
            onTouchColorDropperMode(event);
            return true;
        } else if (textSizeMode){
            onTouchTextResizeEvent(event);
            return true;
        }


        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();
                break;
            default:
                return false;
        }
        //System.out.println(mqtt);

        invalidate();
        mqtt.publish(touchX, touchY, event.getAction(), paintColor, mode, strokeWidth);

        return true;
    }


    /********************************************************************************/

    //draws rectangle received from other clients
    public void onDrawReceivedRectangle(JSONObject rectangle) {
        if (rectangle == null)
            throw new NullPointerException("Invalid rectangle object");
        objectDrawables.put(rectangle.optString("id"), rectangle);
        invalidate();
    }

    //draws rectangle object on Canvas canvas
    public void onDrawRectangle(Canvas canvas) {
        Paint paint = drawPaint;
        if(points[3]==null) //point4 null when user did not touch and move on screen.
            return;
        int left, top, right, bottom;
        left = points[0].x;
        top = points[0].y;
        right = points[0].x;
        bottom = points[0].y;
        for (int i = 1; i < points.length; i++) {
            left = left > points[i].x ? points[i].x:left;
            top = top > points[i].y ? points[i].y:top;
            right = right < points[i].x ? points[i].x:right;
            bottom = bottom < points[i].y ? points[i].y:bottom;
        }

        //draw stroke
        paint.setStyle(Paint.Style.STROKE);
        // paint.setColor(Color.parseColor("#AADB1255"));
        paint.setStrokeWidth(5);

        if (finished) {

            Rect rect = new Rect(left + colorballs.get(0).getWidthOfBall() / 2,
                    top + colorballs.get(0).getWidthOfBall() / 2,
                    right + colorballs.get(2).getWidthOfBall() / 2,
                    bottom + colorballs.get(2).getWidthOfBall() / 2);

            JSONObject objectProperties = new JSONObject();
            String key = UUID.randomUUID().toString();

            try {
                objectProperties.put("type", "Rectangle");
                objectProperties.put("clientId", client.getClientId());
                objectProperties.put("id", key);
                objectProperties.put("color", drawPaint.getColor());
                objectProperties.put("size", 5);
                objectProperties.put("dimens", rect.flattenToString());

            } catch (JSONException e) {

            }

            objectDrawables.put(key, objectProperties);

            mqtt.publishRectangle(objectProperties);
            //reset to start drawing again
            points = new Point[4];
            colorballs.clear();
            return;
        }

        //temporary canvas drawing on resize mode
        canvas.drawRect(
                left + colorballs.get(0).getWidthOfBall() / 2,
                top + colorballs.get(0).getWidthOfBall() / 2,
                right + colorballs.get(2).getWidthOfBall() / 2,
                bottom + colorballs.get(2).getWidthOfBall() / 2, paint);


        //draw the corners
        BitmapDrawable bitmap = new BitmapDrawable();
        // draw the balls on the canvas
        paint.setTextSize(18);
        paint.setStrokeWidth(0);

        for (int i =0; i < colorballs.size(); i ++) {
            ColorBall ball = colorballs.get(i);
            canvas.drawBitmap(ball.getBitmap(), ball.getX(), ball.getY(),
                    paint);
            //  System.out.println("RectMode");

            canvas.drawText("" + (i+1), ball.getX(), ball.getY(), paint);
        }
    }


    //defines onTouchEvent Method for Rect Mode. Draws rectangle handles appropriately
    public void onTouchRectangleMode(MotionEvent event) {
        int eventaction = event.getAction();

        int X = (int) event.getX();
        int Y = (int) event.getY();


        switch (eventaction) {

            case MotionEvent.ACTION_DOWN: // touch down so check if the finger is on
                // a ball
                finished = false;
                if (points[0] == null) {
                    //initialize rectangle.
                    points[0] = new Point();
                    points[0].x = X;
                    points[0].y = Y;

                    points[1] = new Point();
                    points[1].x = X;
                    points[1].y = Y + 30;

                    points[2] = new Point();
                    points[2].x = X + 30;
                    points[2].y = Y + 30;

                    points[3] = new Point();
                    points[3].x = X +30;
                    points[3].y = Y;

                    balID = 2;
                    groupId = 1;
                    // declare each ball with the ColorBall class
                    for (Point pt : points) {
                        colorballs.add(new ColorBall(getContext(), R.drawable.dot_drag_handle, pt));
                    }
                } else {
                    //resize rectangle
                    balID = -1;
                    groupId = -1;
                    for (int i = colorballs.size()-1; i>=0; i--) {
                        ColorBall ball = colorballs.get(i);
                        // check if inside the bounds of the ball (circle)
                        // get the center for the ball
                        int centerX = ball.getX() + ball.getWidthOfBall();
                        int centerY = ball.getY() + ball.getHeightOfBall();

                        // calculate the radius from the touch to the center of the
                        // ball
                        double radCircle = Math
                                .sqrt((double) (((centerX - X) * (centerX - X)) + (centerY - Y)
                                        * (centerY - Y)));

                        if (radCircle < ball.getWidthOfBall()) {

                            balID = ball.getID();
                            if (balID == 1 || balID == 3) {
                                groupId = 2;
                            } else {
                                groupId = 1;
                            }
                            invalidate();
                            break;
                        }
                        invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE: // touch drag with the ball


                if (balID > -1) {
                    // move the balls the same as the finger
                    colorballs.get(balID).setX(X);
                    colorballs.get(balID).setY(Y);


                    if (groupId == 1) {
                        colorballs.get(1).setX(colorballs.get(0).getX());
                        colorballs.get(1).setY(colorballs.get(2).getY());
                        colorballs.get(3).setX(colorballs.get(2).getX());
                        colorballs.get(3).setY(colorballs.get(0).getY());
                    } else {
                        colorballs.get(0).setX(colorballs.get(1).getX());
                        colorballs.get(0).setY(colorballs.get(3).getY());
                        colorballs.get(2).setX(colorballs.get(3).getX());
                        colorballs.get(2).setY(colorballs.get(1).getY());
                    }

                    invalidate();
                }

                break;

            case MotionEvent.ACTION_UP:
                // touch drop - just do things here after dropping
                finished = true;

                break;
        }
        // redraw the canvas
        invalidate();
        return ;

    }

    /*****************************************************************/
    //draws circle objects received from the broker
    public void onDrawReceivedCircle(JSONObject objectReceived) {
        if (objectReceived == null)
            throw new NullPointerException("Circle object invalid");
        objectDrawables.put(objectReceived.optString("id"), objectReceived);
        invalidate();
    }
    //draws circle objects on canvas
    public void onDrawCircle(Canvas canvas) {
        Paint paint = drawPaint;
        if(points[3]==null) //point4 null when user did not touch and move on screen.
            return;
        int left, top, right, bottom;
        left = points[0].x;
        top = points[0].y;
        right = points[0].x;
        bottom = points[0].y;
        for (int i = 1; i < points.length; i++) {
            left = left > points[i].x ? points[i].x:left;
            top = top > points[i].y ? points[i].y:top;
            right = right < points[i].x ? points[i].x:right;
            bottom = bottom < points[i].y ? points[i].y:bottom;
        }

        float cx = (left+right)/2f;
        float cy = (top+bottom)/2f;
        float radius = (float)Math.hypot(top - bottom, right - left)/2f;

        //draw stroke
        paint.setStyle(Paint.Style.STROKE);
        // paint.setColor(Color.parseColor("#AADB1255"));
        paint.setStrokeWidth(5);

        if (finished) {

            JSONObject objectProperties = new JSONObject();
            String key = UUID.randomUUID().toString();

            try {
                objectProperties.put("id", key);
                objectProperties.put("type", "Circle");
                objectProperties.put("color", drawPaint.getColor());
                objectProperties.put("size", 5);
                objectProperties.put("clientId", client.getClientId());
                objectProperties.put("radius", radius);
                objectProperties.put("x", cx);
                objectProperties.put("y", cy);

            } catch (JSONException e) {

            }



            objectDrawables.put(key, objectProperties);

            //  drawCanvas.drawCircle(cx, cy, radius, paint);

            //   drawCanvas.save();
            //  mqtt.publishRectangle(objectProperties);
            mqtt.publishCircle(objectProperties);

            //reset to start drawing again
            points = new Point[4];
            colorballs.clear();
            return;


        }

        canvas.drawCircle(cx, cy, radius, paint);
        // draw the balls on the canvas
        //  paint.setColor(Color.BLUE);
        paint.setTextSize(18);
        paint.setStrokeWidth(0);
        // paint.setColor(Color.BLUE);
        for (int i =0; i < colorballs.size(); i ++) {
            ColorBall ball = colorballs.get(i);
            canvas.drawBitmap(ball.getBitmap(), ball.getX(), ball.getY(),
                    paint);

            canvas.drawText("" + (i+1), ball.getX(), ball.getY(), paint);
        }
    }

    //handles onTouchEvents in circle Mode
    public void onTouchCircleMode(MotionEvent event) {
        int eventaction = event.getAction();

        int X = (int) event.getX();
        int Y = (int) event.getY();

        switch (eventaction) {

            case MotionEvent.ACTION_DOWN: // touch down so check if the finger is on
                // a ball
                finished = false;
                if (points[0] == null) {
                    //initialize rectangle.
                    points[0] = new Point();
                    points[0].x = X;
                    points[0].y = Y;

                    points[1] = new Point();
                    points[1].x = X;
                    points[1].y = Y + 30;

                    points[2] = new Point();
                    points[2].x = X + 30;
                    points[2].y = Y + 30;

                    points[3] = new Point();
                    points[3].x = X +30;
                    points[3].y = Y;

                    balID = 2;
                    groupId = 1;
                    // declare each ball with the ColorBall class
                    for (Point pt : points) {
                        colorballs.add(new ColorBall(getContext(), R.drawable.dot_drag_handle, pt));
                    }
                }
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE: // touch drag with the ball


                if (balID > -1) {
                    // move the balls the same as the finger
                    colorballs.get(balID).setX(X);
                    colorballs.get(balID).setY(Y);


                    if (groupId == 1) {
                        colorballs.get(1).setX(colorballs.get(0).getX());
                        colorballs.get(1).setY(colorballs.get(2).getY());
                        colorballs.get(3).setX(colorballs.get(2).getX());
                        colorballs.get(3).setY(colorballs.get(0).getY());
                    } else {
                        colorballs.get(0).setX(colorballs.get(1).getX());
                        colorballs.get(0).setY(colorballs.get(3).getY());
                        colorballs.get(2).setX(colorballs.get(3).getX());
                        colorballs.get(2).setY(colorballs.get(1).getY());
                    }

                    invalidate();
                }

                break;

            case MotionEvent.ACTION_UP:
                // touch drop - just do things here after dropping
                finished = true;

                break;
        }
        // redraw the canvas
        invalidate();
        return ;

    }

    public int startX = 0;//current  line starting point
    public int startY = 0;//current line ending point

    StraightLinePath linePath = new StraightLinePath(0,0);

    private static class StraightLinePath {
        private int startXX; //start position X
        private int startYY;//start position y

        private int stopYY; //stop position y
        private int stopXX; //stop position x

        public StraightLinePath(int mStartX, int mStartY) {
            startXX = mStartX;
            startYY = mStartY;

        }

        //defines start position
        public void from(int x, int y) {
            startXX = x;
            startYY = y;
        }

        //defines ending point
        public void to(int stopX, int stopY) {
            stopYY = stopY;
            stopXX = stopX;
        }

        //calculates gradient
        public double getGradient() {
            double dy = this.stopYY - this.startYY;
            double dx = this.stopXX - this.startXX;
            System.out.println("Gradient of line: " + Math.round(dx/dy*100.0)/100.0);

            double slopeAngle = Math.toDegrees(Math.tan(Math.round((dx/dy)*100.0)/100.0));

            System.out.println("Degrees:" + slopeAngle);

            return  Math.round(dx/dy*100.0)/100.0;
        }
        //calculates length of line
        public double length() {
            double dxSquared = (this.startXX-this.stopXX)*(this.startXX-this.stopXX);
            double dySquared = (this.startYY-this.stopYY)*(this.startYY-this.stopYY);
            double sum = dxSquared+dySquared;
            return Math.abs(Math.sqrt(sum));
        }
        public int startx() {return startXX;}
        public int starty() {return startYY;}
        public int stopy() {return stopYY;}
        public int stopx() {return stopXX;}

    }


    /***********************************************************************************/
    //draws line objects received from the broker
    public void onDrawReceivedLine(JSONObject objectReceived) {
        if (objectReceived == null)
            throw new NullPointerException("Invalid Line Object");
        objectDrawables.put(objectReceived.optString("id"), objectReceived);
        invalidate();
    }

    //draws line objects on canvas
    public void onDrawLine(Canvas canvas) {
        Paint paint = drawPaint;
        if(points[1]==null) //point4 null when user did not touch and move on screen.
            return;

        //draw stroke
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        if (finished) {

            JSONObject objectProperties = new JSONObject();
            String key = UUID.randomUUID().toString();

            try {
                objectProperties.put("id", key);
                objectProperties.put("clientId", client.getClientId());
                objectProperties.put("type", "Line");
                objectProperties.put("color", drawPaint.getColor());
                objectProperties.put("size", 5);
                objectProperties.put("startx", linePath.startx());
                objectProperties.put("starty", linePath.starty() );
                objectProperties.put("stopx", linePath.stopx() );
                objectProperties.put("stopy", linePath.stopy());
                objectProperties.put("gradient", linePath.getGradient());
                objectProperties.put("length", linePath.length());

            } catch (JSONException e) {
                e.printStackTrace();
            }

            objectDrawables.put(key, objectProperties);

            mqtt.publishLine(objectProperties);

            //reset to start drawing again
            points = new Point[4];
            colorballs.clear();
            return;


        }

        canvas.drawLine(linePath.startx(),linePath.starty(), linePath.stopx(), linePath.stopy(), paint);


        // draw the balls on the canvas
        paint.setTextSize(18);
        paint.setStrokeWidth(0);
        for (int i =0; i < colorballs.size(); i ++) {
            ColorBall ball = colorballs.get(i);
            canvas.drawBitmap(ball.getBitmap(), ball.getX(), ball.getY(),
                    paint);
            canvas.drawText("" + (i+1), ball.getX(), ball.getY(), paint);
        }
    }
    //handles touch events on line mode
    public void onTouchLineMode(MotionEvent event) {
        int eventaction = event.getAction();

        int X = (int) event.getX();
        int Y = (int) event.getY();

        switch (eventaction) {

            case MotionEvent.ACTION_DOWN: // touch down so check if the finger is on
                // a ball

                points = new Point[2];
                startX = X;
                startY = Y;
                linePath.from(X, Y);
                linePath.to(X, Y);

                finished = false;
                if (points[0] == null) {
                    //initialize rectangle.
                    points[0] = new Point();
                    points[0].x = X;
                    points[0].y = Y;

                    points[1] = new Point();
                    points[1].x = X;
                    points[1].y = Y + 30;

                    for (Point pt : points) {
                        colorballs.add(new ColorBall(getContext(), R.drawable.dot_drag_handle, pt));
                    }

                    invalidate();
                }
                break;
            case MotionEvent.ACTION_MOVE: // touch drag with the ball
                linePath.to(X,Y);
                colorballs.get(1).setX(X);
                colorballs.get(1).setY(Y);
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                // touch drop - just do things here after dropping
                linePath.to(X,Y);
                colorballs.get(1).setX(X);
                colorballs.get(1).setY(Y);
                finished = true;

                break;
        }
        // redraw the canvas
        invalidate();
        return ;

    }
    /****************************************************************************************/

    /****************************************************************************************/
    private float  textPosX = 500;  //default text position
    private float textPosY = 500;   //default textPositon
    // private ImageView imageView = new ImageView(getContext());

    //draws text object received from broker
    public void onDrawReceivedText(JSONObject objectReceived) {
        if (objectReceived == null)
            throw new NullPointerException("Invalid text field object");
        // objectDrawables.put(objectReceived.optString("id"), objectReceived);
        textObjects.put(objectReceived.optString("id"), objectReceived);
        // Toast.makeText(getContext(), "text Object received",Toast.LENGTH_SHORT );
        invalidate();
    }

    //converts text to Bitmap
    public Bitmap textToBitmap(String text, int color, float posX, float posY, int size) {
        TextView textView = new TextView(getContext());
        textView.setVisibility(View.VISIBLE);
        textView.setTextColor(color);
        textView.setMaxWidth(500);
        textView.setMaxHeight(500);
        textView.setMaxLines(4);
        textView.setX(posX);
        textView.setY(posY);
        textView.setText(text);
        textView.setTextSize(size);



        LinearLayout layout = new LinearLayout(getContext());
        layout.addView(textView);
        layout.measure(500, 500);
        layout.layout(0, 0, 500, 500);



        textView.setDrawingCacheEnabled(true);
        textView.buildDrawingCache();
        Bitmap bm = textView.getDrawingCache();
        return bm;
    }

    //draws text objects on canvas
    public void onDrawText(Canvas canvas) {

        if (finished) {

            Bitmap bm = textToBitmap(textBoxData, drawPaint.getColor(), textPosX, textPosY, textViewSize);

            // create a rectangle to define the boundary

            Rect region = new Rect((int)textPosX-5, (int)textPosY-5, (int)textPosX+bm.getWidth()+5, (int)textPosY+bm.getHeight()+5);


            JSONObject textJson = new JSONObject();
            try {

                String key = UUID.randomUUID().toString();
                textJson.put("type", "Text");
                textJson.put("id", key);
                textJson.put("clientId", client.getClientId());
                textJson.put("x", textPosX);
                textJson.put("y", textPosY);
                textJson.put("region", region.flattenToString());
                textJson.put("color", drawPaint.getColor());
                textJson.put("size", textViewSize);
                textJson.put("textBitmap", mqtt.bitmapToString(bm));
                textJson.put("text", textBoxData);
                // objectDrawables.put(key, textJson);
                textObjects.put(key, textJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mqtt.publishtext(textJson);

            finished = false;
            placed = true;

        }

    }

    public static boolean placed = false; //checks if text is already placed on the view
    public boolean isPlaced() {
        return placed;
    }

    //handles touch events in text mode
    public void onTouchTextMode(MotionEvent event) {
        int eventaction = event.getAction();

        int X = (int) event.getX();
        int Y = (int) event.getY();
        textPosX = X;
        textPosY = Y;

        if (isPlaced()) {
            return;
        }
        finished = true;

        // redraw the canvas
        invalidate();

        return ;

    }
    /***********************************************************************************/
    //handles color dropper mode touch events
    public void onTouchColorDropperMode(MotionEvent event) {

        int X = (int) event.getX();
        int Y = (int) event.getY();
        dropperX = X;
        dropperY = Y;


        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            JSONObject touchedObject = getTouchedObject(X,Y);
            if (touchedObject != null) {

                try {
                    touchedObject.put("color", dropperColor);
                    //for text
                    if (touchedObject.optString("type").compareTo("Text") == 0) {
                        Bitmap bm =  textToBitmap(touchedObject.optString("text"), dropperColor, touchedObject.optInt("x"), touchedObject.optInt("y"), touchedObject.optInt("size"));
                        touchedObject.put("textBitmap", mqtt.bitmapToString(bm));
                    }
                    mqtt.publishObject(touchedObject);
                } catch (JSONException e) {

                }
                invalidate();
            }
        }

    }

    //draws a paint can on the screen in color dropper mode
    public void onDrawColorDropper(Canvas canvas, int dropX, int dropY) {
        Bitmap dropperImage = BitmapFactory.decodeResource(getResources(), R.drawable.drop_color_iconpng);
        canvas.drawBitmap(dropperImage, dropX, dropY, drawPaint);
    }

    //handles touch events for text resize
    public void onTouchTextResizeEvent(MotionEvent event) {
        int eventaction = event.getAction();

        int X = (int) event.getX();
        int Y = (int) event.getY();
        textResizePosX = X;
        textResizePosY = Y;


        JSONObject touchedObject = getTouchedObject(X,Y);
        if (touchedObject != null) {

            try {

                //for text
                if (touchedObject.optString("type").compareTo("Text") == 0) {
                    touchedObject.put("size", textViewSize);
                    Bitmap bm = textToBitmap(touchedObject.optString("text"), touchedObject.optInt("color"), touchedObject.optInt("x"), touchedObject.optInt("y"), textViewSize);
                    touchedObject.put("textBitmap", mqtt.bitmapToString(bm));
                }
                mqtt.publishObject(touchedObject);
            } catch (JSONException e) {

            }
            invalidate();
        }
    }

    /***********************************************DRAG DRAG DRAG!!!!!!!!!!!!!!!!***************/
    /********************************************************************************************/
    private static class CircleRegion {
        public int radius;
        public int centerX;
        public int centerY;

        public CircleRegion(int centerX, int centerY, int radius) {
            this.radius = radius;
            this.centerX = centerX;
            this.centerY = centerY;
        }

        @Override
        public String toString() {
            return "Circle[" + centerX + ", " + centerY + ", " + radius + "]";
        }
    }


    /**********************************************************************************************/

    Bitmap dragImage = BitmapFactory.decodeResource(getResources(), R.drawable.pointer); //hand drag image
    int dragX; //drag position X
    int dragY; //drag position Y
    boolean dragFinished = false; //is drag finished?

    //draws hand tool as you drag on screen
    public void onDragDraw(Canvas canvas, int x, int y) {

        canvas.drawBitmap(dragImage, x, y-20, drawPaint);

    }

    boolean dragLineStart = false; //marked if the point touched on line is closer to start of line than end of line
    //handles drag event for objects on canvass
    public boolean onTouchDragEvent(MotionEvent event) {

        boolean handled  = false;

        JSONObject mObjectTouched;
        int X;
        int Y;
        int pointerId;
        int actionIndex = event.getActionIndex();

        //get coordinates and make object transparent
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragFinished = false;

                //it's the first pointer, so clear all existing pointers data
                mObjectPointers.clear();
                X = (int) event.getX(actionIndex);
                Y = (int) event.getY(actionIndex);
                dragX = X;
                dragY = Y;
                String objectType = "";
                //check if we've touched inside some object
                mObjectTouched = getTouchedObject(X, Y);

                if (mObjectTouched == null) {
                    return true;
                } else {
                    objectType = getObjectType(mObjectTouched);
                }

                if (objectType.compareTo("Circle") == 0) {
                    try {
                        mObjectTouched.put("x", X);
                        mObjectTouched.put("y", Y);

                    } catch (JSONException e) {
                        e.printStackTrace();

                    }
                } else if (objectType.compareTo("Rectangle") == 0) {
                    Rect tempRect = Rect.unflattenFromString(mObjectTouched.optString("dimens"));
                    tempRect.set(X, Y, X+tempRect.width(), Y+tempRect.height());
                    try {
                        mObjectTouched.put("dimens", tempRect.flattenToString());
                    }catch (JSONException e) {

                    }

                } else if (objectType.compareTo("Line") == 0) {

                    if (lengthOfLine(X, Y, mObjectTouched.optInt("startx"), mObjectTouched.optInt("starty"))<lengthOfLine(X, Y, mObjectTouched.optInt("stopx"), mObjectTouched.optInt("stopy"))) {

                        try {
                            mObjectTouched.put("startx", X);
                            mObjectTouched.put("starty", Y);
                            mObjectTouched.put("length", lengthOfLine(X, Y, mObjectTouched.optInt("stopx"), mObjectTouched.optInt("stopy")));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        dragLineStart = true;
                    } else {
                        try {
                            mObjectTouched.put("stopx", X);
                            mObjectTouched.put("stopy", Y);
                            mObjectTouched.put("length", lengthOfLine(X, Y, mObjectTouched.optInt("startx"), mObjectTouched.optInt("starty")));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        dragLineStart = false;
                    }


                } else if (objectType.compareTo("Text") == 0) {
                    try {
                        mObjectTouched.put("x", X);
                        mObjectTouched.put("y", Y);
                        Rect tempRect = Rect.unflattenFromString(mObjectTouched.getString("region"));
                        tempRect.set(X, Y, tempRect.width()+X,tempRect.height()+Y);
                        mObjectTouched.put("region", tempRect.flattenToString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                mObjectPointers.put(event.getPointerId(0), mObjectTouched);
                if (mObjectTouched != null) {
                    mqtt.publishObject(mObjectTouched);
                }
                invalidate();
                handled = true;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                dragFinished = false;

                final int pointerCount = event.getPointerCount();

                for (actionIndex = 0; actionIndex < pointerCount; actionIndex++) {
                    //some pointer has moved, search it by pointer id
                    pointerId = event.getPointerId(actionIndex);
                    X = (int) event.getX(actionIndex);
                    Y = (int) event.getY(actionIndex);

                    dragX = X;
                    dragY = Y;


                    mObjectTouched = mObjectPointers.get(pointerId);
                    if(mObjectTouched == null) continue; // if null no object was touched so skip

                    if (mObjectTouched.optString("type").compareTo("Circle") == 0) {
                        try {
                            mObjectTouched.put("x", X);
                            mObjectTouched.put("y", Y);
                        } catch (JSONException e) {
                        }
                    } else if (mObjectTouched.optString("type").compareTo("Rectangle") == 0) {

                        Rect tempRect = Rect.unflattenFromString(mObjectTouched.optString("dimens"));
                        tempRect.set(X, Y, X+tempRect.width(), Y+tempRect.height());
                        try {
                            mObjectTouched.put("dimens", tempRect.flattenToString());
                        }catch (JSONException e) {

                        }

                    } else if (mObjectTouched.optString("type").compareTo("Text") == 0) {
                        try {
                            mObjectTouched.put("x", X);
                            mObjectTouched.put("y", Y);
                            Rect tempRect = Rect.unflattenFromString(mObjectTouched.getString("region"));
                            tempRect.set(X, Y, tempRect.width()+X,tempRect.height()+Y);
                            mObjectTouched.put("region", tempRect.flattenToString());
                        } catch (JSONException e) {

                            e.printStackTrace();
                        }
                    }

                    else if (mObjectTouched.optString("type").compareTo("Line") == 0) {

                        if (dragLineStart) {
                            try {
                                mObjectTouched.put("startx", X);
                                mObjectTouched.put("starty", Y);
                                mObjectTouched.put("length", lengthOfLine(X, Y, mObjectTouched.optInt("stopx"), mObjectTouched.optInt("stopy")));

                                //mObjectTouched.put("stopx", tempStopX);
                                // mObjectTouched.put("stopy", tempStopY);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                mObjectTouched.put("stopx", X);
                                mObjectTouched.put("stopy", Y);
                                mObjectTouched.put("length", lengthOfLine(X, Y, mObjectTouched.optInt("startx"), mObjectTouched.optInt("starty")));

                                //mObjectTouched.put("stopx", tempStopX);
                                // mObjectTouched.put("stopy", tempStopY);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }

                    }

                    if (mObjectTouched != null) {
                        mqtt.publishObject(mObjectTouched);
                    }
                }

                invalidate();
                handled = true;
                break;
            case MotionEvent.ACTION_UP:
                dragFinished = true;

                mObjectPointers.clear();
                invalidate();
                handled = true;
                break;
            case MotionEvent.ACTION_CANCEL:
                handled = true;
                break;

            default:
                // do nothing
                break;
        }

        return super.onTouchEvent(event)||handled;
    }

    //gets the object touched on the canvas
    private JSONObject getTouchedObject(final int xTouch, final int yTouch) {
        JSONObject touched = null;

        //makes textObjects preferable on selection for drag
        //search for text object first
        for (JSONObject textObject: textObjects.values()) {
            Rect region = Rect.unflattenFromString(textObject.optString("region"));
            //System.out.println(region);
            if (region.contains(xTouch, yTouch)) {

                touched = textObject;
                return touched;
            }
        }

        for ( JSONObject object: objectDrawables.values()) {

            if (object.optString("type").compareTo("Circle") == 0) {
                int x = object.optInt("x");
                int y = object.optInt("y");
                int radius = object.optInt("radius");

                if ((x - xTouch) * (x - xTouch) + (y - yTouch) * (y - yTouch) <= radius * radius) {
                    touched = object;
                    break;
                }
            } else if (object.optString("type").compareTo("Rectangle") == 0) {
                Rect tempRect = Rect.unflattenFromString(object.optString("dimens"));
                if (tempRect.contains(xTouch, yTouch)) {
                    touched = object;
                    break;
                }
            } else if (object.optString("type").compareTo("Line") == 0) {

                double tempGradient = gradient(object.optInt("startx"), object.optInt("starty"), xTouch, yTouch);
                int startx = object.optInt("startx");
                int stopx = object.optInt("stopx");
                int starty = object.optInt("starty");
                int stopy = object.optInt("stopy");

                if (lengthOfLine(startx,starty, xTouch, yTouch) + lengthOfLine(stopx, stopy, xTouch, yTouch) <= object.optDouble("length")+5) {
                    touched = object;
                    break;
                }

            } else if (object.optString("type").compareTo("Text") == 0) {

                Rect region = Rect.unflattenFromString(object.optString("region"));
                //System.out.println(region);
                if (region.contains(xTouch, yTouch)) {

                    touched = object;
                    break;
                }
            }
        }

        return touched;
    }

    //returns gradient of a line defined by the parameters
    public double gradient(int startX, int startY, int stopX, int stopY) {

        double dy = stopY - startY;
        double dx = stopX - startX;
        System.out.println("Gradient of temp line: " + Math.round((dx/dy)*100.0)/100.0);
        double slopeAngle = Math.toDegrees(Math.tan(Math.round((dx/dy)*100.0)/100.0));

        System.out.println("Degrees:" + slopeAngle);
        return  Math.round((dx/dy)*100.0)/100.0;

    }

    //returns length of line defined by the parameters
    public double lengthOfLine(int startX, int startY, int stopX, int stopY) {
        double dy = stopY - startY;
        double dx = stopX - startX;
        return Math.sqrt((dy*dy)+(dx*dx));
    }

    //returns type of object o
    private String getObjectType(JSONObject o) {
        return o.optString("type");
    }

    /**************************************DRAG DRAG DRAG!!!!!!!!!!!!!!!!***************************/

    /***********************************REMOVE OBJECT************************************/

    //removes an object touched from the canvas
    public void onRemoveObjectEvent(MotionEvent event) {
        int xRemove = (int) event.getX();
        int yRemove = (int) event.getY();

        JSONObject touchedObject = getTouchedObject(xRemove, yRemove);

        if (touchedObject != null) {
            removeObject(touchedObject.optString("id"));
            mqtt.removeObjectFromServer(touchedObject.optString("id"));
        }

    }
    //This class defines resize handles (blue balls seen on the canvas) for an object
    public static class ColorBall {

        Bitmap bitmap;
        Context mContext;
        Point point;
        int id;
        static int count = 0;

        public ColorBall(Context context, int resourceId, Point point) {
            this.id = count++;
            bitmap = BitmapFactory.decodeResource(context.getResources(),
                    resourceId);
            mContext = context;
            this.point = point;
        }

        public int getWidthOfBall() {
            return bitmap.getWidth();
        }

        public int getHeightOfBall() {
            return bitmap.getHeight();
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public int getX() {
            return point.x;
        }

        public int getY() {
            return point.y;
        }

        public int getID() {
            return id;
        }

        public void setX(int x) {
            point.x = x;
        }

        public void setY(int y) {
            point.y = y;
        }
    }

}
