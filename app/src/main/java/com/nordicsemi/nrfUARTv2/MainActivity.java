
/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/* BEE 484 Water Monitoring System
   Depending on sensor value suggests maintenance
 */

package com.nordicsemi.nrfUARTv2;




import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import com.nordicsemi.nrfUARTv2.UartService;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import Processing.Filter_50_100;
import cloudBCI.CloudBciClient;
import cloudBCI.DummySignal;

import static java.lang.Math.pow;

public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;
    private static final int CREATE_REQUEST_CODE = 40;
    private static final int OPEN_REQUEST_CODE = 41;
    private static final int SAVE_REQUEST_CODE = 42;

    //private static EditText textView;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private Button btnConnectDisconnect,btnSaveData,btnViewSaveData;
    int lengthTxtValue = 61;
    double[] ECG_dataIn;
    double[] outPut;
    byte[] txValue;
    int[] a = new int[lengthTxtValue];
    double[] receivedData = new double[(lengthTxtValue-1)/3];
    double[] receivedData_test = new double[(lengthTxtValue-1)/3];
    private LineGraphSeries<DataPoint> dataChannel;
    private LineGraphSeries<DataPoint> indexChannel;
    private int lastX1 = 0;
    private int lastX2 = 0;
    boolean isRunning = false;
    boolean isConnect = false;
    boolean isSaving = false;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    ArrayList<Double> indexData = new ArrayList<Double>();
    ArrayList<Double> rawDataSave = new ArrayList<Double>();
    SaveData saver = new SaveData();
    IIR_Filter filter = new IIR_Filter();
    CloudBciClient client  = new CloudBciClient("http://cloud-bci.duckdns.org" +
            ":8000");
    double[] filter_input = {0, 0, 0, 0, 0, 0, 0, 0, 0};
    double[] filter_output = {0, 0, 0, 0, 0, 0, 0, 0};
    String gainSelect[] = {"1", "2", "3", "4"};
    byte[] value = {0x00};
    int index = 1;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        /**
         * Checks if the app has permission for BLE
         *
         * If the app does not has permission then the user will be prompted to grant permissions
         *
         */
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
            }else{
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }else{
            Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();
        }

        /**
         * Checks if the app has permission to write to device storage for API 23+
         *
         * If the app does not has permission then the user will be prompted to grant permissions
         *
         */
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
        DummySignal dummy = new DummySignal();
       // dummy.start();


        initGraphIndex();
        initGraphData();
        btnSaveData = (Button)findViewById(R.id.btn_saveData);
        btnViewSaveData = (Button)findViewById(R.id.btn_viewSaveData);
        // select Gain to send to BLE device
        Spinner spinGain = (Spinner)findViewById(R.id.FGA);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, gainSelect);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        spinGain.setAdapter(adapter);
       // spinGain.setEnabled(false);

        //Performing action onItemSelected and onNothing selected
        spinGain.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        switch (gainSelect[i])
                        {
                            case "1":
                               // Toast.makeText(getApplicationContext(), "Hello", Toast.LENGTH_LONG).show();
                                index = 1;
                                break;
                            case "2":
                                //Toast.makeText(getApplicationContext(), "Good byte", Toast.LENGTH_LONG).show();
                               // mService.writeRXCharacteristic(value);
                                index = 2;
                                break;
                            case "3":
                                index = 3;
                                break;
                            case "4":
                                index = 4;
                                break;
                             default:
                                // Toast.makeText(getApplicationContext(), "!!", Toast.LENGTH_LONG).show();
                                 break;
                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                }
        );




        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnConnectDisconnect=(Button) findViewById(R.id.btn_connect);
        service_init();

        // Handle Save data function
        btnSaveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(rawDataSave.size() == 0) { Toast.makeText(getApplicationContext(), "No data available yet.", Toast.LENGTH_SHORT).show();}

                else {
                    isSaving = true;
                    isRunning = false;
                    mService.disconnect();

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.popup_title2)
                            .setMessage(R.string.popup_message2)
                            .setNeutralButton(R.string.popup_overwrite, null)
                            .setNegativeButton(R.string.popup_new, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                  //  saver.save(indexData);
                                    saver.save(rawDataSave);
                                    Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
                                    isSaving = false;
                                    isRunning = true;
                                }
                            })

                            .setPositiveButton(R.string.popup_cancel, null)
                            .show();
                }
            }
        });

        // Handle Viewing of saved data function
        btnViewSaveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        // Handle Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else {
                	if (btnConnectDisconnect.getText().equals("Connect")){
            			Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
            			startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
            			// Start Thread for displaying data
                       //new FilterThread(handler).start();


        			} else {
        				//Disconnect button pressed
        				if (mDevice!=null)
        				{
        					mService.disconnect();

                            //resetData();
        				}
        			}
                }
            }
        });

    }
    private void resetData(){
        isRunning = false;
        rawDataSave.clear();
    }
    private void initGraphIndex(){
        // we get graph view instance
        GraphView graph = (GraphView) findViewById(R.id.graph);
        // data
        indexChannel = new LineGraphSeries<DataPoint>();
            graph.addSeries(indexChannel);
        // customize a little bit viewport
        Viewport viewport = graph.getViewport();
            viewport.setYAxisBoundsManual(false);
         //   viewport.setMinY(-5);
         //   viewport.setMaxY(300);
         //   viewport.setMinX(-1000);
         //   viewport.setMaxX(1000);
         //   viewport.setScrollable(false);
        //graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);
          //  graph.getGridLabelRenderer().setNumHorizontalLabels(40);
          //  graph.getGridLabelRenderer().setNumVerticalLabels(20);
            graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
            graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
            graph.setTitle("Index");
        // viewport
    }

    private void initGraphData(){
        // we get graph view instance
        GraphView graph2 = (GraphView) findViewById(R.id.graph2);
        // data
        dataChannel = new LineGraphSeries<DataPoint>();
        graph2.addSeries(dataChannel);
        // customize a little bit viewport
        Viewport viewport2 = graph2.getViewport();
        viewport2.setYAxisBoundsManual(false);
       // viewport2.setMinY(0);
        //viewport2.setMaxY(0.5);
      //  viewport2.setMinX(-1000);
       // viewport2.setMaxX(1000);
        viewport2.setScrollable(false);
        graph2.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);
        graph2.getGridLabelRenderer().setNumHorizontalLabels(40);
       // graph2.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.VERTICAL);
        graph2.getGridLabelRenderer().setNumVerticalLabels(40);

     //   graph2.getGridLabelRenderer().setPadding(32);
        graph2.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph2.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph2.setTitle("Channel 1");
        // viewport.
    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
        		mService = ((UartService.LocalBinder) rawBinder).getService();
        		Log.d(TAG, "onServiceConnected mService= " + mService);
        		if (!mService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                }

        }

        public void onServiceDisconnected(ComponentName classname) {
       ////     mService.disconnect(mDevice);
        		mService = null;
        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
           //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
            	 runOnUiThread(new Runnable() {
                     public void run() {
                         	String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                             Log.d(TAG, "UART_CONNECT_MSG");
                             btnConnectDisconnect.setText("Disconnect");
                             isConnect = true;
                             //spinGain.setEnabled(true);
                             if(!isSaving) {Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();}
                            //((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - ready");
                             //Toast.makeText(getApplicationContext(), "["+currentDateTimeString+"] Connected to: "+ mDevice.getName(),Toast.LENGTH_LONG).show();
                             mState = UART_PROFILE_CONNECTED;
                     }
            	 });
            }

          //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
            	 runOnUiThread(new Runnable() {
                     public void run() {
                    	 	 String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                             Log.d(TAG, "UART_DISCONNECT_MSG");
                             btnConnectDisconnect.setText("Connect");
                             if(!isSaving) {Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_LONG).show();}
                         //   ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                            // Toast.makeText(getApplicationContext(), "["+currentDateTimeString+"] Disconnected to: "+ mDevice.getName(),Toast.LENGTH_LONG).show();
                             mState = UART_PROFILE_DISCONNECTED;
                             mService.close();
                             //resetData();
                             isRunning = false;

                     }
                 });
            }


          //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
             	 mService.enableTXNotification();
            }
            int i; double d_index = 0;
          //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
                txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
               // firstDataBuffer = new double[3000];
                for ( i = 0; i < txValue.length; i++) {
                    a[i] = 0xFF & txValue[i];
                    Log.d(Utils.MTAG, "Length=" + txValue.length);
                    d_index = a[i];
                    if (i == txValue.length - 1){
                        indexData.add(d_index);
                       indexChannel.appendData(new DataPoint(lastX1++, a[i]), true, 300);
                      // String data = a[i] + "";
                       //client.insert_real_time_data(1, data);
                     //  Log.d(Utils.MTAG,"Index:" +i);
                    }
                }
                /*
                for (int i = 0; i < txValue.length / 2; i++) {
                    receivedData[i] = (256 * a[2 * i] + a[(2 * i) + 1]);
                    rawDataSave.add(receivedData[i]);

                    filter_input = filter.update_input_filter_array(filter_input, receivedData[0]);
                    double filtered_point = filter.filter(filter_input, filter_output);
                    filter_output = filter.update_output_filter_array(filter_output, filtered_point);

                    filteredDataSave.add(filtered_point);
                    series_maternal.appendData(new DataPoint(lastX1++, filtered_point / 700), true, 800);
                    series_fetal.appendData(new DataPoint(lastX2++, 3-receivedData[0] / 900), true, 800);
                }
                */
                for ( i = 1; i < (txValue.length - 1) / 3; i++) {
                    double temp_value = 0;
                    temp_value = 65536*a[3*i] + 256 * a[3 * i + 1] + a[(3 * i) + 2];

                    if (temp_value >= 8388608) // check it is negative or not
                    {
                        temp_value = temp_value - 16777216;
                    }
                    if ( i == index){
                        rawDataSave.add(temp_value*4.5/(pow(2,23)-1));
                        filter_input = filter.update_input_filter_array(filter_input, temp_value);
                        double filtered_point = filter.filter(filter_input, filter_output);
                        filter_output = filter.update_output_filter_array(filter_output, filtered_point);
                        dataChannel.appendData(new DataPoint(lastX2++,  filtered_point*4.5/(pow(2,23)-1)), true, 500);
                       // String data = (int)temp_value + "";
                       // client.insert_real_time_data(2, data);
                        //Log.d(Utils.MTAG,"Value:" +10*temp_value*4.5/(pow(2,23)-1));
                    }
                }

             }
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                    showMessage("Device doesn't support UART. Disconnecting");
                    mService.disconnect();
            }
        }
    };

    private class CalHR extends Thread{
        private Handler mHandler;
        public CalHR(Handler handler){
            this.mHandler = handler;
        }
        @Override
        public void run() {
            super.run();
        }
    }
    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
        	LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                  //  ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                    mService.connect(deviceAddress);
                    resetData();
                    isRunning = true;

                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            //case CREATE_REQUEST_CODE:

            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }


    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        }
        else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.popup_title1)
                    .setMessage(R.string.popup_message)
                    .setNegativeButton(R.string.popup_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setPositiveButton(R.string.popup_no, null)
                    .show();
        }
    }

}