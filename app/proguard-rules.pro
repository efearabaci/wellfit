# AndroidX, Room, WorkManager, OkHttp and kotlinx.serialization ship consumer rules.
# Reflective discovery of CoroutineWorkers uses their Context/WorkerParameters constructor.
-keep class com.denge.app.integration.RecapWorker { public <init>(android.content.Context, androidx.work.WorkerParameters); }
