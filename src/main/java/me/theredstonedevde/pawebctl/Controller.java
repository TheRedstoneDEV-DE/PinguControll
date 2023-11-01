package me.theredstonedevde.pawebctl;

import java.io.IOException;

public class Controller {
    public void setVolume(String id, int type, String vol){
        if (type == 1){
            ProcessBuilder processBuilder = new ProcessBuilder("pactl", "set-sink-input-volume", id, vol+"%");
            try {
                processBuilder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if(type == 0){
            ProcessBuilder processBuilder = new ProcessBuilder("pactl", "set-sink-volume", id, vol+"%");
            try {
                processBuilder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            System.err.println("ERR: unpossible exception: unused type provided");
        }
    }
}