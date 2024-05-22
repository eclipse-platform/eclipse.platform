/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.harness.session;

import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * The implementation of the {@link SessionTestExtension} is to be instantiated
 * during remote execution of a single session test case. It ensures that
 * before/after methods are only executed if they are not marked to be executed
 * on the host.
 */
class SessionTestExtensionRemote implements SessionTestExtension {

	@Override
	public void interceptAfterAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		skipIfExecuteInHost(invocation, invocationContext);
	}

	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		skipIfExecuteInHost(invocation, invocationContext);
	}

	@Override
	public void interceptBeforeAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		skipIfExecuteInHost(invocation, invocationContext);
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		skipIfExecuteInHost(invocation, invocationContext);
	}

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		skipIfExecuteInHost(invocation, invocationContext);
	}

	private void skipIfExecuteInHost(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext)
			throws Throwable {
		boolean shouldExecuteInHost = invocationContext.getExecutable().getAnnotation(ExecuteInHost.class) != null;
		if (shouldExecuteInHost) {
			invocation.skip();
		} else {
			invocation.proceed();
		}
	}

	@Override
	public void setEclipseArgument(String key, String value) {
		// Do nothing
	}

	@Override
	public void setSystemProperty(String key, String value) {
		// Do nothing
	}

}
