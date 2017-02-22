package com.gokulnc.ums_universal;

class Constants {

    //Following strings correspond to SharedPref keys
    public static final String MyPREFERENCES = "Settings" ; //Name of SharedPref
    public static final String firstRun = "isFirstRun";
    public static final String currentVersionCode = "currentVersionCode";
    public static final String LUNsToUse = "LUNsToUse";
    public static final String LUNsFound = "LUNsFound";
    public static final String defaultBlockNumber = "defaultBlockNumber";
    public static final String blocksList = "blocksList";
    public static final String luns = "luns";
    public static final String ADBenable = "ADBenable";
    public static final String NotifsEnable = "NotificationsEnabled";
    public static final String blockRecommendations = "blockRecommendations";
    public static final String autoStart = "autoStart";
    public static final String autoUpdate = "autoUpdateCheck";
    public static final String mediaScanEnable = "MediaScanEnabled";
    public static final String mediaScanPath = "mediaScanPath";
    public static final String enableUMScmds = "enableUMScommands";
    public static final String disableUMScmds = "disableUMScommands";
    public static final String setPermissionCmds = "setPermissionCommands";
    public static final String widgetEnabled = "isWidgetEnabled";
    public static final String isUmsDisabled = "isUmsDisabled";

    //public static final String chargingMode = "setprop sys.usb.config charging";
    public static final String enableADBmode = "setprop persist.service.adb.enable 1\n"
            +"setprop service.adb.enable 1";
    public static final String ADBonlyMode = "setprop sys.usb.config adb";
    public static final String MTPprop = "setprop sys.usb.config mtp";
    //public static final String disconnectUSB = "setprop sys.usb.config none";

    public static final String CDprop = "setprop service.cdrom.enable 1";
    public static final String UMSprop="setprop sys.usb.config mass_storage";
    public static final String UMSconfig = "echo 0 > /sys/class/android_usb/android0/enable\n"
            +"echo 12d1 > /sys/class/android_usb/android0/idVendor\n"
            +"echo 1037 > /sys/class/android_usb/android0/idProduct\n"
            +"echo mass_storage > /sys/class/android_usb/android0/functions\n"
            +"echo 1 > /sys/class/android_usb/android0/enable";
    //Not at all necessary, except for old kernels:
    public static final String UMSlegacy = "echo 239 > /sys/class/android_usb/android0/bDeviceClass\n"
            +"echo 2 > /sys/class/android_usb/android0/bDeviceSubClass\n"
            +"echo 1 > /sys/class/android_usb/android0/bDeviceProtocol";
    //To revert from UMSlegacy properties:
    public static final String defaultDeviceProp = "echo 0 > /sys/class/android_usb/android0/bDeviceClass\n"
            +"echo 0 > /sys/class/android_usb/android0/bDeviceSubClass\n"
            +"echo 0 > /sys/class/android_usb/android0/bDeviceProtocol";

    public static final String memoryCardBlock = "/dev/block/mmcblk1";

    public static final String LOG_TAG = "UMSenabler";

    public static final String XdaThreadURL = "http://forum.xda-developers.com/android/apps-games/app-universal-mass-storage-enabler-beta-t3240097";

}
