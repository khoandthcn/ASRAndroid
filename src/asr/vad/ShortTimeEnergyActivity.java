package asr.vad;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.view.View;
import android.os.AsyncTask;

public class ShortTimeEnergyActivity extends Activity {
	protected static TextView tv;

	RecognizerTask rec;
	Thread rec_thread;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		tv = (TextView) findViewById(R.id.textView);

		this.rec = new RecognizerTask();
		this.rec_thread = new Thread(this.rec);
		new LoadRecognizerTask().execute();
	}

	public void onPause() {
		super.onPause();
		this.rec.stop();
	}

	public void speakBtn_click(View v) {
		this.rec.start();
	}

	public static void logD(String tag, String msg) {
		final String ftag = tag.substring(tag.lastIndexOf('.'));
		final String fmsg = msg;
		tv.post(new Runnable() {
			public void run() {
				ShortTimeEnergyActivity.tv.setText("[DEBUG] " + ftag + ": "
						+ fmsg + "\n" + ShortTimeEnergyActivity.tv.getText());
			}
		});
	}

	public static void logE(String tag, String msg) {
		final String ftag = tag;
		final String fmsg = msg;
		tv.post(new Runnable() {
			public void run() {
				ShortTimeEnergyActivity.tv.setText("[ERROR] " + ftag + ": "
						+ fmsg + "\n" + ShortTimeEnergyActivity.tv.getText());
			}
		});
	}

	public static void logI(String tag, String msg) {
		final String ftag = tag;
		final String fmsg = msg;
		tv.post(new Runnable() {
			public void run() {
				ShortTimeEnergyActivity.tv.setText("[INFO] " + ftag + ": "
						+ fmsg + "\n" + ShortTimeEnergyActivity.tv.getText());
			}
		});
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
