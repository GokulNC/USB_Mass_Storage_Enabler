package com.gokulnc.ums_universal;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuItem;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.util.Arrays;
import static com.gokulnc.ums_universal.Constants.*;


public class AdvancedOptions extends AppCompatActivity {

	private static final int ROOT_DIR_REQ_CODE = 1;
	private static final int BLOCK_FILE_REQ_CODE = 2;
	LinearLayout ll;
	int cbCount, rbCount;
	CheckBox[] cb;
	RadioButton[] rb;
	RadioGroup radioGroup;
	Switch adb, disableNotifs, enableAutoStart, enableMediaScanner, autoUpdateCheck;
	Button save, selectDir;
	TextView dir;
	String blockSuggestion = "", memoryCardPath="";
	static int blockNum = -1;
	SharedPreferences data = MainActivity.data;
	//Map<Integer, Integer> radioGroupToBlockMap; //not working currently, so setting id directly, bad practice though

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_advancedoptions);
		ll = (LinearLayout) findViewById(R.id.advancedlinearlayout);
		cbCount = MainActivity.availableLUNs.size();
		rbCount = MainActivity.blockDeviceFiles.size();
		if(data==null) data= getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
		memoryCardPath = data.getString(mediaScanPath, "");

		//ll.setPadding(20, 20, 20, 20);
		//ll.setBackgroundColor(Color.parseColor("#000000"));
		radioGroup = new RadioGroup(this);
		rbCount = MainActivity.blockDeviceFiles.size();
		cbCount = MainActivity.availableLUNs.size();
		//radioGroupToBlockMap = new HashMap<Integer, Integer>();
		cb = new CheckBox[cbCount];
		rb = new RadioButton[rbCount];

		adb = (Switch) findViewById(R.id.switch1);
		adb.setChecked(MainActivity.enableADB);

		disableNotifs = (Switch) findViewById(R.id.switch2);
		disableNotifs.setChecked(!MainActivity.enableNotifications);

		enableAutoStart = (Switch) findViewById(R.id.switch3);
		enableAutoStart.setChecked(data.getBoolean(autoStart, false));

		autoUpdateCheck = (Switch) findViewById(R.id.switch4);
		autoUpdateCheck.setChecked(data.getBoolean(autoUpdate, true));

		enableMediaScanner = (Switch) findViewById(R.id.switch5);
		enableMediaScanner.setChecked(data.getBoolean(mediaScanEnable, false));

		selectDir = (Button) findViewById(R.id.button3);
		dir = (TextView) findViewById(R.id.textView);
		dir.setText(getString(R.string.settings_layout_mediascan_path)+": "+memoryCardPath.replace("file://",""));
		if(enableMediaScanner.isChecked()) {
			selectDir.setVisibility(View.VISIBLE);
			dir.setVisibility(View.VISIBLE);
		}
		enableMediaScanner.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) {
					selectDir.setVisibility(View.VISIBLE);
					dir.setVisibility(View.VISIBLE);
					dir.setText(getString(R.string.settings_layout_mediascan_path)+": "+memoryCardPath.replace("file://",""));
				} else {
					selectDir.setVisibility(View.GONE);
					dir.setVisibility(View.GONE);
				}
			}
		});
		
		TextView LUNs = new TextView(this);
		LUNs.setText("\n  "+getString(R.string.settings_layout_luns_in_use)+"\n");
		LUNs.setTextSize(18);
		LUNs.setTypeface(null, Typeface.BOLD);
		ll.addView(LUNs);
		
		for(int i=0; i<cbCount; i++) {
			cb[i] = new CheckBox(this);
			cb[i].setText(MainActivity.availableLUNs.get(i));
			cb[i].setTextColor(Color.parseColor("#000000"));
			if(MainActivity.enabledLUNs[i]) cb[i].setChecked(true);
			ll.addView(cb[i]);
		}
		
		TextView blocks = new TextView(this);
		blocks.setText("\n  "+getString(R.string.settings_layout_partition_in_use)+"\n");
		blocks.setTextSize(18);
		blocks.setTypeface(null, Typeface.BOLD);
		ll.addView(blocks);

		radioGroup.removeAllViews();
		for(int i=0; i<rbCount; i++) {
			rb[i] = new RadioButton(this);
			if(MainActivity.blockRecommendation) blockSuggestion = (MainActivity.recommendedBlocks[i])? " (Recommended)" : "";
			rb[i].setText( MainActivity.blockDeviceFiles.get(i) + blockSuggestion );
			rb[i].setTextColor(Color.parseColor("#000000"));
			//radioGroupToBlockMap.put(rb[i].getId(), i);
			rb[i].setId(i);
			radioGroup.addView(rb[i], i);
			if( MainActivity.blockDeviceFiles.get(i).equals( MainActivity.blockDeviceFiles.get(data.getInt(defaultBlockNumber, -1)))) {
				radioGroup.check(radioGroup.getChildAt(i).getId());
				//Toast.makeText(getApplicationContext(), "id: "+radioGroup.getChildAt(i).getId(), Toast.LENGTH_SHORT).show();
			}
		}
		
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
	        @Override
	        public void onCheckedChanged(RadioGroup group, int checkedId) {
	            //if(radioGroupToBlockMap.get(checkedId)!=null) blockNum = (int) radioGroupToBlockMap.get(checkedId);
				blockNum = checkedId;
				//Toast.makeText(getApplicationContext(), "checkid: "+checkedId+" block: "+blockNum, Toast.LENGTH_SHORT).show();
	        }
	    });
		
		ll.addView(radioGroup);
		
		save = new Button(this);
		save.setText(getString(R.string.action_save_close));
		//To save all the options selected:
		save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

				if(adb.isChecked() != MainActivity.enableADB) {
					MainActivity.enableADB = adb.isChecked();
					data.edit().putBoolean(ADBenable, MainActivity.enableADB).apply();
				}

				if(disableNotifs.isChecked() == MainActivity.enableNotifications) {
					MainActivity.enableNotifications = !disableNotifs.isChecked();
					data.edit().putBoolean(NotifsEnable, MainActivity.enableNotifications).apply();
				}

				data.edit().putBoolean(autoStart, enableAutoStart.isChecked()).apply();
				data.edit().putBoolean(autoUpdate, autoUpdateCheck.isChecked()).apply();

				MainActivity.enableMediaScan = enableMediaScanner.isChecked();
				data.edit().putBoolean(mediaScanEnable, MainActivity.enableMediaScan).apply();
				data.edit().putString(mediaScanPath, memoryCardPath).apply();

				//Save Selected Partition Block:
            	if(blockNum != -1) {
            		MainActivity.requiresUnmount = !MainActivity.recommendedBlocks[blockNum];
            		data.edit().putInt(defaultBlockNumber, blockNum).apply();
            		MainActivity.blockDevice = MainActivity.blockDeviceFiles.get(blockNum);
            		Log.d(LOG_TAG, "Advanced: Selected "+MainActivity.blockDeviceFiles.get(blockNum)+" as Default Block.");
            	}

				//Save Selected LUNs:
            	int count = 0;
            	String writeDefaults = "";
            	for(int i=0; i<cbCount; i++) {
            		if(cb[i].isChecked()) {
            			count++;
            			MainActivity.enabledLUNs[i] = true;
            			if(!writeDefaults.equals("")) writeDefaults += ",";
            			writeDefaults += i;
            		} else MainActivity.enabledLUNs[i] = false;
            	}
            	Log.d(LOG_TAG, "Advanced: Selected no. of LUNs - "+count);
            	if(count!=0 && !writeDefaults.isEmpty()) {
            		data.edit().putString(LUNsToUse, writeDefaults).apply();
            		MainActivity.numberOfLUNsUsed = count;
            		Log.d(LOG_TAG, "Saved Advanced Settings.");
            	} else {
            		Toast.makeText(getApplicationContext(), getString(R.string.toast_zero_luns), Toast.LENGTH_SHORT).show();
            		return;
            	}
            	finish();
            }
        });
		
		TextView blankLine = new TextView(this);
		blankLine.setText("");
		ll.addView(blankLine);
		ll.addView(save);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(false);
	}

	public void pickRootDir(View v) {
		// https://github.com/spacecowboy/NoNonsense-FilePicker
		Intent i = new Intent(this, FilePickerActivity.class);
		i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
		i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
		i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
		i.putExtra(FilePickerActivity.EXTRA_START_PATH, "/");
		startActivityForResult(i, ROOT_DIR_REQ_CODE);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ROOT_DIR_REQ_CODE && resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();
			memoryCardPath = uri.toString();
			dir.setText(getString(R.string.settings_layout_mediascan_path)+": "+uri.toString().replace("file://", ""));
		}else if(requestCode == BLOCK_FILE_REQ_CODE && resultCode == Activity.RESULT_OK) {
			addBlockManually(data.getData().toString().replace("file://",""));
		}
	}

	void initADs() {

		if(MainActivity.enableADs) {
		/*AdView mAdView = (AdView) findViewById(R.id.adViewInSettings);
		mAdView.loadAd(new AdRequest.Builder().build());*/

			AdView mAdView1 = (AdView) findViewById(R.id.adViewInSettingsBottom);
			mAdView1.loadAd(new AdRequest.Builder().build());
			Log.d(LOG_TAG, "Ads initialized in Settings Activity..");
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if(data==null) data= getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
		initADs();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.settings_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_reload:
				MainActivity.listAllLUNsAndBlocks = true;
				finish();
				return true;

			case android.R.id.home:
				onBackPressed();
				return true;

			case R.id.action_add_block:
				selectBlockManually();
				return true;

			default:
				//Toast.makeText(getApplicationContext(), (String) item.getTitle()+item.getItemId(), Toast.LENGTH_SHORT ).show();
				return super.onOptionsItemSelected(item);
		}
	}

	void selectBlockManually() {
		Intent i = new Intent(this, FilePickerActivity.class);
		i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
		i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
		i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
		i.putExtra(FilePickerActivity.EXTRA_START_PATH, "/");
		startActivityForResult(i, BLOCK_FILE_REQ_CODE);
	}

	void addBlockManually(final String path) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.alert_add_block_manually)
				.setMessage(getString(R.string.alert_add_block_manually_msg)+"\n"+path)
				.setPositiveButton(R.string.action_add, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						MainActivity.blockDeviceFiles.add(path);
						String blocks = data.getString(blocksList, null);
						blocks += "\n" + path; //Add the block to SharedPref
						data.edit().putString(blocksList, blocks).apply();
						if(MainActivity.blockRecommendation) {
							MainActivity.recommendedBlocks = Arrays.copyOf(MainActivity.recommendedBlocks, rbCount + 1);
							MainActivity.recommendedBlocks[rbCount] = false;
						}
						recreate();
					}
				})
				.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {

					}
				})
				.show();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}
}