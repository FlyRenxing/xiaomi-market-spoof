package io.github.xiaomimarket.spoof;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView identity = findViewById(R.id.identity);
        identity.setText(getString(R.string.spoof_identity, DeviceIdentity.describe()));
    }
}
