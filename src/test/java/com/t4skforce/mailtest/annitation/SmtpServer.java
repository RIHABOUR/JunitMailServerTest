package com.t4skforce.mailtest.annitation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.dumbster.smtp.SimpleSmtpServer;

@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface SmtpServer {
	
	public int port() default SimpleSmtpServer.AUTO_SMTP_PORT;
	
	public long timeout() default 60000L;
}
