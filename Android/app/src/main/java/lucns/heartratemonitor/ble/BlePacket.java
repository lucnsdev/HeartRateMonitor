package lucns.heartratemonitor.ble;

public class BlePacket {

    byte[] data;
    String tag;

    public BlePacket(String tag, byte[] data) {
        this.tag = tag;
        this.data = data;
    }
}
