package com.sharvari.changelog

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sharvari.changelog.data.service.DeviceService

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
        println("🔔 New FCM token received")
        DeviceService.shared.onFCMTokenReceived(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        println("📩 FCM message received: ${message.messageId}")
    }
}