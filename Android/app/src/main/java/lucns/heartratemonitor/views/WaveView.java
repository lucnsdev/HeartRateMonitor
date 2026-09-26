package lucns.heartratemonitor.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

import lucns.heartratemonitor.R;
import lucns.heartratemonitor.data.Stabilizer;
import lucns.heartratemonitor.utils.Constants;

public class WaveView extends View {

    private ColorGradient colorGradient;
    private Paint paintLinePrimary, paintLineSecondary, paintLineWave, paintTextVoltage, paintTextTimeBig, paintTextTimeSmall;

    private int[] amplitudes;
    private Rect bounds;
    private int minAmplitude, maxAmplitude;
    private boolean acrossVariations;
    private boolean showCentralLine;
    private int amplitudePeakToPeak;
    private final int millisecondsPerRow = 100;
    private final int ONE_SECOND = 1000;
    private final int bottomPadding = 96;
    private int packetsCounter;
    private int packetLength;

    public WaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) init();
    }

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) init();
    }

    public WaveView(Context context) {
        super(context);
        if (!isInEditMode()) init();
    }

    private void init() {
        Typeface typeface = getContext().getResources().getFont(R.font.barlow_bold);

        paintLinePrimary = new Paint();
        paintLinePrimary.setColor(getContext().getColor(R.color.gray));
        paintLinePrimary.setStyle(Paint.Style.STROKE);

        paintLineSecondary = new Paint();
        paintLineSecondary.setColor(getContext().getColor(R.color.gray_6));
        paintLineSecondary.setStyle(Paint.Style.STROKE);

        paintLineWave = new Paint();
        paintLineWave.setColor(getContext().getColor(R.color.accent));
        paintLineWave.setStyle(Paint.Style.STROKE);
        paintLineWave.setStrokeWidth(2f);

        paintTextVoltage = new Paint();
        paintTextVoltage.setColor(Color.WHITE);
        paintTextVoltage.setTextSize(48f);
        paintTextVoltage.setAntiAlias(true);
        paintTextVoltage.setTypeface(typeface);

        paintTextTimeBig = new Paint();
        paintTextTimeBig.setColor(getContext().getColor(R.color.gray));
        paintTextTimeBig.setTextSize(48f);
        paintTextTimeBig.setAntiAlias(true);
        paintTextTimeBig.setTypeface(typeface);

        paintTextTimeSmall = new Paint();
        paintTextTimeSmall.setColor(getContext().getColor(R.color.gray_4));
        paintTextTimeSmall.setTextSize(24f);
        paintTextTimeSmall.setAntiAlias(true);
        paintTextTimeSmall.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        bounds = new Rect();
        colorGradient = new ColorGradient();
    }

    public void showCentralLine(boolean showCentralLine) {
        this.showCentralLine = showCentralLine;
    }

    public void setAcrossVariations(boolean acrossVariations) {
        this.acrossVariations = acrossVariations;
    }

    public void setPacketLength(int length) {
        packetLength = length;
    }

    public int getAmplitudePeak() {
        return amplitudePeakToPeak;
    }

    public int getColor() {
        double percentage = amplitudePeakToPeak / (double) Constants.goodPeakToPeak;
        return colorGradient.getValor(percentage);
    }

    public void setValues(int[] values, int averageWindowSize) {
        packetsCounter++;
        int width = getWidth();
        if (values.length <= width) {
            amplitudes = values;
        } else {
            amplitudes = new int[width];
            for (int i = 0; i < width; i++) {
                amplitudes[i] = values[i + (values.length - width)];
            }
        }
        amplitudes = Stabilizer.filterNoise(amplitudes, averageWindowSize);
        maxAmplitude = 0;
        for (int i : amplitudes) {
            if (i > maxAmplitude) maxAmplitude = i;
        }
        minAmplitude = maxAmplitude;
        for (int i : amplitudes) {
            if (i < minAmplitude) minAmplitude = i;
        }
        amplitudePeakToPeak = (maxAmplitude - minAmplitude) * Constants.resolution;
        paintLineWave.setColor(getColor());

        invalidate();
    }

    public void reset() {
        amplitudes = null;
        packetsCounter = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int halfHeight = (getHeight() - bottomPadding) / 2;
        if (showCentralLine) canvas.drawLine(0, halfHeight, width, halfHeight, paintLinePrimary);
        canvas.drawLine(0, height - bottomPadding, width, height - bottomPadding, paintLinePrimary);
        canvas.drawRect(0, 0, width, height, paintLinePrimary);

        if (amplitudes != null && amplitudes.length > 0) {
            int length = Math.min(amplitudes.length, width);
            drawVerticallyLines(canvas, length);
            drawWaveForm(canvas, length);
            String text = String.valueOf(maxAmplitude * Constants.resolution);
            paintTextVoltage.getTextBounds(text, 0, text.length(), bounds);
            canvas.drawText(text, 32, 8 + bounds.height(), paintTextVoltage);
            canvas.drawText(String.valueOf((((maxAmplitude - minAmplitude) / 2) +  minAmplitude) * Constants.resolution), 32, (height / 2f) - bounds.height(), paintTextTimeBig);
            canvas.drawText(String.valueOf(minAmplitude * Constants.resolution), 32, height - (bounds.height()), paintTextVoltage);
        }
    }

    private void drawVerticallyLines(Canvas canvas, int length) {
        if (length < 2 * millisecondsPerRow) return;
        int width = getWidth();
        int height = getHeight();

        int totalShift = packetsCounter * packetLength;
        int xOffset = totalShift % millisecondsPerRow;
        int baseLineIndex = totalShift / millisecondsPerRow;
        int numberOfLinesInDisplay = (width / millisecondsPerRow) + 1;

        for (int a = 0; a <= numberOfLinesInDisplay; a++) {
            int x = (a * millisecondsPerRow) - xOffset;
            int absoluteLineIndex = baseLineIndex + a;
            long elapsedSeconds = absoluteLineIndex / 10;
            if (absoluteLineIndex % 10 == 0) {
                String text = String.format(Locale.getDefault(), "%ds", elapsedSeconds);
                paintLineSecondary.setColor(getContext().getColor(R.color.white));
                canvas.drawText(text, x - (paintTextTimeBig.measureText(text) / 2), height - (bounds.height()), paintTextTimeBig);
                canvas.drawLine(x, 0, x, height - bottomPadding, paintLineSecondary);
            } else {
                paintLineSecondary.setColor(getContext().getColor(R.color.gray_6));
                String text = String.valueOf(absoluteLineIndex);
                text = elapsedSeconds + "." + text.substring(text.length() - 1);
                canvas.drawText(text, x - (paintTextTimeSmall.measureText(text) / 2), height - (bounds.height()), paintTextTimeSmall);
                canvas.drawLine(x, 0, x, height - 56, paintLineSecondary);
            }
        }
    }

    private void drawWaveForm(Canvas canvas, int length) {
        int width = getWidth();
        int height = getHeight() - bottomPadding;
        float pixelsPerAmplitude;
        if (acrossVariations) pixelsPerAmplitude = (float) height / (maxAmplitude - minAmplitude);
        else pixelsPerAmplitude = (float) height / maxAmplitude;
        float xWidth = (float) width / length;
        float lastY = 0;
        for (int a = length; a > 0; a--) {
            int x = a - 1;
            float current;
            if (acrossVariations) current = (amplitudes[x + Math.max(amplitudes.length - width, 0)] - minAmplitude) * pixelsPerAmplitude;
            else current = (amplitudes[x + Math.max(amplitudes.length - width, 0)]) * pixelsPerAmplitude;
            if (current < 1) current = 1;
            canvas.drawLine(xWidth * x, lastY == 0 ? height - current : lastY, xWidth * (x + 1), height - current, paintLineWave);
            lastY = height - current;
        }
    }

    public static class ColorGradient {

        public int getValor(double valor) {
            double v = Math.min(Math.max(valor, 0.0), 1.0);

            // Pontos de parada do gradiente (Ancoras em RGB)
            // 0.0 -> Vermelho Puro   (255,   0, 0)
            // 0.25 -> Laranja         (255, 127, 0)
            // 0.5 -> Amarelo         (255, 255, 0)
            // 1.00 -> Verde           (  0, 200, 0)
            double red = 0.25;
            double yellow = 0.4;

            int r, g, b;

            if (v <= red) {
                // Segmento 1: Vermelho -> Laranja
                double t = v / red;
                r = 255;
                g = (int) lerp(0, 127, t);
                b = 0;
            } else if (v <= yellow) {
                // Segmento 2: Laranja -> Amarelo
                double t = (v - red) / red;
                r = 255;
                g = (int) lerp(127, 255, t);
                b = 0;
            } else {
                // Segmento 3: Amarelo -> Verde
                double t = (v - yellow) / 0.75;
                r = (int) lerp(255, 0, t);
                g = (int) lerp(255, 200, t);
                b = 0;
            }

            return (0xFF << 24) | (r << 16) | (g << 8) | b;
        }

        private double lerp(double start, double end, double t) { // Linear interpolation
            return start + t * (end - start);
        }
    }
}
