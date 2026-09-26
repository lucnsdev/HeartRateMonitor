package lucns.heartratemonitor.data;

import lucns.heartratemonitor.utils.Constants;

public class StabilizerVerifier extends Stabilizer {

    private int lastValue, lastAmplitude;
    private long lastTime;
    private long lastDuration;

    public StabilizerVerifier() {}

    public boolean isValid(int amplitude) {
        boolean valid =  System.currentTimeMillis() - lastTime > 250 || lastDuration > 1000 && getDifference(amplitude, lastAmplitude) < Constants.goodPeakToPeak / 2;
        lastAmplitude = amplitude;
        return valid;
    }

    @Override
    public int put(int value) {
        int current = super.put(value);
        if (current != lastValue && getDifference(current, lastValue) > 5) {
            long now = System.currentTimeMillis();
            lastDuration = now - lastTime;
            lastTime = now;
            lastValue = current;
        }
        return lastValue;
    }

    public int getDifference(int a, int b) {
        if (a > b) return a - b;
        return b - a;
    }
}
