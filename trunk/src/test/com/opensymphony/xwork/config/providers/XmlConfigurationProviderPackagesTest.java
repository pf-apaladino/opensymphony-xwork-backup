/*
 * Copyright (c) 2002-2003 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork.config.providers;

import com.opensymphony.xwork.config.ConfigurationException;
import com.opensymphony.xwork.config.ConfigurationProvider;
import com.opensymphony.xwork.config.RuntimeConfiguration;
import com.opensymphony.xwork.config.entities.PackageConfig;

import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: May 6, 2003
 * Time: 3:10:16 PM
 * To change this template use Options | File Templates.
 */
public class XmlConfigurationProviderPackagesTest extends ConfigurationTestBase {

    public void testBadInheritance() throws ConfigurationException {
        final String filename = "com/opensymphony/xwork/config/providers/xwork-test-bad-inheritance.xml";
        ConfigurationProvider provider = buildConfigurationProvider(filename);
        provider.init(configuration);
    }

    public void testBasicPackages() throws ConfigurationException {
        final String filename = "com/opensymphony/xwork/config/providers/xwork-test-basic-packages.xml";
        ConfigurationProvider provider = buildConfigurationProvider(filename);
        provider.init(configuration);

        // setup our expectations
        PackageConfig expectedNamespacePackage = new PackageConfig("namespacepkg", "/namespace/set", false, null);
        PackageConfig expectedAbstractPackage = new PackageConfig("abstractpkg", null, true, null);

        // test expectations
        assertEquals(3, configuration.getPackageConfigs().size());
        assertEquals(expectedNamespacePackage, configuration.getPackageConfig("namespacepkg"));
        assertEquals(expectedAbstractPackage, configuration.getPackageConfig("abstractpkg"));
    }

    public void testDefaultPackage() throws ConfigurationException {
        final String filename = "com/opensymphony/xwork/config/providers/xwork-test-default-package.xml";
        ConfigurationProvider provider = buildConfigurationProvider(filename);
        provider.init(configuration);

        // setup our expectations
        PackageConfig expectedPackageConfig = new PackageConfig("default");

        // test expectations
        assertEquals(1, configuration.getPackageConfigs().size());
        assertEquals(expectedPackageConfig, configuration.getPackageConfig("default"));
    }

    public void testPackageInheritance() throws ConfigurationException {
        final String filename = "com/opensymphony/xwork/config/providers/xwork-test-package-inheritance.xml";
        ConfigurationProvider provider = buildConfigurationProvider(filename);

        provider.init(configuration);

        // test expectations
        assertEquals(4, configuration.getPackageConfigs().size());
        PackageConfig defaultPackage = configuration.getPackageConfig("default");
        assertNotNull(defaultPackage);
        assertEquals("default", defaultPackage.getName());
        PackageConfig abstractPackage = configuration.getPackageConfig("abstractPackage");
        assertNotNull(abstractPackage);
        assertEquals("abstractPackage", abstractPackage.getName());
        PackageConfig singlePackage = configuration.getPackageConfig("singleInheritance");
        assertNotNull(singlePackage);
        assertEquals("singleInheritance", singlePackage.getName());
        assertEquals(1, singlePackage.getParents().size());
        assertEquals(defaultPackage, singlePackage.getParents().get(0));
        PackageConfig multiplePackage = configuration.getPackageConfig("multipleInheritance");
        assertNotNull(multiplePackage);
        assertEquals("multipleInheritance", multiplePackage.getName());
        assertEquals(3, multiplePackage.getParents().size());
        List multipleParents = multiplePackage.getParents();
        assertTrue(multipleParents.contains(defaultPackage));
        assertTrue(multipleParents.contains(abstractPackage));
        assertTrue(multipleParents.contains(singlePackage));

        configurationManager.addConfigurationProvider(provider);

        RuntimeConfiguration runtimeConfiguration = configurationManager.getConfiguration().getRuntimeConfiguration();
        assertNotNull(runtimeConfiguration.getActionConfig("/multiple", "default"));
        assertNotNull(runtimeConfiguration.getActionConfig("/multiple", "abstract"));
        assertNotNull(runtimeConfiguration.getActionConfig("/multiple", "single"));
        assertNotNull(runtimeConfiguration.getActionConfig("/multiple", "multiple"));
        assertNotNull(runtimeConfiguration.getActionConfig("/single", "default"));
        assertNull(runtimeConfiguration.getActionConfig("/single", "abstract"));
        assertNotNull(runtimeConfiguration.getActionConfig("/single", "single"));
        assertNull(runtimeConfiguration.getActionConfig("/single", "multiple"));

    }
}
