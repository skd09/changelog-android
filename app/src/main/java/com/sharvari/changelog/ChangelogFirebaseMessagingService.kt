package com.sharvari.changelog

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sharvari.changelog.service.deeplink.DeepLinkManager
import com.sharvari.changelog.service.device.DeviceService
import timber.log.Timber

// Register in AndroidManifest.xml inside <application>:
//
//   <service
//       android:name=".ChangelogFirebaseMessagingService"
//       android:exported="false">
//       <intent-filter>
//           <action android:name="com.google.firebase.MESSAGING_EVENT" />
//       </intent-filter>
//   </service>

class ChangelogFirebaseMessagingService : FirebaseMessagingService() {

    // Called when Firebase issues a new or rotated token.
    // FcmTokenStore uses SharedPreferences so saveToken() is synchronous — no context needed.
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("New FCM token received")
        DeviceService.shared.onFCMTokenReceived(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.d("FCM message received: %s", message.messageId)
        DeepLinkManager.handleNotification(message.data)
    }
}
