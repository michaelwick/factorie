/* Copyright (C) 2008-2010 University of Massachusetts Amherst,
   Department of Computer Science.
   This file is part of "FACTORIE" (Factor graphs, Imperative, Extensible)
   http://factorie.cs.umass.edu, http://code.google.com/p/factorie/
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package cc.factorie

/** Provides member "attr" which is a map from class to an attribute value (instance of that class).
    For example: object foo extends Attr; foo.attr += "bar"; require(foo.attr[String] == "bar"); foo.attr.remove[String] */
trait AttrOld {
  object attr extends scala.collection.mutable.ListMap[Class[_],AnyRef] {
    def apply[C<:AnyRef]()(implicit m: Manifest[C]): C = this(m.erasure).asInstanceOf[C]
    def get[C<:AnyRef](implicit m: Manifest[C]): Option[C] = this.get(m.erasure).asInstanceOf[Option[C]]
    def getOrElse[C<:AnyRef](defaultValue:C)(implicit m: Manifest[C]): C = super.getOrElse(m.erasure, defaultValue).asInstanceOf[C]
    def getOrElseUpdate[C<:AnyRef](defaultValue:C)(implicit m: Manifest[C]): C = super.getOrElseUpdate(m.erasure, defaultValue).asInstanceOf[C]
    def +=[C<:AnyRef](value:C): C = { this(value.getClass) = value; value }
    def -=[C<:AnyRef](value:C): C = { super.-=(value.getClass); value }
    def remove[C<:AnyRef](implicit m: Manifest[C]): Unit = super.-=(m.erasure)
  }
}

/** Provides member "attr" which is a map from class to an attribute value (instance of that class).
    For example: object foo extends Attr; foo.attr += "bar"; require(foo.attr[String] == "bar"); foo.attr.remove[String] */
trait Attr {
  /** A collection of attributes, keyed by the attribute class. */
  object attr {
    private var _attr: Array[AnyRef] = new Array[AnyRef](2)
    /** The number of attributes present. */
    def length: Int = { var i = 0; while ((i < _attr.length) && (_attr(i) ne null)) i += 1; i }
    def capacity: Int = _attr.length
    // Methods that manipulate _attr
    private def setCapacity(cap:Int): Unit = { val ta = new Array[AnyRef](cap); System.arraycopy(_attr, 0, ta, 0, math.min(cap, math.min(cap, _attr.length))); ta }
    /** Make sure there is capacity of at least "cap"  */
    def ensureCapacity(cap:Int): Unit = if (cap > _attr.length) { val ta = new Array[AnyRef](cap); System.arraycopy(_attr, 0, ta, 0, _attr.length); ta }
    /** Increase capacity by "incr". */
    def increaseCapacity(incr:Int): Unit = { val ta = new Array[AnyRef](_attr.length+incr); System.arraycopy(_attr, 0, ta, 0, _attr.length); _attr = ta }
    def removeIndex(i:Int): Unit = {
      val len = length
      if (i == len - 1) _attr(i) = null
      else {
        System.arraycopy(_attr, i+1, _attr, i, len-i-1)
        _attr(len-1) = null
      }
    }
    /** Re-allocate to remove any unused capacity */
    def trimCapacity: Unit = { val l = length; if (l < _attr.length) setCapacity(l) }
    // Methods that search through _attr
    /** Add the given attribute, with key equal to its class. */
    def +=[C<:AnyRef](value:C): C = {
      var i = 0
      val key = value.getClass
      while (i < _attr.length && (_attr(i) ne null) && _attr(i).getClass != key)
        i += 1
      if (i == _attr.length)
        increaseCapacity(1)
      _attr(i) = value
      value
    }
    /** Returns the index of the first attribute with class matching the argument.
        Attributes occur in the order in which they were inserted.
        Note this means you can add a:MyClass, then add b:MySubclass, then index[MyClass] will return the index of a. */
    @inline final def index(key:Class[_]): Int = {
      var i = 0
      while (i < _attr.length) {
        if ((_attr(i) ne null) && key.isAssignableFrom(_attr(i).getClass))
          return i
        i += 1
      }
      -1
    }
    def all[C<:AnyRef]()(implicit m: Manifest[C]): Seq[C] = {
      val key = m.erasure
      val result = new scala.collection.mutable.ArrayBuffer[C]
      var i = 0 
      while (i < _attr.length) {
        if ((_attr(i) ne null) && key.isAssignableFrom(_attr(i).getClass)) result += _attr(i).asInstanceOf[C]
        i += 1
      }
      result
    }
    /** Remove all attributes with class matching or subclass of C.
        For example, to remove all attributes call remove[AnyRef].
        If call results in no removals, will not throw an Error. */
    def remove[C<:AnyRef](implicit m: Manifest[C]): Unit = {
      val key = m.erasure
      val result = new scala.collection.mutable.ArrayBuffer[C]
      var i = 0 
      while (i < _attr.length) {
        if ((_attr(i) ne null) && key.isAssignableFrom(_attr(i).getClass)) removeIndex(i)
        else i += 1
      }
    }
    def values: Seq[AnyRef] = {
      val result = new scala.collection.mutable.ArrayBuffer[AnyRef]
      var i = 0 
      while (i < _attr.length) {
        if ((_attr(i) ne null)) result += _attr(i)
        i += 1
      }
      result
    }
    @inline final def index[C<:AnyRef]()(implicit m: Manifest[C]): Int = index(m.erasure)
    def contains[C<:AnyRef]()(implicit m: Manifest[C]): Boolean = index(m.erasure) >= 0
    def contains(key:Class[_]): Boolean = index(key) >= 0
    /** Fetch the first value associated with the given class.  If none present, return null. */
    def apply[C<:AnyRef]()(implicit m: Manifest[C]): C = {
      var i = index[C]
      if (i >= 0) _attr(i).asInstanceOf[C] else null.asInstanceOf[C]
    }
    def get[C<:AnyRef](implicit m: Manifest[C]): Option[C] = {
      val result = this.apply[C]
      if (result ne null) Option(result) else None
    }
    def getOrElse[C<:AnyRef](defaultValue:C)(implicit m: Manifest[C]): C = {
      val result = this.apply[C]
      if (result ne null) result else defaultValue
    }
    def getOrElseUpdate[C<:AnyRef](defaultValue: =>C)(implicit m: Manifest[C]): C = {
      val result = this.apply[C]
      if (result ne null) result else {
        val value = defaultValue
        this += value
        value
      }
    }
  }
}