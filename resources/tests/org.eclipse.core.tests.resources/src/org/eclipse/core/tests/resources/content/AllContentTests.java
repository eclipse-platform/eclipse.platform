/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.content;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Runs all content type tests
 */
@Suite
@SelectClasses({ //
		IContentTypeManagerTest.class, //
		SpecificContextTest.class, //
		ContentDescriptionTest.class, //
		XMLContentDescriberTest.class, //
		LazyInputStreamTest.class, //
		LazyReaderTest.class, //
		TestBug94498.class, //
})
public class AllContentTests {

}
