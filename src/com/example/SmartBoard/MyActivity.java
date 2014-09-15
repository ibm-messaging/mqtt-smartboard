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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import android.provider.MediaStore;
import android.support.v4.print.PrintHelper;
import android.text.Html;
import android.view.*;
import android.widget.*;
import org.eclipse.paho.android.service.MqttAndroidClient;

import java.util.UUID;

public class MyActivity extends Activity {

    static final int TEXT_BOX_INPUT = 1;
    private static MqttAndroidClient client = MQTTHandler.getInstance().getClientHandle();

    private String mode = "Pencil Mode"; //default mode

    private ListView listView; //list View for users
    public static DrawingView drawer; //drawingView
    private MQTTHandler handler = MQTTHandler.getInstance(); //MQTTHandler Instance

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        handler.usersAdapter = new UserListArrayAdapter(this,R.layout.online_list_message, handler.usersListHistory);
        listView = (ListView) findViewById(R.id.usersListView);

        getActionBar().setDisplayHomeAsUpEnabled(false); //disables up navigation

        TextView header = new TextView(this);
        header.setBackgroundColor(Color.parseColor("#FFF5EE"));

        header.setText("Designers:");
        header.setTypeface(null, Typeface.BOLD_ITALIC);


        header.setText(Html.fromHtml("<font color ='black'>D</font>" +
                "<font color ='blue'>e</font>" +
                "<font color = 'black'>s</font>" +
                "<font color ='red'>i</font>" +
                "<font color = 'black'>g</font>" +
                "<font color ='magenta'>n</font>" +
                "<font color = 'black'>e</font>" +
                "<font color ='blue'>r</font>" +
                "<font  color = 'black'>s</font>"+
                "<font color = 'black'>:</font>"

        ));


        listView.addHeaderView(header, "Online", false);
        listView.setAdapter(handler.usersAdapter);

