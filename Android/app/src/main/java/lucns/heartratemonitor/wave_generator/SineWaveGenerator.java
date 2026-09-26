package lucns.heartratemonitor.wave_generator;

public class SineWaveGenerator {

    private double frequencyHz;
    private long startTime;
    private double amplitude;

    public SineWaveGenerator(double frequencyHz, double amplitude) {
        this.amplitude = amplitude;
        setFrequency(frequencyHz);
        this.startTime = System.nanoTime();
    }

    public void setFrequency(double frequencyHz) {
        this.frequencyHz = frequencyHz / 1000;
    }

    public int next() {
        long now = System.nanoTime();
        double elapsedSeconds = (now - startTime) / 1_000_000_000.0;
        double phase = 2.0 * Math.PI * frequencyHz * elapsedSeconds;
        double sine = Math.sin(phase);
        return (int) ((sine + 1.0) * 0.5 * amplitude);
    }
}