package com.ayu.devices.ayu_ui;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import static android.view.Gravity.*;

public class OtherVisualizer extends AppCompatActivity implements View.OnClickListener {

    private final Handler handler = new Handler();
    private volatile GraphView graph;
    private volatile LineGraphSeries<DataPoint> series;
    private Runnable timer;

//    Audio recorder

    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final double SAMPLING_RATE = RECORDER_SAMPLERATE;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MAX_DATA_POINTS = 2000;
    private static final int PERMISSION_RECORD_AUDIO = 0;
    private static final double REFERENCE = 0.6;

    short[] audioData;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private volatile double currentAmp;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, PERMISSION_RECORD_AUDIO);
        }

        this.currentAmp = 0;
        setContentView(R.layout.graph);
        FrameLayout frameLayout;
        frameLayout = findViewById(R.id.frameLayout);
        frameLayout.setForegroundGravity(END);

        setButtonHandlers();

        graph = findViewById(R.id.graph);
        series = new LineGraphSeries<>();
        series.setDrawAsPath(true);

        series.appendData(new DataPoint(0,0), true, MAX_DATA_POINTS);

        series.setThickness(1);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
//        graph.getViewport().setMaxX((1/SAMPLING_RATE) * 1000);
        graph.getViewport().setMaxX(0.1);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(40);
        graph.getViewport().setMaxY(100);

        graph.getViewport().setScrollable(true);

        graph.addSeries(series);

        timer = new Runnable() {
            private double time = 0;
            @Override
            public void run() {
                time += 1/SAMPLING_RATE;
                long amp = (long)currentAmp;
                if (amp > -1){
                    series.appendData(new DataPoint(time,amp), true, MAX_DATA_POINTS);
                }

                handler.post(this);
            }
        };

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING) * 3;
        audioData = new short[bufferSize];

        startRecording();

        handler.post(timer);
    }

    private double map(double x, double in_min, double in_max, double out_min, double out_max)
    {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    private String getFileName(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()){
            file.mkdirs();
        }

        return file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV;
    }

    private void startRecording(){
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING,
                bufferSize);

        int i = recorder.getState();
        if(i == 1){
            recorder.startRecording();
        }

        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "ÄudioRecorder Thread");

        recordingThread.start();
    }

    private void deleteTempFile(){
        File file = new File(getTempFileName());
        file.delete();
    }

    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen,
                                     long longSampleRate, int channels,long byteRate) throws IOException{

        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private void copyWavFile(String inFileName, String outFileName){
        FileInputStream in = null;
        FileOutputStream out = null;

        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 1;

        long byteRate = RECORDER_BPP + RECORDER_SAMPLERATE + channels/8;

        byte data[] = new byte[bufferSize];

        try{
            in = new FileInputStream(inFileName);
            out = new FileOutputStream(outFileName);

            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            writeWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        }catch (FileNotFoundException fnf){
            fnf.printStackTrace();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    private void writeAudioDataToFile(){
        byte[] data = new byte[bufferSize];
        String filename = getTempFileName();
        FileOutputStream os = null;

        try{
            os = new FileOutputStream(filename);
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }

        int read = 0;

        if (os != null){
            while(isRecording){
                read = recorder.read(data, 0, bufferSize);
                short[] amps = new short[data.length/2];
                ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(amps);
                int max = 0;

                boolean neg = false;

                for (int amp: amps){
                    if(amp < 0){
                        if((-amp) > max){
                            max = (-amp);
                            neg = true;
                        }
                    }else{
                        if(amp > max){
                            max = amp;
                            neg = false;
                        }
                    }
                }
                if (neg)

//                this.currentAmp = (20 * Math.log10(max/REFERENCE));

                if(AudioRecord.ERROR_INVALID_OPERATION  != read){
                    try{
                        os.write(data);
                    }catch (IOException ioe){
                        ioe.printStackTrace();
                    }
                }
            }

            try{
                os.close();
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
        }
    }

    private String getTempFileName(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists()){
            tempFile.delete();
        }
        try{
            tempFile.createNewFile();
        }catch (IOException e){
            e.printStackTrace();
        }

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private void setButtonHandlers(){
        Button stopButton, uploadButton, retakeButton;

        (retakeButton = findViewById(R.id.retake)).setOnClickListener(this);
        (stopButton = findViewById(R.id.stop)).setOnClickListener(this);
        (uploadButton = findViewById(R.id.upload)).setOnClickListener(this);
    }

    private void stopRecording(){
        if (recorder != null){
            isRecording = false;

            int i = recorder.getState();
            if(i == 1){
                recorder.stop();
            }

            recorder.release();

            recorder = null;

            while(true){
                try{
                    recordingThread.join();
                    recordingThread = null;
                    break;
                }catch (InterruptedException ie){

                }
            }

            copyWavFile(getTempFileName(), getFileName());
            deleteTempFile();

            series.appendData(new DataPoint(currentAmp + 1,0), false, MAX_DATA_POINTS);
        }
    }

    @Override
    protected  void onPause(){
        super.onPause();
        handler.removeCallbacks(timer);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.stop){
            stopRecording();
        }
    }
}
