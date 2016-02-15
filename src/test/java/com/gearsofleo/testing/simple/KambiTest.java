package com.gearsofleo.testing.simple;

import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.gearsofleo.platform.core.gaming.betting.api.PlatformCoreGamingBettingApiProtos.BettingTransactionDTO;
import com.gearsofleo.platform.core.gaming.betting.api.PlatformCoreGamingBettingApiProtos.BettingTransactionTypeDTO;
import com.gearsofleo.platform.integration.gaming.kambi.api.KambiApiProtos.AuthenticateResponse;
import com.gearsofleo.platform.integration.gaming.kambi.api.KambiApiProtos.KambiTransactionType;
import com.gearsofleo.platform.integration.gaming.kambi.api.KambiApiProtos.WalletResponse;
import com.gearsofleo.rhino.core.bootstrap.AppProfile;
import com.gearsofleo.rhino.test.AbstractTestNGTest;
import com.gearsofleo.testing.dsl.domain.valueobject.PlayerWithSession;
import com.gearsofleo.testing.dsl.domain.valueobject.sportsbook.Coupon;
import com.gearsofleo.testing.dsl.facade.AuthenticationFacade;
import com.gearsofleo.testing.dsl.facade.PlayerFacade;
import com.gearsofleo.testing.dsl.facade.SportsbookFacade;
import com.gearsofleo.testing.dsl.service.external.ExternalAuthenticationService;
import com.gearsofleo.testing.dsl.service.external.ExternalGamingBettingService;
import com.gearsofleo.testing.dsl.service.external.ExternalIntegrationGamingKambiService;
import com.gearsofleo.testing.simple.config.TestConfig;

@ActiveProfiles(AppProfile.NORMAL)
@ContextConfiguration(classes = { TestConfig.class })
public class KambiTest extends AbstractTestNGTest   {

	@Resource(name="externalIntegrationGamingKambiService")
	private ExternalIntegrationGamingKambiService kambiService;
	
	@Resource
	private SportsbookFacade sportsbookFacade;
	
	@Resource
	private PlayerFacade playerFacade;
	
	@Resource
	private AuthenticationFacade authenticationFacade;
	
	@Resource
	private ExternalAuthenticationService externalAuthenticationService;
	
	@Resource
	private ExternalGamingBettingService externalGamingBettingService;
	
	@Test
	public void authenticate(){
		
		// Login the player and get a token
		PlayerWithSession player = authenticationFacade.login("daniel.lundmark@gmail.com", "testar");
		String token = externalAuthenticationService.createToken(player.getSessionDTO().getSessionUid()).getTokenUid();
		
		// Authenticate against our implementation of the Wallet API
		AuthenticateResponse resp = kambiService.authenticate(token);
		
		Assert.assertEquals(resp.getPlayerSessionToken(), player.getSessionDTO().getSessionUid());
		Assert.assertEquals(resp.getCustomerPlayerId(), player.getPlayerUid());
		
	}
	
	@Test
	public void fund_and_win_live_bet(){
		// Login the player
		PlayerWithSession player = authenticationFacade.login("daniel.lundmark@gmail.com", "testar");
		
		double stakeAmount = 10;
		
		// Get a coupon and fund it
		Coupon coupon = sportsbookFacade.createCoupon(player, 2, stakeAmount);
		
		WalletResponse response;
		
		// Fund the coupon
		response = kambiService.fundLiveBet(coupon);
		Assert.assertEquals(response.getSuccess(), true);
		
		// Approve the live bet
		response = kambiService.approveBet(coupon);
		Assert.assertEquals(response.getSuccess(), true);
		
		double winAmount = 100;
		
		// Settle each combination as a win
		coupon.getCombinations().forEach(c -> {
			WalletResponse cresp = kambiService.winCombination(coupon, c, winAmount);
			Assert.assertEquals(cresp.getSuccess(), true);
		});
		
		// Make sure we have matching betting transactions
	
		List<BettingTransactionDTO> bettingTransactions = externalGamingBettingService.findBettingTransactions(coupon).getTransactionList();
		
		sportsbookFacade.assertTransaction(1, bettingTransactions, coupon, Optional.of(BettingTransactionTypeDTO.PLACE_COUPON_WAGER), Optional.of(KambiTransactionType.STAKE_TRANSACTION_UNAPPROVED_BET), 20);
		sportsbookFacade.assertTransaction(1, bettingTransactions, coupon, Optional.of(BettingTransactionTypeDTO.APPROVE_COUPON_WAGER), Optional.of(KambiTransactionType.ACCEPT_COUPON_NOTIFICATION), 0);
		sportsbookFacade.assertTransaction(2, bettingTransactions, coupon, Optional.of(BettingTransactionTypeDTO.REPORT_COUPON_RESULT), Optional.of(KambiTransactionType.PAYOUT_SETTLED_BET_DEPOSIT), 100); // There should actually be two of these
		
	}
	
	@Test
	public void fund_and_lose_live_bet(){
		// Login the player
		PlayerWithSession player = authenticationFacade.login("daniel.lundmark@gmail.com", "testar");
		
		double stakeAmount = 10;
		
		// Get a coupon and fund it
		Coupon coupon = sportsbookFacade.createCoupon(player, 2, stakeAmount);
		
		WalletResponse response;
		
		// Fund the coupon
		response = kambiService.fundLiveBet(coupon);
		Assert.assertEquals(response.getSuccess(), true);
		
		// Approve the live bet
		response = kambiService.approveBet(coupon);
		Assert.assertEquals(response.getSuccess(), true);

		// Settle each combination as a loss
		coupon.getCombinations().forEach(c -> {
			WalletResponse cresp = kambiService.loseCombination(coupon, c);
			Assert.assertEquals(cresp.getSuccess(), true);
		});
		
		// Make sure we have matching betting transactions
		
		List<BettingTransactionDTO> bettingTransactions = externalGamingBettingService.findBettingTransactions(coupon).getTransactionList();	
		
		sportsbookFacade.assertTransaction(1, bettingTransactions, coupon, Optional.of(BettingTransactionTypeDTO.PLACE_COUPON_WAGER), Optional.of(KambiTransactionType.STAKE_TRANSACTION_UNAPPROVED_BET), 20);
		sportsbookFacade.assertTransaction(1, bettingTransactions, coupon, Optional.of(BettingTransactionTypeDTO.APPROVE_COUPON_WAGER), Optional.of( KambiTransactionType.ACCEPT_COUPON_NOTIFICATION), 0);
		sportsbookFacade.assertTransaction(2, bettingTransactions, coupon, Optional.of(BettingTransactionTypeDTO.REPORT_COUPON_RESULT), Optional.of(KambiTransactionType.CLOSE_LOST_BET), 0);
	}
	 
	
	
}
