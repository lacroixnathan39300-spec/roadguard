/*
 * Adapté d'UnknownAuto (https://github.com/GitMag/UnknownAuto) — MIT License, Copyright (c) 2026 GitMag.
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 */
package fr.roadguard.suite;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Tourne dans le processus lancé par Shizuku, avec l'identité « shell » (uid 2000, celle d'adb). C'est ce qui permet
 * à l'installateur d'Android, ouvert d'ici, d'accepter « installé par le Play Store » (com.android.vending) :
 * Android Auto n'affiche un GPS qu'avec cette marque. Relié par Shizuku.bindUserService.
 */
public final class ShellService extends IShellService.Stub {

    // Shizuku essaie d'abord (Context), puis le constructeur sans argument.
    public ShellService() {
    }

    public ShellService(Context context) {
    }

    @Override
    public void destroy() {
        System.exit(0);
    }

    /**
     * Réécrit l'installateur noté de [pkg] vers [installer] (ex. com.android.vending). L'installation, elle, garde
     * son initiateur : Android Auto veut initiateur = installateur du système ET installateur = Play Store, et c'est
     * ce second champ qui manque après une installation normale. L'identité shell (uid 2000) a INSTALL_PACKAGES,
     * donc le gestionnaire de paquets l'autorise. Essaie d'abord la commande pm, puis l'API cachée.
     */
    @Override
    public String setInstaller(String pkg, String installer) {
        String viaCmd = run(new String[]{"/system/bin/pm", "set-installer", pkg, installer});
        if (viaCmd != null && viaCmd.toLowerCase().contains("success")) {
            return "ok";
        }
        try {
            Object sm = Class.forName("android.os.ServiceManager").getMethod("getService", String.class).invoke(null, "package");
            Object pm = Class.forName("android.content.pm.IPackageManager$Stub")
                    .getMethod("asInterface", IBinder.class).invoke(null, sm);
            Method set = pm.getClass().getMethod("setInstallerPackageName", String.class, String.class);
            set.invoke(pm, pkg, installer);
            return "ok";
        } catch (InvocationTargetException e) {
            String cmd = viaCmd == null ? "" : " / pm: " + viaCmd.trim();
            return String.valueOf(e.getCause()) + cmd;
        } catch (Throwable t) {
            return String.valueOf(t);
        }
    }

    @Override
    public String run(String[] cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            InputStream in = p.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) >= 0) {
                if (out.size() < MAX_OUTPUT) {
                    out.write(buf, 0, Math.min(n, MAX_OUTPUT - out.size()));
                }
            }
            int code = p.waitFor();
            String s = out.toString("UTF-8");
            return code == 0 ? s : s + "\n[exit " + code + "]";
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Ouvre [intent] en tant que shell, avec [resultTo] (une activité de Pilote Suite) comme destinataire du résultat :
     * l'installateur voit alors Pilote Suite comme appelant et le shell comme lanceur (Android 14 et avant).
     * API cachée : ce processus n'est pas une appli, les restrictions ne s'appliquent pas.
     */
    @Override
    public String startForResult(Intent intent, IBinder resultTo, int requestCode) {
        try {
            Object atm = Build.VERSION.SDK_INT >= 29
                    ? Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null)
                    : ActivityManager.class.getMethod("getService").invoke(null);
            String type = intent.getType();
            for (Method m : atm.getClass().getMethods()) {
                if (!m.getName().equals("startActivity")) {
                    continue;
                }
                Class<?>[] p = m.getParameterTypes();
                Object[] args;
                if (p.length == 11 && p[3] == Intent.class) {
                    // API 30+ : (caller, callingPackage, callingFeatureId, intent, resolvedType,
                    //            resultTo, resultWho, requestCode, flags, profilerInfo, options)
                    args = new Object[]{null, SHELL, null, intent, type, resultTo, null, requestCode, 0, null, null};
                } else if (p.length == 10 && p[2] == Intent.class) {
                    // API 26-29 : la même sans callingFeatureId
                    args = new Object[]{null, SHELL, intent, type, resultTo, null, requestCode, 0, null, null};
                } else {
                    continue;
                }
                int res = (Integer) m.invoke(atm, args);
                return res >= 0 ? "ok" : "startActivity returned " + res;
            }
            return "no usable startActivity on " + atm.getClass().getName();
        } catch (InvocationTargetException e) {
            return String.valueOf(e.getCause());
        } catch (Throwable t) {
            return String.valueOf(t);
        }
    }

    private static final String SHELL = "com.android.shell";
    private static final int MAX_OUTPUT = 256 * 1024;
}
