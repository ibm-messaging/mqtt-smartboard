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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by Allan Marube on 8/19/2014.
 * Provides a space for writing text to be input to the canvas.
 * I use another activity to launch the Alert popup
 * because the activity is recreated when popup closes and would lose some
 * freedraw information.
 */
public class TextBoxAlert extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    @Override
    public void onResume() {

        super.onResume();

        final EditText input = new EditText(this);
        final AlertDialog.Builder textAlert = new AlertDialog.Builder(this);
        textAlert.setTitle("Text");
        textAlert.setMessage("Enter text here:");
        textAlert.setView(input);
        textAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which){
                //save drawing

                Editable value = input.getText();
                //textMode(true, value.toString());
                Toast.makeText(getApplicationContext(), "Touch where to place text", Toast.LENGTH_LONG).show();


                Intent result = new Intent();
                result.putExtra("InputString", value.toString());
                setResult(Activity.RESULT_OK, result);

                finish();
            }
        });
        textAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //do nothing
                finish();
            }
        });

        textAlert.show();

    }

}