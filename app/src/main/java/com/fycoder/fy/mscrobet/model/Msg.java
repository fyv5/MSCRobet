package com.fycoder.fy.mscrobet.model;

/**
 * Created by fy on 2016/10/8.
 */
public class Msg {

    private String message;
    private boolean isComing;

    public Msg(String message, boolean isComing){
        this.message = message;
        this.isComing = isComing;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean getIsComing() {
        return isComing;
    }

    public void setIsComing(boolean isComing) {
        this.isComing = isComing;
    }
}
