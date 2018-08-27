/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
package org.eclipse.core.tests.net;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import junit.framework.*;

@RunWith(Suite.class)
@Suite.SuiteClasses({ NetTest.class, PreferenceModifyListenerTest.class })
public class AllNetTests extends TestCase {

}
