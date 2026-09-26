package lucns.heartratemonitor.data;

import java.util.ArrayList;
import java.util.List;

public class MinAmplitudeDetector {

    private static final int BUFFER_SIZE = 1024;
    private static final int SMOOTH_WINDOW = 41;

    private final int[] buffer = new int[BUFFER_SIZE];
    private int index = 0;
    private int count = 0;

    public void setBuffer(int[] buffer) {
        for (int v : buffer) {
            next(v);
        }
    }

    public void next(int value) {
        buffer[index] = value;
        index = (index + 1) % BUFFER_SIZE;
        if (count < BUFFER_SIZE) count++;
    }

    public double getMinimumAverage() {
        if (count < SMOOTH_WINDOW)
            return Double.NaN;

        double[] smooth = smooth();
        List<Double> minimos = new ArrayList<>();
        for (int i = 1; i < smooth.length - 1; i++) {
            if (smooth[i] <= smooth[i - 1] && smooth[i] < smooth[i + 1]) {
                minimos.add(smooth[i]);
            }
        }

        if (minimos.isEmpty()) return Double.NaN;
        double soma = 0;
        for (double v : minimos) soma += v;
        return soma / minimos.size();
    }

    private double[] smooth() {
        int n = count;
        double[] out = new double[n];
        int half = SMOOTH_WINDOW / 2;

        for (int i = 0; i < n; i++) {
            double soma = 0;
            int qtd = 0;
            for (int j = i - half; j <= i + half; j++) {
                if (j >= 0 && j < n) {
                    soma += getOrdered(j);
                    qtd++;
                }
            }
            out[i] = soma / qtd;
        }
        return out;
    }

    private int getOrdered(int pos) {
        int start = (count == BUFFER_SIZE) ? index : 0;
        return buffer[(start + pos) % BUFFER_SIZE];
    }
}
