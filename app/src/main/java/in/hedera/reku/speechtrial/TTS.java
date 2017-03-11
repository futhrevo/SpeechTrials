package in.hedera.reku.speechtrial;

import android.content.Context;
import android.speech.tts.TextToSpeech;

/**
 * Created by rakeshkalyankar on 09/03/17.
 */

public class TTS {
    private static TextToSpeech textToSpeech;

    public static void init(final Context context) {
        if (textToSpeech == null) {
            textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {

                }
            });
        }
    }

    public static void speak(final String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    public static void shutdown(){
        if(textToSpeech != null){
            stop();
            textToSpeech.shutdown();
        }
    }
    public static void stop(){
        if(textToSpeech != null){
            textToSpeech.stop();
        }
    }

}
