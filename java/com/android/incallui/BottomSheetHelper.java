/* Copyright (c) 2017-2018, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.incallui;

import android.support.v4.app.FragmentManager;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telecom.Call.Details;
import android.view.View;

import com.android.dialer.common.LogUtil;
import com.android.incallui.call.DialerCall;
import com.android.incallui.call.state.DialerCallState;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import org.codeaurora.ims.QtiCallConstants;
import org.codeaurora.ims.utils.QtiCallUtils;
import org.codeaurora.ims.utils.QtiImsExtUtils;

public class BottomSheetHelper implements PrimaryCallTracker.PrimaryCallChangeListener {

   private ConcurrentHashMap<String,Boolean> moreOptionsMap;
   private ExtBottomSheetFragment moreOptionsSheet;
   private Context mContext;
   private DialerCall mCall;
   private PrimaryCallTracker mPrimaryCallTracker;
   private Resources mResources;
   private static BottomSheetHelper mHelper;

   private BottomSheetHelper() {
     LogUtil.d("BottomSheetHelper"," ");
   }

   public static BottomSheetHelper getInstance() {
     if (mHelper == null) {
       mHelper = new BottomSheetHelper();
     }
     return mHelper;
   }

   public void setUp(Context context) {
     LogUtil.d("BottomSheetHelper","setUp");
     mContext = context;
     mResources = context.getResources();
     final String[][] moreOptions = getMoreOptionsFromRes(R.array.bottom_sheet_more_options);
     moreOptionsMap = prepareSheetOptions(moreOptions);
     mPrimaryCallTracker = new PrimaryCallTracker();
     InCallPresenter.getInstance().addListener(mPrimaryCallTracker);
     InCallPresenter.getInstance().addIncomingCallListener(mPrimaryCallTracker);
     mPrimaryCallTracker.addListener(this);
   }

   public void tearDown() {
     LogUtil.d("BottomSheetHelper","tearDown");
     InCallPresenter.getInstance().removeListener(mPrimaryCallTracker);
     InCallPresenter.getInstance().removeIncomingCallListener(mPrimaryCallTracker);
     if (mPrimaryCallTracker != null) {
       mPrimaryCallTracker.removeListener(this);
       mPrimaryCallTracker = null;
     }
     mContext = null;
     mResources = null;
     moreOptionsMap = null;
   }

   public void updateMap() {
     if (mPrimaryCallTracker == null) {
       LogUtil.w("BottomSheetHelper.updateMap : ", "PrimaryCallTracker is null");
       return;
     }
     mCall = mPrimaryCallTracker.getPrimaryCall();
     LogUtil.i("BottomSheetHelper.updateMap","mCall = " + mCall);
   }

   // Utility function which converts options from string array to HashMap<String,Boolean>
   private static ConcurrentHashMap<String,Boolean> prepareSheetOptions(String[][] answerOptArray) {
     ConcurrentHashMap<String,Boolean> map = new ConcurrentHashMap<String,Boolean>();
     for (int iter = 0; iter < answerOptArray.length; iter ++) {
       map.put(answerOptArray[iter][0],Boolean.valueOf(answerOptArray[iter][1]));
     }
     return map;
   }

   public void showBottomSheet(FragmentManager manager) {
     LogUtil.d("BottomSheetHelper.showBottomSheet","moreOptionsMap: " + moreOptionsMap);
     moreOptionsSheet = ExtBottomSheetFragment.newInstance(moreOptionsMap);
     moreOptionsSheet.show(manager, null);
   }

   public void dismissBottomSheet() {
     final InCallActivity inCallActivity = InCallPresenter.getInstance().getActivity();
     if (inCallActivity == null || !inCallActivity.isVisible()) {
       LogUtil.w("BottomSheetHelper.dismissBottomSheet",
               "In call activity is either null or not visible");
       return;
     }
     if (moreOptionsSheet != null && moreOptionsSheet.isVisible()) {
       moreOptionsSheet.dismiss();
       moreOptionsSheet = null;
     }
   }

   public void optionSelected(@Nullable String text) {
     //callback for bottomsheet clicks
     LogUtil.d("BottomSheetHelper.optionSelected","text : " + text);
     moreOptionsSheet = null;
   }

   public void sheetDismissed() {
     LogUtil.d("BottomSheetHelper.sheetDismissed"," ");
     moreOptionsSheet = null;
   }

   private String[][] getMoreOptionsFromRes(final int resId) {
     TypedArray typedArray = mResources.obtainTypedArray(resId);
     String[][] array = new String[typedArray.length()][];
     for  (int iter = 0;iter < typedArray.length(); iter++) {
       int id = typedArray.getResourceId(iter, 0);
       if (id > 0) {
         array[iter] = mResources.getStringArray(id);
       }
     }
     typedArray.recycle();
     return array;
   }

   public boolean shallShowMoreButton(Activity activity) {
     if (mPrimaryCallTracker != null) {
       DialerCall call = mPrimaryCallTracker.getPrimaryCall();
       if (call != null && activity != null) {
         int primaryCallState = call.getState();
         return !(activity.isInMultiWindowMode()
           || call.isEmergencyCall()
           || DialerCallState.isDialing(primaryCallState)
           || DialerCallState.CONNECTING == primaryCallState
           || DialerCallState.DISCONNECTING == primaryCallState
           || call.hasSentVideoUpgradeRequest()
           || !(getPhoneIdExtra(call) != QtiCallConstants.INVALID_PHONE_ID));
       }
     }
     LogUtil.w("BottomSheetHelper shallShowMoreButton","returns false");
     return false;
   }

   public void updateMoreButtonVisibility(boolean isVisible, View moreOptionsMenuButton) {
     if (moreOptionsMenuButton == null) {
       return;
     }

     if (isVisible) {
       moreOptionsMenuButton.setVisibility(View.VISIBLE);
     } else {
       dismissBottomSheet();
       moreOptionsMenuButton.setVisibility(View.GONE);
     }
   }

   private int getPhoneIdExtra(DialerCall call) {
     final Bundle extras = call.getExtras();
     return ((extras == null) ? QtiCallConstants.INVALID_PHONE_ID :
         extras.getInt(QtiImsExtUtils.QTI_IMS_PHONE_ID_EXTRA_KEY,
         QtiCallConstants.INVALID_PHONE_ID));
   }

    /**
    * This API should be called only when there is a call.
    * Caller should handle if INVALID_PHONE_ID is returned.
    */
   public int getPhoneId() {
     if (mPrimaryCallTracker == null) {
       LogUtil.w("BottomSheetHelper.getPhoneId", "mPrimaryCallTracker is null.");
       return QtiCallConstants.INVALID_PHONE_ID;
     }

     final DialerCall call = mPrimaryCallTracker.getPrimaryCall();
     if (call == null) {
       LogUtil.w("BottomSheetHelper.getPhoneId", "primaryCall is null.");
       return QtiCallConstants.INVALID_PHONE_ID;
     }

     final int phoneId = getPhoneIdExtra(call);
     LogUtil.d("BottomSheetHelper.getPhoneId", "phoneId : " + phoneId);
     return phoneId;
   }

    @Override
    public void onPrimaryCallChanged(DialerCall call) {
      LogUtil.d("BottomSheetHelper.onPrimaryCallChanged", "");
      dismissBottomSheet();
      updateMap();
    }
}
