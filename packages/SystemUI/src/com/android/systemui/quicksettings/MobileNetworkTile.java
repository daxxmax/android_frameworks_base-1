package com.android.systemui.quicksettings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsController;
import com.android.systemui.statusbar.phone.QuickSettingsContainerView;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.NetworkSignalChangedCallback;

import static com.android.internal.util.cm.QSUtils.deviceSupportsMobileData;

public class MobileNetworkTile extends QuickSettingsTile implements NetworkSignalChangedCallback{

    private static final int NO_OVERLAY = 0;
    private static final int DISABLED_OVERLAY = -1;

    private boolean mEnabled;
    private String mDescription;
    private int mDataTypeIconId = NO_OVERLAY;
    private String dataContentDescription;
    private String signalContentDescription;
    private boolean wifiOn = false;

    private ConnectivityManager mCm;

    public MobileNetworkTile(Context context, QuickSettingsController qsc) {
        super(context, qsc, R.layout.quick_settings_tile_rssi);

        mCm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mCm.getMobileDataEnabled()) {
                    updateOverlayImage(NO_OVERLAY); // None, onMobileDataSignalChanged will set final overlay image
                    mCm.setMobileDataEnabled(true);
                } else {
                    updateOverlayImage(DISABLED_OVERLAY);
                    mCm.setMobileDataEnabled(false);
                }
            }
        };

        mOnLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings$DataUsageSummaryActivity"));
                startSettingsActivity(intent);
                return true;
            }
        };
    }

    @Override
    void onPostCreate() {
        NetworkController controller = new NetworkController(mContext);
        controller.addNetworkSignalChangedCallback(this);
        updateTile();
        super.onPostCreate();
    }

    @Override
    public void updateResources() {
        updateTile();
        super.updateResources();
    }

    private synchronized void updateTile() {
        Resources r = mContext.getResources();
        dataContentDescription = mEnabled && (mDataTypeIconId > 0) && !wifiOn
                ? dataContentDescription
                : r.getString(R.string.accessibility_no_data);
        mLabel = mEnabled
                ? removeTrailingPeriod(mDescription)
                : r.getString(R.string.quick_settings_rssi_emergency_only);
    }

    @Override
    public void onWifiSignalChanged(boolean enabled, int wifiSignalIconId,
            String wifitSignalContentDescriptionId, String description) {
        wifiOn = enabled;
    }

    @Override
    public void onMobileDataSignalChanged(boolean enabled,
            int mobileSignalIconId, String mobileSignalContentDescriptionId,
            int dataTypeIconId, String dataTypeContentDescriptionId,
            String description) {
        if (deviceSupportsMobileData(mContext)) {
            // TODO: If view is in awaiting state, disable
            Resources r = mContext.getResources();
            mDrawable = enabled && (mobileSignalIconId > 0)
                    ? mobileSignalIconId
                    : R.drawable.ic_qs_signal_no_signal;
            signalContentDescription = enabled && (mobileSignalIconId > 0)
                    ? signalContentDescription
                    : r.getString(R.string.accessibility_no_signal);

            // Determine the overlay image
            if (enabled && (dataTypeIconId > 0) && !wifiOn) {
                mDataTypeIconId = dataTypeIconId;
            } else if (!mCm.getMobileDataEnabled()) {
                mDataTypeIconId = DISABLED_OVERLAY;
            } else {
                mDataTypeIconId = NO_OVERLAY;
            }

            mEnabled = enabled;
            mDescription = description;

            updateResources();
        }
    }

    @Override
    public void onAirplaneModeChanged(boolean enabled) {
    }

    @Override
    void updateQuickSettings() {
        TextView tv = (TextView) mTile.findViewById(R.id.rssi_textview);
        ImageView iv = (ImageView) mTile.findViewById(R.id.rssi_image);

        iv.setImageResource(mDrawable);
        updateOverlayImage(mDataTypeIconId);
        tv.setText(mLabel);
        tv.setTextSize(1, mTileTextSize);
        if (mTileTextColor != -2) {
            tv.setTextColor(mTileTextColor);
        }
        mTile.setContentDescription(mContext.getResources().getString(
                R.string.accessibility_quick_settings_mobile,
                signalContentDescription, dataContentDescription,
                mLabel));
    }

    void updateOverlayImage(int dataTypeIconId) {
        ImageView iov = (ImageView) mTile.findViewById(R.id.rssi_overlay_image);
        if (dataTypeIconId > 0) {
            iov.setImageResource(dataTypeIconId);
        } else if (dataTypeIconId == DISABLED_OVERLAY) {
            iov.setImageResource(R.drawable.ic_qs_signal_data_off);
        } else {
            iov.setImageDrawable(null);
        }
    }

    // Remove the period from the network name
    public static String removeTrailingPeriod(String string) {
        if (string == null) return null;
        final int length = string.length();
        if (string.endsWith(".")) {
            string.substring(0, length - 1);
        }
        return string;
    }

}
