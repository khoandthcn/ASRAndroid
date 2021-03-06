package asr.vad;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Speech recognition task, which runs in a worker thread.
 * 
 * This class implements speech recognition for this demo application. It takes
 * the form of a long-running task which accepts requests to start and stop
 * listening, and emits recognition results to a listener.
 * 
 * @author khoand <khoa.nd.thcn@gmail.com>
 * modified 
 * 
 * @author David Huggins-Daines <dhuggins@cs.cmu.edu>
 */
public class RecognizerTask implements Runnable {

	protected VoiceActivityDectector audio;
	/**
	 * Thread associated with recording task.
	 */
	protected Thread audio_thread;
	/**
	 * Queue of audio buffers.
	 */
	protected LinkedBlockingQueue<short[]> audioq;
	/**
	 * Listener for recognition results.
	 */
	protected RecognitionListener rl;
	/**
	 * Whether to report partial results.
	 */
	protected boolean use_partials;

	/**
	 * State of the main loop.
	 */
	protected enum State {
		IDLE, LISTENING
	};

	/**
	 * Events for main loop.
	 */
	protected enum Event {
		NONE, START, STOP, SHUTDOWN
	};

	/**
	 * Current event.
	 */
	protected Event mailbox;

	public RecognitionListener getRecognitionListener() {
		return rl;
	}

	public void setRecognitionListener(RecognitionListener rl) {
		this.rl = rl;
	}

	public void setUsePartials(boolean use_partials) {
		this.use_partials = use_partials;
	}

	public boolean getUsePartials() {
		return this.use_partials;
	}

