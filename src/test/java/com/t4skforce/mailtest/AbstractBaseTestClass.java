package com.t4skforce.mailtest;

import org.junit.Rule;

import com.t4skforce.mailtest.annitation.rules.SmtpServerRule;

public abstract class AbstractBaseTestClass {

	@Rule
	public final SmtpServerRule smtpserver = new SmtpServerRule();
	
}
