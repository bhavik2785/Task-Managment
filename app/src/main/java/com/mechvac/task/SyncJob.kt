package com.mechvac.task

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context

/** Silent backup check about every 15 minutes (no notification unless there is something new). */
class SyncJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        val app = applicationContext
        Thread {
            try {
                Feed.run(app)   // also starts/stops the optional always-on check and refreshes the Firebase token
                if (Prefs(app).keepAlive && !LiveService.running) LiveService.start(app)
            } finally {
                jobFinished(params, false)
            }
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    companion object {
        private const val JOB_ID = 5151

        fun schedule(ctx: Context) {
            val js = ctx.getSystemService(JobScheduler::class.java) ?: return
            if (js.allPendingJobs.any { it.id == JOB_ID }) return
            val info = JobInfo.Builder(JOB_ID, ComponentName(ctx, SyncJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(15 * 60 * 1000L)
                .build()
            js.schedule(info)
        }

        fun cancel(ctx: Context) {
            ctx.getSystemService(JobScheduler::class.java)?.cancel(JOB_ID)
        }
    }
}
