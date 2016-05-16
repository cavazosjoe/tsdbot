package org.tsd.tsdbot.tsdtv;

import java.net.InetAddress;

public class TSDTVWebUser extends TSDTVUser {

    private InetAddress inetAddress;

    public TSDTVWebUser(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    @Override
    public boolean isOp() {
        return false;
    }

    @Override
    public String getId() {
        return inetAddress.getHostAddress();
    }
}
