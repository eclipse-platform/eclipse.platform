/*******************************************************************************
 * Copyright (c) 2026 Vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.eclipse.core.internal.events.BuildManager;
import org.eclipse.core.internal.events.InternalBuilder;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.jupiter.api.Test;

/**
 * Verifies the format of the enhanced {@link IllegalArgumentException} that
 * {@code BuildManager} throws when a builder hits a scheduling-rule conflict
 * during {@code beginRule} or {@code endRule}. The wrapped message has to
 * identify the offending builder so it can be diagnosed from a stack trace.
 */
public class BuildManagerRuleConflictMessageTest {

	private static final String BUILDER_LABEL = "My Awesome Builder";
	private static final String PLUGIN_ID = "com.example.builders";
	private static final String PROJECT_PATH = "/MyProject";

	@Test
	public void beginRuleConflictMessageIdentifiesOffendingBuilder() throws Exception {
		InternalBuilder builder = newStubBuilder(stubProject(PROJECT_PATH));
		IllegalArgumentException original = new IllegalArgumentException(
				"Attempted to beginRule: P/MyProject/foo, does not match outer scope rule: P/Other");

		IllegalArgumentException result = invokeHandleRuleConflict(true, builder, original);

		assertThat(result.getMessage()) //
				.startsWith("beginRule failed for builder " + builder.getClass().getName()) //
				.contains("'" + BUILDER_LABEL + "'") //
				.contains("plugin " + PLUGIN_ID) //
				.contains("on project " + PROJECT_PATH) //
				.endsWith(": " + original.getMessage());
		assertSame(original, result.getCause());
	}

	@Test
	public void endRuleConflictMessageIdentifiesOffendingBuilder() throws Exception {
		InternalBuilder builder = newStubBuilder(stubProject(PROJECT_PATH));
		IllegalArgumentException original = new IllegalArgumentException(
				"Attempted to endRule: P/MyProject/foo, does not match the rule of current entry: P/MyProject");

		IllegalArgumentException result = invokeHandleRuleConflict(false, builder, original);

		assertThat(result.getMessage()) //
				.startsWith("endRule failed for builder " + builder.getClass().getName()) //
				.contains("'" + BUILDER_LABEL + "'") //
				.contains("plugin " + PLUGIN_ID) //
				.contains("on project " + PROJECT_PATH) //
				.endsWith(": " + original.getMessage());
		assertSame(original, result.getCause());
	}

	@Test
	public void messageFallsBackToPlaceholdersWhenBuilderMetadataMissing() throws Exception {
		InternalBuilder builder = newStubBuilder(null);
		IllegalArgumentException original = new IllegalArgumentException("boom");
		setInternalBuilderField(builder, "label", null);
		setInternalBuilderField(builder, "pluginId", null);

		IllegalArgumentException result = invokeHandleRuleConflict(true, builder, original);

		assertThat(result.getMessage()) //
				.contains("'<unknown>'") //
				.contains("plugin <unknown>") //
				.contains("on project <unknown>");
	}

	private static InternalBuilder newStubBuilder(IProject project) throws Exception {
		IncrementalProjectBuilder builder = new IncrementalProjectBuilder() {
			@Override
			protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) {
				return null;
			}
		};
		setInternalBuilderField(builder, "label", BUILDER_LABEL);
		setInternalBuilderField(builder, "pluginId", PLUGIN_ID);
		setInternalBuilderField(builder, "buildConfiguration", stubBuildConfiguration(project));
		return builder;
	}

	private static IBuildConfiguration stubBuildConfiguration(IProject project) {
		return (IBuildConfiguration) Proxy.newProxyInstance(IBuildConfiguration.class.getClassLoader(),
				new Class<?>[] { IBuildConfiguration.class }, (proxy, method, args) -> {
					if ("getProject".equals(method.getName())) {
						return project;
					}
					return defaultReturnValue(method.getReturnType());
				});
	}

	private static IProject stubProject(String path) {
		IPath fullPath = IPath.fromPortableString(path);
		return (IProject) Proxy.newProxyInstance(IProject.class.getClassLoader(),
				new Class<?>[] { IProject.class }, (proxy, method, args) -> {
					if ("getFullPath".equals(method.getName())) {
						return fullPath;
					}
					return defaultReturnValue(method.getReturnType());
				});
	}

	private static Object defaultReturnValue(Class<?> returnType) {
		if (!returnType.isPrimitive()) {
			return null;
		}
		if (returnType == boolean.class) {
			return Boolean.FALSE;
		}
		if (returnType == void.class) {
			return null;
		}
		return 0;
	}

	private static void setInternalBuilderField(InternalBuilder target, String name, Object value) throws Exception {
		Field field = InternalBuilder.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static IllegalArgumentException invokeHandleRuleConflict(boolean beginRule, InternalBuilder builder,
			IllegalArgumentException original) throws Exception {
		Method method = BuildManager.class.getDeclaredMethod("handleRuleConflict", boolean.class, InternalBuilder.class,
				IllegalArgumentException.class);
		method.setAccessible(true);
		try {
			return (IllegalArgumentException) method.invoke(null, beginRule, builder, original);
		} catch (java.lang.reflect.InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException re) {
				throw re;
			}
			throw new RuntimeException(cause);
		}
	}
}
