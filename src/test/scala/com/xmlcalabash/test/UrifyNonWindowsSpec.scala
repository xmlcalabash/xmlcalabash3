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

  "u:pass@ftp.acme.com/dir " should " throw an exception against ftp://ftp.example.com/" in {
    try {
      Urify.urify("u:pass@ftp.acme.com/dir", "ftp://ftp.example.com/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _: Throwable => fail()
    }
  }

  "\\path\\to\\thing " should " resolve against file:///root/" in {
    val path = Urify.urify("\\path\\to\\thing", "file:///root/")
    assert(path == "file:///root/%5Cpath%5Cto%5Cthing")
  }

  "\\path\\to\\thing " should " resolve against http://example.com/" in {
    val path = Urify.urify("\\path\\to\\thing", "http://example.com/")
    assert(path == "http://example.com/%5Cpath%5Cto%5Cthing")
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
    val path = Urify.urify("///path/to/thing", "file:///home/jdoe/documents/")
    assert(path == "file:///path/to/thing")
  }

  "//authority " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("//authority", "file:///home/jdoe/documents/")
    assert(path == "file://authority")
  }

  "//authority/ " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("//authority/", "file:///home/jdoe/documents/")
    assert(path == "file://authority/")
  }

  "//authority/path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("//authority/path/to/thing", "file:///home/jdoe/documents/")
    assert(path == "file://authority/path/to/thing")
  }

  "/Documents and Files/thing " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("/Documents and Files/thing", "file:///home/jdoe/documents/")
    assert(path == "file:///Documents%20and%20Files/thing")
  }

  "/path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("/path/to/thing", "file:///home/jdoe/documents/")
    assert(path == "file:///path/to/thing")
  }

  "C:/Users/Jane/Documents and Files/Thing " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "file:///home/jdoe/documents/")
    assert(path == "C:/Users/Jane/Documents and Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " throw an exception against file:///home/jdoe/documents/" in {
    try {
      Urify.urify("C:Users/Jane/Documents and Files/Thing", "file:///home/jdoe/documents/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _: Throwable => fail()
    }
  }

  "Documents and Files/thing " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("Documents and Files/thing", "file:///home/jdoe/documents/")
    assert(path == "file:///home/jdoe/documents/Documents%20and%20Files/thing")
  }

  "file: " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("file:", "file:///home/jdoe/documents/")
    assert(path == "file:///home/jdoe/documents/")
  }

  "file:///path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("file:///path/to/thing", "file:///home/jdoe/documents/")
    assert(path == "file:///path/to/thing")
  }

  "file://authority.com " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("file://authority.com", "file:///home/jdoe/documents/")
    assert(path == "file://authority.com")
  }

  "file://authority.com/ " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("file://authority.com/", "file:///home/jdoe/documents/")
    assert(path == "file://authority.com/")
  }

  "file://authority.com/path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("file://authority.com/path/to/thing", "file:///home/jdoe/documents/")
    assert(path == "file://authority.com/path/to/thing")
  }

  "file:/path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("file:/path/to/thing", "file:///home/jdoe/documents/")
    assert(path == "file:///path/to/thing")
  }

  "file:C:/Users/Jane/Documents and Files/Thing " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "file:///home/jdoe/documents/")
    assert(path == "file:///home/jdoe/documents/C:/Users/Jane/Documents and Files/Thing")
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("file:C:Users/Jane/Documents and Files/Thing", "file:///home/jdoe/documents/")
    assert(path == "file:///home/jdoe/documents/C:Users/Jane/Documents and Files/Thing")
  }

  "file:path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("file:path/to/thing", "file:///home/jdoe/documents/")
    assert(path == "file:///home/jdoe/documents/path/to/thing")
  }

  "https: " should " throw an exception against file:///home/jdoe/documents/" in {
    try {
      Urify.urify("https:", "file:///home/jdoe/documents/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _: Throwable => fail()
    }
  }

  "https://example.com " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("https://example.com", "file:///home/jdoe/documents/")
    assert(path == "https://example.com")
  }

  "https://example.com/ " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("https://example.com/", "file:///home/jdoe/documents/")
    assert(path == "https://example.com/")
  }

  "https://example.com/path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("https://example.com/path/to/thing", "file:///home/jdoe/documents/")
    assert(path == "https://example.com/path/to/thing")
  }

  "path/to/thing " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("path/to/thing", "file:///home/jdoe/documents/")
    assert(path == "file:///home/jdoe/documents/path/to/thing")
  }

  "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN " should " resolve against file:///home/jdoe/documents/" in {
    val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "file:///home/jdoe/documents/")
    assert(path == "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
  }

  "///path/to/thing " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("///path/to/thing", "http://example.com/documents/")
    assert(path == "http://example.com/path/to/thing")
  }

  "//authority " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("//authority", "http://example.com/documents/")
    assert(path == "http://authority")
  }

  "//authority/ " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("//authority/", "http://example.com/documents/")
    assert(path == "http://authority/")
  }

  "//authority/path/to/thing " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("//authority/path/to/thing", "http://example.com/documents/")
    assert(path == "http://authority/path/to/thing")
  }

  "/Documents and Files/thing " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("/Documents and Files/thing", "http://example.com/documents/")
    assert(path == "http://example.com/Documents%20and%20Files/thing")
  }

  "/path/to/thing " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("/path/to/thing", "http://example.com/documents/")
    assert(path == "http://example.com/path/to/thing")
  }

  "C:/Users/Jane/Documents and Files/Thing " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "http://example.com/documents/")
    assert(path == "C:/Users/Jane/Documents and Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " throw an exception against http://example.com/documents/" in {
    try {
      Urify.urify("C:Users/Jane/Documents and Files/Thing", "http://example.com/documents/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _: Throwable => fail()
    }
  }

  "Documents and Files/thing " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("Documents and Files/thing", "http://example.com/documents/")
    assert(path == "http://example.com/documents/Documents%20and%20Files/thing")
  }

  "file: " should " throw an exception against http://example.com/documents/" in {
    try {
      Urify.urify("file:", "http://example.com/documents/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _: Throwable => fail()
    }
  }

  "file:///path/to/thing " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("file:///path/to/thing", "http://example.com/documents/")
    assert(path == "file:///path/to/thing")
  }

  "file://authority.com " should " throw an exception against http://example.com/documents/" in {
    try {
      Urify.urify("file://authority.com", "http://example.com/documents/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _: Throwable => fail()
    }
  }

  "file://authority.com/ " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("file://authority.com/", "http://example.com/documents/")
    assert(path == "file://authority.com/")
  }

  "file://authority.com/path/to/thing " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("file://authority.com/path/to/thing", "http://example.com/documents/")
    assert(path == "file://authority.com/path/to/thing")
  }

  "file:/path/to/thing " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("file:/path/to/thing", "http://example.com/documents/")
    assert(path == "file:///path/to/thing")
  }

  "file:C:/Users/Jane/Documents and Files/Thing " should " throw an exception against http://example.com/documents/" in {
    try {
      Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "http://example.com/documents/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _: Throwable => fail()
    }
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " throw an exception against http://example.com/documents/" in {
    try {
      Urify.urify("file:C:Users/Jane/Documents and Files/Thing", "http://example.com/documents/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _: Throwable => fail()
    }
  }

  "file:path/to/thing " should " throw an exception against http://example.com/documents/" in {
    try {
      Urify.urify("file:path/to/thing", "http://example.com/documents/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _: Throwable => fail()
    }
  }

  "https: " should " throw an exception against http://example.com/documents/" in {
    try {
      Urify.urify("https:", "http://example.com/documents/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _: Throwable => fail()
    }
  }

  "https://example.com " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("https://example.com", "http://example.com/documents/")
    assert(path == "https://example.com")
  }

  "https://example.com/ " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("https://example.com/", "http://example.com/documents/")
    assert(path == "https://example.com/")
  }

  "https://example.com/path/to/thing " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("https://example.com/path/to/thing", "http://example.com/documents/")
    assert(path == "https://example.com/path/to/thing")
  }

  "path/to/thing " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("path/to/thing", "http://example.com/documents/")
    assert(path == "http://example.com/documents/path/to/thing")
  }

  "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN " should " resolve against http://example.com/documents/" in {
    val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "http://example.com/documents/")
    assert(path == "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
  }

  "///path/to/thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    try {
      Urify.urify("///path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _: Throwable => fail()
    }
  }

  "//authority " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    try {
      Urify.urify("//authority", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _: Throwable => fail()
    }
  }

  "//authority/ " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    try {
      Urify.urify("//authority/", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _: Throwable => fail()
    }
  }

  "//authority/path/to/thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    try {
      Urify.urify("//authority/path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _: Throwable => fail()
    }
  }

  "/Documents and Files/thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    try {
      Urify.urify("/Documents and Files/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _: Throwable => fail()
    }
  }

  "/path/to/thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    try {
      Urify.urify("/path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _: Throwable => fail()
    }
  }

  "C:/Users/Jane/Documents and Files/Thing " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    assert(path == "C:/Users/Jane/Documents and Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    try {
      Urify.urify("C:Users/Jane/Documents and Files/Thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _: Throwable => fail()
    }
  }

  "Documents and Files/thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    try {
      Urify.urify("Documents and Files/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _: Throwable => fail()
    }
  }

  "file: " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    try {
      Urify.urify("file:", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _: Throwable => fail()
    }
  }

  "file:///path/to/thing " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val path = Urify.urify("file:///path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    assert(path == "file:///path/to/thing")
  }

  "file://authority.com " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    try {
      Urify.urify("file://authority.com", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _: Throwable => fail()
    }
  }

  "file://authority.com/ " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val path = Urify.urify("file://authority.com/", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    assert(path == "file://authority.com/")
  }

  "file://authority.com/path/to/thing " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val path = Urify.urify("file://authority.com/path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    assert(path == "file://authority.com/path/to/thing")
  }

  "file:/path/to/thing " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val path = Urify.urify("file:/path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    assert(path == "file:///path/to/thing")
  }

  "file:C:/Users/Jane/Documents and Files/Thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    try {
      Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _: Throwable => fail()
    }
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    try {
      Urify.urify("file:C:Users/Jane/Documents and Files/Thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _: Throwable => fail()
    }
  }

  "file:path/to/thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    try {
      Urify.urify("file:path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _: Throwable => fail()
    }
  }

  "https: " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    try {
      Urify.urify("https:", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _: Throwable => fail()
    }
  }

  "https://example.com " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val path = Urify.urify("https://example.com", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    assert(path == "https://example.com")
  }

  "https://example.com/ " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val path = Urify.urify("https://example.com/", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    assert(path == "https://example.com/")
  }

  "https://example.com/path/to/thing " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val path = Urify.urify("https://example.com/path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    assert(path == "https://example.com/path/to/thing")
  }

  "path/to/thing " should " throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    try {
      Urify.urify("path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0080")
      case _: Throwable => fail()
    }
  }

  "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    assert(path == "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
  }

  "///path/to/thing " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("///path/to/thing", "file://hostname/Documents/")
    assert(path == "file://hostname/path/to/thing")
  }

  "//authority " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("//authority", "file://hostname/Documents/")
    assert(path == "file://authority")
  }

  "//authority/ " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("//authority/", "file://hostname/Documents/")
    assert(path == "file://authority/")
  }

  "//authority/path/to/thing " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("//authority/path/to/thing", "file://hostname/Documents/")
    assert(path == "file://authority/path/to/thing")
  }

  "/Documents and Files/thing " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("/Documents and Files/thing", "file://hostname/Documents/")
    assert(path == "file://hostname/Documents%20and%20Files/thing")
  }

  "/path/to/thing " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("/path/to/thing", "file://hostname/Documents/")
    assert(path == "file://hostname/path/to/thing")
  }

  "C:/Users/Jane/Documents and Files/Thing " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "file://hostname/Documents/")
    assert(path == "C:/Users/Jane/Documents and Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " throw an exception against file://hostname/Documents/" in {
    try {
      Urify.urify("C:Users/Jane/Documents and Files/Thing", "file://hostname/Documents/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _: Throwable => fail()
    }
  }

  "Documents and Files/thing " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("Documents and Files/thing", "file://hostname/Documents/")
    assert(path == "file://hostname/Documents/Documents%20and%20Files/thing")
  }

  "file: " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("file:", "file://hostname/Documents/")
    assert(path == "file://hostname/Documents/")
  }

  "file:///path/to/thing " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("file:///path/to/thing", "file://hostname/Documents/")
    assert(path == "file:///path/to/thing")
  }

  "file://authority.com " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("file://authority.com", "file://hostname/Documents/")
    assert(path == "file://authority.com")
  }

  "file://authority.com/ " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("file://authority.com/", "file://hostname/Documents/")
    assert(path == "file://authority.com/")
  }

  "file://authority.com/path/to/thing " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("file://authority.com/path/to/thing", "file://hostname/Documents/")
    assert(path == "file://authority.com/path/to/thing")
  }

  "file:/path/to/thing " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("file:/path/to/thing", "file://hostname/Documents/")
    assert(path == "file:///path/to/thing")
  }

  "file:C:/Users/Jane/Documents and Files/Thing " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "file://hostname/Documents/")
    assert(path == "file://hostname/Documents/C:/Users/Jane/Documents and Files/Thing")
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("file:C:Users/Jane/Documents and Files/Thing", "file://hostname/Documents/")
    assert(path == "file://hostname/Documents/C:Users/Jane/Documents and Files/Thing")
  }

  "file:path/to/thing " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("file:path/to/thing", "file://hostname/Documents/")
    assert(path == "file://hostname/Documents/path/to/thing")
  }

  "https: " should " throw an exception against file://hostname/Documents/" in {
    try {
      Urify.urify("https:", "file://hostname/Documents/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _: Throwable => fail()
    }
  }

  "https://example.com " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("https://example.com", "file://hostname/Documents/")
    assert(path == "https://example.com")
  }

  "https://example.com/ " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("https://example.com/", "file://hostname/Documents/")
    assert(path == "https://example.com/")
  }

  "https://example.com/path/to/thing " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("https://example.com/path/to/thing", "file://hostname/Documents/")
    assert(path == "https://example.com/path/to/thing")
  }

  "path/to/thing " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("path/to/thing", "file://hostname/Documents/")
    assert(path == "file://hostname/Documents/path/to/thing")
  }

  "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN " should " resolve against file://hostname/Documents/" in {
    val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "file://hostname/Documents/")
    assert(path == "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
  }

  "///path/to/thing " should " throw an exception against file:not-absolute" in {
    try {
      Urify.urify("///path/to/thing", "file:not-absolute")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _: Throwable => fail()
    }
  }

  "//authority " should " throw an exception against file:not-absolute" in {
    try {
      Urify.urify("//authority", "file:not-absolute")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _: Throwable => fail()
    }
  }

  "//authority/ " should " throw an exception against file:not-absolute" in {
    try {
      Urify.urify("//authority/", "file:not-absolute")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _: Throwable => fail()
    }
  }

  "//authority/path/to/thing " should " throw an exception against file:not-absolute" in {
    try {
      Urify.urify("//authority/path/to/thing", "file:not-absolute")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _: Throwable => fail()
    }
  }

  "/Documents and Files/thing " should " throw an exception against file:not-absolute" in {
    try {
      Urify.urify("/Documents and Files/thing", "file:not-absolute")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _: Throwable => fail()
    }
  }

  "/path/to/thing " should " throw an exception against file:not-absolute" in {
    try {
      Urify.urify("/path/to/thing", "file:not-absolute")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _: Throwable => fail()
    }
  }

  "C:/Users/Jane/Documents and Files/Thing " should " resolve against file:not-absolute" in {
    val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "file:not-absolute")
    assert(path == "C:/Users/Jane/Documents and Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " throw an exception against file:not-absolute" in {
    try {
      Urify.urify("C:Users/Jane/Documents and Files/Thing", "file:not-absolute")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _: Throwable => fail()
    }
  }

  "Documents and Files/thing " should " throw an exception against file:not-absolute" in {
    try {
      Urify.urify("Documents and Files/thing", "file:not-absolute")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _: Throwable => fail()
    }
  }

  "file: " should " throw an exception against file:not-absolute" in {
    try {
      Urify.urify("file:", "file:not-absolute")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _: Throwable => fail()
    }
  }

  "file:///path/to/thing " should " resolve against file:not-absolute" in {
    val path = Urify.urify("file:///path/to/thing", "file:not-absolute")
    assert(path == "file:///path/to/thing")
  }

  "file://authority.com " should " throw an exception against file:not-absolute" in {
    try {
      Urify.urify("file://authority.com", "file:not-absolute")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _: Throwable => fail()
    }
  }

  "file://authority.com/ " should " resolve against file:not-absolute" in {
    val path = Urify.urify("file://authority.com/", "file:not-absolute")
    assert(path == "file://authority.com/")
  }

  "file://authority.com/path/to/thing " should " resolve against file:not-absolute" in {
    val path = Urify.urify("file://authority.com/path/to/thing", "file:not-absolute")
    assert(path == "file://authority.com/path/to/thing")
  }

  "file:/path/to/thing " should " resolve against file:not-absolute" in {
    val path = Urify.urify("file:/path/to/thing", "file:not-absolute")
    assert(path == "file:///path/to/thing")
  }

  "file:C:/Users/Jane/Documents and Files/Thing " should " throw an exception against file:not-absolute" in {
    try {
      Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "file:not-absolute")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _: Throwable => fail()
    }
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " throw an exception against file:not-absolute" in {
    try {
      Urify.urify("file:C:Users/Jane/Documents and Files/Thing", "file:not-absolute")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _: Throwable => fail()
    }
  }

  "file:path/to/thing " should " throw an exception against file:not-absolute" in {
    try {
      Urify.urify("file:path/to/thing", "file:not-absolute")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _: Throwable => fail()
    }
  }

  "https: " should " throw an exception against file:not-absolute" in {
    try {
      Urify.urify("https:", "file:not-absolute")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _: Throwable => fail()
    }
  }

  "https://example.com " should " resolve against file:not-absolute" in {
    val path = Urify.urify("https://example.com", "file:not-absolute")
    assert(path == "https://example.com")
  }

  "https://example.com/ " should " resolve against file:not-absolute" in {
    val path = Urify.urify("https://example.com/", "file:not-absolute")
    assert(path == "https://example.com/")
  }

  "https://example.com/path/to/thing " should " resolve against file:not-absolute" in {
    val path = Urify.urify("https://example.com/path/to/thing", "file:not-absolute")
    assert(path == "https://example.com/path/to/thing")
  }

  "path/to/thing " should " throw an exception against file:not-absolute" in {
    try {
      Urify.urify("path/to/thing", "file:not-absolute")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0074")
      case _: Throwable => fail()
    }
  }

  "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN " should " resolve against file:not-absolute" in {
    val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "file:not-absolute")
    assert(path == "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
  }
}
