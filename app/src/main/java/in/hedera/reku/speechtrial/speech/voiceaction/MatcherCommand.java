package in.hedera.reku.speechtrial.speech.voiceaction;

import in.hedera.reku.speechtrial.speech.text.WordList;
import in.hedera.reku.speechtrial.speech.text.match.WordMatcher;

/**
 * Created by rakeshkalyankar on 21/04/17.
 */

public class MatcherCommand implements VoiceActionCommand {

    private static final String TAG = "MatcherCommand";

    private WordMatcher matcher;

    private OnUnderstoodListener onUnderstood;

    public MatcherCommand(WordMatcher matcher, OnUnderstoodListener onUnderstood) {
        this.matcher = matcher;
        this.onUnderstood = onUnderstood;
    }

    @Override
    public boolean interpret(WordList heard, float[] confidenceScores) {
        boolean understood = false;
        if (matcher.isIn(heard.getWords())) {
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
