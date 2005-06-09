/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.model;

import java.io.File;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;
import org.eclipse.ant.internal.ui.editor.AntEditorCompletionProcessor;
import org.xml.sax.Attributes;

public class AntAntNode extends AntTaskNode {

    private String fFile;

    public AntAntNode(Task task, Attributes attributes) {
        super(task);
        StringBuffer label= new StringBuffer("ant "); //$NON-NLS-1$
        fFile= attributes.getValue(IAntModelConstants.ATTR_DIR);
        if (fFile != null) {
            label.append(fFile);
            label.append(File.separatorChar);
        } else {
            File basedir= task.getProject().getBaseDir();
            if (basedir != null) {
                fFile=basedir.getAbsolutePath();
            }
        }
        String fileName = attributes.getValue(IAntModelConstants.ATTR_ANT_FILE);
        if (fileName == null) {
            fileName= "build.xml"; //$NON-NLS-1$
        }
        label.append(fileName);
        if (fFile == null || FileUtils.isAbsolutePath(fileName)) {
            fFile= fileName;
        } else {
            fFile+=File.separatorChar + fileName;
        }
        
        String more = attributes.getValue(IAntModelConstants.ATTR_TARGET);
        if(more != null) {
            label.append(' ');
            label.append(more);
        }
        setBaseLabel(label.toString());
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ant.internal.ui.model.AntElementNode#getReferencedElement(int)
     */
    public String getReferencedElement(int offset) {
        if (fFile != null) {
            String textToSearch= getAntModel().getText(getOffset(), offset - getOffset());
            if (textToSearch != null && textToSearch.length() != 0) {
            	String attributeString = AntEditorCompletionProcessor.getAttributeStringFromDocumentStringToPrefix(textToSearch);
            	if (IAntModelConstants.ATTR_ANT_FILE.equals(attributeString)) {
            		return fFile;
            	}
            }
        }
        return null;
    }
}
