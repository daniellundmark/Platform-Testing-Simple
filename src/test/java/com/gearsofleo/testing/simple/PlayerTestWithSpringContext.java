package com.gearsofleo.testing.simple;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.gearsofleo.rhino.core.bootstrap.AppProfile;
import com.gearsofleo.rhino.test.AbstractTestNGTest;
import com.gearsofleo.testing.dsl.domain.valueobject.Player;
import com.gearsofleo.testing.dsl.domain.valueobject.PlayerWithSession;
import com.gearsofleo.testing.dsl.facade.AuthenticationFacade;
import com.gearsofleo.testing.dsl.facade.PlayerFacade;
import com.gearsofleo.testing.dsl.facade.PlayerFacade.Country;
import com.gearsofleo.testing.dsl.facade.PlayerFacade.Currency;
import com.gearsofleo.testing.dsl.facade.PlayerFacade.Language;
import com.gearsofleo.testing.simple.config.TestConfig;

@ActiveProfiles(AppProfile.NORMAL)
@ContextConfiguration(classes = { TestConfig.class })
public class PlayerTestWithSpringContext extends AbstractTestNGTest  {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Resource
	private PlayerFacade playerFacade;
	
	@Resource
	private AuthenticationFacade authenticationFacade;
	
	@Test
	public void test_playerLogin(){
		
		Player player = playerFacade.getByUsername("daniel.lundmark@gmail.com");
		PlayerWithSession playerWithSession = authenticationFacade.login(player, "testar");
		
		Assert.assertNotNull(playerWithSession.getSessionDTO().getSessionUid());
		logger.debug("Resulting session: {}", playerWithSession.getSessionDTO());		
	}
	
	@Test
	public void test_createRandomPlayerAndLogin() {
		
		String password = playerFacade.randomPassword();
		Player player = 
				playerFacade.createTestPlayer(PlayerFacade.Country.CA, PlayerFacade.Language.en, password);
		
		PlayerWithSession session = authenticationFacade.login(player, password);
		
		Assert.assertNotNull(session.getSessionDTO().getSessionUid());
		logger.debug("Resulting session: {}", session.getSessionDTO());
		logger.debug("Player: {}/{}", player.getPlayerDTO().getUsername(), password);
	}
	
	@Test
	public void test_createPlayer() {
		String password = "kambi";
		Player player = playerFacade.createTestPlayer("dan.eriksson@kambi.com", "Dan", "Eriksson", Country.SE, Language.sv, Currency.SEK, password);
		PlayerWithSession session = authenticationFacade.login(player, password);
		
		Assert.assertNotNull(session.getSessionDTO().getSessionUid());
		logger.debug("Resulting session: {}", session.getSessionDTO());
		logger.debug("Player: {}/{}", player.getPlayerDTO().getUsername(), password);
	}
	
}
