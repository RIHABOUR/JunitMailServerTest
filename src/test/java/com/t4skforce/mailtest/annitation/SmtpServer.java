package com.t4skforce.mailtest.annitation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.dumbster.smtp.SimpleSmtpServer;
import com.t4skforce.mailtest.annitation.wait.Wait;

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
public @interface SmtpServer {
	
	/**
	 * port to use for fake smtp server
	 * @return port to use for fake smtp server
	 */
	public int port() default SimpleSmtpServer.AUTO_SMTP_PORT;
	
	/**
	 * Maximum time in which given conditions from {@link com.t4skforce.mailtest.annitation.SmtpServer#wait()} need to be met
	 * @return Maximum time in which given conditions need to be met
	 */
	public long timeout() default 60000L;
	
	/**
	 * A list if conditions the method {@link com.t4skforce.mailtest.annitation.rules.SmtpServerRule#getReceivedEmails} should wait for before returning the values
	 * @return list of conditions to wait for
	 */
	public Wait[] waitFor() default { };
	
	/**
	 * Minimum count of mails to receive before {@link com.t4skforce.mailtest.annitation.rules.SmtpServerRule#getReceivedEmails} returns without error 
	 * @return Minimum count of mails to receive
	 */
	int count() default 1;
	
	/**
	 * used for formating exception message
	 * @return formatted string for message
	 */
	String message() default "Mail count {0} of expected {1} not received within {2}ms";
}
