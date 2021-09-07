package com.fxkxb.homework0906;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ExecutorService fixedThreadPool;
    private final int THREAD_NUMBERS = 3;
    private final String TAG = "";
    private IntentFilter intentFilter;
    private LocalReceiver localReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private ImageView mImageView = null;
    private ProgressBar mProgressBar = null;
    private static final int RECEVED_LOCAL_BROADCAST = 1;
    String URI = "https://fxkxb.com/bg3.jpg";

    //下载服务
    private DownloadService.DownloadBiner downloadBiner;

    //下载服务
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            downloadBiner = (DownloadService.DownloadBiner) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RECEVED_LOCAL_BROADCAST:
                    Toast.makeText(getApplicationContext(), msg.obj.toString(),Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //下载服务
        Button startDownload = findViewById(R.id.button3);
        Button pauseDownload = findViewById(R.id.button4);
        Button cancelDownload = findViewById(R.id.button5);

        Intent intent = new Intent(this, DownloadService.class);
        startService(intent);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            },1);
        }

        startDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadBiner.startDownload(URI);
            }
        });
        pauseDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadBiner.pauseDownload();
            }
        });
        cancelDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadBiner.cancelDownload();
            }
        });




        fixedThreadPool = Executors.newFixedThreadPool(THREAD_NUMBERS);
        // 获取实例。
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        this.mImageView = (ImageView) findViewById(R.id.imageView);
        this.mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        Button button = findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //实例化异步任务
                ImageDownloadTask task = new ImageDownloadTask();
                //执行异步任务

                task.execute(URI);
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1){
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "sf", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        final MainActivity self = this;

        Button clickButton = findViewById(R.id.button);
        clickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fixedThreadPool.execute(new Another() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.e(TAG, "run: ");

                        Intent intent = new Intent("com.fxkxb.homework0906.LOCAL_BROADCAST");
                        localBroadcastManager.sendBroadcast(intent);

//                        this.setCallback(Handler.Callback);

                        Message msg = new Message();
                        msg.what = RECEVED_LOCAL_BROADCAST;
                        msg.obj = "Received Message";
                        handler.sendMessage(msg);

//                        先调用Looper.prepare();再调用Toast.makeText().show();最后再调用Looper.loop();
//                        Looper.prepare();
//                        Toast.makeText(getApplicationContext(), "Delay", Toast.LENGTH_SHORT).show();
//                        Looper.loop();
                    }
                });
            }
        });

        intentFilter = new IntentFilter();
        intentFilter.addAction("com.fxkxb.homework0906.LOCAL_BROADCAST");
        localReceiver = new LocalReceiver();
        localBroadcastManager.registerReceiver(localReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        localBroadcastManager.unregisterReceiver(localReceiver);
    }

    class LocalReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Received Local Broadcast", Toast.LENGTH_SHORT).show();
        }
    }

    class ImageDownloadTask extends AsyncTask<String,Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = null;    //待返回的结果
            String url = params[0];  //获取URL
            URLConnection connection;   //网络连接对象
            InputStream is;    //数据输入流
            try {
                connection = new URL(url).openConnection();
                is = connection.getInputStream();   //获取输入流
                BufferedInputStream buf = new BufferedInputStream(is);
//解析输入流
                bitmap = BitmapFactory.decodeStream(buf);
                is.close();
                buf.close();
                String URL = "https://fxkxb.com/bg3.jpg";
            } catch (MalformedURLException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            }
//返回给后面调用的方法
            return bitmap;
        }

        @Override
        protected void onPreExecute() {
//显示等待圆环
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
//下载完毕，隐藏等待圆环
            mProgressBar.setVisibility(View.GONE);
            mImageView.setImageBitmap(result);
        }
    }

}