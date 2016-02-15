package com.gearsofleo.testing.simple;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.gearsofleo.platform.core.responsiblegaming.api.PlatformCoreResponsibleGamingApiProtos.FutureGamingLimitDTO;
import com.gearsofleo.platform.core.responsiblegaming.api.PlatformCoreResponsibleGamingApiProtos.GamingLimitDTO;
import com.gearsofleo.platform.core.responsiblegaming.api.PlatformCoreResponsibleGamingApiProtos.LimitType;
import com.gearsofleo.platform.core.responsiblegaming.api.PlatformCoreResponsibleGamingApiProtos.PeriodType;
import com.gearsofleo.rhino.core.bootstrap.AppProfile;
import com.gearsofleo.rhino.core.exception.InvalidStateException;
import com.gearsofleo.rhino.test.AbstractTestNGTest;
import com.gearsofleo.testing.dsl.domain.valueobject.Player;
import com.gearsofleo.testing.dsl.facade.PlayerFacade;
import com.gearsofleo.testing.dsl.service.external.ExternalResponsibleGamingService;
import com.gearsofleo.testing.simple.config.PropertiesConfig;
import com.gearsofleo.testing.simple.config.TestConfig;

@ActiveProfiles(AppProfile.NORMAL)
@ContextConfiguration(classes = { PropertiesConfig.class, TestConfig.class })
public class GamingLimitsTest extends AbstractTestNGTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Resource
	PlayerFacade playerFacade;
	
	@Resource
	ExternalResponsibleGamingService externalResponsibleGamingService;
	
	@Test
	public void test_set_limit() throws Exception{
		
		Player player = playerFacade.getByUsername("daniel.lundmark@gmail.com");
		
		// If we set a limit for this player, we should be able to retrieve one of that type
		externalResponsibleGamingService.setLimit(player.getPlayerDTO().getPlayerUid(), LimitType.WAGER, PeriodType.DAY, "1000");

		Assert.assertTrue(externalResponsibleGamingService.getLimits(player.getPlayerDTO().getPlayerUid())
							.getLimits().getActiveList().stream()
				.anyMatch(l -> l.getLimitType() == LimitType.WAGER), 
				"There should be an active limit of the right type after setting such a limit");
	}
	
	@Test
	public void test_raising_limit_requires_cooldown() throws Exception{
		
		Player player = playerFacade.getByUsername("daniel.lundmark@gmail.com");
		
		// Set a limit for the player, then retrieve the value of the active limit 
		// (as there might have been one since before)
		// And then raise it, the resulting limit should require cooldown
		externalResponsibleGamingService.setLimit(player.getPlayerDTO().getPlayerUid(), LimitType.WAGER, PeriodType.DAY, "1000");

		GamingLimitDTO activeLimit = externalResponsibleGamingService.getLimits(player.getPlayerDTO().getPlayerUid())
										.getLimits().getActiveList().stream()
				.filter(l -> l.getLimitType() == LimitType.WAGER && l.getPeriodType() == PeriodType.DAY)
				.findFirst().orElseThrow(() -> new InvalidStateException("There was no active exception of the right type"));


		// Raise the limit that was the active one
		String raisedValue = String.valueOf((int)(Integer.parseInt(activeLimit.getValue()) * 1.2));
		externalResponsibleGamingService.setLimit(player.getPlayerDTO().getPlayerUid(), LimitType.WAGER, PeriodType.DAY, raisedValue);

		
		FutureGamingLimitDTO pendingLimit = externalResponsibleGamingService.getLimits(player.getPlayerDTO().getPlayerUid())
				.getLimits().getPendingList().stream()
				.filter(l -> l.getLimit().getLimitType() == LimitType.WAGER && l.getLimit().getPeriodType() == PeriodType.DAY 
				&& l.getLimit().getValue().equals(raisedValue))
				.findFirst().orElseThrow(() -> new InvalidStateException("There was no pending exception of the right type and value"));
		
	}
	
	
}
