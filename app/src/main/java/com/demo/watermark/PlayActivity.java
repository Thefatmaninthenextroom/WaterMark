package com.demo.watermark;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.MediaController;
import android.widget.VideoView;

import com.demo.watermark.utils.FFmpegKit;
import com.demo.watermark.utils.ThreadPoolUtils;
import com.demo.watermark.view.WaitDialog;

import java.io.File;
import java.util.Date;

public class PlayActivity extends AppCompatActivity {

    private static final String TAG = "PlayActivity";
    private VideoView mVideoView;

    private WaitDialog mWaitDialog;
    private Handler mHandler;
    private String videoAbsolutePath;
    private String textAbsolutePath;
    private String outputUrl;
    private File videoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        initData();
        initView();
    }

    private void initData() {
        Intent intent = getIntent();
        videoAbsolutePath = intent.getStringExtra("path");
        textAbsolutePath = intent.getStringExtra("imagePath");
        //获取路径
        String path = Environment.getExternalStorageDirectory() + File.separator + "video1";
        //定义文件名
        String fileName = new Date().getTime() + ".mp4";
        videoFile = new File(path, fileName);
        //文件夹不存在
        if (!videoFile.getParentFile().exists()) {
            videoFile.getParentFile().mkdirs();
        }
        outputUrl = videoFile.getAbsolutePath();
    }

    private void initView() {
        mVideoView = (VideoView) findViewById(R.id.view);
        //将视频和图片水印合成
        makeVideo();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    //视频播放的控制
                    playVideo();
                }
            }
        };
    }

    private void makeVideo() {
        mWaitDialog = new WaitDialog(PlayActivity.this, R.style.waitDialog);
        mWaitDialog.setWaitText("正在生成视频...请不要退出(大概一分钟左右)");
        mWaitDialog.show();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String[] commands = new String[12];
                commands[0] = "ffmpeg";
                commands[1] = "-i";
                commands[2] = videoAbsolutePath;
                commands[3] = "-i";
                commands[4] = textAbsolutePath;
                commands[5] = "-filter_complex";
                //水印位置：（x,y)=(10,10)<=(left,top)距离左侧、底部各多少像素
                commands[6]="overlay=main_w-overlay_w+75:main_h-overlay_h-1 ";
                commands[7] = "-b";        //这个是为了使视频质量不掉的太严重
                commands[8] = "2048k";     //这个是设置生成视频的大小
                commands[9] = "-codec:a";
                commands[10] = "copy";
                commands[11] = outputUrl;
                FFmpegKit.execute(commands, new FFmpegKit.KitInterface() {
                    @Override
                    public void onStart() {
                        Log.e("PlayActivity", "FFmpeg 命令行开始执行了...");
                    }

                    @Override
                    public void onProgress(int progress) {
                        Log.e("PlayActivity", "done com" + "FFmpeg 命令行执行进度..." + progress);
                    }

                    @Override
                    public void onEnd(int result) {
                        Log.e("PlayActivity", "FFmpeg 命令行执行完成...");
                        Message msg = new Message();
                        msg.what = 0;
                        mHandler.sendMessage(msg);
                        mWaitDialog.dismiss();
                    }
                });
            }
        };
        ThreadPoolUtils.execute(runnable);
    }

    private void playVideo() {
        mVideoView.setVideoPath(outputUrl);
        mVideoView.setMediaController(new MediaController(PlayActivity.this));
        if (!mVideoView.isPlaying()) {
            mVideoView.start();
        } else {
            mVideoView.pause();
        }
    }
}
