# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Data models used by Firestore serialisation
-keep class com.wishesapp.data.model.** { *; }

# Glance widget
-keep class androidx.glance.** { *; }

# Hilt — component-level annotations
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
# @EntryPoint interfaces are accessed reflectively via EntryPointAccessors
-keep @dagger.hilt.EntryPoint interface * { *; }
# @HiltWorker classes are instantiated reflectively by HiltWorkerFactory
-keep @dagger.hilt.android.HiltWorker class * { *; }

# WorkManager workers are instantiated by class name
-keep class * extends androidx.work.ListenableWorker { *; }
