package com.gokulnc.ums_universal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.os.EnvironmentCompat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.gokulnc.ums_universal.MainActivity.memoryCardBlock;

class FirstTimeSetup {

    static final String LUNlist[] = { //all these are just symlinks
            "/sys/class/android_usb/android0/f_mass_storage/lun",
            "/sys/class/android_usb/android0/f_mass_storage/lun0",
            "/sys/class/android_usb/android0/f_mass_storage/lun1",
            "/sys/class/android_usb/android0/f_mass_storage/lun2",
            "/sys/class/android_usb/android0/f_mass_storage/lun_ex",
            "/sys/class/android_usb/android1/f_mass_storage/lun_ex", //<- for some stupid kernel I saw somewhere
            "/sys/class/android_usb/android0/f_mass_storage/lun_cd",
            "/sys/class/android_usb/android0/f_mass_storage/rom",
            "/sys/class/android_usb/android0/f_mass_storage/cdrom",
            "/sys/class/android_usb/android0/f_mass_storage/usbdisk",
            "/sys/class/android_usb/android0/f_mass_storage/uicc0",
            "/sys/class/android_usb/android0/f_mass_storage/uicc1",
            "/sys/class/android_usb/android0/f_mass_storage/usbdisk",
            "/sys/class/android_usb/android0/f_cdrom_storage/lun" //for very old devices

            //Below are direct LUN paths for some device kernels, which I've generally seen
            //I don't use these now, since the searchLUNs() does this automatically
			/* "/sys/devices/platform/mt_usb/gadget/lun#/file"
			"/sys/devices/platform/mt_usb/musb-hdrc.0/gadget/lun%d/file"
			"/sys/devices/platform/s3c-usbgadget/gadget/lun#/file"
			"/sys/devices/platform/usb_mass_storage/lun#/file"
			"/sys/devices/platform/omap/musb-omap2430/musb-hdrc/gadget/lun#/file"
			"/sys/devices/platform/fsl-tegra-udc/gadget/lun#/file"
			"/sys/devices/platform/msm_hsusb/gadget/lun#/file" */
    };

    static ArrayList<String> availableLUNs = MainActivity.availableLUNs;
    static ArrayList<String> blockDeviceFiles = MainActivity.blockDeviceFiles;
    static boolean busyboxPresent = true;
    static SharedPreferences data;

