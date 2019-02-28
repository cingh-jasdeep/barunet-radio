package com.example.android.uamp.media.notifications.barunet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.example.android.uamp.media.R
import com.example.android.uamp.media.extensions.*
import com.example.android.uamp.media.notifications.sample.NOW_PLAYING_CHANNEL

/**
 * Helper class to encapsulate code for building notifications.
 */
class RadioNotificationBuilder(private val context: Context) {
    private val platformNotificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val playAction = NotificationCompat.Action(
            R.drawable.exo_controls_play,
            context.getString(R.string.notification_play),
            MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                    PlaybackStateCompat.ACTION_PLAY))
    private val pauseAction = NotificationCompat.Action(
            R.drawable.exo_controls_pause,
            context.getString(R.string.notification_pause),
            MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                    PlaybackStateCompat.ACTION_PAUSE))
    private val prevAction = NotificationCompat.Action(
            R.drawable.exo_controls_previous,
            context.getString(R.string.notification_previous),
            MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
    private val nextAction = NotificationCompat.Action(
            R.drawable.exo_controls_next,
            context.getString(R.string.notification_next),
            MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
    private val stopPendingIntent =
            MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                    PlaybackStateCompat.ACTION_STOP)
    private val stopAction = NotificationCompat.Action(
            R.drawable.exo_notification_stop,
            context.getString(R.string.notification_stop),
            stopPendingIntent)

    fun buildNotification(sessionToken: MediaSessionCompat.Token): Notification {
        if (shouldCreateNowPlayingChannel()) {
            createNowPlayingChannel()
        }

        val controller = MediaControllerCompat(context, sessionToken)
        val description = controller.metadata.description
        val playbackState = controller.playbackState

        val builder = NotificationCompat.Builder(context, NOW_PLAYING_CHANNEL)

        // Only add actions for play, stop, based on what's enabled.
        var currActionIndex = 0
        val compactViewActionIndexes: MutableList<Int> = mutableListOf()

        if (playbackState.isSkipToPreviousEnabled) {
            builder.addAction(prevAction)
            compactViewActionIndexes.add(currActionIndex)
            ++currActionIndex
        }
        if (playbackState.isPlaying) { // only one of the two will be added to compact view
            builder.addAction(pauseAction)
            compactViewActionIndexes.add(currActionIndex)
            ++currActionIndex
        } else if (playbackState.isPlayEnabled) {
            builder.addAction(playAction)
            compactViewActionIndexes.add(currActionIndex)
            ++currActionIndex
        }
        if (playbackState.isSkipToNextEnabled) {
            builder.addAction(nextAction)
            compactViewActionIndexes.add(currActionIndex)//max 3 actions
            ++currActionIndex
        }
        if (playbackState.isPlaying) { // only one of the two will be added to compact view
            builder.addAction(stopAction)
            ++currActionIndex
        }

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
                .setCancelButtonIntent(stopPendingIntent)
                .setMediaSession(sessionToken)
                .setShowActionsInCompactView(*compactViewActionIndexes.toIntArray())
                .setShowCancelButton(true)

        if (playbackState.isBuffering) {
            builder.setContentText(context.getString(R.string.notification_buffering))
        } else {
            builder.setContentText(description.subtitle)
        }


        return builder.setContentIntent(controller.sessionActivity)
                .setContentTitle(description.title)
                .setDeleteIntent(stopPendingIntent)
                .setLargeIcon(description.iconBitmap)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setStyle(mediaStyle)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
    }

    private fun shouldCreateNowPlayingChannel() =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !nowPlayingChannelExists()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun nowPlayingChannelExists() =
            platformNotificationManager.getNotificationChannel(NOW_PLAYING_CHANNEL) != null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNowPlayingChannel() {
        val notificationChannel = NotificationChannel(NOW_PLAYING_CHANNEL,
                context.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW)
                .apply {
                    description = context.getString(R.string.notification_channel_description)
                }

        platformNotificationManager.createNotificationChannel(notificationChannel)
    }
}