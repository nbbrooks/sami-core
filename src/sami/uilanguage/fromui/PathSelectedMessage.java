package sami.uilanguage.fromui;

import java.util.Hashtable;
import java.util.UUID;
import sami.path.Path;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class PathSelectedMessage extends FromUiMessage {

    private Hashtable<ProxyInt, Path> proxyPaths;

    public PathSelectedMessage(UUID relevantOutputEventId, UUID missionId, Hashtable<ProxyInt, Path> path) {
        this.relevantOutputEventId = relevantOutputEventId;
        this.missionId = missionId;
        this.proxyPaths = path;
    }

    public Hashtable<ProxyInt, Path> getProxyPaths() {
        return proxyPaths;
    }

    public void setProxyPaths(Hashtable<ProxyInt, Path> proxyPaths) {
        this.proxyPaths = proxyPaths;
    }

    @Override
    public UUID getRelevantOutputEventId() {
        return relevantOutputEventId;
    }
}