package net.openright.jee.server.plugin.jetty.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CombinedLoginBuilder extends CoreLoginBuilder<CombinedLoginBuilder> {
    private final List<CoreLoginBuilder<?>> builders;

    public CombinedLoginBuilder(final List<CoreLoginBuilder<?>> builders) {
        this.builders = builders;
    }

    public CombinedLoginBuilder(final CoreLoginBuilder<?>... builders) {
        this(Arrays.asList(builders));
    }

    @Override
    protected CoreLoginAuthenticator getLoginAuthenticator() {
        List<CoreLoginAuthenticator> handlers = new ArrayList<>();
        for(CoreLoginBuilder<?> b: builders){
            handlers.add(b.getLoginAuthenticator());
        }
        return new CombinedLoginAuthenticator(handlers);
    }

    @Override
    protected void initLoginBuilder() throws Exception {
        for(CoreLoginBuilder<?> b: builders){
            b.initLoginBuilder();
        }
    }

    @Override
    protected List<CoreLogoutHandler> getLogoutHandlers() {
       List<CoreLogoutHandler> handlers = new ArrayList<>();
       for(CoreLoginBuilder<?> b: builders){
           handlers.addAll(b.getLogoutHandlers());
       }
       return handlers;
       
    }

}
