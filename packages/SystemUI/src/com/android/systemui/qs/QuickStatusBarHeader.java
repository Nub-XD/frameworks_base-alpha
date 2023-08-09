/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.qs;

import static android.app.StatusBarManager.DISABLE2_QUICK_SETTINGS;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.Space;
import android.widget.TextView;

import com.android.internal.graphics.ColorUtils;
import com.android.systemui.Dependency;
import com.android.systemui.R;

import com.android.settingslib.Utils;
import com.android.systemui.util.LargeScreenUtils;
import com.android.systemui.tuner.TunerService;

/**
 * View that contains the top-most bits of the QS panel (primarily the status bar with date, time,
 * battery, carrier info and privacy icons) and also contains the {@link QuickQSPanel}.
 */
public class QuickStatusBarHeader extends FrameLayout implements TunerService.Tunable {

    private static final String QS_HEADER_IMAGE =
            "system:" + Settings.System.QS_HEADER_IMAGE;
    private static final String QS_HEADER_IMAGE_TINT =
            "system:" + Settings.System.QS_HEADER_IMAGE_TINT;
    private static final String QS_HEADER_IMAGE_TINT_CUSTOM =
            "system:" + Settings.System.QS_HEADER_IMAGE_TINT_CUSTOM;
    private static final String QS_HEADER_IMAGE_ALPHA =
            "system:" + Settings.System.QS_HEADER_IMAGE_ALPHA;
    private static final String QS_HEADER_IMAGE_HEIGHT_PORTRAIT =
            "system:" + Settings.System.QS_HEADER_IMAGE_HEIGHT_PORTRAIT;
    private static final String QS_HEADER_IMAGE_HEIGHT_LANDSCAPE =
            "system:" + Settings.System.QS_HEADER_IMAGE_HEIGHT_LANDSCAPE;
    private static final String QS_HEADER_IMAGE_LANDSCAPE_ENABLED =
            "system:" + Settings.System.QS_HEADER_IMAGE_LANDSCAPE_ENABLED;
    private static final String QS_HEADER_IMAGE_PADDING_SIDE =
            "system:" + Settings.System.QS_HEADER_IMAGE_PADDING_SIDE;
    private static final String QS_HEADER_IMAGE_PADDING_TOP =
            "system:" + Settings.System.QS_HEADER_IMAGE_PADDING_TOP;

    private boolean mExpanded;
    private boolean mQsDisabled;
    private boolean mQsHeaderImageEnabled;
    private boolean mQsHeaderImageLandscapeEnabled;
    private int mQsHeaderImageHeightPortrait;
    private int mQsHeaderImageHeightLandscape;
    private int mQsHeaderImage;
    private int mQsHeaderImageAlpha;
    private int mQsHeaderImageTint;
    private int mQsHeaderImageTintCustom;
    private int mColorAccent;
    private int mColorTextPrimary;
    private int mColorTextPrimaryInverse;

    private int mQsHeaderImagePaddingSide;
    private int mQsHeaderImagePaddingTop;

    protected QuickQSPanel mHeaderQsPanel;

    // QS Header
    private ImageView mQsHeaderImageView;
    private View mQsHeaderLayout;
    private int mQsHeaderImageValue;

    public QuickStatusBarHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mHeaderQsPanel = findViewById(R.id.quick_qs_panel);
        mQsHeaderLayout = findViewById(R.id.layout_header);
        mQsHeaderImageView = findViewById(R.id.qs_header_image_view);
        mQsHeaderImageView.setClipToOutline(true);

        updateResources();

