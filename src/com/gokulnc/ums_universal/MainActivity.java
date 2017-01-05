package com.gokulnc.ums_universal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.os.EnvironmentCompat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootShell.execution.Shell;
import com.stericson.RootTools.RootTools;
import com.mikepenz.materialdrawer.Drawer;


public class MainActivity extends AppCompatActivity {

	public static SharedPreferences data;
	public static final String MyPREFERENCES = "Settings" ;
	public static Boolean extSDpresent = false;
	Boolean isFirstRun = false;
	public static final String extSDpresence = "extSDpresence";
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

	int numberOfLUNsFound = 1;
	public static int numberOfLUNsUsed = 1;
	
	static Boolean[] enabledLUNs;
	static Boolean[] recommendedBlocks;

	public static boolean isAppOpen = false;
	static Boolean SELinux = true;
	static Boolean requiresUnmount = false;
	static Boolean busyboxPresent = true;
	static Boolean rootAccess = false;
	static Boolean USBconnected = false;
	static boolean enableADB = false;
	static boolean enableNotifications = true;
	static boolean enableMediaScan = false;
	static boolean enableADs = true;
	static Boolean blockRecommendation = true;
	static boolean listAllLUNsAndBlocks = false;
	String USBconfig = "mtp";
	static int currentVersionNumber;
	static String currentVersion;
	
	public static Shell rootShell;
	
	RootShell root = new RootShell();
	
	Button mtp, unmount;
	ImageButton ums;
	TextView USBstatus, USBmode;

	BroadcastReceiver mUsbReceiver;
	
	public static final String LUNlist[] = { //all these are just symlinks
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

	static ArrayList<String> availableLUNs = new ArrayList<>();
	static ArrayList<String> blockDeviceFiles = new ArrayList<>();

	String clearLUN = "";
	String enableMSG = "";
	
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
	static String blockDevice = "";
	static String memoryCardPath = "";
	public static int blockNumber = 0;
	public static final String LOG_TAG = "UMSenabler";

	public static final String XdaThreadURL = "http://forum.xda-developers.com/android/apps-games/app-universal-mass-storage-enabler-beta-t3240097";

	InterstitialAd mInterstitialAd;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		//active = true;
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);  // Fixed Portrait orientation
		setContentView(R.layout.activity_empty);
		//((RelativeLayout) findViewById(R.id.empty_container)).addView(new ProgressBar(this));


		new Thread() {
			@Override
			public void run() {
				try {

					getRootShell();

					data = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
					extSDpresent = data.getBoolean(extSDpresence, false);
					isFirstRun = data.getBoolean(firstRun, true);
					if(isFirstRun) Log.d(LOG_TAG, "Running app for first time");
					blockRecommendation = data.getBoolean(blockRecommendations, true);
					currentVersionNumber = data.getInt(currentVersionCode, 0);
					try {
						currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
						if(currentVersionNumber < getPackageManager().getPackageInfo(getPackageName(), 0).versionCode) {
							isFirstRun = true;
							Log.d(LOG_TAG, "Possible Update from "+currentVersionNumber+" to "+MainActivity.this.getPackageManager().getPackageInfo(MainActivity.this.getPackageName(), 0).versionCode);
							currentVersionNumber = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
						}
					} catch (NameNotFoundException e1) {
						Log.d(LOG_TAG, "Version Read Error!");
						e1.printStackTrace();
					}

					if(RootTools.isAccessGiven()) {

						Log.d(LOG_TAG,"Obtained Root Access..");
						Log.d(LOG_TAG,"Obtaining RootShell..");
						try {
							rootAccess = root.getNewShell();
						} catch (IOException e1) {
							rootAccess = false;
							e1.printStackTrace();
						}
						if(!rootAccess) {
							Log.d(LOG_TAG, "Unable to obtain su RootShell.");
							noRootAccess(getString(R.string.error_unable_shell));
						} else {
							Log.d(LOG_TAG, "Obtained RootShell..");
							Log.d(LOG_TAG, "Checking SE Linux Status");
							try {
								checkSELinux();
							} catch (IOException e) {
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										Toast.makeText(getApplicationContext(), getString(R.string.error_selinux), Toast.LENGTH_SHORT).show();
									}
								});
								Log.d(LOG_TAG, "Unable to check SE Linux Status!");
								e.printStackTrace();
							}

						}

					} else noRootAccess(getString(R.string.error_root_not_granted));
					//Further path of execution is from continueExec() (which is called from checkSELinux() method)

