package org.eclipse.ant.internal.core.old;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;

/**
 * An Ant data type used to specify commands (builder name and arguments for this builder)
 * while working on projects.
 * <p>
 * The name of the builder must be specified.<br>
 * It is posible to specify arguments with nested elements. It is also 
 * possible to reference this command using a reference id.
 * <p><p>
 * Example:<p>
 *	&lt;command name="Main Builder" id="mainBuilder"&gt;
 * 		&lt;eclipse.argument name="arg1" value="value1"/&gt;
 *  &lt;/command&gt;
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost 
 * certainly be broken (repeatedly) as the API evolves.
 * </p>
 * @see CreateProject, MoveProject, CopyProject
 */
public class CommandDataType extends DataType {
	
	/**
	 * The name of the builder. It is an identifier defined in the extension points of Eclipse.
	 */
	private String name = null;
	
	/**
	 * A vector of Argument objects.
	 */
	private HashMap args = null;

	/**
	 * Inner class that represents a name-value pair.
	 */
	public class Argument {
		private String name;

		/**
		 * Sets the name of this argument. 
		 * @param name the name of this argument
		 */
		public void setName(String value) {
			name= value;
		}

		/**
		 * Sets the value of this argument. 
		 * @param value the value of this argument
		 */
		public void setValue(String value) {
			if (value == null)
				args.remove(name);
			else
				args.put(name, value);
		}
	}
	
	
/**
 * Constructs a new <code>CommandDataType</code> instance.
 */	
public CommandDataType() {
	super();
	args= new HashMap(5);
}

public Argument createArgument() {
	if (isReference())
		throw new BuildException(Policy.bind("exception.noNestedElements"));
    return new Argument();
}

/**
 * Makes this instance in effect a reference to another CommandDataType
 * instance.
 *
 * <p>You must not set attributes, just the reference ID.</p> 
 */
public void setRefid(Reference r) throws BuildException {
    if (name != null || !args.isEmpty())
        throw new BuildException(Policy.bind("exception.noAttributes"));
    super.setRefid(r);
}

/**
 * Sets the name of the builder.
 * 
 * @param the name of the builder		
 */
public void setName(String name) {
	if (isReference())
		throw new BuildException(Policy.bind("exception.tooManyAttributes"));
	this.name = name;
}

/**
 * Returns the name of the command builer, or the one of the command that is refered to 
 * via the "refid" attribute.
 * 
 * @param the project to which the CommandDataType belongs
 * @return the name of the builder	
 */
public String getName(Project p) {
	if (isReference())
		return getRef(p).getName(p);
	if (name == null)
		throw new BuildException(Policy.bind("exception.commandMustHaveName"));
	return name;
}

/**
 * Returns the arguments of the command, or the one of the command that is refered to 
 * via the "refid" attribute.<br>
 * The arguments are returned in a <code>HashMap</code> where the names of the arguments
 * are the keys and the values are the actual values in the hashMap.
 * 
 * @param the project to which the CommandDataType belongs
 * @return the hashMap containing the arguments
 */
public HashMap getArguments(Project p) {
	if (isReference())
		return getRef(p).getArguments(p);
	return args;
}

/**
 * Performs the check for circular references and returns the
 * referenced PatternSet.
 * 
 * @param the project to which the CommandDataType belongs
 */
private CommandDataType getRef(Project p) {
    if (!checked) {
        Stack stk = new Stack();
        stk.push(this);
        dieOnCircularReference(stk, p);
    }
    
    Object o = ref.getReferencedObject(p);
    if (!(o instanceof CommandDataType)) {
        throw new BuildException(Policy.bind("exception.doesntDenoteACommand", ref.getRefId()));
    } else {
        return (CommandDataType) o;
    }
}

}

