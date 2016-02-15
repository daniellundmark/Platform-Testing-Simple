package com.gearsofleo.testing.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.gearsofleo.testing.dsl.TestDsl;
import com.gearsofleo.testing.dsl.domain.valueobject.Player;
import com.gearsofleo.testing.dsl.domain.valueobject.PlayerWithSession;
import com.gearsofleo.testing.dsl.facade.AuthenticationFacade;
import com.gearsofleo.testing.dsl.facade.PlayerFacade;

public class PlayerTestWithStaticAccess {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private PlayerFacade playerFacade;
	private AuthenticationFacade authenticationFacade;
	
	@BeforeClass
	public void init(){
		playerFacade = TestDsl.getBean(PlayerFacade.class);
		authenticationFacade = TestDsl.getBean(AuthenticationFacade.class);
	}
	
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
	
}
