/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.tests.resources.perf;

import java.util.ArrayList;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.OldCorePerformanceTest;

/**
 * Benchmarks for <code>ElementTree</code>.
 */
public class BenchElementTree extends OldCorePerformanceTest {
	static String[] javaLangUnits = {"AbstractMethodError.java", "ArithmeticException.java", "ArrayIndexOutOfBoundsException.java", "ArrayStoreException.java", "Boolean.java", //
			"Byte.java", "Character.java", "Class.java", "ClassCastException.java", "ClassCircularityError.java", "ClassFormatError.java", "ClassLoader.java", "ClassNotFoundException.java", //
			"Cloneable.java", "CloneNotSupportedException.java", "Compiler.java", "Double.java", "Error.java", "Exception.java", "ExceptionInInitializerError.java", "Float.java", "FloatingDecimal.java", //
			"IllegalAccessError.java", "IllegalAccessException.java", "IllegalArgumentException.java", "IllegalMonitorStateException.java", "IllegalStateException.java", "IllegalThreadStateException.java", //
			"IncompatibleClassChangeError.java", "IndexOutOfBoundsException.java", "InstantiationError.java", "InstantiationException.java", "Integer.java", "InternalError.java", "InterruptedException.java", //
			"LinkageError.java", "Long.java", "Math.java", "NegativeArraySizeException.java", "NoClassDefFoundError.java", "NoSuchFieldError.java", "NoSuchFieldException.java", "NoSuchMethodError.java", //
			"NoSuchMethodException.java", "NullPointerException.java", "Number.java", "NumberFormatException.java", "Object.java", "OutOfMemoryError.java", "Process.java", "Runnable.java", //
			"Runtime.java", "RuntimeException.java", "SecurityException.java", "SecurityManager.java", "Short.java", "StackOverflowError.java", "String.java", "StringBuilder.java", //
			"StringIndexOutOfBoundsException.java", "System.java", "Thread.java", "ThreadDeath.java", "ThreadGroup.java", "Throwable.java", "UnknownError.java", "UnsatisfiedLinkError.java", //
			"VerifyError.java", "VirtualMachineError.java", "Void.java"};

	static final IPath solution = Path.ROOT.append("solution");
	static final IPath project = solution.append("project");
	static final IPath folder = project.append("folder");
	static final IPath[] files = getFilePaths();

	public BenchElementTree() {
		super();
	}

	public BenchElementTree(String name) {
		super(name);
	}

