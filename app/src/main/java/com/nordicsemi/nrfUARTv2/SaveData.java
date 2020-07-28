package com.nordicsemi.nrfUARTv2;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class SaveData {
    public void save(final ArrayList<Double> ecgData) {
       // String currentTime = DateFormat.getTimeInstance().format(new Date());

        String filename = "" + getDateTime() + "_ECG.txt";
        OutputStreamWriter writer = null;
        if (Environment.getExternalStorageState() == null){

            File directory = new File(Environment.getDataDirectory()+"/ECG_Data_02");
            if (!directory.exists()) {
                directory.mkdirs();
                Log.i("making", "Creating Directory: " + directory);
            }
            Log.i("making", "AAAAA: " + directory);
            File newFile = new File(directory, filename);
            try {
                writer = new OutputStreamWriter(new FileOutputStream(newFile));
                for (int i = 0; i < ecgData.size(); i++) {
                    Log.i("writer", "Writing to file");
                    writer.write(String.valueOf(ecgData.get(i) + "\n"));
                }

                writer.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if (Environment.getExternalStorageState() != null){
            File directory = new File(Environment.getExternalStorageDirectory()+"/ECG_Data");
            if (!directory.exists()) {
                directory.mkdirs();
                Log.i("making", "Creating Directory: " + directory);
            }
            File newFile = new File(directory, filename);
            try {
                writer = new OutputStreamWriter(new FileOutputStream(newFile));
                for (int i = 0; i < ecgData.size(); i++) {
                    Log.i("writer", "Writing to file");
                    writer.write(String.valueOf(ecgData.get(i) + "\n"));
                }

                writer.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private  final static String getDateTime()
    {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("GMT-8"));
        return df.format(new Date());
    }
}