        Dependency.get(TunerService.class).addTunable(this,
            QS_HEADER_IMAGE,
            QS_HEADER_IMAGE_TINT,
            QS_HEADER_IMAGE_TINT_CUSTOM,
            QS_HEADER_IMAGE_ALPHA,
            QS_HEADER_IMAGE_HEIGHT_PORTRAIT,
            QS_HEADER_IMAGE_HEIGHT_LANDSCAPE,
            QS_HEADER_IMAGE_LANDSCAPE_ENABLED,
            QS_HEADER_IMAGE_PADDING_SIDE,
            QS_HEADER_IMAGE_PADDING_TOP);
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        switch (key) {
            case QS_HEADER_IMAGE:
                mQsHeaderImageValue = TunerService.parseInteger(newValue, 0);
                mQsHeaderImageEnabled = mQsHeaderImageValue > 0;
                break;
            case QS_HEADER_IMAGE_TINT:
                mQsHeaderImageTint = TunerService.parseInteger(newValue, 0);
                break;
            case QS_HEADER_IMAGE_TINT_CUSTOM:
                mQsHeaderImageTintCustom = TunerService.parseInteger(newValue, 0XFFFFFFFF);
                break;
            case QS_HEADER_IMAGE_ALPHA:
                mQsHeaderImageAlpha = TunerService.parseInteger(newValue, 255);
                break;
            case QS_HEADER_IMAGE_HEIGHT_PORTRAIT:
                mQsHeaderImageHeightPortrait = TunerService.parseInteger(newValue, 155);
                break;
            case QS_HEADER_IMAGE_HEIGHT_LANDSCAPE:
                mQsHeaderImageHeightLandscape = TunerService.parseInteger(newValue, 155);
                break;
            case QS_HEADER_IMAGE_LANDSCAPE_ENABLED:
                mQsHeaderImageLandscapeEnabled = TunerService.parseIntegerSwitch(newValue, true);
                break;
            case QS_HEADER_IMAGE_PADDING_SIDE:
                mQsHeaderImagePaddingSide = TunerService.parseInteger(newValue, 0);
                break;
            case QS_HEADER_IMAGE_PADDING_TOP:
                mQsHeaderImagePaddingTop = TunerService.parseInteger(newValue, 0);
                break;
            default:
                return;
        }
        updateResources();
    }

    private void updateQSHeaderImage() {
        Resources resources = mContext.getResources();
        Configuration config = resources.getConfiguration();

        if (!mQsHeaderImageEnabled) {
            mQsHeaderLayout.setVisibility(View.GONE);
            return;
        }

        int orientation = getResources().getConfiguration().orientation;
        mColorAccent = Utils.getColorAttrDefaultColor(mContext,
                android.R.attr.colorAccent);
        mColorTextPrimary = Utils.getColorAttrDefaultColor(mContext,
                android.R.attr.textColorPrimary);
        mColorTextPrimaryInverse = Utils.getColorAttrDefaultColor(
                mContext, android.R.attr.textColorPrimaryInverse);

        int mQsHeaderImageMinHeight = resources.getDimensionPixelSize(
                R.dimen.qs_header_image_min_height);
        int mQsHeaderImageSmall = resources.getDimensionPixelSize(
                R.dimen.qs_header_image_small);
        int mQsHeaderImageLarge = resources.getDimensionPixelSize(
                R.dimen.qs_header_image_large);

        if (!mQsHeaderImageLandscapeEnabled &&
                orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mQsHeaderImageView.setVisibility(View.GONE);
        } else {
            mQsHeaderImageView.setVisibility(View.VISIBLE);
        }

        int resId = resources.getIdentifier("qs_header_image_" +
                String.valueOf(mQsHeaderImageValue), "drawable", "com.android.systemui");
        mQsHeaderImageView.setImageResource(resId);

        /*if (mQsHeaderImageScaleType == 0) {
            mQsHeaderImageView.setScaleType(ImageView.ScaleType.FIT_XY);
        } else {
            mQsHeaderImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }*/

        mQsHeaderImageView.setAlpha(mQsHeaderImageAlpha);

        if (mQsHeaderImageTint == 0) {
            mQsHeaderImageView.setColorFilter(null);
        } else if (mQsHeaderImageTint == 1) {
            mQsHeaderImageView.setColorFilter(mColorAccent);
        } else if (mQsHeaderImageTint == 2) {
            mQsHeaderImageView.setColorFilter(mColorTextPrimary);
        } else if (mQsHeaderImageTint == 3) {
            mQsHeaderImageView.setColorFilter(mColorTextPrimaryInverse);
        } else if (mQsHeaderImageTint == 4) {
            mQsHeaderImageView.setColorFilter(mQsHeaderImageTintCustom);
        }

        ViewGroup.MarginLayoutParams mQsHeaderImageParams = (ViewGroup.MarginLayoutParams) mQsHeaderLayout.getLayoutParams();
        if (mQsHeaderImageEnabled) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (mQsHeaderImageHeightLandscape > mQsHeaderImageMinHeight) {
                    mQsHeaderImageParams.height = mQsHeaderImageHeightLandscape;
                } else {
                    mQsHeaderImageParams.height = mQsHeaderImageSmall;
                }
            } else {
                if (mQsHeaderImageHeightPortrait > mQsHeaderImageMinHeight) {
                    mQsHeaderImageParams.height = mQsHeaderImageHeightPortrait;
                } else {
                    mQsHeaderImageParams.height = mQsHeaderImageSmall;
                }
            }
        } else {
            mQsHeaderImageParams.height = mQsHeaderImageMinHeight;
        }
        mQsHeaderImageParams.setMargins(mQsHeaderImagePaddingSide, mQsHeaderImagePaddingTop, mQsHeaderImagePaddingSide, 0);
        mQsHeaderLayout.setLayoutParams(mQsHeaderImageParams);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Only react to touches inside QuickQSPanel
        if (event.getY() > mHeaderQsPanel.getTop()) {
            return super.onTouchEvent(event);
        } else {
            return false;
        }
    }

    void updateResources() {
        Resources resources = mContext.getResources();
        boolean largeScreenHeaderActive =
                LargeScreenUtils.shouldUseLargeScreenShadeHeader(resources);

        int statusBarSideMargin = mQsHeaderImageEnabled ? mContext.getResources().getDimensionPixelSize(
                R.dimen.qs_header_image_side_margin) : 0;
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (mQsDisabled) {
            lp.height = 0;
        } else {
            lp.height = WRAP_CONTENT;
        }
        setLayoutParams(lp);

        MarginLayoutParams qqsLP = (MarginLayoutParams) mHeaderQsPanel.getLayoutParams();
        if (largeScreenHeaderActive) {
            qqsLP.topMargin = resources
                    .getDimensionPixelSize(R.dimen.qqs_layout_margin_top);
        } else {
            qqsLP.topMargin = resources
                    .getDimensionPixelSize(R.dimen.large_screen_shade_header_min_height);
        }
        mHeaderQsPanel.setLayoutParams(qqsLP);
        updateQSHeaderImage();
    }

    public void setExpanded(boolean expanded, QuickQSPanelController quickQSPanelController) {
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        quickQSPanelController.setExpanded(expanded);
    }

    public void disable(int state1, int state2, boolean animate) {
        final boolean disabled = (state2 & DISABLE2_QUICK_SETTINGS) != 0;
        if (disabled == mQsDisabled) return;
        mQsDisabled = disabled;
        mHeaderQsPanel.setDisabledByPolicy(disabled);
        updateResources();
    }

    private void setContentMargins(View view, int marginStart, int marginEnd) {
        MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
        lp.setMarginStart(marginStart);
        lp.setMarginEnd(marginEnd);
        view.setLayoutParams(lp);
    }
}
