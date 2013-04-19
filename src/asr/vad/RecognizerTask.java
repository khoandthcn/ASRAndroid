package asr.vad;

import java.util.concurrent.LinkedBlockingQueue;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;


/**
 * Speech recognition task, which runs in a worker thread.
 * 
 * This class implements speech recognition for this demo application. It takes
 * the form of a long-running task which accepts requests to start and stop
 * listening, and emits recognition results to a listener.
 * 
 * @author David Huggins-Daines <dhuggins@cs.cmu.edu>
 */
public class RecognizerTask implements Runnable {
    /**
     * Audio recording task.
     * 
     * This class implements a task which pulls blocks of audio from the system
     * audio input and places them on a queue.
     * 
     * @author David Huggins-Daines <dhuggins@cs.cmu.edu>
     */
    class AudioTask implements Runnable {
        /**
         * Queue on which audio blocks are placed.
         */
        LinkedBlockingQueue<short[]> q;
        AudioRecord rec;
        int block_size;
	double wlen;
	int nswlen;
	int nswshift;
	int frate;
	int sampling_rate = 8000;
        boolean done;
	short[] pre_samples_buf = null;

        static final int DEFAULT_BLOCK_SIZE = 4096;
	static final double DEFAULT_WINDOW_LENGTH = 0.025625;
	static final int DEFAULT_FRAME_RATE = 100;

        AudioTask() {
            this.init(new LinkedBlockingQueue<short[]>(), DEFAULT_BLOCK_SIZE, DEFAULT_WINDOW_LENGTH, DEFAULT_FRAME_RATE);
        }

        AudioTask(LinkedBlockingQueue<short[]> q) {
            this.init(q, DEFAULT_BLOCK_SIZE, DEFAULT_WINDOW_LENGTH, DEFAULT_FRAME_RATE);
        }

        AudioTask(LinkedBlockingQueue<short[]> q, int block_size) {
            this.init(q, block_size, DEFAULT_WINDOW_LENGTH, DEFAULT_FRAME_RATE);
        }

        AudioTask(LinkedBlockingQueue<short[]> q, int block_size, double wlen, int frate)
        {
	    this.init(q, block_size, wlen, frate);
        }

        void init(LinkedBlockingQueue<short[]> q, int block_size, double wlen, int frate) {
            this.done = false;
            this.q = q;
            this.block_size = block_size;
	    this.wlen = wlen;
	    this.frate = frate;
	    this.nswlen = (int)(this.wlen * this.sampling_rate + 0.5);
	    this.nswshift = (int)(this.sampling_rate/this.frate + 0.5);
            this.rec = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 8000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, 8192);
	    ShortTimeEnergyActivity.logD(getClass().getName(), "window_length: " + this.nswlen + " window_shift: " + this.nswshift);
        }

        public int getBlockSize() {
            return block_size;
        }

        public void setBlockSize(int block_size) {
            this.block_size = block_size;
        }

        public LinkedBlockingQueue<short[]> getQueue() {
            return q;
        }

        public void stop() {
            this.done = true;
        }

        public void run() {
            this.rec.startRecording();
            while (!this.done) {
                int nshorts = this.readBlock();
                if (nshorts <= 0)
                    break;
            }
            this.rec.stop();
            this.rec.release();
        }

        int readBlock() {
            short[] buf = new short[this.block_size];
            int nshorts = this.rec.read(buf, 0, buf.length);
            if (nshorts > 0) {
                //ShortTimeEnergyActivity.logD(getClass().getName(), "Posting " + nshorts + " samples to queue");
                processBlock(buf, nshorts);
		this.q.add(buf);
            }
            return nshorts;
        }

