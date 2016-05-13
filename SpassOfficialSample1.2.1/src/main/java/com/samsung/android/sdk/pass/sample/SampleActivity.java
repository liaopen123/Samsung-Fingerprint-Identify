package com.samsung.android.sdk.pass.sample;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pass.Spass;
import com.samsung.android.sdk.pass.SpassFingerprint;
import com.samsung.android.sdk.pass.SpassInvalidStateException;

public class SampleActivity extends Activity implements Handler.Callback {

    private SpassFingerprint mSpassFingerprint;
    private Spass mSpass;
    private Context mContext;
    private ListView mListView;
    private List<String> mItemArray = new ArrayList<String>();
    private ArrayAdapter<String> mListAdapter;
    private ArrayList<Integer> designatedFingers = null;
    private ArrayList<Integer> designatedFingersDialog = null;


    private boolean needRetryIdentify = false;
    private boolean onReadyIdentify = false;
    private boolean onReadyEnroll = false;
    private boolean hasRegisteredFinger = false;

    private boolean isFeatureEnabled_fingerprint = false;
    private boolean isFeatureEnabled_index = false;
    private boolean isFeatureEnabled_uniqueId = false;
    private boolean isFeatureEnabled_custom = false;
    private boolean isFeatureEnabled_backupPw = false;

    private Handler mHandler;
    private static final int MSG_AUTH = 1000;
    private static final int MSG_AUTH_UI_WITH_PW = 1001;
    private static final int MSG_AUTH_UI_WITHOUT_PW = 1002;
    private static final int MSG_CANCEL = 1003;
    private static final int MSG_REGISTER = 1004;
    private static final int MSG_GET_NAME = 1005;
    private static final int MSG_GET_UNIQUEID = 1006;
    private static final int MSG_AUTH_INDEX = 1007;
    private static final int MSG_AUTH_UI_INDEX = 1008;
    private static final int MSG_AUTH_UI_CUSTOM_LOGO = 1009;
    private static final int MSG_AUTH_UI_CUSTOM_TRANSPARENCY = 1010;
    private static final int MSG_AUTH_UI_CUSTOM_DISMISS = 1011;
    private static final int MSG_AUTH_UI_CUSTOM_BUTTON_STANDBY = 1012;

    private Button mButton_Auth;
    private Button mButton_Auth_UI_WithPW;
    private Button mButton_Auth_UI_WithoutPW;
    private Button mButton_Cancel;
    private Button mButton_Register;
    private Button mButton_GetName;
    private Button mButton_GetUnique;
    private Button mButton_Auth_Index;
    private Button mButton_Auth_UI_Index;
    private Button mButton_Auth_UI_Custon_logo;
    private Button mButton_Auth_UI_Custon_Trans;
    private Button mButton_Auth_UI_Custon_Dismiss;
    private Button mButton_Auth_UI_Custon_Button_Standby;
    private SparseArray<Button> mButtonList = null;

