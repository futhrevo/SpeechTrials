package in.hedera.reku.speechtrial;

import android.app.Notification;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationListenerService extends android.service.notification.NotificationListenerService {
    public NotificationListenerService() {
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification){
        String notificationDescription = String.valueOf(notification.getId()) + ". " + notification.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT, " ").toString();
        Log.i(getClass().toString(), notificationDescription + " onNotificationPosted");
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification){
        String notificationDescription = String.valueOf(notification.getId()) + ". " + notification.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT, " ").toString();
        Log.i(getClass().toString(), notificationDescription + " onNotificationRemoved");
    }
}
