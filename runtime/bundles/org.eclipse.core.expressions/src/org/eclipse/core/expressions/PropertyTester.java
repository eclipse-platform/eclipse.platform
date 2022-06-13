/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.expressions;

import org.osgi.framework.Bundle;

import org.eclipse.core.internal.expressions.PropertyTesterDescriptor;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

/**
 * Abstract superclass of all property testers. Implementation classes of
 * the extension point <code>org.eclipse.core.expresssions.propertyTesters
 * </code> must extend <code>PropertyTester</code>.
 * <p>
 * A property tester implements the property tests enumerated in the property
 * tester extension point. For the following property test extension
 * </p>
 * <pre>
 *   &lt;propertyTester
 *     	 namespace="org.eclipse.jdt.core"
 *       id="org.eclipse.jdt.core.IPackageFragmentTester"
 *       properties="isDefaultPackage"
 *       type="org.eclipse.jdt.core.IPackageFragment"
 *       class="org.eclipse.demo.MyPackageFragmentTester"&gt;
 *     &lt;/propertyTester&gt;
 * </pre>
 * the corresponding implementation class looks like:
 * <pre>
 *   public class MyPackageFragmentTester extends PropertyTester {
 *       public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
 *           IPackageFragment fragment= (IPackageFragment)receiver;
 *	         if ("isDefaultPackage".equals(property)) {
 *               return expectedValue == null
 *               	? fragment.isDefaultPackage()
 *               	: fragment.isDefaultPackage() == ((Boolean)expectedValue).booleanValue();
 *           }
 *           Assert.isTrue(false);
 *           return false;
 *       }
 *   }
 * </pre>
 * The property can then be used in a test expression as follows:
 * <pre>
 *   &lt;instanceof value="org.eclipse.core.IPackageFragment"/&gt;
 *   &lt;test property="org.eclipse.jdt.core.isDefaultPackage"/&gt;
 * </pre>
 * <p>
 * There is no guarantee that the same instance of a property tester is used
 * to handle &lt;test property="..."/&gt; requests. So property testers
 * should always be implemented in a stateless fashion.
 * </p>
 * @since 3.0
 */
public abstract class PropertyTester implements IPropertyTester {

	private IConfigurationElement fConfigElement;
	private String fNamespace;
	private String fProperties;

	/**
	 * Initialize the property tester with the given name space and property.
	 * <p>
	 * Note: this method is for internal use only. Clients must not call
	 * this method.
	 * </p>
	 * @param descriptor the descriptor object for this tester
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public final void internalInitialize(PropertyTesterDescriptor descriptor) {
		fProperties= descriptor.getProperties();
		fNamespace= descriptor.getNamespace();
		fConfigElement= descriptor.getConfigurationElement();
	}

	/**
	 * Note: this method is for internal use only. Clients must not call
	 * this method.
	 *
	 * @return the property tester descriptor
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public final PropertyTesterDescriptor internalCreateDescriptor() {
		return new PropertyTesterDescriptor(fConfigElement, fNamespace, fProperties);
	}

	@Override
	public final boolean handles(String namespace, String property) {
		return fNamespace.equals(namespace) && fProperties.contains("," + property + ",");  //$NON-NLS-1$//$NON-NLS-2$
	}

	@Override
	public final boolean isInstantiated() {
		return true;
	}

	@Override
	public boolean isDeclaringPluginActive() {
		Bundle bundle= Platform.getBundle(fConfigElement.getContributor().getName());
		return bundle.getState() == Bundle.ACTIVE;
	}

	@Override
	public final IPropertyTester instantiate() {
		return this;
	}
}
