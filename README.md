# IMAP IDLE Lift Module

Provides push-like email: your Lift web application can be notified when email arrives, via the [IMAP IDLE](http://en.wikipedia.org/wiki/IMAP_IDLE) feature.

## Using this module

1. Add the following repository to your SBT project file:

  For SBT 0.10:

        resolvers += "liftmodules repository" at "http://repository-liftmodules.forge.cloudbees.com/release/"

  For SBT 0.7:

        lazy val liftModulesRelease = "liftmodules repository" at "http://repository-liftmodules.forge.cloudbees.com/release/"

2. Add the following dependency to your SBT project file:

        "net.liftmodules" %% "imap-idle" % (liftVersion+"-0.9.1")

3. Set your IMAP login credentials in your props file, e.g.:

        $ cat src/main/resources/production.default.props 
        imap.idle.mail.user=you@yourdomain.com
        imap.idle.mail.password=trustno1
        imap.idle.mail.host=imap.gmail.com

4. In your application's Boot.boot code:
    
        import net.liftmodules.imapidle._
        ImapIdle.init { m : javax.mail.Message => 
           println("You've got mail: "+EmailUtils.dump(m))
           true // delete the email on the server
        }

...which will dump the contents of the email to your console and then delete the mail.

If you're doing persistence in the `Message => Boolean` handler, ensure you initialize Record/Mapper before you `ImapIdle.init` because init will try to connect and start processing any emails that may be waiting. 

## Example Scenario

Using this module you can provide a way for users to email in content via an [address tag](http://en.wikipedia.org/wiki/Email_address#Address_tags).  Here's how... listen for messages at a single email address, such as account@example.com. Users email content into account+UNIQUE_ID@example.com.  In your handler, extract the recipient from the message, parse out the UNIQUE_UD. Finally, do whatever you need to do with the content based on the UNIQUE_ID.  

## Gotchas

 * For this module to work, the IMAP server must support the IDLE command.  Tested servers: Google mail (imap.google.com).

 * Your email account must be set to enable IMAP access.  In Gmail this is Settings > Forwarding and POP/IMAP.

 * If you're deploying under Jetty 6.1.26 or earlier, you will need to replace $JETTY_HOME/lib/naming/mail-1.4.jar with mail-1.4.1.jar or later (1.4.4 is the version that ships with Lift). Or... in some way resolve this w.r.t to [Jetty classloader](http://docs.codehaus.org/display/JETTY/Classloading).

 * This code depends on a `com.sun` class in `EmailUtils`, meaning it may not work under non-Sun derived JDKs.

 * If your application is partitioned from the IMAP server it may take 30 minutes for the IMAP IDLE code to detect this and re-establish a connection to collect messages. 


## How this works

The module creates a Lift actor called `EmailReceiver`.  Set the actor up by sending `Credentials`, the `Callback` to execute of type `Message => Boolean`, and finally the `'startup` message.   When email arrives, the callback is executed and if it returns true, the email is considered handled and deleted. 

## Debugging

If you want to see what javax.mail is doing, set `mail.session.debug=true` in your Lift Props file.

If you want to interact with EmailReceiver from the SBT console, here's how:

	sbt> console

	import net.liftweb.common._
	import net.liftweb.util._
	import net.liftmodules.imapidle._

	net.liftweb.util.LoggingAutoConfigurer()()

	EmailReceiver ! Credentials("me@mydomain", "letmein", "imap.gmail.com")
	EmailReceiver ! Callback(EmailUtils.noopHandler)
	EmailReceiver ! 'startup    
	                                 
	// Interact with EmailReceiver as you like. E.g., perform a manual collection of mail:
	EmailReceiver ! 'collect


## To build from source:

    $ git clone git://github.com/d6y/liftmodules-imap-idle.git
    $ cd liftmodules-imap-idle
    $ ./sbt
    > update
    > publish-local

## To publish to Build@CloudBees:

1. Copy `cloudbees.credentials.template` to create `cloudbees.credentials` and modify the file to include your CloudBees username and password.

2. Manually create the directory structure in the CloudBees repo.  E.g., mount https://repository-liftmodules.forge.cloudbees.com/release/ and `mkdir /Volumes/release/net/liftmodules/imap-idle_2.8.1/2.4-M3-0.91` (if you publishing the 2.3-M3-0.91 version)

3. Run `./sbt` and `+publish`

