package com.xmlcalabash.test

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.util.Urify
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

class UrifyNonWindowsSpec extends AnyFlatSpec with BeforeAndAfter {
  private val OSNAME = "MacOs"
  private val FILESEP = "/"
  private val CWD = "/home/johndoe/"

  private var saveOsname = ""
  private var saveFilesep = ""
  private var saveCwd = ""

  before {
    saveOsname = Urify.osname
    saveFilesep = Urify.filesep
    saveCwd = Urify.cwd
    Urify.mockOS(OSNAME, FILESEP, Some(CWD))
  }

  after {
    Urify.mockOS(saveOsname, saveFilesep, Some(saveCwd))
  }

  "" should " parse" in {
    val path = new Urify("")
    assert(path.scheme.isEmpty)
    assert(!path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(!path.absolute)
    assert(path.relative)
    assert(path.path == "")
  }

  "/ " should " parse" in {
    val path = new Urify("/")
    assert(path.scheme.isEmpty)
    assert(!path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.absolute)
    assert(!path.relative)
    assert(path.path == "/")
  }

  "// " should " parse" in {
    val path = new Urify("/")
    assert(path.scheme.isEmpty)
    assert(!path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.absolute)
    assert(!path.relative)
    assert(path.path == "/")
  }

  // =======================================================================================================

  "///path/to/thing " should " parse" in {
    val path = new Urify("///path/to/thing")
    assert(path.scheme.isEmpty)
    assert(!path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.absolute)
    assert(!path.relative)
    assert(path.path == "/path/to/thing")
  }

  "//authority " should " parse" in {
    val path = new Urify("//authority")
    assert(path.scheme.isEmpty)
    assert(!path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isDefined)
    assert(path.authority.get == "authority")
    assert(path.driveLetter.isEmpty)
    assert(!path.absolute)
    assert(path.relative)
    assert(path.path == "")
  }

  "//authority/ " should " parse" in {
    val path = new Urify("//authority/")
    assert(path.scheme.isEmpty)
    assert(!path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isDefined)
    assert(path.authority.get == "authority")
    assert(path.driveLetter.isEmpty)
    assert(path.absolute)
    assert(!path.relative)
    assert(path.path == "/")
  }

  "//authority/path/to/thing " should " parse" in {
    val path = new Urify("//authority/path/to/thing")
    assert(path.scheme.isEmpty)
    assert(!path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isDefined)
    assert(path.authority.get == "authority")
    assert(path.driveLetter.isEmpty)
    assert(path.absolute)
    assert(!path.relative)
    assert(path.path == "/path/to/thing")
  }

  "/Documents and Files/thing " should " parse" in {
    val path = new Urify("/Documents and Files/thing")
    assert(path.scheme.isEmpty)
    assert(!path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.absolute)
    assert(!path.relative)
    assert(path.path == "/Documents and Files/thing")
  }

  "/path/to/thing " should " parse" in {
    val path = new Urify("/path/to/thing")
    assert(path.scheme.isEmpty)
    assert(!path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.absolute)
    assert(!path.relative)
    assert(path.path == "/path/to/thing")
  }

  "C:/Users/Jane/Documents and Files/Thing " should " parse" in {
    val path = new Urify("C:/Users/Jane/Documents and Files/Thing")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "C")
    assert(path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.absolute)
    assert(!path.relative)
    assert(path.path == "/Users/Jane/Documents and Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " parse" in {
    val path = new Urify("C:Users/Jane/Documents and Files/Thing")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "C")
    assert(path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(!path.absolute)
    assert(path.relative)
    assert(path.path == "Users/Jane/Documents and Files/Thing")
  }

  "Documents and Files/thing " should " parse" in {
    val path = new Urify("Documents and Files/thing")
    assert(path.scheme.isEmpty)
    assert(!path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(!path.absolute)
    assert(path.relative)
    assert(path.path == "Documents and Files/thing")
  }

  "file: " should " parse" in {
    val path = new Urify("file:")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "file")
    assert(path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(!path.absolute)
    assert(path.relative)
    assert(path.path == "")
  }

  "file:///path/to/thing " should " parse" in {
    val path = new Urify("file:///path/to/thing")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "file")
    assert(path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.absolute)
    assert(!path.relative)
    assert(path.path == "/path/to/thing")
  }

  "file://authority.com " should " parse" in {
    val path = new Urify("file://authority.com")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "file")
    assert(path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isDefined)
    assert(path.authority.get == "authority.com")
    assert(path.driveLetter.isEmpty)
    assert(!path.absolute)
    assert(path.relative)
    assert(path.path == "")
  }

