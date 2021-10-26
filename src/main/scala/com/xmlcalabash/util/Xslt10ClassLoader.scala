package com.xmlcalabash.util

import java.net.{URL, URLClassLoader}

// Special class loader to load Saxon 6
//
// Adapted from https://dzone.com/articles/java-classloader-handling
//
class Xslt10ClassLoader(classpath: Array[URL]) extends ClassLoader(Thread.currentThread().getContextClassLoader) {
  private val childClassLoader = new ChildClassLoader(classpath, new DetectClass(this.getParent))

  override protected def loadClass(name: String, resolve: Boolean): Class[_] = {
    try {
      childClassLoader.findClass(name)
    } catch {
      case _: ClassNotFoundException =>
        super.loadClass(name, resolve)
    }
  }

  private class ChildClassLoader(urls: Array[URL], parent: DetectClass) extends URLClassLoader(urls, null) {
    private val realParent = parent

    override def findClass(name: String): Class[_] = {
      try {
        val loaded = super.findLoadedClass(name)
        if (loaded == null) {
          super.findClass(name)
        } else {
          loaded
        }
      } catch {
        case _: ClassNotFoundException =>
          realParent.loadClass(name)
      }
    }
  }

  private class DetectClass(parent: ClassLoader) extends ClassLoader(parent) {
    override protected def findClass(name: String): Class[_] = {
      super.findClass(name)
    }
  }

}
