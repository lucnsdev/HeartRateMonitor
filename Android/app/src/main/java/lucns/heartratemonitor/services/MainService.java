package lucns.heartratemonitor.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import lucns.heartratemonitor.ble.BleController;

public class MainService extends Service {

    public class LocalBinder extends Binder {
        public MainService getServiceInstance() {
            return MainService.this;
        }
    }

    private LocalBinder iBinder;
    private BleController bleController;

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        iBinder = new LocalBinder();
        bleController = new BleController(this, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        iBinder = null;
        bleController.close();
    }

    public BleController getBleManagerControl() {
        return bleController;
    }
}
