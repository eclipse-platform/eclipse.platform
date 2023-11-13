/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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
package org.eclipse.e4.core.internal.tests.contexts.inject;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.junit.Test;

/**
 * Tests for the generics context injection functionality
 */
public class GenericsInjectionTest {

	static public class Animal {
	}

	static public class Bird extends Animal {
	}

	static public class Feeder<T extends Animal> {
	}

	static public class BirdHouse extends Feeder<Bird> {
	}

	static public class TestNamedObject {
		public Feeder<Bird> field;

		@Inject
		public void setFeeder(@Named("test") Feeder<Bird> value) {
			field = value;
		}
	}

	static public class TestGenericObject {
		public Feeder<Bird> field;

		@Inject
		public void setFeeder(Feeder<Bird> value) {
			field = value;
		}
	}

	@Test
	public synchronized void testNamedInjection() {
		Animal testAnimal = new Animal();
		Bird testBird = new Bird();
		BirdHouse testBirdHouse = new BirdHouse();

		// create context
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Animal.class, testAnimal);
		context.set(Bird.class, testBird);
		context.set("test", testBirdHouse);

		TestNamedObject userObject = new TestNamedObject();
		ContextInjectionFactory.inject(userObject, context);

		// check field injection
		assertEquals(testBirdHouse, userObject.field);
	}

	@Test
	public synchronized void testGenericInjection() {
		Animal testAnimal = new Animal();
		Bird testBird = new Bird();
		BirdHouse testBirdHouse = new BirdHouse();

		// create context
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Animal.class, testAnimal);
		context.set(Bird.class, testBird);
		context.set(Feeder.class, testBirdHouse); // note that BirdHouse is
													// added as Feeder class

		TestGenericObject userObject = new TestGenericObject();
		ContextInjectionFactory.inject(userObject, context);

		// check field injection
		assertEquals(testBirdHouse, userObject.field);
	}

	static public interface Interface<T> {
	}

	static public class Implementation implements Interface<Object> {
	}

	static public class InterfaceTarget {
		@Inject
		public Interface<Object> field;
	}

	@Test
	public void testInterfaceGenericInjection() {
		Implementation implementation = new Implementation();
		// create context
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Interface.class, implementation);

		InterfaceTarget target = new InterfaceTarget();
		ContextInjectionFactory.inject(target, context);

		// check field injection
		assertEquals(implementation, target.field);
	}

	static public class Superclass<T> {
	}

	static public class Subclass extends Superclass<Object> {
	}

	static public class ClassTarget {
		@Inject
		public Superclass<Object> field;
	}

	@Test
	public void testClassGenericInjection() {
		Subclass implementation = new Subclass();
		// create context
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Superclass.class, implementation);

		ClassTarget target = new ClassTarget();
		ContextInjectionFactory.inject(target, context);

		// check field injection
		assertEquals(implementation, target.field);
	}

}
