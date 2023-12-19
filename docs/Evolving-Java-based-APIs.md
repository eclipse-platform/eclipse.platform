Evolving Java-based APIs
========================

By Jim des Rivières, IBM

Revision history:

October 1, 2007 - revision 1.2 - Clarified moving methods up and down type hierarchy; added note about Java reflection; split document into 3 parts because it was getting too large to edit:

*   Part 1: What is an API? (this page)
*   [Part 2: Achieving API Binary Compatibility](/Evolving_Java-based_APIs_2 "Evolving Java-based APIs 2")
*   [Part 3: Other Notes](/Evolving_Java-based_APIs_3 "Evolving Java-based APIs 3")

February 14, 2007 - revision 1.1 - Add coverage for JDK 1.5 language features: generics, enums, annotation types, variable arity methods.

June 8, 2001 - revision 1.02 - Added note about breakage due to adding API method to classes that may be subclassed.

January 15, 2001 - revision 1.01 - Added suggestion about making obsolete hook methods final.

October 6, 2000 - revision 1.0

This document is about how to evolve Java-based APIs while maintaining compatibility with existing client code. Without loss of generality, we'll assume that there is a generic **Component** with a **Component API**, with one party providing the Component and controlling its API. The other party, or parties, write **Client** code that use the Component's services through its API. This is a very typical arrangement.

Contents
--------

