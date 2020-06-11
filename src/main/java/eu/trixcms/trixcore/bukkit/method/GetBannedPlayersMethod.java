package eu.trixcms.trixcore.bukkit.method;

import eu.trixcms.trixcore.api.method.IMethod;
import eu.trixcms.trixcore.api.method.Methods;
import eu.trixcms.trixcore.api.method.annotation.MethodName;
import eu.trixcms.trixcore.common.response.Response;
import eu.trixcms.trixcore.common.response.SuccessResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.stream.Collectors;

@MethodName(method = Methods.GET_BANNED_PLAYER)
public class GetBannedPlayersMethod implements IMethod {

    @Override
    public Response exec(String[] args) {
        return new SuccessResponse(Bukkit.getBannedPlayers().stream()
                .map(OfflinePlayer::getName)
                .collect(Collectors.toList())
        );
    }
}
