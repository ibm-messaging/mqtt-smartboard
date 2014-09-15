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
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;



/**
 * Created by Allan Marube on 7/29/2014.
 */

public class Chat extends Activity {
    // Debugging
    private static final String TAG = "BcastChatUI";
    private static final boolean D = true;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    //chat Message with Image To Send to another client
    private ChatMessageWithSelfie imageToSend;

    // Name of the connected device
    public static ArrayAdapter<CharSequence> mConversationArrayAdapter;
    public static ChatArrayAdapter chatAdapter;

    //private ArrayList<String> mConversationHistory;
    private StringBuffer mOutStringBuffer;

    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            //repopulate chat history
          //  mConversationHistory = savedInstanceState.getStringArrayList("History");
            MQTTHandler.getInstance().sessionHistory = (ArrayList<ChatMessageWithSelfie>)savedInstanceState.getSerializable("History");
            Log.e(TAG, "arrayList from History");


        }
        else {
           // mConversationHistory = new ArrayList<String>();
        }


        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        getActionBar().setDisplayHomeAsUpEnabled(false); //disables up navigation

        // Set up the window layout
        setContentView(R.layout.chat);
    }


    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        setupChat();


    }


    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putSerializable("History", MQTTHandler.getInstance().sessionHistory);
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<CharSequence>(this, R.layout.message);
       // mImageAdapter = new ArrayAdapter<Bitmap>(this, R.layout.image);
       // mConversationHistory = new ArrayList<String>();
        chatAdapter = new ChatArrayAdapter(this, R.layout.activity_chat_singlemessage);

        mConversationView = (ListView) findViewById(R.id.in);
        //mConversationView.setAdapter(mConversationArrayAdapter);
        mConversationView.setAdapter(chatAdapter);
       // mConversationView.setAdapter(mImageAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(D) Log.e(TAG, "[sendButton clicked]");
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);

            }
        });

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        //populate chat history

        if (!MQTTHandler.getInstance().sessionHistory.isEmpty() )

            for (ChatMessageWithSelfie message : MQTTHandler.getInstance().sessionHistory)
                chatAdapter.add(message);
    }


    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }


    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }


    public void onDestroy() {
        super.onDestroy();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

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

            String mImageBitmap = MQTTHandler.bitmapToString(imageBitmap);
            String selfieString = MQTTHandler.bitmapToString(MQTTHandler.myImage);

            MQTTHandler.getInstance().publishImage(imageBitmap);

            imageToSend = new ChatMessageWithSelfie(true, null, selfieString, mImageBitmap, null);
            displayImage(imageToSend);

        } else if (requestCode == REQUEST_SELFIE_CAPTURE && resultCode == RESULT_OK) {

            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            MQTTHandler.myImage = Login.getResizedBitmap(imageBitmap);

        }
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            System.out.println("In video section");
            VideoView mVideoView = (VideoView)findViewById(R.id.videoView);
            Uri videoUri = data.getData();
            String videoUriS = videoUri.toString();
            ChatMessageWithSelfie videoMessage = new ChatMessageWithSelfie(true, null, MQTTHandler.bitmapToString(MQTTHandler.myImage),null,videoUriS);
            chatAdapter.add(videoMessage);
            MQTTHandler.sessionHistory.add(videoMessage);
           // mVideoView.setVideoURI(videoUri);
        }
    }

    static final int REQUEST_VIDEO_CAPTURE = 2;
    //not being used currently. Video recording is not enabled yet.
    public void dispatchTakeVideoIntent(View view) {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    private void displayImage(ChatMessageWithSelfie message) {
        chatAdapter.add(imageToSend);
        MQTTHandler.sessionHistory.add(imageToSend);
    }

    //packages String mesage passed in as argument as a ChatMessageWithSelfie and sends it
    //via MQTT
    private void sendMessage(String message) {

        // Check that there's actually something to send
        ChatMessage mChatMessage = new ChatMessage(false, Login.nameOnly+": " + message);
        Bitmap bitmap = MQTTHandler.myImage;
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, blob);
        byte[] bitmapdata = blob.toByteArray();

        String bitmapString = Base64.encodeToString(bitmapdata, Base64.DEFAULT);
        ChatMessageWithSelfie mChatMessageSelfie = new ChatMessageWithSelfie(true, Login.nameOnly+": " + message, bitmapString, null, null);


        if (message.length() > 0 ) {
            // We will send a message via MQTT

            MQTTHandler.getInstance().publishChatMessage(mChatMessage);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }else{

            Toast.makeText(this, "Please type a message", Toast.LENGTH_SHORT).show();
            return;

        }

        // Add message to UI
        //mConversationArrayAdapter.add("Me:  " + message);
        //set back to left position for local display
        mChatMessage.setLeft();

        chatAdapter.add(mChatMessageSelfie);
       // chatAdapter.add(imageToSend);
        MQTTHandler.getInstance().sessionHistory.add(mChatMessageSelfie);
    }


    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // If the action is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL 				// return key
                            && event.getAction() == KeyEvent.ACTION_UP) // the key has been released
                    {
                        String message = view.getText().toString();
                        sendMessage(message);
                    }
                    if(D) Log.i(TAG, "END onEditorAction");
                    return true;
                }
            };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.history:
                MQTTHandler.sessionHistory.clear();
                chatAdapter.clear();
                setupChat();
                ///hatAdapter.add(null);
                break;
            case R.id.selfieChange:
                dispatchTakeSelfieIntent();
                break;
        }
        return true;
    }


    static final int REQUEST_SELFIE_CAPTURE = 3;
    public void dispatchTakeSelfieIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_SELFIE_CAPTURE);
        }
    }
}