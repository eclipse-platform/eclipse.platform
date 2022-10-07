/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.mapping;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Suite containing all tests in the org.eclipse.core.tests.internal.mapping
 * package.
 *
 * @since 3.2
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ 
	ChangeValidationTest.class,
	TestProjectDeletion.class
	})
public class AllMappingTests {
}
