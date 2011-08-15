The [IMAP IDLE external Lift Module](https://github.com/d6y/liftmodules-imap-idle) provides push-like email facilities. 

Your Lift web application can be notified when email arrives, via the [IMAP IDLE](http://en.wikipedia.org/wiki/IMAP_IDLE) feature.

Use in in your `Boot` like this:

    ImapIdle.init { m : javax.mail.Message => 
      println("You've got mail: "+EmailUtils.dump(m))
      true // delete the email on the server
    }




