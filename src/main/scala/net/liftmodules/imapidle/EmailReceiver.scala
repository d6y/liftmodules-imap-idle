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

import javax.mail._
import javax.mail.event._
import com.sun.mail.imap._

import java.util.Properties

import net.liftweb.actor._
import net.liftweb.common._
import net.liftweb.util._
import net.liftweb.util.Helpers._

import org.joda.time._

import scala.language.postfixOps, scala.language.implicitConversions

case class Credentials(username: String, password: String, host: String = "imap.gmail.com")

case class Callback(h: MessageHandler)

/**
 * Actor which manages the connection to the IMAP server
 * and dispatches javax.mail.Messages to the user function
 * given during `ImapIdle.init`.
 * This actor is started automatically during init.
 */
object EmailReceiver extends LiftActor with Loggable {

  private var inbox: Box[Folder] = Empty
  private var store: Box[Store] = Empty

  private var credentials: Box[Credentials] = Empty
  private var callback: Box[MessageHandler] = Empty

  // To be able to remove listeners during a clean up, we need to keep a list of them.
  private var listeners: List[java.util.EventListener] = Nil

  // If the connection to the IMAP server goes away, we manually disconnect (reap) idle connections that are older than 30 minutes
  private var idleEnteredAt: Box[DateTime] = Empty

  // Idle the connection. "Idle" in the sense of "run slowly while disconnected from a load or out of gear" perhaps. RFC2177
  private def idle {

    idleEnteredAt = Full(new DateTime)

    // IMAPFolder.idle() blocks until the server has an event for us, so we call this in a separate thread.
    def safeIdle(f: IMAPFolder) : Unit = Schedule { () =>
      try {
        logger.debug("IMAP Actor idle block entered")
        f.idle
        logger.debug("IMAP Actor idle block exited")
      } catch { // If the idle fails, we want to restart the connection because we will no longer be waiting for messages.
        case x : Throwable=>
          logger.warn("IMAP Attempt to idle produced " + x)
          EmailReceiver ! 'restart
      }
      idleEnteredAt = Empty
    }

    inbox match {
      case Full(f: IMAPFolder) => safeIdle(f)
      case x => logger.error("IMAP Can't idle " + x)
    }
  }

  // Connect to the IMAP server and pipe all events to this actor
  private def connect = {

    logger.debug("IMAP Connecting")
    require(credentials.isEmpty == false)
    require(callback.isEmpty == false)

    val props = new Properties
    props.put("mail.store.protocol", "imaps")
    props.put("mail.imap.enableimapevents", "true")

    val session = Session.getDefaultInstance(props)

    session.setDebug(Props.getBool("mail.session.debug", true))

    val store = session.getStore()

    val connectionListener = new ConnectionListener {
      def opened(e: ConnectionEvent): Unit = EmailReceiver ! e
      def closed(e: ConnectionEvent): Unit = EmailReceiver ! e
      def disconnected(e: ConnectionEvent): Unit = EmailReceiver ! e
    }
    listeners = connectionListener :: Nil
    store.addConnectionListener(connectionListener)

    // We may be able to live without store event listeners as they only seem to be notices.
    val storeListener = new StoreListener {
      def notification(e: StoreEvent): Unit = EmailReceiver ! e
    }
    listeners = storeListener :: listeners
    store.addStoreListener(storeListener)

    credentials foreach { c =>
    	store.connect(c.host, c.username, c.password)

    	val inbox = store.getFolder("INBOX")
      if (inbox.exists)
        inbox.open(Folder.READ_WRITE)
      else
        logger.error("IMAP - folder INBOX not found. Carrying on in case it reappears")

    	val countListener = new MessageCountAdapter {
      		override def messagesAdded(e: MessageCountEvent): Unit = EmailReceiver ! e
    	}
    	inbox.addMessageCountListener(countListener)
    	listeners = countListener :: listeners

    	logger.info("IMAP Connected, listeners ready")

    	this.inbox = Box !! inbox
    	this.store = Box !! store
    }
  }

  private def disconnect {

    logger.debug("IMAP Disconnecting")

    // We un-bind the listeners before closing to prevent us receiving disconnect messages
    // which... would make us want to reconnect again.

    inbox foreach { i =>
      listeners foreach {
        case mcl: MessageCountListener => i.removeMessageCountListener(mcl)
        case _ =>
      }
      if (i.isOpen) i.close(true)
    }

    store foreach { s =>
      listeners foreach {
        case cl: ConnectionListener => s.removeConnectionListener(cl)
        case sl: StoreListener => s.removeStoreListener(sl)
        case _ =>
      }
      if (s.isConnected) s.close()
    }

    inbox = Empty
    store = Empty
    listeners = Nil

  }


  private def retry(f: => Unit): Unit = try {
    f
  } catch {
    case e : Throwable =>
      logger.warn("IMAP Retry failed - will retry: "+e.getMessage)
      Thread.sleep(1 minute)
      retry(f)
  }

  private def reconnect {
    disconnect
    Thread.sleep(15 seconds)
    connect
  }

  private def processEmail(messages: Array[Message]) {

    for (m <- messages; c <- callback if c(m)) {
		 m.setFlag(Flags.Flag.DELETED, true)
    }

	  inbox foreach { _.expunge }
  }

  // Useful for debugging from the console:
  //def getInbox = inbox

  def messageHandler = {

    case c: Credentials => credentials = Full(c)

    case Callback(h) => callback = Full(h)

    case 'startup =>
      connect
      EmailReceiver ! 'collect
      EmailReceiver ! 'idle
      EmailReceiver ! 'reap

    case 'idle => idle

    case 'shutdown => disconnect

    case 'restart =>
      logger.info("IMAP Restart request received")
      retry {
        reconnect
      }
      // manual collection in case we missed any notifications during restart or during error handling
      EmailReceiver ! 'collect
      EmailReceiver ! 'idle

    case 'collect =>
      logger.info("IMAP Manually checking inbox")
      inbox map { _.getMessages } foreach { msgs => processEmail(msgs) }

    case e: MessageCountEvent if !e.isRemoved =>
      logger.info("IMAP Messages available")
      processEmail(e.getMessages)
      EmailReceiver ! 'idle

    case 'reap =>
      logger.debug("IMAP Reaping old IDLE connections")
      Schedule.schedule(this, 'reap, 1 minute)
      for {
        when <- idleEnteredAt
        dur = new Duration(when, new DateTime)
        if dur.getMillis() > (30 minutes)
      } EmailReceiver ! 'restart

    case e: StoreEvent => logger.warn("IMAP Store event reported: " + e.getMessage)

    case e: ConnectionEvent if e.getType == ConnectionEvent.OPENED => logger.debug("IMAP Connection opened")
    case e: ConnectionEvent if e.getType == ConnectionEvent.DISCONNECTED => logger.warn("IMAP Connection disconnected")

    case e: ConnectionEvent if e.getType == ConnectionEvent.CLOSED =>
      logger.info("IMAP Connection closed - reconnecting")
      reconnect

    case e => logger.warn("IMAP Unhandled email event: "+e)
  }
}
