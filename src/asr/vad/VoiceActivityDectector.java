package asr.vad;

import java.util.Arrays;
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

	class SpeechSegment {

		protected int startfrm;/*
								 * Frame_id in adbuf of start of this segment
								 */
		protected int nfrm;/*
							 * Number of frames in segment ( may wrap around
							 * adbuf)
							 */
		protected SpeechSegment next;/*
									 * Next speech segment
									 */

		public SpeechSegment() {
		}
	}

	/* Number of Frames of internal buffer maintained */
	public static int FRAME_SIZE = 256;

	/* Number of power histogram bins: ~ FRMPOW(65536^2*CONT_AD_SPF) */
	/* Maximum level is 96.3 dB full-scale; 97 for safety, plus 1 for zero-based */
	public static int POWER_HISTOGRAM_SIZE = 98;

	public static int CALIB_FRAMES = POWER_HISTOGRAM_SIZE * 2;

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

	protected boolean done; /* Should be combined with eof flag */

	/**
	 * Properties extended from cont_ad.h
	 * */

	protected enum State {
		STATE_SIL, STATE_SPEECH
	}

	protected State state;

	protected int[] frm_pow;/* Store power of FRAME_SIZE frames */

	protected int[] pow_hist;/* Store power histogram */

	protected short[] adbuf; /*
							 * Circular buffer for maintaining A/D data read
							 * until consumed. Be used to maintain (FRAME_SIZE)
							 * of frames
							 */
	protected int read_ts; /*
							 * Absolute time stamp (total no. of raw samples
							 * consumed up to the most recent cont_ad_read call,
							 * starting from the very beginning). Note that this
							 * is a 32-bit integer; applications should guard
							 * against overflow.
							 */
	protected int siglvl; /*
						 * Max signal level for the data consumed by the most
						 * recent cont_ad_read call (dB range: 0-99). Can be
						 * used to update a V-U meter, for example.
						 */
	protected int sps; /* Sample per second */
	protected int adbufsize;
	protected int spf; /* Samples per frame for analysis */
	protected int headfrm; /* Frame number in adbuf with unconsumed A/D data */
	protected int n_frm; /* Number of completed frames */
	protected int n_sample; /* Number of samples of unconsumed data in adbuf */
	protected int prev_sample; /* For pre-emphasis filter */
	protected int tot_frm; /*
							 * Total number of frames of A/D data read,
							 * including consumed ones
							 */
	protected int noise_level; /*
								 * PWP: what we claim as the "current" noise
								 * level
								 */
	protected boolean auto_thresh; /*
									 * Do automatic threshold adjustment or not
									 */
	protected int delta_sil;
	protected int delta_speech;
	protected int min_noise;
	protected int max_noise;
	protected int winsize;
	protected int speech_onset;
	protected int sil_onset;
	protected int leader;
	protected int trailer;

	protected int thresh_speech;/*
								 * Frame considered to be speech if power >=
								 * thresh_speech (for transition from SILENCE to
								 * SPEECH state)
								 */
	protected int thresh_sil;/*
							 * Frame considered to be silence if power <=
							 * thresh_sil (for transition from SPEECH to SILENCE
							 * state)
							 */

	protected int thresh_update;/*
								 * Number of frames before next update to
								 * pow_hist/thresholds
								 */

	protected double adapt_rate; /*
								 * < Linear interpolation constant for rate at
								 * which noise level adapted to each estimate;
								 * range: 0-1; 0=> no adaptation, 1=> instant
								 * adaptation
								 */
	protected State tail_state; /*
								 * State at the end of its internal buffer
								 * (internal use): CONT_AD_STATE_SIL or
								 * CONT_AD_STATE_SPEECH. Note: This is different
								 * from cont_ad_t.state.
								 */
	protected int win_startfrm;/* Where next analysis window begins */

	protected int win_validfrm;/*
								 * Number of frames currently available from
								 * win_startfrm for analysis
								 */

	protected int n_other; /*
							 * If in SILENCE state, number of frames in analysis
							 * window considered to be speech; otherwise number
							 * of frames considered to be silence
							 */

	protected SpeechSegment spseg_head; /* First of unconsumed speech segments */
	protected SpeechSegment spseg_tail; /* Last of unconsumed speech segments */

	protected int n_calib_frame; /*
								 * Number of frames of calibration data seen so
								 * far.
								 */

	private boolean DEBUG = true;

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

	private void initialize(LinkedBlockingQueue<short[]> q, int block_size) {
		this.audioQueue = q;
		this.block_size = block_size;
		this.rec = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
				SAMPLING_RATE, 1, AudioFormat.ENCODING_PCM_16BIT,
				DEFAULT_BUFFER_SIZE);

		// Initialize Voice Activity Detector properties
		this.sps = this.rec.getSampleRate();
		this.spf = (this.sps * 256) / SAMPLING_RATE;
		this.adbufsize = this.spf * FRAME_SIZE;
		this.adbuf = new short[this.adbufsize];
		this.pow_hist = new int[POWER_HISTOGRAM_SIZE];
		this.frm_pow = new int[FRAME_SIZE];
		this.state = State.STATE_SIL;
		this.read_ts = 0;
		// this.seglen = 0;
		this.siglvl = 0;
		this.prev_sample = 0;
		this.tot_frm = 0;
		this.noise_level = DEFAULT_NOISE;

		this.auto_thresh = true;
		this.delta_sil = DEFAULT_DELTA_SIL;
		this.delta_speech = DEFAULT_DELTA_SPEECH;
		this.min_noise = DEFAULT_MIN_NOISE;
		this.max_noise = DEFAULT_MAX_NOISE;
		this.winsize = WINDOW_SIZE;
		this.speech_onset = DEFAULT_SPEECH_ONSET;
		this.sil_onset = DEFAULT_SIL_ONSET;
		this.leader = LEADER;
		this.trailer = TRAILER;

		this.thresh_sil = this.noise_level + this.delta_sil;
		this.thresh_speech = this.noise_level + this.delta_speech;
		this.thresh_update = THRESHOLD_UPDATE;
		this.adapt_rate = ADAPTED_RATE;
		this.tail_state = State.STATE_SIL;
		this.spseg_head = null;
		this.spseg_tail = null;
		this.n_calib_frame = 0;
	}

	public void stop() {
		this.done = true;
	}

	@Override
	public void run() {
		this.rec.startRecording();
		// do calibrating here
		if (!calibrate()) {
			ShortTimeEnergyActivity.logD(getClass().getName(),
					"Calibration failure !");
			// release resources
		}

		// start recording
		while (!this.done) {
			int nshorts = this.readBlock();
			if (nshorts <= 0) {
				break;
			}
		}
		this.rec.stop();
		this.rec.release();
	}

	/**
	 * Read as much as possible read data into adbuf;
	 * */
	private int readBlock() {
		short[] buf = new short[this.block_size];
		int nshorts = this.rec.read(buf, 0, buf.length);
		if (nshorts > 0) {
			// ShortTimeEnergyActivity.logD(getClass().getName(), "Posting "
			// + nshorts + " samples to queue");
			/* @buf should be processed before adding to audioQueue */

			// this.audioQueue.add(buf);
		}
		return nshorts;
	}

	private int frame_pow(int start_s, int prev_s, int spf) {
		int p, i;
		double v, sumsq;

		sumsq = 0.0;
		p = prev_s;
		for (int j = 0; j < spf; j++) {
			/* Note: pre-emphasis done to remove low-frequency noise. */
			v = (double) (this.adbuf[start_s + j] - this.adbuf[p]);
			sumsq += v * v;
			p = start_s + j;
		}

		this.prev_sample = p;

		/* Make sure FRMPOW(sumsq) >= 0 */
		if (sumsq < spf) {
			sumsq = spf;
		}

		/*
		 * PWP: Units changed to dB
		 * 
		 * Now the units of measurement of an input sample are volts (really!),
		 * so the power in dB is p = 20*log10(samp). Further, we want the RMS
		 * (root-mean-squared) average power across the frame.
		 * 
		 * "sumsq" is the sum of the sum of the squares, so we want
		 * 
		 * i = 20 * log10( sqrt ( sumsq / n_samps) )
		 * 
		 * (Stephen Reed's code actually had i = 20 * log10( sqrt (sumsq) /
		 * n_samps ) but this only produced an additive error.)
		 * 
		 * i = 20 * log10( sqrt ( sumsq / n_samps) ) = 20 * log10( ( sumsq /
		 * n_samps) ^ 0.5 ) = 20 * log10( ( sumsq / n_samps) ) * 0.5 ) = 10 *
		 * log10( ( sumsq / n_samps) ) = 10 * ( log10( sumsq) - log10(n_samps) )
		 */
		i = (int) ((10.0 * (Math.log10(sumsq) - Math.log10((double) spf))) + 0.5);

		/* trim lower bound again to be safe. */
		if (i < 0) {
			i = 0;
		}

		assert (i < 97);

		return i;
	}

	private void compute_frame_pow(int frm) {
		int i;

		i = frame_pow(frm * this.spf, this.prev_sample, this.spf);

		this.frm_pow[frm] = i;
		this.pow_hist[i]++;
		this.thresh_update--;
	}

	private boolean calibrate() {
		int tailfrm, s, len, k;

		// clear power histogram
		for (int i = 0; i < POWER_HISTOGRAM_SIZE; i++) {
			this.pow_hist[i] = 0;
		}

		tailfrm = this.headfrm + this.n_frm;

		if (tailfrm >= FRAME_SIZE) {
			tailfrm -= FRAME_SIZE;
		}

		s = tailfrm * this.spf; /* total of samples available in adbuf */

		for (this.n_calib_frame = 0; this.n_calib_frame < CALIB_FRAMES; ++this.n_calib_frame) {
			len = this.spf;

			// Read len samples into adbuf
			while (len > 0) {
				if ((k = this.rec.read(this.adbuf, s, len)) < 0) {
					return false; // Recording internal error
				}
				len -= k;
				s += k;
			}

			s -= this.spf; // throws audio data read in calibrate phase
			compute_frame_pow(tailfrm);
		}
		this.thresh_update = THRESHOLD_UPDATE;
		return find_thresh();
	}

	private boolean find_thresh() {
		int i, max, j, th;
		int old_noise_level, old_thresh_sil, old_thresh_speech;

		if (!this.auto_thresh) {
			return true;
		}

		/*
		 * Find smallest non-zero histogram entry, but starting at some minimum
		 * power. Power lower than CONT_AD_MIN_NOISE indicates bad A/D input
		 * (eg, mic off...). Too high a minimum power is also bad.
		 */
		for (i = this.min_noise; i < POWER_HISTOGRAM_SIZE
				&& this.pow_hist[i] == 0; i++)
			// not really clear
			;
		if (i > this.max_noise) {
			return false; // bad signal
		}

		/* This method of detecting the noise level is VERY unsatisfactory */
		max = 0;
		for (j = i, th = i; (j < POWER_HISTOGRAM_SIZE) && (j < i + 20); j++) {
			/*
			 * PWP : was i + 6 , which was 9 dB
			 */
			if (max < this.pow_hist[j]) {
				max = this.pow_hist[j];
				th = j;
			}
		}

		/* "Don't change the threshold too fast" */
		old_noise_level = this.noise_level;
		old_thresh_sil = this.thresh_sil;
		old_thresh_speech = this.thresh_speech;

		this.noise_level = (int) (this.noise_level + this.adapt_rate
				* (th - this.noise_level) + 0.5);
		/* update thresholds */
		this.thresh_sil = this.noise_level + this.delta_sil;
		this.thresh_speech = this.noise_level + this.delta_speech;

		// log
		log("noise_level: " + old_noise_level + " => " + this.noise_level);
		log("threshold_sil: " + old_thresh_sil + " => " + this.thresh_sil);
		log("threshold_speech: " + old_thresh_speech + " => " + this.thresh_speech);
		
		return true;
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

	/**
	 * copy data from an array to another array, using System.arraycopy() for
	 * best performance
	 * */
	private void buf_copy() {
	}

	private void log(String message) {
		ShortTimeEnergyActivity.logD(getClass().getName(), message);
	}
}
