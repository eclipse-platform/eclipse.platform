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

import static org.junit.jupiter.api.Assertions.assertTrue;

//import static org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
//import static org.assertj.swing.fixture.JPanelFixture;
//import static org.assertj.swing.testing.AssertJSwingTestCaseTemplate;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

import org.junit.Before;

import org.junit.Test;

import org.eclipse.core.pki.pkiselection.SecurityOpRequest;

public class SecurityOpRequestTest {
	SecurityOpRequest securityOpRequestMock = null;
	
	
	public SecurityOpRequestTest() {}
	
	@Before
	public void Initialize() throws Exception {
		//MockitoAnnotations.initMocks(this);
		securityOpRequestMock = mock(SecurityOpRequest.class);	
	}

	@Test
	public void testSecurityOp() {
		when(securityOpRequestMock.getConnected()).thenReturn(true);
		doNothing().when(securityOpRequestMock).setConnected(false);
		boolean testResult = securityOpRequestMock.getConnected();
		assertTrue(testResult);
		securityOpRequestMock.setConnected(true);	
	}
}
