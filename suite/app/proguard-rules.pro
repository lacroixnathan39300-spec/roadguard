# Service lancé par Shizuku dans un autre processus, par son nom : ni renommé, ni retiré.
-keep class fr.roadguard.suite.ShellService { *; }
-keep class fr.roadguard.suite.IShellService { *; }
-keep class fr.roadguard.suite.IShellService$* { *; }
-keep class fr.roadguard.suite.ApkProvider { *; }
# Client Shizuku.
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-dontwarn rikka.**
-dontwarn moe.shizuku.**
-dontwarn android.**
-dontwarn androidx.**
