package lucns.heartratemonitor.wave_generator;

import java.util.Random;

public class SineWaveGenerator3 {

    private double frequencyHz;

    private int minAmplitude;
    private int maxAmplitude;
    private int currentAmplitude;

    private long updateIntervalMs;
    private long lastUpdateTime;

    private double phase;
    private long lastTime;

    private final Random random = new Random();

    public SineWaveGenerator3(double frequencyHz, int minAmp, int maxAmp, long updateIntervalMs) {
        setFrequency(frequencyHz);
        setAmplitudeRange(minAmp, maxAmp);

        this.updateIntervalMs = updateIntervalMs;

        this.lastTime = System.nanoTime();
        this.lastUpdateTime = System.currentTimeMillis();

        randomizeAmplitude();
    }

    public void setFrequency(double frequencyHz) {
        if (frequencyHz <= 0) {
            throw new IllegalArgumentException("A frequência deve ser maior que zero");
        }
        this.frequencyHz = frequencyHz;
    }

    public void setAmplitudeRange(int minAmp, int maxAmp) {
        if (minAmp < 0 || maxAmp > 32767 || minAmp > maxAmp) {
            throw new IllegalArgumentException("Intervalo de amplitude inválido");
        }
        this.minAmplitude = minAmp;
        this.maxAmplitude = maxAmp;
    }

    private void randomizeAmplitude() {
        currentAmplitude = minAmplitude + random.nextInt(maxAmplitude - minAmplitude + 1);
    }

    public int next() {
        long nowNano = System.nanoTime();
        long nowMs = System.currentTimeMillis();

        double deltaTime = (nowNano - lastTime) / 1_000_000_000.0;
        lastTime = nowNano;

        // troca aleatória da amplitude em intervalos definidos
        if ((nowMs - lastUpdateTime) >= updateIntervalMs) {
            randomizeAmplitude();
            lastUpdateTime = nowMs;
        }

        // evolução contínua da fase
        phase += 2.0 * Math.PI * frequencyHz * deltaTime;

        if (phase > 2.0 * Math.PI) {
            phase = phase % (2.0 * Math.PI);
        }

        double sine = Math.sin(phase);

        // escala para 0..currentAmplitude
        return (int) ((sine + 1.0) * 0.5 * currentAmplitude);
    }
}
