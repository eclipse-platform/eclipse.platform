/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
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
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 548516
 ******************************************************************************/

package org.eclipse.e4.core.tests;

import org.eclipse.e4.core.internal.tests.about.AboutTest;
import org.eclipse.e4.core.internal.tests.about.InstalledFeaturesTest;
import org.eclipse.e4.core.internal.tests.contexts.ActivationTest;
import org.eclipse.e4.core.internal.tests.contexts.ContextDynamicTest;
import org.eclipse.e4.core.internal.tests.contexts.DependenciesLeakTest;
import org.eclipse.e4.core.internal.tests.contexts.EclipseContextTest;
import org.eclipse.e4.core.internal.tests.contexts.NeutralValueTest;
import org.eclipse.e4.core.internal.tests.contexts.ReparentingTest;
import org.eclipse.e4.core.internal.tests.contexts.RunAndTrackTest;
import org.eclipse.e4.core.internal.tests.contexts.StrongIterableTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.ActivationInjectionTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.AnnotationsInjectionTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.Bug317183Test;
import org.eclipse.e4.core.internal.tests.contexts.inject.ComplexDisposalTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.ContextFunctionDynamicsTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.ContextInjectionDisposeTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.ContextInjectionFactoryTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.ContextInjectionTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.DisposingReferencedContextTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.ExtraDependenciesTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.GenericsInjectionTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.GroupedUpdatesTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.InjectStaticContextTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.InjectionUpdateTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.InvokeInRATTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.ParentContextDisposalTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.ProviderInjectionTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.ServiceContextTest;
import org.eclipse.e4.core.internal.tests.contexts.inject.TestConstructorInjection;
import org.eclipse.e4.core.internal.tests.di.AutoConstructWithCreatableTest;
import org.eclipse.e4.core.internal.tests.di.DisposeClassLinkTest;
import org.eclipse.e4.core.internal.tests.di.InjectArraysTest;
import org.eclipse.e4.core.internal.tests.di.InjectBaseTypeTest;
import org.eclipse.e4.core.internal.tests.di.InjectBridgeTest;
import org.eclipse.e4.core.internal.tests.di.InjectionOrderTest;
import org.eclipse.e4.core.internal.tests.di.InjectionResultLeakTest;
import org.eclipse.e4.core.internal.tests.di.InvokeTest;
import org.eclipse.e4.core.internal.tests.di.InvokeTestMissingAnnotation;
import org.eclipse.e4.core.internal.tests.di.RecursiveObjectCreationTest;
import org.eclipse.e4.core.internal.tests.di.extensions.ExtendedSupplierInjectionTests;
import org.eclipse.e4.core.internal.tests.di.extensions.InjectionEventTest;
import org.eclipse.e4.core.internal.tests.di.extensions.InjectionMixedSuppliersTest;
import org.eclipse.e4.core.internal.tests.di.extensions.InjectionOSGiHandlerTest;
import org.eclipse.e4.core.internal.tests.di.extensions.InjectionOSGiTest;
import org.eclipse.e4.core.internal.tests.di.extensions.InjectionPreferencesTest;
import org.eclipse.e4.core.internal.tests.di.extensions.ServiceSupplierTestCase;
import org.eclipse.e4.core.internal.tests.nls.MessageRegistryTest;
import org.eclipse.e4.core.internal.tests.nls.NLSTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		ExtendedSupplierInjectionTests.class,
		InjectionPreferencesTest.class,
		InjectionMixedSuppliersTest.class,
		InjectionEventTest.class,
		InjectionOSGiTest.class,
		InjectionOSGiHandlerTest.class,
		ServiceSupplierTestCase.class,

		// About
		AboutTest.class,
		InstalledFeaturesTest.class,

		// DI
		InjectionOrderTest.class,
		InvokeTest.class,
		InjectBaseTypeTest.class,
		InvokeTestMissingAnnotation.class,
		InjectionResultLeakTest.class,
		AutoConstructWithCreatableTest.class,

		// Contexts
		StrongIterableTest.class,
		EclipseContextTest.class,
		ContextInjectionTest.class,
		ContextInjectionDisposeTest.class,
		ContextInjectionFactoryTest.class,
		ContextDynamicTest.class,
		ReparentingTest.class,
		RunAndTrackTest.class,
		ParentContextDisposalTest.class,
		ComplexDisposalTest.class,
		DisposeClassLinkTest.class,
		InjectStaticContextTest.class,
		ActivationTest.class,
		NeutralValueTest.class,

		// Contexts injection
		AnnotationsInjectionTest.class,
		TestConstructorInjection.class,
		ServiceContextTest.class,
		ProviderInjectionTest.class,
		InjectionUpdateTest.class,
		DisposingReferencedContextTest.class,
		InjectionOrderTest.class,
		GroupedUpdatesTest.class,
		ExtraDependenciesTest.class,
		ContextFunctionDynamicsTest.class,
		InjectArraysTest.class,
		InjectBridgeTest.class,
		InvokeInRATTest.class,
		Bug317183Test.class,
		DependenciesLeakTest.class,
		ActivationInjectionTest.class,
		GenericsInjectionTest.class,
		RecursiveObjectCreationTest.class,

		// NLS
		NLSTest.class,
		MessageRegistryTest.class,
	})
public class CoreTestSuite {
	public static Test suite() {
		return new JUnit4TestAdapter(CoreTestSuite.class);
	}
}
