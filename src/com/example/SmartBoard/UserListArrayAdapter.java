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
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Allan Marube on 8/5/2014.
 * Adapter that organizes users list on a listView
 */
public class UserListArrayAdapter extends ArrayAdapter {
    //user list
    private ArrayList<OnlineStateMessage> usersList = new ArrayList<OnlineStateMessage>();
    //textView with user name info
    private TextView userText;
    //Imageview containing selfie
    private ImageView userImage;

    private LinearLayout singleMessageContainer;

    //DrawView Context
    private Context ctx;

    //instantiate adapter
    public UserListArrayAdapter(Context context, int textViewResourceId, ArrayList list) {
        super(context, textViewResourceId,list);
        this.ctx = context;
        this.usersList = list;

    }

    @Override
    public void add(Object object) {
        OnlineStateMessage message = (OnlineStateMessage)object;
        usersList.add(message);

        super.add(object);
    }

    public int getCount() {
        return this.usersList.size();
    }

    public OnlineStateMessage getItem(int index) {
        return this.usersList.get(index);
    }


    public void remove(OnlineStateMessage object) {
        if (usersList.contains(object))
            Toast.makeText(ctx, "Yes it is there", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(ctx, "No not there", Toast.LENGTH_SHORT).show();
        int index =  usersList.indexOf(object);
        usersList.remove(index);
        super.remove(index);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.online_list_message, parent, false);
        }
        singleMessageContainer = (LinearLayout) row.findViewById(R.id.listMessageContainer);
        OnlineStateMessage userMessageObj = getItem(position);

        userText = (TextView) row.findViewById(R.id.listMessage);
        userText.setText(userMessageObj.getName());

        // userMessageObj.bitmap;
        byte[] bitmapBytes = Base64.decode(userMessageObj.getBitmap(), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);

        userImage = (ImageView) row.findViewById(R.id.imgUser);
        userImage.setImageBitmap(bitmap);
        return row;
    }
}
