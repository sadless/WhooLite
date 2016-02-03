package com.younggeon.whoolite.activity;


import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.h6ah4i.android.compat.content.SharedPreferenceCompat;
import com.h6ah4i.android.compat.preference.MultiSelectListPreferenceCompat;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.fragment.DefaultPreferenceFragment;

import java.util.Arrays;
import java.util.Set;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity implements DefaultPreferenceFragment.Callback {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            findViewById(android.R.id.list).setVisibility(View.GONE);
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new DefaultPreferenceFragment()).commit();
        } else {
            addPreferencesFromResource(R.xml.pref_default);
            initializePreferences((MultiSelectListPreferenceCompat) findPreference(getString(R.string.pref_key_show_slot_numbers)),
                    (ListPreference) findPreference(getString(R.string.pref_key_frequently_input_sort_order)));
        }
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setupActionBar();
    }

    @Override
    public void initializePreferences(MultiSelectListPreferenceCompat showSlotNumber, ListPreference sortOrder) {
        showSlotNumber.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @SuppressWarnings("unchecked")
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Set<String> values = (Set<String>) newValue;

                if (values.size() == 0) {
                    for (int i = 0; i < 3; i++) {
                        values.add("" + (i + 1));
                    }
                    SharedPreferenceCompat.EditorCompat.putStringSet(preference.getEditor(),
                            preference.getKey(),
                            values).apply();
                    newValue = values;
                }
                refreshShowSlotNumberSummary((MultiSelectListPreferenceCompat) preference,
                        (Set<String>) newValue);

                return true;
            }
        });
        refreshShowSlotNumberSummary(showSlotNumber, null);
        sortOrder.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshSortOrder((ListPreference) preference,
                        (String) newValue);

                return true;
            }
        });
        refreshSortOrder(sortOrder, null);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void refreshShowSlotNumberSummary(MultiSelectListPreferenceCompat showSlotNumber, Set<String> values) {
        String summary = null;

        if (values == null) {
            values = showSlotNumber.getValues();
        }
        if (values.size() == 3) {
            summary = getString(R.string.all);
        } else {
            String[] slotNumbers = new String[values.size()];
            boolean first = true;

            values.toArray(slotNumbers);
            Arrays.sort(slotNumbers);
            for (String slotNumber : slotNumbers) {
                if (first) {
                    summary = getString(R.string.numbering, Integer.parseInt(slotNumber));
                    first = false;
                } else {
                    summary += ", " + getString(R.string.numbering, Integer.parseInt(slotNumber));
                }
            }
        }
        summary += " " + getString(R.string.slot);
        showSlotNumber.setSummary(summary);
    }

    private void refreshSortOrder(ListPreference sortOrder, String value) {
        if (value == null) {
            value = sortOrder.getValue();
        }

        String[] titles = getResources().getStringArray(R.array.pref_frequently_input_sort_order_titles);
        int index = sortOrder.findIndexOfValue(value);

        sortOrder.setSummary(titles[index]);
        sortOrder.setValueIndex(index);
    }
}