					/*MainActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// code runs in a UI(main) thread
						}
					});*/
				} catch (final Exception ex) {
					//Toast.makeText(getApplicationContext(), "Sumting wong", Toast.LENGTH_SHORT).show();
					ex.printStackTrace();
				}
			}
		}.start();

	}

	public void onStart() {
		super.onStart();
		//active = true;
		Log.d(LOG_TAG, "onStart() initiated..");
	}

	void setBroadcastReceivers() {
		//Old Reference: http://www.codepool.biz/how-to-monitor-usb-events-on-android.html (Doesn't Help)
		if(mUsbReceiver==null) {
			mUsbReceiver = new BroadcastReceiver() {
				public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();
					Log.d(LOG_TAG, "Received Broadcast: " + action);
					if (action.equalsIgnoreCase("android.hardware.usb.action.USB_STATE") && isAppOpen && rootAccess) {
						USBconnected = intent.getExtras().getBoolean("connected");
						updateUSBstatus();
						//updateUSBconfig();
					}
				}
			};

			IntentFilter filter = new IntentFilter("android.hardware.usb.action.USB_STATE");
			//filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
			//filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
			//filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
			//filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
			registerReceiver(mUsbReceiver, filter);
			Log.d(LOG_TAG, "mUsbReceiver Registered");
		}
	}

	void initADs() {

		//https://firebase.google.com/docs/admob/android/quick-start
		MobileAds.initialize(getApplicationContext(), getString(R.string.ad_app_id));
		AdView mAdView = (AdView) findViewById(R.id.adView);
		AdRequest adRequest = new AdRequest.Builder().build();
		mAdView.loadAd(adRequest);
		Log.d(LOG_TAG, "Ads initialized..");

		mInterstitialAd = new InterstitialAd(this);
		mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
		mInterstitialAd.setAdListener(new AdListener() {
			@Override
			public void onAdClosed() {
				//requestNewInterstitial();
			}
		});

		requestNewInterstitial();
	}

	void requestNewInterstitial() {
		if(enableADs) {
			AdRequest adRequest = new AdRequest.Builder().build();
			mInterstitialAd.loadAd(adRequest);
		}
	}

	void continueExec() {

		Log.d(LOG_TAG, "Initiating continueExec()");

		enableADB = data.getBoolean(ADBenable, false);
		enableNotifications = data.getBoolean(NotifsEnable, true);
		enableMediaScan = data.getBoolean(mediaScanEnable, false);
		memoryCardPath = data.getString(mediaScanPath, "");

		//Auto updates check
		if(data.getBoolean(autoUpdate, true)) AppUpdate.checkForUpdates(true, FirebaseRemoteConfig.getInstance(), MainActivity.this);

    	if(isFirstRun) {
			try {
				Log.d(LOG_TAG, "Initiating firstRun()");
				if(!firstRun()) return;
			} catch (Exception e) {
				e.printStackTrace();
				Log.d(LOG_TAG, "Problem in firstRun()");
			}
		}

		// To get all the blocks from SharedPref and parse it
		blockDeviceFiles.clear();
		String blocks = data.getString(blocksList, null);
		if( blocks!=null && !(blocks.isEmpty()) ) {
			String[] temp = blocks.split("\n");
			/*for(String str : temp) {
    			blockDeviceFiles.add(str);
    		}*/
			blockDeviceFiles.addAll(Arrays.asList(temp));
		}
		blockNumber = data.getInt(defaultBlockNumber , 0);
		if(!blockDeviceFiles.isEmpty()) blockDevice = blockDeviceFiles.get(blockNumber);
		Log.d(LOG_TAG, "Using "+blockDevice+" as default block");

		// To recommend users which block to use
		recommendedBlocks = new Boolean[blockDeviceFiles.size()];
		if(blockRecommendation) {
			String s;
			int prev=-1, curr, last_vold_block_i=-1, vold_block_count=0;
			for(int i=0; i < blockDeviceFiles.size(); i++) {
				s = blockDeviceFiles.get(i);
				if(s.contains(memoryCardBlock)) {
					recommendedBlocks[i] = s.contains(memoryCardBlock + "p");
				} else {
					curr = Integer.parseInt(s.replaceAll("[^0-9]", ""));
					recommendedBlocks[i] = (curr == prev + 1 && prev != -1);
					prev = curr; last_vold_block_i = i; ++vold_block_count;
				}
				if(blockDevice.equals(s) && !recommendedBlocks[i]) requiresUnmount = true;
			}
			if(vold_block_count==1) { //This might mean vold block was obtained from 'mount' cmd; hence might be recommended one
				recommendedBlocks[last_vold_block_i] = true;
				if(blockDevice.contains("vold")) requiresUnmount = false;
			}
		} else { //why did I write this?
			for(int i=0; i<recommendedBlocks.length; i++){
				recommendedBlocks[i] = blockDeviceFiles.get(i).contains(memoryCardBlock + "p");
			}
		}

		// To get all the LUNs from SharedPref and parse it
		String LUNsFoundList = data.getString(LUNsFound,null);
		numberOfLUNsFound = data.getInt(luns, 1);
		enabledLUNs = new Boolean[numberOfLUNsFound];
		for(int i=0; i < enabledLUNs.length; i++) enabledLUNs[i] = false;

		numberOfLUNsUsed = 0;
		availableLUNs.clear();
		String LUNsToBeUsed = data.getString(LUNsToUse, "0");
		if( LUNsFoundList !=null && !(LUNsFoundList.isEmpty()) ) {
			String temp[] = LUNsFoundList.split("\n");
			String LUNnumbers[] = LUNsToBeUsed.split(",");
			/*for(String str: temp) {
    			availableLUNs.add(str);
    		}*/
			availableLUNs.addAll(Arrays.asList(temp));
			if( !(availableLUNs.isEmpty()) && !(LUNsToBeUsed.isEmpty()) ) {
				for(String str: LUNnumbers) {
					++numberOfLUNsUsed;
					try {
						enabledLUNs[java.lang.Integer.parseInt(str)] = true;
					}catch(Exception e) { //For parsing errors incase
						e.printStackTrace();
					}
				}
			}
		}


		if(requiresUnmount) showUnmountWarning();
		if(numberOfLUNsUsed > 1) multipleLUNsWarning();

		if( !(availableLUNs.isEmpty()) && blockDevice != null && !(blockDevice.isEmpty()) && numberOfLUNsUsed!=0 ) {
			buildCommands();
			setPermissions(true);
		} else {
			Log.d(LOG_TAG, "Unable to build commands.");
			Log.d(LOG_TAG, "No. of LUNs found & selected: "+availableLUNs.size() + " & "+ numberOfLUNsUsed);
			Log.d(LOG_TAG, "Partition Block selected: "+blockDevice);
			noSupport();
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setUI();
				setBroadcastReceivers(); //WARNING: this should be after setUI only
			}
		});

	}

	boolean firstRun() throws IOException, NameNotFoundException {

		//TODO: Update the widget: http://stackoverflow.com/a/4412949/5002496

		/* To enable Mediascan after unmounting, we need to explicitly specify
		 the path of the external memory card */
		if(!listAllLUNsAndBlocks) { //To not change the path if already selected by user
			String externalSDs[] = getExternalStorageDirectories();
			if (externalSDs != null && externalSDs.length != 0) {
				if (externalSDs.length == 1) {
					memoryCardPath = externalSDs[0];
					data.edit().putString(mediaScanPath, memoryCardPath).apply();
					enableMediaScan = true;
					data.edit().putBoolean(mediaScanEnable, enableMediaScan).apply();
				} else {
					//unable to find which one is the external memory card
					Log.d(LOG_TAG, "More than 1 path found for extSDcard");
				}
			} else {
				Log.d(LOG_TAG, "Unable to find path of extSDcard");
			}
		}
		//Check if 'find' binary exists; if not, ask to install busybox
		String output = root.execute("type find");
		if(output == null || output.trim().isEmpty() || output.trim().toLowerCase().contains("not found")) {
			busyboxPresent = false;
			Log.d(LOG_TAG, "'find' binary not found.");
		} else busyboxPresent = true;

		//Enumerate all the available LUNs and save it
		checkLUNs();
		if(availableLUNs.size() > 0) {
			Log.d(LOG_TAG, "Found "+availableLUNs.size()+" LUNs available to use.");
			String targetLUNs = "";
			for(String str : availableLUNs) {
				if(!targetLUNs.equals("")) targetLUNs += "\n";
				targetLUNs += str;
			}
			data.edit().putString(LUNsFound, targetLUNs).apply();
			data.edit().putInt(luns, availableLUNs.size()).apply();
		}

		//Enumerate all possible mountable partition blocks and save it
		checkExtSD();
		if(blockDeviceFiles.size() > 0) {
			Log.d(LOG_TAG, "Found "+blockDeviceFiles.size()+" blocks available to use.");
			String targetBlocks = "";
			for(String str : blockDeviceFiles) {
				if(!targetBlocks.equals("")) targetBlocks += "\n";
				targetBlocks += str;
			}
			data.edit().putString(blocksList, targetBlocks).apply();
		}

		if( !(blockDeviceFiles.isEmpty()) && !(availableLUNs.isEmpty())) {
			data.edit().putBoolean(firstRun, false).apply();
			isFirstRun = false;
			currentVersionNumber = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
			data.edit().putInt(currentVersionCode, currentVersionNumber).apply();
			Log.d(LOG_TAG,"isFirstRun set to false");
		} else {
			data.edit().putBoolean(firstRun, true).apply();
			Log.d(LOG_TAG, "firstRun() will be executed next time also.");
		}

		boolean canAppWork = !blockDeviceFiles.isEmpty() && !availableLUNs.isEmpty();
		blockDeviceFiles.clear();
		availableLUNs.clear();
		if(!busyboxPresent) busyboxNotFound();
		Log.d(LOG_TAG, "Finished firstRun() successfully");
		return canAppWork;
	}

	void checkSELinux() throws IOException {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
			SELinux = false;
			continueExec();
			return;
		}

		String output = root.execute("getenforce");

		if ( output==null || !output.equals("Permissive") ) {
			SELinux = true;
			Log.d(LOG_TAG, "SE Linux found to be enforcing.");
		}
		else{
			SELinux = false;
			Log.d(LOG_TAG, "SE Linux Enforcement is already disabled.");
		}

		if(SELinux) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					AlertDialog.Builder fail = new AlertDialog.Builder(MainActivity.this);
					fail.setTitle(getString(R.string.alert_perm_denied));

					fail.setMessage(getString(R.string.alert_selinux));
					fail.setNegativeButton(getString(R.string.action_exit) ,new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							Log.d(LOG_TAG, "User denied to disable SE Linux Enforcement.");
							finish();
						}
					});

					fail.setPositiveButton(getString(R.string.action_disable_selinux),new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							executeAsSU("setenforce 0");
							SELinux = false;
							Log.d(LOG_TAG, "User disabled SE Linux Enforcement.");
							new Thread() {
								@Override
								public void run() {
									try {
										continueExec();
									} catch (final Exception ex) {
										ex.printStackTrace();
									}
								}
							}.start();
						}
					});
					fail.setCancelable(false);
					fail.create();
					fail.show();
				}
			});

		} else continueExec();

	}

	void checkLUNs() throws IOException {

		searchLUNs();

		if(availableLUNs.isEmpty()) {
			Log.d(LOG_TAG, "No usable LUNs found.");

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					AlertDialog.Builder fail = new AlertDialog.Builder(MainActivity.this);
					fail.setTitle(getString(R.string.alert_no_gadget));
					fail.setMessage(getString(R.string.alert_no_gadget_msg));
					fail.setPositiveButton(getString(R.string.action_ok),new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							finish();
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
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					AlertDialog.Builder warn = new AlertDialog.Builder(MainActivity.this);
					warn.setTitle(getString(R.string.alert_warning));
					warn.setMessage(getString(R.string.alert_unsupportedLUNs)+ finalUnsupportedLUNs);
					warn.setPositiveButton(getString(R.string.action_ok),new DialogInterface.OnClickListener() {
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

	boolean searchLUNs() throws IOException {

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

		if((availableLUNs.isEmpty()) || listAllLUNsAndBlocks) { //If user doesn't have busybox
			for(String str:LUNlist) {

				output = root.execute("ls " + str + "/file");
				if( output != null && output.trim().equals(str+"/file") ) {
					availableLUNs.add(str);
				}
			}
		}

		if( !(availableLUNs.isEmpty()) ) {
			data.edit().putString(LUNsToUse, "0").apply();
		}

		return !availableLUNs.isEmpty();

	}

	void checkExtSD() throws IOException {

		listDeviceBlocks();

		if(extSDpresent && blockDeviceFiles.size() > 0) {

			blockNumber = 0;
			data.edit().putInt(defaultBlockNumber, blockNumber).apply();
			Log.d(LOG_TAG, "Selecting "+blockDeviceFiles.get(blockNumber)+" as default block.");

		} else if(!extSDpresent && blockDeviceFiles.size() > 0) {

			Log.d(LOG_TAG, "Default /dev/block/mmcblk1* blocks not found, but vold blocks found.");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					AlertDialog.Builder alternate = new AlertDialog.Builder(MainActivity.this);
					alternate.setTitle("Alternative Partition Block Found");
					String suggestion = "Do you want to try using the vold block device: "+blockDeviceFiles.get(0)+" instead?\n\nProceed at your own risk.\nExit if you don't understand :)";
					alternate.setMessage(suggestion);
					alternate.setNegativeButton("Exit",new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							Log.d(LOG_TAG, "User denied using vold block.");
							finish();

						}
					});

					alternate.setPositiveButton(getString(R.string.action_ok),new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							blockNumber = (blockDeviceFiles.size() > 1)? 1 : 0 ;
							Log.d(LOG_TAG, "Selecting "+blockDeviceFiles.get(blockNumber)+" as default block.");
							data.edit().putInt(defaultBlockNumber, blockNumber).apply();
						}
					});

					alternate.setCancelable(false);
					alternate.create();
					alternate.show();


					AlertDialog.Builder fail = new AlertDialog.Builder(MainActivity.this);
					fail.setTitle(getString(R.string.alert_mmc_not_found));
					fail.setMessage(getString(R.string.alert_mmc_not_found_msg));
					fail.setPositiveButton(getString(R.string.action_ok),new DialogInterface.OnClickListener() {
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
			Log.d(LOG_TAG, "No usable device blocks found.");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					AlertDialog.Builder fail = new AlertDialog.Builder(MainActivity.this);
					fail.setTitle(getString(R.string.alert_no_support));
					fail.setMessage(getString(R.string.alert_no_blocks));
					fail.setPositiveButton(getString(R.string.action_ok),new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							finish();
						}
					});
					fail.setCancelable(false);
					fail.create();
					fail.show();
				}
			});
		}
	}

	void listDeviceBlocks() throws IOException {

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
				if((blockDeviceFiles.isEmpty() && mmcblk1) || listAllLUNsAndBlocks) blockDeviceFiles.add(memoryCardBlock); //Never true
			}
		} else {
			for(int i=1; i<=3; i++) {
				output = root.execute("ls "+memoryCardBlock+"p"+i);
				if ( output!=null && output.equals(memoryCardBlock+"p"+i) ) {
					blockDeviceFiles.add(memoryCardBlock+"p"+i);
				}
			}
			if(blockDeviceFiles.isEmpty() || listAllLUNsAndBlocks) {
				output = root.execute("ls "+memoryCardBlock);
				if ( output!=null && output.equals(memoryCardBlock) ) {
					blockDeviceFiles.add(memoryCardBlock);
				}
			}
		}

		int defaults = blockDeviceFiles.size();
		if(defaults > 0) {
			extSDpresent = true;
			data.edit().putBoolean(extSDpresence, extSDpresent).apply();
		}
		Boolean foundInMount = false;
		if(defaults==0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || listAllLUNsAndBlocks) { //since Internal Storage can be mounted as UMS before KK

			output = root.execute("mount | grep /dev/block/vold");
			if(output == null || output.trim().isEmpty()) {
				Log.d(LOG_TAG, "No vold blocks found from mount command..");
			} else {
				String devicePoints[] = output.split("\n");
				for(String voldPoint: devicePoints) {
					voldPoint = voldPoint.substring(0, voldPoint.indexOf(" "));
					if(voldPoint.contains("/dev/block/vold") && !blockDeviceFiles.contains(voldPoint)) blockDeviceFiles.add(voldPoint);
				}
				if( blockDeviceFiles.size() > defaults ) foundInMount = true;
			}
			if(blockDeviceFiles.size() == 0 || ((Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || listAllLUNsAndBlocks)&&!foundInMount)) {
				output = root.execute("ls /dev/block/vold/");
				if(output == null || output.trim().isEmpty()) {
					Log.d(LOG_TAG, "No vold blocks found from /dev/block/vold");
				} else {
					String devicePoints[] = output.split("\n");
					for(String voldPoint: devicePoints) {
						blockDeviceFiles.add("/dev/block/vold/"+voldPoint);
					}
				}
			}
		}

		if((defaults > 0 || foundInMount)&&!listAllLUNsAndBlocks) {
			blockRecommendation = false;
			data.edit().putBoolean(blockRecommendations, false).apply();
		} else {
			blockRecommendation = true;
			data.edit().putBoolean(blockRecommendations, true).apply();
		}
	}

	void getRootShell() {

		try {

			if (!RootTools.isRootAvailable()) {
				Log.d(LOG_TAG, "It seems su binary is missing, or unable to search for it.");
				noRootAccess(getString(R.string.error_su_not_found)+" \n"+getString(R.string.error_no_root));
				return;
			}
			Log.d(LOG_TAG, "Requesting for Root Access..");
			rootShell = RootTools.getShell(true);

		} catch (IOException e) {
			noRootAccess(getString(R.string.error_root_not_granted));
			e.printStackTrace();
		} catch (TimeoutException e) {
			Log.d(LOG_TAG, "Timeout waiting for Root Access..");
			noRootAccess(getString(R.string.error_root_timeout));
			e.printStackTrace();
		} catch (RootDeniedException e) {
			Log.d(LOG_TAG, "Denied Root Access..");
			noRootAccess(getString(R.string.error_root_denied));
			e.printStackTrace();
		}
	}

	void buildCommands() {

		String nofua="", rw_mode, cdrom, setLUN, remov, lunPath;
		//numberOfLUNsFound = data.getInt(luns, 1);
		enableMSG = "";
		StringBuilder temp = new StringBuilder("");
		for(int i=0; i< availableLUNs.size() ;i++) {
			if(!enabledLUNs[i]) continue;
			lunPath = availableLUNs.get(i);
			if(!temp.equals("")) temp.append("\n");
			rw_mode = "echo 0 > "+lunPath+"/ro";
			cdrom = "echo 0 > "+lunPath+"/cdrom";
			remov = "echo 1 > "+lunPath+"/removable";
			//nofua = "echo 1 > "+lunPath+"/nofua";
			setLUN = "echo "+blockDevice+" > "+lunPath+"/file";
			temp.append(rw_mode).append("\n")
					.append(cdrom).append("\n")
					.append(remov).append("\n")
					.append(setLUN);
		}
		enableMSG = temp.toString();

		clearLUN = "";
		for(String s: availableLUNs) {
			if(!clearLUN.equals("")) clearLUN += "\n";
			clearLUN += "echo '' > "+s+"/file";
		}
	}

	void setPermissions(boolean execute) {
		StringBuilder setPerm = new StringBuilder("");
		for(String str: availableLUNs) {
			if(!setPerm.equals("")) setPerm.append("\n");
			setPerm.append("chmod 777 ").append(str).append("/nofua\n")
					.append("chmod 777 ").append(str).append("/ro\n")
					.append("chmod 777 ").append(str).append("/cdrom\n")
					.append("chmod 777 ").append(str).append("/removable\n")
					.append("chmod 777 ").append(str).append("/file");
		}
		saveCommands(setPerm.toString());
		if(execute) executeAsSU(setPerm.toString());
	}

	void saveCommands(String setPerm) {
		//This shit is saved just for the sake of easily auto-enable UMS when USB is connected, enabled from Settings
		data.edit().putString(enableUMScmds, clearLUN+"\n"+CDprop+"\n"+enableMSG+"\n"+UMSlegacy+"\n"+UMSconfig+"\n"+UMSprop+(enableADB?",adb":"")+"\n"+enableMSG).apply();
		data.edit().putString(disableUMScmds, clearLUN+"\n"+defaultDeviceProp+"\n"+MTPprop).apply();
		data.edit().putString(setPermissionCmds, setPerm).apply();
	}

	void setUI() {

		setContentView(R.layout.activity_main);
		if(listAllLUNsAndBlocks) return; //reduce unnecessary load
		initADs();
		materialDrawer();
		//TextView link = (TextView) findViewById(R.id.textView1);
	   // String linkText = "<a href='http://forum.xda-developers.com/android/apps-games/app-universal-mass-storage-enabler-beta-t3240097'>Created By Gokul NC</a>";
	   // link.setText(Html.fromHtml(linkText));
	   // link.setMovementMethod(LinkMovementMethod.getInstance());
		final Animation animAlpha = AnimationUtils.loadAnimation(this, R.anim.anim_alpha);

	    ums = (ImageButton)findViewById(R.id.button1);
		unmount = (Button)findViewById(R.id.button2);
		mtp = (Button)findViewById(R.id.button4);
		USBmode = (TextView) findViewById(R.id.textView3);
		USBstatus = (TextView) findViewById(R.id.textView2);
		if(rootAccess) updateUSBconfig();

		/*ums.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				executeAsSU(clearLUN+"\n"+CDprop+"\n"+enableMSG+"\n"+UMSlegacy+"\n"+UMSconfig+"\n"+UMSprop+(enableADB?",adb":""));
				Toast.makeText(getApplicationContext(), "Mass Storage Enabled!!", Toast.LENGTH_SHORT).show();
				USBconfig = "mass_storage"+(enableADB?",adb":"");
				USBmode.setText(Html.fromHtml("USB Config: <b>"+USBconfig+"</b>" ));
				ums.setBackgroundResource(R.drawable.usb_on);
				arg0.startAnimation(animAlpha);

			}
		});*/


		mtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

            	executeAsSU(clearLUN+"\n"+defaultDeviceProp+"\n"+MTPprop+(enableADB?",adb":""));
                Toast.makeText(getApplicationContext(), getString(R.string.toast_mtp_enabled), Toast.LENGTH_SHORT).show();
                setUSBconfig("mtp"+(enableADB?",adb":""));
				unmount.setText(getString(R.string.action_enable_ums));
				UsbBroadcastReceiver.isUMSdisabled = true;
				UsbBroadcastReceiver.removeNotification(MainActivity.this);
				/*if(USBconfig.equals("mass_storage")) {
					ums.startAnimation(animAlpha);
					ums.setBackgroundResource(R.drawable.usb_off);
				}
				if(USBconfig.equals("mass_storage,adb")) {
					ums.startAnimation(animAlpha);
					ums.setBackgroundResource(R.drawable.usb_off);
				}*/
				ums.setBackgroundResource(R.drawable.usb_off);

            }
        });

		unmount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
				String ButtonText = unmount.getText().toString();
				if(ButtonText.equals(getString(R.string.action_enable_ums))){
					enableUMS();
					ums.startAnimation(animAlpha);
					/*animFade.setAnimationListener(new Animation.AnimationListener() {
						public void onAnimationStart(Animation animation) {}
						public void onAnimationRepeat(Animation animation) {}
						public void onAnimationEnd(Animation animation) {
							// when fadeout animation ends, fade in your second image
						}
					});*/
				}
				else{
					disableUMS();
					ums.startAnimation(animAlpha);
				}
			}
        });

		/*class backgroundTask extends AsyncTask<Void, Void, Void> {
			@Override
			protected Void doInBackground(Void... params) {
				Intent intent = getApplicationContext().registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE"));
	        	Boolean prev = intent.getExtras().getBoolean("connected");
	        	while(true) {
	        		if(!foreground) break;
	        		intent = getApplicationContext().registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE"));
	        		USBconnected = intent.getExtras().getBoolean("connected");
	        		if(USBconnected != prev) {

						boolean post = USBstatus.post(new Runnable() {
							public void run() {
								USBstatus.setText(Html.fromHtml("USB Status: <b>" + (USBconnected ? "" : "DIS") + "CONNECTED</b>"));
							}
						});

						prev = USBconnected;

	        			try {
							Thread.sleep(2500);
						} catch (InterruptedException e) { e.printStackTrace(); }
	        		}

	        	}
				return null;
			}
			@Override
			protected void onPostExecute(Void params) {
				foreground = false;
			}

		}

		new backgroundTask().execute();*/

	}

	void materialDrawer() {
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		try {
			getSupportActionBar().setTitle(getString(R.string.app_name_short));
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Create a few sample profile
		// NOTE you have to define the loader logic too. See the CustomApplication for more details
		final IProfile profile = new ProfileDrawerItem().withName(getString(R.string.app_name)).withIcon(R.drawable.profile).withIdentifier(100).withEmail(currentVersion);

		// Create the AccountHeader

		AccountHeader headerResult = new AccountHeaderBuilder()
				.withActivity(this)
				.withTranslucentStatusBar(true)
				.withHeaderBackground(R.color.primary)
				.withSelectionListEnabled(false)
				.addProfiles(profile)
				//.withSavedInstance(savedInstanceState)
				.build();

		/*OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
				if (drawerItem instanceof Nameable) {
					Log.i("material-drawer", "DrawerItem: " + ((Nameable) drawerItem).getName() + " - toggleChecked: " + isChecked);
					enableADB = isChecked;
					data.edit().putBoolean(ADBenable, enableADB).apply();
				} else {
					Toast.makeText(getApplicationContext(), "Undefined Switch Toggle.", Toast.LENGTH_SHORT).show();
					Log.i("material-drawer", "toggleChecked: " + isChecked);
				}
			}
		};*/

		Drawer result = new DrawerBuilder()
				.withActivity(this).withToolbar(toolbar)
				.withHasStableIds(true) //TODO: this is something dangerous thanks to vikasb32, have to make ids dynamic to avoid collisions
				.withAccountHeader(headerResult) //set the AccountHeader we created earlier for the header
				.addDrawerItems(
						//new SwitchDrawerItem().withName(R.string.drawer_item_enable_adb).withOnCheckedChangeListener(onCheckedChangeListener).withChecked(enableADB),
						//new DividerDrawerItem(),
						new PrimaryDrawerItem().withIdentifier(103).withName(R.string.drawer_item_app_settings).withIcon(R.drawable.settings).withSelectable(false),
						new PrimaryDrawerItem().withIdentifier(101).withName(R.string.drawer_item_default_usb_settings).withIcon(R.drawable.default_setting).withSelectable(false),
						new PrimaryDrawerItem().withIdentifier(102).withName(R.string.drawer_item_storage_settings).withIcon(R.drawable.storage_setting).withSelectable(false),
						new PrimaryDrawerItem().withIdentifier(104).withName(R.string.drawer_item_enable_selinux).withIcon(R.drawable.selinux).withSelectable(false), //.withDescription(R.string.drawer_item_enable_selinux_description)
						new PrimaryDrawerItem().withIdentifier(106).withName(R.string.drawer_item_check_updates).withIcon(R.drawable.update).withSelectable(false),
						new PrimaryDrawerItem().withIdentifier(107).withName(R.string.drawer_item_support).withIcon(R.drawable.ads_icon).withSelectable(false)

				) // add the items we want to use with our Drawer
				.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
					@Override
					public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
						//check if the drawerItem is set.
						//there are different reasons for the drawerItem to be null
						//--> click on the header
						//--> click on the footer
						//those items don't contain a drawerItem

						if (drawerItem != null) {

							switch ((int) drawerItem.getIdentifier()) {
								case 101:
									if (!USBconnected) Toast.makeText(getApplicationContext(), R.string.toast_usb_not_connected, Toast.LENGTH_SHORT).show();
									if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
										executeAsSU("am start -n 'com.android.settings/.Settings$UsbSettingsActivity'");
									else
										executeAsSU("am start -n 'com.android.settings/.deviceinfo.UsbModeChooserActivity'");
									break;
								case 102:
									startActivity(new Intent(android.provider.Settings.ACTION_MEMORY_CARD_SETTINGS));
									//moveTaskToBack(true);
									break;
								case 103:
									startActivity(new Intent(MainActivity.this, AdvancedOptions.class));
									if (mInterstitialAd.isLoaded()) mInterstitialAd.show();
									requestNewInterstitial();
									break;
								case 104:
									executeAsSU("setenforce 1");
									Toast.makeText(getApplicationContext(), R.string.toast_selinux_enabled, Toast.LENGTH_SHORT).show();
									//active = false;
									finish();
									break;
								case 106:
									AppUpdate.checkForUpdates(false, FirebaseRemoteConfig.getInstance(), MainActivity.this);
									break;
								case 107:
									if (mInterstitialAd.isLoaded()) {
										mInterstitialAd.show();
										Toast.makeText(getApplicationContext(), R.string.toast_thanks+" :)", Toast.LENGTH_SHORT).show();
									} else {
										Toast.makeText(getApplicationContext(), R.string.toast_internet_problem, Toast.LENGTH_SHORT).show();
									}
									requestNewInterstitial();
									break;
							}
						}

						return false;
					}
				})
				//.withSavedInstance(savedInstanceState)
				//.withShowDrawerOnFirstLaunch(true)
				.build();
		result.setSelection(100, false);
		//only set the active selection or active profile if we do not recreate the activity
		/*if (savedInstanceState == null) {
			// set the selection to the item with the identifier 11
			result.setSelection(100, false);

			//set the active profile
			//headerResult.setActiveProfile(profile3);
		}*/

		//result.updateBadge(4, new StringHolder(10 + ""));

	}

	void enableUMS() {
		//If changing the below line, update saveCommands() also
		executeAsSU(clearLUN+"\n"+CDprop+"\n"+enableMSG+"\n"+UMSlegacy+"\n"+UMSconfig+"\n"+UMSprop+(enableADB?",adb":"")+"\n"+enableMSG);
		//LUN parameters are written twice, since some devices' USB configs seem to write their own values (from their init.xxx.usb.rc) after the 'UMSprop' command
		//It is also written before 'UMSprop' since some devices don't seem to allow changing LUN parameters after 'UMSprop'; it's weird

		setUSBconfig("mass_storage"+(enableADB?",adb":""));
		//USBmode.setTextColor(0xff00ff00);

		unmount.setText(getString(R.string.action_disable_ums));
		ums.setBackgroundResource(R.drawable.usb_on);
		//Snackbar.make(findViewById(R.id.frame_container), R.string.toast_ums_enabled, Snackbar.LENGTH_SHORT).show();
		Toast.makeText(getApplicationContext(), getString(R.string.toast_ums_enabled), Toast.LENGTH_SHORT).show();
		UsbBroadcastReceiver.isUMSdisabled = false;
		data.edit().putBoolean(isUmsDisabled, false).apply();
		if(enableNotifications) UsbBroadcastReceiver.showNotification(this);

	}

	void disableUMS() {
		executeAsSU(clearLUN+"\n"+defaultDeviceProp+"\n"+MTPprop);
		if(enableADB) executeAsSU(enableADBmode+"\n"+ADBonlyMode);
		setUSBconfig("mtp");
		UsbBroadcastReceiver.isUMSdisabled = true;
		data.edit().putBoolean(isUmsDisabled, true).apply();
		UsbBroadcastReceiver.removeNotification(this);

		unmount.setText(getString(R.string.action_enable_ums));
		ums.setBackgroundResource(R.drawable.usb_off);
		//Snackbar.make(findViewById(R.id.frame_container), R.string.toast_ums_disabled, Snackbar.LENGTH_SHORT).show();
		Toast.makeText(getApplicationContext(), getString(R.string.toast_ums_disabled), Toast.LENGTH_SHORT).show();
		if(enableMediaScan && !memoryCardPath.equals("")) forceMediaScan(memoryCardPath);
	}

	void setUSBconfig(String mode) {

		USBconfig = mode;
		USBmode.setText(Html.fromHtml(getString(R.string.main_layout_usb_config)+" <b>"+mode+"</b>" ));
	}

	public void updateUSBstatus() {
		//USBconnected = getApplicationContext().registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE")).getExtras().getBoolean("connected");
		if(USBstatus!=null)
			USBstatus.setText(Html.fromHtml(getString(R.string.main_layout_usb_status)+" <b>" + (USBconnected ? getString(R.string.state_connected) : getString(R.string.state_disconnected)) + "</b>"));
	}

	void updateUSBconfig() {

		try {
			USBconfig = root.execute("getprop sys.usb.config");
			if(USBconfig.contains("mass_storage")) {
				unmount.setText(getString(R.string.action_disable_ums));
				UsbBroadcastReceiver.isUMSdisabled = false;
				UsbBroadcastReceiver.showNotification(this);
				ums.setBackgroundResource(R.drawable.usb_on);
			} else {
				unmount.setText(getString(R.string.action_enable_ums));
				ums.setBackgroundResource(R.drawable.usb_off);
			}
			if(USBconfig!=null) {
				USBmode.setText(Html.fromHtml(getString(R.string.main_layout_usb_config)+" <b>" + USBconfig + "</b>"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void forceMediaScan(final String path) {
		try {

			//Method 1:
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
				sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(path)));
			else
				sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse(path)));
			Log.i(LOG_TAG, "Running Media Scan on path: " + path);

			//Method 2:
			MediaScannerConnection.scanFile(this, new String[]{path}, new String[]{"*/*"},
					new MediaScannerConnection.OnScanCompletedListener() {
						@Override
						public void onScanCompleted(final String path, final Uri uri) {
							Log.i(LOG_TAG, "Scanned path: " + path);
							//Log.i(LOG_TAG, String.format("Scanned path %s -> URI = %s", path, uri.toString()));
						}
					});
		} catch(Exception e) {
			Toast.makeText(getApplicationContext(), "Error running mediascan", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}

	@Nullable
	public String[] getExternalStorageDirectories() {
		// http://stackoverflow.com/a/39372019/5002496
		String [] storageDirectories = null;
		List<String> results = new ArrayList<>();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			File[] externalDirs = getExternalFilesDirs(null);
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
			if(rootAccess) {
				String output = null;
				try {
					output = root.execute("mount | grep /dev/block/vold");
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(output == null || output.trim().isEmpty()) {
					//Log.d(LOG_TAG, "No vold blocks found from mount command..");
				} else {
					Log.d(LOG_TAG, "WTF..\n"+output);
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
					Log.d(LOG_TAG, results.get(i) + " might not be extSDcard");
					results.remove(i--);
				}
			}
		} else {
			for (int i = 0; i < results.size(); i++) {
				if (!results.get(i).toLowerCase().contains("ext") && !results.get(i).toLowerCase().contains("sdcard")) {
					Log.d(LOG_TAG, results.get(i)+" might not be extSDcard");
					results.remove(i--);
				}
			}
		}

		storageDirectories = new String[results.size()];
		for(int i=0; i<results.size(); ++i) storageDirectories[i] = results.get(i);

		return storageDirectories;
	}

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
		//TODO: wrote this in a hurry, have to rewrite using resultCode
		if(listAllLUNsAndBlocks) {
			setContentView(R.layout.activity_empty);
			isFirstRun=true;
			try {
				continueExec();
			} catch (final Exception ex) {
				ex.printStackTrace();
			}
			listAllLUNsAndBlocks = false;
			startActivity(new Intent(MainActivity.this, AdvancedOptions.class));
			return;
		}
        buildCommands();
		setPermissions(false);
        if(requiresUnmount && blockRecommendation) showUnmountWarning();
        if(numberOfLUNsUsed > 1) multipleLUNsWarning();
    }

	void executeAsSU(final String command) {

		Command cmd = new Command(0, command);

		try {
			if(rootShell==null || rootShell.isClosed) getRootShell();
			rootShell.add(cmd);
		} catch (final Exception e) {
			e.printStackTrace();
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					AlertDialog.Builder fail = new AlertDialog.Builder(MainActivity.this);
					fail.setTitle(getString(R.string.error_general));
					CharSequence msg = getString(R.string.error_execution)+"\n\n"+command+"\n\n"+e.getMessage()+"\n\n"+getString(R.string.error_contact);
					fail.setMessage(msg);
					fail.setPositiveButton(getString(R.string.action_ok),new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							finish();
						}
					});

					fail.setCancelable(false);
					fail.create();
					fail.show();
				}
			});
		}

	}

	void noRootAccess(final String rootDetails) {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), rootDetails, Toast.LENGTH_SHORT).show();
				Toast.makeText(getApplicationContext(), getString(R.string.toast_no_root), Toast.LENGTH_SHORT);
				AlertDialog.Builder fail = new AlertDialog.Builder(MainActivity.this);
				fail.setTitle(getString(R.string.toast_no_root));
				CharSequence msg = getString(R.string.error_msg)+" \""+rootDetails+"\"\n\n"+getString(R.string.error_details)+"\n"+getString(R.string.alert_no_root_msg);
				fail.setMessage(msg);
				fail.setPositiveButton(getString(R.string.action_ok),new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						finish();
					}
				});

				fail.setCancelable(false);
				fail.create();
				fail.show();
			}
		});
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

		final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE_for_app_download = 10;

		switch (requestCode) {
			case MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE_for_app_download:
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					AppUpdate.downloadAndInstallApk(AppUpdate.downloadURL, getApplicationContext());

				} else {

					if(ActivityCompat.shouldShowRequestPermissionRationale(this,
							Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
						new AlertDialog.Builder(this).
								setTitle(R.string.alert_perm_denied).
								setMessage(R.string.alert_update_download_permission).show();
					} else {
						new AlertDialog.Builder(this).
								setTitle(R.string.alert_perm_denied).
								setMessage(R.string.alert_update_download_permission_failed).show();
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(XdaThreadURL)));
					}

				}

				break;
		}
	}

	void noSupport() {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder fail = new AlertDialog.Builder(MainActivity.this);
				fail.setTitle("App Failed");
				String msg;
				if(availableLUNs.isEmpty()) msg = getString(R.string.alert_no_gadget_msg);
				else if(blockDevice==null || blockDevice.isEmpty()) msg = getString(R.string.alert_mmc_not_found_msg);
				else msg = getString(R.string.error_unknown);
				fail.setMessage(msg);
				fail.setPositiveButton(getString(R.string.action_ok),new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						finish();
					}
				});

				fail.setCancelable(false);
				fail.create();
				fail.show();
			}
		});

		if(!busyboxPresent) busyboxNotFound();
	}

	void busyboxNotFound() {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder fail = new AlertDialog.Builder(MainActivity.this);
				fail.setTitle(getString(R.string.alert_no_busybox));
				fail.setMessage(getString(R.string.alert_no_busybox_msg));
				fail.setNegativeButton(getString(R.string.action_later), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {	}
				});

				fail.setPositiveButton(getString(R.string.action_install_busybox),new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						try {
							startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=stericson.busybox")));
						} catch (android.content.ActivityNotFoundException e) {
							startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=stericson.busybox")));
						}
					}
				});

				fail.setCancelable(false);
				fail.create();
				fail.show();
			}
		});

	}

	void multipleLUNsWarning() {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder warning = new AlertDialog.Builder(MainActivity.this);
				warning.setTitle(R.string.alert_warning);
				warning.setMessage(getString(R.string.alert_multi_luns_msg));
				warning.setPositiveButton(getString(R.string.action_ok),new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {  }
				});

				warning.create();
				warning.show();
			}
		});
	}

	void showUnmountWarning() {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder warn = new AlertDialog.Builder(MainActivity.this);
				warn.setTitle(R.string.alert_warning);
				String msg = getString(R.string.alert_block_notrecommended)+"\n\n"+getString(R.string.alert_unmount);
				warn.setMessage(msg);
				warn.setPositiveButton(getString(R.string.action_ok),new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {   }
				});
				warn.create();
				warn.show();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		//menu.findItem(R.id.adb).setChecked(enableADB);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {

		switch(menuItem.getItemId()) {
			case R.id.action_settings:
				Intent intent = new Intent(MainActivity.this, AdvancedOptions.class);
				startActivityForResult(intent, 1);
				return true;

			case R.id.action_help:
				startActivity(new Intent(MainActivity.this, Help2.class));
				return true;

			default:
				return super.onOptionsItemSelected(menuItem);
		}

		/*else if (itemId == R.id.adb) {
			if (menuItem.isChecked()) menuItem.setChecked(false);
	        else menuItem.setChecked(true);
			enableADB = menuItem.isChecked();
			data.edit().putBoolean(ADBenable, enableADB).commit();
			return true;
		} else if (itemId == R.id.advanced) {
			Intent intent = new Intent(MainActivity.this,AdvancedOptions.class);
			startActivityForResult(intent, 1);
			return true;
		} else if (itemId == R.id.storageSettings) {
			//executeAsSU("am start -n 'com.android.settings/.Settings$StorageSettingsActivity");
			Intent i = new Intent(android.provider.Settings.ACTION_MEMORY_CARD_SETTINGS);
			startActivity(i);
			return true;
		} else if (itemId == R.id.defaultUSBsettings) {
			if(USBconnected) {
				if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) executeAsSU("am start -n 'com.android.settings/.Settings$UsbSettingsActivity'");
				else executeAsSU("am start -n 'com.android.settings/.deviceinfo.UsbModeChooserActivity'");
			} else Toast.makeText(getApplicationContext(), "Make sure to connect USB to choose device's default mode!", Toast.LENGTH_SHORT).show();
			return true;
		} else if (itemId == R.id.enableSElinux) {
			executeAsSU("setenforce 1");
			Toast.makeText(getApplicationContext(), "SELinux set to Enforcing!!", Toast.LENGTH_SHORT).show();
			finish();
			return true;
		} else if (itemId == R.id.updates) {
			checkForUpdates();
			return true;
		} else if (itemId == R.id.help) {
			startActivity(new Intent(MainActivity.this,Help.class));
			return true;
		} else  if (itemId == R.id.exit) {
			foreground = false;
			finish();
			return true;
		}*/



	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		Log.d(LOG_TAG, "Back Pressed..");
		freeEveryShit();
		//finish();
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d(LOG_TAG, "App Stopped..");
		isAppOpen=false;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		freeEveryShit();
		Log.d(LOG_TAG, "App Destroyed..");
	}

	void freeEveryShit() {
		try {
			if(mUsbReceiver != null) unregisterReceiver(mUsbReceiver);
		} catch(Exception e) {
			Log.d(LOG_TAG, "Unable to Unregister the mUsbReceiver");
			e.printStackTrace();
		}
		//active = false;
	}

	@Override
	public void onResume() {
		super.onResume();
		//active = true;
		Log.d(LOG_TAG, "onResume() initiated..");
		isAppOpen=true;
		if(data==null) data=getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
		if(rootAccess) {
			updateUSBconfig();
			updateUSBstatus();
		}
	}

}




/*
References for future: 

1.
To find external SD card mount point: mount | sed 's/ on / /g' | egrep "extSdCard|external_sd|sdcard1" | grep /dev/block | cut -f 2 -d " "
To find its device block: mount | sed 's/ on / /g' | egrep "extSdCard|external_sd|sdcard1" | grep /dev/block | cut -f 1 -d " "
More @ here: http://forum.xda-developers.com/galaxy-s3/themes-apps/app-easyums-t2576500


*/