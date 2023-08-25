package com.rust.sip;

import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import com.autulin.gb28181library.JNIBridge;
import com.autulin.gb28181library.MediaOutput;
import com.autulin.gb28181library.MediaRecorderBase;
import com.autulin.gb28181library.MediaRecorderNative;
import com.autulin.gb28181library.utils.DeviceUtils;
import com.rust.sip.GB28181.GB28181CallBack;
import com.rust.sip.GB28181.gb28181.GB28181Params;
import com.rust.sip.GB28181.tools.NetUtils;

public class MainActivity extends AppCompatActivity implements
        MediaRecorderBase.OnErrorListener, MediaRecorderBase.OnPreparedListener,View.OnClickListener {
    public MyService mService;
    private ServiceConnection mConnection;
    Intent mServiceIntent;


    private MediaRecorderNative mMediaRecorder;
    private MediaOutput mediaOutput;
    private SurfaceView mSurfaceView;


    private Button start_btn;
    private Button register_btn;
    private EditText server_ip;
    private EditText server_port;
    private EditText server_id;
    private EditText user_id;
    private EditText user_password;
//    private TextView local_ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //region 权限申请
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.CAMERA) ==
                PermissionChecker.PERMISSION_DENIED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        //endregion

        initView();
        MediaRecorderBase.QUEUE_MAX_SIZE = 20;
        Log.e("debug", "SMALL_VIDEO_HEIGHT: " + MediaRecorderBase.SMALL_VIDEO_HEIGHT +
                ", SMALL_VIDEO_WIDTH:" + MediaRecorderBase.SMALL_VIDEO_WIDTH);
        initMediaRecorder();



        //region GB service
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MyService.MyBinder mBinder = (MyService.MyBinder) service;
                mService = mBinder.getService();
                Log.e("debug", "mService :" +mService);
                if (mService != null) {
                    mService.setGB28181CallBack(new GB28181CallBack() {
                        @Override
                        public void onStartRtp(int ssrc, String url, int port, int is_udp) {
                            //udp
                            Log.e("debug", "is_udp :" +is_udp);
                            if (is_udp == 0) {
                                mMediaRecorder.setUdpOutPut(url, port, ssrc);
                            } else {
                                mMediaRecorder.setTcpOutPut(url, port, 8088, ssrc);
                            }
                            Log.e("debug", "开始RTP推流: " + url + ":" + port + "  ssrc:" + ssrc + "  is_udp:" + is_udp);
                            mMediaRecorder.startMux();
                        }

                        @Override
                        public void onStopRtp(int ssrc) {
                            Log.e("debug", "onStopRtp ssrc:" + ssrc);
                            mMediaRecorder.endMux();
                        }
                    });
                    mService.GB28181Init();
                    mService.GB28181_Start();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
            }
        };
        mServiceIntent = new Intent(this, MyService.class);
        bindService(mServiceIntent, mConnection, Service.BIND_AUTO_CREATE);
        //endregion



//        mServiceIntent = new Intent(this, MyService.class);
    }


    public void  setUp(){
        //region GB service
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MyService.MyBinder mBinder = (MyService.MyBinder) service;
                mService = mBinder.getService();
                Log.e("debug", "mService :" +mService);
                if (mService != null) {
                    mService.setGB28181CallBack(new GB28181CallBack() {
                        @Override
                        public void onStartRtp(int ssrc, String url, int port, int is_udp) {
                            //udp
                            Log.e("debug", "is_udp :" +is_udp);
                            if (is_udp == 0) {
                                mMediaRecorder.setUdpOutPut(url, port, ssrc);
                            } else {
                                mMediaRecorder.setTcpOutPut(url, port, 8088, ssrc);
                            }
                            Log.e("debug", "开始RTP推流: " + url + ":" + port + "  ssrc:" + ssrc + "  is_udp:" + is_udp);
                            mMediaRecorder.startMux();
                        }

                        @Override
                        public void onStopRtp(int ssrc) {
                            Log.e("debug", "onStopRtp ssrc:" + ssrc);
                            mMediaRecorder.endMux();
                        }
                    });
//                    mService.GB28181Init();
                    mService.GB28181_Start();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
            }
        };
