package eu.trixcms.trixcore.bukkit.method;

import eu.trixcms.trixcore.api.container.PlayerContainer;
import eu.trixcms.trixcore.api.container.PlayersContainer;
import eu.trixcms.trixcore.api.method.IMethod;
import eu.trixcms.trixcore.api.method.Methods;
import eu.trixcms.trixcore.api.method.annotation.MethodName;
import eu.trixcms.trixcore.common.response.Response;
import eu.trixcms.trixcore.common.response.SuccessResponse;
import org.bukkit.Bukkit;

import java.util.stream.Collectors;

@MethodName(method = Methods.GET_PLAYER_LIST)
public class GetPlayerListMethod implements IMethod {

    @Override
    public Response exec(String[] args) {
        return new SuccessResponse(
                new PlayersContainer(
                        Bukkit.getServer().getOnlinePlayers().stream()
                                .map(p -> new PlayerContainer(p.getName(), p.getUniqueId()))
                                .collect(Collectors.toList())

                )
        );
    }
}
