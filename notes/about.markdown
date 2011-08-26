The [IMAP IDLE external Lift Module](https://github.com/d6y/liftmodules-imap-idle) provides push-like email facilities so your Lift web application can be notified when email arrives.

Use in your `Boot` like this:

    ImapIdle.init { m: javax.mail.Message => 
      println("You've got mail: "+EmailUtils.dump(m))
      true // delete the email on the server
    }