    private BroadcastReceiver mPassReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (SpassFingerprint.ACTION_FINGERPRINT_RESET.equals(action)) {
                Toast.makeText(mContext, "all fingerprints are removed", Toast.LENGTH_SHORT).show();
            } else if (SpassFingerprint.ACTION_FINGERPRINT_REMOVED.equals(action)) {
                int fingerIndex = intent.getIntExtra("fingerIndex", 0);
                Toast.makeText(mContext, fingerIndex + " fingerprints is removed", Toast.LENGTH_SHORT).show();
            } else if (SpassFingerprint.ACTION_FINGERPRINT_ADDED.equals(action)) {
                int fingerIndex = intent.getIntExtra("fingerIndex", 0);
                Toast.makeText(mContext, fingerIndex + " fingerprints is added", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(SpassFingerprint.ACTION_FINGERPRINT_RESET);
        filter.addAction(SpassFingerprint.ACTION_FINGERPRINT_REMOVED);
        filter.addAction(SpassFingerprint.ACTION_FINGERPRINT_ADDED);
        mContext.registerReceiver(mPassReceiver, filter);
    };

    private void unregisterBroadcastReceiver() {
        try {
            if (mContext != null) {
                mContext.unregisterReceiver(mPassReceiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetAll() {
        designatedFingers = null;
        needRetryIdentify = false;
        onReadyIdentify = false;
        onReadyEnroll = false;
        hasRegisteredFinger = false;
    }

    private SpassFingerprint.IdentifyListener mIdentifyListener = new SpassFingerprint.IdentifyListener() {
        @Override
        public void onFinished(int eventStatus) {
            log("identify finished : reason =" + getEventStatusName(eventStatus));
            int FingerprintIndex = 0;
            String FingerprintGuideText = null;
            try {
                FingerprintIndex = mSpassFingerprint.getIdentifiedFingerprintIndex();
            } catch (IllegalStateException ise) {
                log(ise.getMessage());
            }
            if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS) {
                log("onFinished() : Identify authentification Success with FingerprintIndex : " + FingerprintIndex);
            } else if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS) {
                log("onFinished() : Password authentification Success");
            } else if (eventStatus == SpassFingerprint.STATUS_OPERATION_DENIED) {
                log("onFinished() : Authentification is blocked because of fingerprint service internally.");
            } else if (eventStatus == SpassFingerprint.STATUS_USER_CANCELLED) {
                log("onFinished() : User cancel this identify.");
            } else if (eventStatus == SpassFingerprint.STATUS_TIMEOUT_FAILED) {
                log("onFinished() : The time for identify is finished.");
            } else if (eventStatus == SpassFingerprint.STATUS_QUALITY_FAILED) {
                log("onFinished() : Authentification Fail for identify.");
                needRetryIdentify = true;
                FingerprintGuideText = mSpassFingerprint.getGuideForPoorQuality();
                Toast.makeText(mContext, FingerprintGuideText, Toast.LENGTH_SHORT).show();
            } else {
                log("onFinished() : Authentification Fail for identify");
                needRetryIdentify = true;
            }
            if (!needRetryIdentify) {
                resetIdentifyIndex();
            }
        }

        @Override
        public void onReady() {
            log("identify state is ready");
        }

        @Override
        public void onStarted() {
            log("User touched fingerprint sensor");
        }

        @Override
        public void onCompleted() {
            log("the identify is completed");
            onReadyIdentify = false;
            if (needRetryIdentify) {
                needRetryIdentify = false;
                mHandler.sendEmptyMessageDelayed(MSG_AUTH, 100);
            }
        }
    };

    private SpassFingerprint.IdentifyListener mIdentifyListenerDialog = new SpassFingerprint.IdentifyListener() {
        @Override
        public void onFinished(int eventStatus) {
            log("identify finished : reason =" + getEventStatusName(eventStatus));
            int FingerprintIndex = 0;
            boolean isFailedIdentify = false;
            onReadyIdentify = false;
            try {
                FingerprintIndex = mSpassFingerprint.getIdentifiedFingerprintIndex();
            } catch (IllegalStateException ise) {
                log(ise.getMessage());
            }
            if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS) {
                log("onFinished() : Identify authentification Success with FingerprintIndex : " + FingerprintIndex);
            } else if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS) {
                log("onFinished() : Password authentification Success");
            } else if (eventStatus == SpassFingerprint.STATUS_USER_CANCELLED
                    || eventStatus == SpassFingerprint.STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE) {
                log("onFinished() : User cancel this identify.");
            } else if (eventStatus == SpassFingerprint.STATUS_TIMEOUT_FAILED) {
                log("onFinished() : The time for identify is finished.");
            } else if (!mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_AVAILABLE_PASSWORD)) {
                if (eventStatus == SpassFingerprint.STATUS_BUTTON_PRESSED) {
                    log("onFinished() : User pressed the own button");
                    Toast.makeText(mContext, "Please connect own Backup Menu", Toast.LENGTH_SHORT).show();
                }
            } else {
                log("onFinished() : Authentification Fail for identify");
                isFailedIdentify = true;
            }
            if (!isFailedIdentify) {
                resetIdentifyIndexDialog();
            }
        }

        @Override
        public void onReady() {
            log("identify state is ready");
        }

        @Override
        public void onStarted() {
            log("User touched fingerprint sensor");
        }

        @Override
        public void onCompleted() {
            log("the identify is completed");
        }
    };
    private SpassFingerprint.RegisterListener mRegisterListener = new SpassFingerprint.RegisterListener() {
        @Override
        public void onFinished() {
            onReadyEnroll = false;
            log("RegisterListener.onFinished()");
        }
    };

