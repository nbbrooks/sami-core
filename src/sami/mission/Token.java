package sami.mission;

import com.perc.mitpas.adi.mission.planning.task.Task;
import sami.proxy.ProxyInt;

/**
 *
 * @author pscerri
 */
public class Token {

    // There are 3 "types" of tokens
    //  Generic tokens do not have a proxy or task associated with them
    //  Proxy tokens have a non-specific proxy, but no task, associated with them
    //  Task tokens have a specific task and potentially a proxy associated with them
    public enum TokenType {

        Task, Proxy, Generic
    };
    private final String name;
    private ProxyInt proxy;
    private final Task task;
    private final TokenType type;

    public Token(String name, TokenType type, ProxyInt proxy, Task task) {
        this.name = name;
        this.proxy = proxy;
        this.task = task;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public TokenType getType() {
        return type;
    }

    public Task getTask() {
        return task;
    }

    public ProxyInt getProxy() {
        return proxy;
    }

    public void setProxy(ProxyInt proxy) {
        this.proxy = proxy;
    }

    @Override
    public String toString() {
        String ret = "Token:";
        if (type == TokenType.Proxy) {
            ret += "P-" + (proxy == null ? "NULL" : proxy.getProxyId()) + "-" + (task == null ? "NULL" : task.getName());
        } else if (type == TokenType.Task) {
            ret += "T-" + (proxy == null ? "NULL" : proxy.getProxyId()) + "-" + (task == null ? "NULL" : task.getName());
        } else {
            ret += type.toString();
        }
        return ret;
    }

    @Override
    public Token clone() {
        return new Token(name, type, proxy, task);
    }
}
