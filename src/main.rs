use libpulse_binding::context::subscribe::InterestMaskSet;
use libpulse_binding::context::{self, State};
use libpulse_binding::mainloop::standard::Mainloop;
use rustbus::{DuplexConn, MessageBuilder, get_session_bus_path};
use std::thread;
use rocket::{launch, put};
use rust_embed::Embed;
use rocket::serde::json::serde_json;
use rocket::serde::json;
use rocket::fs::FileServer;
use std::fs::{self, File};
use std::io::Write;
use std::sync::Arc;
use serde::Deserialize;
use std::ffi::{OsStr, OsString};
use std::path::PathBuf;

use crate::web::*;

mod web;
mod datatypes;

#[derive(Embed)]
#[folder = "assets/"]
struct Asset;

type Tx = rocket::tokio::sync::watch::Sender<String>;

#[derive(Clone)]
struct SyncState {
    tx: Arc<Tx>,
}

#[derive(Debug, Deserialize)]
pub struct Config {
    pub ftsetup: bool,
    pub host: String,
    pub port: u16,
    pub allowed_ips: Vec<String>,
    pub theme: String,
   // button_1: Button,
   // button_2: Button,
   // button_3: Button,
   // button_4: Button,
   // button_5: Button,
   // button_6: Button,
}

pub fn append_to_path(p: impl Into<OsString>, s: impl AsRef<OsStr>) -> PathBuf {
    let mut p = p.into();
    p.push(s);
    p.into()
}

fn subscribe_pulseaudio(state: SyncState) {
    thread::spawn(move || {
        println!("[INFO] [PA] Starting PulseAudio Listener...");
        let mut mainloop = Mainloop::new().expect("Failed to create PulseAudio Mainloop!");
        let mut ctx = context::Context::new(&mainloop, "PinguControl").expect("Failed to create PulseAudio context!");
        ctx.connect(None, context::FlagSet::empty(), None).expect("Failed to connect to PulseAudio");

        while ctx.get_state() != State::Ready {
            mainloop.iterate(false);
        }
        println!("[INFO] [PA] Mainloop context ready!");

        let tx_clone = state.tx.clone();

        ctx.set_subscribe_callback(Some(Box::new(move |_facility, _operation, _idx| {
                let _ = tx_clone.send("mixer_update".to_string()); // don't handle errors, because
                                                                   // the client might not be
                                                                   // available
        })));

        let mask = InterestMaskSet::SINK | InterestMaskSet::SINK_INPUT;
        ctx.subscribe(mask, |success| {
            if !success {
                eprintln!("[FATAL] [PA] PulseAudio subscription failed!");
            }
        });

        mainloop.run().expect("Failed to start PulseAudio Mainloop!");
    });
}

fn subscribe_mpris(state: SyncState) {
    thread::spawn( move || {
        println!("[INFO] [DBUS] Connecting to session bus...");
        let bus_path = get_session_bus_path().expect("Failed to open D-BUS session bus!");
        let mut conn = DuplexConn::connect_to_bus(bus_path, true).expect("Failed to connect to session bus!");
        conn.send_hello(rustbus::connection::Timeout::Infinite).expect("Failed to say Hello to bus!");
        let mut match_msg = MessageBuilder::new()
            .call("AddMatch")
            .with_interface("org.freedesktop.DBus")
            .on("/org/freedesktop/DBus")
            .at("org.freedesktop.DBus")
            .build();

        match_msg.body.push_param("type='signal',interface='org.freedesktop.DBus.Properties',\
                 member='PropertiesChanged',arg0='org.mpris.MediaPlayer2.Player'")
            .unwrap();

        println!("[INFO] [DBUS] Sending discovery message...");
        conn.send.send_message(&match_msg).expect("Failed to send discovery message!")
            .write_all().expect("Failed to send discovery message!");
        println!("[INFO] [DBUS] Entering event loop...");
        loop {
            let _msg = conn.recv.get_next_message(rustbus::connection::Timeout::Infinite).expect("Failed to receive message!");
            let _ = state.tx.send("mpris_update".to_string());
        }
    });
}