	/**
	 * Tests the performance of the createElement operation.
	 */
	public void testCreateElement() {
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				createTestTree(false);
			}
		}.run(this, 10, 400);
	}

	/**
	 * Tests the performance of the deleteElement operation.
	 */
	public void benchDeleteElement() {
		final int repeat = 400;

		/* create copies of the original tree */
		ElementTree[] trees = new ElementTree[repeat];
		for (int i = 0; i < repeat; i++) {
			trees[i] = createTestTree(false);
		}

		startBench();
		for (int rep = repeat; --rep >= 0;) {
			for (IPath file : files) {
				trees[rep].deleteElement(file);
			}
		}

		stopBench("benchDeleteElement", repeat * files.length);
	}

	/**
	 * Tests the performance of the getElementData operation.
	 */
	public void benchGetElementData() {
		final int repeat = 500;

		ElementTree tree = createTestTree(false);

		startBench();

		for (int rep = repeat; --rep >= 0;) {
			for (IPath file : files) {
				tree.getElementData(file);
			}
		}

		stopBench("benchGetElementData", repeat * files.length);
	}

	/**
	 * Tests the performance of the mergeDeltaChain operation.
	 */
	public void benchMergeDeltaChain() {
		final int repeat = 50;

		/* create all the test trees */
		ElementTree[] bases = new ElementTree[repeat];
		ElementTree[][] chains = new ElementTree[repeat][];
		for (int i = 0; i < repeat; i++) {
			bases[i] = createTestTree(true);
			chains[i] = buildDeltaChain(createTestTree(false));
		}
		startBench();

		for (int i = repeat; --i >= 0;) {
			bases[i].mergeDeltaChain(folder, chains[i]);
		}

		stopBench("benchMergeDeltaChain", repeat);
	}

	/**
	 * Tests a typical series of ElementTree operations, where
	 * a new delta is generated for each operation.
	 */
	public void benchRoutineOperations() {
		final int repeat = 75;

		int opCount = 0;
		startBench();

		for (int i = 0; i < repeat; i++) {
			opCount += doRoutineOperations();
		}

		stopBench("benchRoutineOperations", opCount);
	}

	/**
	 * Tests the performance of the setElementData operation.
	 */
	public void benchSetElementData() {
		ElementTree tree = createTestTree(false);
		Object data = new Object();

		startBench();

		for (int rep = 500; --rep >= 0;) {
			for (IPath file : files) {
				tree.setElementData(file, data);
			}
		}

		stopBench("benchSetElementData", 500 * files.length);
	}

	/**
	 * Does several routine operations on the given tree.
	 * Returns an array of all the intermediary element trees.
	 */
	private ElementTree[] buildDeltaChain(ElementTree tree) {
		ArrayList<ElementTree> trees = new ArrayList<>();
		trees.add(tree);

		int repeat = 1;

		/* create file elements */
		tree = tree.newEmptyDelta();
		IPath[] filePaths = getFilePaths();
		for (IPath filePath : filePaths) {
			Object data = filePath.toString();
			tree.createElement(filePath, data);

			tree.immutable();
			trees.add(tree);
			tree = tree.newEmptyDelta();
		}

		/* modify the data of all file elements a few times */
		for (int i = 0; i < repeat; i++) {
			Object data = "data" + i;
			for (IPath filePath : filePaths) {
				tree.setElementData(filePath, data);

				tree.immutable();
				trees.add(tree);
				tree = tree.newEmptyDelta();
			}
		}

		/* delete all file elements */
		for (IPath filePath : filePaths) {
			tree.deleteElement(filePath);
			tree.immutable();
			trees.add(tree);
			tree = tree.newEmptyDelta();
		}

		ElementTree[] results = new ElementTree[trees.size()];
		trees.toArray(results);
		return results;
	}

	/**
	 * Creates a test element tree.  If withDeltas is true,
	 * a new delta is created after each operation.
	 */
	ElementTree createTestTree(boolean withDeltas) {
		ElementTree tree = new ElementTree();
		Object data = new Object();

		tree.createElement(solution, data);

		if (withDeltas) {
			tree.immutable();
			tree = tree.newEmptyDelta();
		}
		tree.createElement(project, data);
		if (withDeltas) {
			tree.immutable();
			tree = tree.newEmptyDelta();
		}
		tree.createElement(folder, data);

		for (String javaLangUnit : javaLangUnits) {
			if (withDeltas) {
				tree.immutable();
				tree = tree.newEmptyDelta();
			}
			IPath file = folder.append(javaLangUnit);
			tree.createElement(file, data);
		}
		return tree;
	}

	/**
	 * Tests a typical series of ElementTree operations, where
	 * a new delta is generated for each operation.
	 * Returns the number of basic ElementTree operations performed.
	 */
	private int doRoutineOperations() {
		ElementTree tree = createTestTree(true);
		int repeat = 1;

		/* modify the data of all file elements a few times */
		for (int i = 0; i < repeat; i++) {
			Object data = "data" + i;
			for (IPath file : files) {
				tree = tree.newEmptyDelta();
				tree.setElementData(file, data);
				tree.immutable();
			}
		}

		/* delete all file elements */
		for (IPath file : files) {
			tree = tree.newEmptyDelta();
			tree.deleteElement(file);
			tree.immutable();
		}

		return (repeat + 2) * files.length;
	}

	static IPath[] getFilePaths() {
		IPath[] jcuIDs = new IPath[javaLangUnits.length];
		for (int i = 0, len = javaLangUnits.length; i < len; ++i) {
			jcuIDs[i] = folder.append(javaLangUnits[i]);
		}
		return jcuIDs;
	}

}
