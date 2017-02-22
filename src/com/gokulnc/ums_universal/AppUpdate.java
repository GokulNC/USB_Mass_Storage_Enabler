package com.gokulnc.ums_universal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.File;

import static com.gokulnc.ums_universal.Constants.LOG_TAG;
import static com.gokulnc.ums_universal.Constants.XdaThreadURL;

class AppUpdate {

    static String downloadURL = "";

    static void checkForUpdates(final boolean auto, final FirebaseRemoteConfig mFirebaseRemoteConfig, final Activity activity) {
        // https://github.com/firebase/quickstart-android
        final Context context = activity.getApplicationContext();
        Log.d(LOG_TAG, "Checking for new update..");
        //if(mFirebaseRemoteConfig==null) mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        //if(!auto) Toast.makeText(getApplicationContext(), getString(R.string.toast_checking_update), Toast.LENGTH_SHORT).show();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                //		.setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);

        long cacheExpiration = 3600;
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) cacheExpiration = 0;
        mFirebaseRemoteConfig.fetch(cacheExpiration).addOnCompleteListener(activity, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull final Task<Void> task) {

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder notify = new AlertDialog.Builder(activity);
                        StringBuilder msg = new StringBuilder("");
                        boolean foundNewUpdate = false;
                        notify.setTitle(context.getString(R.string.alert_check_update));
                        int latestVersionCode;
                        if (task.isSuccessful()) {
                            mFirebaseRemoteConfig.activateFetched();
                            latestVersionCode = (int) mFirebaseRemoteConfig.getLong("latest_version_number");
                            if(latestVersionCode > MainActivity.currentVersionNumber) {
                                downloadURL = mFirebaseRemoteConfig.getString("download_link");
                                msg.append(context.getString(R.string.alert_new_update)).append("\n");
                                msg.append(context.getString(R.string.alert_to_stop_autoupdate)).append("\n\n");
                                msg.append(context.getString(R.string.alert_lastest_ver)).append(mFirebaseRemoteConfig.getString("latest_version_name")).append(" (").append(latestVersionCode).append(")\n");
                                msg.append(context.getString(R.string.alert_current_ver)).append(MainActivity.currentVersion).append(" (").append(MainActivity.currentVersionNumber).append(")");
                                msg.append("\n\n").append(context.getString(R.string.alert_download_new));
                                foundNewUpdate = true;
                                notify.setPositiveButton(context.getString(R.string.action_update),new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        if (downloadURL != null && !downloadURL.isEmpty() && !downloadURL.equalsIgnoreCase(XdaThreadURL)) {
                                            final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE_for_app_download = 10;

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                                //Reference: http://forum.xda-developers.com/android/software/tutorial-manage-permissions-android-t3458683
                                                ActivityCompat.requestPermissions(activity,
                                                        new String[] { android.Manifest.permission.WRITE_EXTERNAL_STORAGE },
                                                        MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE_for_app_download);

                                            } else {
                                                downloadAndInstallApk(downloadURL, context);
                                            }
                                        } else {
                                            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(XdaThreadURL)));
                                        }
                                    }
                                });
                                notify.setNeutralButton(context.getString(R.string.action_show_changelog),new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        showChangelog(mFirebaseRemoteConfig.getString("changelog"), activity);
                                    }
                                });
                            } else {
                                msg.append(context.getString(R.string.alert_upto_date)).append(": ").append(MainActivity.currentVersion).append(" (").append(MainActivity.currentVersionNumber).append(")");
                            }
                        } else {
                            msg.append(context.getString(R.string.alert_update_failed)).append("\n\n").append(context.getString(R.string.alert_check_update_msg));
                            msg.append("\n\n").append(context.getString(R.string.alert_current_ver)).append(MainActivity.currentVersion).append(" (").append(MainActivity.currentVersionNumber).append(")");
                            notify.setPositiveButton(context.getString(R.string.action_yes),new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(XdaThreadURL)));
                                }
                            });
                        }

                        notify.setMessage(msg);
                        notify.setNegativeButton(context.getString(R.string.action_cancel),new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {

                            }
                        });
                        notify.create();
                        if(!auto) notify.show();
                        else if(foundNewUpdate) notify.show();
                    }
                });


            }
        });
    }

    static void downloadAndInstallApk(final String url, final Context context) {

            Toast.makeText(context, R.string.toast_downloading, Toast.LENGTH_SHORT).show();
            new Thread() {
                @Override
                public void run() {
                    try {
                        //Reference: http://stackoverflow.com/a/4969421/5002496
                        String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
                        String fileName = "UMS_Enabler.apk";
                        destination += fileName;
                        final Uri uri = Uri.parse("file://" + destination);

                        File file = new File(destination);
                        if (file.exists()) file.delete();

                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                        //request.setDescription("lolol");
                        request.setTitle(context.getString(R.string.toast_downloading)+" "+fileName);

                        //set destination
                        request.setDestinationUri(uri);

                        // get download service and enqueue file
                        final DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                        final long downloadId = manager.enqueue(request);

						/* //Not needed, since it's handled now in UsbBroadcastReceiver
						//set BroadcastReceiver to install app when .apk is downloaded
						BroadcastReceiver onComplete = new BroadcastReceiver() {
							public void onReceive(Context ctxt, Intent intent) {
								Intent install = new Intent(Intent.ACTION_VIEW);
								install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								Bundle bundle = intent.getExtras();
								Set set = bundle.keySet();
								String id="";
								for( Object s: set) {
									String key = s.toString();
									id += " " + key + " => " + bundle.get(key) + "\n";
								}
								new AlertDialog.Builder(MainActivity.this)
										.setMessage(id)
										.show();
								//install.setDataAndType(uri, manager.getMimeTypeForDownloadedFile(downloadId)); //treats as a zip, lol
								install.setDataAndType(uri, "application/vnd.android.package-archive");
								//referred here: http://stackoverflow.com/questions/20065040/download-installing-and-delete-apk-file-on-android-device-programmatically-fro
								//startActivity(install);
								unregisterReceiver(this);
								//finish();
							}
						};
						//register receiver for when .apk download is compete
						registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));*/
                    } catch (final Exception ex) {
                        ex.printStackTrace();
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(XdaThreadURL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                }
            }.start();

    }

    static void showChangelog(String changelog, final Activity activity) {
        final Context context = activity.getApplicationContext();
        AlertDialog.Builder logs = new AlertDialog.Builder(activity);
        logs.setTitle(context.getString(R.string.action_show_changelog));
        logs.setMessage(changelog.replaceAll("<br>","\n"));
        logs.setNegativeButton(context.getString(R.string.action_later), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {	}
        });
        logs.setPositiveButton(context.getString(R.string.action_download), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                downloadAndInstallApk(downloadURL, context);
            }
        });
        logs.create();
        logs.show();
    }


}
