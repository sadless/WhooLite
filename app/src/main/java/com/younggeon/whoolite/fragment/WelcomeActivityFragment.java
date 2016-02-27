package com.younggeon.whoolite.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.younggeon.whoolite.R;
import com.younggeon.whoolite.activity.ManualActivity;
import com.younggeon.whoolite.activity.WhooingLoginActivity;
import com.younggeon.whoolite.constant.PreferenceKeys;

/**
 * A placeholder fragment containing a simple view.
 */
public class WelcomeActivityFragment extends Fragment {
    private static final int REQUEST_CODE_WHOOING_LOGIN = 1;

    public WelcomeActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome, container, false);
        Button loginButton = (Button) view.findViewById(R.id.login);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getActivity(), WhooingLoginActivity.class),
                        REQUEST_CODE_WHOOING_LOGIN);
            }
        });
        loginButton.getBackground().setColorFilter(0xFF1C1C1C, PorterDuff.Mode.MULTIPLY);

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_WHOOING_LOGIN: {
                if (resultCode == Activity.RESULT_OK) {
                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                            .putString(PreferenceKeys.API_KEY_FORMAT, data.getStringExtra(WhooingLoginActivity.EXTRA_API_KEY_FORMAT))
                            .commit();
                    startActivity(new Intent(getActivity(), ManualActivity.class));
                    getActivity().finish();
                }
                break;
            }
            default:
        }
    }
}
