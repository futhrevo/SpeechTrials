package in.hedera.reku.speechtrial.speech.voiceaction;

import android.util.Log;

import java.util.List;

import in.hedera.reku.speechtrial.speech.text.WordList;

/**
 * Created by reku on 13/4/17.
 */

public class MultiCommandVoiceAction extends AbstractVoiceAction {
    private static final String TAG = MultiCommandVoiceAction.class.getSimpleName();

    private List<VoiceActionCommand> commands;

    public MultiCommandVoiceAction(List<VoiceActionCommand> commands)
    {
        this.commands = commands;
    }

    @Override
    public boolean interpret(List<String> heard, float[] confidenceScores)
    {
        boolean understood = false;

        //Android version 4.0 and less devices will have null
        boolean hasConfidenceScores = (confidenceScores != null);

        // halt after understood something
        for (int i = 0; i < heard.size() && !understood; i++)
        {
            String said = heard.get(i);

            //only check confidence if the app supports it
            boolean exceedsMinConfidence = true;
            if (hasConfidenceScores)
            {
                exceedsMinConfidence =
                        (confidenceScores[i] > getMinConfidenceRequired());
            }

            if (exceedsMinConfidence)
            {
                WordList saidWords = new WordList(said);
                for (VoiceActionCommand command : commands)
                {
                    understood = command.interpret(
                            saidWords, confidenceScores);
                    if (understood)
                    {
                        Log.d(TAG, "Command successful: "
                                + command.getClass().getSimpleName());
                        break;
                    }
                }
            }
        }

        if (!understood)
        {
            if (hasConfidenceScores)
            {
                Log.d(TAG, "VoiceAction unsuccessful: " + getPrompt());
                // interpret confidence to provide a reason to
                // notUnderstood

                // check only the highest confidence score, which should be the
                // first
                float highestConfidenceScore = confidenceScores[0];
                if (highestConfidenceScore < 0.0)
                {
                    getNotUnderstood().notUnderstood(heard,
                            OnNotUnderstoodListener.REASON_UNKNOWN);
                }
                else
                {
                    if (highestConfidenceScore <
                            getInaccurateConfidenceThreshold())
                    {
                        getNotUnderstood()
                                .notUnderstood(
                                        heard,
                                        OnNotUnderstoodListener.
                                                REASON_INACCURATE_RECOGNITION);
                    }
                    else if (highestConfidenceScore >=
                            getNotACommandConfidenceThreshold())
                    {
                        getNotUnderstood().notUnderstood(heard,
                                OnNotUnderstoodListener.REASON_NOT_A_COMMAND);
                    }
                    else
                    {
                        getNotUnderstood().notUnderstood(heard,
                                OnNotUnderstoodListener.REASON_UNKNOWN);
                    }
                }
            }
            else
            {
                getNotUnderstood().notUnderstood(heard,
                        OnNotUnderstoodListener.REASON_UNKNOWN);
            }
        }

        return understood;
    }

    protected void add(VoiceActionCommand command)
    {
        commands.add(command);
    }
}
