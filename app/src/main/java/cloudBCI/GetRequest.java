package cloudBCI;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

enum GetRequest_State{
    NEW, FINISHED, ERROR;
}

public class GetRequest{
    String url = "";
    String response;
    OkHttpClient client;
    GetRequest_State state;

    public GetRequest(String url, OkHttpClient client)
    {
        this.url = url;
        this.client = client;
        state = GetRequest_State.NEW;
    }

    String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    public void run()
    {
        try {
            response = get(url);
        } catch (IOException e) {
            e.printStackTrace();
            state = GetRequest_State.ERROR;
        }
        state = GetRequest_State.FINISHED;
    }

    public String getResponse() {
        return response;
    }

    public static String blockingGetRequest(String url) throws InterruptedException {
        OkHttpClient client = new OkHttpClient();
        GetRequest g = new GetRequest(url, client);
        g.run();
        return g.getResponse();
    }
}
