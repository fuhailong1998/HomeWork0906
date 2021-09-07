package com.fxkxb.homework0906;

public interface DownloadListener {
    void onProgress(int progress);
    void onSuccess();
    void onFailed();
    void onPasued();
    void onCancled();
}
