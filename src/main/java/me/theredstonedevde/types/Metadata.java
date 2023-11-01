package me.theredstonedevde.types;

import java.util.Map;
import org.freedesktop.dbus.types.Variant;

public class Metadata {
        public Map<String,Variant<?>> data;

    public String getTitle(){
        Object title = data.get("xesam:title");
        return title.toString();
    }
    public String getArtist(){
        Object artist = data.get("xesam:artist");
        return artist.toString();
    }
    public String getAlbum(){
        Object album = data.get("xesam:album");
        if (album!=null) {
            return album.toString();
        }else{
            return null;
        }
    }
    public String getArtwork(){
        Object album = data.get("mpris:artUrl");
        return album.toString();
    }
}
