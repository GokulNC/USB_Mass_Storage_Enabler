package com.gokulnc.ums_universal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

class Dialogs {

    static void noRootAccess(final String rootDetails, final Activity activity) {

        final Context context = activity.getApplicationContext();
        
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, rootDetails, Toast.LENGTH_SHORT).show();
                Toast.makeText(context, context.getString(R.string.toast_no_root), Toast.LENGTH_SHORT);
                AlertDialog.Builder fail = new AlertDialog.Builder(activity);
                fail.setTitle(context.getString(R.string.toast_no_root));
                CharSequence msg = context.getString(R.string.error_msg)+" \""+rootDetails+"\"\n\n"+context.getString(R.string.error_details)+"\n"+context.getString(R.string.alert_no_root_msg);
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

    static void noSupport(final Activity activity) {

        final Context context = activity.getApplicationContext();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder fail = new AlertDialog.Builder(activity);
                fail.setTitle("App Failed");
                String msg;
                if(MainActivity.availableLUNs.isEmpty()) msg = context.getString(R.string.alert_no_gadget_msg);
                else if(MainActivity.blockDevice==null || MainActivity.blockDevice.isEmpty()) msg = context.getString(R.string.alert_mmc_not_found_msg);
                else msg = context.getString(R.string.error_unknown);
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

    static void multipleLUNsWarning(final Activity activity) {

        final Context context = activity.getApplicationContext();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder warning = new AlertDialog.Builder(activity);
                warning.setTitle(R.string.alert_warning);
                warning.setMessage(context.getString(R.string.alert_multi_luns_msg));
                warning.setPositiveButton(context.getString(R.string.action_ok),new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {  }
                });

                warning.create();
                warning.show();
            }
        });
    }

    static void showUnmountWarning(final Activity activity) {

        final Context context = activity.getApplicationContext();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder warn = new AlertDialog.Builder(activity);
                warn.setTitle(R.string.alert_warning);
                String msg = context.getString(R.string.alert_block_notrecommended)+"\n\n"+context.getString(R.string.alert_unmount);
                warn.setMessage(msg);
                warn.setPositiveButton(context.getString(R.string.action_ok),new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {   }
                });
                warn.create();
                warn.show();
            }
        });
    }

}