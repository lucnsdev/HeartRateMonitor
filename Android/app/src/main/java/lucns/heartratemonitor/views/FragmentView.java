package lucns.heartratemonitor.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

public abstract class FragmentView extends RelativeLayout {

    private View root;
    private Activity activity;
    private int index;
    private SliderView sliderView;
    private boolean isCreated, isResumed, isPaused, isDestroyed;

    public FragmentView(Context context) {
        super(context);
        initialize();
    }

    public FragmentView(Activity activity) {
        super(activity);
        this.activity = activity;
        initialize();
    }

    public FragmentView(Activity activity, AttributeSet attrs) {
        super(activity, attrs);
        initialize();
    }

    protected void setSlider(SliderView sliderView) {
        this.sliderView = sliderView;
    }

    public SliderView getSliderView() {
        return sliderView;
    }

    public Activity getActivity() {
        return (Activity) getContext();
    }

    public void setFragmentIndex(int index) {
        this.index = index;
    }

    public int getFragmentIndex() {
        return index;
    }

    protected void create() {
        if (isCreated) return;
        isCreated = true;
        isDestroyed = false;
        onCreate();
    }

    protected void resume() {
        if (isResumed) return;
        isResumed = true;
        isPaused = false;
        onResume();
    }

    protected void pause() {
        if (isPaused) return;
        isPaused = true;
        isResumed = false;
        onPause();
    }

    protected void destroy() {
        if (isDestroyed) return;
        isDestroyed = true;
        isCreated = false;
        onDestroy();
    }

    public abstract void onCreate();

    public abstract void onResume();

    public abstract void onPause();

    public abstract void onDestroy();

    public boolean onBackPressed() {
        return true;
    }

    public void finish() {
        activity.finish();
    }

    public void startActivity(Intent intent) {
        getContext().startActivity(intent);
    }

    public void overridePendingTransition(int in, int out) {
        activity.overridePendingTransition(in, out);
    }

    private void initialize() {
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void setContentView(int res) {
        root = LayoutInflater.from(getContext()).inflate(res, null, false);
        addView(root, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(new Runnable() {
            @Override
            public void run() {
                //root.setBackgroundColor(Color.TRANSPARENT);
                /*
                int visibility = root.getVisibility();
                root.setVisibility(View.GONE);
                root.setVisibility(visibility);
                 */
                root.requestLayout();
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        destroy();
    }

    public int getColor(int res) {
        return getContext().getColor(res);
    }

    public String getString(int res) {
        return getContext().getString(res);
    }
}
