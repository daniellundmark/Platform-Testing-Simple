package com.gearsofleo.testing.simple;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.casinomodule.walletserver.types._3_0.WithdrawResponse;
import com.gearsofleo.platform.core.gaming.api.PlatformCoreGamingApiProtos.BetTransactionDTO;
import com.gearsofleo.platform.core.gaming.api.PlatformCoreGamingApiProtos.BetTransactionTypeDTO;
import com.gearsofleo.platform.integration.gaming.testclient.netent3.NetEntClient;
import com.gearsofleo.platform.integration.gaming.testclient.netent3.NetEntDeposit;
import com.gearsofleo.platform.integration.gaming.testclient.netent3.NetEntRequest;
import com.gearsofleo.platform.integration.gaming.testclient.netent3.NetEntRollback;
import com.gearsofleo.platform.integration.gaming.testclient.netent3.NetEntWithdraw;
import com.gearsofleo.rhino.core.bootstrap.AppProfile;
import com.gearsofleo.rhino.test.AbstractTestNGTest;
import com.gearsofleo.testing.dsl.domain.valueobject.PlayerWithSession;
import com.gearsofleo.testing.dsl.facade.AccountFacade;
import com.gearsofleo.testing.dsl.facade.AuthenticationFacade;
import com.gearsofleo.testing.dsl.facade.GamingFacade;
import com.gearsofleo.testing.dsl.facade.NetEntGamingFacadeImpl;
import com.gearsofleo.testing.dsl.facade.PaymentFacade;
import com.gearsofleo.testing.dsl.service.external.ExternalGamingService;
import com.gearsofleo.testing.simple.config.PropertiesConfig;
import com.gearsofleo.testing.simple.config.TestConfig;