*   [1 API Java Elements](#API-Java-Elements)
*   [2 API Prime Directive](#API-Prime-Directive)
*   [3 Achieving API Contract Compatibility](#Achieving-API-Contract-Compatibility)
    *   [3.1 Example 1 - Changing a method postcondition](#Example-1---Changing-a-method-postcondition)
    *   [3.2 Example 2 - Changing a method precondition](#Example-2---Changing-a-method-precondition)
    *   [3.3 Example 3 - Changing a field invariant](#Example-3---Changing-a-field-invariant)
    *   [3.4 Example 4 - Adding an API method](#Example-4---Adding-an-API-method)
    *   [3.5 General Rules for Contract Compatibility](#General-Rules-for-Contract-Compatibility)
*   [4 Achieving API Binary Compatibility](#Achieving-API-Binary-Compatibility)
*   [5 Other Notes](#Other-Notes)
*   [6 Bundle Versioning](#Bundle-Versioning)

API Java Elements
-----------------

All parties need to understand which Java elements (packages, interfaces, classes, methods, constructors, and fields) are part of the Component API, which Clients may use, and those which are part of the internal Component implementation and are off limits to Clients. Having a clearly-defined and marked boundary between API and non-API is therefore very important. The following convention uses the visibility-limiting features of the Java language to distinguish those Java elements which are considered API from those which are not:

> *   **API package** \- a package that contains at least one API class or API interface. The names of API packages are advertised in the Component documentation. These names will appear in Client code; the names of non-API packages should never appear in Client code. Note that Clients must be prohibited from declaring their code in Component packages (API or otherwise).
> *   **API class** \- a `public` class in an API package, or a `public` or `protected` class member declared in, or inherited by, some other API class or interface. The names of API classes appear in Client code.
> *   **API interface** \- a `public` interface in an API package, or a `public` or `protected` interface member declared in, or inherited by, some other API class or interface. The names of API interfaces appear in Client code.
> *   **API method** \- a `public` or `protected` method either declared in, or inherited by, an API class or interface. The names of API methods appear in Client code.
> *   **API constructor** \- a `public` or `protected` constructor of an API class. The names of API constructors appear in Client code.
> *   **API field** \- a `public` or `protected` field either declared in, or inherited by, an API class or interface. The names of API fields appear in Client code.

The following elements are not considered API:

*   Any package that is not advertised in the Component documentation as an API package.
*   All classes and interfaces declared in non-API packages. However, when API classes and interface extend or implement non-API classes, the non-API classes and interface may contribute API elements nevertheless.
*   Non-`public` classes and interfaces in API packages.
*   Default access and `private` methods, constructors, fields, and type members declared in, or inherited by, API classes and interfaces.
*   `protected` methods, constructors, fields, and type members declared in, or inherited by, final API classes, including enums (which are implicitly final). With no ability to declare subclasses, these cannot be referenced by Client code.

API Prime Directive
-------------------

As the Component evolves from release to release, there is an absolute requirement to not break existing Clients that were written in conformance to Component APIs in an earlier release.

> **API Prime Directive:** _When evolving the Component API from release to release, do not break existing Clients._

Changing an API in a way that is incompatible with existing Clients would mean that all Clients would need to be revised (and even in instances where no actual changes are required, the Client code would still need to be reviewed to ensure that it still works with the revised API). Customers upgrading to a new release of the Component would need to upgrade all their Clients at the same time. Since the overall cost of invalidating existing Client code is usually very high, the more realistic approach is to only change the API in ways that do not invalidate existing Clients.

As the Component API evolves, all pre-existing Clients are expected to continue to work, both in principle and in practice.

Suppose a Client was written to a given release of the Component and abided by the contracts spelled out in the Component API specification.  
The first requirement is that when the Component API evolves to follow-on releases, all pre-existing Client must still be legal according to the contracts spelled out in the revised Component API specification, without having to change the Clients source code. This is what is meant by continuing to work in principle.

> **API Contract Compatibility:** _API changes must not invalidate formerly legal Client code._

Since the set of Clients is open-ended, and we have no way of knowing exactly which aspects of the API are being counted on, the only safe assumption to make when evolving APIs is that every aspect of the API matters to some hypothetical Client, and that any incompatible change to the API contract will cause that hypothetical Client to fail.

> **API Usage Assumption:** _Every aspect of the API matters to some Client._

Under this assumption, deleting something in the API, or backtracking on some promise made in the API, will certainly break some Client. For this reason, obsolete API elements are notoriously difficult to get rid of. Obsolete API elements should be marked as deprecated and point new customers at the new API that replaces it, but need to continue working as advertised for a couple more releases until the expense of breakage is low enough that it can be deleted.

Clients are generally written in Java and are compiled to standard Java binary class files. A Client's class files are typically stored in a JAR file on the Client's library path. It would be unsatisfactory if a Client's class files, which were compiled against one release of the Component, do not successfully link and execute correctly with all later releases of the Component. This is what is meant by continuing to work in practice.

> **API Binary Compatibility:** _Pre-existing Client binaries must link and run with new releases of the Component without recompiling._

Achieving API binary compatibility requires being sensitive to the Java language's notion of binary compatibility [JLS3, chapter 13](http://java.sun.com/docs/books/jls/third_edition/html/binaryComp.html).

While the idea that the Java source code for existing Clients should continue to compile without errors against the revised Component API, this is not strictly necessary (and not always achievable). For instance, adding a new public interface to an existing API package may introduce an ambiguous package reference into source code containing multiple on-demand type (".*") imports. Similarly, removing a method `throws` declaration for a checked exception may cause the compiler to detect dead code in a `try`-`catch` block. Happily, the kinds of problems that could be introduced into Client source code can always be easily corrected. The notion of **API source compatibility** is not a requirement. (Note: Problems detected by a Java compiler are therefore not necessarily indicators of any kind of API compatibility that we care about.)

The following sections discuss how API contract and binary compatibility can be achieved.

Achieving API Contract Compatibility
------------------------------------

> _"How could I have broken anything? All I did was change a comment."_

Since API contracts are captured by the API specification, any change to the API specification risks making code written against the old specification incompatible with the revised specification.

The most confining situation is an API that is specified by one party, implemented by a separate second party, and used by yet a different third party. For example, a standards body promulgates a pure specification (such as the HTTP protocol) but leaves it up to others to write browsers and servers. In such cases, making any changes to the existing specification will almost certainly break client code, implementations, or both.

Fortunately, the case is typically lop-sided. Most commonly, the party responsible for specifying the API also provides the sole implementation. Indeed, this is our earlier assumption about the Component. In this situation, the API owner can unilaterally decide to change the API specification and fix up the implementation to match. However, since they can't do anything about the client code already using the API, the changed API must be contract compatible with the old API: all existing contractual obligations must be honored. Contracts can be tightened to allow users to assume more (and require the implementation to do more); this does not invalidate existing code which would have been written assuming less. Conversely, contracts cannot be loosened to require users to assume less, as this could break existing uses.

Note that in some cases, the contractual roles are reversed. The party responsible for specifying the API provides the uses, whereas other parties provide the implementations. Callback interfaces are a prime example of this situation. Contracts can be loosened to require implementors to provide less (and allow the client to assume less); this does not invalidate existing implementations which would have been written under more stringent rules. Conversely, contracts cannot be tightened to require implementors to provide more, as this could break existing implementations.

When contemplating changing an existing API contract, the key questions to ask are:

*   What roles does the API contract involve?  
    For a method contract, there is the caller and the implementor. In the case of frameworks, there is also an additional contract between superclass and subclass regarding default behavior, extending, and overriding.
*   Which role or roles will each party play?  
    For many Component API methods, the Component plays the role of exclusive implementor and the Client plays the role of caller. In the case of Component callbacks, the Component plays the caller role and the Client plays the implementor role. In some cases, the Client might play more than one role.
*   Is a role played exclusively by the Component?  
    Component API changes coincide with Component releases, making it feasible to change Component code to accommodate the changed APIs.
*   For roles played by Clients, would the contemplated API change render invalid a hypothetical Client making legal usage of the existing API?

The following examples illustrate how this analysis is done.

### Example 1 - Changing a method postcondition

Standard method contracts have two roles: caller and implementor. Method postconditions are those things that an implementor must arrange to be true before returning from the method, and that a caller may presume to be true after the return. This first example involves a change to a postcondition.

Consider the following API method specification:

    /** Returns the list of children of this widget.
     * @return a non-empty list of widgets
     */
    Widget[] getChildren();

The contemplated API change is to allow the empty list of widgets to be returned as well, as captured by this revised specification:

    /** Returns the list of children of this widget.
     * @return a list of widgets
     */
    Widget[] getChildren();

Would this change break compatibility with existing Clients? It depends on the role played by the Client.

Looking at the caller role, this change would break a hypothetical pre-existing caller that legitimately counts on the result being non-empty. The relevant snippet from this hypothetical caller might read:

    Widget[] children = widget.getChildren();
    Widget firstChild = children[0];

Under the revised contract, this code would be seen to be in error because it assumes that the result of invoking `getChildren` is non-empty; under the previous contract, this assumption was just fine. This API change weakens a postcondition for the caller, and is not contract compatible for the caller role. The contemplated change would break Clients playing the caller role.

Looking at the implementor role, this change would not break a hypothetical pre-existing implementor which never return empty results anyway. Weakening a method postcondition is contract compatible for the implementor role. The contemplated change would not break Clients playing the implementor role.

So the answer as to whether this change breaks compatibility with existing Clients hinges on which role(s) the Client plays.

Another form of postcondition change is changing the set of checked exceptions that a method throws.

### Example 2 - Changing a method precondition

Method preconditions are those things that a caller must arrange to be true before calling the method, and that an implementor may presume to be true on entry. This second example involves a change to a precondition.

Consider the following API method specification:

    /** Removes the given widgets from this widget's list of children.
     * @param widgets a non-empty list of widgets
     */
    void remove(Widget[] widgets);
    
The contemplated API change is to allow empty lists of widgets to be passed in as well:

    /** Removes the given widgets from this widget's list of children.
     * @param widgets a list of widgets
     */
    void remove(Widget[] widgets);

Would this change break compatibility with existing Clients? Again, it hinges on the role played by the Client.

Looking at the caller role, this change would not break hypothetical pre-existing callers since they pass in non-empty lists. However, this change would break a hypothetical pre-existing implementations that legitimately assumed that the argument is not empty.

The relevant snippet from this hypothetical implementor might read:

    Widget firstChild = widgets[0];

Under the revised contract, this code would be seen to be in error because it assumes that the argument is non-empty; under the previous contract, this assumption was just fine. This API change weakens a method precondition, and is not contract compatible for the implementor role. The contemplated change would break Clients that implement this method.

### Example 3 - Changing a field invariant

Fields can be analyzed as having two roles: a getter and a setter. The Java language does not separate these roles particularly, but it does have the notion of final fields which eliminates setters from the equation. (Perhaps a better way to divvy this up is to say that there is a getter role and a getter/setter role.)  The API specification for a field is usually in the form of an invariant that holds for the lifetime of the field.

Consider the following API field specification:

    /** This widget's list of children, or <code>null</code>.
     */
    Widget[] children;
    
The contemplated API change is to get rid of the possibility of the `null` value:

    /** This widget's list of children.
     */
    Widget[] children;

Would this change break compatibility with existing Clients?

This change would break a hypothetical pre-existing setter that legitimately sets the field to `null`. On the other hand, it would not break a hypothetical pre-existing getter that legitimately had to assume that the field could be `null`. This API change weakens a field invariant, and is not contract compatible for the setter role.

### Example 4 - Adding an API method

Can adding an API method to a class or interface break compatibility with existing Clients?

If the method is added to an interface which Clients may implement, then it is definitely a breaking change.

If the method is added to a class (interface) which Clients are not allowed to subclass (to implement), then it is not a breaking change.

However, if the method is added to a class which Clients may subclass, then the change should ordinarily be viewed as a breaking change. The reason for this harsh conclusion is because of the possibility that a Client's subclass already has its own implementation of a method by that name. Adding the API method to the superclass undercuts the Client's code since it would be sheer coincidence if the Client's existing method met the API contract of the newly added method. In practice, if the likelihood of this kind of name coincidence is sufficiently low, this kind of change is often treated as if it were non-breaking.

### General Rules for Contract Compatibility

Whether a particular Component API change breaks or maintains contract compatibility with hypothetical pre-existing Clients hinges on which role, or roles, the Client plays in the API contract(s) being changed. The following table summarizes the pattern seen in the above examples:  
 
|   |   |   |   |
| --- | --- | --- | --- |
| Method preconditions | Strengthen <br> <br> Weaken | **Breaks compatibility for callers** <br> <br>Contract compatible for callers | Contract compatible for implementors <br> <br>**Breaks compatibility for implementors** |
| Method postconditions | Strengthen <br> <br> Weaken | Contract compatible for callers <br><br> **Breaks compatibility for callers**| **Breaks compatibility for implementors** <br><br> Contract compatible for implementors|
| Field invariants | Strengthen <br><br> Weaken| Contract compatible for getters <br><br> **Breaks compatibility for getters**| **Breaks compatibility for setters** <br><br> Contract compatible for setters |

Achieving API Binary Compatibility
----------------------------------

See [Part 2](/Evolving_Java-based_APIs_2 "Evolving Java-based APIs 2").

Other Notes
-----------

See [Part 3](/Evolving_Java-based_APIs_3 "Evolving Java-based APIs 3").

Bundle Versioning
-----------------

See [Version Numbering](/Version_Numbering "Version Numbering")

Copyright © 2000, 2009 IBM Corporation