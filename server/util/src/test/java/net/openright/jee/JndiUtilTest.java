package net.openright.jee;

import javax.sql.DataSource;

import net.openright.jee.util.JndiUtil;

import org.fest.assertions.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class JndiUtilTest {

    @Test
    public void register_datasource_in_jndi() throws Exception {
        DataSource dataSource = Mockito.mock(DataSource.class);
        JndiUtil.register("jdbc/mockDs", dataSource);
        DataSource fromJndi = JndiUtil.lookup("jdbc/mockDs");
        Assertions.assertThat(dataSource).isEqualTo(fromJndi);
    }
}
