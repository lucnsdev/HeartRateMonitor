package lucns.heartratemonitor.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lucns.heartratemonitor.R;
import lucns.heartratemonitor.ble.BleController;
import lucns.heartratemonitor.ble.BlePacket;
import lucns.heartratemonitor.ble.ScannedBleDevice;
import lucns.heartratemonitor.data.DataController;
import lucns.heartratemonitor.data.Stabilizer;
import lucns.heartratemonitor.data.StabilizerVerifier;
import lucns.heartratemonitor.services.MainService;
import lucns.heartratemonitor.services.ServiceController;
import lucns.heartratemonitor.utils.AppPreferences;
import lucns.heartratemonitor.utils.Constants;
import lucns.heartratemonitor.utils.Notify;
import lucns.heartratemonitor.utils.Utils;
import lucns.heartratemonitor.views.WaveView;

public class DataViewerActivity extends Activity {

    private BleController bleController;
    private CustomDialogs dialog;
    private TextView textSamples, textSampleRate;
    private DataController dataController;
    private PopupMenu popupMenu;

    private TextView textValue, textVpp;
    private WaveView waveView;
    private int sampleRate;
    private StabilizerVerifier stabilizer;
    private Handler handlerTextBpm;
    private Runnable runnableTextBpm;
    private int averageWindowSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_viewer);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        stabilizer = new StabilizerVerifier();
        dataController = DataController.getInstance();
        dataController.setSize(Resources.getSystem().getDisplayMetrics().widthPixels);
        dataController.addCallback("heart_rate_monitor", new DataController.OnValuesChangedListener() {
            @Override
            public void onValuesChanged() {
                updateValues();
            }
        });

        textValue = findViewById(R.id.textValue);
        textVpp = findViewById(R.id.textVpp);
        waveView = findViewById(R.id.waveView);
        //waveView.setVelocity(4f);
        waveView.setAcrossVariations(true);
        waveView.showCentralLine(true);

        String[] averrages = getResources().getStringArray(R.array.averages);
        int index = AppPreferences.getInt("average_window_position", 0);
        averageWindowSize = Integer.parseInt(averrages[index]);
        Spinner spinner = findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                averrages
        );
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(index);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //((TextView) parent.getChildAt(0)).setTextColor(getColor(android.R.color.white));
                AppPreferences.setInt("average_window_position", position);
                averageWindowSize = Integer.parseInt(averrages[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        dataController = DataController.getInstance();
        dialog = new CustomDialogs(this);

        handlerTextBpm = new Handler(Looper.getMainLooper());
        runnableTextBpm = new Runnable() {
            @Override
            public void run() {
                textValue.setTextColor(getColor(R.color.green));
            }
        };

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.buttonMenu) {
                    //popupMenu.getMenu().getItem(1).setTitle(mainService.isMonitoring() ? R.string.disable_monitor : R.string.enable_monitor);
                    popupMenu.show();
                }
            }
        };
        ImageButton buttonMenu = findViewById(R.id.buttonMenu);
        buttonMenu.setOnClickListener(onClickListener);
        popupMenu = new PopupMenu(DataViewerActivity.this, buttonMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_clear) {
                    waveView.reset();
                    dataController.clear();
                }
                return true;
            }
        });
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_main, popupMenu.getMenu());

        textSamples = findViewById(R.id.textSamples);
        textSampleRate = findViewById(R.id.textSampleRate);

        ServiceController.getInstance(this, new ServiceController.OnServiceAvailableListener() {
            @Override
            public void onAvailable(MainService mainService) {
                bleController = mainService.getBleManagerControl();
                bleController.setCallback(callback);
                bleController.setUUIDs("1d01ffcf-a8d1-4e68-a738-62c53e3a0001", "1d01ffcf-a8d1-4e68-a738-62c53e3a0002", "1d01ffcf-a8d1-4e68-a738-62c53e3a0003");
            }
        });
    }

    private void updateValues() {
        int[] a0 = dataController.getA0Samples();
        //textValue.setText(String.valueOf(a0[a0.length - 1]));
        int calculed = calculateBPM(Stabilizer.filterNoise(a0, averageWindowSize), sampleRate);
        int bpm = stabilizer.put(calculed);
        waveView.setValues(a0, averageWindowSize);
        textVpp.setText(String.valueOf(waveView.getAmplitudePeak()));
        textVpp.setTextColor(waveView.getColor());

        if (!stabilizer.isValid(waveView.getAmplitudePeak()) ||  waveView.getAmplitudePeak() < Constants.goodPeakToPeak / 2) {
            handlerTextBpm.removeCallbacks(runnableTextBpm);
            //textValue.setText(R.string.stabilizing_signal);
            textValue.setTextColor(getColor(R.color.gray));
        } else {
            if (sampleRate > 0) textValue.setText(bpm + "bpm");
            handlerTextBpm.postDelayed(runnableTextBpm, 500);
        }
    }

    private int calculateBPM(int[] rawSamples, int sampleRate) {
        if (rawSamples == null || rawSamples.length < 2) return 0;
        int windowSize = 15;
        double[] smoothed = new double[rawSamples.length];

        for (int i = 0; i < rawSamples.length; i++) {
            double sum = 0;
            int count = 0;
            int start = Math.max(0, i - windowSize / 2);
            int end = Math.min(rawSamples.length - 1, i + windowSize / 2);

            for (int j = start; j <= end; j++) {
                sum += rawSamples[j];
                count++;
            }
            smoothed[i] = sum / count;
        }

        double min = smoothed[0];
        double max = smoothed[0];
        for (double val : smoothed) {
            if (val < min) min = val;
            if (val > max) max = val;
        }

        double threshold = min + (max - min) * 0.6;
        int minPeakDistance = (int) (sampleRate / (220.0 / 60.0));

        List<Integer> peakIndices = new ArrayList<>();
        int lastPeakIndex = -minPeakDistance;

        for (int i = 1; i < smoothed.length - 1; i++) {
            if (smoothed[i] >= smoothed[i - 1] && smoothed[i] > smoothed[i + 1]) {
                if (smoothed[i] > threshold && (i - lastPeakIndex) > minPeakDistance) {
                    peakIndices.add(i);
                    lastPeakIndex = i;
                }
            }
        }

        if (peakIndices.size() < 2) return 0;

        double totalInterval = 0;
        for (int i = 1; i < peakIndices.size(); i++) {
            totalInterval += (peakIndices.get(i) - peakIndices.get(i - 1));
        }

        double averageInterval = totalInterval / (peakIndices.size() - 1);
        double bpm = (60.0 * sampleRate) / averageInterval;
        return (int) Math.round(bpm);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) bleController.disconnect();
        //dataController.removeCallback("heart_rate_monitor");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bleController != null) bleController.setCallback(callback);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private final BleController.Callback callback = new BleController.Callback() {

        long start;
        int samples;

        @Override
        public void onStateChanged(boolean enabled) {
            Utils.vibrate();
            if (enabled) {
                if (dialog != null) dialog.dismiss();
            } else {
                dialog.showInfo(getString(R.string.bluetooth_disabled), new CustomDialogs.Callback() {
                    @Override
                    public void onClick() {
                        startActivity(new Intent(DataViewerActivity.this, MainActivity.class));
                        finish();
                    }
                });
            }
        }

        @Override
        public void onDevicesAvailable(ScannedBleDevice[] devices) {
        }

        @Override
        public void onConnectionChanged(boolean connected) {
            if (isDestroyed() || isFinishing()) return;
            if (!connected) {
                Utils.vibrate();
                Notify.showToast(R.string.disconnected);
                startActivity(new Intent(DataViewerActivity.this, MainActivity.class));
                finish();
            }
        }

        @Override
        public void onServicesDiscovered(boolean success) {
        }

        @Override
        public void onReceive(byte[] data) {
            if (data.length % 2 > 0) {
                Log.e("lucas", "Wrong data length: " + data.length);
                return;
            }
            waveView.setPacketLength(data.length / 2);
            int amplitude;
            for (int i = 0; i < data.length; i += 2) {
                amplitude = ((data[i] & 0xFF) << 8) | (data[i + 1] & 0xFF);
                dataController.putValues(amplitude, 0, 0, 0);
            }
            dataController.notifyCallbacks();
            textSamples.setText(String.valueOf(dataController.getSamplesCount()));
            samples+= data.length / 2;
            long m = System.currentTimeMillis();
            if (start == 0) {
                start = m;
                return;
            }
            if (m - start >= 1000) {
                start = m;
                sampleRate = samples;
                textSampleRate.setText(samples + "/s");
                samples = 0;
            }
        }
    };
}
