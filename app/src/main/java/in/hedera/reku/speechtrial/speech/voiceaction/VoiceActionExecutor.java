package in.hedera.reku.speechtrial.speech.voiceaction;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.List;

import in.hedera.reku.speechtrial.speech.SpeechRecognitionUtil;

/**
 * Created by reku on 13/4/17.
 */

public class VoiceActionExecutor implements TextToSpeech.OnInitListener, RecognitionListener {
    private static final String TAG = VoiceActionExecutor.class.getSimpleName();

    private VoiceAction active;
    private TextToSpeech tts;
    private Context context;
    private SpeechRecognizer recognizer;

    /**
     * parameter for TTS to identify utterance
     */
    private final String EXECUTE_AFTER_SPEAK = "EXECUTE_AFTER_SPEAK";
    private final String VA_UTTERENCE_ID = "voiceActionEx";

    public VoiceActionExecutor(Context context) {
        this.context = context;
        active = null;
    }

    public void setTts() {
        this.tts = new TextToSpeech(context, this);
    }

    private void onDoneSpeaking(String utteranceId) {
        if (utteranceId.equals(EXECUTE_AFTER_SPEAK)) {
            // Get a handler that can be used to post to the main thread
            Handler mainHandler = new Handler(context.getMainLooper());

            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    doRecognitionOnActive();
                }
            };
            mainHandler.post(myRunnable);

        }
    }

    /**
     * external handleReceiveWhatWasHeard must call this
     */
    public void handleReceiveWhatWasHeard(List<String> heard,
                                          float[] confidenceScores) {
        active.interpret(heard, confidenceScores);
    }

    /**
     * convenient way to just reply with something spoken
     */
    public void speak(String toSay) {
        tts.speak(toSay, TextToSpeech.QUEUE_FLUSH, null, VA_UTTERENCE_ID);
    }

    /**
     * add speech, don't flush the speaking queue
     */
    public void addSpeech(String toSay) {
        tts.speak(toSay, TextToSpeech.QUEUE_ADD, null, VA_UTTERENCE_ID);
    }

    /**
     * execute the current active {@link VoiceAction} again speaking
     * extraPrompt before
     */
    public void reExecute(String extraPrompt) {
        if ((extraPrompt != null) && (extraPrompt.length() > 0)) {
            tts.speak(extraPrompt, TextToSpeech.QUEUE_FLUSH, null, EXECUTE_AFTER_SPEAK);
        } else {
            execute(getActive());
        }
    }

    /**
     * change the current voice action to this and then execute it, optionally
     * saying a prompt first
     */
    public void execute(VoiceAction voiceAction) {
        if (tts == null) {
            throw new RuntimeException("Text to speech not initialized");
        }

        setActive(voiceAction);

        if (voiceAction.hasSpokenPrompt()) {
            Log.d(TAG, "speaking prompt: " + voiceAction.getSpokenPrompt());
            tts.speak(voiceAction.getSpokenPrompt(), TextToSpeech.QUEUE_ADD, null, EXECUTE_AFTER_SPEAK);
        } else {
            doRecognitionOnActive();
        }
    }

    private void doRecognitionOnActive() {
        Log.e(TAG, "start recognising response");
        final Intent recognizerIntent =
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, getActive()
                .getPrompt());
        SpeechRecognitionUtil.recognizeSpeechDirectly(context,
                recognizerIntent, this, getSpeechRecognizer());
    }

    private VoiceAction getActive() {
        return active;
    }

    private void setActive(VoiceAction active) {
        this.active = active;
    }

    public TextToSpeech getTts() {
        return tts;
    }

    @Override
    public void onInit(int status) {
        Log.d(TAG, "TTS initialized");
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Log.d(TAG, "onStartSpeaking");
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

    /**
     * lazy initialize the speech recognizer
     */
    private SpeechRecognizer getSpeechRecognizer() {
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        }
        return recognizer;
    }

    @Override
    public void onReadyForSpeech(Bundle params) {

    }

    @Override
    public void onBeginningOfSpeech() {

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
    public void onError(int errorCode) {
        Log.d(TAG, "FAILED " + SpeechRecognitionUtil.diagnoseErrorCode(errorCode));
    }

    @Override
    public void onResults(Bundle results) {
        Log.d(TAG, "full results");
        receiveResults(results);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.d(TAG, "partial results");
        receiveResults(partialResults);
    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    /**
     * common method to process any results bundle from {@link SpeechRecognizer}
     */
    private void receiveResults(Bundle results) {
        if ((results != null)
                && results.containsKey(SpeechRecognizer.RESULTS_RECOGNITION)) {
            List<String> heard =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float[] scores =
                    results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
            handleReceiveWhatWasHeard(heard, scores);
        }
    }
}
