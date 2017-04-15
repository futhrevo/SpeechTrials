package in.hedera.reku.speechtrial.speech.activation;

import in.hedera.reku.speechtrial.speech.activation.SpeechActivationListener;

/**
 * Created by rakeshkalyankar on 17/03/17.
 */

public interface SpeechActivator {
    /**
     * listen for speech activation, when heard, call a {@link SpeechActivationListener} * and stop listening
     */
    public void detectActivation();
    /**
     * stop waiting for activation. */
    public void stop();
}