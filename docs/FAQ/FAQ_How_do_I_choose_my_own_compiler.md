

FAQ How do I choose my own compiler?
====================================

The JDT compiler is tightly integrated with the rest of the JDT. Extracting the compiler out of the JDT and properly integrating a different compiler is not trivial. A quick approach is to disable the Java builder from the project's **Builders** property page, and replace it with an Ant task that calls javac or another compiler. However, we strongly advise you to go with the installed compiler. It knows exactly how to interact with the rest of Eclipse-for instance, by assisting in the creation of tasks, quick fixes, and source decorators. It is one of the fastest, most complete Java compilers available. Finally, the JDT compiler can generate class files even when the source contains compilation errors.

  

By activating **Window > Preferences > Java > Compiler**, you have full control over the reporting style of the compiler, severity of error conditions, what to do with unused code, and how to treat javadoc comments.

  

  
Using the Preference page, you can also select the JDK compliance level of the compiler, the version of generated class files, and whether the compiler should generate debugging symbols.

See Also:
---------

[FAQ Why can't my Ant build find javac?](FAQ_Why_cant_my_Ant_build_find_javac.md)

