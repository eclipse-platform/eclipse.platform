package org.eclipse.core.tests.pki.auth;

import static org.junit.jupiter.api.Assertions.*;


import org.eclipse.core.pki.auth.IncomingSystemProperty;

import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import org.junit.Before;
import org.junit.Test;

public class IncomingSystemPropertyTest {

	@Mock
	IncomingSystemProperty incomingSystemPropertyMock = null;
	public IncomingSystemPropertyTest() {}
	
	@Before
	public void Initialize() throws Exception {
		//MockitoAnnotations.initMocks(this);	
		incomingSystemPropertyMock = mock(IncomingSystemProperty.class);
	}
	
	@Test
	public void testCheckType() {
		try {
			boolean result = incomingSystemPropertyMock.checkType();
			assertFalse(result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Test
	public void testCheckKeyStore() {
		try {
			boolean result = incomingSystemPropertyMock.checkKeyStore("PiN");
			assertFalse(result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Test
	public void testCheckTrustStoreType() {
		try {
			boolean result = incomingSystemPropertyMock.checkTrustStoreType();
			assertFalse(result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Test
	public void testCheckTrustStore() {
		try {
			boolean result = incomingSystemPropertyMock.checkTrustStore();
			assertFalse(result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
