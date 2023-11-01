package me.theredstonedevde.mprisctl;

import java.util.Map;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;
import me.theredstonedevde.types.Metadata;

public class MPRISctl {
	PlasmaBrowserExtension pbe;
	DBusConnection dbusconnection;
	String MPRIS_BUSNAME = "org.mpris.MediaPlayer2.plasma-browser-integration";
	String MPRIS_OBJECTPATH = "/org/mpris/MediaPlayer2";
	public void init() {
		try {
			DBusConnection dc = DBusConnectionBuilder.forSessionBus().build();
			dbusconnection = dc;
			pbe = (PlasmaBrowserExtension) dc.getRemoteObject(MPRIS_BUSNAME,
					MPRIS_OBJECTPATH, PlasmaBrowserExtension.class);
		} catch (DBusException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void playpause() {
		pbe.PlayPause();
	}
	public void next() {
		pbe.Next();
	}
	public void prev() {
		pbe.Previous();
	}
	public Metadata getMetadata(){
		try {

            // fetch properties
            Properties properties = dbusconnection.getRemoteObject(MPRIS_BUSNAME, MPRIS_OBJECTPATH, Properties.class);

            // get the 'Sessions', which returns a complex type
            final Map<String,Variant<?>> metadata = properties.Get("org.mpris.MediaPlayer2.Player", "Metadata");

			Metadata meta = new Metadata();
			meta.data=metadata;
			

			return meta;
        }catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
}
