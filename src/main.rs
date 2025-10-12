use rocket::fs::FileServer;
use rocket::{launch, put};
use rust_embed::Embed;
use rocket::serde::json::serde_json;
use rocket::serde::json;
use std::fs::{self, File};
use std::io::Write;
use serde::Deserialize;
use std::ffi::{OsStr, OsString};
use std::path::PathBuf;

#[derive(Embed)]
#[folder = "assets/"]
struct Asset;

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

use crate::web::*;
mod web;
mod datatypes;

pub fn append_to_path(p: impl Into<OsString>, s: impl AsRef<OsStr>) -> PathBuf {
    let mut p = p.into();
    p.push(s);
    p.into()
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

    let config_staticdir = "/home/robert/.config/pingu_control/site/";
    rocket::custom(&rocket_config)
        .mount("/api/mpris", mpris_api::routes())
        .mount("/api/mixer", mixer_api::routes())
        .mount("/", FileServer::from(config_staticdir))
}
