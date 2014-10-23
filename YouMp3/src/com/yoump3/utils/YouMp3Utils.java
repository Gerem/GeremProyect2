package com.yoump3.utils;

import com.caribelabs.utils.Validations;
import com.ricenbeans.yoump3.R;

import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


public class YouMp3Utils {
	public static void showCustomToast(Activity context, String message, Integer color){
		LayoutInflater inflater = context.getLayoutInflater();

        View layout = inflater.inflate(R.layout.custom_toast,(ViewGroup) context.findViewById(R.id.custom_toast_layout_id));
        if(Validations.validateIsNotNull(color)){
        	layout.setBackgroundColor(color);
        }
        // set a message
        TextView text = (TextView) layout.findViewById(R.id.custom_toast_text);
        text.setText(message);

        // Toast...
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
	}
}
