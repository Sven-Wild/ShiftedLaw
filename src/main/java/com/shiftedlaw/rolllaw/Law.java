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
