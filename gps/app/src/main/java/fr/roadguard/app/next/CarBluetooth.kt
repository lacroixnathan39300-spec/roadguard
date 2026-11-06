package fr.roadguard.app.next

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.roadguard.core.DriveSettings

/**
 * Bluetooth de la voiture : à la connexion, Pilote démarre tout seul ; à la déconnexion, le trajet se termine
 * (voiture garée mémorisée, rappel « rien oublié ? »).
 */
class CarBluetoothReceiver : BroadcastReceiver() {
    @android.annotation.SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val settings = DriveSettings(context)
        val car = settings.carBluetooth.takeIf { it.isNotBlank() } ?: return
        @Suppress("DEPRECATION")
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
        if (device.address != car) return
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> AutoStart.launch(context)
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> DriveService.session?.endTrip()
        }
    }
}
