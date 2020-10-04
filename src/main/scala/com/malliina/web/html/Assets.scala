package com.malliina.web.html

import java.nio.file.{FileVisitOption, Files, Path, Paths}

import com.malliina.values.UnixPath
import com.malliina.web.Errors
import com.malliina.web.html.Assets.log
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters.IteratorHasAsScala

object Assets {
  private val log = LoggerFactory.getLogger(getClass)

  val default = apply(Paths.get("assets"))

  def apply(root: Path): Assets = new Assets(root)
}

class Assets(root: Path) {
  val rootPath = UnixPath(root)

  def find(path: Path): Either[Errors, UnixPath] = findAsset(UnixPath(path))

  /** Finds any fingerprinted version of `file`.
    *
    * @param file
    * @return a possibly fingerprinted path to `file`; defaults to `file` if no fingerprinted asset is found
    */
  def findAsset(file: UnixPath): Either[Errors, UnixPath] = {
    log.info(s"Searching '$file' from '$rootPath'...")
    val path = root.resolve(file.value)
    val dir = path.getParent
    // recursive
    val candidates = Files.walk(dir).iterator().asScala.toList
    val lastSlash = file.value.lastIndexOf("/")
    val nameStart = if (lastSlash == -1) 0 else lastSlash + 1
    val name = file.value.substring(nameStart)
    val dotIdx = name.lastIndexOf(".")
    val noExt = name.substring(0, dotIdx)
    val ext = name.substring(dotIdx + 1)
    val result = candidates
      .filter { p =>
        val candidateName = p.getFileName.toString
        candidateName.startsWith(noExt) && candidateName.endsWith(ext)
      }
      .sortBy { p =>
        Files.getLastModifiedTime(p)
      }
      .reverse
      .headOption
    result
      .map(p => UnixPath(root.relativize(p)))
      .toRight(Errors(s"Not found: '$file'. Found ${candidates.mkString(", ")}."))
  }
}
