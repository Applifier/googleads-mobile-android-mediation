// Copyright 2016 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.unity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.Keep;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.BannerErrorCode;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

/**
 * The {@link UnityBannerAd} is used to load Unity Banner ads and mediate the callbacks between Google
 * Mobile Ads SDK and Unity Ads SDK.
 */
@Keep
public class UnityBannerAd extends UnityMediationAdapter implements MediationBannerAdapter {

    /**
     * Placement ID for banner if requested.
     */
    private String bannerPlacementId;

    /**
     * Game ID, required for loading Unity Ads.
     */
    private String gameId;

    /**
     * The view for the banner instance.
     */
    private BannerView mBannerView;

    /**
     * Callback object for Google's Banner Lifecycle.
     */
    private MediationBannerListener mMediationBannerListener;

    /**
     * Activity needed to initialize Unity Ads.
     */
    private Activity activity;

    /**
     * Context needed to load Unity Ads.
     */
    private Context context;

    /**
     * Size of Unity banner ad.
     */
    private AdSize adSize;

    /**
     * BannerView.IListener instance.
     */
    private BannerView.IListener mUnityBannerListener = new BannerView.Listener() {
        @Override
        public void onBannerLoaded(BannerView bannerView) {
            Log.v(TAG, "Unity Ads finished loading banner ad for placement ID '" + mBannerView.getPlacementId() + "'.");
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdLoaded(UnityBannerAd.this);
            }
        }

        @Override
        public void onBannerClick(BannerView bannerView) {
            Log.v(TAG, "Unity Ads banner for placement ID '" + bannerPlacementId + "' was clicked.");
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdClicked(UnityBannerAd.this);
            }
        }

        @Override
        public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo bannerErrorInfo) {
            Log.w(TAG, "Unity Ads failed to load banner ad for placement ID '" + mBannerView.getPlacementId()
                    +  "'. Error: " + bannerErrorInfo.errorMessage);
            if (mMediationBannerListener != null) {
                if (bannerErrorInfo.errorCode == BannerErrorCode.NO_FILL) {
                    mMediationBannerListener.onAdFailedToLoad(UnityBannerAd.this, AdRequest.ERROR_CODE_NO_FILL);
                } else {
                    mMediationBannerListener.onAdFailedToLoad(UnityBannerAd.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        }

        @Override
        public void onBannerLeftApplication(BannerView bannerView) {
            mMediationBannerListener.onAdLeftApplication(UnityBannerAd.this);
        }
    };

    /**
     * Creates a UnityBannerAd.
     */
    public UnityBannerAd(Context context, MediationBannerListener listener, Bundle serverParameters,
                         AdSize adSize, MediationAdRequest adRequest, Bundle mediationExtras)
    {
        mMediationBannerListener = listener;

        gameId = serverParameters.getString(KEY_GAME_ID);
        bannerPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);

        if (!isValidIds(gameId, bannerPlacementId)) {
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdFailedToLoad(UnityBannerAd.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (context == null || !(context instanceof Activity)) {
            Log.e(TAG, "Unity Ads failed to load banner ad for placement ID '" + bannerPlacementId
                    + "': Context is not an Activity. Unity Ads requires an Activity context to load ads.");
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdFailedToLoad(UnityBannerAd.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }
        activity = (Activity) context;
        this.context = context;
        this.adSize = adSize;
    }

    /**
     * Checks whether or not the provided Unity Ads IDs are valid.
     *
     * @param gameId      Unity Ads Game ID to be verified.
     * @param placementId Unity Ads Placement ID to be verified.
     * @return {@code true} if all the IDs provided are valid.
     */
    private static boolean isValidIds(String gameId, String placementId) {
        if (TextUtils.isEmpty(gameId) || TextUtils.isEmpty(placementId)) {
            String ids = TextUtils.isEmpty(gameId) ? TextUtils.isEmpty(placementId)
                    ? "Game ID and Placement ID" : "Game ID" : "Placement ID";
            Log.w(TAG, "Unity Ads failed to load banner ad. " + ids + " cannot be empty.");

            return false;
        }

        return true;
    }

    @Override
    public void onDestroy() {
        if(mBannerView != null) {
            mBannerView.destroy();
        }
        mBannerView = null;
        mMediationBannerListener = null;
        mUnityBannerListener = null;
    }

    @Override
    public void onPause() {}

    @Override
    public void onResume() {}

    public void requestBannerAd() {

        UnityInitializer.getInstance().initializeUnityAds(activity, gameId, new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                Log.d(UnityAdapter.TAG, "Unity Ads successfully initialized, can now load " +
                        "banner ad for placement ID '" + bannerPlacementId + "' in game '" + gameId + "'.");

                float density = context.getResources().getDisplayMetrics().density;
                int bannerWidth = Math.round(adSize.getWidthInPixels(context) / density);
                int bannerHeight = Math.round(adSize.getHeightInPixels(context) / density);

                UnityBannerSize size = new UnityBannerSize(bannerWidth, bannerHeight);

                if (adSize == null || size == null)
                {
                    Log.e(UnityAdapter.TAG, "Unity banner ad failed to load for placement ID '"
                            + bannerPlacementId + "' in game '" + gameId +  "': ad size is null");
                    if (mMediationBannerListener != null) {
                        mMediationBannerListener.onAdFailedToLoad(UnityBannerAd.this,
                                AdRequest.ERROR_CODE_INTERNAL_ERROR);
                    }
                }

                if (mBannerView == null){
                    mBannerView = new BannerView((Activity)context, bannerPlacementId, size);
                }

                mBannerView.setListener(mUnityBannerListener);
                mBannerView.load();
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError
                                                       unityAdsInitializationError, String s) {
                Log.e(UnityAdapter.TAG, "Unity Ads initialization failed: [" +
                        unityAdsInitializationError + "] " + s + ", cannot load banner ad for placement ID '"
                        + bannerPlacementId + "' in game '" + gameId + "'.");
                if (mMediationBannerListener != null) {
                    mMediationBannerListener.onAdFailedToLoad(UnityBannerAd.this,
                            AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        });
    }


    @Override
    public void requestBannerAd(Context context, MediationBannerListener mediationBannerListener, Bundle bundle,
                                AdSize adSize, MediationAdRequest mediationAdRequest, Bundle bundle1) {

    }

    @Override
    public View getBannerView() {
        return mBannerView;
    }

}