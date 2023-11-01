package me.theredstonedevde.pawebctl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PulseAudioSinkInfo {
    public List<SinkInput> get() {
        List<SinkInput> sinkInputs = new ArrayList<>();
        try {
            // Create a ProcessBuilder to run the pactl command
            ProcessBuilder processBuilder = new ProcessBuilder("pactl", "list", "sinks");
            Process process = processBuilder.start();

            // Read the output of the process
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            SinkInput currentSinkInput = null;

            Pattern volumePattern = Pattern.compile("\\s+(\\d+)%"); // Pattern to extract channel volume

            while ((line = reader.readLine()) != null) {
                // Trim leading and trailing spaces from the line
                line = line.trim();

                // Check for lines that start with specific strings
                if (line.startsWith("Sink #")) {
                    currentSinkInput = new SinkInput();
                    currentSinkInput.id = line.substring(6).trim();
                } else if (line.startsWith("Description:")) {
                    // Extract the sink input name
                    currentSinkInput.name = line.substring(12).trim();
                } else if (line.startsWith("Volume:")) {
                    // Extract and parse the volume of each channel
                    Matcher matcher = volumePattern.matcher(line);
                    int sumVolume = 0;
                    int channelCount = 0;

                    while (matcher.find()) {
                        int channelVolume = Integer.parseInt(matcher.group(1));
                        sumVolume += channelVolume;
                        channelCount++;
                    }

                    // Calculate the average volume across channels
                    if (channelCount > 0) {
                        currentSinkInput.volume = sumVolume / channelCount;
                    }
                } else if (line.isEmpty() && currentSinkInput != null) {
                    currentSinkInput.type=0;
                    sinkInputs.add(currentSinkInput);
                    currentSinkInput = null;
                }
            }
            if (currentSinkInput != null){
                currentSinkInput.type=0;
                sinkInputs.add(currentSinkInput);
                currentSinkInput = null;
            }

            // Wait for the process to finish
            process.waitFor();

            // Close the reader
            reader.close();

            // Print sink input information
            /* REMOVED
            for (SinkInput sinkInput : sinkInputs) {
                System.out.println("Sink Input Name: " + sinkInput.name);
                System.out.println("Sink Input Volume: " + sinkInput.volume + "%");
                System.out.println("Sink Input ID: " + sinkInput.id);
                System.out.println();
            }
            */
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return sinkInputs;
    }
}