	private void processBlock(short[] buf, int len)
	{
	    short[] totalBuf;
	    int totalLen;
	    /* append remained samples from previous block to begining of current block */
	    if(pre_samples_buf != null && pre_samples_buf.length > 0)
	    {
		totalLen = len + pre_samples_buf.length;
		totalBuf = new short[totalLen];
		System.arraycopy(buf,0,totalBuf,0,len);
		System.arraycopy(pre_samples_buf,0,totalBuf,len,pre_samples_buf.length);
	    } else 
	    {
		totalLen = len;
		totalBuf = new short[totalLen];
		System.arraycopy(buf,0,totalBuf,0,len);
	    }

	    /* split to frames */
	    int nframes = (int)((totalLen - this.nswlen)/this.nswshift + 1);
	    int overflow_samples = totalLen - (nframes-1)*this.nswshift - this.nswlen;

	    ShortTimeEnergyActivity.logD(getClass().getName(), "Block_size: " + totalLen + "[" + (totalLen - len) + "] Over_flow: " + overflow_samples + " nframes: " + nframes);
	    /* process each frame */
	    for(int i = 0; i < nframes; i++)
	    {
		short[] frameBuf = new short[this.nswlen];
		System.arraycopy(totalBuf,i*this.nswshift,frameBuf,0,this.nswlen);
		int fpow = framePow(frameBuf);
		//ShortTimeEnergyActivity.logD(getClass().getName(), "Frame_Power: " + fpow);
	  	Log.d(getClass().getName(),"frame power: " + fpow);
	    }
	    if(overflow_samples > 0){
	    	pre_samples_buf = new short[overflow_samples];
	    	System.arraycopy(totalBuf, (nframes-1)*this.nswshift + this.nswlen, pre_samples_buf, 0, overflow_samples);
	    }
	}

	private int framePow(short[] frameBuf)
	{
	    double sumsq = 0.0;
	    int p;
	    for(int i=0; i<frameBuf.length; i++)
	    {
		sumsq += frameBuf[i]*frameBuf[i];
	    }
	    p = (int)(10.0 * (Math.log10(sumsq) - Math.log10(frameBuf.length)) + 0.5);
	    return p;
	}

