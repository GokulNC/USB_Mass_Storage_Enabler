package com.gokulnc.ums_universal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import android.Manifest;
import android.app.Dialog;
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

import static com.gokulnc.ums_universal.Dialogs.*;


public class MainActivity extends AppCompatActivity {

	//Following strings correspond to SharedPref keys
	public static SharedPreferences data;
	public static final String MyPREFERENCES = "Settings" ;
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

					blockRecommendation = data.getBoolean(blockRecommendations, true);
					currentVersionNumber = data.getInt(currentVersionCode, 0);
					try {
						currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
						if(currentVersionNumber < getPackageManager().getPackageInfo(getPackageName(), 0).versionCode) {
							data.edit().putBoolean(firstRun, true).apply();
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
							noRootAccess(getString(R.string.error_unable_shell), MainActivity.this);
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

					} else noRootAccess(getString(R.string.error_root_not_granted), MainActivity.this);
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

		if(enableADs) {
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
	}

	void showInterstitialADs() {
		if(enableADs) {
			if (mInterstitialAd.isLoaded()) mInterstitialAd.show();
			requestNewInterstitial();
		}
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

    	if(data.getBoolean(firstRun, true)) {
			Log.d(LOG_TAG, "Running app for first time");
			try {
				Log.d(LOG_TAG, "Initiating firstRun()");
				if(!FirstTimeSetup.firstRun(this, root)) return;
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
		int blockNumber = data.getInt(defaultBlockNumber , 0);
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


		if(requiresUnmount) showUnmountWarning(MainActivity.this);
		if(numberOfLUNsUsed > 1) multipleLUNsWarning(MainActivity.this);

		if( !(availableLUNs.isEmpty()) && blockDevice != null && !(blockDevice.isEmpty()) && numberOfLUNsUsed!=0 ) {
			buildCommands();
			setPermissions(true);
		} else {
			Log.d(LOG_TAG, "Unable to build commands.");
			Log.d(LOG_TAG, "No. of LUNs found & selected: "+availableLUNs.size() + " & "+ numberOfLUNsUsed);
			Log.d(LOG_TAG, "Partition Block selected: "+blockDevice);
			noSupport(this);
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setUI();
				setBroadcastReceivers(); //WARNING: this should be after setUI only
			}
		});

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

	void getRootShell() {

		try {

			if (!RootTools.isRootAvailable()) {
				Log.d(LOG_TAG, "It seems su binary is missing, or unable to search for it.");
				noRootAccess(getString(R.string.error_su_not_found)+" \n"+getString(R.string.error_no_root), this);
				return;
			}
			Log.d(LOG_TAG, "Requesting for Root Access..");
			rootShell = RootTools.getShell(true);

		} catch (IOException e) {
			noRootAccess(getString(R.string.error_root_not_granted), this);
			e.printStackTrace();
		} catch (TimeoutException e) {
			Log.d(LOG_TAG, "Timeout waiting for Root Access..");
			noRootAccess(getString(R.string.error_root_timeout), this);
			e.printStackTrace();
		} catch (RootDeniedException e) {
			Log.d(LOG_TAG, "Denied Root Access..");
			noRootAccess(getString(R.string.error_root_denied), this);
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
									showInterstitialADs();
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
									if(enableADs) {//TODO: showInterstitial()
										if (mInterstitialAd.isLoaded()) {
											mInterstitialAd.show();
											Toast.makeText(getApplicationContext(), R.string.toast_thanks + " :)", Toast.LENGTH_SHORT).show();
										} else {
											Toast.makeText(getApplicationContext(), R.string.toast_internet_problem, Toast.LENGTH_SHORT).show();
										}
										requestNewInterstitial();
									}
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

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
		//TODO: wrote this in a hurry, have to rewrite using resultCode
		if(listAllLUNsAndBlocks) {
			setContentView(R.layout.activity_empty);
			data.edit().putBoolean(firstRun, true).apply();
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
        if(requiresUnmount && blockRecommendation) showUnmountWarning(this);
        if(numberOfLUNsUsed > 1) multipleLUNsWarning(this);
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