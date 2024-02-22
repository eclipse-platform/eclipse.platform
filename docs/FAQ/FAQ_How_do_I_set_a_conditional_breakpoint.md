

FAQ How do I set a conditional breakpoint?
==========================================

First, set a breakpoint at a given location. Then, use the context menu on the breakpoint in the left editor margin or in the Breakpoints view in the Debug perspective, and select the breakpoint's properties. In the dialog box, check **Enable Condition**, and enter an arbitrary Java condition, such as **list.size()==0**. Now, each time the breakpoint is reached, the expression is evaluated in the context of the breakpoint execution, and the breakpoint is either ignored or honored, depending on the outcome of the expression.

Conditions can also be expressed in terms of other breakpoint attributes, such as hit count.

### Why do I get the message "Conditional breakpoint has compilation error(s) - <variable> cannot be resolved" when my breakpoint gets hit?

It can occur that an error message is issued when a conditional breakpoint gets hit, even though the breakpoint condition appears to be syntactically correct:
  

![Conditional breakpoint in java.lang.Class.PNG](https://github.com/eclipse-platform/eclipse.platform/blob/master/docs/FAQ/images/Conditional_Breakpoint_Error.PNG)  

  

This can happen if you are setting a breakpoint in a class whose class file does not contain a local variable table. For example, let's say you want to set a conditional breakpoint on `Class.forName(String)`. If you have a source attachment for `rt.jar` the content assist will allow you to refer to the argument by its variable name, `className`. However, at debug runtime, the variable name will only be known if the class file contains a local variable table. Depending on the options used at compilation time, this information may have been stripped from the class file.

In the **Variables** view of the debugger, the argument will appear as `arg`_n_, and that placeholder name can actually also be used in the conditional expression for the breakpoint. So, instead of using the variable name `className` in your conditional expression, you simply use the placeholder `arg0`:

![Conditional breakpoint in java.lang.Class.PNG](https://github.com/eclipse-platform/eclipse.platform/blob/master/docs/FAQ/images/Conditional_breakpoint_in_java.lang.Class.PNG)  


  

The content assist does currently not work for completing `arg`_n_ expressions.

  

