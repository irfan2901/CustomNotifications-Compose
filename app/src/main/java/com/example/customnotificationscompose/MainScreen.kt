package com.example.customnotificationscompose

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val notificationTitle = remember { mutableStateOf("") }
    val notificationMessage = remember { mutableStateOf("") }
    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                sendNotification(context, notificationTitle.value, notificationMessage.value)
            } else {
                Toast.makeText(context, "Permission denied...", Toast.LENGTH_SHORT).show()
            }
        }

    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = notificationTitle.value,
            onValueChange = { notificationTitle.value = it },
            placeholder = { Text("Title") })
        Spacer(modifier.height(10.dp))
        TextField(
            value = notificationMessage.value,
            onValueChange = { notificationMessage.value = it }, placeholder = { Text("Message") })
        Spacer(modifier.height(10.dp))
        Button(onClick = {
            checkAndRequestPermission(
                context,
                permissionLauncher,
                notificationTitle.value,
                notificationMessage.value
            )
        }) {
            Text("Send notification")
        }
    }
}

fun checkAndRequestPermission(
    context: Context,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    title: String,
    message: String
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            sendNotification(context, title, message)
        }
    } else {
        sendNotification(context, title, message)
    }
}

fun sendNotification(context: Context, title: String, message: String) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        val actionOneIntent = Intent(context, MyBroadcastReceiver::class.java).apply {
            action = "ACTION_1"
        }

        val actionOnePendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            actionOneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionTwoIntent = Intent(context, MyBroadcastReceiver::class.java).apply {
            action = "ACTION_2"
        }

        val actionTwoPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            actionTwoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activityIntent = Intent(context, SecondActivity::class.java).apply {
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_MESSAGE", message)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val activityPendingIntent = PendingIntent.getActivity(
            context,
            2,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remoteViews = RemoteViews(context.packageName, R.layout.custom_notification).apply {
            setTextViewText(R.id.notification_title, title)
            setTextViewText(R.id.notification_message, message)
            setOnClickPendingIntent(R.id.action_one_button, actionOnePendingIntent)
            setOnClickPendingIntent(R.id.action_two_button, actionTwoPendingIntent)
        }

        val notification = NotificationCompat.Builder(context, "notification_channel")
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(activityPendingIntent)
            .setCustomContentView(remoteViews)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(3, notification.build())
        }
    }
}
