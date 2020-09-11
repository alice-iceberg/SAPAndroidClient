package com.example.sapprovider;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgentV2;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

import java.io.IOException;
import java.util.HashMap;


public class SAPServiceProvider extends SAAgentV2 {

    public final static String TAG = "SAPServiceProvider";
    public final static int SAP_SERVICE_CHANNEL_ID = 123;
    private final IBinder mIBinder = new LocalBinder();
    HashMap<Integer, SAPServiceProviderConnection> connectionMap = null;

    public SAPServiceProvider(Context context) {
        super(TAG, context, SAPServiceProviderConnection.class);
        SA mAccessory = new SA();
        try{
            mAccessory.initialize(context);
        }catch (SsdkUnsupportedException e){
            Log.e(TAG, "SAPServiceProvider: SsdkUnsupported Exception"+ e);
        }catch (Exception e1){
            Log.e(TAG, "SAPServiceProvider: Cannot initialize accessory package");
            e1.printStackTrace();
        }
    }

    @Override
    protected void onFindPeerAgentsResponse(SAPeerAgent[] saPeerAgents, int i) {
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent saPeerAgent, SASocket thisConnection, int result) {
        //here the connection is handled
        if (result == CONNECTION_SUCCESS) {
            if (thisConnection != null) {
                SAPServiceProviderConnection myConnection = (SAPServiceProviderConnection) thisConnection;
                if (connectionMap == null) {
                    connectionMap = new HashMap<Integer, SAPServiceProviderConnection>();
                }

                myConnection.connectionID = (int) (System.currentTimeMillis() & 255);
                Log.d(TAG, "onServiceConnectionResponse: ConnectionID" + myConnection.connectionID);
                connectionMap.put(myConnection.connectionID, myConnection);

                Toast.makeText(getApplicationContext(), "CONNECTION ESTABLISHED", Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "onServiceConnectionResponse: SocketObject is null");
            }
        } else if (result == CONNECTION_ALREADY_EXIST) {
            Log.e(TAG, "onServiceConnectionResponse: Connection already exists");
        } else {
            Log.e(TAG, "onServiceConnectionResponse: result error" + result);
        }

    }

    @Override
    protected void onServiceConnectionRequested(SAPeerAgent saPeerAgent) {
        acceptServiceConnectionRequest(saPeerAgent);
    }

    public IBinder onBind(Intent arg0) {
        return mIBinder;
    }

    public String getDeviceInfo() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        return manufacturer + " " + model;
    }

    public class LocalBinder extends Binder {
        public SAPServiceProvider getService() {
            return SAPServiceProvider.this;
        }
    }


    public class SAPServiceProviderConnection extends SASocket {

        private int connectionID;

        protected SAPServiceProviderConnection() {
            super(SAPServiceProviderConnection.class.getName());
        }

        @Override
        public void onError(int channel_id, String errorString, int errorCode) {
            Log.e(TAG, "onError: " + errorString + " " + errorCode);

        }

        @Override
        public void onReceive(int channel_id, byte[] data) {
            //communication with wearable happens here
            final String message;
            long timestamp = System.currentTimeMillis();
            String timeString = timestamp + "";

            String stringToUpdateUI = new String(data);
            message = getDeviceInfo() + stringToUpdateUI.concat(timeString);
            Log.e(TAG, "SAP message: " + message);

            final SAPServiceProviderConnection uHandler = connectionMap.get(Integer.parseInt(String.valueOf(connectionID)));
            if (uHandler == null) {
                Log.e(TAG, "Error: Cannot get SAPServiceConnection handler");
                return;
            }


            //here info from wearable is sent to the device
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        uHandler.send(SAP_SERVICE_CHANNEL_ID, message.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }

        @Override
        protected void onServiceConnectionLost(int i) {
            if (connectionMap != null) {
                connectionMap.remove(connectionID);
            }
        }
    }

}
