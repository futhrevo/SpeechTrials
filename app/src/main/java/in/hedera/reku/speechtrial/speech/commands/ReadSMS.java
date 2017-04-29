package in.hedera.reku.speechtrial.speech.commands;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.List;

import in.hedera.reku.speechtrial.R;
import in.hedera.reku.speechtrial.speech.SpeechRecognitionUtil;
import in.hedera.reku.speechtrial.speech.text.match.WordMatcher;
import in.hedera.reku.speechtrial.speech.voiceaction.OnNotUnderstoodListener;
import in.hedera.reku.speechtrial.speech.voiceaction.OnUnderstoodListener;
import in.hedera.reku.speechtrial.speech.voiceaction.VoiceActionExecutor;
import in.hedera.reku.speechtrial.speech.voiceaction.VoiceAlertDialog;

import static in.hedera.reku.speechtrial.actions.SimpleSmsReceiver.INCOMING_SMS_INTENT_MESSAGE;
import static in.hedera.reku.speechtrial.actions.SimpleSmsReceiver.INCOMING_SMS_INTENT_SENDER;
import static in.hedera.reku.speechtrial.actions.SimpleSmsReceiver.INCOMING_SMS_NUMBER;
import static in.hedera.reku.speechtrial.speech.SpeechRecognitionUtil.diagnoseErrorCode;

/**
 * Created by rakeshkalyankar on 21/04/17.
 */

public class ReadSMS implements RecognitionListener {

    public static final String TAG = ReadSMS.class.getSimpleName();
    private WordMatcher match;

    private Context context;
    private VoiceActionExecutor executor;
    private boolean relaxed;

    private String sender;
    private String message;
    private String number;
    private SpeechRecognizer recognizer;
    private String replymessage;
    private boolean isReplyable;

    public ReadSMS(Context context, VoiceActionExecutor executor, boolean relaxed, final Intent intent) {

        this.context = context;
        this.executor = executor;
        this.relaxed = relaxed;
        sender = intent.getStringExtra(INCOMING_SMS_INTENT_SENDER);
        message = intent.getStringExtra(INCOMING_SMS_INTENT_MESSAGE);
        number = intent.getStringExtra(INCOMING_SMS_NUMBER);
        isReplyable = number.matches("^\\+91.*$");
        Log.d(TAG, "number is " + number + " " + isReplyable);
        interpret();
    }

    public void interpret() {
        String[] yesWords = context.getResources().getStringArray(R.array.read_SMS_command);

        final VoiceAlertDialog confirmDialog = new VoiceAlertDialog(context, yesWords, null, null);
        // add listener for positive response
        // use relaxed matching to increase chance of understanding user
        confirmDialog.addRelaxedPositive(new OnUnderstoodListener() {
            @Override
            public void understood() {
                Log.d(TAG, "READ!: SMS");
                executor.addSpeech(message);
                if(isReplyable){
                    askreplySMS();
                }

            }
        });

        confirmDialog.addNegative(new OnUnderstoodListener() {
            @Override
            public void understood() {
                Log.d(TAG, "Dont Read");
                String toSayCancelled = "Ok not reading it";
                executor.speak(toSayCancelled);
            }
        });
        confirmDialog.addNeutral(new OnUnderstoodListener() {
            @Override
            public void understood() {
                Log.d(TAG, "Ask again");
                executor.reExecute("Didn't understand it Well, Try again");
            }
        });
        //prompt for the confirm VoiceAction
        String toSay = "You got a new SMS from "+ sender + " . Do you want me to read it?";
        confirmDialog.setPrompt(toSay);
        confirmDialog.setSpokenPrompt(toSay);

        // if the user says anything else besides the yes words cancel
        confirmDialog.setNotUnderstood(new OnNotUnderstoodListener() {
            @Override
            public void notUnderstood(List<String> heard, int reason) {
                String toSayCancelled = "Didn't understand it well, Try later";
                executor.speak(toSayCancelled);
            }
        });
        executor.execute(confirmDialog);
    }

