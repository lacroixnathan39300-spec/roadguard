// Petit service que Shizuku lance avec l'identité « shell » (uid 2000, comme adb), pour ouvrir l'installateur
// d'Android depuis cette identité. Adapté d'UnknownAuto (https://github.com/GitMag/UnknownAuto), licence MIT,
// Copyright (c) 2026 GitMag.
package fr.roadguard.suite;

import android.content.Intent;

interface IShellService {
    // Convention Shizuku : destroy() doit porter ce numéro pour que la déconnexion l'arrête.
    void destroy() = 16777114;
    // Lance une commande en tant que shell ; rend sa sortie, suivie de « [exit N] » en cas d'échec ; null si impossible.
    String run(in String[] cmd) = 1;
    // Ouvre une activité en tant que shell, avec l'activité de Pilote Suite comme destinataire du résultat ;
    // rend « ok », ou la raison de l'échec.
    String startForResult(in Intent intent, IBinder resultTo, int requestCode) = 2;
}