  "file://authority.com/ " should " parse" in {
    val path = new Urify("file://authority.com/")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "file")
    assert(path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isDefined)
    assert(path.authority.get == "authority.com")
    assert(path.driveLetter.isEmpty)
    assert(path.absolute)
    assert(!path.relative)
    assert(path.path == "/")
  }

  "file://authority.com/path/to/thing " should " parse" in {
    val path = new Urify("file://authority.com/path/to/thing")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "file")
    assert(path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isDefined)
    assert(path.authority.get == "authority.com")
    assert(path.driveLetter.isEmpty)
    assert(path.absolute)
    assert(!path.relative)
    assert(path.path == "/path/to/thing")
  }

  "file:/path/to/thing " should " parse" in {
    val path = new Urify("file:/path/to/thing")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "file")
    assert(path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.absolute)
    assert(!path.relative)
    assert(path.path == "/path/to/thing")
  }

  "file:C:/Users/Jane/Documents and Files/Thing " should " parse" in {
    val path = new Urify("file:C:/Users/Jane/Documents and Files/Thing")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "file")
    assert(path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(!path.absolute)
    assert(path.relative)
    assert(path.path == "C:/Users/Jane/Documents and Files/Thing")
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " parse" in {
    val path = new Urify("file:C:Users/Jane/Documents and Files/Thing")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "file")
    assert(path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(!path.absolute)
    assert(path.relative)
    assert(path.path == "C:Users/Jane/Documents and Files/Thing")
  }

  "file:path/to/thing " should " parse" in {
    val path = new Urify("file:path/to/thing")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "file")
    assert(path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(!path.absolute)
    assert(path.relative)
    assert(path.path == "path/to/thing")
  }

  "https: " should " parse" in {
    val path = new Urify("https:")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "https")
    assert(path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(!path.absolute)
    assert(path.relative)
    assert(path.path == "")
  }

  "https://example.com " should " parse" in {
    val path = new Urify("https://example.com")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "https")
    assert(path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.absolute)
    assert(!path.relative)
    assert(path.path == "//example.com")
  }

  "https://example.com/ " should " parse" in {
    val path = new Urify("https://example.com/")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "https")
    assert(path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.absolute)
    assert(!path.relative)
    assert(path.path == "//example.com/")
  }

  "https://example.com/path/to/thing " should " parse" in {
    val path = new Urify("https://example.com/path/to/thing")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "https")
    assert(path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.absolute)
    assert(!path.relative)
    assert(path.path == "//example.com/path/to/thing")
  }

  "path/to/thing " should " parse" in {
    val path = new Urify("path/to/thing")
    assert(path.scheme.isEmpty)
    assert(!path.explicit)
    assert(path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(!path.absolute)
    assert(path.relative)
    assert(path.path == "path/to/thing")
  }

  "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN " should " parse" in {
    val path = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    assert(path.scheme.isDefined)
    assert(path.scheme.get == "urn")
    assert(path.explicit)
    assert(!path.hierarchical)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(!path.absolute)
    assert(path.relative)
    assert(path.path == "publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
  }

  // =======================================================================================================

  "///path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("///path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "//authority " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("//authority")
    assert(path == "file://authority")
  }

  "//authority/ " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("//authority/")
    assert(path == "file://authority/")
  }

  "//authority/path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("//authority/path/to/thing")
    assert(path == "file://authority/path/to/thing")
  }

  "/Documents and Files/thing " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("/Documents and Files/thing")
    assert(path == "file:///Documents%20and%20Files/thing")
  }

  "/path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("/path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "C:/Users/Jane/Documents and Files/Thing " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("C:/Users/Jane/Documents and Files/Thing")
    assert(path == "C:/Users/Jane/Documents and Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " throw an exception against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    try {
      basepath.resolve("C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "Documents and Files/thing " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("Documents and Files/thing")
    assert(path == "file:///home/jdoe/documents/Documents%20and%20Files/thing")
  }

  "file: " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("file:")
    assert(path == "file:///home/jdoe/documents/")
  }

  "file:///path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("file:///path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file://authority.com " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("file://authority.com")
    assert(path == "file://authority.com")
  }

  "file://authority.com/ " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("file://authority.com/")
    assert(path == "file://authority.com/")
  }

  "file://authority.com/path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("file://authority.com/path/to/thing")
    assert(path == "file://authority.com/path/to/thing")
  }

  "file:/path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("file:/path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file:C:/Users/Jane/Documents and Files/Thing " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("file:C:/Users/Jane/Documents and Files/Thing")
    assert(path == "file:///home/jdoe/documents/C:/Users/Jane/Documents and Files/Thing")
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("file:C:Users/Jane/Documents and Files/Thing")
    assert(path == "file:///home/jdoe/documents/C:Users/Jane/Documents and Files/Thing")
  }

  "file:path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("file:path/to/thing")
    assert(path == "file:///home/jdoe/documents/path/to/thing")
  }

  "https: " should " throw an exception against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    try {
      basepath.resolve("https:")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "https://example.com " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("https://example.com")
    assert(path == "https://example.com")
  }

  "https://example.com/ " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("https://example.com/")
    assert(path == "https://example.com/")
  }

  "https://example.com/path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("https://example.com/path/to/thing")
    assert(path == "https://example.com/path/to/thing")
  }

  "path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    val path = basepath.resolve("path/to/thing")
    assert(path == "file:///home/jdoe/documents/path/to/thing")
  }

  "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN " should " throw an exception against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    try {
      basepath.resolve("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "///path/to/thing " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("///path/to/thing")
    assert(path == "http://example.com/path/to/thing")
  }

  "//authority " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("//authority")
    assert(path == "http://authority")
  }

  "//authority/ " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("//authority/")
    assert(path == "http://authority/")
  }

  "//authority/path/to/thing " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("//authority/path/to/thing")
    assert(path == "http://authority/path/to/thing")
  }

  "/Documents and Files/thing " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("/Documents and Files/thing")
    assert(path == "http://example.com/Documents%20and%20Files/thing")
  }

  "/path/to/thing " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("/path/to/thing")
    assert(path == "http://example.com/path/to/thing")
  }

  "C:/Users/Jane/Documents and Files/Thing " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("C:/Users/Jane/Documents and Files/Thing")
    assert(path == "C:/Users/Jane/Documents and Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " throw an exception against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    try {
      basepath.resolve("C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "Documents and Files/thing " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("Documents and Files/thing")
    assert(path == "http://example.com/documents/Documents%20and%20Files/thing")
  }

  "file: " should " throw an exception against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    try {
      basepath.resolve("file:")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "file:///path/to/thing " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("file:///path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file://authority.com " should " throw an exception against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    try {
      basepath.resolve("file://authority.com")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "file://authority.com/ " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("file://authority.com/")
    assert(path == "file://authority.com/")
  }

  "file://authority.com/path/to/thing " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("file://authority.com/path/to/thing")
    assert(path == "file://authority.com/path/to/thing")
  }

  "file:/path/to/thing " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("file:/path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file:C:/Users/Jane/Documents and Files/Thing " should " throw an exception against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    try {
      basepath.resolve("file:C:/Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " throw an exception against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    try {
      basepath.resolve("file:C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "file:path/to/thing " should " throw an exception against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    try {
      basepath.resolve("file:path/to/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "https: " should " throw an exception against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    try {
      basepath.resolve("https:")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "https://example.com " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("https://example.com")
    assert(path == "https://example.com")
  }

  "https://example.com/ " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("https://example.com/")
    assert(path == "https://example.com/")
  }

  "https://example.com/path/to/thing " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("https://example.com/path/to/thing")
    assert(path == "https://example.com/path/to/thing")
  }

  "path/to/thing " should " resolve against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    val path = basepath.resolve("path/to/thing")
    assert(path == "http://example.com/documents/path/to/thing")
  }

  "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN " should " throw an exception against http://example.com/documents/" in {
    val basepath = new Urify("http://example.com/documents/")
    try {
      basepath.resolve("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "///path/to/thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("///path/to/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "//authority " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("//authority")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "//authority/ " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("//authority/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "//authority/path/to/thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("//authority/path/to/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "/Documents and Files/thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("/Documents and Files/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "/path/to/thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("/path/to/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "C:/Users/Jane/Documents and Files/Thing " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    val path = basepath.resolve("C:/Users/Jane/Documents and Files/Thing")
    assert(path == "C:/Users/Jane/Documents and Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "Documents and Files/thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("Documents and Files/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "file: " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("file:")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "file:///path/to/thing " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    val path = basepath.resolve("file:///path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file://authority.com " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("file://authority.com")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "file://authority.com/ " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    val path = basepath.resolve("file://authority.com/")
    assert(path == "file://authority.com/")
  }

  "file://authority.com/path/to/thing " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    val path = basepath.resolve("file://authority.com/path/to/thing")
    assert(path == "file://authority.com/path/to/thing")
  }

  "file:/path/to/thing " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    val path = basepath.resolve("file:/path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file:C:/Users/Jane/Documents and Files/Thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("file:C:/Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("file:C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "file:path/to/thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("file:path/to/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "https: " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("https:")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "https://example.com " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    val path = basepath.resolve("https://example.com")
    assert(path == "https://example.com")
  }

  "https://example.com/ " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    val path = basepath.resolve("https://example.com/")
    assert(path == "https://example.com/")
  }

  "https://example.com/path/to/thing " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    val path = basepath.resolve("https://example.com/path/to/thing")
    assert(path == "https://example.com/path/to/thing")
  }

  "path/to/thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("path/to/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    try {
      basepath.resolve("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _ => fail()
    }
  }

  "///path/to/thing " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("///path/to/thing")
    assert(path == "file://hostname/path/to/thing")
  }

  "//authority " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("//authority")
    assert(path == "file://authority")
  }

  "//authority/ " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("//authority/")
    assert(path == "file://authority/")
  }

  "//authority/path/to/thing " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("//authority/path/to/thing")
    assert(path == "file://authority/path/to/thing")
  }

  "/Documents and Files/thing " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("/Documents and Files/thing")
    assert(path == "file://hostname/Documents%20and%20Files/thing")
  }

  "/path/to/thing " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("/path/to/thing")
    assert(path == "file://hostname/path/to/thing")
  }

  "C:/Users/Jane/Documents and Files/Thing " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("C:/Users/Jane/Documents and Files/Thing")
    assert(path == "C:/Users/Jane/Documents and Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " throw an exception against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    try {
      basepath.resolve("C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "Documents and Files/thing " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("Documents and Files/thing")
    assert(path == "file://hostname/Documents/Documents%20and%20Files/thing")
  }

  "file: " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("file:")
    assert(path == "file://hostname/Documents/")
  }

  "file:///path/to/thing " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("file:///path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file://authority.com " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("file://authority.com")
    assert(path == "file://authority.com")
  }

  "file://authority.com/ " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("file://authority.com/")
    assert(path == "file://authority.com/")
  }

  "file://authority.com/path/to/thing " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("file://authority.com/path/to/thing")
    assert(path == "file://authority.com/path/to/thing")
  }

  "file:/path/to/thing " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("file:/path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file:C:/Users/Jane/Documents and Files/Thing " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("file:C:/Users/Jane/Documents and Files/Thing")
    assert(path == "file://hostname/Documents/C:/Users/Jane/Documents and Files/Thing")
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("file:C:Users/Jane/Documents and Files/Thing")
    assert(path == "file://hostname/Documents/C:Users/Jane/Documents and Files/Thing")
  }

  "file:path/to/thing " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("file:path/to/thing")
    assert(path == "file://hostname/Documents/path/to/thing")
  }

  "https: " should " throw an exception against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    try {
      basepath.resolve("https:")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "https://example.com " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("https://example.com")
    assert(path == "https://example.com")
  }

  "https://example.com/ " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("https://example.com/")
    assert(path == "https://example.com/")
  }

  "https://example.com/path/to/thing " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("https://example.com/path/to/thing")
    assert(path == "https://example.com/path/to/thing")
  }

  "path/to/thing " should " resolve against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    val path = basepath.resolve("path/to/thing")
    assert(path == "file://hostname/Documents/path/to/thing")
  }

  "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN " should " throw an exception against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    try {
      basepath.resolve("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "///path/to/thing " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("///path/to/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }

  "//authority " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("//authority")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }

  "//authority/ " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("//authority/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }

  "//authority/path/to/thing " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("//authority/path/to/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }

  "/Documents and Files/thing " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("/Documents and Files/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }

  "/path/to/thing " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("/path/to/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }

  "C:/Users/Jane/Documents and Files/Thing " should " resolve against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    val path = basepath.resolve("C:/Users/Jane/Documents and Files/Thing")
    assert(path == "C:/Users/Jane/Documents and Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }

  "Documents and Files/thing " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("Documents and Files/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }

  "file: " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("file:")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }

  "file:///path/to/thing " should " resolve against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    val path = basepath.resolve("file:///path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file://authority.com " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("file://authority.com")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }

  "file://authority.com/ " should " resolve against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    val path = basepath.resolve("file://authority.com/")
    assert(path == "file://authority.com/")
  }

  "file://authority.com/path/to/thing " should " resolve against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    val path = basepath.resolve("file://authority.com/path/to/thing")
    assert(path == "file://authority.com/path/to/thing")
  }

  "file:/path/to/thing " should " resolve against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    val path = basepath.resolve("file:/path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file:C:/Users/Jane/Documents and Files/Thing " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("file:C:/Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("file:C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }

  "file:path/to/thing " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("file:path/to/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }

  "https: " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("https:")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }

  "https://example.com " should " resolve against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    val path = basepath.resolve("https://example.com")
    assert(path == "https://example.com")
  }

  "https://example.com/ " should " resolve against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    val path = basepath.resolve("https://example.com/")
    assert(path == "https://example.com/")
  }

  "https://example.com/path/to/thing " should " resolve against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    val path = basepath.resolve("https://example.com/path/to/thing")
    assert(path == "https://example.com/path/to/thing")
  }

  "path/to/thing " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("path/to/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }

  "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN " should " throw an exception against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    try {
      basepath.resolve("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _ => fail()
    }
  }



}
