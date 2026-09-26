package lucns.heartratemonitor.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BleController {

    public interface Callback {
        void onStateChanged(boolean enabled);

        void onDevicesAvailable(ScannedBleDevice[] devices);

        void onConnectionChanged(boolean connected);

        void onServicesDiscovered(boolean success);

        void onReceive(byte[] data);
    }

    private Context context;
    private Callback callback;
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeScanner bluetoothLeScanner;
    private boolean connected;
    private final List<ScannedBleDevice> scannedBleDevices;
    private String uuidService, uuidRx, uuidTx;
    private BluetoothDevice bluetoothDevice;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic characteristicTx;
    private final Map<String, byte[]> map;

    public BleController(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
        map = new LinkedHashMap<>();
        scannedBleDevices = new LinkedList<>();
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(bluetoothStateReceiver, filter);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setUUIDs(String uuidService, String uuidRx, String uuidTx) {
        this.uuidService = uuidService;
        this.uuidRx = uuidRx;
        this.uuidTx = uuidTx;
    }

    public boolean isEnabled() {
        return bluetoothAdapter.isEnabled();
    }

    public boolean isConnected() {
        return connected;
    }

    public void close() {
        stopScan();
        disconnect();
        context.unregisterReceiver(bluetoothStateReceiver);
    }

    public void startScan() {
        if (!isEnabled()) return;
        scannedBleDevices.clear();
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        if (bluetoothAdapter.isEnabled()) {
            try {
                bluetoothLeScanner.startScan(null, builder.build(), scanCallback);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            /*
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan();
                }
            }, 10000);
            */
        }
    }

    public void stopScan() {
        try {
            bluetoothLeScanner.stopScan(scanCallback);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            boolean founded = false;
            for (int i = 0; i < scannedBleDevices.size(); i++) {
                if (scannedBleDevices.get(i).device.getAddress().equals(result.getDevice().getAddress())) {
                    founded = true;
                    try {
                        if (scannedBleDevices.get(i).device.getName() == null && result.getDevice().getName() != null) {
                            scannedBleDevices.set(i, new ScannedBleDevice(result.getDevice(), result.getRssi()));
                            break;
                        } else {
                            return;
                        }
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!founded) scannedBleDevices.add(new ScannedBleDevice(result.getDevice(), result.getRssi()));
            //scannedBleDevices.sort((a, b) -> Integer.compare(b.rssi, a.rssi));
            callback.onDevicesAvailable(scannedBleDevices.toArray(new ScannedBleDevice[0]));
        }
    };

    public void disconnect() {
        if (!connected) return;
        try {
            bluetoothGatt.disconnect();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void connect(BluetoothDevice device) {
        bluetoothDevice = device;
        try {
            bluetoothGatt = device.connectGatt(context, false, bluetoothGattCallback);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void put(BlePacket packet) {
        boolean wasEmpty = map.isEmpty();
        map.put(packet.tag, packet.data);
        if (wasEmpty) dequeue();
    }

    private void dequeue() {
        if (map.isEmpty()) return;
        send(map.remove(map.keySet().iterator().next()));
    }

    private void send(byte[] bytes) {
        try {
            bluetoothGatt.writeCharacteristic(characteristicTx, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        Handler mainLooper = new Handler(Looper.getMainLooper());

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (!gatt.getDevice().getAddress().equals(bluetoothDevice.getAddress())) return;
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    connected = true;
                    try {
                        if (gatt.requestMtu(40)) Log.i("Lucas", "MTU adjusted.");
                        else Log.e("Lucas", "MTU request failure!");
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onConnectionChanged(true);
                        }
                    });
                    try {
                        bluetoothGatt.discoverServices();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    connected = false;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onConnectionChanged(false);
                        }
                    });
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService bluetoothGattService = gatt.getService(UUID.fromString(uuidService));
                characteristicTx = bluetoothGattService.getCharacteristic(UUID.fromString(uuidTx));
                BluetoothGattCharacteristic characteristicRx = bluetoothGattService.getCharacteristic(UUID.fromString(uuidRx));
                try {
                    gatt.setCharacteristicNotification(characteristicRx, true);
                    List<BluetoothGattDescriptor> list = characteristicRx.getDescriptors();
                    int properties = characteristicRx.getProperties();
                    for (BluetoothGattDescriptor descriptor : list) {
                        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        } else if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        }
                    }

                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onServicesDiscovered(true);
                    }
                });
            } else {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onServicesDiscovered(false);
                    }
                });
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                callback.onReceive(value);
                return;
            }
            mainLooper.post(new Runnable() {
                @Override
                public void run() {
                    callback.onReceive(value);
                }
            });
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            dequeue();
        }
    };

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        callback.onStateChanged(false);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        callback.onStateChanged(true);
                        break;
                }
            }
        }
    };
}
