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
 * Created by Allan Marube on 8/5/2014.
 * Defines a message that announces user prescence
 */
public class OnlineStateMessage implements Serializable {

    private String bitmap;//user selfie
    private String name; //user name

    public OnlineStateMessage(String bitmap, String name) {
        this.bitmap = bitmap;
        this.name = name;
    }

    //splits name and random string ID and returns the name only
    public String getName() {
        String [] split = name.split("%");
        return split[0];
       // return name;
    }
    //gets user selfie
    public String getBitmap() {
        return bitmap;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) return true;
        if(object == null ) return false;
        if (!(object instanceof OnlineStateMessage)) return false;

        OnlineStateMessage x = (OnlineStateMessage)object;
        OnlineStateMessage y = this;
        return (x.name.compareTo(y.name) == 0);
    }
}
