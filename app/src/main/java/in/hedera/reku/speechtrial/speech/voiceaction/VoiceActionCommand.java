package in.hedera.reku.speechtrial.speech.voiceaction;

/**
 * Created by reku on 13/4/17.
 */

import in.hedera.reku.speechtrial.speech.text.WordList;

/**
 * Part of a {@link VoiceAction} representing one command the user can
 * say
 * @author Greg Milette &#60;<a href="mailto:gregorym@gmail.com">gregorym@gmail.com</a>&#62;
 */
public interface VoiceActionCommand
{
    boolean interpret(WordList heard, float [] confidenceScores);
}
