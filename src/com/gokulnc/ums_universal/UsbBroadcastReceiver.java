package com.gokulnc.ums_universal;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.stericson.RootShell.execution.Command;
import com.stericson.RootShell.execution.Shell;
import com.stericson.RootTools.RootTools;

import java.util.Date;

import static android.content.Context.NOTIFICATION_SERVICE;

public class UsbBroadcastReceiver extends BroadcastReceiver {

    static SharedPreferences data;
    private static long lastAccessTime = Long.MAX_VALUE;
   // public static boolean isNewInstance = true; //These state variables get lost if app is opened & closed, so to read from SharedPref when that happens
    public static boolean isUMSdisabled = true;

    static final String usbStateChangeAction = "android.hardware.usb.action.USB_STATE";
    static final String intentEnableUMS = "com.gokulnc.ums_universal.ACTION_ENABLE_UMS";
    static final String intentDisableUMS = "com.gokulnc.ums_universal.ACTION_DISABLE_UMS";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(MainActivity.LOG_TAG, "Received Broadcast: "+action);
        if(data==null) data = context.getSharedPreferences(MainActivity.MyPREFERENCES, Context.MODE_PRIVATE);

        if(data.getBoolean(MainActivity.autoStart, false) && !MainActivity.isAppOpen && action.equalsIgnoreCase(usbStateChangeAction)) {
            if(intent.getBooleanExtra("connected", false)) toggleUMS(true, false, context);
            else toggleUMS(false, true, context);
        } else if(action.equalsIgnoreCase(intentEnableUMS)) {
            if(intent.getBooleanExtra("fromWidget", false) && !data.getBoolean(MainActivity.widgetEnabled, false)) {
                data.edit().putBoolean(MainActivity.widgetEnabled, true).apply();
                return; //all this bullshit to ignore the onUpdate() from widget when widget created for first time
            }
            toggleUMS(true, false, context);
        } else if(action.equalsIgnoreCase(intentDisableUMS)) {
            if(intent.getBooleanExtra("fromWidget", false) && !data.getBoolean(MainActivity.widgetEnabled, false)) {
                data.edit().putBoolean(MainActivity.widgetEnabled, true).apply();
                return;
            }
            //Notification will not be removed when toggled from Notification
            boolean removeNotif = intent.getBooleanExtra("removeNotif", true);
            toggleUMS(false, removeNotif, context);
        //} else if(action.equalsIgnoreCase("android.intent.action.BOOT_COMPLETED")) {

        } else if(action.equalsIgnoreCase(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
            Toast.makeText(context, "Download Complete", Toast.LENGTH_SHORT).show();
            try {
                installUpdate(context, intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void installUpdate(Context context, Intent intent) throws Exception {
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        long downloadId = intent.getExtras().getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
        Log.d(MainActivity.LOG_TAG, "download id: "+downloadId);
        DownloadManager dlMgr = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            /*try {
                dlMgr.openDownloadedFile(downloadId);
            } catch (Exception e) {
                e.printStackTrace();
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.XdaThreadURL)));
            }*/

        //install.setDataAndType(downloadedApk, dlMgr.getMimeTypeForDownloadedFile(downloadId)); //treats apk as a zip, lol
        Uri downloadedApk = dlMgr.getUriForDownloadedFile(downloadId);
        install.setDataAndType(getFileUriFromUri(context, downloadedApk) , "application/vnd.android.package-archive");
        //MIME-type referred here: http://stackoverflow.com/questions/20065040/download-installing-and-delete-apk-file-on-android-device-programmatically-fro
        context.startActivity(install);
    }

    public static Uri getFileUriFromUri(Context c, Uri uri) {
        //Reference: http://stackoverflow.com/questions/9194361/how-to-use-android-downloadmanager
        try {
            if ("content".equals(uri.getScheme())) { //Android 4.2 and above behavior
                String[] filePathColumn = {MediaStore.MediaColumns.DATA};
                ContentResolver contentResolver = c.getContentResolver();

                Cursor cursor = contentResolver.query(uri, filePathColumn, null,
                        null, null);

                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                cursor.close();
                return Uri.parse("file://" + filePath);
            } else if ("file".equals(uri.getScheme())) {
                return uri;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }

    void openApp(Context context) {
        Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    boolean toggleUMS(boolean enableUMS, boolean removeNotif, Context context) {
        if(new Date().getTime()-lastAccessTime > 1500 || lastAccessTime==Long.MAX_VALUE) {
            lastAccessTime = new Date().getTime();
            return enableUMS?enableUMS(context):disableUMS(context, removeNotif);
        }
        return false;
    }

    boolean enableUMS(Context context) {
        try {
            String enableCommand = data.getString(MainActivity.setPermissionCmds, "")+"\n"+data.getString(MainActivity.enableUMScmds, "");
            if(enableCommand.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.toast_open_app_once), Toast.LENGTH_SHORT).show();
                return false;
            }
            Shell rootShell = RootTools.getShell(true);
            Command cmd = new Command(0, "setenforce 0\n"+enableCommand);
            rootShell.add(cmd);
            //if(!rootShell.isExecuting) rootShell.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.error_ums), Toast.LENGTH_SHORT).show();
            openApp(context);
            return false;
        }
        Toast.makeText(context, context.getString(R.string.toast_ums_enabled), Toast.LENGTH_SHORT).show();
        isUMSdisabled = false;
        showNotification(context);
        return true;
    }

    boolean disableUMS(Context context, boolean removeNotif) {
        try {
            String disableCommand = data.getString(MainActivity.disableUMScmds, "");
            if(disableCommand.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.toast_open_app_once), Toast.LENGTH_SHORT).show();
                return false;
            }
            Shell rootShell = RootTools.getShell(true);
            Command cmd = new Command(0, disableCommand);
            rootShell.add(cmd);
            //if(!rootShell.isExecuting) rootShell.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.error_ums_disable), Toast.LENGTH_SHORT).show();
            return false;
        }
        Toast.makeText(context, context.getString(R.string.toast_ums_disabled), Toast.LENGTH_SHORT).show();
        isUMSdisabled = true;
        if(removeNotif) removeNotification(context);
        else showNotification(context);
        //TODO: if(MainActivity.isAppOpen) MainActivity.updateUSBconfig(); //When toggled from notif
        return true;
    }

    public static void showNotification(Context context) {

        if(data==null) data = context.getSharedPreferences(MainActivity.MyPREFERENCES, Context.MODE_PRIVATE);

        if(data.getBoolean(MainActivity.NotifsEnable, true) || data.getBoolean(MainActivity.autoStart, false)&&!MainActivity.isAppOpen) {
            PendingIntent pi = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);
            PendingIntent ums_action;
            if(isUMSdisabled) {
                ums_action = PendingIntent.getBroadcast(context, 0, new Intent(intentEnableUMS), PendingIntent.FLAG_CANCEL_CURRENT);
            } else {
                Intent i = new Intent(intentDisableUMS);
                i.putExtra("removeNotif", false);
                ums_action = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
            }

            NotificationManager notificationManager  = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            Notification notification = new NotificationCompat.Builder(context)
                    .setTicker(isUMSdisabled ? context.getString(R.string.notif_ums_disabled) : context.getString(R.string.notif_ums_enabled))
                    .setSmallIcon(android.R.drawable.stat_notify_sdcard_usb)
                    .setContentTitle(isUMSdisabled ? context.getString(R.string.notif_ums_disabled) : context.getString(R.string.notif_ums_enabled))
                    .setContentText(isUMSdisabled ? context.getString(R.string.notif_ums_disabled_msg):context.getString(R.string.notif_ums_enabled_msg))
                    .setContentIntent(pi)
                    //.setOngoing(true)
                    .addAction(new android.support.v4.app.NotificationCompat.Action.Builder(
                            R.drawable.empty_icon,
                            isUMSdisabled ? context.getString(R.string.action_enable_ums_short): context.getString(R.string.action_disable_ums_short),
                            ums_action
                    ).build())
                    .build();
            if(notification!=null) notificationManager.notify(0, notification);
        }
    }

    public static void removeNotification(Context context) {
        NotificationManager notificationManager  = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

}