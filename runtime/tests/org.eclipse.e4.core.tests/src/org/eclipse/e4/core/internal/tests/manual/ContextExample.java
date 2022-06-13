/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.e4.core.internal.tests.manual;

import java.text.NumberFormat;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.RunAndTrack;
import org.eclipse.e4.core.internal.tests.CoreTestsActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 *
 */
public class ContextExample {
	static class Crayon {
		@Inject
		IPaletteService pallete;

		public void draw() {
			if (pallete == null)
				System.out.println("No palette");
			else
				System.out.println("My pen is:  " + pallete.getColor());
		}
	}

	static enum Color {
		RED, BLUE, YELLOW, GREEN, ORANGE, PURPLE;
	}

	interface IPaletteService {
		public Color getColor();
	}

	static class PaletteImpl implements IPaletteService {
		private final Color color;

		PaletteImpl(Color color) {
			this.color = color;
		}

		@Override
		public Color getColor() {
			return color;
		}
	}

	static class ComplementaryColor extends ContextFunction {
		@Override
		public Object compute(IEclipseContext context, String contextKey) {
			switch ((Color) context.get("color")) {
			case RED:
				return Color.GREEN;
			case GREEN:
				return Color.RED;
			case BLUE:
				return Color.ORANGE;
			case ORANGE:
				return Color.BLUE;
			case YELLOW:
				return Color.PURPLE;
			case PURPLE:
				return Color.YELLOW;
			default:
				return null;
			}
		}
	}

	static class ResourceSelection extends ContextFunction {
		@Override
		public Object compute(IEclipseContext context, String contextKey) {
			return null;
		}
	}

	public static void main(String[] arguments) {
		new ContextExample().price();
	}

	/**
	 *
	 */
	public void run() {
		IEclipseContext parent = EclipseContextFactory.create();
		parent.set("complement", new ComplementaryColor());
		IEclipseContext context = parent.createChild();
		context.set("color", Color.YELLOW);
		Crayon crayon = new Crayon();
		ContextInjectionFactory.inject(crayon, context);
		crayon.draw();
	}

	public void runWithService() {
		BundleContext bundleContext = CoreTestsActivator.getDefault().getBundleContext();
		ServiceRegistration<?> reg = bundleContext.registerService(IPaletteService.class
				.getName(), new PaletteImpl(Color.BLUE), null);
		IEclipseContext context = EclipseContextFactory.getServiceContext(bundleContext);
		Crayon crayon = new Crayon();
		ContextInjectionFactory.inject(crayon, context);
		crayon.draw();
		reg.unregister();
		crayon.draw();
	}

	public void run2() {
		IEclipseContext parent = EclipseContextFactory.create();
		parent.set("complement", new ComplementaryColor());
		IEclipseContext child = parent.createChild();
		child.set("color", Color.RED);
		System.out.println(child.get("color"));
		System.out.println(child.get("complement"));

	}

	public void run3() {
		// IEclipseContext context = EclipseContextFactory.create();
		// Object[] args = new Object[] {IResource.class};
		// IResource[] resources = context.get("Selection", args);
	}

	double total = 0;

	public void price() {
		final IEclipseContext context = EclipseContextFactory.create();
		context.set("price", 19.99);
		context.set("tax", 0.05);
		context.runAndTrack(new RunAndTrack() {
			@Override
			public boolean changed(IEclipseContext context) {
				total = (Double) context.get("price") * (1.0 + (Double) context.get("tax"));
				return true;
			}

			@Override
			public String toString() {
				return "calculator";
			}
		});
		print(total);
		context.set("tax", 0.07);
		print(total);
	}

	private void print(double price) {
		System.out.println(NumberFormat.getCurrencyInstance().format(price));
	}
}
