
<p align="center">
 <img width="140px" src="https://github.com/TheRedstoneDEV-DE/PinguControll/blob/main/PinguControll.png" align="center" alt="GitHub Readme Stats" />
 <h2 align="center">PinguControll</h2>
 <p align="center">A simple Web-UI to control PulseAudio (or Pipewire) and any mediaplayer with MPRIS on Linux.
This project was just created since all software I found in this direction was deprecated or not working, so I created my own.
</p>
</p>
  <p align="center">
    <a href="https://github.com/TheRedstoneDEV-DE/PinguControll/releases">
      <img alt="GitHub Release" src="https://img.shields.io/github/release/theredstonedev-de/pingucontroll" />
    </a>
    <a href="https://github.com/TheRedstoneDEV-DE/PinguControll/issues">
      <img alt="Issues" src="https://img.shields.io/github/issues/theredstonedev-de/pingucontroll?color=0088ff" />
    </a>
    <a href="https://github.com/TheRedstoneDEV-DE/PinguControll/pulls">
      <img alt="GitHub pull requests" src="https://img.shields.io/github/issues-pr/theredstonedev-de/pingucontroll?color=0088ff" />
    </a>
    <br />
  </p>
  

<p align="center">
We rewrite it in Rust. <br>Status: 14.10.2025: Its rewritten! But we still need to add a pretty Web-UI.<br>
⚠️ This is still Work-In-Progress! ⚠️
</p>
  <br />
<hr>

# Todolist

## Frontend:
- [ ] First-Time-Setup
- [ ] user defined buttons
- [ ] IP allowlist
- [ ] Multiple factory themes (theme support overall)
- [ ] Dashboard
- [ ] Controls
- [x] Music player (MPRIS) controller
- [x] Mixer
- [ ] Settings

## Backend:
- [ ] auto update 

# Usage

## Requirements
- A PC running Linux
- A libpulse compatible sound server
- Cargo for building the project (or a Nix installed)

## Sound-Server Compatability:
- [x] Pipewire (pipewire-pulse module, standard on most Distros)
- [x] PulseAudio (native)
- [ ] ~~Jack~~ (not supported at all)
- [ ] ALSA (not planned)

## Compiling
0. (With Nix only) Enter a nix-shell with the Rust-toolchain installed:
```
nix-shell
```

1. Build and run the program with
```
cargo run
```

3. Open the webui!
   The WebUI runs under http://127.0.0.1:8193/ or http://<your pc's IP here>:8193/ (from another device)

## Configuration and Customisation
### Config file
The configuration file for PinguControl is located in `~/.config/pingu_control/pingu-control.conf`.
A Default setup looks like this:
```json
{
  "ftsetup":true,
  "host":"127.0.0.1",
  "port":8193,
  "allowed_ips": ["127.0.0.1"],
  "theme":"std",
  "button_1": {
    "text":"undefined",
    "executable":"",
    "args": [""]  
  },
  "button_2": {
    "text":"undefined",
    "executable":"",
    "args": [""]
  },
  "button_3": {
    "text":"undefined",
    "executable":"",
    "args": [""]
  },
  "button_4": {
    "text":"undefined",
    "executable":"",
    "args": [""]
  },
  "button_5": {
    "text":"undefined",
    "executable":"",
    "args": [""]
  },
  "button_6": {
    "text":"undefined",
    "executable":"",
    "args": [""]
  }
}

```
|Key|Explanation|Default Value|
|---|-----------|-------------|
|`ftsetup`|Whether the First-Time-Setup is shown (will automatically be set to false when setup is completed)|`true`|
|`host`|The host the Web-UI should run on|`"127.0.0.1"`|
|`port`|The port the Web-UI should run on|`8193`|
|`allowed_ips`|IPs allowed to access the Web-UI|`["127.0.0.1"]`|
|`theme`|Web-UI theme|`"std"`|
|`button_x`|user-defined button configuration| `-------------` |
|`[+] text`|Text in the button shown in the webui|`"undefined"`|
|`[+] executable`|Program/Action to execute on button press|`""`|
|`[+] args`|Extra arguments for the program/action|`[""]`|

### Customization
The Web-UI is in the `site` directory located under `~/.config/pingu_control/` and can be fully modified.
|File|Function|
|----|--------|
|`index.html`|Main dashboard|
|`mixer.html`|Mixer UI (with MPRIS controller at the bottom)|
|`mpris.html`|MPRIS standalone UI (deprecated - might be removed)|
|`ftsetup.html`|First-time Setup UI|
|`conf.html`|Configuration UI|
|`sysmon.html`|System monitor UI (deprecated - needs additional server functionality that isn't there) |
|`style.css`|Styling for all UIs and UI components (should simplify theming)|

