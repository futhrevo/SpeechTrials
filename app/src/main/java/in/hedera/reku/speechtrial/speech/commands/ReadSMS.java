package in.hedera.reku.speechtrial.speech.commands;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

import in.hedera.reku.speechtrial.R;
import in.hedera.reku.speechtrial.speech.text.match.SoundsLikeThresholdWordMatcher;
import in.hedera.reku.speechtrial.speech.text.match.WordMatcher;
import in.hedera.reku.speechtrial.speech.voiceaction.OnNotUnderstoodListener;
import in.hedera.reku.speechtrial.speech.voiceaction.OnUnderstoodListener;
import in.hedera.reku.speechtrial.speech.voiceaction.VoiceActionExecutor;
import in.hedera.reku.speechtrial.speech.voiceaction.VoiceAlertDialog;

import static in.hedera.reku.speechtrial.actions.SimpleSmsReceiver.INCOMING_SMS_INTENT_MESSAGE;
import static in.hedera.reku.speechtrial.actions.SimpleSmsReceiver.INCOMING_SMS_INTENT_SENDER;

/**
 * Created by rakeshkalyankar on 21/04/17.
 */

public class ReadSMS {

    public static final String TAG = ReadSMS.class.getSimpleName();
    private WordMatcher match;

    private Context context;
    private VoiceActionExecutor executor;
    private boolean relaxed;

    private String sender;
    private String message;

    public ReadSMS(Context context, VoiceActionExecutor executor, boolean relaxed, final Intent intent) {
        String[] commandWords =
                context.getResources().getStringArray(
                        R.array.read_SMS_command);
        if (relaxed) {
            // match "remove" if 3 of the 4 soundex characters match
            match = new SoundsLikeThresholdWordMatcher(3, commandWords);
        } else {
            //exact match
            match = new WordMatcher(commandWords);
        }
        this.context = context;
        this.executor = executor;
        this.relaxed = relaxed;
        sender = intent.getStringExtra(INCOMING_SMS_INTENT_SENDER);
        message = intent.getStringExtra(INCOMING_SMS_INTENT_MESSAGE);
        interpret();
    }

    public void interpret() {

        final VoiceAlertDialog confirmDialog = new VoiceAlertDialog();
        // add listener for positive response
        // use relaxed matching to increase chance of understanding user
        confirmDialog.addRelaxedPositive(new OnUnderstoodListener() {
            @Override
            public void understood() {
                Log.d(TAG, "READ!: SMS");
                executor.speak(message);
            }
        });

        //prompt for the confirm VoiceAction
        String toSay = "You got a new SMS from "+ sender + " . Do you want me to read it";
        confirmDialog.setPrompt(toSay);
        confirmDialog.setSpokenPrompt(toSay);

        // if the user says anything else besides the yes words cancel
        confirmDialog.setNotUnderstood(new OnNotUnderstoodListener() {
            @Override
            public void notUnderstood(List<String> heard, int reason) {
                String toSayCancelled = "Ok not reading it";
                executor.speak(toSayCancelled);
            }
        });

        executor.execute(confirmDialog);
    }
}
