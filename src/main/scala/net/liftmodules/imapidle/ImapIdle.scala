/*
        Copyright 2011-2013 Spiral Arm Ltd

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

import net.liftweb.util._
import net.liftweb.common._

/**
 * Module configuration, used during Boot.boot.
 */
object ImapIdle extends Loggable {

	def init : Unit = { }

  /**
  * Start the IMAP IDLE module using the imap.idle.mail.user, imap.idle.mail.password and imap.idle.mail.host Props.
  * @param handler the function to execute for each email received.
  */
	def init(handler: MessageHandler) : Unit =  List("user", "password", "host") map { k => Props.get("imap.idle.mail."+k) } match {
		case Full(u) :: Full(p) :: Full(h) :: Nil => init(u,p,h) { handler }
    case _ => logger.warn("IMAP - feature not starting because of missing imap.idle props")
    }

  /**
   * Start the IMAP IDLE module using the given host and credentials.
   * @param username the IMAP server username to connect as. Often an email address.
   * @param password the IMAP server password associated with the given username.
   * @param host the IMAP server to connect to.
   * @param handler the function to execute for each email received.
   */
  def init(username: String, password: String, host: String)(handler: MessageHandler): Unit = {
    EmailReceiver ! Credentials(username, password, host)
    EmailReceiver ! Callback(handler)
    EmailReceiver ! 'startup
  }


}