    private static String getEventStatusName(int eventStatus) {
        switch (eventStatus) {
            case SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS:
                return "STATUS_AUTHENTIFICATION_SUCCESS";
            case SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS:
                return "STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS";
            case SpassFingerprint.STATUS_TIMEOUT_FAILED:
                return "STATUS_TIMEOUT";
            case SpassFingerprint.STATUS_SENSOR_FAILED:
                return "STATUS_SENSOR_ERROR";
            case SpassFingerprint.STATUS_USER_CANCELLED:
                return "STATUS_USER_CANCELLED";
            case SpassFingerprint.STATUS_QUALITY_FAILED:
                return "STATUS_QUALITY_FAILED";
            case SpassFingerprint.STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE:
                return "STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE";
            case SpassFingerprint.STATUS_BUTTON_PRESSED:
                return "STATUS_BUTTON_PRESSED";
            case SpassFingerprint.STATUS_OPERATION_DENIED:
                return "STATUS_OPERATION_DENIED";
            case SpassFingerprint.STATUS_AUTHENTIFICATION_FAILED:
            default:
                return "STATUS_AUTHENTIFICATION_FAILED";
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_AUTH:
                startIdentify();
                break;
            case MSG_AUTH_UI_WITH_PW:
                startIdentifyDialog(true);
                break;
            case MSG_AUTH_UI_WITHOUT_PW:
                startIdentifyDialog(false);
                break;
            case MSG_CANCEL:
                cancelIdentify();
                break;
            case MSG_REGISTER:
                registerFingerprint();
                break;
            case MSG_GET_NAME:
                getFingerprintName();
                break;
            case MSG_GET_UNIQUEID:
                getFingerprintUniqueID();
                break;
            case MSG_AUTH_INDEX:
                makeIdentifyIndex(1);
                startIdentify();
                break;
            case MSG_AUTH_UI_INDEX:
                makeIdentifyIndexDialog(2);
                makeIdentifyIndexDialog(3);
                startIdentifyDialog(false);
                break;
            case MSG_AUTH_UI_CUSTOM_LOGO:
                setDialogTitleAndLogo();
                startIdentifyDialog(false);
                break;
            case MSG_AUTH_UI_CUSTOM_TRANSPARENCY:
                setDialogTitleAndTransparency();
                startIdentifyDialog(false);
                break;
            case MSG_AUTH_UI_CUSTOM_DISMISS:
                setDialogTitleAndDismiss();
                startIdentifyDialog(false);
                break;
            case MSG_AUTH_UI_CUSTOM_BUTTON_STANDBY:
                setDialogButtonAndStandbyText();
                startIdentifyDialog(false);
                break;
        }
        return true;
    }

    private void startIdentify() {
        if (onReadyIdentify == false) {
            try {
                onReadyIdentify = true;
                if (mSpassFingerprint != null) {
                    setIdentifyIndex();
                    mSpassFingerprint.startIdentify(mIdentifyListener);
                }
                if (designatedFingers != null) {
                    log("Please identify finger to verify you with " + designatedFingers.toString() + " finger");
                } else {
                    log("Please identify finger to verify you");
                }
            } catch (SpassInvalidStateException ise) {
                onReadyIdentify = false;
                resetIdentifyIndex();
                if (ise.getType() == SpassInvalidStateException.STATUS_OPERATION_DENIED) {
                    log("Exception: " + ise.getMessage());
                }
            } catch (IllegalStateException e) {
                onReadyIdentify = false;
                resetIdentifyIndex();
                log("Exception: " + e);
            }
        } else {
            log("The previous request is remained. Please finished or cancel first");
        }
    }

    private void startIdentifyDialog(boolean backup) {
        if (onReadyIdentify == false) {
            onReadyIdentify = true;
            try {
                if (mSpassFingerprint != null) {
                    setIdentifyIndexDialog();
                    mSpassFingerprint.startIdentifyWithDialog(SampleActivity.this, mIdentifyListenerDialog, backup);
                }
                if (designatedFingersDialog != null) {
                    log("Please identify finger to verify you with " + designatedFingersDialog.toString() + " finger");
                } else {
                    log("Please identify finger to verify you");
                }
            } catch (IllegalStateException e) {
                onReadyIdentify = false;
                resetIdentifyIndexDialog();
                log("Exception: " + e);
            }
        } else {
            log("The previous request is remained. Please finished or cancel first");
        }
    }

