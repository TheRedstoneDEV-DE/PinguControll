package me.theredstonedevde.mprisctl;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
public interface PlasmaBrowserExtension extends DBusInterface {
	String PlaybackStatus();
    void PlayPause();
    void Next();
    void Previous();
}
