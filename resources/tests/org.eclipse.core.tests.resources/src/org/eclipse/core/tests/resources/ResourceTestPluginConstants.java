/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.core.tests.resources;

/**
 * A utility class representing information about the extensions defined in this
 * test project.
 */
public final class ResourceTestPluginConstants {
	public static final String PI_RESOURCES_TESTS = "org.eclipse.core.tests.resources"; //$NON-NLS-1$

	// nature that installs and runs a builder (regression test for bug 29116)
	public static final String NATURE_29116 = "org.eclipse.core.tests.resources.nature29116";
	// cycle1 requires: cycle2
	public static final String NATURE_CYCLE1 = "org.eclipse.core.tests.resources.cycle1";
	// cycle2 requires: cycle3
	public static final String NATURE_CYCLE2 = "org.eclipse.core.tests.resources.cycle2";
	// cycle3 requires: cycle1
	public static final String NATURE_CYCLE3 = "org.eclipse.core.tests.resources.cycle3";
	// earthNature, one-of: stateSet
	public static final String NATURE_EARTH = "org.eclipse.core.tests.resources.earthNature";
	// invalidNature
	public static final String NATURE_INVALID = "org.eclipse.core.tests.resources.invalidNature";
	// missing nature
	public static final String NATURE_MISSING = "no.such.nature.Missing";
	// missing pre-req nature
	public static final String NATURE_MISSING_PREREQ = "org.eclipse.core.tests.resources.missingPrerequisiteNature";
	// mudNature, requires: waterNature, earthNature, one-of: otherSet
	public static final String NATURE_MUD = "org.eclipse.core.tests.resources.mudNature";
	// simpleNature
	public static final String NATURE_SIMPLE = "org.eclipse.core.tests.resources.simpleNature";
	// nature for regression tests of bug 127562
	public static final String NATURE_127562 = "org.eclipse.core.tests.resources.bug127562Nature";
	// snowNature, requires: waterNature, one-of: otherSet
	public static final String NATURE_SNOW = "org.eclipse.core.tests.resources.snowNature";
	// waterNature, one-of: stateSet
	public static final String NATURE_WATER = "org.eclipse.core.tests.resources.waterNature";
	public static final String SET_OTHER = "org.eclipse.core.tests.resources.otherSet";
	// constants for nature sets
	public static final String SET_STATE = "org.eclipse.core.tests.resources.stateSet";

	private ResourceTestPluginConstants() {
	}

	/**
	 * Returns valid sets of natures
	 */
	public static String[][] getValidNatureSets() {
		return new String[][] { {}, { NATURE_SIMPLE }, { NATURE_SNOW, NATURE_WATER }, { NATURE_EARTH },
				{ NATURE_WATER, NATURE_SIMPLE, NATURE_SNOW }, };
	}

	/**
	 * Returns invalid sets of natures
	 */
	public static String[][] getInvalidNatureSets() {
		return new String[][] { { NATURE_SNOW }, // missing water pre-req
				{ NATURE_WATER, NATURE_EARTH }, // duplicates from state-set
				{ NATURE_WATER, NATURE_MUD }, // missing earth pre-req
				{ NATURE_WATER, NATURE_EARTH, NATURE_MUD }, // duplicates from state-set
				{ NATURE_SIMPLE, NATURE_SNOW, NATURE_WATER, NATURE_MUD }, // duplicates from other-set, missing pre-req
				{ NATURE_MISSING }, // doesn't exist
				{ NATURE_SIMPLE, NATURE_MISSING }, // missing doesn't exist
				{ NATURE_MISSING_PREREQ }, // requires nature that doesn't exist
				{ NATURE_SIMPLE, NATURE_MISSING_PREREQ }, // requires nature that doesn't exist
				{ NATURE_CYCLE1 }, // missing pre-req
				{ NATURE_CYCLE2, NATURE_CYCLE3 }, // missing pre-req
				{ NATURE_CYCLE1, NATURE_SIMPLE, NATURE_CYCLE2, NATURE_CYCLE3 }, // cycle
		};
	}

}
