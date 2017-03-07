package babble.dialog.androidapp;

import babble.dialog.remote.Client;
import babble.dialog.remote.ClientDisconnectedException;

import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;

import android.content.Intent;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.os.StrictMode;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE = 1234;
    public Activity currentView;
    Button Start;
    public TextView Speech, System;
    SpeechRecognizer babbleRecognizer;
    Intent babbleIntent;
    Client c;
    private ArrayList<String> aggregated_utterances = new ArrayList<String>();

    public void buildToSpeech(ArrayList<String> content, boolean isUser)    {
        String aggregatedUtterances = "";

        for(String str: content){
            if(str != "")
                aggregatedUtterances += str + " ";
        }

        if(aggregatedUtterances != "") {
            //Log.v("AggUtt", content.toString());
            Log.v("AggUtt", aggregatedUtterances);
            // a SpannableStringBuilder containing text to display
            SpannableStringBuilder sb = new SpannableStringBuilder("User: " + aggregatedUtterances.substring(0, aggregatedUtterances.length() - 1));

            // create a bold StyleSpan to be used on the SpannableStringBuilder
            StyleSpan b = new StyleSpan(android.graphics.Typeface.BOLD);

            // set only the name part of the SpannableStringBuilder to be bold --> first 5 characters
            sb.setSpan(b, 0, 5, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

            if(Speech != null)
                Speech.setText(sb);

            //currentView.findViewById(R.id.speech);

            //Log.v("Sp", ((TextView)findViewById(R.id.speech)).toString());

            //Speech.setText(sb);
        }
    }

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Start = (Button)findViewById(R.id.start_reg);
        Speech = (TextView)findViewById(R.id.speech);
        System = (TextView)findViewById(R.id.system);


        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            c = new Client("bruntonross.co.uk");
            //Log.v("CLIENT", c.toString());
            //GetResponse gr = new GetResponse(c);
            //runOnUiThread(new GetResponse(c));
            //gr.start();
            //c.sendUtterance("a");
        }
        catch(ClientDisconnectedException cde)  {
            Log.e("No Connection", "Client unable to connect to server.\n" + cde.getMessage());
        }


        currentView = this;

        //aggregated_utterances.clear();

        Log.v("Going", "1");

        Start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("BUTTON", "Listening");

                System.setText("Initialising");


                babbleRecognizer = SpeechRecognizer.createSpeechRecognizer(currentView);
                Log.v("Speechrecog", babbleRecognizer.toString());

                RecognitionListener babbleListener = new RecognitionListener() {

                    public void checkNull() {
                        if(Speech == null)
                            Log.v("Null", "Is still null");

                    }

                    @Override
                    public void onReadyForSpeech(Bundle params) {
                        System.setText("Ready");

                    }

                    @Override
                    public void onBeginningOfSpeech() {
                        Log.v("LISTENER", "Speech started");

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
                        Log.v("LISTENER", "Speech ended.");
                        babbleRecognizer.stopListening();
                        Log.v("EVENT", "Not listening");
                        System.setText("Speech ended");

                    }


                    @Override
                    public void onError(int error) {
                        String errorMsg = "";

                        // Error codes from SpeechRecognizer
                        // https://developer.android.com/reference/android/speech/SpeechRecognizer.html

                        switch (error) {
                            case 3:  errorMsg = "Audio recording error.";
                                break;
                            case 5:  errorMsg = "Other client side errors.";
                                break;
                            case 9:  errorMsg = "Insufficient permissions.";
                                break;
                            case 2:  errorMsg = "Other network related errors.";
                                break;
                            case 1:  errorMsg = "Network operation timed out.";
                                break;
                            case 7:  errorMsg = "No recognition result matched.";
                                break;
                            case 8:  errorMsg = "RecognitionService busy.";
                                break;
                            case 4:  errorMsg = "Server sends error status.";
                                break;
                            case 6:  errorMsg = "No speech input.";
                                break;
                        }
                        Log.v("LISTENER ERROR", errorMsg);
                        System.setText(errorMsg);

                        babbleRecognizer.destroy();

                    }

                    @Override
                    public void onResults(Bundle results) {
                        String confidenceScores = "";
                        for(float i: results.getFloatArray("confidence_scores") ) {
                            confidenceScores += String.valueOf(i) + " ";
                        }

                        Log.v("Results", results.keySet().toString());
                        Log.v("Results recognition", results.get("results_recognition").toString());
                        Log.v("Confidence scores", confidenceScores);
                        babbleRecognizer.destroy();
                    }

                    @Override
                    public void onPartialResults(Bundle partialResults) {
                        int newCount = 0;
                        //if(aggregated_utterances != null)   {
                        newCount = aggregated_utterances.size();
                        Log.v("Agg", String.valueOf(newCount));
                        //}

                        String[] results =  partialResults.getStringArrayList("results_recognition").get(0).split(" ");
                        String res_msg = "";
                        Log.v("Agg", "Agg: " + String.valueOf(newCount) + " Part: " + results.length);

                        for(int i = newCount; i < results.length; i++){
                            aggregated_utterances.add(results[i]);
                            res_msg += results[i] + " ";
                            Log.v("Aggadd", results[i]);
                        }


                        //Speech.setText(aggregateUtterances());
                        buildToSpeech(aggregated_utterances, true);
                        Log.v("Partial results", partialResults.keySet().toString());
                        Log.v("Partres recognition", res_msg);

                        try {
                            c.sendUtterance(res_msg);
                        }
                        catch(ClientDisconnectedException cde)  {
                            Log.e("No Connection", "Client unable to connect to server.");
                        }
                        catch(NullPointerException npe) {
                            Log.e("No connection", "Client not initialised.");
                        }

                        Log.v("Unstable text", partialResults.get("android.speech.extra.UNSTABLE_TEXT").toString());
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

        });

    }

}