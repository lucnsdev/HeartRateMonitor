package lucns.heartratemonitor.views.textview;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

public class AutofitTextView extends TextView {

    private String baseText;

    public AutofitTextView(Context context) {
        super(context);
        init();
    }

    public AutofitTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AutofitTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setSingleLine(true);
        setMaxLines(1);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                autoSizeWithText(baseText);
            }
        });
    }

    public void autoSizeWithText(String text) {
        this.baseText = text;
        Rect bounds = new Rect();
        TextPaint textPaint = getPaint();
        int size = 100;
        while (true) {
            textPaint.setTextSize(size);
            textPaint.getTextBounds(text, 0, text.length(), bounds);
            if (bounds.width() > getWidth()) {
                size--;
                break;
            }
            size++;
        }
        textPaint.setTextSize(size);
        //Log.d("Lucas", "text: " + text + " textSize: " + textSize + " bounds: " + bounds.width() + "x" + bounds.height());
        setText(text);
    }

    public void setText(String text) {
        super.setText(text);
        setIncludeFontPadding(false);
        setPadding(getPaddingLeft(), -100, getPaddingRight(), getPaddingBottom());
    }
}