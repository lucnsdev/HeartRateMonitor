package lucns.heartratemonitor.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import lucns.heartratemonitor.utils.Annotator;

public class DataController {

    public interface OnValuesChangedListener {
        void onValuesChanged();
    }

    public static double DEFAULT_COMPENSATION = 100.0d;

    private int size = 1024;
    private int[] a0Samples, a1Samples, a2Samples, a3Samples;
    private double a0Compensation = 1;
    private double a1Compensation = 1;
    private double a2Compensation = 1;
    private double a3Compensation = 1;
    private final Map<String, OnValuesChangedListener> map;

    private static DataController instance;

    private DataController() {
        map = new HashMap<>();
        load();
    }

    public static DataController getInstance() {
        if (instance == null) {
            synchronized (DataController.class) {
                instance = new DataController();
            }
        }
        return instance;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void clear() {
        a0Samples = new int[0];
        a1Samples = new int[0];
        a2Samples = new int[0];
        a3Samples = new int[0];
    }

    public static double getResolutionVoltage() {
        return 3.3 / 3400; //4096; // 3400 Esp32-H2 Hardware limitation
    }

    public void addCallback(String tag, OnValuesChangedListener callback) {
        map.put(tag, callback);
    }

    public void removeCallback(String tag) {
        map.remove(tag);
    }

    public void putValues(int a0, int a1, int a2, int a3) {
        putA0(a0);
        putA1(a1);
        putA2(a2);
        putA3(a3);
    }

    public void notifyCallbacks() {
        for (String key : map.keySet()) {
            map.get(key).onValuesChanged();
        }
    }

    public int getSamplesCount() {
        return a0Samples.length;
    }

    public void setCompensations(double[] compensations) {
        a0Compensation = compensations[0];
        a1Compensation = compensations[1];
        a2Compensation = compensations[2];
        a3Compensation = compensations[3];
    }

    public double[] getCompensations() {
        return new double[]{a0Compensation, a1Compensation, a2Compensation, a3Compensation};
    }

    public int[] getA0Samples() {
        return a0Samples;
    }

    public int[] getA1Samples() {
        return a1Samples;
    }

    public int[] getA2Samples() {
        return a2Samples;
    }

    public int[] getA3Samples() {
        return a3Samples;
    }

    public double[] getVoltages() {
        double parcel = DataController.getResolutionVoltage();
        double v0 = a0Samples[a0Samples.length - 1] * parcel * a0Compensation;
        double v1 = a1Samples[a1Samples.length - 1] * parcel * a1Compensation;
        double v2 = a2Samples[a2Samples.length - 1] * parcel * a2Compensation;
        double v3 = a3Samples[a3Samples.length - 1] * parcel * a3Compensation;
        return new double[]{v0, v1, v2, v3};
    }

    public double[] getVoltagesWithoutCompensations() {
        double parcel = DataController.getResolutionVoltage();
        double v0 = a0Samples[a0Samples.length - 1] * parcel;
        double v1 = a1Samples[a1Samples.length - 1] * parcel;
        double v2 = a2Samples[a2Samples.length - 1] * parcel;
        double v3 = a3Samples[a3Samples.length - 1] * parcel;
        return new double[]{v0, v1, v2, v3};
    }

    private void putA0(int v) {
        if (a0Samples == null) {
            a0Samples = new int[]{v};
        } else if (a0Samples.length >= size) {
            for (int i = 1; i < size; i++) {
                a0Samples[i - 1] = a0Samples[i];
            }
            a0Samples[size - 1] = v;
        } else {
            int[] a = new int[a0Samples.length + 1];
            for (int i = 0; i < a0Samples.length; i++) a[i] = a0Samples[i];
            a[a0Samples.length] = v;
            a0Samples = a;
        }
    }

    private void putA1(int v) {
        if (a1Samples == null) {
            a1Samples = new int[]{v};
        } else if (a1Samples.length >= size) {
            for (int i = 1; i < size; i++) {
                a1Samples[i - 1] = a1Samples[i];
            }
            a1Samples[size - 1] = v;
        } else {
            int[] a = new int[a1Samples.length + 1];
            for (int i = 0; i < a1Samples.length; i++) a[i] = a1Samples[i];
            a[a1Samples.length] = v;
            a1Samples = a;
        }
    }

    private void putA2(int v) {
        if (a2Samples == null) {
            a2Samples = new int[]{v};
        } else if (a2Samples.length >= size) {
            for (int i = 1; i < size; i++) {
                a2Samples[i - 1] = a2Samples[i];
            }
            a2Samples[size - 1] = v;
        } else {
            int[] a = new int[a2Samples.length + 1];
            for (int i = 0; i < a2Samples.length; i++) a[i] = a2Samples[i];
            a[a2Samples.length] = v;
            a2Samples = a;
        }
    }

    private void putA3(int v) {
        if (a3Samples == null) {
            a3Samples = new int[]{v};
        } else if (a3Samples.length >= size) {
            for (int i = 1; i < size; i++) {
                a3Samples[i - 1] = a3Samples[i];
            }
            a3Samples[size - 1] = v;
        } else {
            int[] a = new int[a3Samples.length + 1];
            for (int i = 0; i < a3Samples.length; i++) a[i] = a3Samples[i];
            a[a3Samples.length] = v;
            a3Samples = a;
        }
    }

    public void save() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("a0_compensation", a0Compensation);
            jsonObject.put("a1_compensation", a1Compensation);
            jsonObject.put("a2_compensation", a2Compensation);
            jsonObject.put("a3_compensation", a3Compensation);
            new Annotator("settings.json").setContent(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        Annotator annotator = new Annotator("settings.json");
        if (!annotator.exists()) return;
        try {
            JSONObject jsonObject = new JSONObject(annotator.getContent());
            a0Compensation = jsonObject.getDouble("a0_compensation");
            a1Compensation = jsonObject.getDouble("a1_compensation");
            a2Compensation = jsonObject.getDouble("a2_compensation");
            a3Compensation = jsonObject.getDouble("a3_compensation");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
