# SchXslt2 Schematron to XSLT 3.0 transpiler

SchXslt2 is the second iteration of SchXslt, a modern XSLT-based implementation of the ISO Schematron validation
language (ISO/IEC-19757:3).

SchXslt2 Schematron to XSLT 3.0 transpiler is copyright by David Maus &lt;dmaus@dmaus.name&gt; and released under the
terms of the MIT license.

Feedback to SchXslt2 is welcome via [email](mailto:dmaus@dmaus.name) or SourceHut's email-based [issue
tracker](https://todo.sr.ht/~dmaus/schxslt2).

## Installing

Every release provides a ZIP file with just the XSLT transpiler for you to download and extract.

In addition, a Java package is published to [Maven Central](https://mvnrepository.com/artifact/name.dmaus.schxslt/schxslt2) 
for use with Maven or the Java dependency management tool of your choice.

## Usage

Use the transpiler to create an XSLT transformation that processes a document and returns a SVRL (Schematron Validation
Reporting Language) report.

## Transpiler parameters

### schxslt:debug as xs:boolean

Enable or disable debugging. When debugging is enabled, the validation stylesheet is indented. Defaults to false.

### schxslt:phase as xs:string?

Name of the validation phase. The value ```'#DEFAULT'``` selects the pattern in the ```sch:schema/@defaultPhase```
attribute or ```'#ALL'```, if this attribute is not present. The value ```'#ALL'``` selects all patterns. Defaults to
```'#DEFAULT'```.

### schxslt:streamable as xs:boolean

Set to boolean ```true``` to create a streamable validation stylesheet. This *does not* check the streamability of XPath
expressions in rules, assertions, variables etc. It merely declares the modes in the validation stylesheet to be
streamable and removes the ```@location``` attribute from the SVRL output when no location function is given because the
default ```fn:path()``` is not streamable. Defaults to ```false```.

### schxslt:location-function as xs:string?

Name of a ```function f($context as node()) as xs:string``` that provides location information for the SVRL
report. Defaults to ```fn:path()``` when not set.

### schxslt:expand-text as xs:boolean

When set to boolean ```true```, the validation stylesheet globally enables text value templates, and you may use them in
assertion or diagnostic messages. Defaults to ```false```.

### schxslt:fail-early as xs:boolean

When set to boolean ```true```, the validation stylesheet stops as soon as it encounters the first failed assertion or
successful report. Defaults to ```false```.

### schxslt:terminate-validation-on-error as xs:boolean

When set to boolean ```true```, the validation stylesheet terminates the XSLT processor when it encounters a dynamic
error. Defaults to ```true```.

## Enhancements

### Typed variables

[Proposal 1](https://github.com/Schematron/schematron-enhancement-proposals/issues/1)

The Schematron specification does not allow for annotating variables with the expected type of its value. Type
annotations are helpful to make the most of XSLT 3.0. Using them is current best practice.

This proposal adds support for an ```@as``` attribute on variable declarations.

### Global abstract rules

[Proposal 3](https://github.com/Schematron/schematron-enhancement-proposals/issues/3)

The Schematron specification limits the reuse of abstract rules to the current pattern element. The ```@href
attribute``` on ```extends``` was introduced in 2016 to overcome this limitation but requires a schema author to
externalize abstract rules for them to be used.

This proposal extends Schematron with a top-level ```rules``` element to hold abstract rules that are globally
referable by the ```@rule``` attribute of ```extends```.

### Additional XSLT elements

[Proposal 4](https://github.com/Schematron/schematron-enhancement-proposals/issues/4)

The Schematron specification allows the XSLT elements ```function``` and ```key``` to be used in a Schematron
schema. This makes sense because both are required to set up the query language environment. The ```key``` element
prepares data structures for the ```key()``` function and the ```function``` element allows the use of user-defined
functions.

This proposal adds support for the following XSLT elements:

* xsl:accumulator
* xsl:import
* xsl:import-schema
* xsl:include
* xsl:use-package

### Declare abstract pattern parameters

To address the shortcomings discussed in [Proposal
8](https://github.com/Schematron/schematron-enhancement-proposals/issues/8) SchXslt2 supports the ```sch:param```
element as child of an abstract pattern to declare an abstract pattern parameter.

As of version 1.2 an abstract pattern parameter may also declare a default value in a ```@value``` attribute.

```
<sch:pattern id="a-001" abstract="true">
  <sch:param name="_placeholder" value="default"/>
  ...
</sch:pattern>
```

If at least one ```sch:param``` element is present, the transpiler terminates with an error if a declared parameter is
not provided, and if a provided parameter ist not declared.

### Introspection

Expressions in the validation stylesheet can access the effective phase it was compiled for by using the global variable
```Q{http://dmaus.name/ns/2023/schxslt}phase```.

### Logging dynamic errors

Dynamic errors during validation are logged by a svrl:error element (see [Proposal
69](https://github.com/Schematron/schematron-enhancement-proposals/issues/69)). 

Unless the static parameter ```schxslt:terminate-validation-on-error``` is set to ```false``` the validation stylesheet still
terminates the XSLT processor.

## Limitations

SchXslt2 does not implement proper scoping rules of pattern and phase variables. Schema, pattern, and phase variables
are all implemented as global XSLT variables. As a consequence, the name of a schema, pattern, or phase variable MUST be
unique in the entire schema.
