package com.ayu.devices.ayu_ui;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class Plotter extends GLSurfaceView{
    PlotRenderer plotRenderer;
    public Plotter(Context context) {
        super(context);
        plotRenderer = new PlotRenderer();
        setRenderer(plotRenderer);
    }
}
