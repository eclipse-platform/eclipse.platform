/*******************************************************************************
 *  Copyright (c) 2019 ArSysOp and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      Alexander Fedorov <alexander.fedorov@arsysop.ru> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.internal.tests.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.about.AboutSections;
import org.eclipse.e4.core.services.about.ISystemInformation;
import org.eclipse.e4.core.services.translation.TranslationService;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class AboutTest {

	@Test
	public void testCreateSectionFilter() throws Exception {
		String section = AboutSections.SECTION_SYSTEM_ENVIRONMENT;
		BundleContext context = FrameworkUtil.getBundle(AboutTest.class).getBundleContext();
		ServiceReference<ISystemInformation> reference = reference(context, section);
		assertEquals("Invalid service metadata", section, reference.getProperty(AboutSections.SECTION));
	}

	@Test
	public void testSystemEnvironmentVariables() throws Exception {
		String printed = printSection(AboutSections.SECTION_SYSTEM_ENVIRONMENT);
		System.getenv().keySet().forEach(key -> assertTrue(printed.contains(key)));
	}

	@Test
	public void testSystemProperties() throws Exception {
		String printed = printSection(AboutSections.SECTION_SYSTEM_PROPERTIES);
		System.getProperties().keySet().forEach(key -> assertTrue(printed.contains(String.valueOf(key))));
	}

	@Test
	public void testAppendInstalledFeatures() throws Exception {
		// tested with InstalledFeaturesTest, just check that it is present here
		String printed = printSection(AboutSections.SECTION_INSTALLED_FEATURES);
		assertNotNull(printed);
	}

	@Test
	public void testAppendInstalledBundles() throws Exception {
		Bundle bundle = FrameworkUtil.getBundle(AboutTest.class);
		IEclipseContext context = EclipseContextFactory.getServiceContext(bundle.getBundleContext());
		context.set(TranslationService.LOCALE, Locale.getDefault());
		String version = bundle.getVersion().toString();
		String aboutMe = String.format("org.eclipse.e4.core.tests (%s) \"E4 Core Tests\" [Active]", version);
		String printed = printSection(AboutSections.SECTION_INSTALLED_BUNDLES);
		assertTrue(printed.contains(aboutMe));
	}

	@Test
	public void testAppendUserPreferences() throws Exception {
		Bundle bundle = FrameworkUtil.getBundle(AboutTest.class);
		IEclipseContext context = EclipseContextFactory.getServiceContext(bundle.getBundleContext());
		context.set(TranslationService.LOCALE, Locale.getDefault());
		String printed = printSection(AboutSections.SECTION_USER_PREFERENCES);
		assertFalse(printed.isEmpty());
	}

	@Test
	public void testMaskPassword() throws Exception {
		String key = "A_PASSWORD_PROPERTY";
		String dec = "A_PASSWORD_VALUE";
		String enc = "****************";
		System.setProperty(key, dec);
		String printed = printSection(AboutSections.SECTION_SYSTEM_PROPERTIES);
		assertTrue(printed.contains(key + "=" + enc));
	}

	@Test
	public void testMultilineValue() throws Exception {
		String lf = System.lineSeparator();
		String key = "A_MULTILINE_PROPERTY";
		String value = "a" + "\n" + "b" + "\n" + "c";
		String expected = "a" + lf + "b" + lf + "c";
		System.setProperty(key, value);
		String printed = printSection(AboutSections.SECTION_SYSTEM_PROPERTIES);
		assertTrue(printed.contains(key + "=" + expected));
	}

	private String printSection(String section) throws Exception {
		BundleContext context = FrameworkUtil.getBundle(AboutTest.class).getBundleContext();
		ServiceReference<ISystemInformation> reference = reference(context, section);
		ISystemInformation service = context.getService(reference);
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintWriter pw = new PrintWriter(baos)) {
			service.append(pw);
			pw.flush();
			return new String(baos.toByteArray(), Charset.defaultCharset());
		} finally {
			context.ungetService(reference);
		}
	}

	private ServiceReference<ISystemInformation> reference(BundleContext context, String section)
			throws InvalidSyntaxException {
		String filter = AboutSections.createSectionFilter(section);
		Optional<ServiceReference<ISystemInformation>> first = context
				.getServiceReferences(ISystemInformation.class, filter)
				.stream().findFirst();
		assumeTrue("No reference found", first.isPresent());
		return first.get();
	}
}
