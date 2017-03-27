/*
 * Copyright (C) 2017 sonvp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sonvp.tooltip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.annotation.UiThread;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import static com.sonvp.tooltip.R.styleable.Tooltip_android_lines;
import static com.sonvp.tooltip.R.styleable.Tooltip_android_text;
import static com.sonvp.tooltip.R.styleable.Tooltip_android_textSize;
import static com.sonvp.tooltip.R.styleable.Tooltip_tooltipMargin;

public class Tooltip implements ViewTreeObserver.OnPreDrawListener, View.OnClickListener,
        View.OnTouchListener, PopupWindow.OnDismissListener {

    private static final int SIZE_TOUCH = 5;

    private final PopupWindow popupWindow;
    private final Rect rectAnchorView;
    private View overlay = null;
    private ViewGroup rootView = null;

    @Override
    public boolean onTouch(View view, MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            final int x = (int) event.getX();
            final int y = (int) event.getY();

            Rect rect = new Rect(x, y, x + SIZE_TOUCH, y + SIZE_TOUCH);
            if (!rectAnchorView.contains(rect) || !rectAnchorView.intersect(rect)) {
                if (listener != null) {
                    listener.onToolTipClicked(this);
                }
                remove();
            }
        }


        return false;
    }


    @Override
    public void onDismiss() {
        if (rootView != null && overlay != null) {
            rootView.removeView(overlay);
        }
    }

    public interface OnToolTipClickedListener {
        void onToolTipClicked(Tooltip tooltip);
    }

    private static final int GRAVITY_START = 0x00800003;
    private static final int GRAVITY_END = 0x00800005;

    private static final long ANIMATION_DURATION = 00L;

    private final View anchorView;
    private int gravity;

    private final LinearLayout container;
    private final TextView text;
    private final ImageView arrow;

    private Builder builder;

    private float pivotX;
    private float pivotY;

    @Nullable
    private OnToolTipClickedListener listener;

    private Tooltip(View anchorView, Builder builder) {
        this.builder = builder;
        this.anchorView = anchorView;
        this.gravity = builder.tooltipGravity;

        if (builder.dismissOutsideTouch) {

            rootView = (ViewGroup) anchorView.getRootView();
            overlay = new View(builder.context);
            overlay.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
//        overlay.setBackgroundColor(builder.context.getResources().getColor(android.R.color.holo_green_light));
            overlay.setOnTouchListener(this);
            rootView.addView(overlay);
        }

        // TODO container should NOT capture all events
        container = new LinearLayout(builder.context);
        container.setOnClickListener(this);

        text = new TextView(builder.context);
        text.setPadding((int) builder.leftPadding, (int) builder.topPadding,
                (int) builder.rightPadding, (int) builder.bottomPadding);
        text.setGravity(builder.textGravity);
        text.setTextColor(builder.textColor);
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, builder.textSize);
        text.setTypeface(builder.typeface, builder.typefaceStyle);
        int lines = builder.lines;
        if (lines > 0) {
            text.setLines(lines);
            text.setEllipsize(TextUtils.TruncateAt.END);
        }

        CharSequence txt = builder.text;
        if (TextUtils.isEmpty(txt)) {
            txt = builder.context.getString(builder.textResourceId);
        }
        text.setText(txt);

        int backgroundColor = builder.backgroundColor;
        float radius = builder.radius;
        if (radius > 0.0F) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(backgroundColor);
            drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            drawable.setCornerRadius(radius);

            //noinspection deprecation
            text.setBackgroundDrawable(drawable);
        } else {
            text.setBackgroundColor(backgroundColor);
        }


        rectAnchorView = getRectView(anchorView);
        changeGravityToolTip();
        if (builder.arrowDrawable == null) {
            builder.arrowDrawable = new ArrowDrawable(backgroundColor, gravity);
        }

        arrow = new ImageView(builder.context);
        // TODO supports Gravity.NO_GRAVITY
        switch (gravity) {
            case Gravity.LEFT:
                container.setOrientation(LinearLayout.HORIZONTAL);
                container.addView(text, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                arrow.setImageDrawable(builder.arrowDrawable);
                container.addView(arrow, new LinearLayout.LayoutParams((int) builder.arrowWidth, (int) builder.arrowHeight));
                break;
            case Gravity.RIGHT:
                container.setOrientation(LinearLayout.HORIZONTAL);
                arrow.setImageDrawable(builder.arrowDrawable);
                container.addView(arrow, new LinearLayout.LayoutParams((int) builder.arrowWidth, (int) builder.arrowHeight));
                container.addView(text, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                break;
            case Gravity.TOP:
                container.setOrientation(LinearLayout.VERTICAL);
                container.addView(text, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                arrow.setImageDrawable(builder.arrowDrawable);
                container.addView(arrow, new LinearLayout.LayoutParams((int) builder.arrowWidth, (int) builder.arrowHeight));
                break;
            case Gravity.BOTTOM:
                container.setOrientation(LinearLayout.VERTICAL);
                arrow.setImageDrawable(builder.arrowDrawable);
                container.addView(arrow, new LinearLayout.LayoutParams((int) builder.arrowWidth, (int) builder.arrowHeight));
                container.addView(text, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                break;
        }

        popupWindow = new PopupWindow(container,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setOnDismissListener(this);
        popupWindow.setClippingEnabled(false);
//        popupWindow.setBackgroundDrawable(builder.context.getResources().getDrawable(android.R.color.holo_blue_bright));

    }

    private Rect getRectView(View view) {
        Rect rect = new Rect();
        int[] screenLoc = new int[2];
        view.getLocationOnScreen(screenLoc);
        rect.left += screenLoc[0];
        rect.right += screenLoc[0] + view.getWidth();
        rect.top += screenLoc[1];
        rect.bottom += screenLoc[1] + view.getHeight();

        return rect;
    }

    /**
     * Sets a listener that will be called when the tool tip view is clicked.
     */
    public void setOnToolTipClickedListener(OnToolTipClickedListener listener) {
        this.listener = listener;
    }

    /**
     * Shows the tool tip.
     */
    @UiThread
    public void show() {
        container.getViewTreeObserver().addOnPreDrawListener(this);
        popupWindow.showAsDropDown(container);
    }

    /**
     * <p>Indicate whether this Tooltip is showing on screen.</p>
     *
     * @return true if the Tooltip is showing, false otherwise
     */

    public boolean isShowing() {
        return popupWindow.isShowing();
    }

    /**
     * Shows the tool tip with the specified delay.
     */
    public void showDelayed(long milliSeconds) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                show();
            }
        }, milliSeconds);
    }

    /**
     * Removes the tool tip view from the view hierarchy.
     */
    @UiThread
    public void remove() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            container.setPivotX(pivotX);
            container.setPivotY(pivotY);
            container.animate().setDuration(ANIMATION_DURATION).alpha(0.0F).scaleX(0.0F).scaleY(0.0F)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            popupWindow.dismiss();

                        }
                    });
        } else {
            AnimationSet animationSet = new AnimationSet(true);
            animationSet.setDuration(ANIMATION_DURATION);
            animationSet.addAnimation(new AlphaAnimation(1.0F, 0.0F));
            animationSet.addAnimation(new ScaleAnimation(1.0F, 0.0F, 1.0F, 0.0F, pivotX, pivotY));
            animationSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    // do nothing
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    popupWindow.dismiss();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    // do nothing
                }
            });
            container.startAnimation(animationSet);
        }
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = builder.context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = builder.context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    public boolean onPreDraw() {
        container.getViewTreeObserver().removeOnPreDrawListener(this);

        Context context = container.getContext();
        if (!(context instanceof Activity)) {
            return false;
        }
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int displayWidth = displayMetrics.widthPixels;
        int displayHeight = displayMetrics.heightPixels;
        int displayTop = getStatusBarHeight();

        int anchorTop = rectAnchorView.top;
        int anchorLeft = rectAnchorView.left;
        int anchorWidth = anchorView.getWidth();
        int anchorHeight = anchorView.getHeight();

        int textWidth = text.getWidth();
        //default height 1 line
        int textHeight = text.getHeight();
        int arrowWidth = arrow.getWidth();
        int arrowHeight = arrow.getHeight();


        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(displayWidth, View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

        if (gravity == Gravity.TOP || gravity == Gravity.BOTTOM) {
            int width = Math.max(textWidth, arrowWidth);
            int height = textHeight + arrowHeight;

            int leftPadding;
            int topPadding;

            if (gravity == Gravity.TOP) {
                topPadding = anchorTop - height;
            } else {
                // gravity == Gravity.BOTTOM
                topPadding = anchorTop + anchorHeight;
            }

            int anchorHorizontalCenter = anchorLeft + anchorWidth / 2;
            int left = anchorHorizontalCenter - width / 2;
            int right = left + width;
            leftPadding = Math.max(0, right > displayWidth ? displayWidth - width : left);


            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) arrow.getLayoutParams();
            layoutParams.leftMargin = anchorHorizontalCenter - leftPadding - arrowWidth / 2;
            arrow.setLayoutParams(layoutParams);
            popupWindow.update(leftPadding, topPadding, container.getWidth(), container.getHeight());

            pivotX = width / 2;
            pivotY = gravity == Gravity.TOP ? height : 0;
        } else {
            // gravity == Gravity.LEFT || gravity == Gravity.RIGHT

            int width = textWidth + arrowWidth;

            int leftPadding;
            int topPadding;
            int rightPadding = 0;

            if (gravity == Gravity.LEFT) {
                leftPadding = Math.max(0, anchorLeft - width);
                leftPadding += (int) builder.toolTipMargin;
                rightPadding = displayWidth - anchorLeft;
            } else {
                // gravity == Gravity.RIGHT
                leftPadding = anchorLeft + anchorWidth;
                rightPadding = (int) builder.toolTipMargin;
            }

            text.setMaxWidth(displayWidth - rightPadding - leftPadding - arrowWidth);

            text.measure(widthMeasureSpec, heightMeasureSpec);
            textHeight = text.getMeasuredHeight(); // height multi line

            int height = Math.max(textHeight, arrowHeight);


            int anchorVerticalCenter = anchorTop + anchorHeight / 2;
            int top = anchorVerticalCenter - height / 2;
            int bottom = top + height;

            if (builder.arrowGravity == Gravity.TOP) {
                top = anchorTop;
                bottom = anchorTop + height;
            } else if (builder.arrowGravity == Gravity.BOTTOM) {
                top = anchorTop + anchorHeight - height;
            }


            topPadding = Math.max(0, bottom > displayHeight ? displayHeight - height - (int) builder.toolTipMargin : top);
            topPadding = Math.max(0, topPadding < displayTop ? displayTop + (int) builder.toolTipMargin : topPadding);

            container.measure(widthMeasureSpec, heightMeasureSpec);
            int popupWidth = container.getMeasuredWidth();
            int popupHeight = container.getMeasuredHeight();
            popupWindow.update(leftPadding, topPadding, popupWidth, popupHeight);

            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) arrow.getLayoutParams();
            layoutParams.topMargin = anchorVerticalCenter - topPadding - arrowHeight / 2;
            arrow.setLayoutParams(layoutParams);

            pivotX = gravity == Gravity.LEFT ? popupWidth : 0;
            pivotY = anchorVerticalCenter - topPadding;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            container.setAlpha(0.0F);
            container.setPivotX(pivotX);
            container.setPivotY(pivotY);
            container.setScaleX(0.0F);
            container.setScaleY(0.0F);
            container.animate().setDuration(ANIMATION_DURATION).scaleX(1.0F).scaleY(1.0F).alpha(1.0F);
        } else {
            AnimationSet animationSet = new AnimationSet(true);
            animationSet.setDuration(ANIMATION_DURATION);
            animationSet.addAnimation(new AlphaAnimation(0.0F, 1.0F));
            animationSet.addAnimation(new ScaleAnimation(0.0F, 1.0F, 0.0F, 1.0F, pivotX, pivotY));
            container.startAnimation(animationSet);
        }

        return false;
    }

    /**
     * change gravity tooltip if anchorView intersect display screen
     */
    private void changeGravityToolTip() {

        Context context = container.getContext();
        if (!(context instanceof Activity)) {
            return;
        }
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int displayHeight = displayMetrics.heightPixels;
        int displayWidth = displayMetrics.widthPixels;
        int displayTop = getStatusBarHeight();

        int anchorTop = rectAnchorView.top;
        int anchorHeight = anchorView.getHeight();

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(displayWidth, View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        text.measure(widthMeasureSpec, heightMeasureSpec);

        int textHeight = text.getMeasuredHeight(); // height multi line
        int heightToolTip = textHeight +
                (int) builder.arrowHeight +
                (int) builder.topPadding +
                (int) builder.bottomPadding +
                (int) builder.toolTipMargin;

        switch (gravity) {
            case Gravity.LEFT:
            case Gravity.RIGHT:

                int anchorVerticalCenter = anchorTop + anchorHeight / 2;
                int bottomArrow = anchorVerticalCenter + (int) builder.arrowHeight / 2;
                int topArrow = anchorVerticalCenter - (int) builder.arrowHeight / 2;
                if (bottomArrow +  builder.radius +  builder.toolTipMargin
                        > displayHeight) {
                    gravity = Gravity.TOP;
                } else if (topArrow < getStatusBarHeight() +  builder.radius
                        + builder.toolTipMargin) {
                    gravity = Gravity.BOTTOM;
                }
                break;
            case Gravity.TOP:
                if (anchorTop - displayTop < heightToolTip) {
                    gravity = Gravity.BOTTOM;
                }
                break;

            case Gravity.BOTTOM:
                int anchorBottom = displayHeight - (anchorTop + anchorHeight);
                if (anchorBottom < heightToolTip) {
                    gravity = Gravity.TOP;
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        if (listener != null) {
            listener.onToolTipClicked(this);
        }

        remove();
    }

    /**
     * Used to build a tool tip.
     */
    public static class Builder {
        private static final float DEFAULT_PADDING_TEXT = 15.0F;
        private static final float DEFAULT_ARROW_SIZE = 30.0F;

        private final Context context;

        private View anchorView;
        private int tooltipGravity = Gravity.BOTTOM;
        private int arrowGravity = Gravity.CENTER;
        @StringRes
        private int textResourceId = 0;
        private CharSequence text;
        private int textGravity = Gravity.NO_GRAVITY;
        private int textColor = Color.WHITE;
        private float textSize = 13.0F;
        private Typeface typeface = Typeface.DEFAULT;
        private int typefaceStyle = Typeface.NORMAL;
        private int lines = 0;
        private int backgroundColor = Color.BLUE;
        private float leftPadding = DEFAULT_PADDING_TEXT;
        private float rightPadding = DEFAULT_PADDING_TEXT;
        private float topPadding = DEFAULT_PADDING_TEXT;
        private float bottomPadding = DEFAULT_PADDING_TEXT;
        private float radius = 0.0F;
        private float arrowHeight = DEFAULT_ARROW_SIZE;
        private float arrowWidth = DEFAULT_ARROW_SIZE;
        private float toolTipMargin = 0.0F;
        private boolean dismissOutsideTouch = true;

        private Drawable arrowDrawable;

        /**
         * Creates a new builder.
         */
        public Builder(Context context) {
            this.context = context;

        }

        public Builder(Context context, @NonNull View anchorView, @StyleRes int resId) {
            this.context = context;
            this.anchorView = anchorView;
            init(context, anchorView, resId);
        }


        private void init(@NonNull Context context, @NonNull View anchorView, @StyleRes int resId) {
            TypedArray a = context.obtainStyledAttributes(resId, R.styleable.Tooltip);

            tooltipGravity = a.getInt(R.styleable.Tooltip_tooltipGravity, Gravity.BOTTOM);
            toolTipMargin = a.getDimensionPixelSize(Tooltip_tooltipMargin, 0);
            arrowGravity = a.getInt(R.styleable.Tooltip_arrowGravity, Gravity.CENTER);
            text = a.getString(Tooltip_android_text);
            textSize = a.getDimensionPixelSize(Tooltip_android_textSize, -1);
            textColor = a.getColor(R.styleable.Tooltip_android_textColor, Color.WHITE);
            lines = a.getInt(Tooltip_android_lines, 0);
            backgroundColor = a.getColor(R.styleable.Tooltip_backgroundColor, Color.BLACK);
            leftPadding = a.getDimension(R.styleable.Tooltip_leftPadding, DEFAULT_PADDING_TEXT);
            rightPadding = a.getDimension(R.styleable.Tooltip_rightPadding, DEFAULT_PADDING_TEXT);
            topPadding = a.getDimension(R.styleable.Tooltip_topPadding, DEFAULT_PADDING_TEXT);
            bottomPadding = a.getDimension(R.styleable.Tooltip_bottomPadding, DEFAULT_PADDING_TEXT);
            radius = a.getDimension(R.styleable.Tooltip_radius, 0.0F);
            arrowHeight = a.getDimension(R.styleable.Tooltip_arrowHeight, DEFAULT_ARROW_SIZE);
            arrowWidth = a.getDimension(R.styleable.Tooltip_arrowWidth, DEFAULT_ARROW_SIZE);
            arrowDrawable = a.getDrawable(R.styleable.Tooltip_arrowDrawable);
            textResourceId = a.getResourceId(R.styleable.Tooltip_textResourceId, -1);
            dismissOutsideTouch = a.getBoolean(R.styleable.Tooltip_dismissOutsideTouch, true);

            typefaceStyle = a.getInteger(R.styleable.Tooltip_android_textStyle, -1);
            final String fontFamily = a.getString(R.styleable.Tooltip_android_fontFamily);
            final int typefaceIndex = a.getInt(R.styleable.Tooltip_android_typeface, -1);
            typeface = getTypefaceFromAttr(fontFamily, typefaceIndex, typefaceStyle);

            a.recycle();
        }


        private Typeface getTypefaceFromAttr(String familyName, int typefaceIndex, int styleIndex) {
            Typeface tf = null;
            if (familyName != null) {
                tf = Typeface.create(familyName, styleIndex);
                if (tf != null) {
                    return tf;
                }
            }
            switch (typefaceIndex) {
                case 1: // SANS
                    tf = Typeface.SANS_SERIF;
                    break;
                case 2: // SERIF
                    tf = Typeface.SERIF;
                    break;
                case 3: // MONOSPACE
                    tf = Typeface.MONOSPACE;
                    break;
            }
            return tf;
        }

        /**
         * Sets the text of the tool tip. If both the resource ID and the char sequence are set, the
         * char sequence will be used.
         */
        public Builder withText(@StringRes int text) {
            this.textResourceId = text;
            return this;
        }

        /**
         * Sets the text of the tool tip. If both the resource ID and the char sequence are set, the
         * char sequence will be used.
         */
        public Builder withText(CharSequence text) {
            this.text = text;
            return this;
        }

        /**
         * Sets the text color for the tool tip. The default color is white.
         */
        public Builder withTextColor(@ColorInt int textColor) {
            this.textColor = textColor;
            return this;
        }

        /**
         * Sets the text size in pixel for the tool tip. The default size is 13.
         */
        public Builder withTextSize(float textSize) {
            this.textSize = textSize;
            return this;
        }

        /**
         * Sets the arrow size in pixel for the tool tip. The default size is 30.
         */
        public Builder withArrowSize(float arrowHeight, float arrowWidth) {
            this.arrowHeight = arrowHeight;
            this.arrowWidth = arrowWidth;
            return this;
        }

        /**
         * Sets the margin tooltip in pixel for the tool tip. The default size is 0.
         */
        public Builder withToolTipMargin(float toolTipMargin) {
            this.toolTipMargin = toolTipMargin;
            return this;
        }


        /**
         * Sets the arrow drawable for the tool tip. The default size is 13.
         */
        public Builder withArrowDrawable(Drawable arrowDrawable) {
            this.arrowDrawable = arrowDrawable;
            return this;
        }

        /**
         * Sets the typeface for the tool tip. The default value is {@link Typeface.DEFAULT}.
         */
        public Builder withTypeface(Typeface typeface) {
            if (typeface != null) {
                this.typeface = typeface;
            }
            return this;
        }

        /**
         * Sets the typeface style for the tool tip. The default value is {@link Typeface.NORMAL}.
         */
        public Builder withTypefaceStyle(int style) {
            this.typefaceStyle = style;
            return this;
        }

        /**
         * Sets the exact lines for the tool tip. The default value is unset.
         */
        public Builder withLines(int lines) {
            this.lines = lines;
            return this;
        }

        /**
         * Sets the background color for the tool tip. The default color is black.
         */
        public Builder withBackgroundColor(@ColorInt int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        /**
         * Sets the padding in pixel for the tool tip. The default padding is 0.
         */
        public Builder withPadding(int leftPadding, int rightPadding, int topPadding, int bottomPadding) {
            this.leftPadding = leftPadding;
            this.rightPadding = rightPadding;
            this.topPadding = topPadding;
            this.bottomPadding = bottomPadding;
            return this;
        }

        /**
         * Sets the corner radius in pixel for the tool tip. The default value is 0.
         */
        public Builder withCornerRadius(float radius) {
            this.radius = radius;
            return this;
        }

        /**
         * Sets the tooltip dismiss when touch outside. The default value is true.
         */
        public Builder withDismissOutsideTouch(boolean dismissOutsideTouch) {
            this.dismissOutsideTouch = dismissOutsideTouch;
            return this;
        }

        /**
         * Sets the tool tip gravity. By default, it will be anchored to bottom of the anchor view.
         * <p/>
         * Only the following are supported: Gravity.TOP, Gravity.BOTTOM, Gravity.LEFT, Gravity.RIGHT,
         * Gravity.START, and Gravity.END.
         */
        public Builder withTooltipGravity(int gravity) {
            this.tooltipGravity = gravity;
            return this;
        }


        /**
         * Sets the arrow gravity. By default, it will be anchored to top of the tooltip view.
         * <p/>
         * Only the following are supported: Gravity.TOP, Gravity.CENTER.
         */
        public Builder withArrowGravity(int gravity) {
            this.arrowGravity = gravity;
            return this;
        }

        /**
         * Sets the view that the tool tip view will try to anchor.
         */
        public Builder withAnchor(View anchorView) {
            this.anchorView = anchorView;
            return this;
        }

        /**
         * Creates a tool tip.
         */
        public Tooltip build() {
            if (tooltipGravity == GRAVITY_START || tooltipGravity == GRAVITY_END) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                        && anchorView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                    tooltipGravity = tooltipGravity == GRAVITY_START ? Gravity.RIGHT : Gravity.LEFT;
                } else {
                    tooltipGravity &= Gravity.HORIZONTAL_GRAVITY_MASK;
                }
            }
            if (tooltipGravity != Gravity.TOP && tooltipGravity != Gravity.BOTTOM
                    && tooltipGravity != Gravity.LEFT && tooltipGravity != Gravity.RIGHT) {
                throw new IllegalArgumentException("Unsupported tooltip gravity - " + tooltipGravity);
            }

            if (arrowGravity != Gravity.TOP && arrowGravity != Gravity.CENTER
                    && arrowGravity != Gravity.BOTTOM) {
                throw new IllegalArgumentException("Unsupported arrow gravity - " + arrowGravity);
            }

            return new Tooltip(anchorView, this);
        }

        /**
         * Builds a {@link Tooltip} with builder attributes and {@link Tooltip#show()}'s the tooltip.
         */
        public Tooltip show() {
            Tooltip tooltip = build();
            tooltip.show();
            return tooltip;
        }
    }
}
