//! Precision 20 TPS Tick Loop with Nanosecond Time Accounting
//! Similar to SparklyPaper multithreaded ticking and Folia regional scheduling concepts

use std::time::{Duration, Instant};

pub struct TickScheduler {
    pub tick_count: u64,
    pub target_tps: f64,
    pub target_tick_duration: Duration,
    pub last_tick_time: Instant,
    pub current_mspt: f64,
    pub rolling_tps: f64,
}

impl TickScheduler {
    pub fn new(target_tps: f64) -> Self {
        let tick_nanos = (1_000_000_000.0 / target_tps) as u64;
        Self {
            tick_count: 0,
            target_tps,
            target_tick_duration: Duration::from_nanos(tick_nanos),
            last_tick_time: Instant::now(),
            current_mspt: 0.0,
            rolling_tps: target_tps,
        }
    }

    pub fn begin_tick(&mut self) -> Instant {
        Instant::now()
    }

    pub fn end_tick(&mut self, start_time: Instant) {
        let elapsed = start_time.elapsed();
        self.current_mspt = elapsed.as_secs_f64() * 1000.0;
        self.tick_count += 1;

        if elapsed < self.target_tick_duration {
            let sleep_time = self.target_tick_duration - elapsed;
            std::thread::sleep(sleep_time);
            self.rolling_tps = self.target_tps;
        } else {
            self.rolling_tps = 1000.0 / self.current_mspt;
        }
    }
}
