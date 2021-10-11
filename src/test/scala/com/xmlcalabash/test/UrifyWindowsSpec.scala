package com.xmlcalabash.test

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.util.Urify
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

class UrifyWindowsSpec extends AnyFlatSpec with BeforeAndAfter {
  private val OSNAME = "Windows"
  private val FILESEP = "\\"
  private val CWD = "C:\\Users\\JohnDoe\\"

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

  "///path/to/thing " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("///path/to/thing")
    assert(path == "file:///C:/path/to/thing")
  }

  "//authority " should " throw an exception against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    try {
      basepath.resolve("//authority")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0076")
      case _ => fail()
    }
  }

  "//authority/ " should " throw an exception against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    try {
      basepath.resolve("//authority/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0076")
      case _ => fail()
    }
  }

  "//authority/path/to/thing " should " throw an exception against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    try {
      basepath.resolve("//authority/path/to/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0076")
      case _ => fail()
    }
  }

  "/Documents and Files/thing " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("/Documents and Files/thing")
    assert(path == "file:///C:/Documents%20and%20Files/thing")
  }

  "/path/to/thing " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("/path/to/thing")
    assert(path == "file:///C:/path/to/thing")
  }

  "C:/Users/Jane/Documents and Files/Thing " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("C:/Users/Jane/Documents and Files/Thing")
    assert(path == "file:///C:/Users/Jane/Documents%20and%20Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("C:Users/Jane/Documents and Files/Thing")
    assert(path == "file:///C:/Users/Jane%20Doe/Documents/Users/Jane/Documents%20and%20Files/Thing")
  }

  "Documents and Files/thing " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("Documents and Files/thing")
    assert(path == "file:///C:/Users/Jane%20Doe/Documents/Documents%20and%20Files/thing")
  }

  "file: " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("file:")
    assert(path == "file:///C:/Users/Jane%20Doe/Documents/")
  }

  "file:///path/to/thing " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("file:///path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file://authority.com " should " throw an exception against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    try {
      basepath.resolve("file://authority.com")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0076")
      case _ => fail()
    }
  }

  "file://authority.com/ " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("file://authority.com/")
    assert(path == "file://authority.com/")
  }

  "file://authority.com/path/to/thing " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("file://authority.com/path/to/thing")
    assert(path == "file://authority.com/path/to/thing")
  }

  "file:/path/to/thing " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("file:/path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file:C:/Users/Jane/Documents and Files/Thing " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("file:C:/Users/Jane/Documents and Files/Thing")
    assert(path == "file:///C:/Users/Jane/Documents and Files/Thing")
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("file:C:Users/Jane/Documents and Files/Thing")
    assert(path == "file:///C:/Users/Jane%20Doe/Documents/Users/Jane/Documents and Files/Thing")
  }

  "file:path/to/thing " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("file:path/to/thing")
    assert(path == "file:///C:/Users/Jane%20Doe/Documents/path/to/thing")
  }

  "https: " should " throw an exception against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    try {
      basepath.resolve("https:")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "https://example.com " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("https://example.com")
    assert(path == "https://example.com")
  }

  "https://example.com/ " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("https://example.com/")
    assert(path == "https://example.com/")
  }

  "https://example.com/path/to/thing " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("https://example.com/path/to/thing")
    assert(path == "https://example.com/path/to/thing")
  }

  "path/to/thing " should " resolve against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("path/to/thing")
    assert(path == "file:///C:/Users/Jane%20Doe/Documents/path/to/thing")
  }

  "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN " should " throw an exception against file:///C:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///C:/Users/Jane%20Doe/Documents/")
    try {
      basepath.resolve("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "///path/to/thing " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("///path/to/thing")
    assert(path == "file:///D:/path/to/thing")
  }

  "//authority " should " throw an exception against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    try {
      basepath.resolve("//authority")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0076")
      case _ => fail()
    }
  }

  "//authority/ " should " throw an exception against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    try {
      basepath.resolve("//authority/")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0076")
      case _ => fail()
    }
  }

  "//authority/path/to/thing " should " throw an exception against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    try {
      basepath.resolve("//authority/path/to/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0076")
      case _ => fail()
    }
  }

  "/Documents and Files/thing " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("/Documents and Files/thing")
    assert(path == "file:///D:/Documents%20and%20Files/thing")
  }

  "/path/to/thing " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("/path/to/thing")
    assert(path == "file:///D:/path/to/thing")
  }

  "C:/Users/Jane/Documents and Files/Thing " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("C:/Users/Jane/Documents and Files/Thing")
    assert(path == "file:///C:/Users/Jane/Documents%20and%20Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " throw an exception against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    try {
      basepath.resolve("C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0075")
      case _ => fail()
    }
  }

  "Documents and Files/thing " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("Documents and Files/thing")
    assert(path == "file:///D:/Users/Jane%20Doe/Documents/Documents%20and%20Files/thing")
  }

  "file: " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("file:")
    assert(path == "file:///D:/Users/Jane%20Doe/Documents/")
  }

  "file:///path/to/thing " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("file:///path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file://authority.com " should " throw an exception against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    try {
      basepath.resolve("file://authority.com")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0076")
      case _ => fail()
    }
  }

  "file://authority.com/ " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("file://authority.com/")
    assert(path == "file://authority.com/")
  }

  "file://authority.com/path/to/thing " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("file://authority.com/path/to/thing")
    assert(path == "file://authority.com/path/to/thing")
  }

  "file:/path/to/thing " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("file:/path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file:C:/Users/Jane/Documents and Files/Thing " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("file:C:/Users/Jane/Documents and Files/Thing")
    assert(path == "file:///C:/Users/Jane/Documents and Files/Thing")
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " throw an exception against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    try {
      basepath.resolve("file:C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0075")
      case _ => fail()
    }
  }

  "file:path/to/thing " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("file:path/to/thing")
    assert(path == "file:///D:/Users/Jane%20Doe/Documents/path/to/thing")
  }

  "https: " should " throw an exception against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    try {
      basepath.resolve("https:")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "https://example.com " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("https://example.com")
    assert(path == "https://example.com")
  }

  "https://example.com/ " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("https://example.com/")
    assert(path == "https://example.com/")
  }

  "https://example.com/path/to/thing " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("https://example.com/path/to/thing")
    assert(path == "https://example.com/path/to/thing")
  }

  "path/to/thing " should " resolve against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    val path = basepath.resolve("path/to/thing")
    assert(path == "file:///D:/Users/Jane%20Doe/Documents/path/to/thing")
  }

  "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN " should " throw an exception against file:///D:/Users/Jane%20Doe/Documents/" in {
    val basepath = new Urify("file:///D:/Users/Jane%20Doe/Documents/")
    try {
      basepath.resolve("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
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
    assert(path == "file:///C:/Users/Jane/Documents%20and%20Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " throw an exception against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    try {
      basepath.resolve("C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0075")
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
    assert(path == "file:///C:/Users/Jane/Documents and Files/Thing")
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " throw an exception against file://hostname/Documents/" in {
    val basepath = new Urify("file://hostname/Documents/")
    try {
      basepath.resolve("file:C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0075")
      case _ => fail()
    }
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

  "///path/to/thing " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("///path/to/thing")
    assert(path == "http://example.com/path/to/thing")
  }

  "//authority " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("//authority")
    assert(path == "http://authority")
  }

  "//authority/ " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("//authority/")
    assert(path == "http://authority/")
  }

  "//authority/path/to/thing " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("//authority/path/to/thing")
    assert(path == "http://authority/path/to/thing")
  }

  "/Documents and Files/thing " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("/Documents and Files/thing")
    assert(path == "http://example.com/Documents%20and%20Files/thing")
  }

  "/path/to/thing " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("/path/to/thing")
    assert(path == "http://example.com/path/to/thing")
  }

  "C:/Users/Jane/Documents and Files/Thing " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("C:/Users/Jane/Documents and Files/Thing")
    assert(path == "file:///C:/Users/Jane/Documents%20and%20Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " throw an exception against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    try {
      basepath.resolve("C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0075")
      case _ => fail()
    }
  }

  "Documents and Files/thing " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("Documents and Files/thing")
    assert(path == "http://example.com/Documents/Documents%20and%20Files/thing")
  }

  "file: " should " throw an exception against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    try {
      basepath.resolve("file:")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "file:///path/to/thing " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("file:///path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file://authority.com " should " throw an exception against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    try {
      basepath.resolve("file://authority.com")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "file://authority.com/ " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("file://authority.com/")
    assert(path == "file://authority.com/")
  }

  "file://authority.com/path/to/thing " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("file://authority.com/path/to/thing")
    assert(path == "file://authority.com/path/to/thing")
  }

  "file:/path/to/thing " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("file:/path/to/thing")
    assert(path == "file:///path/to/thing")
  }

  "file:C:/Users/Jane/Documents and Files/Thing " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("file:C:/Users/Jane/Documents and Files/Thing")
    assert(path == "file:///C:/Users/Jane/Documents and Files/Thing")
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " throw an exception against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    try {
      basepath.resolve("file:C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0075")
      case _ => fail()
    }
  }

  "file:path/to/thing " should " throw an exception against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    try {
      basepath.resolve("file:path/to/thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "https: " should " throw an exception against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    try {
      basepath.resolve("https:")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

  "https://example.com " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("https://example.com")
    assert(path == "https://example.com")
  }

  "https://example.com/ " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("https://example.com/")
    assert(path == "https://example.com/")
  }

  "https://example.com/path/to/thing " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("https://example.com/path/to/thing")
    assert(path == "https://example.com/path/to/thing")
  }

  "path/to/thing " should " resolve against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    val path = basepath.resolve("path/to/thing")
    assert(path == "http://example.com/Documents/path/to/thing")
  }

  "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN " should " throw an exception against http://example.com/Documents/" in {
    val basepath = new Urify("http://example.com/Documents/")
    try {
      basepath.resolve("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0077")
      case _ => fail()
    }
  }

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
    assert(path == "file:///C:/Users/Jane/Documents%20and%20Files/Thing")
  }

  "C:Users/Jane/Documents and Files/Thing " should " throw an exception against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    try {
      basepath.resolve("C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0075")
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
    assert(path == "file:///C:/Users/Jane/Documents and Files/Thing")
  }

  "file:C:Users/Jane/Documents and Files/Thing " should " throw an exception against file:///home/jdoe/documents/" in {
    val basepath = new Urify("file:///home/jdoe/documents/")
    try {
      basepath.resolve("file:C:Users/Jane/Documents and Files/Thing")
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code.getLocalName == "XD0075")
      case _ => fail()
    }
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
    assert(path == "file:///C:/Users/Jane/Documents%20and%20Files/Thing")
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

  "file:C:/Users/Jane/Documents and Files/Thing " should " resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN" in {
    val basepath = new Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    val path = basepath.resolve("file:C:/Users/Jane/Documents and Files/Thing")
    assert(path == "file:///C:/Users/Jane/Documents and Files/Thing")
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
    assert(path == "file:///C:/Users/Jane/Documents%20and%20Files/Thing")
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

  "file:C:/Users/Jane/Documents and Files/Thing " should " resolve against file:not-absolute" in {
    val basepath = new Urify("file:not-absolute")
    val path = basepath.resolve("file:C:/Users/Jane/Documents and Files/Thing")
    assert(path == "file:///C:/Users/Jane/Documents and Files/Thing")
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
