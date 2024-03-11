

FAQ Why does the Eclipse compiler create a different serialVersionUID from javac?
=================================================================================

You may discover that serializable classes compiled with Eclipse are not compatible with the same classes compiled using javac. When classes compiled in Eclipse are written to an object stream, you may not be able to read them back in a program that was compiled elsewhere. Many people blame this on the Eclipse compiler, assuming that it is somehow not conforming properly to spec. In fact, this can be a problem between any two compilers or even two versions of a compiler provided by the same vendor.

If you need object serialization, the _only_ way to be safe is to explicitly define the serialVersionUID in your code:

        class MyClass implements Serializable {
            public static final long serialVersionUID = 1;
        }

  

Then, whenever your class changes shape in a way that will be incompatible with previously serialized versions, simply increment this number.

  

  
If you don't explicitly define a serialVersionUID, the language requires that the VM generate one, using some function of all field and method names in the class. The problem is, the compiler generates some _synthetic_ methods that you never see in your source file, and there is no clear specification for how these synthetic method names are generated. Any two compilers are likely to generate different method names, and so the serialVersionUID will be different. Bottom line: Always define the serialVersionUID explicitly in your source files.

