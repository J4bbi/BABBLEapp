package babble.dialog.androidapp;

/*
  Class to launch an Android app to communicate with BABBLE via a TCP client/server architechture

  @author Hrafn Malmquist - hrafn.malmquist@gmail.com, ?.
 */

import babble.dialog.remote.Client;
import babble.dialog.remote.ClientDisconnectedException;
import babble.dialog.remote.ClientResponse;

import java.util.ArrayList;
import java.util.Locale;

import android.os.Build;
import android.os.Bundle;
import android.app.Activity;

import android.content.Intent;

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
    public Activity currentView;
    public TextView UserSpeech, System, BABBLESpeech;
    // ASR & TTS
    SpeechRecognizer babbleRecognizer;
    TextToSpeech babbleTTS;
    // Network
    Thread clientThread;
    Client c;
    Runnable runnableClient;
    // Instance data variables
    private ArrayList<String> aggregated_utterances = new ArrayList<String>();
    private boolean isTTSready = false;
    static final String DOMAIN = "bruntonross.co.uk";//192.168.1.42"; //192.168.44.212";//192.168.0.10";

    // Method that calls appropriate TTS method depending on device's API version
    public void speak(String spokenLine) {
        if (isTTSready) {
            if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                babbleTTS.speak(spokenLine, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                babbleTTS.speak(spokenLine, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    // Method to create "pretty" output texts for the TextViews
    public void buildToSpeech(ArrayList<String> content, String agent)    {
        String aggregatedUtterances = "";

        // Build up a sentence to print out
        for(String str: content){
            if(!str.equals(""))
                aggregatedUtterances += str.trim() + " ";
            else
                Log.v("BABBLE-apps", "String is empty");
        }

        if(!aggregatedUtterances.equals("")) {
            SpannableStringBuilder sb = new SpannableStringBuilder(agent + ": " + aggregatedUtterances);

            // create a bold StyleSpan to be used on the SpannableStringBuilder
            StyleSpan b = new StyleSpan(android.graphics.Typeface.BOLD);

            // set only the name part of the SpannableStringBuilder to be bold --> first n characters
            sb.setSpan(b, 0, agent.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

            if(agent.equals("BABBLE")) // || agent.equals("ERROR"))
                BABBLESpeech.setText(sb);
            else
                UserSpeech.setText(sb);

        }
    }

    // Method that returns a new runnable with a new client allowing for
    // a reset functionality.
    private Runnable getRunnableClient()  {
        // Return a runnable to be run inside a thread
        return new Runnable() {
            private String sysMessage = "";

            public void run() {
                try {
                    c = new Client(DOMAIN);

                }
                catch(ClientDisconnectedException cde)  {
                    Log.e("BABBLE-app", "No Connection\nClient unable to connect to server.\n" + cde.getMessage());
                    System.setText("Client unable to connect to server");

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

                            }
                        }
                    }
                } catch(ClientDisconnectedException e) {
                    System.setText("Client unable to connect to server");
                    Log.e("BABBLE-app", "No Connection\nClient unable to connect to server.\n" + e.getMessage());
                }

            }
        };
    }

    // Method that return a listener with customised events
    private RecognitionListener getBABBLEListener() {
        // Events docs at: https://developer.android.com/reference/android/speech/RecognitionListener.html
        return new RecognitionListener() {
            // If a single word is uttered no partial results get sent, only (final) results
            private Boolean partialResultsSent = false;

            @Override
            public void onReadyForSpeech(Bundle params) {
                // Let user know he can begin speaking
                System.setText("Ready");
            }

            // The user has started to speak.
            @Override
            public void onBeginningOfSpeech() {
                Log.v("BABBLE-app LISTENER", "Speech started");
                // Clear textview for incoming words
                UserSpeech.setText("");
            }

            // The sound level in the audio stream has changed.
            // gets called many times
            @Override
            public void onRmsChanged(float rmsdB) {
                //Log.v("LISTENER", "Sound level has changed: " +String.valueOf(rmsdB));
            }

            // More sound has been received. (does not seem to get called)
            @Override
            public void onBufferReceived(byte[] buffer) {
                Log.v("LISTENER", "More sound has been received.");

            }

            // Called after the user stops speaking
            @Override
            public void onEndOfSpeech() {
                Log.v("BABBLE-app LISTENER", "Speech ended.");
                // Stop listening
                babbleRecognizer.stopListening();
                Log.v("BABBLE-app EVENT", "Not listening");
                System.setText("Speech ended");

            }

            // A network or recognition error occurred.
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
                // Output error message to user
                System.setText(errorMsg);
                // Listening won't be successful so we can destroy recognizer
                babbleRecognizer.destroy();

            }

            // Called when recognition results are ready.
            @Override
            public void onResults(Bundle results) {
                // Results bundle passed contains an unordered string array of possible utterances
                // and float array of likelihood of each utterance.
                // Here we pick the most likely
                // strictly speaking this is only necessary if partialresults haven't been sent
                // but is included here inorder to log them regardless
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
                // results have to be sent now
                if(!partialResultsSent) {
                    String res_msg = results.getStringArrayList("results_recognition").get(floatIndex);
                    Log.v("BABBLE-app", "onresults sending: " + res_msg);
                    try {
                        c.sendUtterance(res_msg);
                    }catch (ClientDisconnectedException cde) {
                        Log.e("No Connection", "Client unable to connect to server.");
                    }catch (NullPointerException npe) {
                        Log.e("No connection", "Client not initialised.");
                    }
                }

                // Release turn
                try {
                    c.sendUtterance("<rt>");

                } catch (ClientDisconnectedException e) {
                    e.printStackTrace();
                }

                // Utterance finished and turn released so destroy recognizer
                babbleRecognizer.destroy();

                Log.v("BABBLE-app - Results", results.keySet().toString());
                Log.v("BABBLE-app", " - Results recognition: " + results.get("results_recognition").toString());
                Log.v("BABBLE-app", " - Confidence scores: " + confidenceScores);
            }

            // Called when partial recognition results are available.
            // this will happen if utterance is more than two words
            @Override
            public void onPartialResults(Bundle partialResults) {
                // Disregard empty partial results
                if (!partialResults.getStringArrayList("results_recognition").get(0).equals("")) {
                    // Current word count of utterance
                    int wordCount = aggregated_utterances.size();
                    Log.v("BABBLE-app", "Count:" + String.valueOf(wordCount));

                    String[] results = partialResults.getStringArrayList("results_recognition").get(0).split(" ");
                    Log.v("BABBLE-app", "Agg: " + String.valueOf(wordCount) + " Part: " + results.length + "\nResults: " + partialResults.getStringArrayList("results_recognition").get(0));

                    // Iterate through new words arriving
                    for (int i = wordCount; i < results.length; i++) {
                        aggregated_utterances.add(results[i]);

                        try {
                            Log.v("BABBLE-app", "utterance sent: " + results[i]);
                            c.sendUtterance(results[i]);

                        } catch (ClientDisconnectedException cde) {
                            Log.e("No Connection", "Client unable to connect to server.");
                        } catch (NullPointerException npe) {
                            Log.e("No connection", "Client not initialised.");
                        }

                        Log.v("BABBLE-app", "added word: " + results[i]);
                    }

                    // If new words have been added, print out
                    if (wordCount < aggregated_utterances.size()) {
                        buildToSpeech(aggregated_utterances, "User");
                        partialResultsSent = true;
                        Log.v("BABBLE-app", "Partial results: " + partialResults.keySet().toString());
                        //Log.v("BABBLE-app", "Partres recognition: " + res_msg);

                    }

                    Log.v("Unstable text", partialResults.get("android.speech.extra.UNSTABLE_TEXT").toString());
                }

            }

            // Reserved for adding future events.
            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        };
    }

    // Runs when app is opened
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Prevent screen timeout during run

        // Buttons
        Button speak = (Button) findViewById(R.id.speak);
        Button reset = (Button) findViewById(R.id.reset);

        // TextViews
        System = (TextView)findViewById(R.id.system);

        UserSpeech = (TextView)findViewById(R.id.speech);
        BABBLESpeech = (TextView)findViewById(R.id.sysspeech);

        // Initialize TTS object.
        babbleTTS = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    babbleTTS.setLanguage(Locale.UK);
                    isTTSready = true;
                    Log.v("TTS", "Initialised.");
                }
            }
        });

        // Allow app to run threads
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Thread initialisation
        runnableClient = getRunnableClient();
        clientThread = new Thread(runnableClient);
        clientThread.start();

        currentView = this;

        // Listener for speak button
        speak.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Provide that a connection has been made
                if(c != null && c.isConnected()) {
                    // Tell user we are initialising
                    System.setText("Initialising");
                    // Array list to accumulate utterance
                    aggregated_utterances = new ArrayList<String>();
                    // Initialise SpeechRecognizer object
                    babbleRecognizer = SpeechRecognizer.createSpeechRecognizer(currentView);
                    // Initialise RecognitionListener
                    RecognitionListener babbleListener = getBABBLEListener();
                    Log.v("Listener", babbleListener.toString());
                    // Assign listener to speechrecognizer
                    babbleRecognizer.setRecognitionListener(babbleListener);

                    // Constants from https://developer.android.com/reference/android/speech/RecognizerIntent.html
                    // Starts an activity that will prompt the user for speech and send it through a speech recognizer.
                    Intent babbleIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    // Informs the recognizer which speech model to prefer when performing ACTION_RECOGNIZE_SPEECH.
                    // Use a language model based on free-form speech recognition.
                    babbleIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    // A float array of confidence scores of the recognition results when performing ACTION_RECOGNIZE_SPEECH.
                    babbleIntent.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true);
                    // Optional boolean to indicate whether partial results should be returned by the recognizer as the user speaks (default is false).
                    // This allows for incrementality!
                    babbleIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

                    babbleRecognizer.startListening(babbleIntent);
                }


            }

        });

        // Listener for reset button
        reset.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // No use resetting if the thread isn't running
                if(clientThread.isAlive()) {
                    // Notifying user of restart
                    System.setText("Restarting");
                    // Destroying client
                    c = null;
                    // Kill thread
                    clientThread.interrupt();
                    clientThread = null;
                    // Re-instantiate runnable (includes new connection)
                    runnableClient = getRunnableClient();
                    // Re-instantiate thread with runnable
                    clientThread = new Thread(runnableClient);
                    // Empty textboxes of earlier dialogue
                    BABBLESpeech.setText("");
                    UserSpeech.setText("");
                    // Start new thread
                    clientThread.start();
                    System.setText("Ready");
                }

            }

        });
    }

}