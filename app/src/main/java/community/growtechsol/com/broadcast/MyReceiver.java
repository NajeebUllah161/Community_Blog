package community.growtechsol.com.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;

import static android.content.Intent.EXTRA_CHOSEN_COMPONENT;
import static community.growtechsol.com.utils.helper.shared;

public class MyReceiver extends BroadcastReceiver {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public void onReceive(Context context, Intent intent) {
        String selectedAppPackage = String.valueOf(intent.getExtras().get(EXTRA_CHOSEN_COMPONENT));
        if (!selectedAppPackage.isEmpty()) {
            shared = true;
        }
    }
}
