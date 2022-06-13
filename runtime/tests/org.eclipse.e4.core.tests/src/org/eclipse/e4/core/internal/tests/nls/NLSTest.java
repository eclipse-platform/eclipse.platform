/*******************************************************************************
 * Copyright (c) 2014, 2015  Dirk Fauth and others.
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
import org.eclipse.e4.core.internal.tests.CoreTestsActivator;
import org.eclipse.e4.core.services.nls.Translation;
import org.eclipse.e4.core.services.translation.TranslationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NLSTest {

	static class TestSimpleObject {
		@Inject
		@Translation
		SimpleMessages simpleMessages;
	}

	static class TestMessagesObject {
		@Inject
		@Translation
		Messages Messages;
	}

	static class TestBundleObject {
		@Inject
		@Translation
		BundleMessages bundleMessages;
	}

	static class TestResourceBundleClassObject {
		@Inject
		@Translation
		ResourceBundleClassMessages bundleClassMessages;
	}

	static class TestResourcesBundleObject {
		@Inject
		@Translation
		ResourcesMessages resourcesMessages;
	}

	private IEclipseContext context;
	private Locale beforeLocale;

	@Before
	public void setUp() {
		this.context = EclipseContextFactory.getServiceContext(CoreTestsActivator.getDefault().getBundleContext());
		ContextInjectionFactory.setDefault(context);

		beforeLocale = Locale.getDefault();
		//always set the locale to en prior a test case
		Locale.setDefault(new Locale("en"));
	}

	@After
	public void tearDown() {
		Locale.setDefault(beforeLocale);
	}

	@Test
	public void testSimpleMessages() {
		//ensure the en Locale is set for this test
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestSimpleObject o = ContextInjectionFactory.make(TestSimpleObject.class, this.context);

		SimpleMessages messages = o.simpleMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//test the set values
		assertEquals("SimpleMessage", messages.message);
		assertEquals("SimpleMessageUnderscore", messages.message_one);
		assertEquals("SimpleMessageCamelCase", messages.messageOne);
		assertEquals("SimpleMessageUnderscoreDot", messages.message_two);
		assertEquals("SimpleMessageCamelCaseDot", messages.messageThree);
		assertEquals("The idea is from Tom", messages.messageFour);
		assertEquals("SimpleMessageCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("SimpleMessageCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("SimpleMessageCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified", messages.messageSeven_Sub);
		assertEquals("SimpleMessageCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("SimpleMessageCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified", messages.messageNine_Sub);
	}

	@Test
	public void testSimpleMessagesDifferentLocale() {
		//set Locale to de
		this.context.set(TranslationService.LOCALE, Locale.GERMAN);
		TestSimpleObject o = ContextInjectionFactory.make(TestSimpleObject.class, this.context);

		SimpleMessages messages = o.simpleMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//test the set values
		assertEquals("SimpleNachricht", messages.message);
		assertEquals("SimpleNachrichtUnderscore", messages.message_one);
		assertEquals("SimpleNachrichtCamelCase", messages.messageOne);
		assertEquals("SimpleNachrichtUnderscoreDot", messages.message_two);
		//for messageThree there is no key specified in de properties file, so there should be the fallback
		assertEquals("SimpleMessageCamelCaseDot", messages.messageThree);
		assertEquals("Die Idee ist von Tom", messages.messageFour);
		assertEquals("SimpleNachrichtCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("SimpleNachrichtCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("SimpleNachrichtCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified",
				messages.messageSeven_Sub);
		assertEquals("SimpleNachrichtCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("SimpleNachrichtCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified",
				messages.messageNine_Sub);
	}

	@Test
	public void testSimpleMessagesSkipDefaultLocaleForEquinoxRoot() {
		//change the default Locale for this testcase
		Locale.setDefault(new Locale("de"));

		//set the locale to en
		//as there is no _en properties file, by default the _de properties file would be loaded and we would
		//get german translations as the default locale is set to "de_DE"
		//with checking the equinox.root.locale in the system properties the fallback is skipped as it tells
		//that the root properties file is for locale en.
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestSimpleObject o = ContextInjectionFactory.make(TestSimpleObject.class, this.context);

		SimpleMessages messages = o.simpleMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//not the default resource bundle should be used (e.g. de when running on a machine with german OS)
		//but the default resource bundle
		assertEquals("SimpleMessage", messages.message);
		assertEquals("SimpleMessageUnderscore", messages.message_one);
		assertEquals("SimpleMessageCamelCase", messages.messageOne);
		assertEquals("SimpleMessageUnderscoreDot", messages.message_two);
		assertEquals("SimpleMessageCamelCaseDot", messages.messageThree);
		assertEquals("The idea is from Tom", messages.messageFour);
		assertEquals("SimpleMessageCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("SimpleMessageCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("SimpleMessageCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified", messages.messageSeven_Sub);
		assertEquals("SimpleMessageCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("SimpleMessageCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified", messages.messageNine_Sub);
	}

	@Test
	public void testSimpleMessagesUseDefaultLocaleForInvalidLocale() {
		//change the default Locale for this testcase
		Locale.setDefault(new Locale("de"));

		//set a locale for which no resource bundle is set
		this.context.set(TranslationService.LOCALE, Locale.FRENCH);
		TestSimpleObject o = ContextInjectionFactory.make(TestSimpleObject.class, this.context);

		SimpleMessages messages = o.simpleMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//the default resource bundle should be used
		assertEquals("SimpleNachricht", messages.message);
		assertEquals("SimpleNachrichtUnderscore", messages.message_one);
		assertEquals("SimpleNachrichtCamelCase", messages.messageOne);
		assertEquals("SimpleNachrichtUnderscoreDot", messages.message_two);
		//for messageThree there is no key specified in de properties file, so there should be the fallback
		assertEquals("SimpleMessageCamelCaseDot", messages.messageThree);
		assertEquals("Die Idee ist von Tom", messages.messageFour);
		assertEquals("SimpleNachrichtCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("SimpleNachrichtCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("SimpleNachrichtCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified",
				messages.messageSeven_Sub);
		assertEquals("SimpleNachrichtCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("SimpleNachrichtCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified",
				messages.messageNine_Sub);
	}

	@Test
	public void testMessages() {
		//ensure the en Locale is set for this test
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestMessagesObject o = ContextInjectionFactory.make(TestMessagesObject.class, this.context);

		Messages messages = o.Messages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//test the set values
		assertEquals("Message", messages.message);
		assertEquals("MessageUnderscore", messages.message_one);
		assertEquals("MessageCamelCase", messages.messageOne);
		assertEquals("MessageUnderscoreDot", messages.message_two);
		assertEquals("MessageCamelCaseDot", messages.messageThree);
		assertEquals("The idea is from Tom", messages.messageFour);
		assertEquals("MessageCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("MessageCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("MessageCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified", messages.messageSeven_Sub);
		assertEquals("MessageCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("MessageCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified", messages.messageNine_Sub);
	}

	@Test
	public void testMessagesDifferentLocale() {
		//set Locale to de
		this.context.set(TranslationService.LOCALE, Locale.GERMAN);
		TestMessagesObject o = ContextInjectionFactory.make(TestMessagesObject.class, this.context);

		Messages messages = o.Messages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//test the set values
		assertEquals("Nachricht", messages.message);
		assertEquals("NachrichtUnderscore", messages.message_one);
		assertEquals("NachrichtCamelCase", messages.messageOne);
		assertEquals("NachrichtUnderscoreDot", messages.message_two);
		//for messageThree there is no key specified in de properties file, so there should be the fallback
		assertEquals("MessageCamelCaseDot", messages.messageThree);
		assertEquals("Die Idee ist von Tom", messages.messageFour);
		assertEquals("NachrichtCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("NachrichtCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("NachrichtCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified",
				messages.messageSeven_Sub);
		assertEquals("NachrichtCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("NachrichtCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified",
				messages.messageNine_Sub);
	}

	@Test
	public void testMessagesSkipDefaultLocaleForEquinoxRoot() {
		//change the default Locale for this testcase
		Locale.setDefault(new Locale("de"));

		//set the locale to en
		//as there is no _en properties file, by default the _de properties file would be loaded and we would
		//get german translations as the default locale is set to "de_DE"
		//with checking the equinox.root.locale in the system properties the fallback is skipped as it tells
		//that the root properties file is for locale en.
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestMessagesObject o = ContextInjectionFactory.make(TestMessagesObject.class, this.context);

		Messages messages = o.Messages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//not the default resource bundle should be used (e.g. de when running on a machine with german OS)
		//but the default resource bundle
		assertEquals("Message", messages.message);
		assertEquals("MessageUnderscore", messages.message_one);
		assertEquals("MessageCamelCase", messages.messageOne);
		assertEquals("MessageUnderscoreDot", messages.message_two);
		assertEquals("MessageCamelCaseDot", messages.messageThree);
		assertEquals("The idea is from Tom", messages.messageFour);
		assertEquals("MessageCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("MessageCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("MessageCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified", messages.messageSeven_Sub);
		assertEquals("MessageCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("MessageCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified", messages.messageNine_Sub);
	}

	@Test
	public void testMessagesUseDefaultLocaleForInvalidLocale() {
		//change the default Locale for this testcase
		Locale.setDefault(new Locale("de"));

		//set a locale for which no resource bundle is set
		this.context.set(TranslationService.LOCALE, Locale.FRENCH);
		TestMessagesObject o = ContextInjectionFactory.make(TestMessagesObject.class, this.context);

		Messages messages = o.Messages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//the default resource bundle should be used
		assertEquals("Nachricht", messages.message);
		assertEquals("NachrichtUnderscore", messages.message_one);
		assertEquals("NachrichtCamelCase", messages.messageOne);
		assertEquals("NachrichtUnderscoreDot", messages.message_two);
		//for messageThree there is no key specified in de properties file, so there should be the fallback
		assertEquals("MessageCamelCaseDot", messages.messageThree);
		assertEquals("Die Idee ist von Tom", messages.messageFour);
		assertEquals("NachrichtCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("NachrichtCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("NachrichtCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified",
				messages.messageSeven_Sub);
		assertEquals("NachrichtCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("NachrichtCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified",
				messages.messageNine_Sub);
	}

	@Test
	public void testBundleMessages() {
		//ensure the en Locale is set for this test
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestBundleObject o = ContextInjectionFactory.make(TestBundleObject.class, this.context);

		BundleMessages messages = o.bundleMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//test the set values
		assertEquals("BundleMessage", messages.message);
		assertEquals("BundleMessageUnderscore", messages.message_one);
		assertEquals("BundleMessageCamelCase", messages.messageOne);
		assertEquals("BundleMessageUnderscoreDot", messages.message_two);
		assertEquals("BundleMessageCamelCaseDot", messages.messageThree);
		assertEquals("The idea is from Tom", messages.messageFour);
		assertEquals("BundleMessageCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("BundleMessageCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("BundleMessageCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified", messages.messageSeven_Sub);
		assertEquals("BundleMessageCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("BundleMessageCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified", messages.messageNine_Sub);
	}

	@Test
	public void testBundleMessagesDifferentLocale() {
		//set Locale to de
		this.context.set(TranslationService.LOCALE, Locale.GERMAN);
		TestBundleObject o = ContextInjectionFactory.make(TestBundleObject.class, this.context);

		BundleMessages messages = o.bundleMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//test the set values
		assertEquals("BundleNachricht", messages.message);
		assertEquals("BundleNachrichtUnderscore", messages.message_one);
		assertEquals("BundleNachrichtCamelCase", messages.messageOne);
		assertEquals("BundleNachrichtUnderscoreDot", messages.message_two);
		//for messageThree there is no key specified in de properties file, so there should be the fallback
		assertEquals("BundleMessageCamelCaseDot", messages.messageThree);
		assertEquals("Die Idee ist von Tom", messages.messageFour);
		assertEquals("BundleNachrichtCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("BundleNachrichtCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("BundleNachrichtCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified",
				messages.messageSeven_Sub);
		assertEquals("BundleNachrichtCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("BundleNachrichtCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified",
				messages.messageNine_Sub);
	}

	@Test
	public void testBundleMessagesSkipDefaultLocaleForEquinoxRoot() {
		//change the default Locale for this testcase
		Locale.setDefault(new Locale("de"));

		//set the locale to en
		//as there is no _en properties file, by default the _de properties file would be loaded and we would
		//get german translations as the default locale is set to "de_DE"
		//with checking the equinox.root.locale in the system properties the fallback is skipped as it tells
		//that the root properties file is for locale en.
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestBundleObject o = ContextInjectionFactory.make(TestBundleObject.class, this.context);

		BundleMessages messages = o.bundleMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//not the default resource bundle should be used (e.g. de when running on a machine with german OS)
		//but the default resource bundle
		assertEquals("BundleMessage", messages.message);
		assertEquals("BundleMessageUnderscore", messages.message_one);
		assertEquals("BundleMessageCamelCase", messages.messageOne);
		assertEquals("BundleMessageUnderscoreDot", messages.message_two);
		assertEquals("BundleMessageCamelCaseDot", messages.messageThree);
		assertEquals("The idea is from Tom", messages.messageFour);
		assertEquals("BundleMessageCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("BundleMessageCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("BundleMessageCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified", messages.messageSeven_Sub);
		assertEquals("BundleMessageCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("BundleMessageCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified", messages.messageNine_Sub);
	}

	@Test
	public void testBundleMessagesUseDefaultLocaleForInvalidLocale() {
		//change the default Locale for this testcase
		Locale.setDefault(new Locale("de"));

		//set a locale for which no resource bundle is set
		this.context.set(TranslationService.LOCALE, Locale.FRENCH);
		TestBundleObject o = ContextInjectionFactory.make(TestBundleObject.class, this.context);

		BundleMessages messages = o.bundleMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//the default resource bundle should be used
		assertEquals("BundleNachricht", messages.message);
		assertEquals("BundleNachrichtUnderscore", messages.message_one);
		assertEquals("BundleNachrichtCamelCase", messages.messageOne);
		assertEquals("BundleNachrichtUnderscoreDot", messages.message_two);
		//for messageThree there is no key specified in de properties file, so there should be the fallback
		assertEquals("BundleMessageCamelCaseDot", messages.messageThree);
		assertEquals("Die Idee ist von Tom", messages.messageFour);
		assertEquals("BundleNachrichtCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("BundleNachrichtCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("BundleNachrichtCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified",
				messages.messageSeven_Sub);
		assertEquals("BundleNachrichtCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("BundleNachrichtCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified",
				messages.messageNine_Sub);
	}

	@Test
	public void testClassBasedResourceBundle() {
		//ensure the en Locale is set for this test
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestResourceBundleClassObject o = ContextInjectionFactory.make(TestResourceBundleClassObject.class, this.context);

		ResourceBundleClassMessages messages = o.bundleClassMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//test the set values
		assertEquals("ResourceBundleClassMessage", messages.message);
		assertEquals("ResourceBundleClassMessageUnderscore", messages.message_one);
		assertEquals("ResourceBundleClassMessageCamelCase", messages.messageOne);
		assertEquals("ResourceBundleClassMessageUnderscoreDot", messages.message_two);
		assertEquals("ResourceBundleClassCamelCaseDot", messages.messageThree);
		assertEquals("The idea is from Tom", messages.messageFour);
		assertEquals("ResourceBundleClassMessageCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("ResourceBundleClassMessageCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("ResourceBundleClassMessageCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified",
				messages.messageSeven_Sub);
		assertEquals("ResourceBundleClassMessageCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("ResourceBundleClassMessageCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified",
				messages.messageNine_Sub);
	}

	@Test
	public void testClassBasedResourceBundleDifferentLocale() {
		//set Locale to de
		this.context.set(TranslationService.LOCALE, Locale.GERMAN);
		TestResourceBundleClassObject o = ContextInjectionFactory.make(TestResourceBundleClassObject.class, this.context);

		ResourceBundleClassMessages messages = o.bundleClassMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//test the set values
		assertEquals("ResourceBundleClassNachricht", messages.message);
		assertEquals("ResourceBundleClassNachrichtUnderscore", messages.message_one);
		assertEquals("ResourceBundleClassNachrichtCamelCase", messages.messageOne);
		assertEquals("ResourceBundleNachrichtMessageUnderscoreDot", messages.message_two);
		//for messageThree there is no key specified in de properties file, so there should be the fallback
		assertEquals("ResourceBundleClassCamelCaseDot", messages.messageThree);
		assertEquals("Die Idee ist von Tom", messages.messageFour);
		assertEquals("ResourceBundleClassNachrichtCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("ResourceBundleClassNachrichtCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("ResourceBundleClassNachrichtCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified",
				messages.messageSeven_Sub);
		assertEquals("ResourceBundleClassNachrichtCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("ResourceBundleClassNachrichtCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified",
				messages.messageNine_Sub);
	}

	@Test
	public void testClassBasedResourceBundleSkipDefaultLocaleForEquinoxRoot() {
		//change the default Locale for this testcase
		Locale.setDefault(new Locale("de"));

		//set the locale to en
		//as there is no _en properties file, by default the _de properties file would be loaded and we would
		//get german translations as the default locale is set to "de_DE"
		//with checking the equinox.root.locale in the system properties the fallback is skipped as it tells
		//that the root properties file is for locale en.
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestResourceBundleClassObject o = ContextInjectionFactory.make(TestResourceBundleClassObject.class, this.context);

		ResourceBundleClassMessages messages = o.bundleClassMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//not the default resource bundle should be used (e.g. de when running on a machine with german OS)
		//but the default resource bundle
		assertEquals("ResourceBundleClassMessage", messages.message);
		assertEquals("ResourceBundleClassMessageUnderscore", messages.message_one);
		assertEquals("ResourceBundleClassMessageCamelCase", messages.messageOne);
		assertEquals("ResourceBundleClassMessageUnderscoreDot", messages.message_two);
		assertEquals("ResourceBundleClassCamelCaseDot", messages.messageThree);
		assertEquals("The idea is from Tom", messages.messageFour);
		assertEquals("ResourceBundleClassMessageCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("ResourceBundleClassMessageCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("ResourceBundleClassMessageCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified",
				messages.messageSeven_Sub);
		assertEquals("ResourceBundleClassMessageCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("ResourceBundleClassMessageCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified",
				messages.messageNine_Sub);
	}

	@Test
	public void testClassBasedResourceBundleUseDefaultLocaleForInvalidLocale() {
		//change the default Locale for this testcase
		Locale.setDefault(new Locale("de"));

		//set a locale for which no resource bundle is set
		this.context.set(TranslationService.LOCALE, Locale.FRENCH);
		TestResourceBundleClassObject o = ContextInjectionFactory.make(TestResourceBundleClassObject.class, this.context);

		ResourceBundleClassMessages messages = o.bundleClassMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//the default resource bundle should be used
		assertEquals("ResourceBundleClassNachricht", messages.message);
		assertEquals("ResourceBundleClassNachrichtUnderscore", messages.message_one);
		assertEquals("ResourceBundleClassNachrichtCamelCase", messages.messageOne);
		assertEquals("ResourceBundleNachrichtMessageUnderscoreDot", messages.message_two);
		//for messageThree there is no key specified in de properties file, so there should be the fallback
		assertEquals("ResourceBundleClassCamelCaseDot", messages.messageThree);
		assertEquals("Die Idee ist von Tom", messages.messageFour);
		assertEquals("ResourceBundleClassNachrichtCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("ResourceBundleClassNachrichtCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("ResourceBundleClassNachrichtCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified",
				messages.messageSeven_Sub);
		assertEquals("ResourceBundleClassNachrichtCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("ResourceBundleClassNachrichtCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified",
				messages.messageNine_Sub);
	}

	@Test
	public void testResourcesBundle() {
		//ensure the en Locale is set for this test
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestResourcesBundleObject o = ContextInjectionFactory.make(TestResourcesBundleObject.class, this.context);

		ResourcesMessages messages = o.resourcesMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//test the set values
		assertEquals("ResourcesMessage", messages.message);
		assertEquals("ResourcesMessageUnderscore", messages.message_one);
		assertEquals("ResourcesMessageCamelCase", messages.messageOne);
		assertEquals("ResourcesMessageUnderscoreDot", messages.message_two);
		assertEquals("ResourcesMessageCamelCaseDot", messages.messageThree);
		assertEquals("The idea is from Tom", messages.messageFour);
		assertEquals("ResourcesMessageCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("ResourcesMessageCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("ResourcesMessageCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified",
				messages.messageSeven_Sub);
		assertEquals("ResourcesMessageCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("ResourcesMessageCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified",
				messages.messageNine_Sub);
	}

	@Test
	public void testResourcesBundleDifferentLocale() {
		//set Locale to de
		this.context.set(TranslationService.LOCALE, Locale.GERMAN);
		TestResourcesBundleObject o = ContextInjectionFactory.make(TestResourcesBundleObject.class, this.context);

		ResourcesMessages messages = o.resourcesMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//test the set values
		assertEquals("ResourcesNachricht", messages.message);
		assertEquals("ResourcesNachrichtUnderscore", messages.message_one);
		assertEquals("ResourcesNachrichtCamelCase", messages.messageOne);
		assertEquals("ResourcesNachrichtUnderscoreDot", messages.message_two);
		//for messageThree there is no key specified in de properties file, so there should be the fallback
		assertEquals("ResourcesMessageCamelCaseDot", messages.messageThree);
		assertEquals("Die Idee ist von Tom", messages.messageFour);
		assertEquals("ResourcesNachrichtCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("ResourcesNachrichtCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("ResourcesNachrichtCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified",
				messages.messageSeven_Sub);
		assertEquals("ResourcesNachrichtCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("ResourcesNachrichtCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified",
				messages.messageNine_Sub);
	}

	@Test
	public void testResourcesBundleSkipDefaultLocaleForEquinoxRoot() {
		//change the default Locale for this testcase
		Locale.setDefault(new Locale("de"));

		//set the locale to en
		//as there is no _en properties file, by default the _de properties file would be loaded and we would
		//get german translations as the default locale is set to "de_DE"
		//with checking the equinox.root.locale in the system properties the fallback is skipped as it tells
		//that the root properties file is for locale en.
		this.context.set(TranslationService.LOCALE, Locale.ENGLISH);
		TestResourcesBundleObject o = ContextInjectionFactory.make(TestResourcesBundleObject.class, this.context);

		ResourcesMessages messages = o.resourcesMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//not the default resource bundle should be used (e.g. de when running on a machine with german OS)
		//but the default resource bundle
		assertEquals("ResourcesMessage", messages.message);
		assertEquals("ResourcesMessageUnderscore", messages.message_one);
		assertEquals("ResourcesMessageCamelCase", messages.messageOne);
		assertEquals("ResourcesMessageUnderscoreDot", messages.message_two);
		assertEquals("ResourcesMessageCamelCaseDot", messages.messageThree);
		assertEquals("The idea is from Tom", messages.messageFour);
		assertEquals("ResourcesMessageCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("ResourcesMessageCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("ResourcesMessageCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified",
				messages.messageSeven_Sub);
		assertEquals("ResourcesMessageCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("ResourcesMessageCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified",
				messages.messageNine_Sub);
	}

	@Test
	public void testResourcesBundleUseDefaultLocaleForInvalidLocale() {
		//change the default Locale for this testcase
		Locale.setDefault(new Locale("de"));

		//set a locale for which no resource bundle is set
		this.context.set(TranslationService.LOCALE, Locale.FRENCH);
		TestResourcesBundleObject o = ContextInjectionFactory.make(TestResourcesBundleObject.class, this.context);

		ResourcesMessages messages = o.resourcesMessages;

		//test all values are set
		assertNotNull(messages);
		assertNotNull(messages.message);
		assertNotNull(messages.message_one);
		assertNotNull(messages.messageOne);
		assertNotNull(messages.message_two);
		assertNotNull(messages.messageThree);
		assertNotNull(messages.messageFour);
		assertNotNull(messages.messageFive_Sub);
		assertNotNull(messages.messageSix_Sub);
		assertNotNull(messages.messageSeven_Sub);
		assertNotNull(messages.messageEight_Sub);
		assertNotNull(messages.messageNine_Sub);

		//the default resource bundle should be used
		assertEquals("ResourcesNachricht", messages.message);
		assertEquals("ResourcesNachrichtUnderscore", messages.message_one);
		assertEquals("ResourcesNachrichtCamelCase", messages.messageOne);
		assertEquals("ResourcesNachrichtUnderscoreDot", messages.message_two);
		//for messageThree there is no key specified in de properties file, so there should be the fallback
		assertEquals("ResourcesMessageCamelCaseDot", messages.messageThree);
		assertEquals("Die Idee ist von Tom", messages.messageFour);
		assertEquals("ResourcesNachrichtCamelCaseAndUnderscoreOriginal", messages.messageFive_Sub);
		assertEquals("ResourcesNachrichtCamelCaseAndUnderscoreDeCamelCasified", messages.messageSix_Sub);
		assertEquals("ResourcesNachrichtCamelCaseAndUnderscoreDeCamelCasifiedAndDeUnderscorified",
				messages.messageSeven_Sub);
		assertEquals("ResourcesNachrichtCamelCaseAndUnderscoreDeUnderscorified", messages.messageEight_Sub);
		assertEquals("ResourcesNachrichtCamelCaseAndUnderscoreDeUnderscorifiedAndDeCamelCasified",
				messages.messageNine_Sub);
	}

}