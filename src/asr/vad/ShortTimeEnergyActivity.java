package asr.vad;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.os.AsyncTask;
import asr.service.MyService;
import asr.vad.R;

public class ShortTimeEnergyActivity extends Activity {
	protected static TextView tv;
	protected static ScrollView scrollview;

	RecognizerTask rec;
	Thread rec_thread;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		tv = (TextView) findViewById(R.id.textView);
		scrollview = (ScrollView) findViewById(R.id.scrollview);

		// this.rec = new RecognizerTask();
		// this.rec_thread = new Thread(this.rec);
		// new LoadRecognizerTask().execute();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		// this.rec.stop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.setting:
			showSetting();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void speakBtn_click(View v) {
		// this.rec.start();

		// start service
		Intent i = new Intent(getApplicationContext(), MyService.class);
		i.putExtra("KEY1", "Value to be use by service");
		getApplicationContext().startService(i);
	}

	public static void logD(String tag, String msg) {
		final String ftag = tag.substring(tag.lastIndexOf('.'));
		final String fmsg = msg;
		tv.post(new Runnable() {
			public void run() {
				// ShortTimeEnergyActivity.tv.setText("[DEBUG] " + ftag + ": " +
				// fmsg + "\n" + ShortTimeEnergyActivity.tv.getText());
				ShortTimeEnergyActivity.tv.append("[DEBUG] " + ftag + ": "
						+ fmsg + "\n");
				scrollview.smoothScrollTo(0, tv.getBottom());
			}
		});
	}

	public static void logE(String tag, String msg) {
		final String ftag = tag;
		final String fmsg = msg;
		tv.post(new Runnable() {
			public void run() {
				// ShortTimeEnergyActivity.tv.setText("[ERROR] " + ftag + ": "
				// + fmsg + "\n" + ShortTimeEnergyActivity.tv.getText());
				ShortTimeEnergyActivity.tv.append("[ERROR] " + ftag + ": "
						+ fmsg + "\n");
				scrollview.smoothScrollTo(0, tv.getBottom());
			}
		});
	}

	public static void logI(String tag, String msg) {
		final String ftag = tag;
		final String fmsg = msg;
		tv.post(new Runnable() {
			public void run() {
				// ShortTimeEnergyActivity.tv.setText("[INFO] " + ftag + ": "
				// + fmsg + "\n" + ShortTimeEnergyActivity.tv.getText());
				ShortTimeEnergyActivity.tv.append("[INFO] " + ftag + ": "
						+ fmsg + "\n");
				scrollview.smoothScrollTo(0, tv.getBottom());
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

	private void showSetting() {
		Intent intent = new Intent(this, VADSettingActivity.class);
		startActivity(intent);
	}

}