    public static boolean firstRun(Activity activity, RootShell root) throws IOException, PackageManager.NameNotFoundException {

        Context context = activity.getApplicationContext();

        //TODO: Update the widget: http://stackoverflow.com/a/4412949/5002496
        data = context.getSharedPreferences(MainActivity.MyPREFERENCES, Context.MODE_PRIVATE);

		/* To enable Mediascan after unmounting, we need to explicitly specify
		 the path of the external memory card */
        if(!MainActivity.listAllLUNsAndBlocks) { //To not change the path if already selected by user, while list All is invoked by user
            String externalSDs[] = FirstTimeSetup.getExternalStorageDirectories(context, root);
            if (externalSDs != null && externalSDs.length != 0) {
                if (externalSDs.length == 1) {
                    MainActivity.memoryCardPath = externalSDs[0];
                    data.edit().putString(MainActivity.mediaScanPath, MainActivity.memoryCardPath).apply();
                    MainActivity.enableMediaScan = true;
                    data.edit().putBoolean(MainActivity.mediaScanEnable, MainActivity.enableMediaScan).apply();
                } else {
                    //unable to find which one is the external memory card
                    Log.d(MainActivity.LOG_TAG, "More than 1 path found for extSDcard");
                }
            } else {
                Log.d(MainActivity.LOG_TAG, "Unable to find path of extSDcard");
            }
        }
        //Check if 'find' binary exists; if not, ask to install busybox
        String output = root.execute("type find");
        if(output == null || output.trim().isEmpty() || output.trim().toLowerCase().contains("not found")) {
            busyboxPresent = false;
            Log.d(MainActivity.LOG_TAG, "'find' binary not found.");
        } else busyboxPresent = true;

        //Enumerate all the available LUNs and save it
        checkLUNs(activity, context, root);
        if(availableLUNs.size() > 0) {
            Log.d(MainActivity.LOG_TAG, "Found "+availableLUNs.size()+" LUNs available to use.");
            String targetLUNs = "";
            for(String str : availableLUNs) {
                if(!targetLUNs.equals("")) targetLUNs += "\n";
                targetLUNs += str;
            }
            data.edit().putString(MainActivity.LUNsFound, targetLUNs).apply();
            data.edit().putInt(MainActivity.luns, availableLUNs.size()).apply();
        }

        if( !(availableLUNs.isEmpty()) ) {
            data.edit().putString(MainActivity.LUNsToUse, "0").apply();
        }

        //Enumerate all possible mountable partition blocks and save it
        checkExtSD(activity, context, root);
        if(blockDeviceFiles.size() > 0) {
            
            Log.d(MainActivity.LOG_TAG, "Found "+blockDeviceFiles.size()+" blocks available to use.");
            String targetBlocks = "";
            for(String str : blockDeviceFiles) {
                if(!targetBlocks.equals("")) targetBlocks += "\n";
                targetBlocks += str;
            }
            data.edit().putString(MainActivity.blocksList, targetBlocks).apply();
        }

        if( !(blockDeviceFiles.isEmpty()) && !(availableLUNs.isEmpty())) {
            data.edit().putBoolean(MainActivity.firstRun, false).apply();
            MainActivity.currentVersionNumber = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
            data.edit().putInt(MainActivity.currentVersionCode, MainActivity.currentVersionNumber).apply();
            Log.d(MainActivity.LOG_TAG,"isFirstRun set to false");
        } else {
            data.edit().putBoolean(MainActivity.firstRun, true).apply();
            Log.d(MainActivity.LOG_TAG, "firstRun() will be executed next time also.");
        }

        boolean canAppWork = !blockDeviceFiles.isEmpty() && !availableLUNs.isEmpty();
        blockDeviceFiles.clear();
        availableLUNs.clear();
        if(!busyboxPresent) busyboxNotFound(activity);
        Log.d(MainActivity.LOG_TAG, "Finished firstRun() successfully");
        return canAppWork;
    }

