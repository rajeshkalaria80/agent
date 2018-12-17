package com.evernym.agent.core_agent

import java.io.File
import java.net.{URL, URLClassLoader}

import com.evernym.agent.api._
import java.util.jar.Manifest


class DefaultExtensionFilter(config: ConfigProvider) extends ExtensionFilter {
  override def fileExtension: String = ".jar"
}


class DefaultExtensionManager(config: ConfigProvider) extends ExtensionManager {

  val MANIFEST_FILENAME = "EXT_MANIFEST.MF"

  var loaded: Map[String, ExtensionWrapper] = Map.empty

  override def load(dirPaths: Set[String], filter: ExtensionFilter): Unit = {
    dirPaths.foreach { dirPath =>
      val folder = new File(dirPath)
      Option(folder.listFiles(filter)).map(_.toList).getOrElse(List.empty).foreach { extFile =>
        val classLoader = new URLClassLoader(Array(extFile.toURI.toURL), getClass.getClassLoader)
        val manifestResource: URL = classLoader.findResource(MANIFEST_FILENAME)
        val manifest: Manifest = new Manifest(manifestResource.openStream())
        val name: String = manifest.getMainAttributes.getValue("name")

        lazy val extWrapperClasspath: String = manifest.getMainAttributes.getValue("wrapper-class")
        lazy val extWrapper = classLoader.loadClass(extWrapperClasspath).newInstance().asInstanceOf[ExtensionWrapper]
        loaded += name -> extWrapper
      }
    }
  }

  override def getExtWrappers: Map[String, ExtensionWrapper] = loaded

}
