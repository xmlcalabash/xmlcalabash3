package com.xmlcalabash.util

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.util.Urify.{filesep, windows}

import java.net.URI
import scala.collection.mutable.ListBuffer

object Urify {
  private var _osname: String = Option(System.getProperty("os.name")).getOrElse("not-windows")
  private var _filesep: String = Option(System.getProperty("file.separator")).getOrElse("/")
  private var _cwd = Option.empty[String]

  def cwd: String = {
    val dir = _cwd.getOrElse(System.getProperty("user.dir"))
    if (dir.endsWith(filesep)) {
      dir
    } else {
      dir + filesep
    }
  }

  def osname: String = _osname
  def filesep: String = _filesep
  def windows: Boolean = osname.startsWith("Windows")

  def mockOS(name: String, sep: String, cwd: Option[String]): Unit = {
    _osname = name
    _filesep = sep
    _cwd = cwd
  }

  private val WindowsFilepath = "^(?i)(file:/*)?(?i)([a-z]):(.*)$".r
  private val FileAuthority = "^(?i)(file://)([^/]+)(/.*)?$".r
  private val Filepath = "^(?i)(file:)(.*)$".r
  private val OtherScheme = "^(?i)([a-z]+):(.*)$".r
  private val Authority = "^//([^/]+)(/.*)?$".r
}

class Urify(val filepath: String) {
  private var _scheme = Option.empty[String]
  private var _explicit = false
  private var _hierarchical = true
  private var _authority = Option.empty[String]
  private var _driveLetter = Option.empty[String]
  private var _path: String = _
  private var _absolute = false

  if (filesep != "/") {
    _path = filepath.replace(filesep, "/")
  } else {
    _path = filepath
  }

  if (windows) {
    filepath match {
      case "" =>
        _path = ""
      case "//" =>
        _path = ""
        _absolute = true
      case Urify.WindowsFilepath(fpfile, fpdrive, fppath) =>
        _scheme = Some("file")
        _explicit = Option(fpfile).getOrElse("").toLowerCase().startsWith("file:")
        _driveLetter = Some(fpdrive)
        _path = fppath.replaceAll("^/+", "/")
        _absolute = _path.startsWith("/")
      case Urify.FileAuthority(fpfile, fpauthority, fppath) =>
        _scheme = Some("file")
        _explicit = true
        _authority = Some(fpauthority)
        _path = Option(fppath).getOrElse("").replaceAll("^/+", "/")
        _absolute = _path.startsWith("/")
      case Urify.Filepath(fpfile, fppath) =>
        _scheme = Some("file")
        _explicit = true
        _path = fppath.replaceAll("^/+", "/")
        _absolute = _path.startsWith("/")
      case Urify.OtherScheme(fpscheme, fppath) =>
        _scheme = Some(fpscheme)
        _explicit = true
        _path = fppath
        if (List("http", "https", "ftp").contains(fpscheme)) {
          _absolute = _path.startsWith("/")
        } else if (List("urn", "doi", "mailto").contains(fpscheme)) {
          _hierarchical = false
        } else {
          _hierarchical = filepath.contains("/")
          _absolute = _hierarchical && _path.startsWith("/")
        }
      case Urify.Authority(fpauthority, fppath) =>
        _authority = Some(fpauthority)
        _path = Option(fppath).getOrElse("").replaceAll("^/+", "/")
        _absolute = _path.startsWith("/")
      case _ =>
        _path = filepath.replaceAll("^/+", "/")
        _absolute = _path.startsWith("/")
    }
  } else {
    filepath match {
      case "" =>
        _path = ""
      case "//" =>
        _path = ""
        _absolute = true
      case Urify.FileAuthority(fpfile, fpauthority, fppath) =>
        _scheme = Some("file")
        _explicit = true
        _authority = Some(fpauthority)
        _path = Option(fppath).getOrElse("").replaceAll("^/+", "/")
        _absolute = _path.startsWith("/")
      case Urify.Filepath(fpfile, fppath) =>
        _scheme = Some("file")
        _explicit = true
        _path = fppath.replaceAll("^/+", "/")
        _absolute = _path.startsWith("/")
      case Urify.OtherScheme(fpscheme, fppath) =>
        _scheme = Some(fpscheme)
        _explicit = true
        _path = fppath
        if (List("http", "https", "ftp").contains(fpscheme)) {
          _absolute = _path.startsWith("/")
        } else if (List("urn", "doi", "mailto").contains(fpscheme)) {
          _hierarchical = false
        } else {
          _hierarchical = filepath.contains("/")
          _absolute = _hierarchical && _path.startsWith("/")
        }
      case Urify.Authority(fpauthority, fppath) =>
        _authority = Some(fpauthority)
        _path = Option(fppath).getOrElse("").replaceAll("^/+", "/")
        _absolute = _path.startsWith("/")
      case _ =>
        _path = filepath.replaceAll("^/+", "/")
        _absolute = _path.startsWith("/")
    }
  }

  def this(uri: URI) = {
    this(uri.toString)
  }

  def this(copy: Urify) = {
    this(copy.filepath)
    _scheme = copy._scheme
    _explicit = copy._explicit
    _hierarchical = copy._hierarchical
    _authority = copy._authority
    _driveLetter = copy._driveLetter
    _path = copy._path
    _absolute = copy._absolute
  }

  def scheme: Option[String] = _scheme
  def explicit: Boolean = _explicit
  def hierarchical: Boolean = _hierarchical
  def authority: Option[String] = _authority
  def driveLetter: Option[String] = _driveLetter
  def path: String = _path
  def absolute: Boolean = _absolute
  def relative: Boolean = !_absolute

