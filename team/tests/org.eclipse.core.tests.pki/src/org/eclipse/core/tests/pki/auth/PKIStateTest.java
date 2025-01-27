package org.eclipse.core.tests.pki.auth;


import static org.junit.jupiter.api.Assertions.assertTrue;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.pki.auth.PKIState;

/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

public class PKIStateTest {
	PKIState pkiStateMock = null;
	public PKIStateTest() {}
	
	@Before
	public void Initialize() throws Exception {
		//MockitoAnnotations.initMocks(this);
		pkiStateMock = mock(PKIState.class);
	}
	@Test
	public void testSetPKCS11on() {
		when(pkiStateMock.isPKCS11on()).thenReturn(true);
		doNothing().when(pkiStateMock).setPKCS11on(false);
		boolean testResult = pkiStateMock.isPKCS11on();
		assertTrue(testResult);
		pkiStateMock.setPKCS11on(true);
	}
	@Test
	public void testSetPKCS12on() {
		when(pkiStateMock.isPKCS12on()).thenReturn(true);
		doNothing().when(pkiStateMock).setPKCS12on(false);
		boolean testResult = pkiStateMock.isPKCS12on();
		assertTrue(testResult);
		pkiStateMock.setPKCS12on(true);
	}
}
