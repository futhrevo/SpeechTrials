package in.hedera.reku.speechtrial;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.RecognizerIntent;

import java.util.List;

/**
 * Created by rakeshkalyankar on 16/03/17.
 */

public class Utils {
    public static boolean isSpeechAvailable(Context context){
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        boolean available = true;
        if(activities.size() == 0){
            available = false;
        }
        return available;
    }

}
