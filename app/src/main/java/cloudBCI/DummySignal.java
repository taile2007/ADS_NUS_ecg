package cloudBCI;

import android.util.Log;

public class DummySignal extends Thread{
    @Override
    public void run()
    {
        CloudBciClient client = new CloudBciClient("http://cloud-bci.duckdns.org");
        int count = 1;

        while(true) {
            try {
                this.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(count > 255)
            {
                count = 1;
            }
            else
            {
                count += 1;
            }

            Log.d("Dummy Signal:", " " + count);
            int data1 = count;
            String data1_str = data1 + "";
            client.insert_real_time_data(1, data1_str);
            int data2 = count % 128 * 2;
            String data2_str = data2 + "";
            client.insert_real_time_data(2, data2_str);
            int data3 = count % 64 * 4;
            String data3_str = data3 + "";
            client.insert_real_time_data(3, data3_str);
            int data4 = count % 32 * 8;
            String data4_str = data4 + "";
            client.insert_real_time_data(4, data4_str);
        }
    }
}
