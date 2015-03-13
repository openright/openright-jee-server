package net.openright.jee.server.plugin.jetty.security;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Keep track of user info for this thread and provide easy access to this (through {@link SecurityContext}
 */
class ThreadLocalSecurityContext {

    private static final ThreadLocal<Deque<UserPrincipalInfo>> currentUser = new ThreadLocal<>();

    public static UserPrincipalInfo currentUser() {
        Deque<UserPrincipalInfo> deque = currentUser.get();
        return deque == null || deque.isEmpty() ? null : deque.getFirst();
    }

    static void runWithUser(DoExecute call, UserPrincipalInfo user) throws Exception {
        Deque<UserPrincipalInfo> deque = currentUser.get();
        if (deque == null) {
            deque = new LinkedList<>();
            currentUser.set(deque);
        }

        try {
            deque.addFirst(user);
            call.execute();
        } finally {
            deque.removeFirst();
        }
    }

    interface DoExecute {
        void execute() throws Exception;
    }
}
