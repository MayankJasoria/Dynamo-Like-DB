package com.cloudproject.dynamo.msgmanager;
import javax.management.timer.Timer;
import java.util.Date;

public class TimeoutTimer extends Timer {
    private long ttl;
    private DynamoNode source;

    public TimeoutTimer(long mSecsTTL, DynamoServer server, DynamoNode node) {
        super();
        this.ttl = mSecsTTL;
        this.source = node;
        addNotificationListener(server, null, null);
    }

    public void start() {
        this.reset();
        super.start();
    }

    public void reset() {
        removeAllNotifications();
        setWakeupTime(ttl);
    }

    private void setWakeupTime(long mSecs) {
        addNotification("type", "message", source, new Date(System.currentTimeMillis()+mSecs));
    }
}
