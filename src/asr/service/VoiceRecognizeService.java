package asr.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

public class VoiceRecognizeService extends Service {

	private RecognizerTask rec;
	private Thread rec_thread;
	

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		// return super.onStartCommand(intent, flags, startId);
		return Service.START_NOT_STICKY;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub

		Toast.makeText(this, "Vietnamese ASR service starting ...",
				Toast.LENGTH_SHORT).show();

		this.rec = new RecognizerTask();
		this.rec.setRecognitionListener(new RecognitionListener() {

			@Override
			public void onSpeechStartpoint() {
				// TODO Auto-generated method stub
				Handler h = new Handler(getApplicationContext().getMainLooper());
				// Although you need to pass an appropriate context
				h.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(),
								"Utterance started ...", Toast.LENGTH_SHORT)
								.show();
					}
				});

			}

			@Override
			public void onSpeechEndpoint(int utteranceLength) {
				// TODO Auto-generated method stub
				Handler h = new Handler(getApplicationContext().getMainLooper());
				// final int utt_len = utteranceLength;
				final double utt_len = (double) utteranceLength
						/ (double) VoiceActivityDectector.SAMPLING_RATE;
				// Although you need to pass an appropriate context
				h.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(
								getApplicationContext(),
								"Utterance stopped ["
										+ String.format("%.2f", utt_len)
										+ " seconds].", Toast.LENGTH_SHORT)
								.show();
					}
				});

			}

			@Override
			public void onResults(Bundle b) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onPartialResults(Bundle b) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onError(int err) {
				// TODO Auto-generated method stub

			}
		});
		this.rec_thread = new Thread(this.rec);
		new LoadRecognizerTask().execute();
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		this.rec.shutdown();
		Toast.makeText(this, "Vietnamese ASR service stopped.",
				Toast.LENGTH_SHORT).show();
		super.onDestroy();
	}

	private class LoadRecognizerTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... v) {
			rec_thread.start();
			rec.start();
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {

		}
	}
}
