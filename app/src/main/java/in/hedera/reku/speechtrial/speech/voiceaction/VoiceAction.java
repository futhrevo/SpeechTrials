package in.hedera.reku.speechtrial.speech.voiceaction;

import java.util.List;

/**
 * a specific piece of speech functionality,
 * similar to a GUI Dialog but for speech. Instead of Buttons, it
 * may use {@link VoiceActionCommand} to recognize certain speech
 * inputs.
 * @author Greg Milette &#60;<a
 *         href="mailto:gregorym@gmail.com">gregorym@gmail.com</a>&#62;
 */

public interface VoiceAction {
    /**
     * match String in heard, optionally take action and
     * call OnNotUnderstoodListener if cannot match.
     * @param heard recognition results
     * @param confidenceScores score for each String in heard
     */
    boolean interpret(List<String> heard, float[] confidenceScores);

    /**
     * return the text to show as a prompt when executing <br>
     * if there is no prompt, then return null or an empty string
     */
    public String getPrompt();

    public void setPrompt(String prompt);

    /**
     * the prompt to speak before presenting the recognition dialog
     */
    public String getSpokenPrompt();

    public void setSpokenPrompt(String prompt);

    public boolean hasSpokenPrompt();

    public void setNotUnderstood(OnNotUnderstoodListener notUnderstood);

    public OnNotUnderstoodListener getNotUnderstood();

    /**
     * ignore any responses below this minimum confidence
     */
    public float getMinConfidenceRequired();

    /**
     * confidence greater than this means
     * {@link OnNotUnderstoodListener#REASON_NOT_A_COMMAND}
     */
    public float getNotACommandConfidenceThreshold();

    public void setNotACommandConfidenceThreshold(
            float notACommandConfidenceThreshold);

    /**
     * confidence less than this means
     * {@link OnNotUnderstoodListener#REASON_INACCURATE_RECOGNITION}
     */
    public float getInaccurateConfidenceThreshold();

    public void setInaccurateConfidenceThreshold(
            float inaccurateConfidenceThreshold);
}
