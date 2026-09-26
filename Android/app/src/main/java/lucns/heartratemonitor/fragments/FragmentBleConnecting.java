package lucns.heartratemonitor.fragments;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

import lucns.heartratemonitor.R;
import lucns.heartratemonitor.utils.Notify;
import lucns.heartratemonitor.utils.Utils;
import lucns.heartratemonitor.views.FragmentView;

public class FragmentBleConnecting extends FragmentView {

    private TextView textTitle;
    private boolean connected, disconnectedByUser;

    public FragmentBleConnecting(Activity activity) {
        super(activity);
    }

    @Override
    public void onCreate() {
        setContentView(R.layout.fragment_ble_connecting);

        textTitle = findViewById(R.id.textTitle);
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public boolean onBackPressed() {
        return false;
    }
}
