package asr.vad;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class VADSettingActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public static String KEY_THRESHOLD_UPDATEE_PERIOD = "threshold_update_period";

	public static String KEY_ADAPT_RATE = "adapt_rate";

	public static String KEY_NOISE_LEVEL = "noise_level";

	public static String KEY_WINDOW_SIZE = "window_size";

	public static String KEY_SPEECH_ON_SET = "speech_on_set";

	public static String KEY_SIL_ON_SET = "sil_on_set";

	public static String KEY_LEADER = "leader";

	public static String KEY_TRAILER = "trailer";

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.vad_setting);
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		for (String key : sharedPref.getAll().keySet()) {
			updateSummary(sharedPref, key);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updateSummary(sharedPreferences,key);
	}

	@SuppressWarnings("deprecation")
	private void updateSummary(SharedPreferences sharedPreferences, String key) {
		Preference connectionPref = findPreference(key);
		connectionPref.setSummary(sharedPreferences.getString(key, ""));
	}

}
