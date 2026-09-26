package lucns.heartratemonitor.wave_generator;

import java.util.Random;

public class RandomWaveGenerator {

    private double minFrequencyHz;
    private double maxFrequencyHz;
    private double currentFrequencyHz;

    private int minAmplitude;
    private int maxAmplitude;
    private int currentAmplitude;

    private long lastUpdateTime;
    private long updateIntervalMs;

    private double phase; // fase acumulada
    private long lastTime;

    private final Random random = new Random();

    public RandomWaveGenerator(double minHz, double maxHz, int minAmp, int maxAmp, long updateIntervalMs) {
        setFrequencyRange(minHz, maxHz);
        setAmplitudeRange(minAmp, maxAmp);

        this.updateIntervalMs = updateIntervalMs;

        this.lastTime = System.nanoTime();
        this.lastUpdateTime = System.currentTimeMillis();

        randomizeParameters();
    }

    public void setFrequencyRange(double minHz, double maxHz) {
        if (minHz <= 0 || maxHz <= 0 || minHz > maxHz) {
            throw new IllegalArgumentException("Intervalo de frequência inválido");
        }
        this.minFrequencyHz = minHz;
        this.maxFrequencyHz = maxHz;
    }

    public void setAmplitudeRange(int minAmp, int maxAmp) {
        if (minAmp < 0 || maxAmp > 32767 || minAmp > maxAmp) {
            throw new IllegalArgumentException("Intervalo de amplitude inválido");
        }
        this.minAmplitude = minAmp;
        this.maxAmplitude = maxAmp;
    }

    private void randomizeParameters() {
        currentFrequencyHz = minFrequencyHz + (maxFrequencyHz - minFrequencyHz) * random.nextDouble();
        currentAmplitude = minAmplitude + random.nextInt(maxAmplitude - minAmplitude + 1);
    }

    public int next() {
        long nowNano = System.nanoTime();
        long nowMs = System.currentTimeMillis();

        // tempo decorrido em segundos
        double deltaTime = (nowNano - lastTime) / 1_000_000_000.0;
        lastTime = nowNano;

        // atualizar parâmetros aleatórios periodicamente
        if ((nowMs - lastUpdateTime) >= updateIntervalMs) {
            randomizeParameters();
            lastUpdateTime = nowMs;
        }

        // acumular fase continuamente (evita descontinuidade)
        phase += 2.0 * Math.PI * currentFrequencyHz * deltaTime;

        // manter fase limitada (evita overflow numérico)
        if (phase > 2.0 * Math.PI) {
            phase = phase % (2.0 * Math.PI);
        }

        double sine = Math.sin(phase);

        // escala para 0..currentAmplitude
        return (int) ((sine + 1.0) * 0.5 * currentAmplitude);
    }
}