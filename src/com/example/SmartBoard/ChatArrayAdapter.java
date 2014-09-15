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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Allan Marube on 7/31/2014.
 */
public class ChatArrayAdapter extends ArrayAdapter {
    //Conversation History
    private ArrayList<ChatMessageWithSelfie> chatMessageList = new ArrayList<ChatMessageWithSelfie>();
    private TextView chatText; //text view for writing message
    private LinearLayout singleMessageContainer; //Parent layout
    private Context ctx; //application context


    //instantiate adapter
    public ChatArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        this.ctx = context;

    }

    @Override
    public void add(Object object) {
        ChatMessageWithSelfie message = (ChatMessageWithSelfie)object;
        chatMessageList.add(message);

        System.out.println("Message Added");
        //image = message.getSelfie();
        super.add(object);
    }

    public int getCount() {
        return this.chatMessageList.size();
    }

    public ChatMessageWithSelfie getItem(int index) {
        return this.chatMessageList.get(index);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.activity_chat_singlemessage, parent, false);
        }
        singleMessageContainer = (LinearLayout) row.findViewById(R.id.singleMessageContainer);
        ChatMessageWithSelfie chatMessageObj = getItem(position);
        chatText = (TextView) row.findViewById(R.id.singleMessage);

        chatText.setText(chatMessageObj.message);

        byte[] bitmapBytes = Base64.decode(chatMessageObj.getSelfie(), Base64.DEFAULT);
        Bitmap bitmapEq = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);

        Bitmap bitmapOptImgEq = null;
        if (chatMessageObj.optImage != null) {
            byte[] bitmapBytesOptImg = Base64.decode(chatMessageObj.optImage, Base64.DEFAULT);
            bitmapOptImgEq = BitmapFactory.decodeByteArray(bitmapBytesOptImg, 0, bitmapBytesOptImg.length);
        }

        ImageView imgViewLeft = (ImageView)row.findViewById(R.id.img);
        ImageView imgViewRight = (ImageView)row.findViewById(R.id.imgDisp);

        VideoView mVideoView = (VideoView) row.findViewById(R.id.videoView);
        ViewGroup.LayoutParams lpVid = mVideoView.getLayoutParams();
        if (chatMessageObj.uri == null) {

            lpVid.height = 0;
            lpVid.width = 0;
            mVideoView.setLayoutParams(lpVid);
        }
        else {
            lpVid.height = 400;
            lpVid.width = 400;
            mVideoView.setLayoutParams(lpVid);

        }

        //align selfies correctly
        if (chatMessageObj.left && chatMessageObj.optImage != null) {

            ViewGroup.LayoutParams lp = imgViewLeft.getLayoutParams();
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            imgViewLeft.setLayoutParams(lp);
            imgViewLeft.setImageBitmap(bitmapEq);
            imgViewLeft.setBackgroundResource(0);


            ViewGroup.LayoutParams lp2 = imgViewRight.getLayoutParams();
            lp2.height = 480;
            lp2.width = 270;
            imgViewRight.setLayoutParams(lp2);
            imgViewRight.setImageBitmap(bitmapOptImgEq);

            imgViewRight.setBackgroundResource(chatMessageObj.left ? R.drawable.left2 : R.drawable.right2);
            chatText.setBackgroundResource(0);

        }
        else if (chatMessageObj.left &&chatMessageObj.message!=null ) {

            ViewGroup.LayoutParams lp = imgViewLeft.getLayoutParams();
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            imgViewLeft.setLayoutParams(lp);
            imgViewLeft.setImageBitmap(bitmapEq);
            imgViewLeft.setBackgroundResource(0);


            ViewGroup.LayoutParams lp2 = imgViewRight.getLayoutParams();
            lp2.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp2.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            imgViewRight.setLayoutParams(lp2);
            imgViewRight.setImageBitmap(null);
            imgViewRight.setBackgroundResource(0);
            chatText.setBackgroundResource(chatMessageObj.left ? R.drawable.left2 : R.drawable.right2);

        }
        else if(!chatMessageObj.left && chatMessageObj.message!=null) {

            ViewGroup.LayoutParams lp = imgViewRight.getLayoutParams();
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            imgViewRight.setImageBitmap(bitmapEq);
            imgViewRight.setLayoutParams(lp);

            ViewGroup.LayoutParams lp2 = imgViewLeft.getLayoutParams();
            lp2.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp2.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            imgViewLeft.setLayoutParams(lp2);
            imgViewLeft.setImageBitmap(null);
            imgViewLeft.setBackgroundResource(0);
            imgViewRight.setBackgroundResource(0);

            chatText.setBackgroundResource(chatMessageObj.left ? R.drawable.left2 : R.drawable.right2);


        }

        else if (!chatMessageObj.left && chatMessageObj.optImage!=null) {

            ViewGroup.LayoutParams lp = imgViewRight.getLayoutParams();
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            imgViewRight.setLayoutParams(lp);
            imgViewRight.setImageBitmap(bitmapEq);
            imgViewRight.setBackgroundResource(0);

            ViewGroup.LayoutParams lp2 = imgViewLeft.getLayoutParams();
            lp2.height = 480;
            lp2.width = 270;
            imgViewLeft.setLayoutParams(lp2);
            imgViewLeft.setImageBitmap(bitmapOptImgEq);
            imgViewLeft.setBackgroundResource(chatMessageObj.left ? R.drawable.left2 : R.drawable.right2);

            chatText.setBackgroundResource(0);
        }


        /**************Tests video app*************************/
        if (chatMessageObj.uri!=null) {
            Toast.makeText(ctx, "Cheza ngoma", Toast.LENGTH_SHORT).show();
            MediaController controller = new MediaController(getContext());
            mVideoView.setVideoURI(Uri.parse(chatMessageObj.uri));
            mVideoView.setMediaController(controller);

        }

        /*******************************************************/

        //chatText.setBackgroundResource(chatMessageObj.left ? R.drawable.left : R.drawable.right);
        singleMessageContainer.setGravity(chatMessageObj.left ? Gravity.LEFT : Gravity.RIGHT);
        return row;
    }

}


