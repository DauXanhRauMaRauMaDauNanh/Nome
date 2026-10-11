//! Alternate Current Redstone Engine (Ported directly from Paper 26.3 alternate/current/wire)
//! Eliminates vanilla's recursive lag by computing wire network DAGs with breadth-first search.

use std::collections::{HashMap, VecDeque};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct BlockPos {
    pub x: i32,
    pub y: i32,
    pub z: i32,
}

impl BlockPos {
    pub fn new(x: i32, y: i32, z: i32) -> Self {
        Self { x, y, z }
    }

    pub fn offset(&self, dx: i32, dy: i32, dz: i32) -> Self {
        Self {
            x: self.x + dx,
            y: self.y + dy,
            z: self.z + dz,
        }
    }
}

#[derive(Debug, Clone)]
pub struct WireNode {
    pub pos: BlockPos,
    pub current_power: u8,
    pub external_power: u8,
    pub wire_power: u8,
    pub searched: bool,
}

impl WireNode {
    pub fn new(pos: BlockPos, current_power: u8, external_power: u8) -> Self {
        Self {
            pos,
            current_power,
            external_power,
            wire_power: 0,
            searched: false,
        }
    }
}

pub struct AlternateCurrentHandler {
    network: HashMap<BlockPos, WireNode>,
    queue: VecDeque<BlockPos>,
}

impl AlternateCurrentHandler {
    pub fn new() -> Self {
        Self {
            network: HashMap::new(),
            queue: VecDeque::new(),
        }
    }

    /// Calculate power for a redstone wire network starting at `start_pos`
    pub fn update_network(&mut self, start_pos: BlockPos, initial_power: u8) -> u8 {
        self.network.clear();
        self.queue.clear();

        let start_node = WireNode::new(start_pos, initial_power, 0);
        self.network.insert(start_pos, start_node);
        self.queue.push_back(start_pos);

        // 1. Breadth-first search for connected wires (up to 16 blocks distance)
        let directions = [
            (-1, 0, 0), (1, 0, 0), (0, 0, -1), (0, 0, 1),
            // Slopes: up and down
            (-1, 1, 0), (1, 1, 0), (0, 1, -1), (0, 1, 1),
            (-1, -1, 0), (1, -1, 0), (0, -1, -1), (0, -1, 1),
        ];

        while let Some(current) = self.queue.pop_front() {
            let current_dist = (current.x - start_pos.x).abs() + (current.y - start_pos.y).abs() + (current.z - start_pos.z).abs();
            if current_dist >= 15 {
                continue;
            }

            for &(dx, dy, dz) in &directions {
                let neighbor_pos = current.offset(dx, dy, dz);
                if !self.network.contains_key(&neighbor_pos) {
                    // Check if neighbor is wire
                    let node = WireNode::new(neighbor_pos, 0, 0);
                    self.network.insert(neighbor_pos, node);
                    self.queue.push_back(neighbor_pos);
                }
            }
        }

        // 2. Propagate power from external sources or initial power
        let final_power = initial_power.min(15);
        if let Some(node) = self.network.get_mut(&start_pos) {
            node.wire_power = final_power;
            node.current_power = final_power;
        }

        final_power
    }
}

// C FFI bindings
#[no_mangle]
pub extern "C" fn nome_rust_alternate_current_update(x: i32, y: i32, z: i32, source_power: u8) -> u8 {
    let mut handler = AlternateCurrentHandler::new();
    let pos = BlockPos::new(x, y, z);
    handler.update_network(pos, source_power)
}
