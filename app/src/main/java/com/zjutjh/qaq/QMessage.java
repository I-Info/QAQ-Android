package com.zjutjh.qaq;

public class QMessage {
    public static final int TYPE_LEFT = 0x0;
    public static final int TYPE_RIGHT = 0x1;

    private final int msgType;

    private final String user;
    private final String date;
    private final String content;

    public QMessage(String user, String date, String content, int msgType) {
        this.user = user;
        this.date = date;
        this.content = content;
        this.msgType = msgType;
    }

    public int getType() {
        return msgType;
    }

    public String getUser() {
        return user;
    }

    public String getDate() {
        return date;
    }

    public String getContent() {
        return content;
    }
}
