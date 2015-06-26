package com.mysentosa.android.sg;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.crittercism.app.Crittercism;
import com.flurry.android.FlurryAgent;
import com.mysentosa.android.sg.asynctask.GetEventsPromotionsAsyncTask;
import com.mysentosa.android.sg.asynctask.GetLocationsAsyncTask;
import com.mysentosa.android.sg.asynctask.GetNodesEdgesAsyncTask;
import com.mysentosa.android.sg.asynctask.GetRegisterTokenAsyncTask;
import com.mysentosa.android.sg.provider.SentosaContentProvider;
import com.mysentosa.android.sg.utils.Const;
import com.mysentosa.android.sg.utils.Const.FlurryStrings;
import com.mysentosa.android.sg.utils.SentosaUtils;

public class SplashScreenActivity extends Activity {

	private final int SPLASH_TIMEOUT = 1500;
	private boolean appFirstLaunch = true;
	private SharedPreferences mPrefs;
	
	private int eventId = -1;
	private Intent mainIntent;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		ImageView iv = new ImageView(this);
		iv.setImageResource(R.drawable.splash);
		iv.setScaleType(ScaleType.FIT_XY);
		this.setContentView(iv);
		Crittercism.initialize(getApplicationContext(), Const.CRITTERCISM_KEY);

		mainIntent = this.getIntent();
//		eventId = this.getIntent().getIntExtra(EventsAndPromotionsDetailActivity.EVENT_ID, -1);
		mPrefs = getSharedPreferences(ProfileAndSettingsActivity.USER_DETAILS_PREFS, MODE_PRIVATE);
		appFirstLaunch = !mPrefs.getBoolean(ProfileAndSettingsActivity.USER_DETAILS_PREFS_ENTRY_CREATED, false);

				
		// This is to delete the database if the database version has been
		// increased or to install it in the beginning
		SentosaContentProvider.setupDatabase(this);

		// clear all the old images from the cache
		((SentosaApplication) this.getApplication()).mImageCache.clearExpiredCacheImages();

		// ((SentosaApplication)this.getApplication()).mImageCache.clearCaches();
		// download the events and promotions data
		if (!SentosaContentProvider.IS_PRELOAD_DATABASE_CREATION) {			
			new GetEventsPromotionsAsyncTask(this).execute();
			new GetNodesEdgesAsyncTask(this).execute();
			new GetLocationsAsyncTask(this).execute();
		}
		if (appFirstLaunch) {			
			new GetRegisterTokenAsyncTask(SplashScreenActivity.this).execute();			
		}

		// new GetTicketEventAsyncTask(this).execute();
		Handler mSplashHandler = new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				Intent mIntent;
				if (appFirstLaunch) {								
					SharedPreferences mPrefs = getSharedPreferences(ProfileAndSettingsActivity.USER_DETAILS_PREFS, MODE_PRIVATE);
					final SharedPreferences.Editor edit = mPrefs.edit();
					edit.putBoolean(ProfileAndSettingsActivity.USER_DETAILS_PREFS_ENTRY_CREATED, true);
					edit.commit();
					mIntent = new Intent(SplashScreenActivity.this, ProfileAndSettingsActivity.class);
					mIntent.putExtra(ProfileAndSettingsActivity.FROM_FIRST_LAUNCH, true);

//					if (eventId != -1)
//						mIntent.putExtra(EventsAndPromotionsDetailActivity.EVENT_ID, eventId);
				} else {
					boolean isRegistered = mPrefs.getBoolean(ProfileAndSettingsActivity.USER_DETAILS_PREFS_IS_REGISTERED, false);
					if (!isRegistered) {
						String name = mPrefs.getString(ProfileAndSettingsActivity.USER_DETAILS_PREFS_NAME, null);
						if (SentosaUtils.isValidString(name)) {
							String email = mPrefs.getString(ProfileAndSettingsActivity.USER_DETAILS_PREFS_EMAIL, null);
							String mobile = mPrefs.getString(ProfileAndSettingsActivity.USER_DETAILS_PREFS_MOBILE, null);
							String gender = mPrefs.getString(ProfileAndSettingsActivity.USER_DETAILS_PREFS_GENDER, null);
							String birthDate = mPrefs.getString(ProfileAndSettingsActivity.USER_DETAILS_PREFS_DOB, null);
							String postalCode = mPrefs.getString(ProfileAndSettingsActivity.USER_DETAILS_PREFS_POSTAL_CODE, null);

							if (SentosaUtils.isValidString(birthDate)) {
							    birthDate = birthDate.replace("/", "-");
							}
							ProfileAndSettingsActivity.registerVisitor(SplashScreenActivity.this, name, email, mobile, gender, postalCode, birthDate);
						}
					}

					mIntent = new Intent(SplashScreenActivity.this,
							NavigationManagerActivity.class);
					String toClass = mainIntent.getStringExtra(NavigationManagerActivity.ACTIVITY_TO_START);
					eventId = mainIntent.getIntExtra(NavigationManagerActivity.ACTIVITY_TO_START_ID, -1);
					if (toClass == null || toClass.length() == 0) {
					    mIntent.putExtra(
                                NavigationManagerActivity.ACTIVITY_TO_START,
                                HomeActivity.class.getName());
                        FlurryAgent.logEvent(FlurryStrings.HomePageLaunch);
					} else {
					    if (eventId != -1){
					        mIntent.putExtra(
	                                NavigationManagerActivity.ACTIVITY_TO_START_ID, eventId);
					    }
					    if (toClass.equals(EventsAndPromotionsActivity.class.getName())) {
					        mIntent.putExtra(
	                                NavigationManagerActivity.ACTIVITY_TO_START,
	                                EventsAndPromotionsActivity.class.getName());
                            mIntent.putExtra(EventsAndPromotionsActivity.CURRENT_TYPE, mainIntent.getIntExtra(
                                    EventsAndPromotionsActivity.CURRENT_TYPE, EventsAndPromotionsActivity.TYPE_EVENT));
					    } else if (toClass.equals(ThingsToDoCategoryListActivity.class.getName())) {
					        mIntent.putExtra(
                                    NavigationManagerActivity.ACTIVITY_TO_START,
                                    ThingsToDoCategoryListActivity.class.getName());
					        mIntent.putExtra(ThingsToDoCategoryListActivity.CATEGORY_NAME_FOR_NODES,
					                mainIntent.getStringExtra(ThingsToDoCategoryListActivity.CATEGORY_NAME_FOR_NODES));
					    } else if (toClass.equals(TicketsActivity.class.getName())) {
					        mIntent.putExtra(
                                    NavigationManagerActivity.ACTIVITY_TO_START,
                                    TicketsActivity.class.getName());
					        mIntent.putExtra(TicketsActivity.TICKET_TYPE, mainIntent.getStringExtra(TicketsActivity.TICKET_TYPE));
					    }
					}

				}
				SplashScreenActivity.this.startActivity(mIntent);
				overridePendingTransition(R.anim.fadein, R.anim.fadeout);
				finish();
				return true;
			}
		});
		mSplashHandler.sendEmptyMessageDelayed(0, SPLASH_TIMEOUT);

	}

}
