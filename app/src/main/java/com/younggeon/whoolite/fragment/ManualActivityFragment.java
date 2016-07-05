package com.younggeon.whoolite.fragment;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.younggeon.whoolite.R;
import com.younggeon.whoolite.databinding.FragmentManualBinding;

/**
 * A placeholder fragment containing a simple view.
 */
public class ManualActivityFragment extends Fragment {
    public static final String ARG_DRAWABLE_ID = "drawable_id";
    public static final String ARG_TEXT_ID = "text_id";

    public ManualActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentManualBinding binding = DataBindingUtil.bind(inflater.inflate(R.layout.fragment_manual, container, false));
        Bundle arguments = getArguments();

        binding.imageView.setImageResource(arguments.getInt(ARG_DRAWABLE_ID));
        binding.textView.setText(arguments.getInt(ARG_TEXT_ID));

        return binding.getRoot();
    }
}
