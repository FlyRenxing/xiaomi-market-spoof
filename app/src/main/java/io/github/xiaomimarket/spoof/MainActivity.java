package io.github.xiaomimarket.spoof;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * 使用系统 PreferenceFragment 展示模块说明，跟随设备原生设置页。
 */
@SuppressWarnings("deprecation")
public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new InfoFragment())
                    .commit();
        }
    }

    public static final class InfoFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            setSummary("manufacturer", DeviceIdentity.MANUFACTURER);
            setSummary("device", DeviceIdentity.DEVICE);
            setSummary("model", DeviceIdentity.MODEL);
        }

        private void setSummary(String key, String value) {
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setSummary(value);
            }
        }
    }
}
