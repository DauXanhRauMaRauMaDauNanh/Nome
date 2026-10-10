//! Minecraft Protocol Handling (Modern 1.20 - 1.21+ / Protocol 777)

#[derive(Debug, PartialEq, Clone, Copy)]
pub enum ConnectionState {
    Handshake,
    Status,
    Login,
    Configuration,
    Play,
}

pub struct ServerListStatus {
    pub version_name: String,
    pub protocol: i32,
    pub max_players: i32,
    pub online_players: i32,
    pub motd: String,
}

impl ServerListStatus {
    pub fn new(motd: &str, max_players: i32, online_players: i32) -> Self {
        Self {
            version_name: "Nome 26.3 (Native C/C++/Rust)".to_string(),
            protocol: 777,
            max_players,
            online_players,
            motd: motd.to_string(),
        }
    }

    pub fn to_json(&self) -> String {
        format!(
            "{{\"version\":{{\"name\":\"{}\",\"protocol\":{}}},\"players\":{{\"max\":{},\"online\":{}}},\"description\":{{\"text\":\"{}\"}}}}",
            self.version_name, self.protocol, self.max_players, self.online_players, self.motd
        )
    }
}
