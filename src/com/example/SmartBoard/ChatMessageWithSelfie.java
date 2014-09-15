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

import android.graphics.Bitmap;
import android.net.Uri;

import java.io.Serializable;
import java.net.URI;


/**
 * Created by Allan Marube on 8/1/2014.
 * This class defines a chatMessage which consists
 * of message String, a selfie, or optionally an image
 * to be sent or a short video(not implemented)
 */
public class ChatMessageWithSelfie implements Serializable {

    public boolean left; //chat position
    public String message; //chat message
    public String selfie;  //selfie
    public String optImage; //sent image
    public String uri;  //uri for a video ro send(functionality not implemented yet)


    public ChatMessageWithSelfie(boolean left, String message, String selfie, String optImage, String uri) {
        super();
        this.left = left;
        this.message = message;
        this.selfie = selfie;
        this.optImage = optImage;
        this.uri = uri;

    }
    //sets chat message position to the left
    public void setLeft() {this.left = true;}
    //sets chat message to the right
    public void setRight() {this.left = false;}
    //returns chat message
    public String getMessage() {
        return message;
    }
    //returns selfie associated with the chat message
    public String getSelfie() {
       return selfie;
    }
}
