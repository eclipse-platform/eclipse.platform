package org.eclipse.core.tests.pki.auth;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.isA;
import org.junit.Before;
import org.junit.Test;
import org.eclipse.core.pki.auth.PublishPasswordUpdate;

public class PublishPasswordUpdateTest {
	Properties properties = new Properties();
	Object o = new Object();
	String testName = "PKItestSubscriber";
	PKITestSubscriber<String> subscriber = null;
	PublishPasswordUpdate publishPasswordUpdateMock = null;
	
	
	public PublishPasswordUpdateTest() {}
	
	@Before
	public void Initialize() throws Exception {
		subscriber = new PKITestSubscriber<String>(testName);
		publishPasswordUpdateMock = mock(PublishPasswordUpdate.class);
	}

	@Test
	public void testRun() {
		assertEquals(this.testName, subscriber.getName(), "The name should be set correctly.");
		assertNotEquals("footest", subscriber.getName(), "The name should be set correctly.");
	}
	
	@Test
	public void testPublishPasswordUpdate() {
		doNothing().when(publishPasswordUpdateMock).subscribe(subscriber);
		when(publishPasswordUpdateMock.getSubscriberCount()).thenReturn(1);
		doNothing().when(publishPasswordUpdateMock).publishMessage(isA(String.class));
		doNothing().when(publishPasswordUpdateMock).close();	
	}
}
