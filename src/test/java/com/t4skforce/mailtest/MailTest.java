package com.t4skforce.mailtest;

import java.util.List;
import java.util.Properties;

import javax.naming.TimeLimitExceededException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is; 
import com.dumbster.smtp.SmtpMessage;
import com.t4skforce.mailtest.annitation.SmtpServer;
import com.t4skforce.mailtest.annitation.wait.Header;
import com.t4skforce.mailtest.annitation.wait.WaitFor;

import static org.junit.Assert.assertThat;

public class MailTest extends AbstractBaseTestClass {

	private JavaMailSenderImpl mailSender;
	
	@Before
	public void setUp()
	{
		mailSender = new JavaMailSenderImpl();
		mailSender.setHost("localhost");
		mailSender.setPort(smtpserver.getPort());
		mailSender.setUsername("my.gmail@gmail.com");
		mailSender.setPassword("password");
		Properties props = mailSender.getJavaMailProperties();
	    props.put("mail.transport.protocol", "smtp");
	    props.put("mail.smtp.auth", "true");
	    props.put("mail.smtp.starttls.enable", "true");
	    props.put("mail.debug", "true");
	}
	
	@Test
	@SmtpServer(timeout=60000)
	public void testSendReceiveMail() throws Exception
	{
		sendMessage("sender@here.com", "Test", "Test Body", "receiver@there.com");
		List<SmtpMessage> emails = smtpserver.getReceivedEmails();
        assertThat(emails, hasSize(1));
        SmtpMessage email = emails.get(0);
        assertThat(email.getHeaderValue("From"), is("sender@here.com"));
        assertThat(email.getHeaderValue("Subject"), is("Test"));
        assertThat(email.getBody(), is("Test Body"));
        assertThat(email.getHeaderValue("To"), is("receiver@there.com"));
	}
	
	@Test(expected=TimeLimitExceededException.class)
	@SmtpServer(timeout=1000)
	public void testSendReceiveMailException() throws Exception
	{
		List<SmtpMessage> emails = smtpserver.getReceivedEmails();
		assertThat(emails, hasSize(0));
	}
	
	@Test
	@SmtpServer(timeout=60000)
	public void testSendReceiveMultibleMail() throws Exception
	{
		int msgs = 500;
		for(int i=0;i<msgs;i++)
		{
			sendMessage("sender"+i+"@here.com", "Test"+i, "Test Body"+i, "receiver"+i+"@there.com");
		}
		
		List<SmtpMessage> emails = smtpserver.getReceivedEmails();
        assertThat(emails, hasSize(msgs));
        for(int i=0;i<msgs;i++)
        {
        	SmtpMessage email = emails.get(i);
            assertThat(email.getHeaderValue("From"), is("sender"+i+"@here.com"));
            assertThat(email.getHeaderValue("Subject"), is("Test"+i));
            assertThat(email.getBody(), is("Test Body"+i));
            assertThat(email.getHeaderValue("To"), is("receiver"+i+"@there.com"));	
        }
	}
	
	@Test
	@SmtpServer(timeout=10000,count=12)
	public void testSendReceiveMultibleMailCount() throws Exception
	{
		int msgs = 24;
		for(int i=0;i<msgs;i++)
		{
			sendMessage("sender"+i+"@here.com", "Test"+i, "Test Body"+i, "receiver"+i+"@there.com");
		}
		List<SmtpMessage> emails = smtpserver.getReceivedEmails();
		assertThat(emails, hasSize(msgs));
	}
	
	@Test(timeout=6000,expected=TimeLimitExceededException.class)
	@SmtpServer(timeout=1000,count=12)
	public void testSendReceiveMultibleMailCountException() throws Exception
	{
		int msgs = 11;
		for(int i=0;i<msgs;i++)
		{
			sendMessage("sender"+i+"@here.com", "Test"+i, "Test Body"+i, "receiver"+i+"@there.com");
		}
		List<SmtpMessage> emails = smtpserver.getReceivedEmails();
		assertThat(emails, hasSize(msgs));
	}
	
	@Test
	@SmtpServer(timeout=10000,waitFor={ 
			@WaitFor(headers=@Header(key="Subject",value="Test23")) 
	})
	public void testSendReceiveMultibleMailHeader() throws Exception
	{
		int msgs = 24;
		for(int i=0;i<msgs;i++)
		{
			sendMessage("sender"+i+"@here.com", "Test"+i, "Test Body"+i, "receiver"+i+"@there.com");
		}
		List<SmtpMessage> emails = smtpserver.getReceivedEmails();
		assertThat(emails, hasSize(msgs));
		assertThat(emails.get(23).getHeaderValue("Subject"), is("Test23"));
	}
	
	@Test(expected=TimeLimitExceededException.class)
	@SmtpServer(timeout=1000,waitFor={ 
			@WaitFor(headers=@Header(key="Subject",value="DoesNotExist")) 
	})
	public void testSendReceiveMultibleMailHeaderError1() throws Exception
	{
		int msgs = 24;
		for(int i=0;i<msgs;i++)
		{
			sendMessage("sender"+i+"@here.com", "Test"+i, "Test Body"+i, "receiver"+i+"@there.com");
		}
		List<SmtpMessage> emails = smtpserver.getReceivedEmails();
		assertThat(emails, hasSize(msgs));
	}
	
	@Test(expected=TimeLimitExceededException.class)
	@SmtpServer(timeout=1000,waitFor={ 
			@WaitFor(headers=@Header(key="DoesNotExist",value="")) 
	})
	public void testSendReceiveMultibleMailHeaderKeyError() throws Exception
	{
		int msgs = 24;
		for(int i=0;i<msgs;i++)
		{
			sendMessage("sender"+i+"@here.com", "Test"+i, "Test Body"+i, "receiver"+i+"@there.com");
		}
		List<SmtpMessage> emails = smtpserver.getReceivedEmails();
		assertThat(emails, hasSize(msgs));
	}

	private void sendMessage(String from, String subject, String body, String to) {
		SimpleMailMessage message = new SimpleMailMessage(); 
		message.setFrom(from);
        message.setTo(to); 
        message.setSubject(subject); 
        message.setText(body);
        mailSender.send(message);
	}
	
}
