package com.gearsofleo.testing.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.gearsofleo.platform.core.payment.api.PlatformCorePaymentApiProtos.PaymentDTO;
import com.gearsofleo.testing.dsl.AbstractDslTest;
import com.gearsofleo.testing.dsl.TestDsl;
import com.gearsofleo.testing.dsl.domain.valueobject.PlayerWithSession;
import com.gearsofleo.testing.dsl.facade.AccountFacade;
import com.gearsofleo.testing.dsl.facade.AuthenticationFacade;
import com.gearsofleo.testing.dsl.facade.PaymentFacade;
import com.gearsofleo.testing.dsl.service.external.ExternalIntegrationPaymentService;

public class PaymentTest extends AbstractDslTest  {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private AuthenticationFacade authenticationFacade = TestDsl.getAuthenticationFacade();
	private PaymentFacade paymentFacade = TestDsl.getPaymentFacade();
	private AccountFacade accountFacade = TestDsl.getAccountFacade();
	private ExternalIntegrationPaymentService externalIntegrationPaymentService 
		= TestDsl.getBean(ExternalIntegrationPaymentService.class);
	
	@Test
	public void test_deposit_existing_player() throws Exception{
			
		PlayerWithSession playerWithSession = authenticationFacade.login("daniel.lundmark@gmail.com", "testar");
		
		// Check balance before and after and see that we have gained some money
		double balanceBefore = accountFacade.getRealMoneyBalance(playerWithSession);
		logger.debug("Balance before deposit is: {}", balanceBefore);
		
		double depositAmount = 1000.0;
		paymentFacade.makeMockAdyenDeposit(playerWithSession, depositAmount);
		double balanceAfter = accountFacade.getRealMoneyBalance(playerWithSession);
		logger.debug("Balance after deposit is: {}", balanceAfter);
		
		Assert.assertEquals(balanceAfter, balanceBefore + depositAmount);
		
	}
	
	@Test
	public void test_request_withdrawal_existing_player() throws Exception {
		PlayerWithSession playerWithSession = authenticationFacade.login("daniel.lundmark@gmail.com", "testar");
		
		double withdrawAmount = 200;
		double balanceBefore = accountFacade.getRealMoneyBalance(playerWithSession);
		paymentFacade.makeMockAdyenWithdraw(playerWithSession, withdrawAmount);
		double balanceAfter = accountFacade.getRealMoneyBalance(playerWithSession);
		
		Assert.assertEquals(balanceAfter, balanceBefore - withdrawAmount);
	}
	
	@Test
	public void test_deny_withdrawal() throws Exception {
		PlayerWithSession playerWithSession = authenticationFacade.login("daniel.lundmark@gmail.com", "testar");
		
		double withdrawAmount = 200;
		double balanceBefore = accountFacade.getRealMoneyBalance(playerWithSession);
		PaymentDTO paymentDTO = 
			paymentFacade.requestSimpleAdyenWithdrawal(playerWithSession, withdrawAmount).getPayment();
		paymentFacade.confirmWithdrawalManually(paymentDTO);
		externalIntegrationPaymentService.mockDenyAdyenWithdrawal(paymentDTO);
		double balanceAfter = accountFacade.getRealMoneyBalance(playerWithSession);
		
		Assert.assertEquals(balanceAfter, balanceBefore);
	}
}
