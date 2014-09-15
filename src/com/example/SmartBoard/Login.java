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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.UUID;



/**
 * Created by Allan Marube on 7/22/2014.
 */
public class Login extends Activity {

    private MQTTHandler handler; //MQTTHandler instance
    private MqttAndroidClient client; //QTT client

    public static String roomId;  //roomId
    public static String nameId; // name concatenated added unique identifier
    public static String nameOnly; //name only
    private boolean isSelfieTaken = false; //marked if selfie is taken


    @Override
    public void onCreate(Bundle savedInstanceState) {
//        if (savedInstanceState != null) {
//            client.unregisterResources();
//        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        this.handler = MQTTHandler.getInstance();

    }


    //returns room and username
    public static String[] getRoomAndName() {
        return new String[] {Login.roomId, Login.nameId};
    }

    //Receives login info and creates a client connection
    public void login(View view) {
        EditText name = (EditText) findViewById(R.id.name);
        EditText room = (EditText) findViewById(R.id.room);

        if (name.getText().toString().compareTo("")==0 || room.getText().toString().compareTo("")==0) {
            Toast.makeText(this, "Please fill both name and room you want to enter", Toast.LENGTH_SHORT).show();
            return;
        }
       client =  handler.getClient(this, "tcp://messagesight.demos.ibm.com:1883", MqttClient.generateClientId());
        roomId = room.getText().toString();
        nameId = name.getText().toString() +"%"+ UUID.randomUUID().toString();
        nameOnly = name.getText().toString();

        if (isSelfieTaken) {

            handler.connect(this);
            Intent intent = new Intent(this, MyActivity.class);
            startActivity(intent);
        }
        else {

            handler.connect(this);
            Bitmap defaultImage = BitmapFactory.decodeResource(getResources(), R.drawable.face);
            Bitmap scaledSelfie = getResizedBitmap(defaultImage);
            handler.myImage = scaledSelfie;
            Intent intent = new Intent(this, MyActivity.class);
            startActivity(intent);
        }

    }

    @Override
    public void onPause() {
        //handler.registerResources(this);
        super.onPause();
    }


    @Override
    public void onDestroy() {

        if (handler.getClientHandle()!=null) {
           handler.unregisterClientResources();
          // handler.disconnect();
        }
        super.onDestroy();
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;
    //Takes a picture using built in camera
    public void dispatchTakePictureIntent(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            Bitmap scaledBitmap = getResizedBitmap(imageBitmap);
            handler.myImage = scaledBitmap;
            isSelfieTaken = true;

        }
    }

    //resizes Selfie to 80by80 scale
    public static Bitmap getResizedBitmap(Bitmap srcBmp) {
        Bitmap dstBmp = null;
        if (srcBmp.getWidth() >= srcBmp.getHeight()) {

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    srcBmp.getWidth()/4 /  - srcBmp.getHeight() / 4,
                    0,
                    srcBmp.getHeight(),
                    srcBmp.getHeight()
            );

        } else {

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.getHeight() / 4 - srcBmp.getWidth() / 4,
                    srcBmp.getWidth(),
                    srcBmp.getWidth()
            );
        }

        return dstBmp.createScaledBitmap(dstBmp, 80, 80, false);
    }


}