@ActiveProfiles(AppProfile.NORMAL)
@ContextConfiguration(classes = { PropertiesConfig.class, TestConfig.class })
public class NetEntRollbackTest extends AbstractTestNGTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource
	private AuthenticationFacade authenticationFacade;
	
	@Resource
	private AccountFacade accountFacade;
	
	@Resource
	private PaymentFacade paymentFacade;
	
	@Resource
	private GamingFacade netEntGamingFacade;
	
	@Resource
	private ExternalGamingService externalGamingService;
	
	PlayerWithSession player;

	@BeforeClass
	public void initPlayer(){
		player = authenticationFacade.login("daniel.lundmark@gmail.com", "testar");
	}
	
	private NetEntClient buildClient() {
		return NetEntClient.builder()
				.callerId("gutro")
				.password("leovegas")
				.endpointUrl("http://leo-mt-stage-gaming01.sth.basefarm.net:8080/gaming-integration/api/ws/netent/WalletServer3")
				.build();
	}
	
	private NetEntWithdraw withdraw(double amount){
		return NetEntWithdraw.builder()
				.playerUid(player.getPlayerDTO().getPlayerUid())
				.amount(new BigDecimal(amount))
				.build();
	}
	
	private NetEntDeposit deposit(double amount){
		return NetEntDeposit.builder()
				.playerUid(player.getPlayerDTO().getPlayerUid())
				.amount(new BigDecimal(amount))
				.build();
	}
	
	private List<BetTransactionDTO> getBetTransactions(NetEntRequest request){
		List<BetTransactionDTO> betTransactions =
				externalGamingService.getBetTransactions(
						NetEntGamingFacadeImpl.DEFAULT_EXTERNAL_PROVIDER_ID, 
						String.valueOf(request.getExternalBetId()));
		return betTransactions;
	}

	@Test
	public void test_use_netent_client_directly() throws Exception {
		
		NetEntClient netEntClient = buildClient();

		// First make a deposit, then play for the money
		double amount = 100;
		paymentFacade.makeMockAdyenDeposit(player, amount);

		double currentBalance = accountFacade.getRealMoneyBalance(player);

		netEntClient.execute(withdraw(100), false);

		Assert.assertEquals(accountFacade.getRealMoneyBalance(player), currentBalance - 100);

		netEntClient.execute(deposit(10), true);

		Assert.assertEquals(accountFacade.getRealMoneyBalance(player), currentBalance - 100 + 10);
	}

	@Test
	public void test_retransmit_is_not_handled_twice() throws Exception {

		NetEntClient netEntClient = buildClient();

		// First make a deposit, then play for the money
		double amount = 100;
		paymentFacade.makeMockAdyenDeposit(player, amount);

		double currentBalance = accountFacade.getRealMoneyBalance(player);

		NetEntWithdraw netEntWithdraw = withdraw(100);
		
		WithdrawResponse response1 = netEntClient.execute(netEntWithdraw, true);
		Assert.assertEquals(accountFacade.getRealMoneyBalance(player), currentBalance - amount);
		
		WithdrawResponse response2 = netEntClient.execute(netEntWithdraw, true);
		Assert.assertEquals(accountFacade.getRealMoneyBalance(player), currentBalance - amount);
		
		Assert.assertEquals(response2.getTransactionId(), response1.getTransactionId());
	}
	
	@Test
	public void test_rollback_and_retransmit_of_first_wager_should_cause_cancel_wager() throws Exception {

		NetEntClient netEntClient = buildClient();
		
		double amount = 100.0;
		
		NetEntWithdraw withdraw = withdraw(amount);
		netEntClient.execute(withdraw, false);
		
		NetEntRollback rollback = NetEntRollback.builder().ofTransaction(withdraw).build();
		netEntClient.execute(rollback, false);
		netEntClient.execute(rollback, false);

		List<BetTransactionDTO> betTransactions = getBetTransactions(withdraw);
		
		Assert.assertTrue(betTransactions.stream().anyMatch( bt ->
			bt.getType() == BetTransactionTypeDTO.WAGER &&
			Double.parseDouble(bt.getAmount()) == amount &&
			bt.getBetClosed() == false
		));
		
		Assert.assertTrue(betTransactions.stream().anyMatch( bt ->
			bt.getType() == BetTransactionTypeDTO.CANCEL_WAGER &&
			Double.parseDouble(bt.getAmount()) == amount && 
			bt.getBetClosed() == true
		));

		Assert.assertEquals(betTransactions.size(), 2);
		
	}
	/*
	@Test
	public void test_rollback_and_retransmit_of_closed_round_should_cause_new_result() throws Exception {
		PlayerHelper playerHelper = new PlayerHelper();
		BetHelper betHelper = new BetHelper();
		
		Player player = playerHelper.loginByUsernameAndPassword("daniel.lundmark@gmail.com", "leovegas");

		NetEntGame game = NetEntGame.builder().player(player).build();
		NetEntClient netEntClient = game.getClient();
		netEntClient.initialize();
		
		double amount = 100.0;
		
		NetEntWithdraw withdraw = NetEntWithdraw.builder().playerUid(player.getPlayerUid()).amount(new BigDecimal(amount)).build();
		netEntClient.execute(withdraw, true);
		
		NetEntRollback rollback = NetEntRollback.builder().ofTransaction(withdraw).build();
		netEntClient.execute(rollback, false);
		netEntClient.execute(rollback, false);

		List<Bet> betsForWager = betHelper.getBetsByExternalTxId(String.valueOf(withdraw.getExternalTxId()));
		logger.debug("Bets are {}", betsForWager);
		
		Assert.assertEquals(betsForWager.size(), 2);
		
		// Find the wager and result bets
		Bet wager = null, result = null;
		for(Bet bet : betsForWager){
			if(bet.getBetTransactionDTOs().stream().anyMatch(bt -> bt.getType() == BetTransactionTypeDTO.WAGER)){
				wager = bet;
			}
			if(bet.getBetTransactionDTOs().stream().anyMatch(bt -> bt.getType() == BetTransactionTypeDTO.RESULT)){
				result = bet;
			}
		}
		
		Assert.assertTrue(wager.getBetTransactionDTOs().stream().anyMatch( bt ->
			bt.getType() == BetTransactionTypeDTO.WAGER &&
			Double.parseDouble(bt.getAmount()) == amount && 
			bt.getBetClosed())
		);		
		Assert.assertEquals(wager.getBetTransactionDTOs().size(), 1);
		
		Assert.assertTrue(result.getBetTransactionDTOs().stream().anyMatch( bt ->
			bt.getType() == BetTransactionTypeDTO.RESULT &&
			Double.parseDouble(bt.getAmount()) == amount && 
			bt.getBetClosed())
		);		
		Assert.assertEquals(result.getBetTransactionDTOs().size(), 1);	
	}
	
	@Test
	public void rollback_of_non_existing_transaction_should_not_cause_exception() throws Exception{
		PlayerHelper playerHelper = new PlayerHelper();
		Player player = playerHelper.loginByUsernameAndPassword("daniel.lundmark@gmail.com", "leovegas");
		NetEntGame game = NetEntGame.builder().player(player).build();
		NetEntClient netEntClient = game.getClient();
		netEntClient.initialize();
		
		double amount = 100.0;
		
		NetEntWithdraw withdraw = NetEntWithdraw.builder().playerUid(player.getPlayerUid()).amount(new BigDecimal(amount)).externalTxId(0L).build();	
		NetEntRollback rollback = NetEntRollback.builder().ofTransaction(withdraw).build();
		netEntClient.execute(rollback, false);
	}
	
	@Test
	public void test_rollback_of_free_round_transaction() throws Exception {
		PlayerHelper playerHelper = new PlayerHelper();
		Player player = playerHelper.loginByUsernameAndPassword("daniel.lundmark@gmail.com", "leovegas");
		NetEntGame game = NetEntGame.builder().player(player).build();
		NetEntClient netEntClient = game.getClient();
		netEntClient.initialize();
		
		Accounts accounts = player.getAccounts();
		double initialTotalBonusBalance = accounts.getTotalBonusBalance();
		
		// Send and retransmit a free round transaction
		double bonusAmount = 500;
		NetEntDeposit freeRoundDeposit = NetEntDeposit.builder().reason(Reason.WAGERED_BONUS.name()).playerUid(player.getPlayerUid()).amount(new BigDecimal(bonusAmount)).build();
		netEntClient.execute(freeRoundDeposit);
	
		// Verify that we increased our bonus by doing this
		Assert.assertEquals(accounts.getTotalBonusBalance(), initialTotalBonusBalance+bonusAmount);
		
		netEntClient.execute(freeRoundDeposit);
		
		// But not again by a retransmit
		Assert.assertEquals(accounts.getTotalBonusBalance(), initialTotalBonusBalance+bonusAmount);
		
		// Then rollback the transaction (and retransmit that too)
		NetEntRollback rollback = NetEntRollback.builder().ofTransaction(freeRoundDeposit).build();
		netEntClient.execute(rollback);
		netEntClient.execute(rollback);
		
		// Make sure we are back where we started
		Assert.assertEquals(accounts.getTotalBonusBalance(), initialTotalBonusBalance);
				
	}
	
	*/
}
