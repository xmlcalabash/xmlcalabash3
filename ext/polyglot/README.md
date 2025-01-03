#  Polyglot extension step

This is an extension step for [XML Calabash 3.x](https://github.com/xmlcalabash/xmlcalabash3).

To use this step, you must add it to your XML Calabash configuration. It doesn’t
have any standalone functionality.

## Install with Maven

Alpha versions of XML Calabash and this step are published to the Maven
[SNAPSHOT](https://help.sonatype.com/en/maven-repositories.html) repository,
https://oss.sonatype.org/content/repositories/snapshots/

Add `com.xmlcalabash:xmlcalabash:<version>` and
`com.xmlcalabash:polyglot:<version>` to your project.

# Install “by hand”

To install this package by hand, download the distribution
[from GitHub](https://github.com/xmlcalabash/xmlcalabash3/releases) and
unzip it somewhere. When you run XML Calabash, make sure that
all of the jar files in the `extra` directory are included on your classpath.

One way to do this is to copy all of them into the `extra` directory
in the XML Calabash release. The scripts included in the release will
then load them automatically.
