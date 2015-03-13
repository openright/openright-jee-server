package net.openright.jee.util;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JMXUtil {
	private static final Logger log = LoggerFactory.getLogger(JMXUtil.class);

	public static final String DEFAULT_DOMAIN = JMXUtil.class.getPackage()
			.getName();
	private static final MBeanServer mbs = ManagementFactory
			.getPlatformMBeanServer(); // NOSONAR

	private final String domain;

	public JMXUtil() {
		this(DEFAULT_DOMAIN);
	}

	public JMXUtil(String domain) {
		this.domain = domain;
	}

	public ObjectName createMBeanObjectName(String name) {
		String navn = domain + ":name=" + name;
		return createMBeanObjectNameFullyQualified(navn);
	}

	public ObjectName createMBeanObjectName(String typeName, String name) {
		String navn = domain + ":type=" + typeName + ",name=" + name;
		return createMBeanObjectNameFullyQualified(navn);
	}

	static ObjectName createMBeanObjectNameFullyQualified(String navn) {
		try {
			return new ObjectName(navn);
		} catch (MalformedObjectNameException e) {
			throw new IllegalArgumentException("Invalid mbean name [" + navn
					+ "]", e);
		}
	}

	public void registerMBean(ObjectName objectName, Object mbean) {
		try {
			if (mbs.isRegistered(objectName)) {
				log.error("MBean for " + objectName + " is already registered");
				return;
			}
			mbs.registerMBean(mbean, objectName);
		} catch (JMException e) {
			throw new IllegalArgumentException("MBean for " + objectName
					+ " is invalid", e);
		}
	}

	public void removeAllMBeans() {
		removeAllMBeans(domain + ":*");
	}

	public static void removeAllMBeans(String mbeanQuery) {
		Set<ObjectName> names = mbs.queryNames(
				createMBeanObjectNameFullyQualified(mbeanQuery), null);
		for (ObjectName objName : names) {
			unregister(objName);
		}
	}

	public MBeanInfo getMBeanInfo(ObjectName name) {
		try {
			return mbs.getMBeanInfo(name); // NOSONAR
		} catch (JMException e) {
			throw new IllegalArgumentException("Did not find info for name="
					+ name, e);
		}
	}

	public static void unregister(ObjectName objName) {
		try {
			mbs.unregisterMBean(objName);
		} catch (Exception ignored) { // NOSONAR
		}
	}

}
