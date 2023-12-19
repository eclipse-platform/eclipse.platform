Evolving Java-based APIs 3
==========================

Part 3 of [Evolving\_Java-based\_APIs](/Evolving_Java-based_APIs "Evolving Java-based APIs").

Contents
--------

*   [1 Other notes](#Other-notes)
    *   [1.1 Data Compatibility](#Data-Compatibility)
    *   [1.2 Standard Workarounds](#Standard-Workarounds)
        *   [1.2.1 Deprecate and Forward](#Deprecate-and-Forward)
        *   [1.2.2 Start over in a New Package](#Start-over-in-a-New-Package)
        *   [1.2.3 Adding an Argument](#Adding-an-Argument)
        *   [1.2.4 "2" Convention](#.222.22-Convention)
        *   [1.2.5 COM Style](#COM-Style)
        *   [1.2.6 Making Obsolete Hook Methods Final](#Making-Obsolete-Hook-Methods-Final)
    *   [1.3 Defective API Specifications](#Defective-API-Specifications)
    *   [1.4 A Word about Source Code Incompatibilities](#A-Word-about-Source-Code-Incompatibilities)
    *   [1.5 Java Reflection](#Java-Reflection)
    *   [1.6 Versioning](#Versioning)

Other notes
===========

Data Compatibility
------------------

The Component implementation may need to store and retrieve its internal data from a file. For example, Microsoft Word stores a document in a file. When one of these files may live from release to release, clients would break if the format or interpretation of that data changed in an incompatible way.  
**Data compatibility** is an additional issue for components with persistent data.

The standard technique is to tag all stored data with its format version number. The format version number is increased when the format is changed from one release to the next. The Component implementation contains readers for the current format version and for all past versions, but usually only the writer for the current format version (unless for some reason there is an ongoing need to write older versions).

Standard Workarounds
--------------------

When evolving APIs, the prime directive places serious constraints on how this can be done.

Here are some standard techniques that come in handy when you're caught between a rock and a hard place. They're not necessarily pretty, but they get the job done.

### Deprecate and Forward

When some part of the Component API is made obsolete by some new and improved Component API, the old API should be marked as deprecated using the `@deprecated` Javadoc tag (the comment directing the reader attention to the replacement API). When feasible, the implementation of the old API should forward the message to the corresponding method in the replacement API; doing so will mean that any performance improvements or bug fixes made to the implementation of the new API will automatically be of benefit to clients of the old API.

### Start over in a New Package

Even simpler than Deprecate and Forward, the Component API and implementation can be redone in new packages. The old API and implementation are left in the old location untouched, except to mark them as deprecated. Old and new API and implementations co-exist independent of one another.

### Adding an Argument

Here is a simple technique for adding an argument to a method that is intended to be overridden by subclasses. For example the `Viewer.inputChanged(Object input)` method should get an additional argument `Object oldInput`. Adding the argument results in pre-existing clients overridding the wrong method. The workaround is to call the old method as the default implementation of the new method:

    public void inputChanged(Object input, Object oldInput) {
       inputChanged(input);
    }

Pre-existing clients which override the old method continue to work; and all calls to the old method continue to work. New or upgraded clients will override the new method; and all calls to the new method will work, even if they happen to invoke an old implementation.

Javadocs should document the addition in both API methods.

Since Java 8, this technique also works for interface methods (new method is a default method).

### "2" Convention

The first release of an API callback-style interface didn't work as well as hoped. For example, the first release contained:

    public interface IProgressMonitor {
       void start();
       void stop();
    }
    
You now wish you had something like:

    public interface IProgressMonitor {
       void start(int total);
       void worked(int units);
       void stop();
    }
    
But it's too late to change `IProgressMonitor` to be that API. So you mark `IProgressMonitor` as deprecated and introduce the new and improved one under the name `IProgressMonitor2` (a name everyone recognizes as the second attempt):

    public interface IProgressMonitor2 extends IProgressMonitor {
       void start(int total);
       void worked(int units);
       void stop();
    }
    
By declaring the new interface to extend the old one, any object of type `IProgressMonitor2` can be passed to a method expecting an old `IProgressMonitor`. Don't forget to mention `IProgressMonitor2` in the API of `IProgressMonitor`, even if you don't deprecate it, e.g.:

    /**
     * [...]
     * @see IProgressMonitor2
     */
    public interface IProgressMonitor {
       [...]
    }
    
### COM Style

The "COM style" is to not implement interfaces directly but to ask for an interface by using `getAdapter(someInterfaceID)`. This allows adding new interfaces in the implementation without breaking existing classes.

### Making Obsolete Hook Methods Final

As a framework evolves, it may sometimes be necessary to break compatibility. When compatibility is being broken knowingly, there are some tricks that make it easier for broken clients to find and fix the breakage.

A common situation occurs when the signature of a framework hook method is changed. Overridding a hook method that is no longer called by the base class can be tough to track down, especially if the base class contains a default implementation of the hook method. In order to make this jump out, the obsolete method should be marked as `final` in addition to being deprecated. This ensures that existing subclasses which override the obsolete method will no longer compile or link.

Defective API Specifications
----------------------------

As hard as one might try, achieving perfect APIs is difficult. The harsh reality is that some parts of large Component API will be specified better than others.

One problem is specification bugs---when the API spec actually says the wrong thing. Every effort should be made to catch these prior to release.

Another problem is underspecification---when the API spec does not specify enough. In some cases, the implementor will notice this before the API is ever released. In other cases, the specification will be adequate for the implementor's needs but inadequate for clients. When an API is released in advance of serious usage from real clients, it may be discovered too late that the specification should have been tighter or, even worse, that the API should have been designed differently.

When you find out that you're saddled with a defective API specification, these points are worth bearing in mind:

*   APIs are not sacrosanct; it's just that breaking compatibility is usually very costly. For a truly unusable feature, the cost is likely much lower.
*   Tightening up a seriously weak specification can often be achieved without breaking compatibility by changing the specification in a way consistent with the existing implementation. That is, codify more of how it actually works to ensure that clients that currently work continue to work in subsequent releases.
*   Breaking compatibility in a limited way may be cheaper in the long run that leaving a bad patch of API as it is.
*   If you break compatibility between releases, do it in a controlled way that only breaks those Clients that actually utilize of the bad parts of the API. This localizes the pain to affected Clients (and their downstream customers), rather than foisting a "Big Bang" release on everyone.
*   Document all breaking API changes in the release notes. Clients appreciate this much more than discovering for themselves that you knowingly broke them.

A Word about Source Code Incompatibilities
------------------------------------------

While the idea that the Java source code for existing Clients should continue to compile without errors against the revised Component API, this is not strictly necessary (and not always achievable). API contract and binary compatibility are the only hard requirements. Source code incompatibilities are not worth losing sleep over because the Client's owner can easily correct these problems if they do arise with only localized editing of the source code.

The following is a list of known kinds of Java source code incompatibilities that can arise as APIs evolve:

*   Ambiguities involving type-import-on-demand declarations.

*   Triggered by: adding an API class or interface.
*   Remedy: add single type import declaration to disambiguate.
*   Avoidance strategy: use at most one type-import-on-demand declaration per compilation unit.

*   Ambiguities involving overloaded methods.

*   Triggered by: adding an overloaded API method or constructor.
*   Remedy: add casts to disambiguate ambiguously typed arguments.
*   Avoidance strategy: put casts on null arguments.

*   Ambiguities involving field and type member hiding.

*   Triggered by: adding an API field.
*   Remedy: add qualification to disambiguate ambiguous field references.
*   Avoidance strategy: none.

*   Ambiguities involving fields and local variables.

*   Triggered by: adding an API field.
*   Remedy: rename conflicting local variables to avoid new field name.
*   Avoidance strategy: don't declared API fields in classes and interfaces that Clients implement.

*   Problems involving checked exceptions thrown by methods.

*   Triggered by: removing checked exceptions from a method's `throws` clause.
*   Remedy: add or remove exception handlers as required.
*   Avoidance strategy: none.

Java Reflection
---------------

The Java language provides a reflection mechanism that allows a program to introspect on some aspects of its structure at runtime. Much of what can be found out at runtime is finer-grained that what a program could depend on at compile time. This raises the question of which details a program may rely on; or, equivalently, which aspects of the API must be held fixed so that Clients that use reflection to access the API are not broken.

As a general rule, API contracts should assume that clients are using built-in facilities of the Java language. No additional provisions are made for clients that access the API using Java reflection.

That said, there are no particular problems with clients using Java reflection to access an API, provided they are careful not to rely on something that is not already entrenched in virtue of binary compatibility considerations.

The following methods on `java.lang.Class` are generally safe choices because they are the direct reflective equivalents of the corresponding compile-time features. These methods reveal only public elements, and always take inheritance into account.

*   `Class.forName(String name)`
*   `Class.getClasses()`
*   `Class.getFields()`
*   `Class.getMethods()`
*   `Class.getConstructors()`
*   `Class.getField(String name)`
*   `Class.getMethod(String name, Class... parameterTypes()`
*   `Class.getConstructors(Class... parameterTypes)`

On the other hand, clients should avoid all `Class.getDeclared_XXX_` methods as these are dependent on the exact location of an element and include non-public members as well as public ones.

Versioning
----------

It should be easy for API clients to know whether a new version of your components broke APIs or not. Eclipse projects implement semantic versioning according to the [Version Numbering](/Version_Numbering "Version Numbering") specification.

Copyright © 2000, 2007 IBM Corporation