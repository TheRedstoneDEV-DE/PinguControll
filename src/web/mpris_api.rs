use rocket::{get, routes};
use rocket::serde::json;
use crate::datatypes::{MprisPlayer, Song};
use mpris::{Metadata, PlayerFinder};

fn pack_song_meta(metadata: Metadata) -> Song {
    let mut song = Song {
        trackid: "".to_string(),
        artist: "".to_string(),
        album: "".to_string(),
        title: "".to_string(),
        art_url: "".to_string()
    };

    if let Some(artist) = metadata.artists().and_then(|artists| artists.get(0).cloned()){
        song.artist = artist.to_string();
    }
    if let Some(album) = metadata.album_name() {
        song.album = album.to_string();
    }
    if let Some(title) = metadata.title() {
        song.title = title.to_string();
    }
    if let Some(trackid) = metadata.track_id() {
        song.trackid = trackid.as_str().to_string();
    }
    if let Some(art_url) = metadata.art_url() {
        song.art_url = art_url.to_string();
    } 
    song
}

#[get("/get_players")]
fn get_players() -> Option<json::Json<Vec<MprisPlayer>>> {
    let player_finder = PlayerFinder::new().ok()?;
    let players = player_finder.find_all().ok()?;
    let mut player_list: Vec<MprisPlayer> = vec![];
    for player in players {
        if let Ok(metadata) = player.get_metadata(){
            player_list.push(MprisPlayer {
                name: player.identity().to_string(),
                current_song: pack_song_meta(metadata) 
            });
        }
    }
    Some(json::Json(player_list))     
}

#[get("/control?<action>&<player>")]
fn control(action: &str, player: &str) -> Option<()> {
    let player = PlayerFinder::new().ok()?.find_by_name(&player).ok()?;
    match action {
        "play" => player.play_pause().ok()?,
        "next" => player.next().ok()?,
        "prev" => player.previous().ok()?,
        _ => return None,
    }
    Some(())
}

pub fn routes() -> Vec<rocket::Route> {
   routes![get_players, control] 
}
