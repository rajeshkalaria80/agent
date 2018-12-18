package com.evernym.agent.core_agent

import java.io.File
import java.net.{URL, URLClassLoader}

import com.evernym.agent.api._
import java.util.jar.Manifest


class DefaultExtensionFilter(configProvider: ConfigProvider) extends ExtensionFilter {
  override def fileExtension: String = ".jar"
}

class DefaultExtensionManager(configProvider: ConfigProvider)
  extends ExtensionManager {

  val MANIFEST_FILENAME = "EXT_MANIFEST.MF"

  var loaded: Map[String, Extension] = Map.empty

//  def addJarFileToClassPath(path: String): Unit = {
//    val urlPath: String = "jar:file://" + path + "!/"
//    addURL(new URL(urlPath))
//  }

  override def load(dirPathsOpt: Option[Set[String]] = None, filterOpt: Option[ExtensionFilter]): Unit = {
    val dirPaths = dirPathsOpt.getOrElse(configProvider.getStringSet("agent.extensions.load-paths"))
    val filter = filterOpt.getOrElse(new DefaultExtensionFilter(configProvider))
    val classLoader = getClass.getClassLoader

    dirPaths.foreach { dirPath =>
      val folder = new File(dirPath)
      Option(folder.listFiles(filter)).map(_.toList).getOrElse(List.empty).foreach { extFile =>
        val urlClassLoader = new URLClassLoader(Array(extFile.toURI.toURL), classLoader)

        val manifestResource: URL = urlClassLoader.findResource(MANIFEST_FILENAME)
        val manifest: Manifest = new Manifest(manifestResource.openStream())
        val extName = manifest.getMainAttributes.getValue("name")

        val extWrapperClasspath = manifest.getMainAttributes.getValue("ext-class")
        //TODO: need to remove a deprecated warning in below line
        val extWrapper = urlClassLoader.loadClass(extWrapperClasspath).newInstance().asInstanceOf[Extension]
        loaded += extName -> extWrapper

        //addJarFileToClassPath(extFile.getAbsolutePath)

      }
    }
  }

  override def getExtensions: Map[String, Extension] = loaded

}
