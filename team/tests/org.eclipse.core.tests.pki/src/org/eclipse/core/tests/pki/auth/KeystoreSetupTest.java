package org.eclipse.core.tests.pki.auth;


import org.eclipse.core.pki.auth.KeystoreSetup;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;


public class KeystoreSetupTest {
	

	@Mock
	KeystoreSetup keyStoreSetupMock = null;
	public KeystoreSetupTest() {}
	
	@Before
	public void Initialize() throws Exception {
		//MockitoAnnotations.initMocks(this);	
		keyStoreSetupMock = mock(KeystoreSetup.class);
	}
	@Test
	public void testgetInstance() {
		KeystoreSetup keystoreMock = Mockito.spy(KeystoreSetup.getInstance());
		
		try (MockedStatic <KeystoreSetup> keyStoreSetupMock = Mockito.mockStatic(KeystoreSetup.class)) {
			keyStoreSetupMock.when(() -> { KeystoreSetup.getInstance(); })
	      .thenReturn(keystoreMock);
		}
	}
}
