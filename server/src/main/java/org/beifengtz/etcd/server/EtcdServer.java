package org.beifengtz.etcd.server;

import org.beifengtz.etcd.server.config.Configuration;
import org.beifengtz.etcd.server.service.HttpService;
import org.beifengtz.jvmm.common.util.CommonUtil;
import org.beifengtz.jvmm.common.util.IOUtil;
import org.beifengtz.jvmm.common.util.StringUtil;
import org.beifengtz.jvmm.common.util.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * description: TODO
 * date: 14:48 2023/5/23
 *
 * @author beifengtz
 */
public class EtcdServer {

    private static final String JVMM_URL = "https://github.com/tzfun/jvmm";

    private static void loadBanner() throws Exception {
        InputStream is = EtcdServer.class.getResourceAsStream("/banner.txt");
        if (is != null) {
            System.out.print(IOUtil.toString(is));
        }
        System.out.print("Powered by \u001b[0m\u001b[92mJVMM\u001b[0m\u001b[96m " + JVMM_URL + " \u001b[0m\n");
        System.out.printf("Framework version: \u001b[0m\u001b[93m%s\u001b[0m\n\n", CommonUtil.getJvmmVersion());
    }

    private static void loadConfiguration() throws Exception {
        String config = null;
        File file = new File(SystemPropertyUtil.get("user.dir"), "etcd-workbench.conf");
        if (file.exists()) {
            config = Files.readString(file.toPath());
        } else {
            InputStream is = EtcdServer.class.getResourceAsStream("/etcd-workbench.conf");
            if (is != null) {
                config = IOUtil.toString(is);
            }
        }
        if (config == null) {
            throw new ConfigurationException("No configuration");
        }
        String[] lines = config.split("\n");
        String part = null;
        for (String line : lines) {
            if (StringUtil.nonEmpty(line) && !line.startsWith("#")) {
                if (line.startsWith("[")) {
                    part = line.substring(1, line.lastIndexOf("]"));
                } else {
                    int i = line.indexOf("=");

                    String key = line.substring(0, i).trim();
                    String value = line.substring(i + 1).trim();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (StringUtil.isEmpty(value)) {
                        continue;
                    }

                    //  读取 server 配置
                    if ("server".equalsIgnoreCase(part)) {
                        if ("port".equalsIgnoreCase(key)) {
                            int port = Integer.parseInt(value);
                            if (port <= 0) {
                                throw new ConfigurationException("Parameter configuration error, illegal port " + port);
                            }
                            Configuration.INSTANCE.setPort(port);
                        } else if ("etcdExecuteTimeoutMillis".equals(key)) {
                            Configuration.INSTANCE.setEtcdExecuteTimeoutMillis(Integer.parseInt(value));
                        } else if ("dataDir".equalsIgnoreCase(key)) {
                            Configuration.INSTANCE.setDataDir(value);
                        } else if ("configEncryptKey".equalsIgnoreCase(key)) {
                            Configuration.INSTANCE.setConfigEncryptKey(value);
                        } else if ("enableHeartbeat".equalsIgnoreCase(key)) {
                            Configuration.INSTANCE.setEnableHeartbeat(Boolean.parseBoolean(value));
                        }
                    }
                    //  读取 auth 配置
                    else if ("auth".equalsIgnoreCase(part)) {
                        if ("enable".equalsIgnoreCase(key)) {
                            Configuration.INSTANCE.setEnableAuth(Boolean.parseBoolean(value));
                        } else if ("user".equalsIgnoreCase(key)) {
                            String[] split = value.split(":");
                            Configuration.INSTANCE.addUser(split[0], split[1]);
                        }
                    }
                    //  读取 log 配置
                    else if ("log".equalsIgnoreCase(part)) {
                        System.setProperty("jvmm.log." + key, value);
                    }
                }
            }
        }
        Configuration.INSTANCE.verify();
        System.setProperty(SystemPropertyUtil.PROPERTY_JVMM_SCAN_PACKAGE, "org.beifengtz.etcd.server");
        LoggerFactory.getLogger(EtcdServer.class).info("Load configuration successfully");
    }

    private static void bootstrap() {
        new HttpService().start(Configuration.INSTANCE.getPort());
        String[] versionInfo = org.beifengtz.etcd.server.util.CommonUtil.getVersionInfo();
        Logger logger = LoggerFactory.getLogger(EtcdServer.class);
        logger.info("Etcd workbench version: {}", versionInfo[0]);
        logger.info("Etcd workbench build hash: {}", versionInfo[1]);
    }

    public static void main(String[] args) throws Exception {
        loadBanner();
        loadConfiguration();
        bootstrap();
    }
}
