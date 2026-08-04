package com.shiftedlaw.rolllaw;

import java.util.List;

public enum Law {
	GRAVITY_ANCHOR(
			"The Gravity Anchor",
			List.of("Fall damage converts into healing"),
			List.of("Jumping costs hunger")
	),
	INFERNO(
			"The Inferno",
			List.of("Half damage from fire and lava"),
			List.of("Fire spreads twice as fast around you")
	),
	CULTIVATOR(
			"The Cultivator",
			List.of("Nearby crops grow 50% faster"),
			List.of("You can no longer sprint")
	),
	PHANTOM(
			"The Phantom",
			List.of("20% faster sneaking", "20% slower falling"),
			List.of("No known weaknesses... yet")
	),
	BEACON(
			"The Beacon",
			List.of("Allies gain +20% resistance"),
			List.of("You take +10% more damage")
	),
	LEECH(
			"The Leech",
			List.of("Melee hits heal you for 30% of the damage dealt"),
			List.of("You take +20% damage from all sources")
	),
	MINER(
			"The Miner",
			List.of("Permanent Haste II"),
			List.of("Hunger drains twice as fast")
	),
	FROSTBOURNE(
			"The Frostbourne",
			List.of("Never drown (permanent Water Breathing)"),
			List.of("15% slower movement on land")
	),
	REAPER(
			"The Reaper",
			List.of("Killing anything grants a brief Strength boost"),
			List.of("You take +20% damage from mobs")
	),
	WARDENS_CURSE(
			"The Warden's Curse",
			List.of("Permanent Night Vision"),
			List.of("Permanent Nausea")
	),
	ALCHEMIST(
			"The Alchemist",
			List.of("Permanent Regeneration I"),
			List.of("Permanent Weakness I")
	),
	NIGHTSTALKER(
			"The Nightstalker",
			List.of("Invisible while sneaking"),
			List.of("Hunger drains noticeably faster")
	),
	JUGGERNAUT(
			"The Juggernaut",
			List.of("Permanent Resistance I"),
			List.of("Permanent Mining Fatigue I")
	),
	LEAPER(
			"The Leaper",
			List.of("Permanent Jump Boost II"),
			List.of("Permanent Slowness I")
	),
	VAMPIRE(
			"The Vampire",
			List.of("Regeneration I at night"),
			List.of("Hunger drains faster during the day")
	),
	SUNBOUND(
			"The Sunbound",
			List.of("Strength I during the day"),
			List.of("Slowness I at night")
	),
	STORMCALLER(
			"The Stormcaller",
			List.of("Immune to lightning damage"),
			List.of("Slowness I while it's raining")
	),
	SUNDER(
			"The Sunder",
			List.of("20% less damage from explosions"),
			List.of("20% more damage from projectiles")
	),
	HARVESTER(
			"The Harvester",
			List.of("Permanent Saturation I (hunger drains slower)"),
			List.of("Permanent Mining Fatigue I")
	),
	HOLLOW(
			"The Hollow",
			List.of("Permanent Absorption I (extra shield hearts)"),
			List.of("Permanent Darkness")
	);

	private final String displayName;
	private final List<String> pros;
	private final List<String> cons;

	Law(String displayName, List<String> pros, List<String> cons) {
		this.displayName = displayName;
		this.pros = pros;
		this.cons = cons;
	}

	public String getDisplayName() {
		return displayName;
	}

	public List<String> getPros() {
		return pros;
	}

	public List<String> getCons() {
		return cons;
	}
}
