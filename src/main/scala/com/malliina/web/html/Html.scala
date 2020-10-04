package com.malliina.web.html

import com.malliina.values.UnixPath
import com.malliina.web.AppService
import scalatags.Text.all._
import scalatags.text.Builder
import scalatags.Text

case class PageConf(title: String, bodyClass: String)

object Html {
  def apply(isProd: Boolean): Html = {
    val name = "client"
    val opt = if (isProd) "opt" else "fastopt"
    val scripts =
      if (isProd) List(s"$name-$opt.js")
      else List(s"$name-$opt-library.js", s"$name-$opt-loader.js", s"$name-$opt.js")
    new Html(scripts)
  }
}

class Html(scripts: Seq[String]) {
  val titleTag = tag("title")
  val Stylesheet = "stylesheet"
  val assets = AppService.assets
  implicit val unixPathAttr = attrValue[UnixPath](_.value)

  def attrValue[T](f: T => String): AttrValue[T] =
    (t: Builder, a: Text.Attr, v: T) => t.setAttr(a.name, Builder.GenericAttrValueSource(f(v)))

  def index = page(PageConf("Hello", "hello"))(p("Hi"))

  def page(conf: PageConf)(bodyContent: Modifier*) = TagPage(
    html(lang := "en")(
      head(
        meta(charset := "utf-8"),
        titleTag(conf.title),
        deviceWidthViewport,
        link(rel := "shortcut icon", `type` := "image/png", href := "/files/favicon.png"),
        cssLink(at("styles.css")),
        scripts.map { js =>
          deferredJsPath(js)
        }
      ),
      body(`class` := conf.bodyClass)(
        bodyContent
      )
    )
  )

  def deferredJsPath(path: String) = script(`type` := "application/javascript", src := at(path), defer)

  def at(ref: String) = {
    val file = assets.findAsset(UnixPath(ref)).getOrElse(UnixPath(ref))
    UnixPath(s"/assets/$file")
  }

  def deviceWidthViewport =
    meta(name := "viewport", content := "width=device-width, initial-scale=1.0")

  def cssLink[V: AttrValue](url: V, more: Modifier*) =
    link(rel := Stylesheet, href := url, more)
}
