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
import android.widget.Toast;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Created by Allan Marube on 7/17/2014.
 * IMqttActionListener that is called when a client connection is successful
 * onSuccess() or when it fails onFailure()
 */
public class StatusListener implements IMqttActionListener {
    // Context context = getApplicationContext();

    private Context ctx; //application context
    private MqttAndroidClient client; //client
    private Toast toast; //notification toast of client connection status
    public static boolean connectFlag = false; //connection status

    public StatusListener(Context ctx, MqttAndroidClient client) {
        this.ctx = ctx;
        this.client = client;
        CharSequence text = "Connection Failure";  //default
        int duration = Toast.LENGTH_SHORT;
        toast = Toast.makeText(ctx, text, duration);

    }

    public void onSuccess(IMqttToken asyncActionToken) {
        toast.setText("Connection successful!");
        toast.show();
        System.out.println("Connection is complete");

        connectFlag = true;
       // MQTTHandler.getInstance().registerResources(ctx);

        //subscribe to topic
        try {
            client.subscribe("smartboard/"+Login.roomId+"/#", 0);
            MQTTHandler.getInstance().publishUserStateOnline();
        } catch (MqttException e) {
            Toast.makeText(ctx,"Error occurred. Try again", Toast.LENGTH_SHORT).show();
        }
    }

    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        System.out.println("Connection not successful bro!");
        toast.setText("Connection failure. Try again!");
        toast.show();
        connectFlag = false;
        exception.printStackTrace();
    }

    private class SubscribeCallback implements IMqttActionListener {

        public void onSuccess(IMqttToken token) {
            Toast toast = Toast.makeText(ctx, "Subscription successful", Toast.LENGTH_SHORT);
            toast.show();
        }

        public void onFailure(IMqttToken token, Throwable cause) {
            Toast toast = Toast.makeText(ctx, "subscription failed", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
