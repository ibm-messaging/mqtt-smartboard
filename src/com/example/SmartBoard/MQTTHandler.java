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

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;

import android.graphics.BitmapFactory;
import android.os.Vibrator;

import android.support.v4.app.NotificationCompat;
import android.util.Base64;

import android.widget.Toast;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;


/**
 * Created by Allan Marube on 7/16/2014.
 */
public class MQTTHandler implements MqttCallback {

    private MqttAndroidClient client; //Mqtt Android Client

    private Context ctx; //application context
    private Context drawingContext; //drawView Context
    public int numMessages; //number of notifications arrived for the chat
    private MessageContainer persistStore; //MQTTPersistence store
    public static ArrayList<ChatMessageWithSelfie> sessionHistory = new ArrayList<ChatMessageWithSelfie>();
    //store users messages
    public static  UserListArrayAdapter usersAdapter; //Array adapter that handles user prescence info
    //online users
    public static final ArrayList<OnlineStateMessage> usersListHistory = new ArrayList<OnlineStateMessage>();
   // public static ChatArrayAdapter chatAdapter = new ChatArrayAdapter(Context, R.layout.activity_chat_singlemessage);
    //current user avatar
    public static Bitmap myImage;
    //thread executor for points published in freedraw mode. Executes these publish calls in a separate thread
    private final ExecutorService mPublishExecutor = newSingleThreadExecutor();

    //type of message received
    public enum type {
        ColorChange, Point, ClearScreen, User, Brush, Chat, Text, Image, Eraser, Pencil, Rectangle, Circle, Line
    }

    //MqttHandler Instance
    private static MQTTHandler ourInstance = new MQTTHandler();

    //returns MQTTHAndler instance
    public static MQTTHandler getInstance() {
        return ourInstance;
    }

    //empty constructor
    private MQTTHandler() {

    }

    //Gets context, broker URI and clientID, creates and returns an Android Client
    public MqttAndroidClient getClient(Context ctx, String broker, String clientID) {

            if (client != null) {
                client.unregisterResources();
            }
           // persistStore = new MessageContainer();
            //client = new MqttAndroidClient(ctx, broker, clientID, persistStore );
            client = new MqttAndroidClient(ctx, broker, clientID);
            client.setCallback(ourInstance);

        return client;
    }

    public MqttAndroidClient getClientHandle() {
        return client;
    }

