package com.sonvp.tooltip_simple;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;

import com.sonvp.tooltip.Tooltip;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToolTipView(v, Gravity.LEFT, "Tool 11111 111111 111111 111111 111111 111111 111111 111111 111111 111111",
                        ContextCompat.getColor(MainActivity.this, R.color.maroon), false);
            }
        });

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToolTipView(v, Gravity.LEFT, "Tool 11111 111111 111111 111111 111111 111111 111111 111111 111111 5555",
                        ContextCompat.getColor(MainActivity.this, R.color.maroon), true);
            }
        });
    }


    private void showToolTipView(View anchorView, int gravity, CharSequence text, int backgroundColor, boolean theme) {
        showToolTipView(anchorView, gravity, text, backgroundColor, 0L, theme);
    }

    private void showToolTipView(final View anchorView, int gravity, CharSequence text, int backgroundColor, long delay, boolean theme) {
        if (anchorView.getTag() != null) {
            ((Tooltip) anchorView.getTag()).remove();
            anchorView.setTag(null);
            return;
        }

        Tooltip tooltip = createToolTipView(text, anchorView, gravity, theme);
        if (delay > 0L) {
            tooltip.showDelayed(delay);
        } else {
            tooltip.show();
        }
        anchorView.setTag(tooltip);

        tooltip.setOnToolTipClickedListener(new Tooltip.OnToolTipClickedListener() {
            @Override
            public void onToolTipClicked(Tooltip tooltip) {
                anchorView.setTag(null);
            }
        });
    }

    private Tooltip createToolTipView(CharSequence text, View anchorView, int gravity, boolean theme) {

        Resources resources = getResources();
        int padding = resources.getDimensionPixelSize(R.dimen.padding);
        int textSize = resources.getDimensionPixelSize(R.dimen.text_size);
        int radius = resources.getDimensionPixelSize(R.dimen.radius);

        if (theme) {
            return new Tooltip.Builder(this)
                    .withAnchor(anchorView)
                    .withTooltipGravity(gravity)
                    .withText(text)
                    .withTextColor(Color.WHITE)
                    .withArrowGravity(Gravity.TOP)
                    .withTextSize(textSize)
                    .withPadding(padding, padding, padding, padding)
                    .withCornerRadius(radius)
                    .build();
        } else {
            return new Tooltip.Builder(this, anchorView,R.style.Tooltip)
                    .build();
        }
    }

}
