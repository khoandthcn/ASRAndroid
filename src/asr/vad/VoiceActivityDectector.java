package asr.vad;

import java.util.concurrent.LinkedBlockingQueue;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/*
 * The VoiceActivityDector class implement a VAD algorithm based on energy of speech signal
 * step:
 * 		- 
 * */
public class VoiceActivityDectector implements Runnable {

	/* Number of Frames of internal buffer maintained */
	public static int FRAME_SIZE = 256;

	/* Number of power histogram bins: ~ FRMPOW(65536^2*CONT_AD_SPF) */
	/* Maximum level is 96.3 dB full-scale; 97 for safety, plus 1 for zero-based */
	public static int POWER_HISTOGRAM_SIZE = 98;

	/* Update thresholds approximate every so many frames */
	/* PWP: update was 200 frames, or 3.2 seconds. Now about every 1.6 sec. */
	public static int THRESHOLD_UPDATE = 100;

	/* Interpolation of new and old noise level */
	public static double ADAPTED_RATE = 0.2;

	/* Sampling rate */
	public static int SAMPLING_RATE = 16000;

	/* Default background noise power level */
	public static int DEFAULT_NOISE = 30;

	/* Initial default for cont_ad_t.delta_sil */
	public static int DEFAULT_DELTA_SIL = 10;

	/* Initial default for cont_ad_t.delta_speech */
	public static int DEFAULT_DELTA_SPEECH = 17;

	/* Expected minimum background noise level */
	public static int DEFAULT_MIN_NOISE = 2;

	/* Maximum background noise level */
	public static int DEFAULT_MAX_NOISE = 70;

	/* Analysis window for state transitions */
	/* rkm had 16 */
	public static int WINDOW_SIZE = 21;

	/*
	 * Min #speech frames in analysis window for SILENCE -> SPEECH state
	 * transition SReed had 100 ms == 6.25 frames contiguous; rkm had 9 (out of
	 * 16+10) with a lower threshold
	 */
	public static int DEFAULT_SPEECH_ONSET = 9;

	/*
	 * Min #silence frames in analysis window for SPEECH -> SILENCE state
	 * transition MUST BE <= CONT_AD_WINSIZE SReed had 400 ms == 25 fr
	 * contiguous; rkm had 14 out of 16
	 */
	public static int DEFAULT_SIL_ONSET = 18;

	/*
	 * On transition to SPEECH state, so many frames BEFORE window included in
	 * speech data (>0)
	 */

	/* SReed had 200 ms == 12.5 fr; rkm had 5 */
	public static int LEADER = 5;

	/*
	 * On transition to SILENCE state, so many frames of silence included in
	 * speech data (>0). NOTE: Ensure (0 < TRAILER+LEADER <= WINSIZE)
	 */
	/* SReed had 100 ms == 6.25 fr; rkm had 10 */
	public static int TRAILER = 10;

	public static int DEFAULT_BLOCK_SIZE = 4096;

	public static int DEFAULT_BUFFER_SIZE = 4096;

	/*
	 * Queue of audio buffer, continuously read audio data from audio device
	 * into this buffer
	 */
	protected LinkedBlockingQueue<short[]> audioQueue = null;

	protected int block_size;

	protected AudioRecord rec;

	protected boolean done;

	/**
	 * Properties extended from cont_ad.h
	 * */

	protected enum State {
		STATE_SIL, STATE_SPEECH
	}

	protected State state;

	/* Number of completed frames */
	protected int nframe;

	/* Samples per frame for analysis */
	protected int spf;

	/*
	 * If in SILENCE state, number of frames in analysis window considered to be
	 * speech; otherwise number of frames considered to be silence
	 */
	protected int n_other;

	/* Store power of FRAME_SIZE frames */
	protected int[] frm_pow;

	/* Store power histogram */
	protected int[] pow_hist;

	/*
	 * < Frame considered to be speech if power >= thresh_speech (for transition
	 * from SILENCE to SPEECH state)
	 */
	protected int threshold_speech;

	/*
	 * < Frame considered to be silence if power <= thresh_sil (for transition
	 * from SPEECH to SILENCE state)
	 */
	protected int threshold_sil;

	/* < Number of frames before next update to pow_hist/thresholds */
	protected int thresold_update;

	/*
	 * < Linear interpolation constant for rate at which noise level adapted to
	 * each estimate; range: 0-1; 0=> no adaptation, 1=> instant adaptation
	 */
	protected double adapt_rate;
	
	protected LinkedBlockingQueue<short[]> window_buf;

	public VoiceActivityDectector() {
		initialize(new LinkedBlockingQueue<short[]>(), DEFAULT_BLOCK_SIZE);
	}

	public VoiceActivityDectector(LinkedBlockingQueue<short[]> q) {
		initialize(q, DEFAULT_BLOCK_SIZE);
	}

	public VoiceActivityDectector(LinkedBlockingQueue<short[]> q, int block_size) {
		initialize(q, block_size);
	}

	public LinkedBlockingQueue<short[]> getAudioQueue() {
		return audioQueue;
	}

	public void stop() {
		this.done = true;
	}

	@Override
	public void run() {
		this.rec.startRecording();
		while (!this.done) {
			int nshorts = this.readBlock();
			if (nshorts <= 0) {
				break;
			}
		}
		this.rec.stop();
		this.rec.release();
	}

	private void initialize(LinkedBlockingQueue<short[]> q, int block_size) {
		this.audioQueue = q;
		this.block_size = block_size;
		this.rec = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
				SAMPLING_RATE, 1, AudioFormat.ENCODING_PCM_16BIT,
				DEFAULT_BUFFER_SIZE);
	}

	private int readBlock() {
		short[] buf = new short[this.block_size];
		int nshorts = this.rec.read(buf, 0, buf.length);
		if (nshorts > 0) {
			ShortTimeEnergyActivity.logD(getClass().getName(), "Posting "
					+ nshorts + " samples to queue");
			/* @buf should be processed before adding to audioQueue */
			this.audioQueue.add(buf);
		}
		return nshorts;
	}

	/**
	 * @param Break
	 *            read data into frame and update threshold
	 * */
	private void classify() {
	}

	/**
	 * @param frameIndex
	 * 
	 *            Start to process on each analysis frame when it reach full
	 *            analysis window size
	 * */
	private void boundaryDetect(int frameIndex) {
	}

}