//        mServiceIntent = new Intent(this, MyService.class);
        bindService(mServiceIntent, mConnection, Service.BIND_AUTO_CREATE);

    }

    /**
     * 初始化画布
     */
    private void initSurfaceView() {
        final int w = DeviceUtils.getScreenWidth(this);
        // 避免摄像头的转换，只取上面h部分
        // ((RelativeLayout.LayoutParams) bottomLayout.getLayoutParams()).topMargin = (int) (w / (MediaRecorderBase.SMALL_VIDEO_HEIGHT / (MediaRecorderBase.SMALL_VIDEO_WIDTH * 1.0f)));
        int width = w;
        int height = (int) (w * (MediaRecorderBase.mSupportedPreviewWidth * 1.0f)) / MediaRecorderBase.SMALL_VIDEO_HEIGHT;
        Log.e("debug", "initSurfaceView: w=" + width + ",h=" + height);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mSurfaceView.getLayoutParams();
        lp.width = width;
        lp.height = height;
        mSurfaceView.setLayoutParams(lp);
    }

    /**
     * 初始化拍摄SDK
     */
    private void initMediaRecorder() {
        mMediaRecorder = new MediaRecorderNative();
        mMediaRecorder.setOnErrorListener(this);
        mMediaRecorder.setOnPreparedListener(this);
//        mMediaRecorder.setCameraFront();   // 设置前置摄像头
        mMediaRecorder.setCameraBack();
        // 设置输出
        // mMediaRecorder.setFileOutPut("aaa.ps");  //输出到文件，这里demo是/sdcard/pstest/tttttt.ps
        //mMediaRecorder.setUdpOutPut(ip, port, 1);
        mMediaRecorder.setSurfaceHolder(mSurfaceView.getHolder());
        mMediaRecorder.prepare();
    }

    public void switchCamera(){
//        mMediaRecorder.switchCamera();
        mMediaRecorder. setCameraBack();
        mMediaRecorder.setCameraFront();
    }

    private void initView() {
        start_btn = findViewById(R.id.start_btn);
        start_btn.setOnClickListener(this);
        mSurfaceView = findViewById(R.id.record_preview);
        register_btn = findViewById(R.id.register_btn);
        register_btn.setOnClickListener(this);
//        local_ip = findViewById(R.id.local_ip);
//        local_ip.setText(NetUtils.getIPAddress(this));
        server_ip = findViewById(R.id.server_ip);
        server_port = findViewById(R.id.server_port);
        server_id = findViewById(R.id.server_id);
        user_id = findViewById(R.id.user_id);
        user_password = findViewById(R.id.user_password);
        //server_ip.setText("192.168.31.78");
//        server_ip.setText("192.168.1.6");
//        server_port.setText("15060");
//        server_id.setText("34020000002000000001");
//        user_id.setText("34020000001320000001");
//        user_password.setText("12345678");
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_btn:
//                if (mMediaRecorder != null) {
//                    if (!mMediaRecorder.mRecording) {
//                        mMediaRecorder.setUdpOutPut("192.168.1.6", 15060, 1);
//                        start_btn.setText("点击结束");
//                        mMediaRecorder.startMux();
//                    } else {
//                        start_btn.setText("点击开始");
//
//                        mMediaRecorder.endMux();
//                    }
//                }

                String serverIp = server_ip.getText().toString();
                String serverPort = server_port.getText().toString();
                String serverId = server_id.getText().toString();
                String userId = user_id.getText().toString();
                String userPassword = user_password.getText().toString();


                String ip = NetUtils.getIPAddress(getApplication());
                GB28181Params.setLocalSIPIPAddress(ip);//本机地址

                GB28181Params.setSIPServerIPAddress(serverIp);//SIP服务器地址
                GB28181Params.setRemoteSIPServerID(serverId);
                GB28181Params.setRemoteSIPServerSerial(serverId.substring(0, 10));
                GB28181Params.setLocalSIPDeviceId(userId);
                GB28181Params.setLocalSIPMediaId(userId);
                GB28181Params.setPassword(userPassword);//密码
                GB28181Params.setRemoteSIPServerPort(Integer.valueOf(serverPort));//SIP服务器端口


                GB28181Params.setLocalSIPPort(5060);//本机端口
                GB28181Params.setCameraHeigth(480);
                GB28181Params.setCameraWidth(640);
                // GB28181Params.setCameraHeigth(720);
                // GB28181Params.setCameraWidth(1080);

                GB28181Params.setCurGBState(0);
                GB28181Params.setCurDeviceDownloadMeidaState(0);
                GB28181Params.setCurDevicePlayMediaState(0);
                GB28181Params.setCameraState(0);

                if(null != mService){
                    unbindService(mConnection);
                }

                setUp();
                break;

            case R.id.register_btn:


                break;
        }


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaRecorder != null) mMediaRecorder.endMux();
        unbindService(mConnection);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int result : grantResults) {
                if (result != PermissionChecker.PERMISSION_GRANTED) {
                    this.finish();
                }
            }
        }
    }

    @Override
    public void onPrepared() {
        Log.e("debug", "onPrepared");
//        initSurfaceView();
    }

    @Override
    public void onVideoError(int what, int extra) {
        Log.e("debug", "onVideoError what:" + what + "  extra:" + extra);
    }

    @Override
    public void onAudioError(int what, String message) {
        Log.e("debug", "onAudioError what:" + what + "  message:" + message);
    }
}
