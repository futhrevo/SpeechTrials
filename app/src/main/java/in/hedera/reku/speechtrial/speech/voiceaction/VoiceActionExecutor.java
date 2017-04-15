package in.hedera.reku.speechtrial.speech.voiceaction;

import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.List;

/**
 * Created by reku on 13/4/17.
 */

public class VoiceActionExecutor {
    private static final String TAG = VoiceActionExecutor.class.getSimpleName();

    private VoiceAction active;
    private TextToSpeech tts;
    private Context context;

    /**
     * parameter for TTS to identify utterance
     */
    private final String EXECUTE_AFTER_SPEAK = "EXECUTE_AFTER_SPEAK";
    private final String VA_UTTERENCE_ID = "voiceActionEx";

    public VoiceActionExecutor(Context context)
    {
        this.context = context;
        active = null;
    }

    public void setTts(TextToSpeech tts){
        this.tts = tts;

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {

            }

            @Override
            public void onDone(String utteranceId) {
                onDoneSpeaking(utteranceId);
            }

            @Override
            public void onError(String utteranceId) {

            }
        });
    }

    private void onDoneSpeaking(String utteranceId)
    {
        if (utteranceId.equals(EXECUTE_AFTER_SPEAK))
        {
            doRecognitionOnActive();
        }
    }

    /**
     * external handleReceiveWhatWasHeard must call this
     */
    public void handleReceiveWhatWasHeard(List<String> heard,
                                          float[] confidenceScores)
    {
        active.interpret(heard, confidenceScores);
    }

    /**
     * convenient way to just reply with something spoken
     */
    public void speak(String toSay)
    {
        tts.speak(toSay, TextToSpeech.QUEUE_FLUSH, null, VA_UTTERENCE_ID);
    }

    /**
     * add speech, don't flush the speaking queue
     */
    public void addSpeech(String toSay)
    {
        tts.speak(toSay, TextToSpeech.QUEUE_ADD, null, VA_UTTERENCE_ID);
    }

    /**
     * execute the current active {@link VoiceAction} again speaking
     * extraPrompt before
     */
    public void reExecute(String extraPrompt)
    {
        if ((extraPrompt != null) && (extraPrompt.length() > 0))
        {
            tts.speak(extraPrompt, TextToSpeech.QUEUE_FLUSH, null, EXECUTE_AFTER_SPEAK);
        }
        else
        {
            execute(getActive());
        }
    }

    /**
     * change the current voice action to this and then execute it, optionally
     * saying a prompt first
     */
    public void execute(VoiceAction voiceAction)
    {
        if (tts == null)
        {
            throw new RuntimeException("Text to speech not initialized");
        }

        setActive(voiceAction);

        if (voiceAction.hasSpokenPrompt())
        {
            Log.d(TAG, "speaking prompt: " + voiceAction.getSpokenPrompt());
            tts.speak(voiceAction.getSpokenPrompt(), TextToSpeech.QUEUE_FLUSH, null, EXECUTE_AFTER_SPEAK);
        }
        else
        {
            doRecognitionOnActive();
        }
    }

    private void doRecognitionOnActive()
    {
        Intent recognizerIntent =
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, getActive()
                .getPrompt());
        context.startService(recognizerIntent);
    }

    private VoiceAction getActive()
    {
        return active;
    }

    private void setActive(VoiceAction active)
    {
        this.active = active;
    }

    public TextToSpeech getTts()
    {
        return tts;
    }
}