    //creates a client connection to broker and sets Connection options appropriately
    public void connect(Context ctx) {
        this.ctx = ctx;

        //set last will to indicate to other users offline state
        MqttConnectOptions conOpts = new MqttConnectOptions();
        conOpts.setKeepAliveInterval(10);
        JSONObject userJson = new JSONObject();

       //Toast.makeText(ctx, Login.roomId, Toast.LENGTH_SHORT).show()

        try {
            userJson.put("type", "User");
            userJson.put("status", "offline");
            userJson.put("selfie", bitmapToString(myImage));
            userJson.put("userId", Login.nameId);
            userJson.put("clientId", client.getClientId());
            userJson.put("time", getTimeStamp());

        } catch (JSONException j) {
            //error loading JSONObject
            j.printStackTrace();
        }
        conOpts.setWill("smartboard/" + Login.roomId + "/users/" + Login.nameId, "".getBytes(), 0, true);

        try {
            client.connect(conOpts, ctx, new StatusListener(ctx, client));
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    //publishes free draw points to the broker on a separate thread from the main UI thread.
    //Makes lines drawn more fluid
    public void publish(final double mX, final double mY, final int action, final int color, final String mode, final int brushSize) {
        if (StatusListener.connectFlag == false)
            return;

        mPublishExecutor.submit(new Runnable() {
            @Override
            public void run() {
                JSONObject pointJson = new JSONObject();
                try {
                    pointJson.put("type", "Point");
                    pointJson.put("mX", mX);
                    pointJson.put("mY", mY);
                    pointJson.put("color",color);
                    pointJson.put("mode", mode);
                    pointJson.put("brushSize", brushSize);
                    pointJson.put("drawActionFlag", action);
                    pointJson.put("clientId", client.getClientId());
                    client.publish("smartboard/"+Login.roomId+"/points", pointJson.toString().getBytes(), 0, false);
                } catch (JSONException j) {
                    //failed to generate message
                } catch (MqttException e) {
                    //failed to send the message
                }
           }
        });
    }

    //publish system color change to other users logged in to the room
    public void publishColor(int color) {

        if (!StatusListener.connectFlag)
            return;
        JSONObject colorJson = new JSONObject();

        try {
            colorJson.put("type", "ColorChange");
            colorJson.put("code", color);
            colorJson.put("clientId", client.getClientId());
            client.publish("smartboard/"+Login.roomId+"/color", colorJson.toString().getBytes(), 1, true);
        } catch (JSONException j) {
            //
            j.printStackTrace();
        } catch (MqttException e) {
            //
            e.printStackTrace();
        }

    }

    //publish clear screen message to other users in the room
    public void publishClearScreen() {
        if (!StatusListener.connectFlag)
            return;
        JSONObject clearJson = new JSONObject();
        try {
            clearJson.put("type", "ClearScreen");
            client.publish("smartboard/"+Login.roomId+"/clear", clearJson.toString().getBytes(), 1, false);

        } catch (JSONException j) {
            //
        } catch (MqttException e) {
            //
        }
        clearAllRetainedObjects();

    }

    //converts a bitmap image to a string
    public static String bitmapToString(Bitmap bitmapImage) {
        if (bitmapImage == null)
            return null;
        Bitmap bitmap = bitmapImage;
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, blob);
        byte[] bitmapdata = blob.toByteArray();
        return Base64.encodeToString(bitmapdata, Base64.DEFAULT);
    }

    //broadcast chat message to other users logged in over MQTT
    public void publishChatMessage(ChatMessage message) {
        if (StatusListener.connectFlag == false)
            return;

        String bitmapString = bitmapToString(myImage);

        JSONObject chatMessage = new JSONObject();
        try {
            chatMessage.put("type", "Chat");
            chatMessage.put("username", Login.nameId);

            chatMessage.put("message", message.getMessage());
            chatMessage.put("direction", message.left);
            chatMessage.put("selfie", bitmapString);

            chatMessage.put("time", getTimeStamp());
            chatMessage.put("clientId", client.getClientId());
            client.publish("smartboard/"+Login.roomId+"/chat", chatMessage.toString().getBytes(), 1, false);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    //publishes image sent in chat environment to other users
    public void publishImage(Bitmap bitmap) {
        if (StatusListener.connectFlag == false || bitmap == null)
            return;

        String serializedBitmap = bitmapToString(bitmap);
        String bitmapString = bitmapToString(myImage);

        JSONObject chatMessage = new JSONObject();
        try {
            chatMessage.put("type", "Image");
            chatMessage.put("username", Login.nameId);
            chatMessage.put("message", null);
            chatMessage.put("direction", false); //message to the right
            chatMessage.put("selfie", bitmapString);
            chatMessage.put("image", serializedBitmap);
            chatMessage.put("time", getTimeStamp());
            chatMessage.put("clientId", client.getClientId());
            client.publish("smartboard/"+Login.roomId+"/chat", chatMessage.toString().getBytes(), 1, false);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }


    //publish user state (online) to other users in the room
    public void publishUserStateOnline() {
        if (!StatusListener.connectFlag)
            return;
        JSONObject userJson = new JSONObject();

        try {
            userJson.put("type", "User");
            userJson.put("status", "online");
            userJson.put("userId", Login.nameId);
            userJson.put("selfie", bitmapToString(myImage));
            userJson.put("clientId", client.getClientId());
            userJson.put("time", getTimeStamp());
            client.publish("smartboard/" + Login.roomId + "/users/" + Login.nameId, userJson.toString().getBytes(), 1, true);
        } catch (JSONException j) {

            j.printStackTrace();
        } catch (MqttException e) {

            e.printStackTrace();
        }
    }

    //publish user state (offline) to other users in the room
    public void publishUserStateOffline() {
         if (!StatusListener.connectFlag)
            return;
        JSONObject userJson = new JSONObject();

        try {
            userJson.put("type", "User");
            userJson.put("status", "offline");
            userJson.put("selfie", bitmapToString(myImage));
            userJson.put("userId", Login.nameId);
            userJson.put("clientId", client.getClientId());
            userJson.put("time", getTimeStamp());
            client.publish("smartboard/" + Login.roomId + "/users/" + Login.nameId, "".getBytes(), 2, true);
        } catch (JSONException j) {
            //
        } catch (MqttException e) {
            //
        }
    }

    //publish brush size change as global setting to other designers as retained message
    public void publishBrushSize(float size, String type) {
        if (!StatusListener.connectFlag)
            return;

       JSONObject brush = new JSONObject();

        try {
            brush.put("type", type);
            brush.put("size", size);
            client.publish("smartboard/"+Login.roomId+"/"+"brush", brush.toString().getBytes(), 0, true);
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    //globally publish erasor mode as a retained message
    public void publishErasorSize(float size, boolean isErase) {
        if (!StatusListener.connectFlag)
            return;

        JSONObject eraser = new JSONObject();

        try {
            eraser.put("type", "Eraser");
            eraser.put("size", size);
            eraser.put("erase", isErase);
            client.publish("smartboard/"+Login.roomId+"/"+"eraser", eraser.toString().getBytes(), 0, true);
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    //publishes rectangle objects
    public void publishRectangle(JSONObject rectangle) {
        if (!StatusListener.connectFlag)
            return;

        try {
            client.publish("smartboard/"+Login.roomId+"/objects/"+rectangle.optString("id"), rectangle.toString().getBytes(),0, true);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    //publishes circle objects
    public void publishCircle(JSONObject circle) {
        if (!StatusListener.connectFlag)
            return;

        try {
            client.publish("smartboard/"+Login.roomId+"/objects/"+circle.optString("id"), circle.toString().getBytes(), 0, true);
        } catch (MqttException e){
            e.printStackTrace();

        }

    }

    //publishes object as they are dragged
    public void publishObject(JSONObject object) {
        if (!StatusListener.connectFlag)
            return;

        try {
            object.put("clientId", client.getClientId()); //client that modified object position
            client.publish("smartboard/"+Login.roomId+"/objects/"+object.optString("id"), object.toString().getBytes(), 0, true);
        } catch (MqttException e){
            e.printStackTrace();

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    //publishes line object to broker
    public void publishLine(JSONObject line) {
        if (!StatusListener.connectFlag)
            return;

        try {
            client.publish("smartboard/"+Login.roomId+"/objects/"+line.optString("id"), line.toString().getBytes(), 0, true);
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    //publishes text message to broker
    public void publishtext(JSONObject textObject) {
        if (!StatusListener.connectFlag)
            return;
        try {
            client.publish("smartboard/"+Login.roomId+"/objects/"+textObject.optString("id"), textObject.toString().getBytes(), 0, true);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    //removes object with string id "s" retained in the broker
    public void removeObjectFromServer(String s) {
        try {
            client.publish("smartboard/" + Login.roomId + "/objects/" + s, "".getBytes(), 0, true);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    //clears all retained Objects in the broker
    public void clearAllRetainedObjects() {
        MyActivity drawingActivity = (MyActivity)drawingContext;
        for (String s: drawingActivity.drawer.objectDrawables.keySet()) {
            removeObjectFromServer(s);
        }
        for (String s: drawingActivity.drawer.textObjects.keySet()) {
            removeObjectFromServer(s);
        }
    }

    //called when a connection from the server is lost
    public void connectionLost(Throwable cause) {
        System.out.println("connection was lost");
        Toast lost = Toast.makeText(ctx, "Connection lost", Toast.LENGTH_SHORT);
        lost.show();
        StatusListener.connectFlag = false; //order of execution is important here
        close();
    }

    //clears datastore for the users in the room
    public void clearUserLog() {

        usersListHistory.clear();
        usersAdapter.notifyDataSetChanged();
    }

    //retrieves the drawView Context
    public void passDrawingContext(Context ctx) {
        drawingContext = ctx;
    }

    //Converts Date Object with current time to a simple String Date format
    public String getTimeStamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date date = new Date();
        String timeStamp = dateFormat.format(date);
        // System.out.println("Current Date Time : " + datetime);
        return timeStamp;
    }

    //parse a String timeStamp and returns an equivalent date object
    public Date getDate(String timeStamp) {
        Date date  = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            date = format.parse(timeStamp);
            // System.out.println(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        //do nothing
    }

    //handles all messages received from the broker
    public void messageArrived(String topic, MqttMessage message) {
        MyActivity drawingActivity = (MyActivity)drawingContext;
        JSONObject recvMessage = null;

        if (message.toString().compareTo("") ==0) {
            //user went offline
             String []topicStruct = topic.split("/");
           // System.out.println("user to be removed: "+topicStruct[2]);

            if (topicStruct[2].compareTo("users") == 0) {
                usersListHistory.remove(new OnlineStateMessage(null, topicStruct[3]));
                usersAdapter.notifyDataSetChanged();

            } else if (topicStruct[2].compareTo("objects") == 0) {
                drawingActivity.drawer.removeObject(topicStruct[3]);
            }
            return;
        }

            try {
                recvMessage = new JSONObject(message.toString());
            } catch (JSONException j) {
                j.printStackTrace();
            }


        if (recvMessage.optString("status").compareTo("online") == 0) {

            OnlineStateMessage newUser = new OnlineStateMessage(recvMessage.optString("selfie"),
                    recvMessage.optString("userId"));
           // System.out.println("user added: "+ recvMessage.optString("userId"));
            usersListHistory.add(newUser);
            usersAdapter.notifyDataSetChanged();
            return;


        }



        String clientId = recvMessage.optString("clientId");

        if (clientId.compareTo(client.getClientId()) != 0) {

            switch (type.valueOf(recvMessage.optString("type"))) {

                case Point:
                    drawingActivity.drawer.drawPoint((float) recvMessage.optDouble("mX"),
                            (float) recvMessage.optDouble("mY"), recvMessage.optInt("drawActionFlag"),
                            recvMessage.optInt("color"), recvMessage.optString("mode"), recvMessage.optInt("brushSize"), recvMessage.optString("clientId"));
                    break;
                case Eraser:
                    float eSize = (float)recvMessage.optDouble("size");
                    drawingActivity.drawer.updateEraseSize(eSize);
                    break;
                case Pencil: //shares topic with Eraser
                    float size = (float)recvMessage.optDouble("size");
                    drawingActivity.drawer.updateBrushSize(size);
                    break;

                case ColorChange:
                    drawingActivity.drawer.updateColor(recvMessage.optInt("code"));
                    break;

                case ClearScreen:
                    drawingActivity.drawer.updateClearScreen();
                    break;
                case Chat:
                    Vibrator v = (Vibrator)ctx.getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(new long[]{3,100}, -1);

                   String[] nameMessage =  recvMessage.optString("message").split(":");
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(ctx)
                                    .setLargeIcon(stringToBitmap(recvMessage.optString("selfie")))
                                    .setSmallIcon(R.drawable.smart2)
                                    .setContentTitle(nameMessage[0])
                                    .setContentText(nameMessage[1])
                                    .setTicker("New Message Arrived")
                                    .setAutoCancel(true)
                            .setNumber(++numMessages);


                    NotificationManager mNotificationManager =
                            (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                    // mId allows you to update the notification later on.
                    mNotificationManager.notify(1000, mBuilder.build());



                    ChatMessageWithSelfie mChatMessage = new ChatMessageWithSelfie(recvMessage.optBoolean("direction"), recvMessage.optString("message"),
                            recvMessage.optString("selfie"), recvMessage.optString("imageSent"), null);
                    sessionHistory.add(mChatMessage);
                    Chat.chatAdapter.add(mChatMessage);
                    break;

                case Image:
                    Vibrator v2 = (Vibrator)ctx.getSystemService(Context.VIBRATOR_SERVICE);
                    v2.vibrate(new long[]{3,100}, -1);
                    Toast.makeText(ctx, recvMessage.optString("username")+" has sent you a message!",
                            Toast.LENGTH_SHORT).show();
                    ChatMessageWithSelfie mChatImageMessage = new ChatMessageWithSelfie(recvMessage.optBoolean("direction"), null,
                            recvMessage.optString("selfie"), recvMessage.optString("image"), null);
                    sessionHistory.add(mChatImageMessage);
                    Chat.chatAdapter.add(mChatImageMessage);
                    break;
                case Rectangle:
                        drawingActivity.drawer.onDrawReceivedRectangle(recvMessage);
                    break;
                case Circle:
                    drawingActivity.drawer.onDrawReceivedCircle(recvMessage);
                    break;
                case Line:
                    drawingActivity.drawer.onDrawReceivedLine(recvMessage);
                    break;
                case Text:
                    drawingActivity.drawer.onDrawReceivedText(recvMessage);
                    break;
                default:
                    //ignore the message
            }
        }
    }

    //disconnects client from the server, throws an exception if an error occurs
    public void disconnect() {

        try {
            client.disconnect();

        } catch (MqttException e) {
            //error occurred while disconnecting
            e.printStackTrace();
        }
    }

    //close connection release client resources and return to log screen
    public void close() {
        clearUserLog();
        clearChatHistory();
        MyActivity drawingActivity = (MyActivity)drawingContext;
        drawingActivity.finish();
    }



    //clears chat history upon disconnection
    public void clearChatHistory() {
        sessionHistory.clear();
    }

    //unsubscribe to smartboard topics
    public void unSubscribe() {
        if (!StatusListener.connectFlag)
            return;
        try {
            client.unsubscribe("smartboard/" + Login.roomId + "/#");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    //Unregisters client resources
    public void unregisterClientResources () {
        client.unregisterResources();
    }

    //register client resources when activity resumes from a  hidden state
    public void registerResources(Context context) {client.registerResources(context);}

    //converts Base64 encoded string to a Bitmap
    public static Bitmap stringToBitmap(String encodedBitmap) {
        byte[] bitmapBytesOptImg = Base64.decode(encodedBitmap, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bitmapBytesOptImg, 0, bitmapBytesOptImg.length);
    }
}
