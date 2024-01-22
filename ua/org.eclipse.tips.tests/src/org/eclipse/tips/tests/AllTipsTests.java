/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tips.tests;

import org.eclipse.tips.core.TipImageBas64Test;
import org.eclipse.tips.core.TipImageURLTest;
import org.eclipse.tips.core.TipManagerTest;
import org.eclipse.tips.core.TipProviderTest;
import org.eclipse.tips.core.TipTest;
import org.eclipse.tips.json.internal.UtilTest;
import org.eclipse.tips.util.ImageUtilTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ //
		TipImageBas64Test.class, //
		TipImageURLTest.class, //
		TipManagerTest.class, //
		TipProviderTest.class, //
		TipTest.class, //
		UtilTest.class, //
		ImageUtilTest.class, //
})
public class AllTipsTests {

}
