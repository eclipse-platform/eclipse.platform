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
package org.eclipse.core.tests.pki.auth;


import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.pki.auth.PublicKeySecurity;

public class PublicKeySecurityTest {
	PublicKeySecurity publicKeySecurityMock = null;
	Properties properties = new Properties();
	String PiN = "12345679";
	public PublicKeySecurityTest() {}
	
	
	@Before
	public void Initialize() throws Exception {
		publicKeySecurityMock = mock(PublicKeySecurity.class);
	}
	
	@Test
	public void testGetpkiPropertyFile() {
		when(publicKeySecurityMock.getPkiPropertyFile(PiN)).thenReturn(properties);
	}

}
