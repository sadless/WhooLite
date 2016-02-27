package com.younggeon.whoolite;

import android.content.Context;
import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.younggeon.whoolite.constant.WhooingKeyValues;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sadless on 2015. 9. 28..
 */
public class WhooLiteNetwork {
    public static RequestQueue requestQueue;

    public static void setContextForNetworking(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }

    public static class WhooingRequest extends StringRequest {
        private String mApiKeyFormat;
        private Bundle mParams;

        public WhooingRequest(int method,
                              String url,
                              Response.Listener<String> listener,
                              Response.ErrorListener errorListener,
                              String apiKeyFormat) {
            super(method, url, listener, errorListener);

            this.mApiKeyFormat = apiKeyFormat;
            setRetryPolicy(new DefaultRetryPolicy(0, 0, 0));
        }

        public WhooingRequest(int method,
                              String url,
                              Response.Listener<String> listener,
                              Response.ErrorListener errorListener,
                              String apiKeyFormat,
                              Bundle params) {
            super(method, url, listener, errorListener);

            this.mApiKeyFormat = apiKeyFormat;
            this.mParams = params;
            setRetryPolicy(new DefaultRetryPolicy(0, 0, 0));
        }

        @Override
        protected Map<String, String> getParams() throws AuthFailureError {
            if (mParams == null) {
                return super.getParams();
            } else {
                HashMap<String, String> params = new HashMap<>();

                for (String key : mParams.keySet()) {
                    params.put(key, mParams.getString(key));
                }

                return params;
            }
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap<String, String> headerMap = new HashMap<>();

            headerMap.put(WhooingKeyValues.API_KEY, String.format(mApiKeyFormat, new Date().getTime()));

            return headerMap;
        }
    }
}