#[put("/update_config", format="json", data="<data>")]
fn update_config(data: json::Json<Config>){

}

#[launch]
fn rocket() -> _ {
    let conf_file_path = append_to_path(dirs::home_dir().expect("User needs to have a homedir!"),"/.config/pingu_control/pingu-control.conf");
    let config_dir = append_to_path(dirs::home_dir().expect("User needs to have a homedir!"),"/.config/pingu_control");
    let conf_file = fs::read_to_string(conf_file_path.clone());
    let mut conf = Config {
        ftsetup: true,
        host: "0.0.0.0".to_string(),
        port: 8193 as u16,
        allowed_ips: vec![
            "127.0.0.1".to_string()
        ],
        theme: "default".to_string()
    };
    if conf_file.is_ok() {
        conf = serde_json::from_str(&conf_file.unwrap()).expect("Config File should really exist by now!");
        println!("[INFO] Configuration file succsessfully loaded!");
    } else {
        println!("[FATAL] Configuration file not found!\n[INFO] unpacking new config...");
        let _ = fs::create_dir(append_to_path(dirs::home_dir().expect("why should this not work?"),"/.config/pingu_control"));
        let _ = File::create(conf_file_path.clone()).expect("You should have permissions to create files in your homedir!")
            .write_all(Asset::get("config/pingu-control.conf").unwrap().data.as_ref());
        println!("[INFO] unpacking assets...");
        let _ = fs::create_dir(append_to_path(config_dir.clone(),"/site"));
        let _ = File::create(append_to_path(config_dir.clone(),"/site/index.html")).expect("You should have permissions to create files in your homedir!")
            .write_all(Asset::get("site/index.html").unwrap().data.as_ref());
        let _ = File::create(append_to_path(config_dir.clone(),"/site/conf.html")).expect("You should have permissions to create files in your homedir!")
            .write_all(Asset::get("site/conf.html").unwrap().data.as_ref());
        let _ = File::create(append_to_path(config_dir.clone(),"/site/mpris.html")).expect("You should have permissions to create files in your homedir!")
            .write_all(Asset::get("site/mpris.html").unwrap().data.as_ref());
        let _ = File::create(append_to_path(config_dir.clone(),"/site/sysmon.html")).expect("You should have permissions to create files in your homedir!")
            .write_all(Asset::get("site/sysmon.html").unwrap().data.as_ref());
        let _ = File::create(append_to_path(config_dir.clone(),"/site/style.css")).expect("You should have permissions to create files in your homedir!")
            .write_all(Asset::get("site/style.css").unwrap().data.as_ref());
        let _ = File::create(append_to_path(config_dir.clone(),"/site/font-awesome.min.css")).expect("You should have permissions to create files in your homedir!")
            .write_all(Asset::get("site/font-awesome.min.css").unwrap().data.as_ref());
        let _ = File::create(append_to_path(config_dir.clone(),"/site/mixer.html")).expect("You should have permissions to create files in your homedir!")
            .write_all(Asset::get("site/mixer.html").unwrap().data.as_ref());
        let _ = File::create(append_to_path(config_dir.clone(),"/site/jquery.min.js")).expect("You should have permissions to create files in your homedir!")
            .write_all(Asset::get("site/jquery.min.js").unwrap().data.as_ref());
        println!("[INFO] Configuration file succsessfully loaded!");
    }

    let rocket_config = rocket::Config {
        address: conf.host.parse().unwrap(),
        port: conf.port as u16,
        ..rocket::Config::default()
    };

    let (tx, _rx) = rocket::tokio::sync::watch::channel::<String>("initial_mixer_state".to_string());

    let state = SyncState {
        tx: Arc::new(tx),
    };

    subscribe_pulseaudio(state.clone());
    subscribe_mpris(state.clone());

    let config_staticdir = "/home/robert/.config/pingu_control/site/";
    rocket::custom(&rocket_config)
        .manage(state)
        .mount("/api/mpris", mpris_api::routes())
        .mount("/api/mixer", mixer_api::routes())
        .mount("/api", eventstream::routes())
        .mount("/", FileServer::from(config_staticdir))
}
