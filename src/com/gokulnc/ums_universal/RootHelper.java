package com.gokulnc.ums_universal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootShell.execution.Shell;
import com.stericson.RootTools.RootTools;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.gokulnc.ums_universal.Dialogs.noRootAccess;
import static com.gokulnc.ums_universal.Constants.LOG_TAG;

class RootHelper {
    //A wrapper for Stericsson's RootTools

    private Shell rootShell;
    private Activity activity;
    private Context context;

    RootHelper(Activity activity) {
        this.activity = activity; //this is quite unrecommended, shouldn't be doing it
        context = activity.getApplicationContext();
        getRootShell();
    }

    void executeAsSU(final String command) {

        Command cmd = new Command(0, command);

        try {
            if(rootShell==null || rootShell.isClosed) getRootShell();
            rootShell.add(cmd);
        } catch (final Exception e) {
            e.printStackTrace();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder fail = new AlertDialog.Builder(activity);
                    fail.setTitle(context.getString(R.string.error_general));
                    CharSequence msg = context.getString(R.string.error_execution)+"\n\n"+command+"\n\n"+e.getMessage()+"\n\n"+context.getString(R.string.error_contact);
                    fail.setMessage(msg);
                    fail.setPositiveButton(context.getString(R.string.action_ok),new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            activity.finish();
                        }
                    });

                    fail.setCancelable(false);
                    fail.create();
                    fail.show();
                }
            });
        }

    }

    private void getRootShell() {

        try {

            if (!RootTools.isRootAvailable()) {
                Log.d(LOG_TAG, "It seems su binary is missing, or unable to search for it.");
                noRootAccess(context.getString(R.string.error_su_not_found)+" \n"+context.getString(R.string.error_no_root), activity);
                return;
            }
            Log.d(LOG_TAG, "Requesting for Root Access..");
            rootShell = RootTools.getShell(true);

        } catch (IOException e) {
            noRootAccess(context.getString(R.string.error_root_not_granted), activity);
            e.printStackTrace();
        } catch (TimeoutException e) {
            Log.d(LOG_TAG, "Timeout waiting for Root Access..");
            noRootAccess(context.getString(R.string.error_root_timeout), activity);
            e.printStackTrace();
        } catch (RootDeniedException e) {
            Log.d(LOG_TAG, "Denied Root Access..");
            noRootAccess(context.getString(R.string.error_root_denied), activity);
            e.printStackTrace();
        }
    }



}
