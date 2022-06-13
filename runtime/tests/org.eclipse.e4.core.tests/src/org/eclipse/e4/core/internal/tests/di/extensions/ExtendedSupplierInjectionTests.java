/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 474274
 *******************************************************************************/
package org.eclipse.e4.core.internal.tests.di.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.di.suppliers.ExtendedObjectSupplier;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.eclipse.e4.core.internal.di.osgi.ProviderHelper;
import org.eclipse.e4.core.internal.tests.di.extensions.InjectionEventTest.EventAdminHelper;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

public class ExtendedSupplierInjectionTests {
	static final String TOPIC = "org/eclipse/e4/core/tests/di/extensions/ExtendedSupplierInjectionTests";
	static final String TOPIC_430041 = "org/eclipse/e4/core/tests/di/extensions/ExtendedSupplierInjectionTests430041";

	static class EventTestObject {
		static int count = 0;

		Object injectedObject;

		@Inject
		@Optional
		void dontExecute(@EventTopic(TOPIC) Object x) {
			count++;
			injectedObject = x;
		}
	}

	static class EventTestObject_430041 {
		static int count = 0;

		Object injectedObject;

		private boolean destroyed;

		@Inject
		@Optional
		void dontExecute(@EventTopic(TOPIC_430041) Object x) {
			count++;
			injectedObject = x;
		}

		@PreDestroy
		void goDown() {
			this.destroyed = true;
		}
	}

	private EventAdminHelper helper;

	@Before
	public void setUp() {
		InjectionEventTest.ensureEventAdminStarted();
		BundleContext bundleContext = FrameworkUtil.getBundle(getClass())
				.getBundleContext();
		IEclipseContext localContext = EclipseContextFactory
				.getServiceContext(bundleContext);
		helper = ContextInjectionFactory.make(
				EventAdminHelper.class, localContext);
	}

	/* Ensure extended suppliers are looked up first */
	@Test
	public void testBug398728() {
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Object.class, new Object());

		assertEquals(0, EventTestObject.count);

		EventTestObject target = ContextInjectionFactory.make(
				EventTestObject.class, context);
		// should be 0 since we haven't posted an event with this topic yet
		assertEquals(0, EventTestObject.count);

		helper.sendEvent(TOPIC, "event1data");

		assertEquals(1, EventTestObject.count);
		assertEquals("event1data", target.injectedObject);
	}

	@Test
	public void testBug430041() {
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Object.class, new Object());

		assertEquals(0, EventTestObject_430041.count);

		EventTestObject_430041 target = ContextInjectionFactory.make(
				EventTestObject_430041.class, context);
		context.set(EventTestObject_430041.class, target);
		// should be 0 since we haven't posted an event with this topic yet
		assertEquals(0, EventTestObject_430041.count);

		helper.sendEvent(TOPIC_430041, "event1data");

		assertEquals(1, EventTestObject_430041.count);
		assertEquals("event1data", target.injectedObject);

		context.dispose();
		assertTrue(target.destroyed);

		helper.sendEvent(TOPIC_430041, "event1data_disposed");

		assertEquals(1, EventTestObject_430041.count);
		assertEquals("event1data", target.injectedObject);
	}

	/** bug 428837: ensure suppliers are ranked by service.ranking */
	@Test
	public void testSupplierOrdering() {
		BundleContext bc = FrameworkUtil.getBundle(getClass())
				.getBundleContext();
		ExtendedObjectSupplier supplier = new ExtendedObjectSupplier() {
			@Override
			public Object get(IObjectDescriptor descriptor,
					IRequestor requestor, boolean track, boolean group) {
				// TODO Auto-generated method stub
				return null;
			}
		};
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(ExtendedObjectSupplier.SERVICE_CONTEXT_KEY,
				EventTopic.class.getName());
		properties.put(Constants.SERVICE_RANKING, 100);
		ServiceRegistration<?> sr = bc.registerService(
				ExtendedObjectSupplier.SERVICE_NAME, supplier, properties);
		try {
			assertEquals(supplier, ProviderHelper.findProvider(
					EventTopic.class.getName(), null));
		} finally {
			sr.unregister();
		}
	}

}
