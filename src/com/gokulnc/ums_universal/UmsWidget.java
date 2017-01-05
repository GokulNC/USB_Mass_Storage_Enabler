package com.gokulnc.ums_universal;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class UmsWidget extends AppWidgetProvider {

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        //Since onUpdate() is also executed when widget is added, to avoid that, this value will be handled by the BroadcastRx
        /*if(UsbBroadcastReceiver.data==null) UsbBroadcastReceiver.data = context.getSharedPreferences(MainActivity.MyPREFERENCES, Context.MODE_PRIVATE);
        UsbBroadcastReceiver.data.edit().putBoolean(MainActivity.widgetEnabled, true).apply();*/
    }

    public void onDisabled(Context context) {
        super.onDisabled(context);
        if(UsbBroadcastReceiver.data==null) UsbBroadcastReceiver.data = context.getSharedPreferences(MainActivity.MyPREFERENCES, Context.MODE_PRIVATE);
        UsbBroadcastReceiver.data.edit().putBoolean(MainActivity.widgetEnabled, false).apply();
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        final int count = appWidgetIds.length;

        for (int i = 0; i < count; i++) {
            int widgetId = appWidgetIds[i];

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_ums);

            Intent toggleIntent = new Intent();
            if(UsbBroadcastReceiver.isUMSdisabled) {
                remoteViews.setImageViewResource(R.id.UMSwidgetButton, R.drawable.usb_on);
                toggleIntent.setAction(UsbBroadcastReceiver.intentEnableUMS);
            } else {
                remoteViews.setImageViewResource(R.id.UMSwidgetButton, R.drawable.usb_off);
                toggleIntent.setAction(UsbBroadcastReceiver.intentDisableUMS);
            }
            toggleIntent.putExtra("fromWidget", true);
            context.sendBroadcast(toggleIntent);

            Intent intent = new Intent(context, UmsWidget.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setOnClickPendingIntent(R.id.UMSwidgetButton, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

}