    @Nullable
    static String[] getExternalStorageDirectories(Context context, RootShell root) {
        // http://stackoverflow.com/a/39372019/5002496
        String [] storageDirectories;
        List<String> results = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] externalDirs = context.getExternalFilesDirs(null);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (File file : externalDirs) {
                    String path = file.getPath().split("/Android")[0];
                    if (Environment.isExternalStorageRemovable(file)) {
                        results.add(path);
                    }
                }
            } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                for (File file : externalDirs) {
                    String path = file.getPath().split("/Android")[0];
                    if (Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(file))) {
                        results.add(path);
                    }
                }
            }
        }
        if(results.isEmpty()){
			/* TODO: implement this: http://stackoverflow.com/a/40123073/5002496 */
            if(MainActivity.rootAccess) {
                String output = null;
                try {
                    output = root.execute("mount | grep /dev/block/vold");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(output == null || output.trim().isEmpty()) {
                    //Log.d(MainActivity.LOG_TAG, "No vold blocks found from mount command..");
                } else {
                    Log.d(MainActivity.LOG_TAG, "WTF..\n"+output);
                    String devicePoints[] = output.split("\n");
                    for(String voldPoint: devicePoints) {
                        results.add(voldPoint.split(" ")[2]);
                    }
                }
            }
        }

        //Remove paths which may not be external memory card, like OTG (feel free to comment them out)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < results.size(); i++) {
                if (!results.get(i).toLowerCase().matches(".*[0-9a-f]{4}[-][0-9a-f]{4}")) {
                    Log.d(MainActivity.LOG_TAG, results.get(i) + " might not be extSDcard");
                    results.remove(i--);
                }
            }
        } else {
            for (int i = 0; i < results.size(); i++) {
                if (!results.get(i).toLowerCase().contains("ext") && !results.get(i).toLowerCase().contains("sdcard")) {
                    Log.d(MainActivity.LOG_TAG, results.get(i)+" might not be extSDcard");
                    results.remove(i--);
                }
            }
        }

        storageDirectories = new String[results.size()];
        for(int i=0; i<results.size(); ++i) storageDirectories[i] = results.get(i);

        return storageDirectories;
    }

    static void checkLUNs(final Activity  activity, final Context context, RootShell root) throws IOException {

        searchLUNs(root);

        if(availableLUNs.isEmpty()) {
            Log.d(MainActivity.LOG_TAG, "No usable LUNs found.");

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder fail = new AlertDialog.Builder(activity);
                    fail.setTitle(context.getString(R.string.alert_no_gadget));
                    fail.setMessage(context.getString(R.string.alert_no_gadget_msg));
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
            return;
        }

        String output;
        String unsupportedLUNs = "";

        for(String lun: availableLUNs) {
            //executeAsSU("echo 0 > "+lun+"/cdrom"); //write first and see if value doesn't change
            output = root.execute("cat "+lun+"/cdrom");
            if(output!=null && output.equals("1")) unsupportedLUNs += "\n\n"+lun;
        }

        if(!unsupportedLUNs.isEmpty()) {
            final String finalUnsupportedLUNs = unsupportedLUNs; //a final copy
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder warn = new AlertDialog.Builder(activity);
                    warn.setTitle(context.getString(R.string.alert_warning));
                    warn.setMessage(context.getString(R.string.alert_unsupportedLUNs)+ finalUnsupportedLUNs);
                    warn.setPositiveButton(context.getString(R.string.action_ok),new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) { }
                    });
                    warn.create();
                    warn.show();
                }
            });
            return;
        }
    }

    static boolean searchLUNs(RootShell root) throws IOException {

        availableLUNs.clear();
        String output;
        if(busyboxPresent) {

            output = root.execute("find /sys/devices -name file -type f");
            if (output != null && !output.trim().isEmpty()) {
                String newLUNs[] = output.split("\n");
                for(String LUN: newLUNs) {
                    if( LUN.contains("gadget") || LUN.contains("lun") ) availableLUNs.add(LUN.replace("/file", ""));
                }
            }
        }

        if((availableLUNs.isEmpty()) || MainActivity.listAllLUNsAndBlocks) { //If user doesn't have busybox
            for(String str:LUNlist) {

                output = root.execute("ls " + str + "/file");
                if( output != null && output.trim().equals(str+"/file") ) {
                    availableLUNs.add(str);
                }
            }
        }

        return !availableLUNs.isEmpty();

    }

    static void checkExtSD(final Activity  activity, final Context context, RootShell root) throws IOException {

        boolean extSDpresent = listDeviceBlocks(root);

        if(extSDpresent && blockDeviceFiles.size() > 0) {
            data.edit().putInt(MainActivity.defaultBlockNumber, 0).apply();
            Log.d(MainActivity.LOG_TAG, "Selecting "+blockDeviceFiles.get(0)+" as default block.");
        } else if(!extSDpresent && blockDeviceFiles.size() > 0) {

            Log.d(MainActivity.LOG_TAG, "Default /dev/block/mmcblk1* blocks not found, but vold blocks found.");
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder alternate = new AlertDialog.Builder(activity);
                    alternate.setTitle("Alternative Partition Block Found");
                    String suggestion = "Do you want to try using the vold block device: "+blockDeviceFiles.get(0)+" instead?\n\nProceed at your own risk.\nExit if you don't understand :)";
                    alternate.setMessage(suggestion);
                    alternate.setNegativeButton("Exit",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            Log.d(MainActivity.LOG_TAG, "User denied using vold block.");
                            activity.finish();

                        }
                    });

                    alternate.setPositiveButton(context.getString(R.string.action_ok),new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            int blockNumber = (blockDeviceFiles.size() > 1)? 1 : 0 ;
                            Log.d(MainActivity.LOG_TAG, "Selecting "+blockDeviceFiles.get(blockNumber)+" as default block.");
                            data.edit().putInt(MainActivity.defaultBlockNumber, blockNumber).apply();
                        }
                    });

                    alternate.setCancelable(false);
                    alternate.create();
                    alternate.show();


                    AlertDialog.Builder fail = new AlertDialog.Builder(activity);
                    fail.setTitle(context.getString(R.string.alert_mmc_not_found));
                    fail.setMessage(context.getString(R.string.alert_mmc_not_found_msg));
                    fail.setPositiveButton(context.getString(R.string.action_ok),new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {

                        }
                    });
                    fail.setCancelable(false);
                    fail.create();
                    fail.show();
                }
            });


        } else {
            Log.d(MainActivity.LOG_TAG, "No usable device blocks found.");
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder fail = new AlertDialog.Builder(activity);
                    fail.setTitle(context.getString(R.string.alert_no_support));
                    fail.setMessage(context.getString(R.string.alert_no_blocks));
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

    static boolean listDeviceBlocks(RootShell root) throws IOException {

        blockDeviceFiles.clear();
        String output;
        if(busyboxPresent) {
            Boolean mmcblk1 = false;
            output = root.execute("find /dev/block/ -name mmcblk1*");
            if (output != null && !output.trim().isEmpty()) {
                String devicePoints[] = output.split("\n");
                for(String block: devicePoints) {
                    if(block.equals(memoryCardBlock)){
                        mmcblk1 = true;
                    }else if(block.contains(memoryCardBlock)) {
                        blockDeviceFiles.add(block.trim());
                    }
                }
                if((blockDeviceFiles.isEmpty() && mmcblk1) || MainActivity.listAllLUNsAndBlocks) blockDeviceFiles.add(memoryCardBlock); //Never true
            }
        } else {
            for(int i=1; i<=3; i++) {
                output = root.execute("ls "+memoryCardBlock+"p"+i);
                if ( output!=null && output.equals(memoryCardBlock+"p"+i) ) {
                    blockDeviceFiles.add(memoryCardBlock+"p"+i);
                }
            }
            if(blockDeviceFiles.isEmpty() || MainActivity.listAllLUNsAndBlocks) {
                output = root.execute("ls "+memoryCardBlock);
                if ( output!=null && output.equals(memoryCardBlock) ) {
                    blockDeviceFiles.add(memoryCardBlock);
                }
            }
        }

        int defaults = blockDeviceFiles.size();
        boolean extSDpresent = false;
        if(defaults > 0) {
            extSDpresent = true;
        }
        Boolean foundInMount = false;
        if(defaults==0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || MainActivity.listAllLUNsAndBlocks) { //since Internal Storage can be mounted as UMS before KK

            output = root.execute("mount | grep /dev/block/vold");
            if(output == null || output.trim().isEmpty()) {
                Log.d(MainActivity.LOG_TAG, "No vold blocks found from mount command..");
            } else {
                String devicePoints[] = output.split("\n");
                for(String voldPoint: devicePoints) {
                    voldPoint = voldPoint.substring(0, voldPoint.indexOf(" "));
                    if(voldPoint.contains("/dev/block/vold") && !blockDeviceFiles.contains(voldPoint)) blockDeviceFiles.add(voldPoint);
                }
                if( blockDeviceFiles.size() > defaults ) foundInMount = true;
            }
            if(blockDeviceFiles.size() == 0 || ((Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || MainActivity.listAllLUNsAndBlocks)&&!foundInMount)) {
                output = root.execute("ls /dev/block/vold/");
                if(output == null || output.trim().isEmpty()) {
                    Log.d(MainActivity.LOG_TAG, "No vold blocks found from /dev/block/vold");
                } else {
                    String devicePoints[] = output.split("\n");
                    for(String voldPoint: devicePoints) {
                        blockDeviceFiles.add("/dev/block/vold/"+voldPoint);
                    }
                }
            }
        }

        if((defaults > 0 || foundInMount) && !MainActivity.listAllLUNsAndBlocks) {
            MainActivity.blockRecommendation = false;
            data.edit().putBoolean(MainActivity.blockRecommendations, false).apply();
        } else {
            MainActivity.blockRecommendation = true;
            data.edit().putBoolean(MainActivity.blockRecommendations, true).apply();
        }

        return extSDpresent;
    }

    static void busyboxNotFound(final Activity activity) {

        final Context context = activity.getApplicationContext();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder fail = new AlertDialog.Builder(activity);
                fail.setTitle(context.getString(R.string.alert_no_busybox));
                fail.setMessage(context.getString(R.string.alert_no_busybox_msg));
                fail.setNegativeButton(context.getString(R.string.action_later), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {	}
                });

                fail.setPositiveButton(context.getString(R.string.action_install_busybox),new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        try {
                            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=stericson.busybox")));
                        } catch (android.content.ActivityNotFoundException e) {
                            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=stericson.busybox")));
                        }
                    }
                });

                fail.setCancelable(false);
                fail.create();
                fail.show();
            }
        });

    }

}
