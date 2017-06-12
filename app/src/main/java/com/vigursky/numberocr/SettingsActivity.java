package com.vigursky.numberocr;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.vigursky.numberocr.database.DetectionHistoryDBHelper;


/**
 * Created by vigursky on 09.03.2017.
 */

public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {
    public static final String TAG_PHONE_SWITCH_VALUE = "TAG_PHONE_SWITCH_VALUE";
    public static final String TAG_URL_SWITCH_VALUE = "TAG_URL_SWITCH_VALUE";
    public static final String TAG_HISTORY_SWITCH_VALUE = "TAG_HISTORY_SWITCH_VALUE";
    public static final String TAG_HISTORY_SEEK_VALUE = "TAG_HISTORY_SEEK_VALUE";
    private SharedPreferences mSharedPref;
    private Switch mSwitchPhone;
    private Switch mSwitchURL;
    private Switch mSwitchHistory;
    private SeekBar mSeekHistoryDepth;
    private TextView mTxtHistoryDepth;
    private String mHistoryDepthSeekBegin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean phoneSwitch_def   = mSharedPref.getBoolean(TAG_PHONE_SWITCH_VALUE, true);
        boolean urlSwitch_def   = mSharedPref.getBoolean(TAG_URL_SWITCH_VALUE, true);
        boolean histSwitch_def   = mSharedPref.getBoolean(TAG_HISTORY_SWITCH_VALUE, true);
        int     hisDepthSeek_def = mSharedPref.getInt(TAG_HISTORY_SEEK_VALUE,
                getResources().getInteger(R.integer.default_history_limit));

        setContentView(R.layout.settings_activity);
        mSwitchPhone = (Switch) findViewById(R.id.switch_phone);
        mSwitchURL = (Switch) findViewById(R.id.switch_url);
        mSwitchHistory = (Switch) findViewById(R.id.switch_history);
        mSeekHistoryDepth = (SeekBar) findViewById(R.id.seek_history_depth);
        mTxtHistoryDepth  = (TextView) findViewById(R.id.txt_history_depth);
        mHistoryDepthSeekBegin = getResources().getText(R.string.history_depth_value).toString();

        mSwitchPhone.setChecked(phoneSwitch_def);
        mSwitchURL.setChecked(urlSwitch_def);
        mSwitchHistory.setChecked(histSwitch_def);
        mSeekHistoryDepth.setProgress(hisDepthSeek_def);
        mTxtHistoryDepth.setText(String.format(mHistoryDepthSeekBegin, hisDepthSeek_def));

        mSwitchPhone.setOnCheckedChangeListener(this);
        mSwitchURL.setOnCheckedChangeListener(this);
        mSwitchHistory.setOnCheckedChangeListener(this);
        mSeekHistoryDepth.setOnSeekBarChangeListener(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.info_action_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.item_info:
                AlertDialog infoDialog = createInfoDialog();
                infoDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private AlertDialog createInfoDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setView(R.layout.info_dialog).setTitle(R.string.info).setCancelable(true).create();
        return dialog;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences.Editor editor = mSharedPref.edit();

        if(buttonView.getId() == R.id.switch_phone){
            editor.putBoolean(TAG_PHONE_SWITCH_VALUE, isChecked);
        }else if(buttonView.getId() == R.id.switch_url){
            editor.putBoolean(TAG_URL_SWITCH_VALUE, isChecked);
        }else if(buttonView.getId() == R.id.switch_history){
            editor.putBoolean(TAG_HISTORY_SWITCH_VALUE, isChecked);
        }
        editor.apply();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        mTxtHistoryDepth.setText(String.format(mHistoryDepthSeekBegin, progress));
        editor.putInt(TAG_HISTORY_SEEK_VALUE, progress);
        editor.apply();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        DetectionHistoryDBHelper detectionHistoryDBHelper = new DetectionHistoryDBHelper(getApplicationContext());
        detectionHistoryDBHelper.updateDetectionsLimitTrigger(seekBar.getProgress(), null);
    }
}
