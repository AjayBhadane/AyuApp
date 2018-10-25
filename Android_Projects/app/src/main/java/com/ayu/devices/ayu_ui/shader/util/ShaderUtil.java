package com.ayu.devices.ayu_ui.shader.util;

import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShaderUtil {
    public static String readFile(Resources resources, int resourceId) throws FileNotFoundException, IOException{
        InputStream ins = resources.openRawResource(resourceId);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(ins));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null){
            content.append(line);
        }
        ins.close();
        bufferedReader.close();
        return content.toString();
    }
}
