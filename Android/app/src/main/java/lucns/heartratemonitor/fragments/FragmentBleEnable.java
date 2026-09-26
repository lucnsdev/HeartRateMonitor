package lucns.heartratemonitor.fragments;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.view.View;

import lucns.heartratemonitor.R;
import lucns.heartratemonitor.views.FragmentView;

public class FragmentBleEnable extends FragmentView {

    public FragmentBleEnable(Activity activity) {
        super(activity);
    }

    @Override
    public void onCreate() {
        setContentView(R.layout.fragment_ble_enable);
        findViewById(R.id.button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    getActivity().startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1234);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        });
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
}
