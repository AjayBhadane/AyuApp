package com.ayu.devices.ayu_ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;

import static android.view.Gravity.CENTER;
import static android.view.Gravity.CENTER_HORIZONTAL;

public class MainActivity extends Activity implements View.OnClickListener {

    private LinearLayout layout;
    private LinearLayout.LayoutParams layoutParams;
    private int orientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout();
        setOrientation();
        setLayoutParams();
        setFullscreen();
        setSampleText();
        setContentView(layout);
    }

    private void setOrientation(){
        orientation = getResources().getConfiguration().orientation;
    }

    private void setLayoutParams(){
        layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = CENTER | CENTER_HORIZONTAL;
    }

    private void setLayout(){
        layout = new LinearLayout(this);
        layout.setGravity(CENTER);
    }

    private void setFullscreen(){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                ,WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void setSampleText(){
        Button button = new Button(this);
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        button.setText(R.string.record);
        button.startAnimation(shake);
        layout.addView(button, layoutParams);

        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, OtherVisualizer.class);
        startActivity(intent);
    }
}
