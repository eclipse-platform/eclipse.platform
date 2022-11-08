/*******************************************************************************
 *  Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import java.util.Random;
import org.eclipse.core.tests.harness.FussyProgressMonitor;
import org.junit.Assert;

/**
 * Abstract superclass of inner classes used for black-box testing
 * For debugging certain failure cases, insert a breakpoint in performTestRecursiveLoop
 * according to the comment in that method.
 */
public abstract class TestPerformer {
	private int count = 0;

	/**
	 * TestPerformer constructor comment.
	 */
	public TestPerformer(String name) {
		super();
	}

	public void cleanUp(Object[] args, int countArg) {
		// do nothing
	}

	public Object[] interestingOldState(Object[] args) throws Exception {
		//subclasses should override to hold onto interesting old state
		return null;
	}

	public abstract Object invokeMethod(Object[] args, int countArg) throws Exception;

	public final void performTest(Object[][] inputs) {
		// call helper method
		int permutations = 1;
		for (Object[] input : inputs) {
			permutations = permutations * input.length;
			if (input.length > 2) {
				scramble(input);
			}
		}
		//	System.out.println("\nTesting " + permutations + " permutations of " + name);
		performTestRecursiveLoop(inputs, new Object[inputs.length], 0);
	}

	/**
	 * Loop through imaginary (nth) index variable, setting args[nth] to inputs[nth][i].
	 * Then invoke method if nth==inputs.length-1, otherwise do recursive call
	 * with incremented nth.
	 */
	private void performTestRecursiveLoop(Object[][] inputs, Object[] args, int nth) {
		for (Object input : inputs[nth]) {
			args[nth] = input;
			if (nth == inputs.length - 1) {
				// breakpoint goes here, may be conditional on name and count, e.g.:
				// name.equals("IResourceTest.testMove") && count==2886
				if (shouldFail(args, count)) {
					try {
						invokeMethod(args, count);
						StringBuilder buffer = new StringBuilder();
						buffer.append("invocation " + count + " should fail, but it doesn't [");
						for (Object arg : args) {
							buffer.append(arg);
							buffer.append(',');
						}
						buffer.deleteCharAt(buffer.length() - 1);
						buffer.append(']');
						Assert.fail(buffer.toString());
					} catch (Exception ex) {
					}
				} else {
					Object[] oldState = null;
					try {
						oldState = interestingOldState(args);
					} catch (Exception ex) {
						throw new RuntimeException("call to interestingOldState failed", ex);
					}
					Object result = null;
					try {
						result = invokeMethod(args, count);
					} catch (FussyProgressMonitor.FussyProgressAssertionFailed fussyEx) {
						throw new AssertionError("invocation " + count + ": " + fussyEx.getMessage(), fussyEx);
					} catch (Exception ex) {
						throw new AssertionError("invocation " + count + " failed with " + ex, ex);
					}
					boolean success = false;
					try {
						success = wasSuccess(args, result, oldState);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					Assert.assertTrue("invocation " + count + " did not produce desired result", success);
				}
				cleanUp(args, count);
				count++;
			} else {
				performTestRecursiveLoop(inputs, args, nth + 1);
			}
		}
	}

	/**
	 * scrambles an array in a deterministic manner (note the constant seed...).
	 */
	protected static void scramble(Object[] first) {
		Random random = new Random(4711);

		final int len = first.length;
		for (int i = 0; i < len * 100; i++) {
			/* get any array offset */
			int off1 = (int) (random.nextFloat() * len);
			if (off1 == len) {
				continue;
			}

			/* get another array offset */
			int off2 = (int) (random.nextFloat() * len);
			if (off2 == len) {
				continue;
			}

			/* switch */
			Object temp = first[off1];
			first[off1] = first[off2];
			first[off2] = temp;
		}
	}

	public abstract boolean shouldFail(Object[] args, int countArg);

	public abstract boolean wasSuccess(Object[] args, Object result, Object[] oldState) throws Exception;
}
