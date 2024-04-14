/*******************************************************************************
 * Copyright (c) 2014, 2020  Dirk Fauth and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Dirk Fauth <dirk.fauth@googlemail.com> - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 474274
 ******************************************************************************/

package org.eclipse.e4.core.internal.tests.nls;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Locale;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.translation.TranslationService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class MessageRegistryTest {

	static class TestObject {
		@Inject
		BundleMessagesRegistry registry;
	}

	static class TestLocalizableObject {
		private String localizableValue;

		public String getLocalizableValue() {
			return localizableValue;
		}

		public void setLocalizableValue(String localizableValue) {
			this.localizableValue = localizableValue;
		}
	}

	private IEclipseContext context;

	@Before
	public void setUp() {
		this.context = EclipseContextFactory.getServiceContext(FrameworkUtil.getBundle(getClass()).getBundleContext());
		ContextInjectionFactory.setDefault(context);
	}

	@Test
	public void testRegisterLocalizationByProperty() {
		// ensure the en Locale is set for this test
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestObject o = ContextInjectionFactory.make(TestObject.class, this.context);

		TestLocalizableObject control = new TestLocalizableObject();
		o.registry.registerProperty(control, "localizableValue", "message");

		// test value is set
		assertNotNull(control.getLocalizableValue());

		// test the set value
		assertEquals("BundleMessage", control.getLocalizableValue());
	}

	@Test
	public void testRegisterLocalizationByMethod() {
		// ensure the en Locale is set for this test
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestObject o = ContextInjectionFactory.make(TestObject.class, this.context);

		TestLocalizableObject control = new TestLocalizableObject();
		o.registry.register(control, "setLocalizableValue", "message");

		// test value is set
		assertNotNull(control.getLocalizableValue());

		// test the set value
		assertEquals("BundleMessage", control.getLocalizableValue());
	}

	@Test
	public void testRegisterLocalizationByMethodReference() {
		// ensure the en Locale is set for this test
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestObject o = ContextInjectionFactory.make(TestObject.class, this.context);

		TestLocalizableObject control = new TestLocalizableObject();
		o.registry.register(control::setLocalizableValue, m -> m.message);

		// test value is set
		assertNotNull(control.getLocalizableValue());

		// test the set value
		assertEquals("BundleMessage", control.getLocalizableValue());
	}

	@Test
	public void testRegisterLocalizationByPropertyAndChangeLocale() {
		// ensure the en Locale is set for this test
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestObject o = ContextInjectionFactory.make(TestObject.class, this.context);

		TestLocalizableObject control = new TestLocalizableObject();
		o.registry.registerProperty(control, "localizableValue", "message");

		// test value is set
		assertNotNull(control.getLocalizableValue());

		// test the set value
		assertEquals("BundleMessage", control.getLocalizableValue());

		// change the locale to GERMAN
		this.context.set(TranslationService.LOCALE, Locale.GERMAN);

		assertEquals("BundleNachricht", control.getLocalizableValue());
	}

	@Test
	public void testRegisterLocalizationByMethodAndChangeLocale() {
		// ensure the en Locale is set for this test
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestObject o = ContextInjectionFactory.make(TestObject.class, this.context);

		TestLocalizableObject control = new TestLocalizableObject();
		o.registry.register(control, "setLocalizableValue", "message");

		// test value is set
		assertNotNull(control.getLocalizableValue());

		// test the set value
		assertEquals("BundleMessage", control.getLocalizableValue());

		// change the locale to GERMAN
		this.context.set(TranslationService.LOCALE, Locale.GERMAN);

		assertEquals("BundleNachricht", control.getLocalizableValue());
	}

	@Test
	public void testRegisterLocalizationByMethodReferenceAndChangeLocale() {
		// ensure the en Locale is set for this test
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestObject o = ContextInjectionFactory.make(TestObject.class, this.context);

		TestLocalizableObject control = new TestLocalizableObject();
		o.registry.register(control::setLocalizableValue, BundleMessages::getMessage);

		// test value is set
		assertNotNull(control.getLocalizableValue());

		// test the set value
		assertEquals("BundleMessage", control.getLocalizableValue());

		// change the locale to GERMAN
		this.context.set(TranslationService.LOCALE, Locale.GERMAN);

		assertEquals("BundleNachricht", control.getLocalizableValue());
	}
}
