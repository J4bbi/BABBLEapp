package babble.dialog.androidapp;

import babble.dialog.remote.Client;
import babble.dialog.remote.ClientDisconnectedException;
import babble.dialog.remote.ClientResponse;

import java.util.ArrayList;
import java.util.Locale;

import android.os.Build;
import android.os.Bundle;
import android.app.Activity;

import android.content.Intent;

import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.os.StrictMode;
import android.speech.tts.TextToSpeech;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE = 1234;
    public Activity currentView;
    Button Start, Restart;
    public TextView Speech, System,sysspeech;
    SpeechRecognizer babbleRecognizer;
    Thread thread;
    Intent babbleIntent;
    Client c;
    TextToSpeech utterance;
    private ArrayList<String> aggregated_utterances = new ArrayList<String>();
    public boolean isTTSready, isTalking, noMore = false;
    static final String DOMAIN = "192.168.0.10";
    static final int TURN_DELAY = 5; // Set to 5 seconds because onResults is particularly slow

    // Zhu's delay method to allow for continuous turn taking
    public void delay(int seconds) {
        final int milliseconds = seconds * 1000;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        babbleRecognizer.destroy();

                        if(!noMore)
                            Start.callOnClick();
                        else    {
                            System.setText("Dialogue ended");
                            Log.v("BABBLE-app", "No more turns.");
                        }
                    }
                }, milliseconds);
            }
        });
    }

    // Method that calls appropriate TTS method depending on device's API version
    public void speak(String spokenLine) {
        if (isTTSready) {
            if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                utterance.speak(spokenLine, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                utterance.speak(spokenLine, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    // Method to create "pretty" output texts for the TextViews
    public void buildToSpeech(ArrayList<String> content, String agent)    {
        String aggregatedUtterances = "";

        for(String str: content){
            if(!str.equals(""))
                aggregatedUtterances += str.trim() + " ";
        }

        if(!aggregatedUtterances.equals("")) {
            SpannableStringBuilder sb = new SpannableStringBuilder(agent + ": " + aggregatedUtterances.substring(0, aggregatedUtterances.length() - 1));

            // create a bold StyleSpan to be used on the SpannableStringBuilder
            StyleSpan b = new StyleSpan(android.graphics.Typeface.BOLD);

            // set only the name part of the SpannableStringBuilder to be bold --> first 5 characters
            sb.setSpan(b, 0, agent.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

            if(agent.equals("BABBLE") || agent.equals("ERROR"))
                sysspeech.setText(sb);
            else
                Speech.setText(sb);

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Prevent screen timeout during run

        Start = (Button)findViewById(R.id.start_reg);
        Restart = (Button)findViewById(R.id.restart);
        Speech = (TextView)findViewById(R.id.speech);
        System = (TextView)findViewById(R.id.system);
        sysspeech = (TextView)findViewById(R.id.sysspeech);

        // Initializes textToSpeech object.
        utterance = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    utterance.setLanguage(Locale.UK);
                    isTTSready = true;
                    Log.v("TTS", "Initialised.");
                }
            }
        });

        // Permissions to run threads on app
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Thread initialisation
        final Runnable runnable = new Runnable() {
             private String sysMessage = "";

             public void postError(final String msg) {
                 runOnUiThread(new Runnable() {
                     @Override

                     public void run() {
                         ArrayList<String> al = new ArrayList<String>(1);
                         al.add(msg);
                         buildToSpeech(al, "ERROR");
                     }
                 });
             }

             public void run() {
                 try {
                     c = new Client(DOMAIN);

                 }
                 catch(ClientDisconnectedException cde)  {
                     Log.e("BABBLE-app", "No Connection\nClient unable to connect to server.\n" + cde.getMessage());
                     postError(cde.getMessage());
                 }

                try {
                    while(c != null && c.isConnected()) {

                        final ClientResponse r = c.getResponse();

                        if(r != null) {
                            Log.v("BABBLE-app", r.toString());
                            if(r.getSpeaker().equals("sys")) {

                                if (r.getWord().equals("<rt>") || r.getWord().equals("bye")) {

                                    runOnUiThread(new Runnable() {
                                        @Override

                                        public void run() {
                                            // TODO Auto-generated method stub
                                            Log.v("BABBLE-app", "serverMessage" + sysMessage);

                                            ArrayList<String> sysMsg = new ArrayList<String>();
                                            sysMsg.add(sysMessage);

                                            if(r.getWord().equals("bye")) sysMsg.add("bye");

                                            buildToSpeech(sysMsg, "BABBLE");
                                            speak(sysMessage);
                                            sysMessage = "";
                                        }
                                    });

                                } else {
                                    Log.v("BABBLE-app - server r",r.toString());
                                    sysMessage += " " + r.getWord();

                                }

                                if(r.getWord().equals("bye") || r.getWord().equals("look"))
                                    noMore = true;
                            }
                        }
                    }
                } catch(ClientDisconnectedException e) {
                   postError(e.getMessage());
                }

            }
        };
        thread = new Thread(runnable);
        thread.start();

        currentView = this;

        Start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(c != null && c.isConnected()) {
                    System.setText("Initialising");
                    noMore = false;

                    aggregated_utterances = new ArrayList<String>();
                    babbleRecognizer = SpeechRecognizer.createSpeechRecognizer(currentView);

                    RecognitionListener babbleListener = new RecognitionListener() {

                        private Boolean partialResultsSent = false;

                        @Override
                        public void onReadyForSpeech(Bundle params) {
                            System.setText("Ready");

                        }

                        @Override
                        public void onBeginningOfSpeech() {
                            Log.v("LISTENER", "Speech started");
                            Speech.setText("");
                        }

                        @Override
                        public void onRmsChanged(float rmsdB) {
                            //Log.v("LISTENER", "Sound level has changed: " +String.valueOf(rmsdB));
                        }

                        @Override
                        public void onBufferReceived(byte[] buffer) {
                            Log.v("LISTENER", "More sound has been received.");

                        }

                        @Override
                        public void onEndOfSpeech() {
                            Log.v("BABBLE-app LISTENER", "Speech ended.");
                            babbleRecognizer.stopListening();
                            Log.v("BABBLE-app EVENT", "Not listening");
                            System.setText("Speech ended");

                        }

                        @Override
                        public void onError(int error) {
                            String errorMsg = "";

                            // Error codes from SpeechRecognizer
                            // https://developer.android.com/reference/android/speech/SpeechRecognizer.html

                            switch (error) {
                                case 3:
                                    errorMsg = "Audio recording error.";
                                    break;
                                case 5:
                                    errorMsg = "Other client side errors.";
                                    break;
                                case 9:
                                    errorMsg = "Insufficient permissions.";
                                    break;
                                case 2:
                                    errorMsg = "Other network related errors.";
                                    break;
                                case 1:
                                    errorMsg = "Network operation timed out.";
                                    break;
                                case 7:
                                    errorMsg = "No recognition result matched.";
                                    break;
                                case 8:
                                    errorMsg = "RecognitionService busy.";
                                    break;
                                case 4:
                                    errorMsg = "Server sends error status.";
                                    break;
                                case 6:
                                    errorMsg = "No speech input.";
                                    break;
                            }
                            Log.e("BABBLE-app LIST. ERROR", errorMsg);
                            System.setText(errorMsg);

                            babbleRecognizer.destroy();
                            //Start.callOnClick();

                        }

                        @Override
                        public void onResults(Bundle results) {
                            String confidenceScores = "";
                            Float hiFloat = 0f;
                            float[] floats = results.getFloatArray("confidence_scores");
                            int floatIndex = 0;

                            for (int i = 0; i < floats.length; i++) {
                                confidenceScores += String.valueOf(floats[i]) + " ";

                                if(floats[i] > hiFloat) {
                                    hiFloat = floats[i];
                                    floatIndex = i;
                                }
                            }

                            // If partial results haven't been sent then
                            // they have to be sent now

                            if(!partialResultsSent) {
                                String res_msg = results.getStringArrayList("results_recognition").get(floatIndex);
                                Log.v("BABBLE-app", "onresults sending: " + res_msg);
                                try {
                                    c.sendUtterance(res_msg);
                                }catch (ClientDisconnectedException cde) {
                                    Log.e("No Connection", "Client unable to connect to server.");
                                } catch (NullPointerException npe) {
                                    Log.e("No connection", "Client not initialised.");
                                }
                            }

                            // Release turn
                            try {
                                c.sendUtterance("<rt>");
                                if(isTalking) {
                                    delay(TURN_DELAY);
                                }
                            } catch (ClientDisconnectedException e) {
                                e.printStackTrace();
                            }

                            babbleRecognizer.destroy();

                            Log.v("BABBLE-app - Results", results.keySet().toString());
                            Log.v("BABBLE-app", " - Results recognition: " + results.get("results_recognition").toString());
                            Log.v("BABBLE-app", " - Confidence scores: " + confidenceScores);
                        }

                        @Override
                        public void onPartialResults(Bundle partialResults) {
                            // Disregard empty partial results
                            if (!partialResults.getStringArrayList("results_recognition").get(0).equals("")) {
                                int newCount = aggregated_utterances.size();
                                Log.v("BABBLE-app", "Count:" + String.valueOf(newCount));

                                String[] results = partialResults.getStringArrayList("results_recognition").get(0).split(" ");
                                String res_msg = "";
                                Log.v("BABBLE-app", "Agg: " + String.valueOf(newCount) + " Part: " + results.length + "\nResults: " + partialResults.getStringArrayList("results_recognition").get(0));

                                for (int i = newCount; i < results.length; i++) {
                                    aggregated_utterances.add(results[i]);

                                    res_msg += results[i] + " ";
                                    Log.v("BABBLE-app", "added word: " + results[i]);
                                }

                                // If there's anything new to report. (But why if partialresults != null?)
                                if (!res_msg.equals("")) {
                                    buildToSpeech(aggregated_utterances, "User");
                                    Log.v("BABBLE-app", "Partial results: " + partialResults.keySet().toString());
                                    Log.v("BABBLE-app", "Partres recognition: " + res_msg);

                                    try {
                                        if(!res_msg.equals("I"))
                                            res_msg = res_msg.toLowerCase();

                                        Log.v("BABBLE-app", "utterance sent: " + res_msg);
                                        c.sendUtterance(res_msg);
                                        partialResultsSent = true;
                                        isTalking = true;
                                    } catch (ClientDisconnectedException cde) {
                                        Log.e("No Connection", "Client unable to connect to server.");
                                    } catch (NullPointerException npe) {
                                        Log.e("No connection", "Client not initialised.");
                                    }
                                }

                                Log.v("Unstable text", partialResults.get("android.speech.extra.UNSTABLE_TEXT").toString());
                            }

                        }

                        @Override
                        public void onEvent(int eventType, Bundle params) {
                            Log.v("LISTENER", "an event.");

                        }
                    };

                    Log.v("Listener", babbleListener.toString());
                    babbleRecognizer.setRecognitionListener(babbleListener);

                    babbleIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    babbleIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    babbleIntent.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true);
                    babbleIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

                    babbleRecognizer.startListening(babbleIntent);
                }


            }

        });

        Restart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if(thread.isAlive())    {
                    System.setText("Restarting");
                    thread.interrupt();
                    thread = new Thread(runnable);
                    Start.callOnClick();

                }

            }

        });
    }

}