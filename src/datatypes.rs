use serde::{Serialize, Deserialize};

#[derive(Deserialize, Serialize)]
pub struct Fader {
    pub id: u32,
    pub name: String,
    pub value: u8
}

#[derive(Deserialize, Serialize)]
pub struct Mixer {
    pub apps: Vec<Fader>,
    pub devices: Vec<Fader>
}

#[derive(Deserialize, Serialize)]
pub struct Song {
    pub trackid: String,
    pub artist: String,
    pub album: String,
    pub title: String,
    pub art_url: String
}

#[derive(Deserialize, Serialize)]
pub struct MprisPlayer {
    pub name: String,
    pub current_song: Song
}
