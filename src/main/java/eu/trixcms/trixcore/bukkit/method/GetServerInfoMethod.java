package eu.trixcms.trixcore.bukkit.method;

import eu.trixcms.trixcore.api.container.ServerCapacityContainer;
import eu.trixcms.trixcore.api.container.ServerInfoContainer;
import eu.trixcms.trixcore.api.method.IMethod;
import eu.trixcms.trixcore.api.method.Methods;
import eu.trixcms.trixcore.api.method.annotation.MethodName;
import eu.trixcms.trixcore.common.response.Response;
import eu.trixcms.trixcore.common.response.SuccessResponse;
import org.bukkit.Bukkit;

import java.net.InetAddress;
import java.net.UnknownHostException;

@MethodName(method = Methods.GET_SERVER_INFO)
public class GetServerInfoMethod implements IMethod {

    @Override
    public Response exec(String[] args) {
        InetAddress ip = null;

        try {
            ip = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        String completeIP = ((ip == null) ? "localhost" : ip.getHostAddress()) + ":" + Bukkit.getPort();

        return new SuccessResponse(
                new ServerInfoContainer(Bukkit.getName(), completeIP, Bukkit.getMotd(), getVersion(), new ServerCapacityContainer(getPlayerNumber(), Bukkit.getMaxPlayers()))
        );
    }

    private int getPlayerNumber() {
        return Bukkit.getOnlinePlayers().size();
    }

    private String getVersion() {
        String version = Bukkit.getServer().getClass().getPackage().getName();
        version = version.substring(version.lastIndexOf('.') + 1);
        version = version.split("R")[0];
        version = version.replace("v", "");
        version = version.replace("_", ".");
        return version;
    }
}
