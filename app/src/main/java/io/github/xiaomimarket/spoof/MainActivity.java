package io.github.xiaomimarket.spoof;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 * 使用系统 Preference 页面展示模块说明，跟随设备原生设置风格。
 */
@SuppressWarnings("deprecation")
public final class MainActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
