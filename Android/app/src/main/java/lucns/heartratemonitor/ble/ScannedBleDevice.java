package lucns.heartratemonitor.ble;

import android.bluetooth.BluetoothDevice;

public class ScannedBleDevice {
    public BluetoothDevice device;
    public int rssi;

    public ScannedBleDevice(BluetoothDevice device, int rssi) {
        this.device = device;
        this.rssi = rssi;
    }
}
