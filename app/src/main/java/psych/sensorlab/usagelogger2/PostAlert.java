package psych.sensorlab.usagelogger2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.ContextThemeWrapper;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import timber.log.Timber;

public class PostAlert {

    private final Context context;
    AlertDialog builder;
    boolean isDialogShowing = false;

    PostAlert(final Context context) {
        this.context = context;
    }

    void customiseMessage(final int messageID, final String permission, final String title,
                          final String content, final String whereToSend){
        final Intent resultOfAlert = new Intent(whereToSend);
        Timber.v("message ID: %s, whereToSend %s", messageID, whereToSend);
        builder = new AlertDialog.Builder(
                new ContextThemeWrapper(context, R.style.AlertDialogCustom))
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    resultOfAlert.putExtra("messageID", messageID);
                    resultOfAlert.putExtra("positiveResponse", true);
                    resultOfAlert.putExtra("permissionToRequest", permission);
                    if (!whereToSend.equals("nowhere")) {
                        LocalBroadcastManager.getInstance(context).sendBroadcast(resultOfAlert);
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) ->
                        builder.cancel()).create();

        builder.show();
        isDialogShowing = true;
        if (messageID == 5) {
            //show the password with bigger text size and allow it to be selected
            TextView msgTxt = builder.findViewById(android.R.id.message);
            msgTxt.setTextIsSelectable(true);
            msgTxt.setTextSize(25);
        }
        builder.getButton(builder.BUTTON_POSITIVE).setTextColor(Color.parseColor("#E91E63"));
        builder.getButton(builder.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#E91E63"));
    }

    public void dismissDialog(){
        builder.dismiss();
    }

    public boolean showingDialog(){
        return isDialogShowing;
    }
}
