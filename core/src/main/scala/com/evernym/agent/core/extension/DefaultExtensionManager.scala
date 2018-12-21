package com.evernym.agent.core.extension

import java.io.{File, FilenameFilter}
import java.net.{URL, URLClassLoader}
import java.util.jar.Manifest

import com.evernym.agent.common.Constants._
import com.evernym.agent.api._


class DefaultExtFileFilter(configProvider: ConfigProvider) extends ExtFileFilterCriteria with FilenameFilter {

  def fileExtension: String = ".jar"

  override def accept(dir: File, name: String): Boolean = {
    name.endsWith(s"$fileExtension")
  }

  override def filteredFiles(folderPath: String): Set[File] = {
    val folder = new File(folderPath)
    folder.listFiles(this).toSet
  }
}

class ExtensionState(status: String, extension: Extension)


class DefaultExtensionManager(configProvider: ConfigProvider)
  extends ExtensionManager {

  var loaded: Set[ExtensionDetail] = Set.empty

  var errors: Set[StatusDetail] = Set.empty

  override def getSuccessfullyLoaded: Set[ExtensionDetail] = loaded

  override def getErrors: Set[StatusDetail] = errors

  private def addToErrors(errorMsg: String): Unit = {
    errors += StatusDetail(EXT_LOADING_FAILED, Option(errorMsg))
  }

  override def load(dirPathsOpt: Option[Set[String]] = None, filterOpt: Option[ExtFileFilterCriteria]): Unit = {
    val dirPaths = dirPathsOpt.getOrElse(configProvider.getStringSet("agent.extensions.load-paths"))
    val filter = filterOpt.getOrElse(new DefaultExtFileFilter(configProvider))
    val classLoader = getClass.getClassLoader

    dirPaths.foreach { dirPath =>
      try {
        filter.filteredFiles(dirPath).foreach { extFile =>
          try {
            val urlClassLoader = new URLClassLoader(Array(extFile.toURI.toURL), classLoader)
            Option(urlClassLoader.findResource(EXT_MANIFEST_FILENAME)) match {
              case Some(mr: URL) =>
                val manifest: Manifest = new Manifest(mr.openStream())
                val extWrapperClasspath = manifest.getMainAttributes.getValue("ext-class")
                //TODO: need to remove the deprecated warning in below line
                val extWrapper = urlClassLoader.loadClass(extWrapperClasspath).newInstance().asInstanceOf[Extension]
                getExtOption(extWrapper.name) match {
                  case Some(ed: ExtensionDetail) =>
                    addToErrors(s"file: ${extFile.getAbsolutePath}, detail: extension name " +
                      s"'${extWrapper.name}' already used by extension loaded from file '${ed.fileAbsolutePath}'")
                  case None =>
                    loaded += ExtensionDetail(extWrapper, extFile.getAbsolutePath)
                }
              case None =>
                addToErrors(s"file: ${extFile.getAbsolutePath}, detail: manifest file '$EXT_MANIFEST_FILENAME' not found")
            }
          } catch {
            case e: Throwable =>
              addToErrors(s"file: ${extFile.getAbsolutePath}, detail: ${e.getMessage}")
          }
        }
      } catch {
        case e: Throwable =>
          addToErrors(s"directory: $dirPath, detail: ${e.getMessage}")

      }
    }
  }

}
