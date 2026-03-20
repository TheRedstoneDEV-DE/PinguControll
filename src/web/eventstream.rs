use rocket::response::stream::{EventStream, Event};
use rocket::{State, get, routes};
use crate::SyncState;

#[get("/sync")]
async fn get_sync(state: &State<SyncState>) -> Option<EventStream![]> {
    let mut rx = state.tx.subscribe();
    Some(
       EventStream! {
        loop {
            if rx.changed().await.is_err() {
                break;
            }

            yield Event::data({
                let borrowed = rx.borrow();
                borrowed.clone()
            });
        }
    } 
    )
}

pub fn routes() -> Vec<rocket::Route> {
    routes![get_sync]
}
