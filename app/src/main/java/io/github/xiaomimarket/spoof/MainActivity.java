package io.github.xiaomimarket.spoof;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setText(R.id.value_manufacturer, DeviceIdentity.MANUFACTURER);
        setText(R.id.value_device, DeviceIdentity.DEVICE);
        setText(R.id.value_model, DeviceIdentity.MODEL);
    }

    private void setText(int id, String value) {
        TextView view = findViewById(id);
        view.setText(value);
    }
}
