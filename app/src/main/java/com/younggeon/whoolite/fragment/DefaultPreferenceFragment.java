package com.younggeon.whoolite.fragment;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;

import com.h6ah4i.android.compat.preference.MultiSelectListPreferenceCompat;
import com.younggeon.whoolite.R;

/**
 * Created by sadless on 2016. 2. 4..
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DefaultPreferenceFragment extends PreferenceFragment {
    public interface Callback {
        void initializePreferences(MultiSelectListPreferenceCompat showSlotNumber,
                                   ListPreference sortOrder);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_default);

        ((Callback) getActivity()).initializePreferences(
                (MultiSelectListPreferenceCompat) findPreference(getString(R.string.pref_key_show_slot_numbers)),
                (ListPreference) findPreference(getString(R.string.pref_key_frequently_input_sort_order)));
    }
}