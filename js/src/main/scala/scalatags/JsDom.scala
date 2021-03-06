package scalatags
import acyclic.file
import org.scalajs.dom
import scala.scalajs.js

import org.scalajs.dom.{Node, Element}
import scala.annotation.unchecked.uncheckedVariance
import scalatags.generic.{Aliases, Modifier}


/**
 * A Scalatags module that generates `dom.Element`s when the tags are rendered.
 * This provides some additional flexibility over the [[Text]] backend, as you
 * can bind structured objects to the attributes of your `dom.Element` without
 * serializing them first into strings.
 */
object JsDom
  extends generic.Bundle[dom.Element, dom.Element, dom.Node]
  with Aliases[dom.Element, dom.Element, dom.Node]{

  object attrs extends JsDom.Cap with Attrs
  object tags extends JsDom.Cap with jsdom.Tags
  object tags2 extends JsDom.Cap with jsdom.Tags2
  object styles extends JsDom.Cap with Styles
  object styles2 extends JsDom.Cap with Styles2
  object svgTags extends JsDom.Cap with jsdom.SvgTags
  object svgStyles extends JsDom.Cap with SvgStyles

  object implicits extends Aggregate

  object all
    extends Cap
    with Attrs
    with Styles
    with jsdom.Tags
    with DataConverters
    with Aggregate
    with LowPriorityImplicits

  object short
    extends Cap
    with Util
    with DataConverters
    with AbstractShort
    with Aggregate
    with LowPriorityImplicits{

    object * extends Cap with Attrs with Styles
  }


  trait Aggregate extends generic.Aggregate[dom.Element, dom.Element, dom.Node]{
    def genericAttr[T] = new JsDom.GenericAttr[T]
    def genericStyle[T] = new JsDom.GenericStyle[T]

    implicit def stringFrag(v: String) = new JsDom.StringFrag(v)


    val RawFrag = JsDom.RawFrag
    val StringFrag = JsDom.StringFrag
    type StringFrag = JsDom.StringFrag
    type RawFrag = JsDom.RawFrag
    def raw(s: String) = RawFrag(s)

    type HtmlTag = JsDom.TypedTag[dom.HTMLElement]
    val HtmlTag = JsDom.TypedTag
    type SvgTag = JsDom.TypedTag[dom.SVGElement]
    val SvgTag = JsDom.TypedTag
    type Tag = JsDom.TypedTag[dom.Element]
    val Tag = JsDom.TypedTag


  }

  trait Cap extends Util{ self =>
    type ConcreteHtmlTag[T <: dom.Element] = TypedTag[T]

    protected[this] implicit def stringAttrX = new GenericAttr[String]
    protected[this] implicit def stringStyleX = new GenericStyle[String]

    def makeAbstractTypedTag[T <: dom.Element](tag: String, void: Boolean): TypedTag[T] = {
      TypedTag(tag, Nil, void)
    }

    implicit class SeqFrag[A <% Frag](xs: Seq[A]) extends Frag{
      def applyTo(t: dom.Element): Unit = xs.foreach(_.applyTo(t))
      def render: dom.Node = {
        val frag = org.scalajs.dom.document.createDocumentFragment()
        xs.map(_.render).foreach(frag.appendChild)
        frag
      }
    }
  }

  object StringFrag extends Companion[StringFrag]
  case class StringFrag(v: String) extends jsdom.Frag{
    def render: dom.Text = dom.document.createTextNode(v)
  }

  object RawFrag extends Companion[RawFrag]
  case class RawFrag(v: String) extends Modifier{
    def applyTo(elem: dom.Element): Unit = {
      elem.insertAdjacentHTML("beforeend", v)
    }
  }

  class GenericAttr[T] extends AttrValue[T]{
    def apply(t: dom.Element, a: Attr, v: T): Unit = {
      t.setAttribute(a.name, v.toString)
    }
  }

  class GenericStyle[T] extends StyleValue[T]{
    def apply(t: dom.Element, s: Style, v: T): Unit = {
      t.asInstanceOf[dom.HTMLElement]
       .style
       .setProperty(s.cssName, v.toString)
    }
  }

  case class TypedTag[+Output <: dom.Element](tag: String = "",
                                              modifiers: List[Seq[Modifier]],
                                              void: Boolean = false)
                                              extends generic.TypedTag[dom.Element, Output, dom.Node]
                                              with jsdom.Frag{
    // unchecked because Scala 2.10.4 seems to not like this, even though
    // 2.11.1 works just fine. I trust that 2.11.1 is more correct than 2.10.4
    // and so just force this.
    protected[this] type Self = TypedTag[Output @uncheckedVariance]

    def render: Output = {
      val elem = dom.document.createElement(tag)
      build(elem)
      elem.asInstanceOf[Output]
    }
    /**
     * Trivial override, not strictly necessary, but it makes IntelliJ happy...
     */
    def apply(xs: Modifier*): TypedTag[Output] = {
      this.copy(tag = tag, void = void, modifiers = xs :: modifiers)
    }
    override def toString = render.outerHTML
  }
}

trait LowPriorityImplicits{
  implicit object bindJsAny extends generic.AttrValue[dom.Element, js.Any]{
    def apply(t: dom.Element, a: generic.Attr, v: js.Any): Unit = {
      t.asInstanceOf[js.Dynamic].updateDynamic(a.name)(v)
    }
  }
  implicit def bindJsAnyLike[T <% js.Any] = new generic.AttrValue[dom.Element, T]{
    def apply(t: dom.Element, a: generic.Attr, v: T): Unit = {
      t.asInstanceOf[js.Dynamic].updateDynamic(a.name)(v)
    }
  }
  implicit class bindNode(e: dom.Node) extends generic.Frag[dom.Element, dom.Node] {
    def applyTo(t: Element) = t.appendChild(e)
    def render = e
  }
}
