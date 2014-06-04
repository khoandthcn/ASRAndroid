package asr.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

import android.os.Handler;
import android.os.Looper;
import asr.vad.ShortTimeEnergyActivity;

/**
 * Speech recognition task, which runs in a worker thread.
 * 
 * This class implements speech recognition for this demo application. It takes
 * the form of a long-running task which accepts requests to start and stop
 * listening, and emits recognition results to a listener.
 * 
 * @author khoand <khoa.nd.thcn@gmail.com> modified
 * 
 * @author David Huggins-Daines <dhuggins@cs.cmu.edu>
 */
public class VoiceRecorderTask implements Runnable {
    
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
	private final Collection<AudioRecordListener> listeners = new HashSet<AudioRecordListener>();
	
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	/**
	 * Whether to report partial results.
	 */
	protected boolean use_partials;

	/**
	 * State of the main loop.
	 */
	protected enum State {
		IDLE, CALIBRATION, READY, LISTENING, STOP_LISTENING
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

	public void addAudioRecordListener(AudioRecordListener listener){
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	public void removeAudioRecordListener(AudioRecordListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	public void setUsePartials(boolean use_partials) {
		this.use_partials = use_partials;
	}

	public boolean getUsePartials() {
		return this.use_partials;
	}

	public VoiceRecorderTask() {
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
		int ts = 0;

		int utt_len = 0;

		while (!done) {
			/* Read the mail. */
			Event todo = Event.NONE;
			synchronized (this.mailbox) {
				todo = this.mailbox;
				/* If we're idle then wait for something to happen. */
				if (state == State.IDLE && todo == Event.NONE) {
					try {
						logI("waiting");
						this.mailbox.wait();
						todo = this.mailbox;
					} catch (InterruptedException e) {
						/* Quit main loop. */
						logE("Interrupted waiting for mailbox, shutting down");
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
					logE("Received NONE in mailbox when IDLE, threading error?");
				break;
			case START:
				if (state == State.IDLE) {
					logI("START");
					this.audio = new VoiceActivityDectector(this.audioq);
					this.audio_thread = new Thread(this.audio);
					state = State.CALIBRATION;
				} else
					logE("Received START in mailbox when LISTENING");
				break;
			case STOP:
				if (state == State.IDLE)
					logE("Received STOP in mailbox when IDLE");
				else {
					logI("STOP");
					assert this.audio != null;
					this.audio.stop();
					try {
						this.audio_thread.join();
					} catch (InterruptedException e) {
						logE("Interrupted waiting for audio thread, shutting down");
						done = true;
					}
					/* Drain the audio queue. */
					short[] buf;

					while ((buf = this.audioq.poll()) != null) {
						logD("Reading " + buf.length + " samples from queue");
						if(state == State.LISTENING){
							utt_len += buf.length;
							mainHandler.post(new ReadRawAudioEvent(buf, buf.length));
						}
					}

					if(state == State.LISTENING){
						mainHandler.post(new EndUtteranceEvent(utt_len));
						utt_len = 0;
					}
					
					this.audio = null;
					this.audio_thread = null;

					state = State.IDLE;
				}
				break;
			case SHUTDOWN:
				logI("SHUTDOWN");
				if (this.audio != null) {
					this.audio.stop();
					assert this.audio_thread != null;
					try {
						this.audio_thread.join();
					} catch (InterruptedException e) {
						/* We don't care! */
					}
				}
				
				if(state == State.LISTENING){
					mainHandler.post(new EndUtteranceEvent(utt_len));
					utt_len = 0;
				}
				
				this.audio = null;
				this.audio_thread = null;
				state = State.IDLE;
				done = true;
				break;
			}
			if (state == State.CALIBRATION) {
				if (this.audio.calibrate()) {
					this.audio_thread.start();
					state = State.READY;
					logI("READY ...");
				} else {
					logE("Calibration failed !");
					stop();
				}
			}
			if (state == State.READY) {
				assert this.audio != null;

				short[] buf = this.audioq.poll();

				if (buf != null) {
					// process first block of speech, start new utterance

					state = State.LISTENING;
					ts = this.audio.getNoneSpeechRead();
					logI("LISTENING ...");
					utt_len = buf.length;
					
					mainHandler.post(new StartUtteranceEvent());
					mainHandler.post(new ReadRawAudioEvent(buf, buf.length));
				}
			}
			if (state == State.LISTENING) {

				assert this.audio != null;
				// try {
				// This should be called from cont_ad
				short[] buf = this.audioq.poll();

				if (buf == null) {
					if ((this.audio.getNoneSpeechRead() - ts) > 16000) {
						state = State.STOP_LISTENING;
						logI("STOP LISTENING.");
						mainHandler.post(new EndUtteranceEvent(utt_len));
						utt_len = 0;
					}
				} else {
					ts = this.audio.getNoneSpeechRead();
					utt_len += buf.length;
					// process next block of speech
					mainHandler.post(new ReadRawAudioEvent(buf, buf.length));
				}

			}

			if (state == State.STOP_LISTENING) {
				state = State.READY;
				logI("READY ...");
			}
		}
		
		mainHandler.removeCallbacksAndMessages(null);
	}

	public void start() {
		synchronized (this.mailbox) {
			this.mailbox.notifyAll();
			this.mailbox = Event.START;
		}
	}

	public void stop() {
		synchronized (this.mailbox) {
			this.mailbox.notifyAll();
			this.mailbox = Event.STOP;
		}
	}

	public void shutdown() {
		synchronized (this.mailbox) {
			this.mailbox.notifyAll();
			this.mailbox = Event.SHUTDOWN;
		}
	}

	private void logI(String msg) {
		ShortTimeEnergyActivity.logI(getClass().getName(), msg);
	}

	private void logD(String msg) {
		ShortTimeEnergyActivity.logD(getClass().getName(), msg);
	}

	private void logE(String msg) {
		ShortTimeEnergyActivity.logE(getClass().getName(), msg);
	}
	
	private abstract class AudioRecordEvent implements Runnable{

		@Override
		public void run() {
			AudioRecordListener[] emptyArray = new AudioRecordListener[0];
			for(AudioRecordListener listener:listeners.toArray(emptyArray)){
				execute(listener);
			}
		}
		
		protected abstract void execute(AudioRecordListener listener);
	}
	
	private class StartUtteranceEvent extends AudioRecordEvent{

		StartUtteranceEvent() {
		}
		
		@Override
		protected void execute(AudioRecordListener listener) {
			listener.utteranceStart();
		}
		
	}
	
	private class EndUtteranceEvent extends AudioRecordEvent{
		private final int utt_length;

		EndUtteranceEvent(int utt_length) {
			this.utt_length = utt_length;
		}
		@Override
		protected void execute(AudioRecordListener listener) {
			listener.utteranceEnd(utt_length);
		}
		
	}
	
	private class ReadRawAudioEvent extends AudioRecordEvent{
		private final short[] buf;
		private final int length;
		
		ReadRawAudioEvent(short[] buf, int length) {
			this.buf = buf;
			this.length = length;
		}
		
		@Override
		protected void execute(AudioRecordListener listener) {
			listener.readRaw(this.buf, this.length);
		}
		
	}
}
