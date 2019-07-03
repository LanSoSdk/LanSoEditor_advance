package com.example.advanceDemo.utils;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.lansoeditor.advanceDemo.R;

public class ButtonEnable extends android.support.v7.widget.AppCompatButton implements View.OnClickListener {

    private boolean isEnable=false;
    public ButtonEnable(Context context) {
        super(context, null);
        setOnClickListener(this);
    }

    public ButtonEnable(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        setOnClickListener(this);
    }

    public ButtonEnable(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnClickListener(this);
    }



    @Override
    public void onClick(View v) {
        isEnable=!isEnable;
        if(isEnable){
            setBackgroundResource(R.drawable.switch_on);
        } else {
            setBackgroundResource(R.drawable.switch_off);
        }
    }
    public boolean isSelecteButton(){
        return isEnable;
    }
}
