Evolving Java-based APIs 2
==========================

Part 2 of [Evolving\_Java-based\_APIs](https://github.com/eclipse-platform/eclipse.platform/blob/master/docs/Evolving-Java-based-APIs.md).

Contents
--------

*   [1 Achieving API Binary Compatibility](#Achieving-API-Binary-Compatibility)
    *   [1.1 Evolving API packages](#Evolving-API-packages)
    *   [1.2 Evolving API Interfaces](#Evolving-API-Interfaces)
        *   [1.2.1 Evolving API interfaces - API methods](#Evolving-API-interfaces---API-methods)
        *   [1.2.2 Evolving API interfaces - API fields](#Evolving-API-interfaces---API-fields)
        *   [1.2.3 Evolving API interfaces - API type members](#Evolving-API-interfaces---API-type-members)
    *   [1.3 Evolving API Classes](#Evolving-API-Classes)
        *   [1.3.1 Evolving API classes - API methods and constructors](#Evolving-API-classes---API-methods-and-constructors)
        *   [1.3.2 Evolving API classes - API fields](#Evolving-API-classes---API-fields)
        *   [1.3.3 Evolving API classes - API type members](#Evolving-API-classes---API-type-members)
    *   [1.4 Evolving non-API packages](#Evolving-non-API-packages)
    *   [1.5 Turning non-generic types and methods into generic ones](#Turning-non-generic-types-and-methods-into-generic-ones)
    *   [1.6 Evolving annotations on API elements](#Evolving-annotations-on-API-elements)
*   [2 Other Notes](#Other-Notes)

Achieving API Binary Compatibility
----------------------------------

> _"\[A\]n object-oriented model must be carefully designed so_
> 
> _that class-library transformations that should not break already compiled applications, indeed, do not break such applications."  
> _---Ira Forman, Michael Conner, Scott Danforth, and Larry Raper, "Release-to-Release Binary Compatibility in SOM", in _Proceedings of OOPSLA '95._

Achieving API binary compatibility depends in part on the Java language's notion of binary compatibility:

> "A change to a type is _binary compatible with_ (equivalently, does not _break binary compatibility_ with) preexisting binaries if preexisting binaries that previously linked without error will continue to link without error." ([JLS8, 13.2](http://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.2))

Reference: [Gosling, Joy, Steele, Bracha, Buckley _The Java Language Specification_, Java SE 8 Edition, 2015-02-13; chapter 13 Binary Compatibility](http://docs.oracle.com/javase/specs/jls/se8/html/index.html).

The tables in the following sections summarize which kinds of changes break API binary compatibility.

Bear in mind that many changes will have effects in several places. For example, defining a new public interface with one public method and adding that interface as the superinterface of an existing interface has the following ramifications:

*   a new public API interface is added to an API package
*   the superinterface set of the existing API interface has expanded
*   a new public API method is added to the existing API interface

Each of these individual net effects could break binary compatibility. Use the tables to determine whether the _net effects_ preserve or break compatibility.

### Evolving API packages

It is always possible to evolve the Component API to include a new API package. However, once introduced in a release, an API package cannot easily be withdrawn from service. When an API package becomes obsolete, its API classes and API interfaces should continue to work but be marked as deprecated. After a couple of releases, it may be possible to phase out an obsolete API package.

The names of non-public (non-API) types in API packages do not appear in Client source code or binaries. Non-API types can be added or deleted without jeopardizing binary compatibility. However, once made public in a release, these types are part of the API and cannot easily be withdrawn from service without breaking existing Clients. When an API type becomes obsolete, it should continue to work but be marked as deprecated.  
 

|   |   |   |
| --- | --- | --- |
| Add API package | - | Binary compatible |
| Delete API package | - | **Breaks compatibility** |
| Add API type to API package | - | Binary compatible |
| Delete API type from API package | - | **Breaks compatibility** |
| Add non-`public` (non-API) type to API package | - | Binary compatible |
| Delete non-`public` (non-API) type from API package | - | Binary compatible |
| Change non-`public` (non-API) type in API package to make public (API) | - | Binary compatible |
| Change `public` type in API package to make non-`public` | - | **Breaks compatibility** |
| Change kind of API type (class, interface, enum, or annotation type) | - | **Breaks compatibility** (1) |

(1) API class-interface gender changes break binary compatibility, even in cases where the class/interface is used by, but not implemented by, Clients. This is because the Java VM bytecodes for invoking a method declared in an interface are different from the ones used for invoking a method declared in a class. More generally, all gender changes involving classes, enums, interfaces, and annotation types break binary compatibility for one reason or another.

### Evolving API Interfaces

Evolving API interfaces is somewhat more straightforward than API classes since all methods are `public`, all fields are `public` `static` and `final`, all type members are `public` and `static`, and there are no constructors. Annotation types (`@interface`) , which are a form of interface, are also covered.

|   |   |   |
| --- | --- | --- |
| Add abstract method | If method need not be implemented by Client <br><br> If method must be implemented by Client | Binary compatible (0) <br><br> **Breaks compatibility** (1)|
| Add default method | If interface not implementable by Clients <br><br> If interface implementable by Clients| Binary compatible <br><br> **Breaks compatibility** (8)|
| Add static method | - | Binary compatible |
| Delete API method  | - | **Breaks compatibility** |
| Move API method up type hierarchy | If method in supertype need not be implemented by Client <br> <br> If method in supertype must be implemented by Client | Binary compatible <br> <br> **Breaks compatibility** (7)|
| Move API method down type hierarchy | - | **Breaks compatibility (7)** |
| Add API field | If interface not implementable by Clients <br> <br> If interface implementable by Clients| Binary compatible <br> <br> **May break compatibility** (2)|
| Delete API field | - | **Breaks compatibility** |
| Expand superinterface set (direct or inherited) | - | Binary compatible |
| Contract superinterface set (direct or inherited) | - | **Breaks compatibility** (3) |
| Add, delete, or change static initializers | - | Binary compatible |
| Add API type member | - | Binary compatible |
| Delete API type member | - | **Breaks compatibility** |
| Re-order field, method, and type member declarations | - | Binary compatible |
| Add type parameter | If interface has no type parameters <br> <br> If interface has type parameters| Binary compatible (4) <br> <br> **Breaks compatibility**|
| Delete type parameter | - | **Breaks compatibility** |
| Re-order type parameters | - | **Breaks compatibility** |
| Rename type parameter | - | Binary compatible |
| Add, delete, or change type bounds of type parameter | - | **Breaks compatibility** |
| Add element to annotation type | If element has a default value <br> <br> If element has no default value| Binary compatible <br> <br> **Breaks compatibility** (5)|
| Delete element from annotation type | - | **Breaks compatibility** (6) |

(0) Although adding a new method to an API interface which need not be reimplemented by Clients does not break binary compatibility, a pre-existing Client subclass of an existing implementation might still provide a pre-existing implementation of a method by this name. 
See [Evolving Java-based APIs#Example 4 - Adding an API method](https://github.com/eclipse-platform/eclipse.platform/blob/master/docs/Evolving-Java-based-APIs.md#Achieving-API-Binary-Compatibility) in the preceding section for why this breaks API contract compatibility.

(1) Adding a new method to an API interface that is implemented by Clients (e.g., a callback, listener, or visitor interface) breaks compatibility because hypothetical pre-existing implementations do not implement the new method.

(2) Adding an API field to an API interface that is implemented by Clients is dangerous in two respects:

*   It may break API contract compatibility similar to [Evolving\_Java-based\_APIs#Example\_4\_-\_Adding\_an\_API\_method](https://github.com/eclipse-platform/eclipse.platform/blob/master/docs/Evolving-Java-based-APIs.md#Achieving-API-Binary-Compatibility) in case of name clashes.
*   It may cause linkage errors in case an instance (respectively static) field hides a static (respectively instance) field, see [JLS8 13.4.8](https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.4.8). This can not happen if all field declarations follow the usual naming conventions from [JLS8 6.1](https://docs.oracle.com/javase/specs/jls/se8/html/jls-6.html#jls-6.1) and classes only have API fields that are either
    *   "static final" (and hence have a name that doesn't contain any lowercase letters), or
    *   non-static and non-final (and hence have a name that contains a least one lowercase letter)

Apart from the binary compatibility issues, it is generally good software engineering practice that API classes should not expose any non-constant fields.

(3) Shrinking the set of API interfaces that a given API interfaces extends (either directly or inherited) breaks compatibility because some casts between API interfaces in hypothetical pre-existing Client code between will no longer work. However, non-API superinterfaces can be removed without breaking binary compatibility.

(4) Altering the type parameters of a parameterized type breaks compatibility. However, adding type parameters to a previously unparameterized type retains compatibility because of Java's special treatment of legacy references (raw types).

(5) Existing annotations would not have a value for the new element, causing an exception (IncompleteAnnotationException) to be thrown when the annotation is read.

(6) Existing annotations that mention the deleted element will cause an exception (AnnotationTypeMismatchException) to be thrown when the annotation is read.

(7) Moving methods (and other members) up and down the type hierarchy are composite changes that must always be analyzed in terms of the net effect on the types involved plus any types laying along the path. For instance, the net effect of moving an API method to a supertype is an API method being added to the supertype; the subtype is not normally affected because the API method would still be inherited.

(8) Adding a default method will break an existing client type if it already implements another interface that declares a default method with a matching signature, and the client type already refers to the default method from the other interface (except when using the Interface.super.method() notation). The added default method will cause an IncompatibleClassChangeError at run time, see [JLS8 13.5.6](http://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.5.6). Furthermore, re-compiling the type will result in a compile error.

In cases where the risk of multiple inheritance of the same method from multiple interfaces is deemed low and the added value is deemed sufficiently high, selected default methods can be added to existing interfaces. However, such a change should only be applied in an early milestone, and it should be reverted if clients report any difficulties with the change.

#### Evolving API interfaces - API methods

All methods in an API interface are implicitly `public`, and are therefore all considered API methods. The same is true for method declarations defining the elements of an API annotation type.

|   |   |   |
| --- | --- | --- |
| Change formal parameter name | - | Binary compatible |
| Change method name | - | **Breaks compatibility** |
| Add or delete formal parameter | - | **Breaks compatibility** |
| Change type of a formal parameter | - | **Breaks compatibility** |
| Change result type (including `void`) | - | **Breaks compatibility** |
| Add checked exceptions thrown | - | **Breaks compatibility** (1) |
| Add unchecked exceptions thrown | - | Binary compatible |
| Delete checked exceptions thrown | - | **Breaks compatibility** (1) |
| Delete unchecked exceptions thrown | - | Binary compatible |
| Re-order list of exceptions thrown | - | Binary compatible |
| Change `static` to non-`static` | - | **Breaks compatibility** |
| Change non-`static` to `static` | - | **Breaks compatibility** |
| Change `default` to `abstract` | - | **Breaks compatibility** |
| Change `abstract` to `default` | - | Binary compatible |
| Add type parameter | If method has no type parameters <br> <br> If method has type parameters| Binary compatible (2) <br> <br> **Breaks compatibility**|
| Delete type parameter | - | **Breaks compatibility** |
| Re-order type parameters | - | **Breaks compatibility** |
| Rename type parameter | - | Binary compatible |
| Add, delete, or change type bounds of type parameter | - | **Breaks compatibility** |
| Change last parameter from array type `T[]` to variable arity `T...` | - | Binary compatible (3) |
| Change last parameter from variable arity `T...` to array type `T[]` | - | **Breaks compatibility** (4) |
| Add default clause to annotation type element | - | Binary compatible |
| Change default clause on annotation type element | - | Binary compatible (5) |
| Delete default clause from annotation type element | - | **Breaks compatibility** |

(1) Adding and deleting checked exceptions declared as thrown by an API method does not break binary compatibility; however, it breaks contract compatibility (and source code compatibility).

(2) Adding type parameters to an unparameterized method is a compatible change owing to Java's story for interfacing with non-generic legacy code.

(3) A variable arity method declaration such as "void foo(int x, String... y)" is compiled as if it had been written "void foo(int x, String\[\] y)".  
Warning: If the vararg type is `Object` (or `Cloneable` or `Serializable`), compilers will typically emit a warning for each method invocation that passes an array whose component type is not exactly the same as the vararg type (e.g. passing a `String[]` to a method that takes an `Object...`).  
Note that passing `null` as sole argument for a variable arity parameter always yields a compile warning at the call site.  
Summary: Think twice before you convert an `Object[]` to an `Object...`, because the latter can cause many warnings in existing client code.

(4) Although existing binaries will continue to work, existing invocations in source code may not compile because the compiler no longer automatically bundles up the extra arguments into an array.

(5) Defaults are applied dynamically at the time annotations are read. Changing the default value may affect annotations in all classes, including ones compiled before the change was made.

#### Evolving API interfaces - API fields

All fields in an API interface are implicitly `public`, `static`, and `final`; they are therefore all considered API fields.

Because of binary compatibility problems with fields, the Java Language Specification recommends against using API fields. However, this is not always possible; in particular, enumeration constants to be used in `switch` statements must be defined as API fields.

|   |   |   |
| --- | --- | --- |
| Change type of API field | - | **Breaks compatibility** (1) |
| Change value of API field | If field is compile-time constant value <br> <br> If field is not compile-time constant value| **Breaks compatibility** (2) <br> <br> Binary compatible|

(1) All field type changes break binary compatibility, even seemingly innocuous primitive type widenings like turning a `short` into an `int`.

(2) Java compilers always inline the value of constant fields (ones with compile-time computable values, whether primitive or `String` type). As a consequence, changing the value of an API constant field does not affect pre-existing Clients. Invariably, this fails to meet the objective for changing the API field's value in the first place.

#### Evolving API interfaces - API type members

All type members in an API interface are implicitly `public` and `static`; they are therefore considered API type members. The rules for evolving an API type member are basically the same as for API classes and interfaces declared at the package level.

### Evolving API Classes

Evolving API classes is somewhat more complex than API interfaces due to the wider variety of modifiers, including `protected` API members. Enums, which are a form of class, are also covered.

|   |   |   |
| --- | --- | --- |
| Add API method | If method need not be reimplemented by Client <br> <br> If method must be reimplemented by Client | Binary compatible (0) <br> <br> **Breaks compatibility** (1)|
| Delete API method  | - | **Breaks compatibility** |
| Move API method up type hierarchy | If method in supertype need not be reimplemented by Client <br <br> If method in supertype must be reimplemented by Client | Binary compatible <br> <br> **Breaks compatibility** (9)|
| Move API method down type hierarchy | - | **Breaks compatibility** (9) |
| Add API constructor | If there are other constructors <br> <br> If this is only constructor| Binary compatible <br> <br> **Breaks compatibility** (2)|
| Delete API constructor | - | **Breaks compatibility** |
| Add API field | If class is not subclassable by Client (this includes enums) <br> <br> If class is subclassable by Client| Binary compatible <br><br> **May break compatibility** (3)|
| Delete API field | - | **Breaks compatibility** |
| Expand superinterface set (direct or inherited) | - | Binary compatible |
| Contract superinterface set (direct or inherited) | - | **Breaks compatibility** (4) |
| Expand superclass set (direct or inherited) | - | Binary compatible |
| Contract superclass set (direct or inherited) | - | **Breaks compatibility** (4) |
| Add, delete, or change static or instance initializers | - | Binary compatible |
| Add API type member | - | Binary compatible |
| Delete API type member | - | **Breaks compatibility** |
| Re-order field, method, constructor, and type member declarations | - | Binary compatible |
| Add or delete non-API members; that is, `private` or default access fields, methods, constructors, and type members | - | Binary compatible |
| Change `abstract` to non-`abstract` | - | Binary compatible |
| Change non-`abstract` to `abstract` | - | **Breaks compatibility** (5) |
| Change `final` to non-`final` | - | Binary compatible |
| Change non-`final` to `final` | - | **Breaks compatibility** (6) |
| Add type parameter | If class has no type parameters <br> <br> If class has type parameters| Binary compatible (7) <br> <br> **Breaks compatibility**|
| Delete type parameter | - | **Breaks compatibility** |
| Re-order type parameters | - | **Breaks compatibility** |
| Rename type parameter | - | Binary compatible |
| Add, delete, or change type bounds of type parameter | - | **Breaks compatibility** |
| Rename enum constant | - | **Breaks compatibility** |
| Add, change, or delete enum constant arguments | - | Binary compatible |
| Add, change, or delete enum constant class body | - | Binary compatible |
| Add enum constant | - | Binary compatible (8) |
| Delete enum constant | - | **Breaks compatibility** |
| Re-order enum constants | - | Binary compatible (8) |

(0) Although adding a new method to an API class which need not be reimplemented by Clients does not break binary compatibility, a pre-existing subclass might still provide a pre-existing implementation of a method by this name. 
See [Evolving Java-based APIs#Example 4 - Adding an API method](https://github.com/eclipse-platform/eclipse.platform/blob/master/docs/Evolving-Java-based-APIs.md#Achieving-API-Binary-Compatibility) in the preceding section for why this breaks API contract compatibility.

(1) Adding a new method to an API class that must be reimplemented by Clients breaks compatibility because pre-existing subclasses would not provide any such implementation.

(2) Adding the first constructor to an API class causes the compiler to no longer generate a default (public, 0 argument) constructor, thereby breaking compatibility with pre-existing code that invoked this API constructor. To avoid this pitfall, API classes should always explicitly declare at least one constructor.

(3) Adding an API field to an API class that is extended by Clients is dangerous in two respects:

*   It may break API contract compatibility similar to [Evolving\_Java-based\_APIs#Example\_4\_-\_Adding\_an\_API\_method](https://github.com/eclipse-platform/eclipse.platform/blob/master/docs/Evolving-Java-based-APIs.md#Achieving-API-Binary-Compatibility) in case of name clashes.
*   It may cause linkage errors in case an instance (respectively static) field hides a static (respectively instance) field, see [JLS8 13.4.8](https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.4.8). This can not happen if all field declarations follow the usual naming conventions from [JLS8 6.1](https://docs.oracle.com/javase/specs/jls/se8/html/jls-6.html#jls-6.1) and classes only have API fields that are either
    *   "static final" (and hence have a name that doesn't contain any lowercase letters), or
    *   non-static and non-final (and hence have a name that contains a least one lowercase letter)

Apart from the binary compatibility issues, it is generally good software engineering practice that API classes should not expose any non-constant fields.

(4) Shrinking an API class's set of API superclasses and superinterfaces (either directly or inherited) breaks compatibility because some casts in pre-existing Client code will now longer work. However, non-API superclasses and superinterfaces can be removed without breaking binary compatibility.

(5) Pre-existing binaries that attempt to create new instances of the API class will fail with a link-time or runtime error.

(6) Pre-existing binaries that subclass the API class will fail with a link-time error.

(7) Adding type parameters to an unparameterized type is a compatible change owing to Java's story for interfacing with non-generic legacy code.

(8) Client code can use the values() method to determine the ordinal positions of the enum constants. So although this is a binary compatible change, it may break contractual compatibility.

(9) Moving methods (and other members) up and down the type hierarchy are composite changes that must always be analyzed in terms of the net effect on the types involved plus any types laying along the path. For instance, the net effect of moving an API method to a supertype is an API method being added to the supertype; the subtype is not normally affected because the API method would still be inherited.

#### Evolving API classes - API methods and constructors

|   |   |   |
| --- | --- | --- |
| Change body of method or constructor | - | Binary compatible |
| Change formal parameter name | - | Binary compatible |
| Change method name | - | **Breaks compatibility** |
| Add or delete formal parameter | - | **Breaks compatibility** |
| Change type of a formal parameter | - | **Breaks compatibility** |
| Change result type (including `void`) | - | **Breaks compatibility** |
| Add checked exceptions thrown | - | **Breaks compatibility** (1) |
| Add unchecked exceptions thrown | - | Binary compatible |
| Delete checked exceptions thrown | - | **Breaks compatibility** (1) |
| Delete unchecked exceptions thrown | - | Binary compatible |
| Re-order list of exceptions thrown | - | Binary compatible |
| Decrease access; that is, from `protected` access to default or `private` access; or from `public` access to `protected`, default, or `private` access | - | **Breaks compatibility** |
| Increase access; that is, from `protected` access to `public` access | - | Binary compatible (2) |
| Change `abstract` to non-`abstract` | - | Binary compatible |
| Change non-`abstract` to `abstract` | - | **Breaks compatibility** (3) |
| Change `final` to non-`final` | - | Binary compatible |
| Change non-`final` to `final` | If method not reimplementable by Clients <br><br> If method reimplementable by Clients| Binary compatible <br><br> **Breaks compatibility** (4)|
| Change `static` to non-`static` | - | **Breaks compatibility** |
| Change non-`static` to `static` | - | **Breaks compatibility** |
| Change `native` to non-`native` | - | Binary compatible |
| Change non-`native` to `native` | - | Binary compatible |
| Change `synchronized` to non-`synchronized` | - | Binary compatible (5) |
| Change non-`synchronized` to `synchronized` | - | Binary compatible (5) |
| Add type parameter | If method has no type parameters <br><br> If method has type parameters| Binary compatible (6) <br><br> **Breaks compatibility**|
| Delete type parameter | - | **Breaks compatibility** |
| Re-order type parameters | - | **Breaks compatibility** |
| Rename type parameter | - | Binary compatible |
| Add, delete, or change type bounds of type parameter | - | **Breaks compatibility** |
| Change last parameter from array type `T[]` to variable arity `T...` | - | Binary compatible (7) |
| Change last parameter from variable arity `T...` to array type `T[]` | - | **Breaks compatibility** (8) |

(1) Adding and deleting checked exceptions declared as thrown by an API method does not break binary compatibility; however, it breaks contract compatibility (and source code compatibility).

(2) Perhaps surprisingly, the binary format is defined so that changing a member or constructor to be more accessible does not cause a linkage error when a subclass (already) defines a method to have less access.

(3) Pre-existing binaries that invoke the method will fail with a runtime error.

(4) Pre-existing binaries that reimplement the method will fail with a link-time error.

(5) Adding or removing the `synchronized` modifier also has a bearing on the method's behavior in a multi-threaded world, and may therefore raise a question of contract compatibility.

(6) Adding type parameters to an unparameterized type is a compatible change owing to Java's story for interfacing with non-generic legacy code.

(7) A variable arity method declaration such as "void foo(int x, String... y)" is compiled as if it had been written "void foo(int x, String\[\] y)".  
Warning: If the vararg type is `Object` (or `Cloneable` or `Serializable`), compilers will typically emit a warning for each method invocation that passes an array whose component type is not exactly the same as the vararg type (e.g. passing a `String[]` to a method that takes an `Object...`).  
Note that passing `null` as sole argument for a variable arity parameter always yields a compile warning at the call site.  
Summary: Think twice before you convert an `Object[]` to an `Object...`, because the latter can cause many warnings in existing client code.

(8) Although existing binaries will continue to work, existing invocations in source code may not compile because the compiler no longer automatically bundles up the extra arguments into an array.

#### Evolving API classes - API fields

Because of binary compatibility problems with fields, the Java Language Specification recommends against using API fields. However, this is not always possible; in particular, enumeration constants to be used in `switch` statements must be defined as API constant fields.  
 

|   |   |   |
| --- | --- | --- |
| Change type of API field | - | **Breaks compatibility** (1) |
| Change value of API field | If field is compile-time constant <br><br> If field is not compile-time constant| **Breaks compatibility** (2) <br><br> Binary compatible|
| Decrease access; that is, from `protected` access to default or `private` access; or from `public` access to `protected`, default, or `private` access | - | **Breaks compatibility** |
| Increase access; that is, from `protected` access to `public` access | - | Binary compatible |
| Change `final` to non-`final` | If field is non-static <br><br> If field is static with compile-time constant value <br><br> If field is static with non-compile-time constant value| Binary compatible <br><br> **Breaks compatibility** (3) <br><br> Binary compatible|
| Change non-`final` to `final` | - | **Breaks compatibility** (4) |
| Change `static` to non-`static` | - | **Breaks compatibility** (5) |
| Change non-`static` to `static` | - | **Breaks compatibility** (5) |
| Change `transient` to non-`transient` | - | Binary compatible |
| Change non-`transient` to `transient` | - | Binary compatible |

(1) All field type changes break binary compatibility, even seemingly innocuous primitive type widenings link turning a `short` into an `int`.

(2) Java compilers always inline the value of constant fields (ones with compile-time computable values, whether primitive or `String` type). As a consequence, changing the value of an API constant field does not affect pre-existing Clients. Invariably, this does not meet the objective for changing the API field's value.

(3) Java compilers always inline the value of constant fields (ones with compile-time computable values, whether primitive or `String` type). As a consequence, changing an API constant field into a non-`final` one does not propagate to pre-existing Clients. Invariably, this does not meet the objective for making the API field non-`final`.

(4) Making an API field final breaks compatibility with pre-existing binaries that attempt to assign new values to the field.

(5) Changing whether an API field is declared static or not results in link-time errors where the field is used by a pre-existing binary which expected a field of the other kind.

#### Evolving API classes - API type members

The rules for evolving an API type member are basically the same as for API classes and interfaces declared at the package level, with these additional rules for changing access modifiers:  
 
|   |   |   |
| --- | --- | --- |
| Decrease access; that is, from `protected` access to default or `private` access; or from `public` access to `protected`, default, or `private` access | - | **Breaks compatibility** |
| Increase access; that is, from `protected` access to `public` access | - | Binary compatible |

### Evolving non-API packages

The names of non-API packages, classes, and interfaces do not appear in Client source code or binaries. Consequently, non-API packages, classes, and interfaces can be added or deleted without jeopardizing binary compatibility. However, when non-API classes and interfaces containing `public` or `protected` members are among the superclass or superinterface sets of API classes and interfaces, non-API changes may have ramifications to API methods, fields, and constructors.

|   |   |   |
| --- | --- | --- |
| Add non-API package | - | Binary compatible |
| Delete non-API package | - | Binary compatible |
| Add class or interface to non-API package | - | Binary compatible |
| Delete class or interface in a non-API package | - | Binary compatible |
| Change existing class or interface in non-API package | - | Binary compatible |

### Turning non-generic types and methods into generic ones

Generic types and methods were added to the Java language in Java SE 5 (aka JDK 1.5) along with a special story for how legacy non-generic code can continue to use types and methods that have been "generified". The prime example of this is the java.util Collections Framework, which was upgraded to make use of generics while remaining compatible with code that uses collections in the old way.

The key concepts behind Java's special compatibility mechanism are raw types and erasures.

A _raw type_ is a use of a generic type without the normal type arguments. For example, "`List`" in the declaration statement "`List x = null;`" is a raw type since List is a generic type declared "`public interface List<E>` ..." in JDK 1.5. Contrast this to a normal use of List which looks like "`List<String> x = null;`" or "`List<?> x = null;`" where a type augument ("`String`") or wildcard is specified.

The term _erasure_ is suggestive. Imagine going through your code and literally erasing the type parameters from the generic type declaration (e.g., erasing the "`<E>`" in "`public interface List<E>` ...") to get a non-generic type declaration, and replacing all occurrence of the deleted type variable with `Object`. For type parameters with type bounds (e.g., "`<E extends T1 & T2 & T3 & ...>`"), the leftmost type bound (`"T1`"), rather than `Object`, is substituted for the type variable. The resulting declaration is known as the erasure.

According to the special compatibility story, the Java compiler treats a raw type as a reference to the type's erasure. An existing type can be evolved into a generic type by adding type parameters to the type declaration and judiciously introducing uses of the type variables into the signatures of its existing methods and fields. As long as the erasure looks like the corresponding declaration prior to generification, the change is binary compatible with existing code.

As a case study of how to generify an existing API, carefully compare the Java 1.4 Collections Framework[\[1\]](http://java.sun.com/j2se/1.4.2/docs/api/java/util/Collection.html) classes with their counterparts in the Java 1.5 Collections Framework[\[2\]](http://java.sun.com/j2se/1.5.0/docs/api/java/util/Collection.html). You will see that the erasures of the 1.5 versions looks just like the 1.4 versions.

Variable arity methods were also introduced in 1.5, also with a special story for how legacy code can continue to invoke or override a method that has been upgraded. A variable arity method declaration such as "`void foo(int x, String... y)`" is compiled as if it had been written "`void foo(int x, String[] y)`". This provides a consistent interpretation for old-style invocations of the form "`foo(int 5, new String[]{"a","b"})`". The class Arrays.asList[\[3\]](http://java.sun.com/j2se/1.5.0/docs/api/java/util/Arrays.html#asList(T...)) is an example of a method that became both generic and variable arity in 1.5.

Two final notes. Regarding whether existing APIs ought to embrace generics, the Java Language Specification says only this:

> "The use of raw types is allowed only as a concession to compatibility of legacy code. The use of raw types in code written after the introduction of genericity into the Java programming language is strongly discouraged. _**It is possible that future versions of the Java programming language will disallow the use of raw types.**_" (JLS3, 4.8[\[4\]](http://java.sun.com/docs/books/jls/third_edition/html/typesValues.html#4.8))

The implication is that code that uses the Collections Framework should use types like "`List<?>`" instead of the raw type "`List`".

But, also bear in mind that there are severe constraints on how a type or method that already is generic can be compatibly evolved with respect to its type parameters (see the tables above). So if you plan to generify an API, remember that you only get one chance (release), to get it right. In particular, if you change a type in an API signature from the raw type "`List`" to "`List<?>`" or "`List<Object>`", you will be locked into that decision. The moral is that generifying an existing API is something that should be considered from the perspective of the API as a whole rather than piecemeal on a method-by-method or class-by-class basis.

### Evolving annotations on API elements

Annotations were added to the Java language in Java SE 5 (aka JDK 1.5). Annotation can be used on most any named Java element, including packages, types, methods, and fields. It's a natural question to ask whether adding, deleting, or changing an annotation on a API element is a compatible or a breaking change.

On one hand, adding or removing annotations has no effect on the correct linkage of class files by the Java virtual machine. On the other hand, annotations exist to be read via reflective APIs for manipulating annotations. So there is no uniform answer as to what will happen if a given annotation is or is not present on an API element (or non-API element, for that matter). It depends entirely on the specifics of the annotation and the mechanisms that are processing those annotations.

Parties that declare annotation types should try to provide helpful guidance for their customers.

Other Notes
-----------

See [Part 3](https://github.com/eclipse-platform/eclipse.platform/blob/master/docs/Evolving-Java-based-APIs-3.md).