	private void writeFrame(short[] frameBuf, String fnprefix, int frameIndex)
	{
	    /*try
	    {
		File f = new File("/sdcard/Android/data/test/" + fnprefix + "_" + frameIndex + ".txt");
		f.createNewFile();
		FileOutputStream fOut = new FileOutputStream(f);
		OutputStreamWriter fWriter = new OutputStreamWriter(fOut);
		for(int i = 0; i<frameBuf.length; i++)
		{
		    fWriter.append(frameBuf[i] + " ");
		}
	    } catch(Exception e)
	    {
		
	    }*/
	}
    }

    AudioTask audio;
    /**
     * Thread associated with recording task.
     */
    Thread audio_thread;
    /**
     * Queue of audio buffers.
     */
    LinkedBlockingQueue<short[]> audioq;
    /**
     * Listener for recognition results.
     */
    RecognitionListener rl;
    /**
     * Whether to report partial results.
     */
    boolean use_partials;

    /**
     * State of the main loop.
     */
    enum State {
        IDLE, LISTENING
    };
    /**
     * Events for main loop.
     */
    enum Event {
        NONE, START, STOP, SHUTDOWN
    };

    /**
     * Current event.
     */
    Event mailbox;

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

    public void run() {
        /* Main loop for this thread. */
        boolean done = false;
        /* State of the main loop. */
        State state = State.IDLE;
        /* Previous partial hypothesis. */
        String partial_hyp = null;
        
        while (!done) {
            /* Read the mail. */
            Event todo = Event.NONE;
            synchronized (this.mailbox) {
                todo = this.mailbox;
                /* If we're idle then wait for something to happen. */
                if (state == State.IDLE && todo == Event.NONE) {
                    try {
                        ShortTimeEnergyActivity.logD(getClass().getName(), "waiting");
                        this.mailbox.wait();
                        todo = this.mailbox;
                        ShortTimeEnergyActivity.logD(getClass().getName(), "got" + todo);
                    } catch (InterruptedException e) {
                        /* Quit main loop. */
                        ShortTimeEnergyActivity.logE(getClass().getName(), "Interrupted waiting for mailbox, shutting down");
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
                    ShortTimeEnergyActivity.logE(getClass().getName(), "Received NONE in mailbox when IDLE, threading error?");
                break;
            case START:
                if (state == State.IDLE) { 
                    ShortTimeEnergyActivity.logD(getClass().getName(), "START");
                    this.audio = new AudioTask(this.audioq);
                    this.audio_thread = new Thread(this.audio);
                    //this.ps.startUtt();
                    this.audio_thread.start();
                    state = State.LISTENING;
                }
                else
                    ShortTimeEnergyActivity.logE(getClass().getName(), "Received START in mailbox when LISTENING");
                break;
            case STOP:
                if (state == State.IDLE)
                    ShortTimeEnergyActivity.logE(getClass().getName(), "Received STOP in mailbox when IDLE");
                else {
                    ShortTimeEnergyActivity.logD(getClass().getName(), "STOP");
                    assert this.audio != null;
                    this.audio.stop();
                    try {
                        this.audio_thread.join();
                    }
                    catch (InterruptedException e) {
                        ShortTimeEnergyActivity.logE(getClass().getName(), "Interrupted waiting for audio thread, shutting down");
                        done = true;
                    }
                    /* Drain the audio queue. */
                    short[] buf;
                    while ((buf = this.audioq.poll()) != null) {
                        ShortTimeEnergyActivity.logD(getClass().getName(), "Reading " + buf.length + " samples from queue");
                        //this.ps.processRaw(buf, buf.length, false, false);
                    }
                    //this.ps.endUtt();
                    this.audio = null;
                    this.audio_thread = null;
                    /*Hypothesis hyp = this.ps.getHyp();
                    if (this.rl != null) {
                        if (hyp == null) {
                            ShortTimeEnergyActivity.logD(getClass().getName(), "Recognition failure");
                            this.rl.onError(-1);
                        }
                        else {
                            Bundle b = new Bundle();
                            ShortTimeEnergyActivity.logD(getClass().getName(), "Final hypothesis: " + hyp.getHypstr());
                            b.putString("hyp", hyp.getHypstr());
                            this.rl.onResults(b);
                        }
                    }*/
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
                    }
                    catch (InterruptedException e) {
                        /* We don't care! */
                    }
                }
                //this.ps.endUtt();
                this.audio = null;
                this.audio_thread = null;
                state = State.IDLE;
                done = true;
                break;
            }
            /* Do whatever's appropriate for the current state.  Actually this just means processing audio if possible. */
            if (state == State.LISTENING) {
                assert this.audio != null;
                try {
                    short[] buf = this.audioq.take();
                    ShortTimeEnergyActivity.logD(getClass().getName(), "Reading " + buf.length + " samples from queue");
                    /*this.ps.processRaw(buf, buf.length, false, false);
                    Hypothesis hyp = this.ps.getHyp();
                    if (hyp != null) {
                        String hypstr = hyp.getHypstr();
                        if (hypstr != partial_hyp) {
                            ShortTimeEnergyActivity.logD(getClass().getName(), "Hypothesis: " + hyp.getHypstr());
                            if (this.rl != null && hyp != null) {
                                Bundle b = new Bundle();
                                b.putString("hyp", hyp.getHypstr());
                                this.rl.onPartialResults(b);
                            }
                        }
                        partial_hyp = hypstr;
                    }*/
                } catch (InterruptedException e) {
                    ShortTimeEnergyActivity.logD(getClass().getName(), "Interrupted in audioq.take");
                }
            }
        }
    }

    public void start() {
        ShortTimeEnergyActivity.logD(getClass().getName(), "signalling START");
        synchronized (this.mailbox) {
            this.mailbox.notifyAll();
            ShortTimeEnergyActivity.logD(getClass().getName(), "signalled START");
            this.mailbox = Event.START;
        }
    }

    public void stop() {
        ShortTimeEnergyActivity.logD(getClass().getName(), "signalling STOP");
        synchronized (this.mailbox) {
            this.mailbox.notifyAll();
            ShortTimeEnergyActivity.logD(getClass().getName(), "signalled STOP");
            this.mailbox = Event.STOP;
        }
    }

    public void shutdown() {
        ShortTimeEnergyActivity.logD(getClass().getName(), "signalling SHUTDOWN");
        synchronized (this.mailbox) {
            this.mailbox.notifyAll();
            ShortTimeEnergyActivity.logD(getClass().getName(), "signalled SHUTDOWN");
            this.mailbox = Event.SHUTDOWN;
        }
    }
}
