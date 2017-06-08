package com.uaes.ble_client_v2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class connectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        Bundle blueToothObject = new Bundle();
        BluetoothLeClass mBLE = (BluetoothLeClass) blueToothObject.getSerializable("BLE");

    }
}
