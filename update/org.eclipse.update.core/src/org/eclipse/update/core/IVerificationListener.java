/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core;
 
/**
 * Verification listener. This interface abstract the user interaction
 * that may be required as a result of feature installation. In particular,
 * as feature archives are downloaded and verified, the user may need to 
 * indicate whether to accept any one of the archives, or abort the 
 * installation.
 * <p>
 * Clients may implement this interface.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.IVerificationResult
 * @see org.eclipse.update.core.IVerifier
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IVerificationListener {
	
	/**
	 * Indicate the action that resulted in this notification should be aborted.
	 * 
	 * @since 2.0
	 */
	public static final int CHOICE_ABORT = 0;
	
	/**
	 * Indicate there was an error in processing the request.
	 * The action that resulted in this notification should be aborted.
	 * 
	 * @since 2.0
	 */
	public static final int CHOICE_ERROR = 1;
	
	/**
	 * Indicate that the target of the verification should be accepted,
	 * but the information supplied with the verification result
	 * should be trusted only for this request.
	 * 
	 * @since 2.0
	 */
	public static final int CHOICE_INSTALL_TRUST_ONCE = 2;
	
	/**
	 * Indicate that the target of the verification should be accepted,
	 * and the information supplied with the verification result
	 * should be trusted for this request, and subsequent requests.
	 * 
	 * @since 2.0
	 */
	public static final int CHOICE_INSTALL_TRUST_ALWAYS = 3;
	
	/**
	 * Determine if we should continue with the current action
	 * based on the indicated verification results. Typically,
	 * the implementation of this method will prompt the user
	 * for the appropriate answer. However, other respose 
	 * implementations can be provided.
	 * 
	 * @param result verification result 
	 * @since 2.0
	 */
	public int prompt(IVerificationResult result);
}
