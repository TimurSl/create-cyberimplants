"""Layer 2 tier tables: the numeric rows of docs/design/implants/*.md for Tiers I-V.

Each entry: id -> (load[5], charge, cooldown_ticks, values). charge and cooldown may be a scalar or a list of five.
Values are lists of five (one per tier) or a scalar. Seconds are converted to ticks, percents to fractions.
Unlock rows (tier-gated features) are NOT here: they arrive in Layer 3 and the "unlocks" lists stay empty.
Where a table row mixes a number with an unlock (for example Sandevistan's 0% time scale at T3, which is the
"Full freeze" unlock), the Layer 2 value is the last non-unlock value and the unlocked number is kept under a
separate key that Java does not read yet.
"""

TIERS = {
    # ---- A. Neural
    "sandevistan": ([9, 10, 11, 12, 14], [60, 90, 120, 150, 200], [3000, 2400, 2000, 1600, 1200], {
        "radius": [3, 4, 5, 6, 7], "duration_ticks": [40, 50, 60, 70, 90],
        "time_scale": [0.25, 0.15, 0.15, 0.15, 0.15], "freeze_time_scale": [0.25, 0.15, 0, 0, 0],
        "cast_range": [0, 0, 8, 12, 16]}),
    "reflex_booster": ([4, 4, 5, 5, 6], 0, 0, {
        "attack_speed": [0.05, 0.10, 0.15, 0.20, 0.25], "item_use_speed": [0.10, 0.15, 0.25, 0.35, 0.50],
        "walk_speed": [0.02, 0.03, 0.04, 0.05, 0.06], "shield_raise_ticks": [5, 4, 3, 2, 0]}),
    "combat_predictor": ([5, 5, 6, 6, 7], 0, 0, {
        "horizon_ticks": [10, 15, 20, 30, 40], "indicators": [1, 1, 2, 3, 4], "counter_bonus": [0, 0.1, 0.2, 0.3, 0.4]}),
    "automatic_evasion_system": ([5, 5, 6, 6, 7], 10, 0, {
        "charges": [1, 1, 2, 2, 3], "recharge_ticks": [240, 200, 160, 120, 80], "detection_ticks": [4, 5, 6, 7, 8],
        "distance": [1.5, 1.75, 2.0, 2.5, 3.0], "condition_slots": [1, 2, 3, 4, 6]}),
    "kerenzikov": ([4, 4, 5, 5, 6], 8, [60, 50, 40, 30, 20], {
        "distance": [2.0, 2.5, 3.0, 3.5, 4.0], "window_ticks": [8, 10, 12, 16, 20]}),
    # ---- B. Cyberdeck hardware (modules cost +1 Load each, regardless of tier)
    "deck_chassis": ([4, 5, 6, 7, 8], 0, 0, {
        "module_sockets": [3, 4, 5, 6, 8], "latency_ticks": [4, 3, 2, 1, 0], "base_threads": 1,
        "macro_keys": [2, 4, 4, 4, 4], "log_lines": [20, 50, 100, 200, 500], "max_coprocessors": [1, 1, 2, 2, 3]}),
    "deck_processor": ([1, 1, 1, 1, 1], 0, 0, {
        "cycles_per_tick": [20, 40, 80, 160, 320], "isa": [1, 2, 3, 4, 5], "min_every_ticks": [20, 10, 5, 2, 1]}),
    "deck_ram": ([1, 1, 1, 1, 1], 0, 0, {"kb": [2, 4, 8, 16, 32]}),
    "deck_storage": ([1, 1, 1, 1, 1], 0, 0, {"kb": [4, 8, 16, 32, 64]}),
    "deck_coprocessor": ([1, 1, 1, 1, 1], 0, 0, {
        "extra_threads": [1, 1, 2, 2, 3], "cycles_per_tick": [10, 20, 40, 80, 160]}),
    "deck_machine_interface": ([1, 1, 1, 1, 1], 0, 0, {
        "direct_range": [16, 24, 32, 48, 64], "bound_ports": [2, 4, 8, 16, 32]}),
    # ---- C. Skeletal
    "reinforced_skeleton": ([3, 3, 4, 4, 5], 0, 0, {
        "blunt_flat": [0.5, 1.0, 1.5, 2.0, 2.5], "machinery": [0.20, 0.30, 0.40, 0.55, 0.70], "max_health": [0, 0, 2, 4, 6]}),
    "power_actuators": ([4, 4, 5, 6, 7], 2, 0, {
        "attack_damage": [1.0, 1.5, 2.0, 3.0, 4.0], "unarmed_multiplier": 2.0, "knockback": 0.2}),
    "kinetic_tendons": ([2, 3, 3, 4, 4], 0, 0, {
        "jump_height": [0.25, 0.5, 0.75, 1.0, 1.25], "jump_strength": [0.042, 0.084, 0.126, 0.168, 0.21],
        "step_height": [0, 0, 0.4, 0.4, 0.65]}),
    "micro_actuators": ([3, 3, 4, 4, 5], 0, 0, {
        "sprint_speed": [0.05, 0.08, 0.11, 0.14, 0.18], "accel_bonus": [0.1, 0.2, 0.3, 0.4, 0.5],
        "climb_speed": [0, 0, 0.25, 0.5, 0.75], "sprint_exhaustion": [0.1, 0.1, 0.1, 0, 0]}),
    "impact_dampeners": ([2, 2, 3, 3, 4], 0, 0, {
        "safe_fall": [2, 4, 6, 8, 12], "fall_reduction": [0.15, 0.25, 0.35, 0.50, 0.65],
        "energy_per_block": [0, 1, 2, 3, 4], "recovery_cap": 50}),
    "inertia_compensation": ([3, 3, 4, 4, 5], 0, 0, {
        "turn_speed_kept": [0.30, 0.45, 0.60, 0.75, 0.90], "knockback_resistance": [0.10, 0.15, 0.20, 0.30, 0.40]}),
    # ---- D. Limbs
    "cybernetic_arm": ([3, 4, 4, 5, 6], 0, 0, {
        "sockets": [1, 1, 2, 2, 3], "break_speed": [0.10, 0.20, 0.30, 0.40, 0.60], "reach": [0, 0.5, 0.5, 1.0, 1.5],
        "harvest_level": [0, 1, 2, 3, 4], "bilateral_break_speed": 0.10}),
    "remote_manipulator": ([3, 3, 4, 4, 5], 1, [15, 12, 10, 8, 6], {"range": [6, 10, 14, 20, 28]}),
    "retractable_blade": ([3, 4, 4, 5, 6], 0, 0, {
        "damage": [6, 7, 8, 9, 11], "attack_speed": [1.8, 1.8, 2.0, 2.0, 2.2], "dual_attack_speed": 0.15}),
    "microgrip": ([2, 2, 3, 3, 4], 0, 0, {
        "stamina_ticks": [60, 100, 160, 240, 400], "climb_speed": [2.0, 2.5, 3.0, 3.5, 4.5]}),
    "ballistic_module": ([3, 4, 4, 5, 6], 2, [20, 16, 13, 10, 8], {
        "damage": [3, 4, 5, 6, 8], "velocity": [2.0, 2.5, 3.0, 3.5, 4.0], "magazine": [4, 6, 8, 12, 16], "reload_ticks": 40}),
    # ---- E. Optical
    "entity_scanner": ([3, 3, 4, 4, 5], [5, 5, 6, 6, 8], [200, 160, 120, 100, 80], {
        "radius": [12, 16, 24, 32, 48], "reveal_ticks": [60, 80, 100, 120, 160], "max_markers": [8, 12, 16, 24, 32]}),
    "thermal_vision": ([2, 3, 3, 4, 4], 0, 0, {
        "range": [16, 24, 32, 40, 48], "wall_penetration": [2, 4, 8, 12, 64], "upkeep_per_second": 1.0}),
    "machine_analyzer": ([2, 2, 3, 3, 4], 0, 0, {
        "virtual_ports": [0, 1, 2, 4, 8], "range": [8, 12, 16, 24, 32], "overlay_upkeep_per_second": 0.5}),
    "ballistic_visor": ([2, 3, 3, 4, 4], 0, 0, {
        "tracking_radius": [8, 12, 16, 24, 32], "max_projectiles": [3, 5, 8, 12, 20]}),
    "optical_camouflage": ([6, 6, 7, 8, 9], [40, 45, 50, 55, 60], [1200, 1000, 800, 700, 500], {
        "duration_ticks": [80, 120, 160, 200, 280], "upkeep_per_second": 2.0,
        "detect_still": [0.5, 0.4, 0.25, 0.15, 0.05], "detect_moving": [0.8, 0.7, 0.6, 0.5, 0.35],
        "aggro_drop_ticks": [60, 50, 40, 30, 0]}),
    "target_tracking": ([3, 3, 4, 4, 5], 3, 0, {
        "range": [24, 32, 48, 64, 96], "max_tags": [1, 2, 3, 5, 8], "persistence_ticks": [200, 400, 800, 1800, -1]}),
    # ---- F. Circulatory
    "biomonitor": ([1, 1, 2, 2, 2], 0, 0, {"alarms": [1, 2, 4, 6, 8], "broadcast_channels": [0, 1, 2, 3, 4]}),
    "regenerator": ([3, 3, 4, 5, 6], 0, 0, {
        "heal_per_second": [0.25, 0.35, 0.5, 0.65, 0.85], "lockout_ticks": [160, 120, 100, 80, 60],
        "charge_per_hp": [2.0, 1.75, 1.5, 1.25, 1.0]}),
    "adrenaline_pump": ([2, 2, 3, 3, 4], 0, [600, 500, 400, 300, 240], {
        "trigger_damage": [3, 3, 2, 2, 0], "boost": [0.15, 0.20, 0.25, 0.30, 0.35], "boost_ticks": [60, 70, 80, 100, 120],
        "crash_ticks": [80, 60, 40, 20, 0]}),
    "emergency_defibrillator": ([4, 4, 5, 5, 6], [80, 100, 120, 140, 160], [36000, 30000, 24000, 18000, 12000], {
        "health_restored": [2, 4, 6, 8, 10], "resistance_ticks": [40, 40, 60, 60, 80],
        "resistance_amplifier": [0, 1, 1, 2, 3]}),
    "energy_buffer": ([2, 2, 3, 3, 4], 0, 0, {
        "capacity": [50, 100, 175, 250, 400], "fill_rate": [1.0, 1.5, 2.0, 3.0, 4.0]}),
    "emergency_response_system": ([3, 3, 4, 4, 5], 0, [600, 500, 400, 300, 200], {"response_slots": [1, 2, 2, 3, 4]}),
    # ---- G. Subdermal
    "armor_plating": ([3, 3, 4, 4, 5], 0, 0, {
        "armor": [2, 3, 4, 5, 6], "toughness": [0, 1, 2, 2, 3], "wear_reduction": [0, 0, 0.20, 0.35, 0.50],
        "weight": [0, 0, -0.02, -0.03, -0.03]}),
    "nanocoating": ([3, 3, 4, 5, 6], 0, 0, {
        "balanced": [0.04, 0.06, 0.08, 0.10, 0.12], "specialised": [0.15, 0.20, 0.30, 0.40, 0.50]}),
    "shock_circuit": ([3, 3, 4, 4, 5], 3, 0, {
        "damage": [2, 3, 4, 5, 6], "internal_cooldown_ticks": [30, 25, 20, 15, 10]}),
    "kinetic_shield": ([4, 4, 5, 6, 7], 0, 0, {
        "capacity": [4, 6, 8, 10, 14], "absorb_share": [0.5, 0.6, 0.7, 0.8, 0.9],
        "recharge_delay_ticks": [160, 140, 120, 100, 80], "recharge_rate": [0.5, 0.75, 1.0, 1.25, 1.75],
        "charge_per_hp": 3.0, "emergency_cooldown_ticks": 900}),
    "emergency_shell": ([3, 3, 4, 4, 5], 30, [2400, 2000, 1800, 1500, 1200], {
        "trigger_health": [0.20, 0.25, 0.25, 0.30, 0.30], "reduction": [0.4, 0.5, 0.6, 0.7, 0.8],
        "duration_ticks": [40, 50, 60, 80, 100], "movement_penalty": [0.4, 0.35, 0.3, 0.2, 0.1]}),
    # ---- H. Mobility
    "enhanced_tendons": ([3, 3, 4, 4, 5], 4, 0, {"air_jumps": [1, 1, 2, 2, 3], "strength": [0.7, 0.8, 0.9, 1.0, 1.0]}),
    "hydraulic_legs": ([4, 4, 5, 5, 6], 15, [60, 60, 50, 50, 40], {
        "max_vertical": [4, 6, 8, 10, 14], "max_horizontal": [6, 9, 12, 16, 22], "full_charge_ticks": [30, 25, 20, 16, 12]}),
    "silent_actuators": ([2, 2, 3, 3, 4], 0, 0, {
        "suppression_level": [1, 2, 3, 4, 5], "sneak_speed": [0, 0.10, 0.15, 0.25, 0.35],
        "sneak_detection_reduction": [0.10, 0.15, 0.20, 0.30, 0.40]}),
    "magnetic_soles": ([2, 2, 3, 3, 4], 0, 0, {
        "upkeep_per_second": 0.5, "wall_walk_speed": [0.5, 0.65, 0.8, 0.9, 1.0]}),
    "inertial_stabilizer": ([2, 3, 3, 4, 4], 0, 0, {
        "air_control": [0.25, 0.5, 0.75, 1.0, 1.5], "wall_impact_reduction": [0.2, 0.4, 0.6, 0.8, 1.0]}),
    "momentum_booster": ([3, 3, 4, 4, 5], 0, 0, {
        "dash_blocks": [6, 8, 10, 12, 16], "buff_speed": [0.20, 0.25, 0.30, 0.40, 0.50], "buff_ticks": [40, 60, 60, 80, 100],
        "sprint_gain_per_second": 8.0, "decay_per_second": [5, 4, 3, 2, 1]}),
}


def per_tier(value, tier):
    return value[tier] if isinstance(value, list) else value
