package com.evernym.agent.core_agent

import java.io.File
import java.net.{URL, URLClassLoader}

import com.evernym.agent.api._
import java.util.jar.Manifest


class DefaultExtensionFilter(config: ConfigProvider) extends ExtensionFilter {
  override def fileExtension: String = ".jar"
}

class ExtensionWrapper(val manifest: Manifest, val extClass: Class[_]) {

  def getSupportedMsgTypes: Set[MsgType] = {
    Set.empty
  }

  def createExtension(param: Option[ExtensionParam]=None): Extension = {
    extClass.getConstructor().newInstance().asInstanceOf[Extension]
  }
}

class DefaultExtensionManager(config: ConfigProvider) extends ExtensionManager {

  val MANIFEST_FILENAME = "EXT_MANIFEST.MF"

  var loaded: Map[String, ExtensionWrapper] = Map.empty

  def getExtWrapperReq(name: String): ExtensionWrapper = {
    loaded.getOrElse(name, throw new RuntimeException(s"extension $name not found"))
  }

  override def load(dirPaths: Set[String], filter: ExtensionFilter): Unit = {
    dirPaths.foreach { dirPath =>
      val folder = new File(dirPath)
      Option(folder.listFiles(filter)).map(_.toList).getOrElse(List.empty).foreach { extFile =>
        val classLoader = new URLClassLoader(Array(extFile.toURI.toURL), getClass.getClassLoader)
        val manifestResource: URL = classLoader.findResource(MANIFEST_FILENAME)
        val manifest: Manifest = new Manifest(manifestResource.openStream())
        val name: String = manifest.getMainAttributes.getValue("name")

        lazy val extClasspath: String = manifest.getMainAttributes.getValue("class")
        lazy val extClass = classLoader.loadClass(extClasspath)
        loaded += name -> new ExtensionWrapper(manifest, extClass)
      }
    }
  }

  override def getLoadedNames: Set[String] = loaded.keySet

  override def getSupportedMsgTypes(name: String): Set[MsgType] = {
    val extWrapper = getExtWrapperReq(name)
    extWrapper.getSupportedMsgTypes
  }

  override def createExtension(name: String, param: Option[ExtensionParam]=None): Extension = {
    val extWrapper = getExtWrapperReq(name)
    extWrapper.createExtension(param)
  }
}
