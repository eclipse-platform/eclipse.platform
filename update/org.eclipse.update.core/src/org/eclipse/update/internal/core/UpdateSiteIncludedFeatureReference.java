/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import org.eclipse.update.core.*;

/**
 * Feature reference on an update site.
 * @since 3.1
 */
public class UpdateSiteIncludedFeatureReference extends IncludedFeatureReference{
    private String os;
    private String ws;
    private String nl;
    private String arch;
    private String patch;
 
 public UpdateSiteIncludedFeatureReference() {
  super();
 }

    public UpdateSiteIncludedFeatureReference(IIncludedFeatureReference include) {
  super(include);
 }


 /**
     * Get optional operating system specification as a comma-separated string.
     *
     * @return the operating system specification string, or <code>null</code>.
     * @since 3.1
     */
    public String getOS() {
        return os;
    }


    /**
     * Get optional windowing system specification as a comma-separated string.
     *
     * @return the windowing system specification string, or <code>null</code>.
     * @since 3.1
     */
    public String getWS() {
        return ws;
    }


    /**
     * Get optional system architecture specification as a comma-separated string.
     *
     * @return the system architecture specification string, or <code>null</code>.
     * @since 3.1
     */
    public String getOSArch() {
        return arch;
    }


    /**
     * Get optional locale specification as a comma-separated string.
     *
     * @return the locale specification string, or <code>null</code>.
     * @since 3.1
     */
    public String getNL() {
        return nl;
    }

    /**
     * Sets the operating system specification.
     * Throws a runtime exception if this object is marked read-only.
     *
     * @param os operating system specification as a comma-separated list
     * @since 3.1
     */
    public void setOS(String os) {
        assertIsWriteable();
        this.os = os;
    }


    /**
     * Sets the windowing system specification.
     * Throws a runtime exception if this object is marked read-only.
     *
     * @param ws windowing system specification as a comma-separated list
     * @since 3.1
     */
    public void setWS(String ws) {
        assertIsWriteable();
        this.ws = ws;
    }


    /**
     * Sets the locale specification.
     * Throws a runtime exception if this object is marked read-only.
     *
     * @param nl locale specification as a comma-separated list
     * @since 3.1
     */
    public void setNL(String nl) {
        assertIsWriteable();
        this.nl = nl;
    }


    /**
     * Sets the system architecture specification.
     * Throws a runtime exception if this object is marked read-only.
     *
     * @param arch system architecture specification as a comma-separated list
     * @since 3.1
     */
    public void setArch(String arch) {
        assertIsWriteable();
        this.arch = arch;
    }

    /**
     * Returns the patch mode.
     * @since 3.1
     */
    public String getPatch() {
        return patch;
    }


    /**
     * Sets the patch mode.
     * @since 3.1
     */
    public void setPatch(String patch) {
        this.patch = patch;
    }

}
