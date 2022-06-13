/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

import org.eclipse.core.runtime.Assert;

/**
 * An evaluation result represents the result of an expression
 * evaluation. There are exact three instances of evaluation
 * result. They are: <code>FALSE</code>, <code>TRUE</code> and
 * <code>NOT_LOADED</code>. <code>NOT_LOADED</code> represents
 * the fact that an expression couldn't be evaluated since a
 * plug-in providing certain test expressions isn't loaded yet.
 * <p>
 * In addition the class implements the three operation <code>and
 * </code>, <code>or</code> and <code>not</code>. The operation are
 * defined as follows:
 * </p>
 * <table border="1" style="text-align: center;">
 *   <caption>The AND operation:</caption>
 *   <colgroup>
 *     <col style="width:120;">
 *     <col style="width:120;">
 *     <col style="width:120;">
 *     <col style="width:120;">
 *   </colgroup>
 *   <tbody>
 *     <tr>
 *       <td style="font-style: italic;">AND</td>
 *       <td>FALSE</td>
 *       <td>TRUE</td>
 *       <td>NOT_LOADED</td>
 *     </tr>
 *     <tr>
 *       <td>FALSE</td>
 *       <td>FALSE</td>
 *       <td>FALSE</td>
 *       <td>FALSE</td>
 *     </tr>
 *     <tr>
 *       <td>TRUE</td>
 *       <td>FALSE</td>
 *       <td>TRUE</td>
 *       <td>NOT_LOADED</td>
 *     </tr>
 *     <tr>
 *       <td>NOT_LOADED</td>
 *       <td>FALSE</td>
 *       <td>NOT_LOADED</td>
 *       <td>NOT_LOADED</td>
 *     </tr>
 *   </tbody>
 * </table>
 * <br>
 * <table border="1" style="text-align: center;">
 *   <caption>The OR operation:</caption>
 *   <colgroup>
 *     <col style="width:120;">
 *     <col style="width:120;">
 *     <col style="width:120;">
 *     <col style="width:120;">
 *   </colgroup>
 *   <tbody>
 *     <tr>
 *       <td style="font-style: italic;">OR</td>
 *       <td>FALSE</td>
 *       <td>TRUE</td>
 *       <td>NOT_LOADED</td>
 *     </tr>
 *     <tr>
 *       <td>FALSE</td>
 *       <td>FALSE</td>
 *       <td>TRUE</td>
 *       <td>NOT_LOADED</td>
 *     </tr>
 *     <tr>
 *       <td>TRUE</td>
 *       <td>TRUE</td>
 *       <td>TRUE</td>
 *       <td>TRUE</td>
 *     </tr>
 *     <tr>
 *       <td>NOT_LOADED</td>
 *       <td>NOT_LOADED</td>
 *       <td>TRUE</td>
 *       <td>NOT_LOADED</td>
 *     </tr>
 *   </tbody>
 * </table>
 * <br>
 * <table border="1" style="text-align: center;">
 *   <caption>The NOT operation:</caption>
 *   <colgroup>
 *     <col style="width:120;">
 *     <col style="width:120;">
 *     <col style="width:120;">
 *     <col style="width:120;">
 *   </colgroup>
 *   <tbody>
 *     <tr>
 *       <td style="font-style: italic;">NOT</td>
 *       <td>FALSE</td>
 *       <td>TRUE</td>
 *       <td>NOT_LOADED</td>
 *     </tr>
 *     <tr>
 *       <td></td>
 *       <td>TRUE</td>
 *       <td>FALSE</td>
 *       <td>NOT_LOADED</td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 * <p>
 * The class is not intended to be subclassed by clients.
 * </p>
 * @since 3.0
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class EvaluationResult {

	private int fValue;

	private static final int FALSE_VALUE= 0;
	private static final int TRUE_VALUE= 1;
	private static final int NOT_LOADED_VALUE= 2;

	/** The evaluation result representing the value FALSE */
	public static final EvaluationResult FALSE= new EvaluationResult(FALSE_VALUE);
	/** The evaluation result representing the value TRUE */
	public static final EvaluationResult TRUE= new EvaluationResult(TRUE_VALUE);
	/** The evaluation result representing the value NOT_LOADED */
	public static final EvaluationResult NOT_LOADED= new EvaluationResult(NOT_LOADED_VALUE);

	private static final EvaluationResult[][] AND= new EvaluationResult[][] {
						// FALSE	//TRUE		//NOT_LOADED
		/* FALSE   */ { FALSE,		FALSE,		FALSE		},
		/* TRUE    */ { FALSE,		TRUE,		NOT_LOADED	},
		/* PNL     */ { FALSE,		NOT_LOADED, NOT_LOADED	},
	};

	private static final EvaluationResult[][] OR= new EvaluationResult[][] {
						// FALSE	//TRUE	//NOT_LOADED
		/* FALSE   */ { FALSE,		TRUE,	NOT_LOADED	},
		/* TRUE    */ { TRUE,		TRUE,	TRUE		},
		/* PNL     */ { NOT_LOADED,	TRUE, 	NOT_LOADED	},
	};

	private static final EvaluationResult[] NOT= new EvaluationResult[] {
		//FALSE		//TRUE	//NOT_LOADED
		TRUE,		FALSE,	NOT_LOADED
	};

	/*
	 * No instances outside of <code>EvaluationResult</code>
	 */
	private EvaluationResult(int value) {
		fValue= value;
	}

	/**
	 * Returns an <code>EvaluationResult</code> whose value is <code>this &amp;&amp; other)</code>.
	 *
	 * @param other the right hand side of the and operation.
	 *
	 * @return <code>this &amp;&amp; other</code> as defined by the evaluation result
	 */
	public EvaluationResult and(EvaluationResult other) {
		return AND[fValue][other.fValue];
	}

	/**
	 * Returns an <code>EvaluationResult</code> whose value is <code>this || other)</code>.
	 *
	 * @param other the right hand side of the or operation.
	 *
	 * @return <code>this || other</code> as defined by the evaluation result
	 */
	public EvaluationResult or(EvaluationResult other) {
		return OR[fValue][other.fValue];
	}

	/**
	 * Returns the inverted value of this evaluation result
	 *
	 * @return the inverted value of this evaluation result
	 */
	public EvaluationResult not() {
		return NOT[fValue];
	}

	/**
	 * Returns an evaluation result instance representing the
	 * given boolean value. If the given boolean value is
	 * <code>true</code> then <code>ExpressionResult.TRUE</code>
	 * is returned. If the value is <code>false</code> then <code>
	 * ExpressionResult.FALSE</code> is returned.
	 *
	 * @param b a boolean value
	 *
	 * @return the expression result representing the boolean
	 *  value
	 */
	public static EvaluationResult valueOf(boolean b) {
		return b ? TRUE : FALSE;
	}

	/**
	 * Returns a evaluation result instance representing the
	 * given <code>Boolean</code> value. If the given <code>Boolean
	 * </code> value is <code>true</code> then <code>ExpressionResult.TRUE</code>
	 * is returned. If the value is <code>false</code> then <code>
	 * ExpressionResult.FALSE</code> is returned.
	 *
	 * @param b a <code>Boolean</code> value
	 *
	 * @return the expression result representing the <code>Boolean</code>
	 *  value
	 */
	public static EvaluationResult valueOf(Boolean b) {
		return b.booleanValue() ? TRUE : FALSE;
	}

	/**
	 * For debugging purpose only
	 *
	 * @return a string representing this object. The result is not
	 *  human readable
	 */
	@Override
	public String toString() {
		switch (fValue) {
			case 0:
				return "false"; //$NON-NLS-1$
			case 1:
				return "true"; //$NON-NLS-1$
			case 2:
				return "not_loaded"; //$NON-NLS-1$
		}
		Assert.isTrue(false);
		return null;
	}
}
