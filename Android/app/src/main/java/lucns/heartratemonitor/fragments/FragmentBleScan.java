package lucns.heartratemonitor.fragments;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import lucns.heartratemonitor.R;
import lucns.heartratemonitor.ble.ScannedBleDevice;
import lucns.heartratemonitor.utils.Notify;
import lucns.heartratemonitor.utils.Utils;
import lucns.heartratemonitor.views.FragmentView;

public class FragmentBleScan extends FragmentView {

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(BluetoothDevice device);
    }

    private ListView listView;
    private OnDeviceSelectedListener listener;
    private ProgressBar progressBar;
    private TextView textTitle;

    public FragmentBleScan(Activity activity, OnDeviceSelectedListener listener) {
        super(activity);
        this.listener = listener;
    }

    @Override
    public void onCreate() {
        setContentView(R.layout.fragment_ble_scan);

        textTitle = findViewById(R.id.textTitle);
        progressBar = findViewById(R.id.progressBar);
        listView = findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ScannedBleDevice data = (ScannedBleDevice) listView.getAdapter().getItem(position);
                listener.onDeviceSelected(data.device);
            }
        });
    }

    public void changeEnabled(boolean enabled) {
        if (!enabled) {
            textTitle.setText(R.string.bluetooth_disabled);
            progressBar.setMax(100);
            progressBar.setProgress(100);
            progressBar.setIndeterminate(false);
            listView.setVisibility(View.INVISIBLE);
            listView.setAdapter(new ArrayAdapter<String>(getActivity(), 0, new String[0]));
            return;
        }
        textTitle.setText(R.string.scanning);
        listView.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
    }

    public void updateList(ScannedBleDevice[] devices) {
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        listView.setAdapter(new ArrayAdapter<ScannedBleDevice>(getActivity(), R.layout.list_item_ble, devices) {

            @Override
            public int getCount() {
                return devices.length;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ScannedBleDevice data = devices[position];
                View view = layoutInflater.inflate(R.layout.list_item_ble, null, false);
                if (position == 0) {
                    if (getCount() == 1) {
                        view.setBackgroundResource(R.drawable.item_background_single);
                    } else {
                        view.setBackgroundResource(R.drawable.item_background_first);
                    }
                } else if (position == getCount() - 1) {
                    view.setBackgroundResource(R.drawable.item_background_last);
                } else {
                    view.setBackgroundResource(R.drawable.item_background_square);
                }
                TextView textTopStart = view.findViewById(R.id.textTopStart);
                TextView textTopEnd = view.findViewById(R.id.textTopEnd);
                TextView textBottomStart = view.findViewById(R.id.textBottomStart);
                try {
                    textTopStart.setText(data.device.getName() == null ? getActivity().getString(R.string.not_specified) : data.device.getName());
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                textBottomStart.setText(data.device.getAddress());
                textTopEnd.setText(String.valueOf(data.rssi));
                return view;
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