    private void cancelIdentify() {
        if (onReadyIdentify == true) {
            try {
                if (mSpassFingerprint != null) {
                    mSpassFingerprint.cancelIdentify();
                }
                log("cancelIdentify is called");
            } catch (IllegalStateException ise) {
                log(ise.getMessage());
            }
            onReadyIdentify = false;
            needRetryIdentify = false;
        } else {
            log("Please request Identify first");
        }
    }

    private void registerFingerprint() {
        if (onReadyIdentify == false) {
            if (onReadyEnroll == false) {
                onReadyEnroll = true;
                if (mSpassFingerprint != null) {
                    mSpassFingerprint.registerFinger(SampleActivity.this, mRegisterListener);
                }
                log("Jump to the Enroll screen");
            } else {
                log("Please wait and try to register again");
            }
        } else {
            log("Please cancel Identify first");
        }
    }

    private void getFingerprintName() {
        SparseArray<String> mList = null;
        log("=Fingerprint Name=");
        if (mSpassFingerprint != null) {
            mList = mSpassFingerprint.getRegisteredFingerprintName();
        }
        if (mList == null) {
            log("Registered fingerprint is not existed.");
        } else {
            for (int i = 0; i < mList.size(); i++) {
                int index = mList.keyAt(i);
                String name = mList.get(index);
                log("index " + index + ", Name is " + name);
            }
        }
    }

    private void getFingerprintUniqueID() {
        SparseArray<String> mList = null;
        try {
            log("=Fingerprint Unique ID=");
            if (mSpassFingerprint != null) {
                mList = mSpassFingerprint.getRegisteredFingerprintUniqueID();
            }
            if (mList == null) {
                log("Registered fingerprint is not existed.");
            } else {
                for (int i = 0; i < mList.size(); i++) {
                    int index = mList.keyAt(i);
                    String ID = mList.get(index);
                    log("index " + index + ", Unique ID is " + ID);
                }
            }
        } catch (IllegalStateException ise) {
            log(ise.getMessage());
        }
    }

    private void setIdentifyIndex() {
        if (isFeatureEnabled_index) {
            if (mSpassFingerprint != null && designatedFingers != null) {
                mSpassFingerprint.setIntendedFingerprintIndex(designatedFingers);
            }
        }
    }

    private void makeIdentifyIndex(int i) {
        if (designatedFingers == null) {
            designatedFingers = new ArrayList<Integer>();
        }
        for(int j = 0; j< designatedFingers.size(); j++){
            if(i == designatedFingers.get(j)){
                return;
            } 
        }
        designatedFingers.add(i);
    }

    private void resetIdentifyIndex() {
        designatedFingers = null;
    }
    private void setIdentifyIndexDialog() {
        if (isFeatureEnabled_index) {
            if (mSpassFingerprint != null && designatedFingersDialog != null) {
                mSpassFingerprint.setIntendedFingerprintIndex(designatedFingersDialog);
            }
        }
    }

    private void makeIdentifyIndexDialog(int i) {
        if (designatedFingersDialog == null) {
            designatedFingersDialog = new ArrayList<Integer>();
        }
        for(int j = 0; j< designatedFingersDialog.size(); j++){
            if(i == designatedFingersDialog.get(j)){
                return;
            } 
        }
        designatedFingersDialog.add(i);
    }

    private void resetIdentifyIndexDialog() {
        designatedFingersDialog = null;
    }
    private void setDialogTitleAndLogo() {
        if (isFeatureEnabled_custom) {
            try {
                if (mSpassFingerprint != null) {
                    mSpassFingerprint.setDialogTitle("Customized Dialog With Logo", 0x000000);
                    mSpassFingerprint.setDialogIcon("logo_image");
                }
            } catch (IllegalStateException ise) {
                log(ise.getMessage());
            }
        }
    }

    private void setDialogTitleAndTransparency() {
        if (isFeatureEnabled_custom) {
            try {
                if (mSpassFingerprint != null) {
                    mSpassFingerprint.setDialogTitle("Customized Dialog With Transparency", 0x000000);
                    mSpassFingerprint.setDialogBgTransparency(0);
                }
            } catch (IllegalStateException ise) {
                log(ise.getMessage());
            }
        }
    }

    private void setDialogTitleAndDismiss() {
        if (isFeatureEnabled_custom) {
            try {
                if (mSpassFingerprint != null) {
                    mSpassFingerprint.setDialogTitle("Customized Dialog With Setting Dialog dismiss", 0x000000);
                    mSpassFingerprint.setCanceledOnTouchOutside(true);
                }
            } catch (IllegalStateException ise) {
                log(ise.getMessage());
            }
        }
    }