	public RecognizerTask() {
		this.audio = null;
		this.audioq = new LinkedBlockingQueue<short[]>();
		this.use_partials = false;
		this.mailbox = Event.NONE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		/* Main loop for this thread. */
		boolean done = false;
		/* State of the main loop. */
		State state = State.IDLE;
		/* Previous partial hypothesis. */
		/* String partial_hyp = null; */

		while (!done) {
			/* Read the mail. */
			Event todo = Event.NONE;
			synchronized (this.mailbox) {
				todo = this.mailbox;
				/* If we're idle then wait for something to happen. */
				if (state == State.IDLE && todo == Event.NONE) {
					try {
						ShortTimeEnergyActivity.logD(getClass().getName(),
								"waiting");
						this.mailbox.wait();
						todo = this.mailbox;
						ShortTimeEnergyActivity.logD(getClass().getName(),
								"got" + todo);
					} catch (InterruptedException e) {
						/* Quit main loop. */
						ShortTimeEnergyActivity
								.logE(getClass().getName(),
										"Interrupted waiting for mailbox, shutting down");
						todo = Event.SHUTDOWN;
					}
				}
				/* Reset the mailbox before releasing, to avoid race condition. */
				this.mailbox = Event.NONE;
			}
			/* Do whatever the mail says to do. */
			switch (todo) {
			case NONE:
				if (state == State.IDLE)
					ShortTimeEnergyActivity
							.logE(getClass().getName(),
									"Received NONE in mailbox when IDLE, threading error?");
				break;
			case START:
				if (state == State.IDLE) {
					ShortTimeEnergyActivity.logD(getClass().getName(), "START");
					this.audio = new VoiceActivityDectector(this.audioq);
					this.audio_thread = new Thread(this.audio);
					// this.ps.startUtt();
					this.audio_thread.start();
					state = State.LISTENING;
				} else
					ShortTimeEnergyActivity.logE(getClass().getName(),
							"Received START in mailbox when LISTENING");
				break;
			case STOP:
				if (state == State.IDLE)
					ShortTimeEnergyActivity.logE(getClass().getName(),
							"Received STOP in mailbox when IDLE");
				else {
					ShortTimeEnergyActivity.logD(getClass().getName(), "STOP");
					assert this.audio != null;
					this.audio.stop();
					try {
						this.audio_thread.join();
					} catch (InterruptedException e) {
						ShortTimeEnergyActivity
								.logE(getClass().getName(),
										"Interrupted waiting for audio thread, shutting down");
						done = true;
					}
					/* Drain the audio queue. */
					short[] buf;

					while ((buf = this.audioq.poll()) != null) {
						ShortTimeEnergyActivity
								.logD(getClass().getName(), "Reading "
										+ buf.length + " samples from queue"); //
						// this.ps.processRaw(buf, buf.length, false, false);
					}

					// this.ps.endUtt();
					this.audio = null;
					this.audio_thread = null;

					// Hypothesis hyp = this.ps.getHyp();
					// if (this.rl != null) {
					// if (hyp == null) {
					// ShortTimeEnergyActivity.logD(getClass().getName(),
					// "Recognition failure");
					// this.rl.onError(-1);
					// } else {
					// Bundle b = new Bundle();
					// ShortTimeEnergyActivity.logD(getClass().getName(),
					// "Final hypothesis: " + hyp.getHypstr());
					// b.putString("hyp", hyp.getHypstr());
					// this.rl.onResults(b);
					// }
					// }

					state = State.IDLE;
				}
				break;
			case SHUTDOWN:
				ShortTimeEnergyActivity.logD(getClass().getName(), "SHUTDOWN");
				if (this.audio != null) {
					this.audio.stop();
					assert this.audio_thread != null;
					try {
						this.audio_thread.join();
					} catch (InterruptedException e) {
						/* We don't care! */
					}
				}
				// this.ps.endUtt();
				this.audio = null;
				this.audio_thread = null;
				state = State.IDLE;
				done = true;
				break;
			}
			/*
			 * Do whatever's appropriate for the current state. Actually this
			 * just means processing audio if possible.
			 */
			if (state == State.LISTENING) {

				assert this.audio != null;
				try {
					// This should be called from cont_ad
					short[] buf = this.audioq.take();
					ShortTimeEnergyActivity.logD(getClass().getName(),
							"Reading " + buf.length + " samples from queue");

					// this.ps.processRaw(buf, buf.length, false, false);
					// Hypothesis hyp = this.ps.getHyp();
					// if (hyp != null) {
					// String hypstr = hyp.getHypstr();
					// if (hypstr != partial_hyp) {
					// ShortTimeEnergyActivity.logD(getClass().getName(),
					// "Hypothesis: " + hyp.getHypstr());
					// if (this.rl != null && hyp != null) {
					// Bundle b = new Bundle();
					// b.putString("hyp", hyp.getHypstr());
					// this.rl.onPartialResults(b);
					// }
					// }
					// partial_hyp = hypstr;
					// }

				} catch (InterruptedException e) {
					ShortTimeEnergyActivity.logD(getClass().getName(),
							"Interrupted in audioq.take");
				}

			}
		}
	}

	public void start() {
		ShortTimeEnergyActivity.logD(getClass().getName(), "signalling START");
		synchronized (this.mailbox) {
			this.mailbox.notifyAll();
			ShortTimeEnergyActivity.logD(getClass().getName(),
					"signalled START");
			this.mailbox = Event.START;
		}
	}

	public void stop() {
		ShortTimeEnergyActivity.logD(getClass().getName(), "signalling STOP");
		synchronized (this.mailbox) {
			this.mailbox.notifyAll();
			ShortTimeEnergyActivity
					.logD(getClass().getName(), "signalled STOP");
			this.mailbox = Event.STOP;
		}
	}

	public void shutdown() {
		ShortTimeEnergyActivity.logD(getClass().getName(),
				"signalling SHUTDOWN");
		synchronized (this.mailbox) {
			this.mailbox.notifyAll();
			ShortTimeEnergyActivity.logD(getClass().getName(),
					"signalled SHUTDOWN");
			this.mailbox = Event.SHUTDOWN;
		}
	}
}
