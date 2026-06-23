package com.wishesapp.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.wishesapp.data.model.WidgetMode
import com.wishesapp.data.model.WidgetState
import com.wishesapp.data.repository.SpaceRepository
import com.wishesapp.data.repository.WidgetPrefsRepository
import com.wishesapp.data.repository.WishRepository
import com.wishesapp.data.repository.ImageUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private const val TAG = "WishWidgetWorker"
private const val KEY_TRIGGER = "trigger"
private const val TRIGGER_PUSH = "push"
private const val TRIGGER_SHUFFLE = "shuffle"
private const val TRIGGER_PERIODIC = "periodic"

@HiltWorker
class WishWidgetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val wishRepo: WishRepository,
    private val spaceRepo: SpaceRepository,
    private val widgetPrefsRepo: WidgetPrefsRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val trigger = inputData.getString(KEY_TRIGGER) ?: TRIGGER_PERIODIC
            val currentState = widgetPrefsRepo.widgetState.first()
            val space = spaceRepo.getMySpace() ?: return Result.success()
            val wishes = wishRepo.getWishesForWidget(space.id)

            if (wishes.isEmpty()) return Result.success()

            val wish = when {
                trigger == TRIGGER_PUSH || currentState.mode == WidgetMode.LATEST ->
                    wishes.first() // newest
                else ->
                    wishes.random() // random shuffle
            }

            // Decode and cache the widget-sized image to a file
            val imagePath = wish.imageSmall?.let { base64 ->
                ImageUtil.decodeBase64(base64)?.let { bitmap ->
                    ImageUtil.saveBitmapForWidget(context, bitmap, wish.id)
                }
            }

            val newState = WidgetState(
                mode = currentState.mode,
                wishId = wish.id,
                wishText = wish.text,
                authorName = wish.authorName,
                imagePath = imagePath,
            )
            widgetPrefsRepo.saveWidgetState(newState)
            WishWidget().updateAll(context)
            Log.d(TAG, "Widget updated: trigger=$trigger, wish=${wish.id}")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Widget update failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_PERIODIC = "wish_widget_periodic"
        private const val WORK_PUSH = "wish_widget_push"
        private const val WORK_SHUFFLE = "wish_widget_shuffle"

        /** Called by FCM service when a new wish arrives. Runs as an expedited job. */
        fun enqueueForPush(context: Context) {
            val request = OneTimeWorkRequestBuilder<WishWidgetWorker>()
                .setInputData(workDataOf(KEY_TRIGGER to TRIGGER_PUSH))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_PUSH, ExistingWorkPolicy.REPLACE, request)
        }

        /** Called when user taps "shuffle" on the widget. */
        fun enqueueForShuffle(context: Context) {
            val request = OneTimeWorkRequestBuilder<WishWidgetWorker>()
                .setInputData(workDataOf(KEY_TRIGGER to TRIGGER_SHUFFLE))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_SHUFFLE, ExistingWorkPolicy.REPLACE, request)
        }

        /** Schedule a periodic refresh (every 30 min). Used for RANDOM mode rotation. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<WishWidgetWorker>(30, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_TRIGGER to TRIGGER_PERIODIC))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /** Cancel the periodic refresh. */
        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_PERIODIC)
        }
    }
}
