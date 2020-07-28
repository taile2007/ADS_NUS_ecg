package com.nordicsemi.nrfUARTv2;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

//import org.apache.http.NameValuePair;
//import org.apache.http.message.BasicNameValuePair;


import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class Utils {
	public static final String MTAG = "MY_TAG";
	public static String to2digit(int number)
	{
		String str;
		str = String.valueOf(number);
		if(number<10)
			str = "0"+str;
		return str;
		
	}
	public static double[] getArray(String stringSrc)
	{
		//int err=0;
		if(stringSrc.length()>2)
		{
			double[] arrDouble;
			String[] arrString = stringSrc.split("\n");
			int arrLength = arrString.length-2;
			arrDouble = new double[arrLength];
			for(int i=0;i<arrLength;i++)
			{
				int j=0;
				String str = arrString[i+1];
				if(str.length()!=4)
				{
					arrDouble[i] = -1;
					Log.e(MTAG, "Error: Line: "+i+", content: "+str);
					//err++;
				}
				else
				{
					for(j=0;j<str.length();j++)
					{
						if(str.charAt(j)<='9' && str.charAt(j)>='0')
							break;
					}
					arrDouble[i] = Double.parseDouble(str.substring(j));
				}
			}
			//Log.e(MTAG, "ERROR: "+err);
			return arrDouble;
		}
		else
			return new double[1];
	}
	public static double[] StringToArray(String strData,String demiter)
	{
		double[] arrDouble;
		String[] arrString = strData.split(demiter);
		int arrLength = arrString.length;
		arrDouble = new double[arrLength];
		for(int i=0;i<arrLength;i++)
		{
			int j=0;
			String str = arrString[i];
			arrDouble[i] = Double.parseDouble(str.substring(j));
		}
		return arrDouble;
	}
    public static String arrayToString(Object[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(array.length * 7);
        sb.append("('");
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append("', '");
            sb.append(array[i]);
        }
        sb.append("')");
        return sb.toString();
    }
	public static void writeData(String fileName, double[] dulieu)
	{
		try {
			String sdcard=Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+fileName;
			FileOutputStream out=new FileOutputStream(sdcard);
			OutputStreamWriter writer= new OutputStreamWriter(out);
			for(int i=0;i<dulieu.length;i++){
				writer.write(i+" "+dulieu[i]+"\n");
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/*
	public static Thread SyncUpdatedData(Context context,Patient patient)
    {
		final Context con = context;
		final Patient pt = patient;
		Thread sync = new Thread(new Runnable() {
			@Override
			public void run() {
				ServiceHandler serviceHandler = new ServiceHandler(con);
				if(serviceHandler.isNetworkOnline())
				{
					DatabaseHandler database = new DatabaseHandler(con);
					String[] timeList = database.getSyncQueue(DatabaseHandler.ACTION_UPDATE);
					if(timeList!=null)
					{
						HistoryItem[] items = database.getAllHistoryItem(timeList);
						if(items!=null)
						{
							for(int i=0;i<items.length;i++)
							{
								List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
								nameValuePairs.add(new BasicNameValuePair("username", pt.getUsername()));
								nameValuePairs.add(new BasicNameValuePair("time", items[i].getTime()));
								nameValuePairs.add(new BasicNameValuePair("data", items[i].getData()));
								nameValuePairs.add(new BasicNameValuePair("heartrate", items[i].getHeartRate()+""));
								nameValuePairs.add(new BasicNameValuePair("state", items[i].getState()));
								nameValuePairs.add(new BasicNameValuePair("note", items[i].getNote()));
								String resultStr = serviceHandler.makeServiceCall(ServiceHandler.serverUrl+"?action=history-update", ServiceHandler.POST,
										nameValuePairs);
								if(resultStr!=null)
								{
									Log.d("MY_TAG", "Update Response > " + resultStr);
									if (resultStr.compareToIgnoreCase(ServiceHandler.SUCCESS_CODE)==0)
									{
										database.deleteQueue(items[i].getTime());
									}
								}
							}
						}
					}
					database.close();
				}
			}
		});
		return sync;
    }
	public static Thread SyncDeletedData(Context context,Patient patient)
    {
		final Context con = context;
		final Patient pt = patient;
		Thread sync = new Thread(new Runnable() {
			@Override
			public void run() {
				ServiceHandler serviceHandler = new ServiceHandler(con);
				if(serviceHandler.isNetworkOnline())
				{
					DatabaseHandler database = new DatabaseHandler(con);
					String[] timeList = database.getSyncQueue(DatabaseHandler.ACTION_DELETE);
					if(timeList!=null)
					{

							for(int i=0;i<timeList.length;i++)
							{
								List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
								nameValuePairs.add(new BasicNameValuePair("username", pt.getUsername()));
								nameValuePairs.add(new BasicNameValuePair("time", timeList[i]));
								String resultStr = serviceHandler.makeServiceCall(ServiceHandler.serverUrl+"?action=history-delete", ServiceHandler.POST,
										nameValuePairs);
								if(resultStr!=null)
								{
									Log.d("MY_TAG", "Delete Response > " + resultStr);
									if (resultStr.compareToIgnoreCase(ServiceHandler.SUCCESS_CODE)==0)
									{
										database.deleteQueue(timeList[i]);
									}
								}
							}
					}
					database.close();
				}
			}
		});
		return sync;
    }*/
}
