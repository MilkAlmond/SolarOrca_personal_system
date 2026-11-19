package com.yujing.tool.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yujing.tool.YujingApplication
import com.yujing.tool.data.ApiClient

class ReminderReceiptWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = (applicationContext as YujingApplication).prefs
        val id = inputData.getLong(KEY_ID, -1)
        val result = inputData.getString(KEY_RESULT) ?: ReminderAlarmReceiver.RESULT_FAILED
        if (id <= 0) {
            return Result.success()
        }
        if (prefs.token == null) {
            prefs.addPendingReceipt(id, result)
            return Result.success()
        }
        return try {
            ApiClient(prefs).receipt(id, result)
            prefs.clearPendingReceipt(id)
            Result.success()
        } catch (ex: Exception) {
            if (runAttemptCount < 1) {
                // Retry once; the server records the failure on the second attempt.
                Result.retry()
            } else {
                prefs.addPendingReceipt(id, ReminderAlarmReceiver.RESULT_FAILED)
                Result.success()
            }
        }
    }

    companion object {
        private const val KEY_ID = "reminderId"
        private const val KEY_RESULT = "result"

        fun enqueue(context: Context, id: Long, result: String) {
            val request = OneTimeWorkRequestBuilder<ReminderReceiptWorker>()
                .setInputData(workDataOf(KEY_ID to id, KEY_RESULT to result))
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
