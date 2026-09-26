package lucns.heartratemonitor.activities;

import android.app.Activity;
import android.app.Dialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import lucns.heartratemonitor.R;
import lucns.heartratemonitor.ble.BleController;
import lucns.heartratemonitor.ble.BlePacket;
import lucns.heartratemonitor.utils.AppPreferences;

public class CustomDialogs extends Dialog {

    public interface Callback {
        void onClick();
    }

    private Callback callback;

    public CustomDialogs(Activity activity) {
        super(activity, R.style.DialogTheme);
        setCancelable(true);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void showInfo(String title, Callback callback) {
        this.callback = callback;
        setContentView(R.layout.dialog_info);
        ((TextView) findViewById(R.id.textTitle)).setText(title);
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                callback.onClick();
            }
        });
        super.show();
    }

    public void showLeds(BleController controller) {
        setContentView(R.layout.dialog_leds);
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        int led = AppPreferences.getInt("selected_led", 0);
        int[] levels = new int[] {AppPreferences.getInt("level_green", 50), AppPreferences.getInt("level_red", 50), AppPreferences.getInt("level_infrared", 50)};
        switch (led) {
            case 1:
                ((RadioButton) findViewById(R.id.buttonRed)).setChecked(true);
                break;
            case 2:
                ((RadioButton) findViewById(R.id.buttonInfrared)).setChecked(true);
                break;
            default:
                ((RadioButton) findViewById(R.id.buttonGreen)).setChecked(true);
                break;
        }
        TextView textPercentage = findViewById(R.id.textPercentage);
        SeekBar seekBar = findViewById(R.id.seekBar);
        if (led == 2) {
            seekBar.setProgress(levels[2]);
            textPercentage.setText(levels[2] + "%");
        } else if (led == 1) {
            seekBar.setProgress(levels[1]);
            textPercentage.setText(levels[1] + "%");
        } else {
            seekBar.setProgress(levels[0]);
            textPercentage.setText(levels[0] + "%");
        }
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textPercentage.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                textPercentage.setText(String.valueOf(seekBar.getProgress()));
                int checkedId = radioGroup.getCheckedRadioButtonId();
                if (checkedId == R.id.buttonInfrared) {
                    AppPreferences.setInt("level_infrared", seekBar.getProgress());
                } else if (checkedId == R.id.buttonRed) {
                    AppPreferences.setInt("level_red", seekBar.getProgress());
                } else {
                    AppPreferences.setInt("level_green", seekBar.getProgress());
                }
                controller.put(new BlePacket("data", new byte[]{(byte) 2, (byte) seekBar.getProgress()}));
            }
        });
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.buttonInfrared) {
                    AppPreferences.setInt("selected_led", 2);
                    seekBar.setProgress(levels[2]);
                    controller.put(new BlePacket("data", new byte[]{(byte) 1, (byte) 2}));
                } else if (checkedId == R.id.buttonRed) {
                    AppPreferences.setInt("selected_led", 1);
                    seekBar.setProgress(levels[1]);
                    controller.put(new BlePacket("data", new byte[]{(byte) 1, (byte) 1}));
                } else {
                    AppPreferences.setInt("selected_led", 0);
                    seekBar.setProgress(levels[0]);
                    controller.put(new BlePacket("data", new byte[]{(byte) 1, (byte) 0}));
                }
            }
        });
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        super.show();
    }
}
