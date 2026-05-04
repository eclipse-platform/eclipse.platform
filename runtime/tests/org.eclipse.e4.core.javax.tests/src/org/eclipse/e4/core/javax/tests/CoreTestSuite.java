/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 474274
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 548516
 ******************************************************************************/

package org.eclipse.e4.core.javax.tests;

import org.eclipse.e4.core.internal.tests.contexts.inject.AnnotationsInjectionTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.ProviderInjectionTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("CoreTestSuite (javax)")
@SelectClasses({

		// Contexts injection
		AnnotationsInjectionTest.class,
		ProviderInjectionTest.class,
	})
public class CoreTestSuite {
}