    private void setDialogButtonAndStandbyText() {
        if (!isFeatureEnabled_backupPw) {
            try {
                if (mSpassFingerprint != null) {
                    mSpassFingerprint.setDialogButton("OWN BUTTON");
                    mSpassFingerprint.changeStandbyString("Touch your fingerprint or press the button for launching own menu");
                }
            } catch (IllegalStateException ise) {
                log(ise.getMessage());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        mListAdapter = new ArrayAdapter<String>(this, R.layout.list_entry, mItemArray);
        mListView = (ListView)findViewById(R.id.listView1);
        mHandler = new Handler(this);

        if (mListView != null) {
            mListView.setAdapter(mListAdapter);
        }
        mSpass = new Spass();

        try {
            mSpass.initialize(SampleActivity.this);
        } catch (SsdkUnsupportedException e) {
            log("Exception: " + e);
        } catch (UnsupportedOperationException e) {
            log("Fingerprint Service is not supported in the device");
        }
        isFeatureEnabled_fingerprint = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT);

        if (isFeatureEnabled_fingerprint) {
            mSpassFingerprint = new SpassFingerprint(SampleActivity.this);
            log("Fingerprint Service is supported in the device.");
            log("SDK version : " + mSpass.getVersionName());
        } else {
            logClear();
            log("Fingerprint Service is not supported in the device.");
            return;
        }

        isFeatureEnabled_index = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_FINGER_INDEX);
        isFeatureEnabled_custom = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_CUSTOMIZED_DIALOG);
        isFeatureEnabled_uniqueId = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_UNIQUE_ID);
        isFeatureEnabled_backupPw = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_AVAILABLE_PASSWORD);

        registerBroadcastReceiver();
        setButton();

    }

    private Button.OnClickListener onButtonClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            logClear();

            switch (v.getId()) {
                case R.id.identify:
                    mHandler.sendEmptyMessage(MSG_AUTH);
                    break;
                case R.id.identifyDialogWithPW:
                    mHandler.sendEmptyMessage(MSG_AUTH_UI_WITH_PW);
                    break;
                case R.id.identifyDialogWithoutPW:
                    mHandler.sendEmptyMessage(MSG_AUTH_UI_WITHOUT_PW);
                    break;
                case R.id.cancel:
                    mHandler.sendEmptyMessage(MSG_CANCEL);
                    break;
                case R.id.registerFinger:
                    mHandler.sendEmptyMessage(MSG_REGISTER);
                    break;
                case R.id.getRegisteredFingerprintName:
                    mHandler.sendEmptyMessage(MSG_GET_NAME);
                    break;
                case R.id.getRegisteredFingerprintID:
                    mHandler.sendEmptyMessage(MSG_GET_UNIQUEID);
                    break;
                case R.id.identifyWithIndex:
                    mHandler.sendEmptyMessage(MSG_AUTH_INDEX);
                    break;
                case R.id.identifyDialogWithIndex:
                    mHandler.sendEmptyMessage(MSG_AUTH_UI_INDEX);
                    break;
                case R.id.identifyDialogTitleAndLogo:
                    mHandler.sendEmptyMessage(MSG_AUTH_UI_CUSTOM_LOGO);
                    break;
                case R.id.identifyDialogTransparency:
                    mHandler.sendEmptyMessage(MSG_AUTH_UI_CUSTOM_TRANSPARENCY);
                    break;
                case R.id.identifyDialogSetDialogDismiss:
                    mHandler.sendEmptyMessage(MSG_AUTH_UI_CUSTOM_DISMISS);
                    break;
                case R.id.identifyDialogButtonAndStandbyText:
                    mHandler.sendEmptyMessage(MSG_AUTH_UI_CUSTOM_BUTTON_STANDBY);
                    break;
            }
        }
    };

    private void setButton() {
        mButton_Auth = (Button)findViewById(R.id.identify);
        mButton_Auth_UI_WithPW = (Button)findViewById(R.id.identifyDialogWithPW);
        mButton_Auth_UI_WithoutPW = (Button)findViewById(R.id.identifyDialogWithoutPW);
        mButton_Cancel = (Button)findViewById(R.id.cancel);
        mButton_Register = (Button)findViewById(R.id.registerFinger);
        mButton_GetName = (Button)findViewById(R.id.getRegisteredFingerprintName);
        mButton_GetUnique = (Button)findViewById(R.id.getRegisteredFingerprintID);
        mButton_Auth_Index = (Button)findViewById(R.id.identifyWithIndex);
        mButton_Auth_UI_Index = (Button)findViewById(R.id.identifyDialogWithIndex);
        mButton_Auth_UI_Custon_logo = (Button)findViewById(R.id.identifyDialogTitleAndLogo);
        mButton_Auth_UI_Custon_Trans = (Button)findViewById(R.id.identifyDialogTransparency);
        mButton_Auth_UI_Custon_Dismiss = (Button)findViewById(R.id.identifyDialogSetDialogDismiss);
        mButton_Auth_UI_Custon_Button_Standby = (Button)findViewById(R.id.identifyDialogButtonAndStandbyText);

        mButtonList = new SparseArray<Button>();
        mButtonList.put(R.id.identify, mButton_Auth);
        mButtonList.put(R.id.identifyDialogWithPW, mButton_Auth_UI_WithPW);
        mButtonList.put(R.id.identifyDialogWithoutPW, mButton_Auth_UI_WithoutPW);
        mButtonList.put(R.id.cancel, mButton_Cancel);
        mButtonList.put(R.id.registerFinger, mButton_Register);
        mButtonList.put(R.id.getRegisteredFingerprintName, mButton_GetName);
        mButtonList.put(R.id.getRegisteredFingerprintID, mButton_GetUnique);
        mButtonList.put(R.id.identifyWithIndex, mButton_Auth_Index);
        mButtonList.put(R.id.identifyDialogWithIndex, mButton_Auth_UI_Index);
        mButtonList.put(R.id.identifyDialogTitleAndLogo, mButton_Auth_UI_Custon_logo);
        mButtonList.put(R.id.identifyDialogTransparency, mButton_Auth_UI_Custon_Trans);
        mButtonList.put(R.id.identifyDialogSetDialogDismiss, mButton_Auth_UI_Custon_Dismiss);
        mButtonList.put(R.id.identifyDialogButtonAndStandbyText, mButton_Auth_UI_Custon_Button_Standby);
    }

    private void setButtonEnable() {
        if (mSpassFingerprint == null || mButtonList == null) {
            return;
        }
        try {
            hasRegisteredFinger = mSpassFingerprint.hasRegisteredFinger();
        } catch (UnsupportedOperationException e) {
            log("Fingerprint Service is not supported in the device");
        }
        if (hasRegisteredFinger) {
            log("The registered Fingerprint is existed");
        } else {
            log("Please register finger first");
        }

        final int N = mButtonList.size();
        for (int i = 0; i < N; i++) {
            int id = mButtonList.keyAt(i);
            Button button = (Button)findViewById(id);
            if (button != null) {
                button.setOnClickListener(onButtonClick);
                button.setTextAppearance(mContext, R.style.ButtonStyle);
                if (!isFeatureEnabled_fingerprint) {
                    button.setEnabled(false);
                } else if (!hasRegisteredFinger && button != mButton_Register) {
                    button.setEnabled(false);
                } else {
                    button.setEnabled(true);
                }
            }
        }
        if (!isFeatureEnabled_backupPw) {
            mButton_Auth_UI_WithPW.setEnabled(false);
        }
        if (!isFeatureEnabled_uniqueId) {
            mButton_GetUnique.setEnabled(false);
        }
        if (!isFeatureEnabled_index) {
            mButton_Auth_Index.setEnabled(false);
            mButton_Auth_UI_Index.setEnabled(false);
        }
        if (!isFeatureEnabled_custom) {
            mButton_Auth_UI_Custon_logo.setEnabled(false);
            mButton_Auth_UI_Custon_Trans.setEnabled(false);
            mButton_Auth_UI_Custon_Dismiss.setEnabled(false);
        }
        if (isFeatureEnabled_backupPw) {
            mButton_Auth_UI_Custon_Button_Standby.setEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        setButtonEnable();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBroadcastReceiver();
        resetAll();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void log(String text) {
        final String txt = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mItemArray.add(0, txt);
                mListAdapter.notifyDataSetChanged();
            }
        });
    }

    private void logClear() {
        if (mItemArray != null) {
            mItemArray.clear();
        }
        if (mListAdapter != null) {
            mListAdapter.notifyDataSetChanged();
        }
    }
}