  def fixedPath: String = {
    if (explicit) {
      return _path
    }

    var newpath = path.replaceAll("\\?", "%3F")
      .replaceAll("#", "%23")
      .replaceAll(" ", "%20")

    // unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
    var buf = new StringBuffer()
    var pos = newpath.indexOf("%")
    while (pos >= 0) {
      buf.append(newpath.substring(0, pos))
      newpath = newpath.substring(pos)
      if (newpath.length < 3) {
        pos = -1
      } else {
        val c1 = newpath.charAt(1).toLower
        val c2 = newpath.charAt(2).toLower
        if (((c1 >= '0' && c1 <= '9') || (c1 >= 'a' && c1 <= 'f'))
          && ((c2 >= '0' && c2 <= '9') || (c2 >= 'a' && c2 <= 'f'))) {
          val num = Integer.parseInt(s"${c1}${c2}", 16)
          if ((num >= 'A' && num <= 'Z') || (num >= 'a' && num <= 'z') || (num >= '0' && num <= '9')
            || (num == '-') || (num == '.') || (num == '_') || (num == '~')) {
            buf.append(num.toChar)
          } else {
            buf.append(newpath.substring(0, 3))
          }
          newpath = newpath.substring(3)
        } else {
          buf.append("%")
          newpath = newpath.substring(1)
        }

        pos = newpath.indexOf("%")
      }
    }
    buf.append(newpath)

    newpath = buf.toString
    buf = new StringBuffer()
    pos = 0
    while (pos < newpath.length) {
      val ch = newpath.charAt(pos)
      ch match {
        case '%' =>
          if (pos + 2 >= newpath.length) {
            buf.append("%25")
          } else {
            val c1 = newpath.charAt(pos + 1).toLower
            val c2 = newpath.charAt(pos + 2).toLower
            if (((c1 >= '0' && c1 <= '9') || (c1 >= 'a' && c1 <= 'f'))
              && ((c2 >= '0' && c2 <= '9') || (c2 >= 'a' && c2 <= 'f'))) {
              buf.append("%")
            } else {
              buf.append("%25")
            }
          }
        case _ =>
          buf.append(ch)
      }
      pos += 1
    }

    buf.toString
  }

  def resolve(uri: URI): String = {
    resolve(uri.toString)
  }

  def resolve(uri: String): String = {
    val respath = new Urify(uri)
    if (respath.scheme.isDefined && respath.absolute) {
      return respath.toString
    }

    if (!hierarchical) {
      throw XProcException.xdUrifyNonhierarchicalBase(uri, filepath, None)
    }

    if (!absolute) {
      throw XProcException.xdUrifyFailed(uri, filepath, None)
    }

    if (respath.driveLetter.isDefined && (driveLetter.isEmpty || respath.driveLetter.get != driveLetter.get)) {
      throw XProcException.xdUrifyDifferentDrives(uri, filepath, None)
    }

    if ((respath.driveLetter.isDefined && authority.isDefined)
      || (respath.authority.isDefined && driveLetter.isDefined)) {
      throw XProcException.xdUrifyMixedDrivesAndAuthorities(uri, filepath, None)
    }

    if ((respath.scheme.isDefined && respath.scheme.get != scheme.get)) {
      throw XProcException.xdUrifyDifferentSchemes(uri, filepath, None)
    }

    if (scheme.isDefined && scheme.get != "file" && absolute) {
      return URI.create(filepath).resolve(respath.toString).toString
    }

    val rscheme = scheme.get
    val rauthority = if (respath.authority.isDefined) {
      respath.authority
    } else {
      authority
    }
    val rdrive = driveLetter
    val rpath = if (respath.authority.isDefined || respath.absolute) {
      respath.fixedPath
    } else {
      resolvePaths(fixedPath, respath.fixedPath)
    }

    val sb = new StringBuffer()
    sb.append(rscheme)
    sb.append(":")
    if (rauthority.isDefined) {
      sb.append("//")
      sb.append(rauthority.get)
    } else {
      if (scheme.isDefined && scheme.get == "file") {
        if (absolute) {
          sb.append("//")
          if (rdrive.isDefined) {
            sb.append("/")
          }
        }
      }
    }
    if (rdrive.isDefined) {
      sb.append(rdrive.get)
      sb.append(":")
    }
    sb.append(rpath)
    sb.toString
  }

  private def resolvePaths(basepath: String, newpath: String): String = {
    // This is only called when newpath is relative.
    val pos = basepath.lastIndexOf("/")
    if (pos >= 0) {
      resolveDotSegments(basepath.substring(0, pos+1) + newpath)
    } else {
      resolveDotSegments("/" + newpath)
    }
  }

  private def resolveDotSegments(path: String): String = {
    val parts = path.split("/")
    var stack = ListBuffer.empty[String]
    for (part <- parts) {
      part match {
        case "." => ()
        case ".." =>
          if (stack.nonEmpty) {
            stack = stack.dropRight(1)
          }
        case _ =>
          stack += part
      }
    }
    if (path.endsWith("/")) {
      stack += ""
    }

    stack.mkString("/")
  }


  def toURI: URI = {
    new URI(toString)
  }

  override def toString: String = {
    val sb = new StringBuffer()
    if (scheme.isDefined) {
      sb.append(scheme.get)
      sb.append(":")
    }
    if (authority.isDefined) {
      sb.append("//")
      sb.append(authority.get)
    } else {
      if (scheme.isDefined && scheme.get == "file") {
        if (absolute) {
          sb.append("//")
          if (driveLetter.isDefined) {
            sb.append("/")
          }
        }
      }
    }
    if (driveLetter.isDefined) {
      sb.append(driveLetter.get)
      sb.append(":")
    }
    sb.append(fixedPath)
    sb.toString
 }
}
