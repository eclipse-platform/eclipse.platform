package org.eclipse.core.tests.pki;

/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
//import org.eclipse.core.pki.PKISetup;
import java.util.Properties;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.isA;

import org.junit.Before;
import org.junit.Test;


import org.eclipse.core.pki.auth.SecurityFileSnapshot;
import org.eclipse.core.pki.pkiselection.PkiPasswordGrabberWidget;
import org.eclipse.core.pki.pkiselection.PKI;
import org.eclipse.core.pki.auth.PKIState;


public class MainTest {
	Properties properties = new Properties();
	String testName = "PKItestSubscriber";
	
	SecurityFileSnapshot securityFileSnapshotMock = null;
	PkiPasswordGrabberWidget pkiPasswordGrabberWidgetMock = null;
	PKI pkiMock = null;
	
	PKIState pkiStateMock = null;
	
	public MainTest() {
		System.out.println("Constructor MainTest");
	}
	
	@Before
	public void Initialize() throws Exception {
		
		securityFileSnapshotMock = mock(SecurityFileSnapshot.class);
		pkiPasswordGrabberWidgetMock = mock(PkiPasswordGrabberWidget.class);
		
		pkiMock = mock(PKI.class);
		pkiStateMock = mock(PKIState.class);
		
	}
	@Test
	public void testSecurityFileSnapshot() {
		when(securityFileSnapshotMock.image()).thenReturn(true);
		when(securityFileSnapshotMock.createPKI()).thenReturn(true);
		when(securityFileSnapshotMock.load(isA(String.class),  isA(String.class))).thenReturn(properties);
		doNothing().when(securityFileSnapshotMock).restoreProperties();
	}
	@Test
	public void testPkiPasswordGrabberWidget() {
		when(pkiPasswordGrabberWidgetMock.getInput()).thenReturn("testPassword");
	}
}
