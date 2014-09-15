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

import java.io.Serializable;



/**
 * Created by Allan Marube on 7/31/2014.
 * This class abstracts a simple chat Message
 * which has text and defines position to be
 * be placed on the screen
 */

public class ChatMessage implements Serializable{
    public boolean left; //chat message position. Default is left
    public String message; //chat message

    public ChatMessage(boolean left, String message) {
        super();
        this.left = left;
        this.message = message;
    }
    //sets chat message to the left
    public void setLeft() {
        this.left = true;
    }
    //sets chat message to the right
    public void setRight() {
        this.left = false;
    }
    //returns message message in the Chat Object
    public String getMessage() {
        return message;
    }
}