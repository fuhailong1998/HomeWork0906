package com.fxkxb.homework0906;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Objects;
import java.util.concurrent.RecursiveTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author : Hailong Fu (hailong.fu@thundersoft.com)
 * @version : 1.0
 * @file : DownTask.class
 * @date : September 07,2021 11:29
 * @description :
 */
public class DownloadTask extends AsyncTask<String, Integer, Integer> {
    public DownloadTask(DownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);
        switch (integer){
            case TYPE_SUCCESS:
                downloadListener.onSuccess();
                break;
            case TYPE_FAILED:
                downloadListener.onFailed();
                break;
            case TYPE_PAUSED:
                downloadListener.onPasued();
                break;
            case TYPE_CANCELED:
                downloadListener.onCancled();
                break;
            default:
                break;
        }
    }

    public void pasueDownload(){
        isPaused = true;
    }

    public void cancelDownload(){
        isCanceled = true;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        int progress = values[0];
        if (progress > lastProgress) {
            downloadListener.onProgress(progress);
            lastProgress = progress;
        }
    }

    private long getContentLength(String downloadUrl) throws IOException{
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = okHttpClient.newCall(request).execute();
        if (response.isSuccessful()) {
            long contentLength = Objects.requireNonNull(response.body()).contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }

    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;

    private DownloadListener downloadListener;
    private boolean isCanceled = false;
    private boolean isPaused = false;
    private int lastProgress;

    public void DownloadTask(DownloadListener downloadListener){
        this.downloadListener = downloadListener;
    }


    @Override
    protected Integer doInBackground(String... strings) {
        InputStream inputStream = null;
        RandomAccessFile savedFile = null;
        File file = null;
        try{
            long downloadedlength = 0;
            String downloadUrl = strings[0];
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory + fileName);
            if (file.exists()){
                downloadedlength = file.length();
            }
            long contentlength = getContentLength(downloadUrl);
            if (contentlength == 0){
                return TYPE_FAILED;
            }else if (contentlength == downloadedlength){
                return TYPE_SUCCESS;
            }
            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("RANGE", "bytes=" + downloadedlength + "-")
                    .url(downloadUrl)
                    .build();
            Response response = okHttpClient.newCall(request).execute();
            inputStream = Objects.requireNonNull(response.body()).byteStream();
            savedFile = new RandomAccessFile(file, "rw");
            savedFile.seek(downloadedlength);
            byte[] b = new byte[1024];
            int total = 0;
            int len;
            while((len = inputStream.read(b)) != -1){
                if (isCanceled) {
                    return TYPE_CANCELED;
                }else if (isPaused) {
                    return TYPE_PAUSED;
                }else {
                    total += len;
                    savedFile.write(b, 0, len);
                    int progress = (int) ((int) ((total + downloadedlength) * 100)/ contentlength);
                    publishProgress(progress);
                }
            }
            Objects.requireNonNull(response.body()).close();
            return TYPE_SUCCESS;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (savedFile != null) {
                try {
                    savedFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (isCanceled && file != null) {
                file.delete();
            }
        }
        return TYPE_FAILED;
    }
}