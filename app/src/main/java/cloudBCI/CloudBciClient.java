package cloudBCI;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CloudBciClient extends Thread{
    String baseUrl = "";
    OkHttpClient client;
    final static int accessingPeriod = 20;
    List<GetRequest> requestQueue;
    String[] dataBuffer = {"", "", "", ""};

    public CloudBciClient(String baseUrl)
    {
        this.baseUrl = baseUrl;
        requestQueue = new ArrayList<GetRequest>();
        this.client = new OkHttpClient();
        start();
    }


    public void run(){
        while(true) {
            Log.d("Queue Size:", " " + requestQueue.size());
            dataQueueToRequest();
            for (int i = 0; i < requestQueue.size(); i++) {
                GetRequest current_request = requestQueue.get(i);
                current_request.run();
            }

            for(int i = requestQueue.size() - 1; i>= 0; i--)
            {
                if(requestQueue.get(i).state == GetRequest_State.FINISHED)
                {
                    requestQueue.remove(i);
                }
            }
        }
    }

    public void insert_real_time_data(int channel, String data)
    {
        if(dataBuffer[channel-1].equals(""))
        {
            dataBuffer[channel-1] = data;
        }
        else {
            dataBuffer[channel-1] = dataBuffer[channel-1] + ";" + data;
        }
    }

    private void dataQueueToRequest(){
        for(int i = 0; i < dataBuffer.length; i++) {
            if (!dataBuffer[i].equals("")) {
                int attribute_index = i + 1;
                String url = baseUrl + "/insert_real_time_data?case_id=1&attribute=data" +
                                attribute_index + "&data=" + dataBuffer[i];
                Log.d("url", url);
                addRawRequest(url);
                dataBuffer[i] = "";
            }
        }
    }

    public void addRawRequest(String url)
    {
        requestQueue.add(new GetRequest(url, client));
    }
}
