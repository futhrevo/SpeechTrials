package in.hedera.reku.speechtrial.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.List;

import in.hedera.reku.speechtrial.speech.text.WordList;
import in.hedera.reku.speechtrial.speech.text.match.SoundsLikeWordMatcher;

/**
 * Created by rakeshkalyankar on 17/03/17.
 */

public class WordActivator implements SpeechActivator, RecognitionListener {
    private static final String TAG = WordActivator.class.getSimpleName();

    private Context context;
    private SpeechRecognizer recognizer;
    private SoundsLikeWordMatcher matcher;
    private SpeechActivationListener resultListener;
    private boolean heardTargetWord = false;

    public WordActivator(Context context, SpeechActivationListener resultListener, String... targetWords) {
        this.context = context;
        this.matcher = new SoundsLikeWordMatcher(targetWords);
        this.resultListener = resultListener;
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "ready for speech " + params);
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech for speech ");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.d(TAG, "onBufferReceived for speech ");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech for speech ");
    }

    @Override
    public void onError(int errorCode) {
        if ((errorCode == SpeechRecognizer.ERROR_NO_MATCH)
                || (errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        {
            Log.d(TAG, "didn't recognize anything");
            // keep going
            recognizeSpeechDirectly();
        }
        else
        {
            Log.d(TAG,
                    "FAILED "
                            + SpeechRecognitionUtil
                            .diagnoseErrorCode(errorCode));
        }
    }

    @Override
    public void onResults(Bundle results) {
        Log.d(TAG, "full results");
        receiveResults(results);
        if(!heardTargetWord){
            recognizeSpeechDirectly();
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.d(TAG, "partial results");
        receiveResults(partialResults);
    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    @Override
    public void detectActivation() {
        recognizeSpeechDirectly();
    }

    @Override
    public void stop() {
        if (getSpeechRecognizer() != null) {
//            getSpeechRecognizer().stopListening();
//            getSpeechRecognizer().cancel();
            getSpeechRecognizer().destroy();
        }
    }

    private void recognizeSpeechDirectly()
    {
        Intent recognizerIntent =
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // accept partial results if they come
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 20000);
        SpeechRecognitionUtil.recognizeSpeechDirectly(context,
                recognizerIntent, this, getSpeechRecognizer());
    }

    /**
     * lazy initialize the speech recognizer
     */
    private SpeechRecognizer getSpeechRecognizer()
    {
        if (recognizer == null)
        {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        }
        return recognizer;
    }

    /**
     * common method to process any results bundle from {@link SpeechRecognizer}
     */
    private void receiveResults(Bundle results){
        if ((results != null)
                && results.containsKey(SpeechRecognizer.RESULTS_RECOGNITION))
        {
            List<String> heard =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float[] scores =
                    results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
            receiveWhatWasHeard(heard, scores);
        }
        else
        {
            Log.d(TAG, "no results");
        }
    }

    private void receiveWhatWasHeard(List<String> heard, float[] scores) {

        // find the target word
        for (String possible : heard)
        {
            WordList wordList = new WordList(possible);
            if (matcher.isIn(wordList.getWords()))
            {
                Log.d(TAG, "HEARD IT!");
                heardTargetWord = true;
                break;
            }
        }

        if (heardTargetWord)
        {
            stop();
            resultListener.activated(true);
        }
//        else
//        {
//            // keep going
//            recognizeSpeechDirectly();
//        }
    }
}
