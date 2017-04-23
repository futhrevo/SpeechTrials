package in.hedera.reku.speechtrial.speech.voiceaction;

import android.content.Context;

import java.util.ArrayList;

import in.hedera.reku.speechtrial.R;
import in.hedera.reku.speechtrial.speech.text.match.SoundsLikeThresholdWordMatcher;
import in.hedera.reku.speechtrial.speech.text.match.SoundsLikeWordMatcher;
import in.hedera.reku.speechtrial.speech.text.match.StemmedWordMatcher;
import in.hedera.reku.speechtrial.speech.text.match.WordMatcher;

/**
 * Created by rakeshkalyankar on 21/04/17.
 */

public class VoiceAlertDialog extends MultiCommandVoiceAction {

    // use match levels to indicate when you want less
    // to allow less strict matches
    public static final int MATCH_LEVEL_STRICT = 0;
    public static final int MATCH_LEVEL_STEM = 1;
    public static final int MATCH_LEVEL_PHONETIC = 2;
    public static final int MATCH_LEVEL_PHONETIC_LESS_STRICT = 3;

    private String[] yesWords = new String[] { "yes", "ok" };
    private String[] noWords = new String[] { "no" };
    private String[] neutralWords = new String[] { "cancel", "done" };


    public VoiceAlertDialog() {
        super(new ArrayList<VoiceActionCommand>());
    }

    /**
     * get the command words from resources
     */
    public VoiceAlertDialog(Context context)
    {
        super(new ArrayList<VoiceActionCommand>());
        yesWords =
                context.getResources().getStringArray(
                        R.array.voiceaction_yeswords);
        noWords =
                context.getResources().getStringArray(
                        R.array.voiceaction_nowords);
        neutralWords =
                context.getResources().getStringArray(
                        R.array.voiceaction_neutralwords);
    }

    /**
     * add your own command to the dialog here if it consists of words
     */
    public void add(OnUnderstoodListener listener, String... words)
    {
        add(new MatcherCommand(new WordMatcher(words), listener));
    }

    public void addPositive(OnUnderstoodListener listener)
    {
        add(listener, yesWords);
    }

    public void addNegative(OnUnderstoodListener listener)
    {
        add(listener, noWords);
    }

    public void addNeutral(OnUnderstoodListener listener)
    {
        add(listener, neutralWords);
    }

    public void addRelaxedPositive(OnUnderstoodListener listener)
    {
        addRelaxedAll(listener, yesWords);
    }

    public void addRelaxedNegative(OnUnderstoodListener listener)
    {
        addRelaxedAll(listener, noWords);
    }

    public void addRelaxedNeutral(OnUnderstoodListener listener)
    {
        addRelaxedAll(listener, neutralWords);
    }

    /**
     * add some command words, but allow for less strict matching
     */
    public void addRelaxedAll(OnUnderstoodListener listener, String... words)
    {
        add(listener, MATCH_LEVEL_STRICT, words);
        add(listener, MATCH_LEVEL_STEM, words);
        add(listener, MATCH_LEVEL_PHONETIC, words);
        add(listener, MATCH_LEVEL_PHONETIC_LESS_STRICT, words);
    }

    /**
     * allow matching at different levels of confidence
     */
    private void add(OnUnderstoodListener listener, int matchType,
                     String... words)
    {
        WordMatcher matcher;
        switch (matchType)
        {
            case MATCH_LEVEL_STEM:
                matcher = new StemmedWordMatcher(words);
                break;
            case MATCH_LEVEL_PHONETIC:
                matcher = new SoundsLikeWordMatcher(words);
                break;
            case MATCH_LEVEL_PHONETIC_LESS_STRICT:
                matcher = new SoundsLikeThresholdWordMatcher(3, words);
                break;
            case MATCH_LEVEL_STRICT:
            default:
                matcher = new WordMatcher(words);
                break;
        }
        add(new MatcherCommand(matcher, listener));
    }

    public void addOnAnyUtterance(OnUnderstoodListener listener)
    {
        add(new MatchAnythingCommand(listener));
    }

}
