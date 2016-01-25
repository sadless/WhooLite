package com.younggeon.whoolite.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A placeholder fragment containing a simple view.
 */
public class WelcomeActivityFragment extends Fragment {
    public static final String ARGUMENT_LAYOUT_ID = "layout_id";

    public WelcomeActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        int layoutId = getArguments().getInt(ARGUMENT_LAYOUT_ID);

        return inflater.inflate(layoutId, container, false);
    }
}
