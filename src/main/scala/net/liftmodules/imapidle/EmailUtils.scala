/*
        Copyright 2011 Spiral Arm Ltd

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.package bootstrap.liftmodules
*/
package net.liftmodules.imapidle

import net.liftweb.actor._
import net.liftweb.common._

import javax.mail._
import javax.mail.event._
import javax.mail.internet._
import com.sun.mail.imap._

object EmailUtils extends Loggable {

  implicit def multipartHelper(m: MimeMultipart) = new {
    def bodyParts = for (i <- 0 until m.getCount) yield m.getBodyPart(i)
  }

  def asString(m: Message) = (decodeSubject(m) + " " + decodeBody(m)) trim

  private def decodeSubject(m: Message): String = Box !! m.getSubject openOr ("")
  
  private def decodeBody(m: Part): String = m.getContent match {
    case c: String if m.isMimeType("text/plain") => c
    case p: MimeMultipart => p.bodyParts map { decodeBody } mkString " "
    case x =>
      logger.warn("IMAP Don't know how to extract text from a " + x.getClass + " " + m.getContentType + ": " + x)
      ""
    }
  
  def to(m: Message) = m.getAllRecipients map { _.toString } mkString (",")

  def address(address: Address) = address match {
    case a: InternetAddress => a.getAddress
    case _ => address.toString
  }

  def localpart(a: Address) = address(a) split ("@") head

  def dump(m: Message) = "Recipients=" + to(m) + " Body=" + asString(m)

  def noopHandler(m: Message) = {
	logger.info(dump(m))
	false
  }

}