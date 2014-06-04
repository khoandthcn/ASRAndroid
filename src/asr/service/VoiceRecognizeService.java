package asr.service;

import static android.widget.Toast.makeText;

import java.io.File;
import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;
import edu.cmu.pocketsphinx.Hypothesis;

public class VoiceRecognizeService extends Service {
	
	private static final String KWS_SEARCH = "wakeup";
    private static final String FORECAST_SEARCH = "forecast";
    private static final String DIGITS_SEARCH = "digits";
    private static final String MENU_SEARCH = "menu";
    private static final String KEYPHRASE = "hello";
    private SpeechRecognizer recognizer;
	

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

		Toast.makeText(this, "Vietnamese continuous digits ASR service starting ...",
				Toast.LENGTH_SHORT).show();
		new LoadRecognizerTask().execute();
//		Toast.makeText(this, "say HELLO to start",
//				Toast.LENGTH_LONG).show();
		super.onCreate();	
	}

	@Override
	public void onDestroy() {
		recognizer.stop();
		Toast.makeText(this, "Vietnamese ASR service stopped.",
				Toast.LENGTH_SHORT).show();
		super.onDestroy();
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
						Toast.makeText(getApplicationContext(),
								"Failed to init recognizer.", Toast.LENGTH_SHORT)
								.show();
					}
				});
            } else {
//                switchSearch(KWS_SEARCH);
            	recognizer.startListening(DIGITS_SEARCH);
            }
		}
	}

    private void switchSearch(String searchName) {
        recognizer.stop();
        recognizer.startListening(searchName);
        Toast.makeText(getApplicationContext(),
				"Using search: " + searchName, Toast.LENGTH_SHORT)
				.show();
    }

    private void setupRecognizer(File assetsDir) {
        File modelsDir = new File(assetsDir, "models");
        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(modelsDir, "hmm/vi"))
                .setDictionary(new File(modelsDir, "dict/vi_subtitle_know_ascii.dic"))
                .setRawLogDir(assetsDir).setKeywordThreshold(1e-20f)
                .getRecognizer();
        recognizer.addListener(new RecognitionListener() {
			
			@Override
			public void onResult(Hypothesis hypothesis) {
		        if (hypothesis != null) {
		            String text = hypothesis.getHypstr();
		            makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
		        }
			}
			
			@Override
			public void onPartialResult(Hypothesis hypothesis) {
				String text = hypothesis.getHypstr();
//		        if (text.equals(KEYPHRASE))
//		            switchSearch(DIGITS_SEARCH);
//		        else
		        	makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void onEndOfSpeech() {
				makeText(getApplicationContext(), "Utterance ended ...", Toast.LENGTH_SHORT).show();
//				if (DIGITS_SEARCH.equals(recognizer.getSearchName())
//		                || FORECAST_SEARCH.equals(recognizer.getSearchName()))
//		            switchSearch(KWS_SEARCH);
			}
			
			@Override
			public void onBeginningOfSpeech() {
				makeText(getApplicationContext(), "Utterance started ...", Toast.LENGTH_SHORT).show();
			}
		});

        // Create keyword-activation search.
//        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
        // Create grammar-based searches.
//        File menuGrammar = new File(modelsDir, "grammar/menu.gram");
//        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
        File digitsGrammar = new File(modelsDir, "grammar/vi_digits.gram");
        recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);
        // Create language model search.
//        File languageModel = new File(modelsDir, "lm/weather.dmp");
//        recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);
    }
}
