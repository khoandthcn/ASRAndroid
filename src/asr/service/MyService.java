package asr.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.widget.Toast;
import asr.vad.RecognizerTask;

public class MyService extends Service {

	RecognizerTask rec;
	Thread rec_thread;

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
		this.rec_thread = new Thread(this.rec);
		new LoadRecognizerTask().execute();

		super.onCreate();
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub

		Toast.makeText(this, "Vietnamese ASR service stopped.",
				Toast.LENGTH_SHORT).show();

		super.onDestroy();
	}

	private class LoadRecognizerTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... v) {
			rec_thread.start();
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {

		}
	}
}
