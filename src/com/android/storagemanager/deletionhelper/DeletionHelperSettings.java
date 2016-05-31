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

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import com.android.storagemanager.ButtonBarProvider;
import com.android.storagemanager.R;

import java.util.HashSet;

/**
 * Settings screen for the deletion helper, which manually removes data which is not recently used.
 */
public class DeletionHelperSettings extends PreferenceFragment implements
        DeletionType.FreeableChangedListener,
        View.OnClickListener {
    private static final String APPS_KEY = "apps_group";
    private static final String KEY_DOWNLOADS_PREFERENCE = "delete_downloads";
    private static final int DOWNLOADS_LOADER_ID = 1;

    private AppDeletionPreferenceGroup mApps;
    private AppDeletionType mAppBackend;
    private DownloadsDeletionPreferenceGroup mDownloadsPreference;
    private DownloadsDeletionType mDownloadsDeletion;
    private Button mCancel, mFree;

    public static DeletionHelperSettings newInstance() {
        return new DeletionHelperSettings();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.deletion_helper_list);
        mApps = (AppDeletionPreferenceGroup) findPreference(APPS_KEY);

        HashSet<String> checkedApplications = null;
        if (savedInstanceState != null) {
            checkedApplications =
                    (HashSet<String>) savedInstanceState.getSerializable(
                            AppDeletionType.EXTRA_CHECKED_SET);
        }
        mAppBackend = new AppDeletionType(getActivity().getApplication(), checkedApplications);
        mAppBackend.registerView(mApps);
        mAppBackend.registerFreeableChangedListener(this);
        mApps.setDeletionType(mAppBackend);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initializeButtons();

        Activity activity = getActivity();
        if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(
                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                    0);
        }
        mDownloadsPreference =
                (DownloadsDeletionPreferenceGroup) findPreference(KEY_DOWNLOADS_PREFERENCE);
        mDownloadsDeletion = new DownloadsDeletionType(getActivity());
        mDownloadsPreference.registerFreeableChangedListener(this);
        mDownloadsPreference.registerDeletionService(mDownloadsDeletion);
        updateFreeButtonText();
    }

    @Override
    public void onResume() {
        super.onResume();
        mAppBackend.onResume();
        mDownloadsDeletion.onResume();

        if (getActivity().checkSelfPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            getLoaderManager().initLoader(DOWNLOADS_LOADER_ID, new Bundle(), mDownloadsDeletion);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        mAppBackend.onPause();
        mDownloadsDeletion.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAppBackend.onSaveInstanceStateBundle(outState);
    }

    @Override
    public void onFreeableChanged(int numItems, long bytesFreeable) {
        // Get the total bytes freeable and then do the thing!
        updateFreeButtonText();
    }

    /**
     * Clears out the selected apps and data from the device and closes the fragment.
     */
    protected void clearData() {
        mDownloadsDeletion.clearFreeableData(getActivity());
        mAppBackend.clearFreeableData(getActivity());
        getActivity().onBackPressed();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == mFree.getId()) {
            ConfirmDeletionDialog dialog =
                    ConfirmDeletionDialog.newInstance(getTotalFreeableSpace());
            // The 0 is a placeholder for an optional result code.
            dialog.setTargetFragment(this, 0);
            dialog.show(getFragmentManager(), ConfirmDeletionDialog.TAG);
        } else {
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mDownloadsDeletion.onResume();
                getLoaderManager().initLoader(DOWNLOADS_LOADER_ID, new Bundle(),
                        mDownloadsDeletion);
            }
        }
    }

    private void initializeButtons() {
        ButtonBarProvider activity = (ButtonBarProvider) getActivity();
        activity.getButtonBar().setVisibility(View.VISIBLE);

        mCancel = activity.getSkipButton();
        mCancel.setText(R.string.cancel);
        mCancel.setOnClickListener(this);
        mCancel.setVisibility(View.VISIBLE);

        mFree = activity.getNextButton();
        mFree.setText(R.string.storage_menu_free);
        mFree.setOnClickListener(this);
    }

    private void updateFreeButtonText() {
        mFree.setText(String.format(getActivity().getString(R.string.deletion_helper_free_button),
                Formatter.formatFileSize(getActivity(), getTotalFreeableSpace())));
    }

    private long getTotalFreeableSpace() {
        long freeableSpace = 0;
        freeableSpace += mAppBackend.getTotalAppsFreeableSpace(false);
        freeableSpace += mDownloadsDeletion.getFreeableBytes();
        return freeableSpace;
    }
}