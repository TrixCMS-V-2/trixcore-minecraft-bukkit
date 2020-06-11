package eu.trixcms.trixcore.bukkit.method;

import eu.trixcms.trixcore.api.method.IMethod;
import eu.trixcms.trixcore.api.method.Methods;
import eu.trixcms.trixcore.api.method.annotation.ArgsPrecondition;
import eu.trixcms.trixcore.api.method.annotation.MethodName;
import eu.trixcms.trixcore.common.response.Response;
import eu.trixcms.trixcore.common.response.SuccessResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Objects;

@MethodName(method = Methods.IS_CONNECTED)
public class IsConnectedMethod implements IMethod {

    @Override
    @ArgsPrecondition(amount = 1)
    public Response exec(String[] args) {
        return new SuccessResponse(Bukkit.getServer().getOnlinePlayers().stream()
                .map(OfflinePlayer::getName).filter(Objects::nonNull)
                .anyMatch(name -> name.equals(args[0]))
        );
    }

}
