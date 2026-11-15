package com.example.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings

data class SocialMediaAppInfo(
    val packageName: String,
    val displayName: String,
    val categoryTag: String,
    val isInstalled: Boolean = false
)

object SocialMediaCatalog {

    val knownApps: List<SocialMediaAppInfo> = listOf(
        SocialMediaAppInfo("com.instagram.android", "Instagram", "Reels & Social"),
        SocialMediaAppInfo("com.google.android.youtube", "YouTube", "Shorts & Video"),
        SocialMediaAppInfo("com.facebook.katana", "Facebook", "Social Feed"),
        SocialMediaAppInfo("com.snapchat.android", "Snapchat", "Stories & Chat"),
        SocialMediaAppInfo("com.twitter.android", "X (Twitter)", "Microblogging"),
        SocialMediaAppInfo("com.reddit.frontpage", "Reddit", "Forums"),
        SocialMediaAppInfo("org.telegram.messenger", "Telegram", "Channels & Chat"),
        SocialMediaAppInfo("com.discord", "Discord", "Servers & Chat"),
        SocialMediaAppInfo("com.instagram.barcelona", "Threads", "Social Feed"),
        SocialMediaAppInfo("com.whatsapp", "WhatsApp", "Messaging"),
        SocialMediaAppInfo("in.mohalla.sharechat", "ShareChat", "Social Video"),
        SocialMediaAppInfo("in.mohalla.video", "Moj", "Short Video"),
        SocialMediaAppInfo("com.eterno.shortvideos", "Josh", "Short Video"),
        SocialMediaAppInfo("com.facebook.orca", "Messenger", "Messaging")
    )

    val defaultBlockedPackages: Set<String> = knownApps
        .filter { it.packageName != "com.whatsapp" }
        .map { it.packageName }
        .toSet()

    fun resolveInstalledStatus(context: Context): List<SocialMediaAppInfo> {
        val pm = context.packageManager
        return knownApps.map { app ->
            val installed = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(app.packageName, 0)
                }
                true
            } catch (_: Exception) {
                false
            }
            app.copy(isInstalled = installed)
        }.sortedByDescending { it.isInstalled }
    }

    fun labelForPackage(packageName: String): String {
        return knownApps.firstOrNull { it.packageName == packageName }?.displayName ?: packageName
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
