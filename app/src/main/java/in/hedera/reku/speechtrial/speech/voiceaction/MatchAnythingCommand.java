package in.hedera.reku.speechtrial.speech.voiceaction;

import in.hedera.reku.speechtrial.speech.text.WordList;

/**
 * Created by rakeshkalyankar on 21/04/17.
 */

public class MatchAnythingCommand implements VoiceActionCommand {
    private static final String TAG = "MatchAnythingCommand";

    private OnUnderstoodListener onUnderstood;

    public MatchAnythingCommand(OnUnderstoodListener onUnderstood) {
        this.onUnderstood = onUnderstood;
    }

    @Override
    public boolean interpret(WordList heard, float[] confidenceScores) {
        boolean understood = false;
        if (heard.getWords().length > 0) {
            understood = true;
            if (onUnderstood != null) {
                onUnderstood.understood();
            }
        }
        return understood;
    }

    public OnUnderstoodListener getOnUnderstood() {
        return onUnderstood;
    }
}
