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

  def urify(filestr: String): String = {
    urify(filestr, None)
  }

  def urify(filestr: String, basedir: String): String = {
    urify(filestr, Some(basedir))
  }

  def urify(filestr: String, basedir: Option[String]): String = {
    val filepath = new Urify(filestr, basedir)
    if (!filepath.hierarchical || (filepath.scheme.isDefined && filepath.absolute)) {
      return filepath.toString
    }

    val basepath = if (basedir.isEmpty) {
      new Urify(s"file://${cwd}", Some(""))
    } else {
      new Urify(basedir.get, Some(""))
    }

    if (!basepath.hierarchical) {
      throw XProcException.xdUrifyNonhierarchicalBase(filepath.toString, basepath.toString, None)
    }

    if (!basepath.absolute) {
      throw XProcException.xdUrifyFailed(filepath.toString, basepath.toString, None)
    }

    if (filepath.driveLetter.isDefined
      && (basepath.driveLetter.isEmpty || filepath.driveLetter.get != basepath.driveLetter.get)) {
      throw XProcException.xdUrifyDifferentDrives(filepath.toString, basepath.toString, None)
    }

    if ((filepath.driveLetter.isDefined && basepath.authority.isDefined)
      || (filepath.authority.isDefined && basepath.driveLetter.isDefined)) {
      throw XProcException.xdUrifyMixedDrivesAndAuthorities(filepath.toString, basepath.toString, None)
    }

    if ((filepath.scheme.isDefined && filepath.scheme.get != basepath.scheme.get)) {
      throw XProcException.xdUrifyDifferentSchemes(filepath.toString, basepath.toString, None)
    }

    if (basepath.scheme.isDefined && basepath.scheme.get != "file" && basepath.absolute) {
      return URI.create(basepath.toString).resolve(filepath.toString).toString
    }

    val rscheme = basepath.scheme.get
    val rauthority = if (filepath.authority.isDefined) {
      filepath.authority
    } else {
      basepath.authority
    }
    val rdrive = basepath.driveLetter
    val rpath = if (filepath.authority.isDefined || filepath.absolute) {
      filepath.fixedPath
    } else {
      basepath.resolvePaths(basepath.fixedPath, filepath.fixedPath)
    }

    val sb = new StringBuffer()
    sb.append(rscheme)
    sb.append(":")
    if (rauthority.isDefined) {
      sb.append("//")
      sb.append(rauthority.get)
    } else {
      if (basepath.scheme.isDefined && basepath.scheme.get == "file") {
        if (basepath.absolute) {
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
}

class Urify(filepath: String, basedir: Option[String]) {
  private var _scheme = Option.empty[String]
  private var _explicit = false
  private var _hierarchical = true
  private var _authority = Option.empty[String]
  private var _driveLetter = Option.empty[String]
  private var _path: String = _
  private var _absolute = false

  if (filesep != "/") {
    var fileuri = Option.empty[Boolean]
    filepath match {
      case Urify.OtherScheme(scheme, path) =>
        if (scheme == "file" || (windows && scheme.length == 1)) {
          fileuri = Some(true)
        } else {
          fileuri = Some(false)
        }
      case _ => ()
    }
    if (fileuri.isEmpty && (basedir.isEmpty || basedir.get == "")) {
      fileuri = Some(true)
    }
    if (fileuri.isEmpty && basedir.isDefined) {
      basedir.get match {
        case Urify.OtherScheme(scheme, path) =>
          if (scheme != "file") {
            fileuri = Some(false)
          }
        case _ => ()
      }
    }

    if (fileuri.isEmpty || fileuri.get) {
      _path = filepath.replace(filesep, "/")
    } else {
      _path = filepath
    }
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
          _hierarchical = _path.contains("/")
          _absolute = _hierarchical && _path.startsWith("/")
        }
      case Urify.Authority(fpauthority, fppath) =>
        _authority = Some(fpauthority)
        _path = Option(fppath).getOrElse("").replaceAll("^/+", "/")
        _absolute = _path.startsWith("/")
      case _ =>
        _path = _path.replaceAll("^/+", "/")
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
          _hierarchical = _path.contains("/")
          _absolute = _hierarchical && _path.startsWith("/")
        }
      case Urify.Authority(fpauthority, fppath) =>
        _authority = Some(fpauthority)
        _path = Option(fppath).getOrElse("").replaceAll("^/+", "/")
        _absolute = _path.startsWith("/")
      case _ =>
        _path = _path.replaceAll("^/+", "/")
        _absolute = _path.startsWith("/")
    }
  }

  def this (filestr: String) = {
    this(filestr, None)
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
      .replaceAll("\\\\", "%5C")
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
