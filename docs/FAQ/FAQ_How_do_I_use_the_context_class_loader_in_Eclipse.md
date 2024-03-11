

FAQ How do I use the context class loader in Eclipse?
=====================================================

In Java, each thread can optionally reference a _context class loader_. This loader can be set at any time by an application and is used for loading classes only when it is explicitly requested to do so. Many code libraries, in particular Java Database Connectivity (JDBC) and Xerces, use the context class loader in factory methods to allow clients of the library to specify what class loader to use. Although the context loader is not used by Eclipse itself, you may need to be aware of it when referencing third-party libraries from within Eclipse.

  

  
By default, the context loader is set to be the application class loader, which is not used in Eclipse. Because Eclipse has a separate class loader for each installed plug-in, a default class loader generally does not make sense as the context loader for a given thread. If you are calling third-party libraries that rely on the context loader, you will need to set it yourself. The following code snippet sets the context class loader before calling a library. Note that the code politely cleans up afterward by resetting the context loader to its original value:

      Thread current = Thread.currentThread();
      ClassLoader oldLoader = current.getContextClassLoader();
      try {
         current.setContextClassLoader(getClass().getClassLoader());
         //call library code here
      } finally {
         current.setContextClassLoader(oldLoader);
      }

  

  

  

See Also:
---------

[FAQ\_What\_is\_the\_classpath\_of\_a_plug-in?](./FAQ_What_is_the_classpath_of_a_plug-in.md "FAQ What is the classpath of a plug-in?")

