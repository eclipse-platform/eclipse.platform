/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

/**
 * Private helper function for use by other (public) functions. 
 */
function findHelpTop() {
	var helpTop;
	for (helpTop=self; helpTop; helpTop = helpTop.parent){
		if (helpTop.liveActionInternal){
			break;
		}
		if (helpTop==helpTop.parent){
			break;
		}
	}
	return helpTop;
}

/**
 * Call this Javascript method to execute a serialized command in
 * the workbench.
 *
 * The parameter is a serialized parameterized command as described
 * in the JavaDoc for ParameterizedCommand#serialize().
 */
function executeCommand(command)
{
	liveAction(
		"org.eclipse.help.ui",
		"org.eclipse.help.ui.internal.ExecuteCommandAction",
		command);
}

/**
 * Call this Javascript method to trigger a specified live help action
 * in the workbench. 
 * The parameters for liveAction  are:
 * - the id of the plug-in that contains the action
 * - the name of the class that implements the action
 * - the String that will be passed to the live help action using setInitializationString
 */

function liveAction(pluginId, className, argument)
{
	// find top help frameset
	var helpTop=findHelpTop();
	if (helpTop != null && helpTop.liveActionInternal){
		return helpTop.liveActionInternal(helpTop, pluginId, className, argument);
	} else if (helpTop == self){
		// no frames, possibly help view
		var url= window.location.href;
		
		var i = url.indexOf("?");
		if(i>0)
			url=url.substring(0, i);
		
		i = url.indexOf("/ntopic/");
		if(i < 0) {
			// not help view
			return;
		}
	
		url=url.substring(0, i+1);
		var encodedArg=encodeURIComponent(argument);
		url=url+"livehelp/?pluginID="+pluginId+"&class="+className+"&arg="+encodedArg+"&nocaching="+Math.random();
		window.location.href = url;
	}
}

/**
 * Show specified topic in the Contents tree.
 * The topic must be passed as a URL string.
 * Example:
 *  // include the script first
 *  <script src="../org.eclipse.help/livehelp.js"></script>
 *  ......
 *  // show specified topic in the tree
 *  showTopicInContents(window.location.href); 
 */
function showTopicInContents(topic) {
		var helpTop=findHelpTop();
		if (helpTop != null && helpTop.showTopicInContentsInternal){
			return helpTop.showTopicInContentsInternal(helpTop, topic);
		}
}
