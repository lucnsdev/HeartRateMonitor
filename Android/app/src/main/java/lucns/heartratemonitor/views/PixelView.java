package lucns.heartratemonitor.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

public class PixelView extends View {

    private int color;

    public PixelView(Context context) {
        super(context);
    }

    public PixelView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PixelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTemperature(double temperature) {
        color = getThermalColor(temperature, 20, 40);
        invalidate();
    }

    public static int getThermalColor(double temperature, double minTemp, double maxTemp) {
        if (temperature < minTemp) temperature = minTemp;
        if (temperature > maxTemp) temperature = maxTemp;

        float t = (float) ((temperature - minTemp) / (maxTemp - minTemp));
        int[][] colors = {
                {0,   0,   0},      // Preto
                {0,   0, 255},      // Azul
                {0, 255, 255},      // Ciano
                {0, 255,   0},      // Verde
                {255,255,  0},      // Amarelo
                {255,  0,  0},      // Vermelho
                {255,255,255}       // Branco
        };

        float position = t * (colors.length - 1);
        int index = (int) Math.floor(position);

        if (index >= colors.length - 1) return Color.rgb(colors[colors.length - 1][0], colors[colors.length - 1][1], colors[colors.length - 1][2]);

        float fraction = position - index;
        int r = (int) (colors[index][0] + fraction * (colors[index + 1][0] - colors[index][0]));
        int g = (int) (colors[index][1] + fraction * (colors[index + 1][1] - colors[index][1]));
        int b = (int) (colors[index][2] + fraction * (colors[index + 1][2] - colors[index][2]));
        return Color.rgb(r, g, b);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(color);
    }
}
