/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.storagemanager.deletionhelper;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.text.format.Formatter;

import android.view.View;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.storagemanager.R;

/**
 * Preference to handle the deletion of photos and videos in the Deletion Helper.
 */
public class PhotosDeletionPreference extends DeletionPreference {
    public static final int DAYS_TO_KEEP = 30;
    private boolean mLoaded;

    public PhotosDeletionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        updatePreferenceText(0, 0);
        setTitle(R.string.deletion_helper_photos_loading_title);
        setSummary(R.string.deletion_helper_photos_loading_summary);
    }

    /**
     * Updates the title and summary of the preference with fresh information.
     */
    public void updatePreferenceText(int items, long bytes) {
        Context context = getContext();
        setTitle(context.getString(R.string.deletion_helper_photos_title, items));
        setSummary(context.getString(R.string.deletion_helper_photos_summary,
                Formatter.formatFileSize(context, bytes), DAYS_TO_KEEP));
    }

    @Override
    public void onFreeableChanged(int items, long bytes) {
        mLoaded = true;
        // Because these operations may cause UI churn, we need to ensure they run on the main
        // thread.
        new Handler(getContext().getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                PhotosDeletionPreference.super.onFreeableChanged(items, bytes);
                updatePreferenceText(items, bytes);
            }
        });
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean checked = (boolean) newValue;
        MetricsLogger.action(getContext(), MetricsEvent.ACTION_DELETION_SELECTION_PHOTOS, checked);
        return super.onPreferenceChange(preference, newValue);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.findViewById(R.id.progress_bar).setVisibility(mLoaded ? View.GONE : View.VISIBLE);

        holder.findViewById(com.android.internal.R.id.icon)
                .setVisibility(mLoaded ? View.VISIBLE : View.GONE);

        holder.findViewById(com.android.internal.R.id.checkbox)
                .setVisibility(mLoaded ? View.VISIBLE : View.GONE);
    }
}
