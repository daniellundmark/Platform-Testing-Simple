package com.gearsofleo.testing.simple;

import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.gearsofleo.platform.core.gaming.api.PlatformCoreGamingApiProtos.BetTransactionDTO;
import com.gearsofleo.rhino.core.bootstrap.AppProfile;
import com.gearsofleo.rhino.test.AbstractTestNGTest;
import com.gearsofleo.testing.dsl.domain.valueobject.PlayerWithSession;
import com.gearsofleo.testing.dsl.facade.AccountFacade;
import com.gearsofleo.testing.dsl.facade.AuthenticationFacade;
import com.gearsofleo.testing.dsl.facade.GamingFacade;
import com.gearsofleo.testing.simple.config.PropertiesConfig;
import com.gearsofleo.testing.simple.config.TestConfig;

@ActiveProfiles(AppProfile.NORMAL)
@ContextConfiguration(classes = {PropertiesConfig.class, TestConfig.class })
public class BetTest extends AbstractTestNGTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource
	private AuthenticationFacade authenticationFacade;
	
	@Resource
	private AccountFacade accountFacade;
	
	@Resource
	private GamingFacade netEntGamingFacade;
	
	@Test
	public void test_loseBet() throws Exception {
		PlayerWithSession player = authenticationFacade.login("daniel.lundmark@gmail.com", "testar");
		
		double balanceBefore = accountFacade.getRealMoneyBalance(player);
		logger.debug("Balance before bet is: {}", balanceBefore);
		
		double betAmount = 10.0;
		netEntGamingFacade.loseBet(player, betAmount);
		
		double balanceAfter = accountFacade.getRealMoneyBalance(player);
		logger.debug("Balance after bet is: {}", balanceAfter);

		Assert.assertEquals(balanceAfter, balanceBefore - betAmount);
	}

	@Test
	public void test_winBet() throws Exception {

		PlayerWithSession player = authenticationFacade.login("daniel.lundmark@gmail.com", "testar");
		
		double balanceBefore = accountFacade.getRealMoneyBalance(player);
		logger.debug("Balance before bet is: {}", balanceBefore);
		
		double betAmount = 10.0;
		double winAmount = 50.0;
		netEntGamingFacade.winBet(player, betAmount, winAmount);
		
		double balanceAfter = accountFacade.getRealMoneyBalance(player);
		logger.debug("Balance after bet is: {}", balanceAfter);

		Assert.assertEquals(balanceAfter, balanceBefore - betAmount + winAmount);

	}

	@Test
	public void test_should_play_for_real_money() throws Exception {
		PlayerWithSession player = authenticationFacade.login("daniel.lundmark@gmail.com", "testar");
		
		double balanceBefore = accountFacade.getRealMoneyBalance(player);
		logger.debug("Balance before bet is: {}", balanceBefore);
		
		double betAmount = 10.0;
		List<BetTransactionDTO> betTransactions = 
				netEntGamingFacade.loseBet(player, betAmount);
		
		double balanceAfter = accountFacade.getRealMoneyBalance(player);
		logger.debug("Balance after bet is: {}", balanceAfter);

		Assert.assertEquals(balanceAfter, balanceBefore - betAmount);
		
		Assert.assertTrue(betTransactions.size() > 0, "There should be bet transactions for this bet");

		Assert.assertEquals(Double.valueOf(betTransactions.get(0).getRealAmount()), betAmount,
				"The bet should  be for real money");
		

	}


}
