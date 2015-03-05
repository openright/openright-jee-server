package net.openright.jee.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;

public abstract class JndiUtil {
    
    @SuppressWarnings("unchecked")
    public static <T> T lookup(String jndiName) {
        try {
            return ((T) new InitialContext().lookup(jndiName));
        } catch (NamingException e) {
            throw new IllegalArgumentException("Looking up " + jndiName, e);
        }
    }

    public static void register(String jndiName, Object object) {
        try {
            InitialContext ictx = new InitialContext();
            Name name = ictx.getNameParser(jndiName).parse(jndiName);
            Context ctx = ictx;
            for (int i = 0, max = name.size() - 1; i < max; i++)
            {
                try
                {
                    ctx = ctx.createSubcontext(name.get(i));
                } catch (NameAlreadyBoundException ignoreAndContinue)
                {
                    ctx = (Context) ctx.lookup(name.get(i));
                }
            }

            ictx.rebind(jndiName, object);
        } catch (NamingException e) {
            throw new IllegalArgumentException("Ugyldig jndiname: " + jndiName, e);
        }
    }

}
