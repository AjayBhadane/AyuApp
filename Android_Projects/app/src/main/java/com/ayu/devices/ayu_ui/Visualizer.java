package com.ayu.devices.ayu_ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.ayu.devices.ayu_ui.shader.util.ShaderUtil;

import java.io.FileNotFoundException;
import java.io.IOException;

import static android.view.Gravity.*;

public class Visualizer extends Activity {
    private FrameLayout frameLayout;
    private Plotter plotter;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            init();
        }catch (IOException io){
            Toast.makeText(this, "Resources required not found!", Toast.LENGTH_LONG);
        }
        setFullscreen();
        setFrameLayout();
        setContentView(frameLayout);
    }

    @Override
    protected void onPause(){
        super.onPause();
        plotter.onPause();
    }

    private void setFullscreen(){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                ,WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    void setFrameLayout(){
        FrameLayout.LayoutParams frameLayoutParams;

        frameLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        frameLayoutParams.gravity = CENTER | RIGHT;

        frameLayout.addView(plotter);
        frameLayout.addView(button, frameLayoutParams);
        frameLayout.setForegroundGravity(CENTER);
    }

    void init() throws IOException {
        frameLayout = new FrameLayout(this);
        plotter = new Plotter(this);
        button = new Button(this );
        String shader = ShaderUtil.readFile(getResources(), R.raw.vert);
        button.setText(shader);
    }
}
