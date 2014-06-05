package asr.service;

import static android.widget.Toast.makeText;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import asr.vad.R;
import edu.cmu.pocketsphinx.Hypothesis;

public class VoiceRecognizeService extends Service {

	private static final String KWS_SEARCH = "wakeup";
	private static final String FORECAST_SEARCH = "forecast";
	private static final String DIGITS_SEARCH = "digits";
	private static final String MENU_SEARCH = "menu";
	private static final String KEYPHRASE = "hello";
	private SpeechRecognizer recognizer;
	private WindowManager windowManager;
	private final List<View> listViews = new ArrayList<View>();

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

		Toast.makeText(this, "Vietnamese continuous digits ASR service starting ...", Toast.LENGTH_SHORT).show();
		new LoadRecognizerTask().execute();
		super.onCreate();

		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		showResult("0932237002");
	}
	
	private void showResult(String result){
		final TextView chatHead = new TextView(this);
		chatHead.setText(result);

		final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);

		params.gravity = Gravity.TOP | Gravity.LEFT;
		params.x = 0;
		params.y = 100;

		chatHead.setOnTouchListener(new View.OnTouchListener() {
			private int initialX;
			private int initialY;
			private float initialTouchX;
			private float initialTouchY;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					initialX = params.x;
					initialY = params.y;
					initialTouchX = event.getRawX();
					initialTouchY = event.getRawY();
					return true;
				case MotionEvent.ACTION_UP:
					return true;
				case MotionEvent.ACTION_MOVE:
					params.x = initialX + (int) (event.getRawX() - initialTouchX);
					params.y = initialY + (int) (event.getRawY() - initialTouchY);
					windowManager.updateViewLayout(chatHead, params);
					return true;
				}
				
				return new GestureDetector(getApplicationContext(), new SimpleOnGestureListener(){
					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
						float distanceX = e2.getX() - e1.getX();
			            float distanceY = e2.getY() - e1.getY();
			            if (Math.abs(distanceX) > Math.abs(distanceY) 
			            		&& Math.abs(distanceX) > 100 
			            		&& Math.abs(velocityX) > 100) {
			                if (distanceX > 0){
			                    windowManager.removeView(chatHead);
			                	listViews.remove(chatHead);
			                } else {
			                    windowManager.removeView(chatHead);
			                	listViews.remove(chatHead);
			                }
			                return true;
			            }
			            return false;
					}
				}).onTouchEvent(event);
//				return false;
			}
		});
		
//		chatHead.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()){
//			private int initialX;
//			private int initialY;
//			private float initialTouchX;
//			private float initialTouchY;
//			@Override
//			public void onSwipeLeft() {
//				windowManager.removeView(chatHead);
//				listViews.remove(chatHead);
//				super.onSwipeLeft();
//			}
//			
//			@Override
//			public void onSwipeRight() {
//				windowManager.removeView(chatHead);
//				listViews.remove(chatHead);
//				super.onSwipeRight();
//			}
//			
//			@Override
//			public boolean onTouch(View v, MotionEvent event) {
//				switch (event.getAction()) {
//				case MotionEvent.ACTION_DOWN:
//					initialX = params.x;
//					initialY = params.y;
//					initialTouchX = event.getRawX();
//					initialTouchY = event.getRawY();
//					return true;
//				case MotionEvent.ACTION_UP:
//					return true;
//				case MotionEvent.ACTION_MOVE:
//					params.x = initialX + (int) (event.getRawX() - initialTouchX);
//					params.y = initialY + (int) (event.getRawY() - initialTouchY);
//					windowManager.updateViewLayout(chatHead, params);
//					return true;
//				}
//				return getGestureDetector().onTouchEvent(event);
//			}
//		});

		windowManager.addView(chatHead, params);
		listViews.add(chatHead);
	}

	private String toContinuousNumber(String result) {
		return result.replaceAll("KHÔNG", "0")
				.replaceAll("MỘT", "1")
				.replaceAll("HAI", "2")
				.replaceAll("BA", "3")
				.replaceAll("BỐN", "4")
				.replaceAll("NĂM", "5")
				.replaceAll("SÁU", "6")
				.replaceAll("BẢY", "7")
				.replaceAll("TÁM", "8")
				.replaceAll("CHÍN", "9")
				.replaceAll(" ", "");
	}

	@Override
	public void onDestroy() {
		recognizer.stop();
		Toast.makeText(this, "Vietnamese ASR service stopped.", Toast.LENGTH_SHORT).show();
		super.onDestroy();

		if (listViews != null && !listViews.isEmpty()){
			for(View v: listViews){
				windowManager.removeView(v);
			}
		}
	}

	private class LoadRecognizerTask extends AsyncTask<Void, Void, Exception> {
		@Override
		protected Exception doInBackground(Void... v) {
			try {
				Assets assets = new Assets(VoiceRecognizeService.this);
				File assetDir = assets.syncAssets();
				setupRecognizer(assetDir);
			} catch (IOException e) {
				return e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Exception result) {
			if (result != null) {
				Handler h = new Handler(getApplicationContext().getMainLooper());
				// Although you need to pass an appropriate context
				h.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(), "Failed to init recognizer.", Toast.LENGTH_SHORT)
								.show();
					}
				});
			} else {
				// switchSearch(KWS_SEARCH);
				recognizer.startListening(DIGITS_SEARCH);
			}
		}
	}

	private void switchSearch(String searchName) {
		recognizer.stop();
		recognizer.startListening(searchName);
		Toast.makeText(getApplicationContext(), "Using search: " + searchName, Toast.LENGTH_SHORT).show();
	}

	private void setupRecognizer(File assetsDir) {
		File modelsDir = new File(assetsDir, "models");
		recognizer = SpeechRecognizerSetup.defaultSetup().setAcousticModel(new File(modelsDir, "hmm/vidigits_8k"))
				.setDictionary(new File(modelsDir, "dict/vidigits_8k.dic")).setSampleRate(8000).setRawLogDir(assetsDir)
				.setKeywordThreshold(1e-20f).getRecognizer();
		recognizer.addListener(new RecognitionListener() {

			@Override
			public void onResult(Hypothesis hypothesis) {
				if (hypothesis != null) {
					String text = hypothesis.getHypstr();
//					makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
					text = toContinuousNumber(text);
//					if(text.length() > 8 && text.length() < 12){
						showResult(text);
//					}
				}
			}

			@Override
			public void onPartialResult(Hypothesis hypothesis) {
//				String text = hypothesis.getHypstr();
				// if (text.equals(KEYPHRASE))
				// switchSearch(DIGITS_SEARCH);
				// else
				// makeText(getApplicationContext(), text,
				// Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onEndOfSpeech() {
//				makeText(getApplicationContext(), "Utterance ended ...", Toast.LENGTH_SHORT).show();
				// if (DIGITS_SEARCH.equals(recognizer.getSearchName())
				// || FORECAST_SEARCH.equals(recognizer.getSearchName()))
				// switchSearch(KWS_SEARCH);
			}

			@Override
			public void onBeginningOfSpeech() {
//				makeText(getApplicationContext(), "Utterance started ...", Toast.LENGTH_SHORT).show();
			}
		});

		// Create keyword-activation search.
		// recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
		// Create grammar-based searches.
		// File menuGrammar = new File(modelsDir, "grammar/menu.gram");
		// recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
		File digitsGrammar = new File(modelsDir, "grammar/vi_digits.gram");
		recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);
		// Create language model search.
		// File languageModel = new File(modelsDir, "lm/weather.dmp");
		// recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);
	}
}
