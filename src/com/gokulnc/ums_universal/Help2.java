package com.gokulnc.ums_universal;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mikepenz.materialdrawer.Drawer;

public class Help2 extends AppCompatActivity {
	private Drawer result = null;
	private WebView mWebView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(false);
		mWebView = new WebView(this);
		mWebView.loadUrl("file:///android_asset/"+getString(R.string.asset_help_html));
		setContentView(mWebView);
		/*setContentView (R.layout.help);
		TextView ans1 = (TextView)findViewById(R.id.ans_1);

		
		String ans1Text = "This app is for <b>ROOTED devices</b> only!!<br>This app is for mounting your memory card as a USB Mass Storage (UMS) device on your computer, or TV or Audio/DVD Player or whatever.<br>"
				+ "This app will NOT mount your Internal Storage (since it's located in /data partition inside the ./media/0/ directory and the /data partition is formatted as EXT4).<br>"
				+ "If you really want to mount your Internal Storage as UMS, it can done using the <a href='http://google.com/search?q=usb+sharer+apk'>USB Sharer app</a> (but it'll be mounted as read-only, the reason why I didn't do it in this app).<br><br>";
		ans1.setText(Html.fromHtml(ans1Text));
		TextView ans2 = (TextView)findViewById(R.id.ans_2);

		String ans2Text =
				  "1. To enable Mass Storage, press 'Enable Mass Storage'.<br><br>"
				+ "2. Before disconnecting USB, eject from Computer first, then press the 'Disconnect Mass Storage' button.<br><br>";
		ans2.setText(Html.fromHtml(ans2Text));
		TextView ans3 = (TextView)findViewById(R.id.ans_3);

		String ans3Text =
				          "1. Your device's kernel doesn't have Mass Storage Gadget or FUSE drivers.<br><u>Solution:</u> Ask any developer of your device to compile a kernel with FUSE module and USB Gadget.<br><br>"
						+ "2. Your device doesn't have memory card support, or there's no memory card inserted.<br><br>"
						+ "3. Your device's memory card is encrypted or used as Internal Storage (for <b>Android 6.0</b> and above) by which it is formatted as an EXT4 partition and encrypted.<br><br>"
						+ "4. There might be something (like Anti-Virus) which doesn't allow to disable SE Linux Enforcements.<br><br>"
						+ "5. Your device might not be rooted properly.<br><br>"

						+ "For further help and more clarifications or if there's any suggestions/bugs found, click here: <h2><b><a href='http://forum.xda-developers.com/android/apps-games/app-universal-mass-storage-enabler-beta-t3240097'>Help & Support</a></b></h2>"
						+ "<br><br>Developed by: <b><a href='http://fb.com/gokulnc'>Gokul NC</a></b><br>";
		ans3.setText(Html.fromHtml(ans3Text));

	*/
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/*@Override
	public void onBackPressed() {
		//handle the back press :D close the drawer first and if the drawer is closed close the activity
		if (result != null && result.isDrawerOpen()) {
			result.closeDrawer();
		} else {
			super.onBackPressed();
		}
	}*/
}