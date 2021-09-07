package com.fxkxb.homework0906;

/**
 * @author : Hailong Fu (hailong.fu@thundersoft.com)
 * @version : 1.0
 * @file : Another.class
 * @date : September 07,2021 11:03
 * @description :
 */
public class Another implements Runnable {
    private Callback mCallback;

    public interface Callback {
        void onNewMessage(String string);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void newMessage() {
        if (mCallback != null) {
            mCallback.onNewMessage("Message From Another");
        }
    }

    public void newThread() {
        Thread childThread = new Thread(this, "Another");
        childThread.start();
    }

    @Override
    public void run() {
        newMessage();
    }
}