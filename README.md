# IMAP IDLE Lift Module

Provides push-like email: your Lift web application can be notified when email arrives, via the [IMAP IDLE](http://en.wikipedia.org/wiki/IMAP_IDLE) feature.

## Using this module

There is currently no public repository containing this module:

    $ git clone git://github.com/d6y/liftmodules-imap-idle.git
    $ cd liftmodules-imap-idle
    $ sbt
    > update
    > publish-local

Once published, add the following dependency to your SBT project file:

	"net.liftweb.modules" %% "imap-idle" % "0.8"

Set your IMAP login credentials in your props file.  For example, add the following to src/main/resources/production.default.props

	# This mail account must have IMAP enable in your Google apps settings 
	imap.idle.mail.user=you@yourdomain.com
	imap.idle.mail.password=trustno1
	imap.idle.mail.host=imap.gmail.com

In your application's Boot.boot code:

	bootstrap.liftmodules.ImapIdle.init { m : javax.mail.Message => 
		println("You've got mail: "+net.liftweb.modules.imapidle.EmailUtils.dump(m))
		true // delete the email on the server
}

...which will dump the contents of the email to your console and then delete the mail.

If you're doing persistence in the Message => Boolean handler, ensure you initialize Record/Mapper before you ImapIdle.init because init will try to connect and start processing any emails that may be waiting. 

## Gotchas

 * For this code to work, the IMAP server must support the IDLE command.  Tested servers: Google mail.

 * Your email account must be set to enable IMAP access.  In Gmail this is Settings > Forwarding and POP/IMAP.

 * If you're deploying under Jetty 6.1.26 or earlier, you will need to replace $JETTY_HOME/lib/naming/mail-1.4.jar with mail-1.4.1.jar which
is the version that ships with Lift. Or... in some way resolve this w.r.t to [Jetty classloader](http://docs.codehaus.org/display/JETTY/Classloading.


## How this works

The module creates a Lift actor called EmailReceiver.  Set the actor up by sending Credentials, the Callback to execute of type Message => Boolean, and finally the 'startup message.   When email arrives, the callback is executed and if it returns true, the email is considered handled and deleted. 

## Debugging

If you want to see what javax.mail is doing, set mail.session.debug=true in your Lift Props file.

If you want to interact with EmailReceiver from the SBT console, here's how:

	sbt> console

	import net.liftweb.common._
	import net.liftweb.util._
	import net.liftweb.modules.imapidle._

	net.liftweb.util.LoggingAutoConfigurer()()

	EmailReceiver ! Credentials("me@mydomain", "letmein", "imap.gmail.com")
	EmailReceiver ! Callback(EmailUtils.noopHandler)
	EmailReceiver ! 'startup    
	                                 
	// Interact with EmailReceiver as you like.

	// E.g., perform a manual collection of mail, which is what you'll almost certainly
	// want to do at start up to catch up any mails you have missed:
	EmailReceiver ! 'collect

