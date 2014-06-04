package asr.service;

import edu.cmu.pocketsphinx.Hypothesis;

/**
 * Interface for speech recognition callbacks.
 * 
 * Essentially a cut-down version of {@link android.speech.RecognitionListener},
 * to avoid dependencies on Froyo and methods we don't need or can't provide.
 * 
 * @author David Huggins-Daines <dhuggins@cs.cmu.edu>
 * 
 */
public interface RecognitionListener {
	
	/**
     * Called at the start of utterance.
     */
    public void onBeginningOfSpeech();

    /**
     * Called at the end of utterance.
     */
    public void onEndOfSpeech();
    
	/**
	 * Called on the recognition thread when partial results are available.
	 * 
	 * Note: This is not like android.speech.RecognitionListener in that it does
	 * not get called on the main thread.
	 * 
	 * @param b
	 *            Bundle containing the partial result string under the "hyp"
	 *            key.
	 */
    public void onPartialResult(Hypothesis hypothesis);

	/**
	 * Called when final results are available.
	 * 
	 * Note: This is not like android.speech.RecognitionListener in that it does
	 * not get called on the main thread.
	 * 
	 * @param b
	 *            Bundle containing the final result string under the "hyp" key.
	 */
    public void onResult(Hypothesis hypothesis);
}
