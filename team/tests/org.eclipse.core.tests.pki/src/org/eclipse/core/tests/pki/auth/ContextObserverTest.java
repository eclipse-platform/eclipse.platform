package org.eclipse.core.tests.pki.auth;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.isA;

import org.eclipse.core.pki.auth.ContextObservable;

public class ContextObserverTest {

	ContextObservable contextObservableMock = mock(ContextObservable.class);
	public ContextObserverTest() {}
	
	@Test
	void testOnChange() {
		doNothing().when(contextObservableMock).onchange(isA(String.class));
	}

}
