/**
 Copyright (c) 2015-2018 The Linux Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are
 met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above
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

import android.content.Context;
import android.widget.Toast;
import android.telecom.VideoProfile;
import com.android.incallui.call.CallList;
import com.android.incallui.call.DialerCall;

/**
 * This class contains Qti specific utiltity functions.
 */
public class QtiCallUtils {

    private static String LOG_TAG = "QtiCallUtils";
   /**
     * Displays the string corresponding to the resourceId as a Toast on the UI
     */
    public static void displayToast(Context context, int resourceId) {
      if (context == null) {
          Log.w(LOG_TAG, "displayToast context is null");
          return;
      }
      displayToast(context, context.getResources().getString(resourceId));
    }

    /**
     * Displays the message as a Toast on the UI
     */
    public static void displayToast(Context context, String msg) {
      if (context == null) {
          Log.w(LOG_TAG, "displayToast context is null");
          return;
      }
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

   /**
     * Checks the boolean flag in config file to figure out if we are going to use Qti extension or
     * not
     */
    public static boolean useExt(Context context) {
        if (context == null) {
            Log.w(context, "Context is null...");
        }
        return context != null && context.getResources().getBoolean(R.bool.video_call_use_ext);
    }

    /**
     * Converts the call type to string
     */
    public static String callTypeToString(int callType) {
        switch (callType) {
            case VideoProfile.STATE_BIDIRECTIONAL:
                return "VT";
            case VideoProfile.STATE_TX_ENABLED:
                return "VT_TX";
            case VideoProfile.STATE_RX_ENABLED:
                return "VT_RX";
        }
        return "";
    }

    public static boolean isVideoBidirectional(DialerCall call) {
        return (call != null && call.getVideoState() == VideoProfile.STATE_BIDIRECTIONAL);
    }


    public static boolean isVideoTxOnly(DialerCall call) {
        return (call != null && call.getVideoState() == VideoProfile.STATE_TX_ENABLED);
    }

    public static boolean isVideoRxOnly(DialerCall call) {
        return (call != null && call.getVideoState() == VideoProfile.STATE_RX_ENABLED);
    }

    /**
     * Returns true if the CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO is set to false.
     * Note that - CAPABILITY_SUPPORTS_DOWNGRADE_TO_VOICE_LOCAL and
     * CAPABILITY_SUPPORTS_DOWNGRADE_TO_VOICE_REMOTE maps to
     * CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO
     */
    public static boolean hasVoiceCapabilities(DialerCall call) {
        return call != null &&
                !call.can(android.telecom.Call.Details.CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO);
    }

    /**
     * Returns true if local has the VT Transmit and if remote capability has VT Receive set i.e.
     * Local can transmit and remote can receive
     */
    public static boolean hasTransmitVideoCapabilities(DialerCall call) {
        return call != null &&
                call.can(android.telecom.Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_TX)
                && call.can(android.telecom.Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_RX);
    }

    /**
     * Returns true if local has the VT Receive and if remote capability has VT Transmit set i.e.
     * Local can transmit and remote can receive
     */
    public static boolean hasReceiveVideoCapabilities(DialerCall call) {
        return call != null &&
                call.can(android.telecom.Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_RX)
                && call.can(android.telecom.Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_TX);
    }

     /**
      * Returns true if both voice and video capabilities (see above) are set
      */
     public static boolean hasVoiceOrVideoCapabilities(DialerCall call) {
         return hasVoiceCapabilities(call) || hasTransmitVideoCapabilities(call)
                 || hasReceiveVideoCapabilities(call);
     }

    public static CharSequence getLabelForIncomingWifiVideoCall(Context context) {
        final DialerCall call = getIncomingOrActiveCall();

        if (call == null) {
            return context.getString(R.string.contact_grid_incoming_wifi_video_call);
        }

        final int requestedVideoState = call.getVideoTech().getRequestedVideoState();

        if (QtiCallUtils.isVideoRxOnly(call)
            || requestedVideoState == VideoProfile.STATE_RX_ENABLED) {
            return context.getString(R.string.incoming_wifi_video_rx_call);
        } else if (QtiCallUtils.isVideoTxOnly(call)
            || requestedVideoState == VideoProfile.STATE_TX_ENABLED) {
            return context.getString(R.string.incoming_wifi_video_tx_call);
        } else {
            return context.getString(R.string.contact_grid_incoming_wifi_video_call);
        }
    }

    public static CharSequence getLabelForIncomingVideoCall(Context context) {
        final DialerCall call = getIncomingOrActiveCall();
        if (call == null) {
            return context.getString(R.string.contact_grid_incoming_video_call);
        }

        final int requestedVideoState = call.getVideoTech().getRequestedVideoState();

        if (QtiCallUtils.isVideoRxOnly(call)
            || requestedVideoState == VideoProfile.STATE_RX_ENABLED) {
            return context.getString(R.string.incoming_video_rx_call);
        } else if (QtiCallUtils.isVideoTxOnly(call)
            || requestedVideoState == VideoProfile.STATE_TX_ENABLED) {
            return context.getString(R.string.incoming_video_tx_call);
        } else {
            return context.getString(R.string.contact_grid_incoming_video_call);
        }
    }

    private static DialerCall getIncomingOrActiveCall() {
        CallList callList = InCallPresenter.getInstance().getCallList();
        if (callList == null) {
           return null;
        } else {
           return callList.getIncomingOrActive();
        }
    }
}
