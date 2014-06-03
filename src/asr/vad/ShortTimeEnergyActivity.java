package asr.vad;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.os.AsyncTask;
import asr.service.VoiceRecognizeService;

public class ShortTimeEnergyActivity extends Activity {
	protected static TextView tv;
	protected static ScrollView scrollview;
	protected static Button speak_btn;

	RecognizerTask rec;
	Thread rec_thread;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		tv = (TextView) findViewById(R.id.textView);
		scrollview = (ScrollView) findViewById(R.id.scrollview);
		speak_btn = (Button) findViewById(R.id.speakBtn);

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

		// start service
		if(isVoiceRecognizeServiceRunning()){
			// stop recognize service
			Intent i = new Intent(getApplicationContext(), VoiceRecognizeService.class);
			getApplicationContext().stopService(i);
			// set speak_btn text to "Start Recognize service"
			speak_btn.setText(R.string.speakBtnStartLabel);
		} else {
			Intent i = new Intent(getApplicationContext(), VoiceRecognizeService.class);
			i.putExtra("KEY1", "Value to be use by service");
			getApplicationContext().startService(i);
			
			speak_btn.setText(R.string.speakBtnStopLabel);
		}
	}

	public static void logD(String tag, String msg) {
		final String ftag = tag.substring(tag.lastIndexOf('.'));
		final String fmsg = msg;
		tv.post(new Runnable() {
			public void run() {
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
	
	private boolean isVoiceRecognizeServiceRunning(){
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (VoiceRecognizeService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
		return false;
	}

}
