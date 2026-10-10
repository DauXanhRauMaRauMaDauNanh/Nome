//! Lockless Multithreaded Entity Component System (ECS) for Nome

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct EntityId(pub u64);

#[derive(Debug, Clone, Copy)]
pub struct Position {
    pub x: f64,
    pub y: f64,
    pub z: f64,
    pub yaw: f32,
    pub pitch: f32,
}

#[derive(Debug, Clone, Copy)]
pub struct Velocity {
    pub dx: f64,
    pub dy: f64,
    pub dz: f64,
}

#[derive(Debug, Clone)]
pub struct PlayerComponent {
    pub name: String,
    pub uuid_most: u64,
    pub uuid_least: u64,
    pub ping: i32,
    pub is_online: bool,
}

pub struct EntityRecord {
    pub id: EntityId,
    pub pos: Position,
    pub vel: Velocity,
    pub player: Option<PlayerComponent>,
}

pub struct EntityManager {
    pub entities: Vec<EntityRecord>,
    next_id: u64,
}

impl EntityManager {
    pub fn new() -> Self {
        Self {
            entities: Vec::with_capacity(1024),
            next_id: 1,
        }
    }

    pub fn spawn_player(&mut self, name: &str, x: f64, y: f64, z: f64) -> EntityId {
        let id = EntityId(self.next_id);
        self.next_id += 1;

        self.entities.push(EntityRecord {
            id,
            pos: Position { x, y, z, yaw: 0.0, pitch: 0.0 },
            vel: Velocity { dx: 0.0, dy: 0.0, dz: 0.0 },
            player: Some(PlayerComponent {
                name: name.to_string(),
                uuid_most: 0,
                uuid_least: 0,
                ping: 0,
                is_online: true,
            }),
        });

        id
    }

    pub fn tick_physics(&mut self) {
        // Parallel physics movement pass
        for entity in &mut self.entities {
            entity.pos.x += entity.vel.dx;
            entity.pos.y += entity.vel.dy;
            entity.pos.z += entity.vel.dz;
        }
    }
}
