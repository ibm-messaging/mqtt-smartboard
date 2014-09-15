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

import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import java.util.Enumeration;
import java.util.Hashtable;


/**
 * Created by Allan Marube on 7/28/2014.
 * A persistence store for messages published with QoS 1 or 2.
 * Uses a hashtable implementation. You may use this class or
 * default class for MqttClientPersistence
 */
public class MessageContainer implements MqttClientPersistence {

    Hashtable <String, MqttPersistable> dataStore; //storage of pending data
    private String clientId;
    private String serverURI;

    public MessageContainer() {
    }


    @Override
    public void open(String clientId, String serverURI) throws MqttPersistenceException {
        if (clientId == null || serverURI == null)
            throw new MqttPersistenceException();
        if (dataStore == null) {
           this.serverURI = serverURI;
           this.clientId = clientId;
           dataStore = new Hashtable<String, MqttPersistable> ();
        }

    }

    @Override
    public void close() throws MqttPersistenceException {
        dataStore.clear();

    }

    @Override
    public void put(String key, MqttPersistable persistentData) throws MqttPersistenceException {
        if (key == null || persistentData == null)
            throw new MqttPersistenceException();
        dataStore.put(key, persistentData);
    }

    @Override
    public MqttPersistable get(String key) throws MqttPersistenceException {
         MqttPersistable message = dataStore.get(key);
         return message;
    }

    @Override
    public void remove(String key) throws MqttPersistenceException {
        dataStore.remove(key);
    }

    @Override
    public Enumeration keys() throws MqttPersistenceException {
        return dataStore.keys();
    }

    @Override
    public void clear() throws MqttPersistenceException {
         dataStore.clear();
    }

    @Override
    public boolean containsKey(String key) throws MqttPersistenceException {
        return dataStore.containsKey(key);
    }
}
