package in.hedera.reku.speechtrial.speech.voiceaction;

import android.util.Log;

import java.util.List;

/**
 * Created by reku on 13/4/17.
 */

public abstract class AbstractVoiceAction implements VoiceAction, OnNotUnderstoodListener {
    private static final String TAG = AbstractVoiceAction.class.getSimpleName();

    private String prompt;

    private String spokenPrompt;

    private OnNotUnderstoodListener notUnderstood;

    /**
     * by default include all possible recognitions
     */
    private float minConfidenceRequired = -1.0f;

    /**
     * must be higher than this in order to report not a command
     * In general this should be a high number
     */
    private float notACommandConfidenceThreshold = 0.9f;

    /**
     * if the confidence is lower than this, assume the recognizer
     * had inaccurate recognition
     */
    private float inaccurateConfidenceThreshold = 0.3f;

    public AbstractVoiceAction()
    {
        //default implementation
        notUnderstood = this;
    }

    public void setMinConfidenceRequired(float minConfidenceRequired)
    {
        this.minConfidenceRequired = minConfidenceRequired;
    }

    /**
     * @param prompt the prompt to set
     */
    public void setPrompt(String prompt)
    {
        this.prompt = prompt;
    }

    /**
     * @see in.hedera.reku.speechtrial.speech.voiceaction.VoiceAction#hasSpokenPrompt()
     */
    @Override
    public boolean hasSpokenPrompt()
    {
        return spokenPrompt != null && spokenPrompt.length() > 0;
    }

    /**
     * @return the prompt
     */
    public String getPrompt()
    {
        return prompt;
    }

    /**
     * @param notUnderstood the notUnderstood to set
     */
    public void setNotUnderstood(OnNotUnderstoodListener notUnderstood)
    {
        this.notUnderstood = notUnderstood;
    }

    /**
     * @return the notUnderstood
     */
    public OnNotUnderstoodListener getNotUnderstood()
    {
        return notUnderstood;
    }

    /**
     * @return the minConfidenceRequired
     */
    public float getMinConfidenceRequired()
    {
        return minConfidenceRequired;
    }

    @Override
    public String getSpokenPrompt()
    {
        return spokenPrompt;
    }

    /**
     * @see in.hedera.reku.speechtrial.speech.voiceaction.VoiceAction#setSpokenPrompt(java.lang.String)
     */
    @Override
    public void setSpokenPrompt(String prompt)
    {
        spokenPrompt = prompt;
    }

    public float getNotACommandConfidenceThreshold()
    {
        return notACommandConfidenceThreshold;
    }

    public void setNotACommandConfidenceThreshold(
            float notACommandConfidenceThreshold)
    {
        this.notACommandConfidenceThreshold = notACommandConfidenceThreshold;
    }

    public float getInaccurateConfidenceThreshold()
    {
        return inaccurateConfidenceThreshold;
    }

    public void setInaccurateConfidenceThreshold(
            float inaccurateConfidenceThreshold)
    {
        this.inaccurateConfidenceThreshold = inaccurateConfidenceThreshold;
    }

    @Override
    public void notUnderstood(List<String> heard, int reason)
    {
        Log.d(TAG, "not understood because of " + reason);
    }
}
