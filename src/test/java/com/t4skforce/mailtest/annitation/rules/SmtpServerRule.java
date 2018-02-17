package com.t4skforce.mailtest.annitation.rules;


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.TimeLimitExceededException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.util.CollectionUtils;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.t4skforce.mailtest.annitation.SmtpServer;
import com.t4skforce.mailtest.annitation.wait.Body;
import com.t4skforce.mailtest.annitation.wait.Header;
import com.t4skforce.mailtest.annitation.wait.Wait;

public class SmtpServerRule implements TestRule {

	private SimpleSmtpServer dumbster;

	private SmtpServer serverAnnotation;
	
	private boolean wasGetReceivedEmailsCalled = false;

	@Override
	public Statement apply(Statement base, Description description) {
		SmtpServer annotation = description.getAnnotation(SmtpServer.class);
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				if (annotation != null) {
					try (SimpleSmtpServer srv = SimpleSmtpServer.start(annotation.port())) {
						Assert.assertNotNull(annotation.timeout());
						dumbster = srv;
						serverAnnotation = annotation;
						base.evaluate();
					} finally {
						try
						{
							if(!wasGetReceivedEmailsCalled)
							{
								getReceivedEmails();
							}
						} catch(TimeLimitExceededException ex) {
							Assert.fail(ex.getMessage());
						}
						if(dumbster != null)
						{
							dumbster.stop();
						}
					}
				} else {
					base.evaluate();
				}
			}
		};
	}

	/**
	 * @see com.dumbster.smtp.SimpleSmtpServer#reset()
	 */
	public void reset() {
		Assert.assertNotNull(dumbster);
		dumbster.reset();
	}

	/**
	 * @see com.dumbster.smtp.SimpleSmtpServer#getPort()
	 */
	public int getPort() {
		Assert.assertNotNull(dumbster);
		return dumbster.getPort();
	}

	/**
	 * Method used for checking delivered mails.
	 * 
	 * @return list of {@link com.dumbster.smtp.SmtpMessage}s received by since
	 *         start up or last reset.
	 * @throws TimeLimitExceededException 
	 * 			   is thrown if in given interval no valid message is being received
	 */
	public List<SmtpMessage> getReceivedEmails() throws TimeLimitExceededException{
		Assert.assertNotNull(dumbster);
		wasGetReceivedEmailsCalled = true;
		List<SmtpMessage> receivedMail = new ArrayList<>();
		long startTime = System.currentTimeMillis();
		
		List<Wait> invalid = new ArrayList<Wait>();
		if(serverAnnotation.waitFor().length > 0) 
		{
			invalid.addAll(Arrays.asList(serverAnnotation.waitFor()));
		}
		Set<SmtpMessage> processedMail = new HashSet<SmtpMessage>();
		do {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Assert.fail(e.getMessage());
			}
			receivedMail = dumbster.getReceivedEmails();
			if(receivedMail.size() >= serverAnnotation.count()) 
			{
				if(CollectionUtils.isEmpty(invalid)) 
				{
					return Collections.unmodifiableList(new ArrayList<>(receivedMail));
				}
				else
				{
					Set<SmtpMessage> toCheckMails = new HashSet<SmtpMessage>(receivedMail);
					toCheckMails.removeAll(processedMail);
					
					for(SmtpMessage mail : toCheckMails)
					{
						List<Wait> valid = new ArrayList<Wait>();
						for(Wait wait : invalid)
						{
							if(isValid(wait,mail)) {
								valid.add(wait);
							}
						}
						invalid.removeAll(valid);
						processedMail.addAll(receivedMail);
					}
				}
			} 
		} while (System.currentTimeMillis() - startTime < serverAnnotation.timeout());
		if(CollectionUtils.isEmpty(invalid)) {
			throw new TimeLimitExceededException(MessageFormat.format(serverAnnotation.message(), receivedMail.size(), serverAnnotation.count() ,(System.currentTimeMillis() - startTime)));
		} else {
			for(Wait wait : invalid){
				throw new TimeLimitExceededException(stringOf(wait));
			}
		}
		return receivedMail;
	}
	
	private String stringOf(Wait wait) {
		List<String> conditions = new ArrayList<>();
				
		Header[] headers = wait.headers();
		for(Header header : headers)
		{
			conditions.add(MessageFormat.format("@Header(key={0},value={1},regex={2},ignoreCase={3})", header.key(), header.value(), header.regex(), header.ignoreCase()));
		}	
		Body[] bodies = wait.body();
		for(Body body : bodies)
		{
			conditions.add(MessageFormat.format("@Body(value={0},regex={1},ignoreCase={2})", body.value(), body.regex(), body.ignoreCase()));
		}
		
		
		return MessageFormat.format(wait.message(), StringUtils.join(conditions.toArray(),','));
	}
	

	private boolean isValid(Wait wait,SmtpMessage mail)
	{
		Header[] headers = wait.headers();
		if(!areValid(headers,mail)) {
			return false;
		}
		
		Body[] bodies = wait.body();
		if(!areValid(bodies,mail))
		{
			return false;
		}
		
		return true;
	}
	
	private boolean areValid(Header[] headers, SmtpMessage mail) {
		for(Header header : headers)
		{
			if(!isValid(header,mail))
			{
				return false;
			}
		}
		return true;
	}
	
	private boolean isValid(Header header, SmtpMessage mail) {
		if(isValidHeader(header.key(),header.value(),header.regex(),header.ignoreCase(), mail)) {
			return true;
		}
		return false;
	}

	private boolean isValidHeader(String key, String value, boolean regex, boolean ignoreCase, SmtpMessage mail) {
		if(mail.getHeaderNames().contains(key))
		{
			String mailValue = mail.getHeaderValue(key);
			String checkValue = value;
			if(ignoreCase) {
				mailValue = mailValue.toLowerCase();
				checkValue = checkValue.toLowerCase();
			}
			if(regex && mailValue.matches(checkValue)) {
				return true;
			} else if(checkValue.equals(mailValue)) {
				return true;
			}
		}
		return false;
	}

	private boolean areValid(Body[] bodies, SmtpMessage mail) {
		for(Body body : bodies)
		{
			if(!isValid(body,mail))
			{
				return false;
			}
		}
		return true;
	}

	private boolean isValid(Body body, SmtpMessage mail) {
		String mailValue = mail.getBody();
		String checkValue = body.value();
		if(body.ignoreCase()) {
			mailValue = mailValue.toLowerCase();
			checkValue = checkValue.toLowerCase();
		}
		if(body.regex() && mailValue.matches(checkValue)) {
			return true;
		} else if(checkValue.equals(mailValue)) {
			return true;
		}
		return false;
	}

}
