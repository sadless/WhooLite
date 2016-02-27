package com.younggeon.whoolite.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.younggeon.whoolite.R;

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
        View view = inflater.inflate(R.layout.fragment_manual, container, false);
        ImageView image = (ImageView) view.findViewById(R.id.image);
        TextView text = (TextView) view.findViewById(R.id.text);
        Bundle arguments = getArguments();

        image.setImageResource(arguments.getInt(ARG_DRAWABLE_ID));
        text.setText(arguments.getInt(ARG_TEXT_ID));

        return view;
    }
}
