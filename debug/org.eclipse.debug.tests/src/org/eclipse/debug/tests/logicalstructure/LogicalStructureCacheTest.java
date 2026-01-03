/*******************************************************************************
 *  Copyright (c) 2020 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.logicalstructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILogicalStructureType;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.internal.ui.views.variables.LogicalStructureCache;
import org.eclipse.debug.tests.DebugTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DebugTestExtension.class)
public class LogicalStructureCacheTest {

	@Test
	public void testReleaseValuesOnClear() throws Exception {
		TestValue rawValue = new TestValue("raw");
		ILogicalStructureType[] logicalStructureTypes = DebugPlugin.getLogicalStructureTypes(rawValue);

		LogicalStructureCache cache = new LogicalStructureCache();
		IValue logicalStructure = cache.getLogicalStructure(logicalStructureTypes[0], rawValue);

		assertTrue(logicalStructure.isAllocated());

		cache.clear();

		assertFalse(logicalStructure.isAllocated());
	}

}