    public void askreplySMS() {
        String toSay = "Do you want to dictate a reply?";

        final VoiceAlertDialog replyDialog = new VoiceAlertDialog(context);
        //prompt for the confirm VoiceAction

        replyDialog.setPrompt(toSay);
        replyDialog.setSpokenPrompt(toSay);

        replyDialog.addRelaxedPositive(new OnUnderstoodListener() {
            @Override
            public void understood() {
                Log.d(TAG, "Start to reply SMS");
                executor.speak("start");
                startDictation("Do you want to dictate a reply?");

            }
        });
        replyDialog.addNegative(new OnUnderstoodListener() {
            @Override
            public void understood() {
                Log.d(TAG, "Dont Reply");
                String toSayCancelled = "Ok not replying it";
                executor.speak(toSayCancelled);
            }
        });
        replyDialog.addNeutral(new OnUnderstoodListener() {
            @Override
            public void understood() {
                Log.d(TAG, "Ask again");
                executor.reExecute("Didn't understand it Well, Try again");
            }
        });
        // if the user says anything else besides the yes words cancel
        replyDialog.setNotUnderstood(new OnNotUnderstoodListener() {
            @Override
            public void notUnderstood(List<String> heard, int reason) {
                String toSayCancelled = "Didn't understand it well, Try later";
                executor.speak(toSayCancelled);
            }
        });
        executor.execute(replyDialog);
    }

    public void confirmSMS(){
        executor.speak(replymessage);
        String toSay = "How does this reply sound? All Correct?";
        final VoiceAlertDialog askDialog = new VoiceAlertDialog(context);
        askDialog.setPrompt(toSay);
        askDialog.setSpokenPrompt(toSay);

        askDialog.addRelaxedPositive(new OnUnderstoodListener() {
            @Override
            public void understood() {
                Log.d(TAG, "confirmed");
                replySMS();

            }
        });
        askDialog.addNegative(new OnUnderstoodListener() {
            @Override
            public void understood() {
                Log.d(TAG, "Dictate again");
                String toSayCancelled = "Ok you can dictate again";
                executor.speak(toSayCancelled);
                startDictation("Do you want to redictate a reply?");
            }
        });
        askDialog.addNeutral(new OnUnderstoodListener() {
            @Override
            public void understood() {
                Log.d(TAG, "Ask again");
                executor.reExecute("Didn't understand it Well, Try again");
            }
        });
        // if the user says anything else besides the yes words cancel
        askDialog.setNotUnderstood(new OnNotUnderstoodListener() {
            @Override
            public void notUnderstood(List<String> heard, int reason) {
                String toSayCancelled = "Didn't understand it well, Try later";
                executor.speak(toSayCancelled);
            }
        });
        executor.execute(askDialog);
    }

    public void replySMS() {
        String toSay = "Do you want to send this SMS reply?";

        final VoiceAlertDialog sendDialog = new VoiceAlertDialog(context);

        sendDialog.setPrompt(toSay);
        sendDialog.setSpokenPrompt(toSay);

        sendDialog.addRelaxedPositive(new OnUnderstoodListener() {
            @Override
            public void understood() {
                Log.d(TAG, "Send reply SMS");
                sendSMSnumber();
            }
        });
        sendDialog.addNegative(new OnUnderstoodListener() {
            @Override
            public void understood() {
                Log.d(TAG, "Dont Reply");
                String toSayCancelled = "Ok not sending it";
                executor.speak(toSayCancelled);
            }
        });
        sendDialog.addNeutral(new OnUnderstoodListener() {
            @Override
            public void understood() {
                Log.d(TAG, "Ask again");
                executor.reExecute("Didn't understand it Well, Try again");
            }
        });
        // if the user says anything else besides the yes words cancel
        sendDialog.setNotUnderstood(new OnNotUnderstoodListener() {
            @Override
            public void notUnderstood(List<String> heard, int reason) {
                String toSayCancelled = "Didn't understand it well, Try later";
                executor.speak(toSayCancelled);
            }
        });
        executor.execute(sendDialog);
    }

    /**
     * lazy initialize the speech recognizer
     */
    private SpeechRecognizer getSpeechRecognizer() {
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        }
        return recognizer;
    }

    private void startDictation(String prompt) {
        Log.e(TAG, "start recognising response");
        final Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000);
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
        SpeechRecognitionUtil.recognizeSpeechDirectly(context,
                recognizerIntent, this, getSpeechRecognizer());
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int error) {
        Log.e(TAG, diagnoseErrorCode(error));
        startDictation("Say again");
    }

    @Override
    public void onResults(Bundle results) {
        compileResults(results);
    }


    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    private void compileResults(Bundle results) {
        Log.d(TAG,"receive results");
        if ((results != null)
                && results.containsKey(SpeechRecognizer.RESULTS_RECOGNITION)) {
            List<String> heard =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float[] scores =
                    results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
            replymessage = heard.get(0);
            Log.d(TAG, replymessage);
            confirmSMS();
        }
    }

    private void sendSMSnumber(){
        String message = replymessage + " - Sent From Rider";
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(number, null, message, null, null);
        ContentValues values = new ContentValues();
        values.put("address", number);
        values.put("body", message);
        context.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
    }
}
