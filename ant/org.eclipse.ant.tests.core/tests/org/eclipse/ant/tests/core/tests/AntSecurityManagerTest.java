package org.eclipse.ant.tests.core.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.eclipse.ant.internal.core.AntSecurityManager;
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.junit.jupiter.api.Test;

public class AntSecurityManagerTest extends AbstractAntTest {

	@SuppressWarnings({ "deprecation", "removal" }) // SecurityManager
	@Test
	public void test_getInCheck() {
		AntSecurityManager securityManager = new AntSecurityManager(System.getSecurityManager(),
				Thread.currentThread());
		assertFalse(securityManager.getInCheck());
	}

}
