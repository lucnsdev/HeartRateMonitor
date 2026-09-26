package lucns.heartratemonitor.activities;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import lucns.heartratemonitor.R;
import lucns.heartratemonitor.ble.BleController;
import lucns.heartratemonitor.ble.ScannedBleDevice;
import lucns.heartratemonitor.fragments.FragmentBleConnecting;
import lucns.heartratemonitor.fragments.FragmentBleEnable;
import lucns.heartratemonitor.fragments.FragmentBleScan;
import lucns.heartratemonitor.services.MainService;
import lucns.heartratemonitor.services.ServiceController;
import lucns.heartratemonitor.utils.Notify;
import lucns.heartratemonitor.utils.Utils;
import lucns.heartratemonitor.views.SliderView;

public class MainActivity extends Activity {

    private SliderView sliderView;
    private BleController bleController;
    private CustomDialogs dialog;
    private FragmentBleScan fragmentBleScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        dialog = new CustomDialogs(this);

        fragmentBleScan = new FragmentBleScan(this, new FragmentBleScan.OnDeviceSelectedListener() {
            @Override
            public void onDeviceSelected(BluetoothDevice device) {
                sliderView.goToIndex(2);
                bleController.stopScan();
                bleController.connect(device);
            }
        });
        sliderView = findViewById(R.id.sliderView);
        sliderView.disableScroll(true);
        sliderView.addFragment(new FragmentBleEnable(this));
        sliderView.addFragment(fragmentBleScan);
        sliderView.addFragment(new FragmentBleConnecting(this));

        ServiceController.getInstance(this, new ServiceController.OnServiceAvailableListener() {
            @Override
            public void onAvailable(MainService mainService) {
                bleController = mainService.getBleManagerControl();
                bleController.setCallback(callback);
                bleController.setUUIDs("f87bf854-b36b-417c-a3d1-c12f91a30001", "f87bf854-b36b-417c-a3d1-c12f91a30002", "f87bf854-b36b-417c-a3d1-c12f91a30003");
                bleController.startScan();
                sliderView.goToIndex(bleController.isEnabled() ? 1 : 0);
            }
        });

        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, onBackPressedListener);
    }

    private final OnBackInvokedCallback onBackPressedListener = new OnBackInvokedCallback() {
        @Override
        public void onBackInvoked() {
            if (isFinishing()) return;
            if (!sliderView.onBackPressed()) return;
            finish();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1234) {
            if (resultCode == RESULT_OK) {
                if (isFinishing()) return;
                sliderView.goToIndex(1);
                bleController.startScan();
                return;
            }
            Notify.showToast(R.string.canceled);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialog != null) dialog.dismiss();
        getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(onBackPressedListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bleController != null) {
            if (bleController.isConnected()) {
                startActivity(new Intent(MainActivity.this, DataViewerActivity.class));
                finish();
                return;
            }
            if (sliderView.getCurrentIndex() == 1) bleController.startScan();
            bleController.setCallback(callback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sliderView.getCurrentIndex() == 1) bleController.stopScan();
    }

    private final BleController.Callback callback = new BleController.Callback() {

        @Override
        public void onStateChanged(boolean enabled) {
            Utils.vibrate();
            fragmentBleScan.changeEnabled(enabled);
            if (enabled) {
                if (dialog != null) dialog.dismiss();
            } else {
                sliderView.goToIndex(0);
                dialog.showInfo(getString(R.string.bluetooth_disabled), new CustomDialogs.Callback() {
                    @Override
                    public void onClick() {
                    }
                });
            }
        }

        @Override
        public void onDevicesAvailable(ScannedBleDevice[] devices) {
            Utils.vibrate();
            fragmentBleScan.updateList(devices);
        }

        @Override
        public void onConnectionChanged(boolean connected) {
            if (!connected) {
                Utils.vibrate();
                Notify.showToast(R.string.disconnected);
                sliderView.goToIndex(bleController.isEnabled() ? 1 : 0);
                bleController.startScan();
            }
        }

        @Override
        public void onServicesDiscovered(boolean success) {
            Utils.vibrate();
            if (!success) {
                dialog.showInfo(getString(R.string.bluetooth_services_error), new CustomDialogs.Callback() {
                    @Override
                    public void onClick() {
                        bleController.disconnect();
                        sliderView.goToIndex(bleController.isEnabled() ? 1 : 0);
                        if (bleController.isEnabled()) bleController.startScan();
                    }
                });
                return;
            }
            startActivity(new Intent(MainActivity.this, DataViewerActivity.class));
            finish();
        }

        @Override
        public void onReceive(byte[] data) {
        }
    };
}