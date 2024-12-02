package xyz.philipjones.muzik.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.philipjones.muzik.services.security.ServerRefreshTokenService;

@Component
public class TokenCleanupTask {

    private final ServerRefreshTokenService serverRefreshTokenService;

    @Autowired
    public TokenCleanupTask(ServerRefreshTokenService serverRefreshTokenService) {
        this.serverRefreshTokenService = serverRefreshTokenService;
    }

    @Scheduled(cron = "0 30 9 * * ?") // Runs every day at 9:30 AM
    public void cleanUpExpiredTokens() {
        serverRefreshTokenService.deleteExpiredTokens();
    }
}