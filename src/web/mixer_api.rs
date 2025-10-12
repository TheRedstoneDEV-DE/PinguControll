use rocket::{get, put, routes};
use rocket::serde::json;
use crate::datatypes::{Fader, Mixer};
use pulsectl::controllers::{SinkController, AppControl, DeviceControl};
use libpulse_binding::volume::Volume;

fn volume_from_percent(volume: u8) -> u32 {
    (volume as u32) * (Volume::NORMAL.0 / 100)
}

fn percent_from_volume(volume: u32) -> u8 {
    (volume / (Volume::NORMAL.0 / 100)) as u8
}

#[get("/get_faders")]
fn get_faders() -> Option<json::Json<Mixer>> {
    let mut pa_handler = SinkController::create();
    let apps = pa_handler.list_applications().ok()?;
    let devices = pa_handler.list_devices().ok()?;
    
    let mut app_faders: Vec<Fader> = vec![];
    for app in apps.clone() {
        if let Some(volume) = app.volume.get().get(1) && 
            let Some(name) = app.proplist.get_str("application.name") {

            app_faders.push(Fader {
                name: name,
                id: app.index,
                value: percent_from_volume(volume.0 as u32)
            });
        }
    }

    let mut device_faders: Vec<Fader> = vec![];
    for device in devices.clone() {
       device_faders.push(Fader {
           name: device.description?,
           id: device.index,
           value: percent_from_volume(device.volume.get().get(1)?.0 as u32)
       });
    }

    Some(json::Json(Mixer{
       apps: app_faders,
       devices: device_faders
    }))
}

#[put("/control", format="json", data="<data>")]
fn put_control(data: json::Json<Fader>) -> Option<json::Json<Fader>> {
    let mut pa_handler = SinkController::create();
    if let Ok(device) = pa_handler.get_device_by_index(data.id) {
        let mut volume = device.volume;
        let channels = device.channel_map.len();
        pa_handler.set_device_volume_by_index(data.id, volume.set(channels, Volume::from(Volume(volume_from_percent(data.value))) ));
    } else if pa_handler.get_app_by_index(data.id).is_ok() {
        pa_handler.set_app_volume_to_percent(data.id, data.value as f64);
    }

    Some(data)
}

pub fn routes() -> Vec<rocket::Route> {
    routes![get_faders, put_control]
}
