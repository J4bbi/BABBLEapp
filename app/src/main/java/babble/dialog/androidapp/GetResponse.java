package babble.dialog.androidapp;

import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;

import babble.dialog.remote.*;


/**
 * Created by jabbi on 06/03/17.
 */

public class GetResponse extends Thread {
    private Client c;

    private MainActivity m = new MainActivity();
    public GetResponse(Client c)    {
        this.c = c;
    }


    public void run(){
        while(true) {
            try {
                ClientResponse cr = c.getResponse();

                if(cr != null)  {
                    ArrayList<String> content = new ArrayList<String>();
                    content.add(cr.toString());

                    //m.buildToSpeech(content, false);

                    Log.v("Threaded response", cr.toString());
                }

            }
            catch(ClientDisconnectedException cde)  {
                Log.e("No Connection", "Client unable to connect to server.");
            } catch (NullPointerException npe)	{
                Log.e("No Connection", "Null. Client unable to connect to server.");
            }

        }
    }
}