        // handler.registerResources(getApplicationContext());
        drawer = (DrawingView) findViewById(R.id.drawing);
        handler.passDrawingContext(this);
    }

    //setsAdapter for the listView
    public void setAdapter(UserListArrayAdapter adapter) {
        listView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        notifyMode(mode);
        //registerReceiver(client);
        //handler.registerResources(this);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    @Override
    public void onDestroy() {
        if (StatusListener.connectFlag) {
            handler.publishUserStateOffline();
            handler.disconnect();
            handler.close();
        }

        //clear client information
        handler.clearUserLog();
        handler.clearChatHistory();
        drawer.objectDrawables.clear();
        drawer.textObjects.clear();

        Bitmap defaultImage  = BitmapFactory.decodeResource(getResources(), R.drawable.face);
        Bitmap scaledSelfie = Login.getResizedBitmap(defaultImage);
        handler.myImage = scaledSelfie;

//        unregisterReceiver(handler.getClientHandle());
        super.onDestroy();
    }

    public void clearScreen() {
        DrawingView drawingView = (DrawingView) findViewById(R.id.drawing);
        drawingView.clearView();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.clear:
                AlertDialog.Builder clearAlert = new AlertDialog.Builder(this);
                clearAlert.setTitle("Clear");
                clearAlert.setMessage("Are you sure you wanna clear everything on the screen?");
                clearAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        //save drawing


                        clearScreen();

                    }
                });
                clearAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                clearAlert.show();


                return true;
            case R.id.deleteObject:
                mode = "Object Delete Mode";
                notifyMode("Object Delete Mode");
                drawer.removeObjectMode(true);
                return true;
            case R.id.blue:
                drawer.changeColor(Color.BLUE);
                return true;
            case R.id.black:
                drawer.changeColor(Color.BLACK);
                return true;
            case R.id.red:
                drawer.changeColor(Color.RED);
                return true;
            case R.id.green:
                drawer.changeColor(Color.parseColor("#228B22"));
                return true;
            case R.id.save:
                AlertDialog.Builder saveAlert = new AlertDialog.Builder(this);
                saveAlert.setTitle("Save");
                saveAlert.setMessage("Save work to device Gallery?");
                saveAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        //save drawing
                        drawer.setDrawingCacheEnabled(true);
                        String imgSaved = MediaStore.Images.Media.insertImage(
                                getContentResolver(), drawer.getDrawingCache(),
                                UUID.randomUUID().toString()+".png", "room ID:"+Login.roomId);
                        if(imgSaved!=null){
                            Toast savedToast = Toast.makeText(getApplicationContext(),
                                    "Drawing saved to Gallery!", Toast.LENGTH_SHORT);
                            savedToast.show();
                        }
                        else{
                            Toast unsavedToast = Toast.makeText(getApplicationContext(),
                                    "Oops! Image could not be saved.", Toast.LENGTH_SHORT);
                            unsavedToast.show();
                        }

                        drawer.destroyDrawingCache();

                    }
                });
                saveAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                });
                saveAlert.show();
                return true;
            case R.id.print:
                drawer.setDrawingCacheEnabled(true);
                doPhotoPrint(drawer.getDrawingCache());
                drawer.destroyDrawingCache();
                return true;

            case R.id.small:
                mode = "Pencil Mode";
                notifyMode("Pencil Mode");
                drawer.changeBrushSize(2);
                return true;
            case R.id.medium:
                mode = "Pencil Mode";
                notifyMode("Pencil Mode");
                drawer.changeBrushSize(5);
                return true;
            case R.id.large:
                mode = "Pencil Mode";
                notifyMode("Pencil Mode");
                drawer.changeBrushSize(10);
                return true;

            case R.id.smallE:
                mode = "Erase Mode";
                notifyMode("Erase Mode");
                drawer.changeEraseSize(30);
                return true;
            case R.id.mediumE:
                mode = "Erase Mode";
                notifyMode("Erase Mode");
                drawer.changeEraseSize(40);
                return true;
            case R.id.largeE:
                mode = "Erase Mode";
                notifyMode("Erase Mode");
                drawer.changeEraseSize(50);
                return true;
            case R.id.circle:
                mode = "Circle Mode";
                drawer.circleMode(true);
                notifyMode("Circle Object Mode");
                return true;
            case R.id.rectangle:
                mode ="Rectangle Object Mode";
                drawer.rectMode(true);
                notifyMode("Rectangle Object Mode");
                return true;
            case R.id.line:
                mode = "Line Object Mode";
                drawer.lineMode(true);
                notifyMode("Line Object Mode");
                return true;
            case R.id.drag:
                mode = "Drag Mode";
                drawer.dragMode(true);
                notifyMode("Drag Mode");
                return true;
            case R.id.text:
                mode = "Text Mode";
                notifyMode("Text Mode");
                DrawingView.placed = false;
                Intent intent2 = new Intent(this, TextBoxAlert.class);
                startActivityForResult(intent2, TEXT_BOX_INPUT);
                return true;
            case R.id.blackDropper:
                mode = "Color Dropper";
                drawer.colorDropperMode(true, Color.BLACK);
                return true;
            case R.id.blueDroppper:
                mode = "Color Dropper";
                drawer.colorDropperMode(true, Color.BLUE);
                return true;
            case R.id.redDropper:
                mode = "Color Dropper";
                drawer.colorDropperMode(true, Color.RED);
                return true;
            case R.id.greenDropper:
                mode = "Color Dropper";
                drawer.colorDropperMode(true, Color.parseColor("#228B22"));
                return true;
            case R.id.smallText:
                mode = "Touch to Resize";
                notifyMode(mode);
                drawer.textSizeMode(true, 10);
                return true;
            case R.id.mediumText:
                mode = "Touch to Resize";
                notifyMode(mode);
                drawer.textSizeMode(true, 15);
                return true;
            case R.id.largeText:
                mode = "Touch to Resize";
                notifyMode(mode);
                drawer.textSizeMode(true, 20);
                return true;
            case R.id.exit:
                finish();
                return true;
            case R.id.chat:
                if (StatusListener.connectFlag) {
                    Intent intent = new Intent(this, Chat.class);
                    startActivity(intent);
                }
                else {
                    Toast.makeText(this, "No active connection. Check your internet connection and enter room", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data ) {

        if (requestCode == TEXT_BOX_INPUT) {
            if (resultCode == RESULT_OK) {
                drawer.textMode(true, data.getStringExtra("InputString"));
            }
        }
    }

    //print Bitmap image
    private void doPhotoPrint(Bitmap bitmap) {
        PrintHelper photoPrinter = new PrintHelper(this);
        photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        photoPrinter.printBitmap("Design print", bitmap);
    }

    //notify users of current drawing mode
    public void notifyMode(String mode) {
       Toast.makeText(this, mode+": on", Toast.LENGTH_SHORT).show();
       // toast.setGravity((Gravity.TOP|Gravity.LEFT), 0, 0);
       // toast.show();
    }

}
