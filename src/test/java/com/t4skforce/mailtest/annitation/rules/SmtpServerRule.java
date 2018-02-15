package com.t4skforce.mailtest.annitation.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.naming.TimeLimitExceededException;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.util.CollectionUtils;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.t4skforce.mailtest.annitation.SmtpServer;

public class SmtpServerRule implements TestRule {

	private long timeout = 60000;

	private SimpleSmtpServer dumbster;

	@Override
	public Statement apply(Statement base, Description description) {
		SmtpServer annotation = description.getAnnotation(SmtpServer.class);
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				if (annotation != null) {
					try (SimpleSmtpServer srv = SimpleSmtpServer.start(annotation.port())) {
						dumbster = srv;
						timeout = annotation.timeout();
						base.evaluate();
					} finally {
						dumbster.stop();
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
	 *             is thrown if in given interval no message is being received
	 * @throws InterruptedException
	 *             is thrown on interrupt
	 */
	public List<SmtpMessage> getReceivedEmails() throws TimeLimitExceededException, InterruptedException {
		Assert.assertNotNull(dumbster);
		List<SmtpMessage> receivedMail = new ArrayList<>();
		long startTime = System.currentTimeMillis();
		do {
			receivedMail.addAll(dumbster.getReceivedEmails());
			if (System.currentTimeMillis() - startTime >= timeout) {
				throw new TimeLimitExceededException(
						String.format("No Mail received in maximum time interval of {}ms", timeout));
			}
			Thread.sleep(500);
		} while (CollectionUtils.isEmpty(receivedMail));
		return Collections.unmodifiableList(new ArrayList<>(receivedMail));
	}

}
