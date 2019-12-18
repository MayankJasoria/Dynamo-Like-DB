package com.cloudproject.dynamo.msgmanager;
import javax.management.timer.Timer;
import java.util.Date;

/**
 * Timer class. DynamoServer can register a node from its node list to the timer class.
 * On start of the timer, the registered class (DynamoServer) is notified of the expiry
 * of TTL of the registered node. The DynamoServer should handle this notification by
 * remove the node entry from its node list.
 */

public class TimeoutTimer extends Timer {
    private long ttl;
    private DynamoNode source;

    public TimeoutTimer(long mSecsTTL, DynamoServer server, DynamoNode node) {
        super();
        this.ttl = mSecsTTL;
        this.source = node;
        addNotificationListener(server, null, null);
    }

    /**
     * Starts the timer.
     */

    public void start() {
        this.reset();
        super.start();
    }

    /**
     * Removes any pending notification, and adds a new notification with this.ttl as
     * Time To Live.
     */

    public void reset() {
        removeAllNotifications();
        setWakeupTime(ttl);
    }

    /**
     * Adds a notification.
     * @param mSecs Notification is triggered after mSecs milliseconds.
     */

    private void setWakeupTime(long mSecs) {
        addNotification("type", "message", source, new Date(System.currentTimeMillis()+mSecs));
    }
}
