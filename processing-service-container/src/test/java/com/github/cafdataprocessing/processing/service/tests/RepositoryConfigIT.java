/*
 * Copyright 2017-2018 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 /*
 * Copyright 2017-2018 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cafdataprocessing.processing.service.tests;

import com.github.cafdataprocessing.processing.service.client.ApiClient;
import com.github.cafdataprocessing.processing.service.client.ApiException;
import com.github.cafdataprocessing.processing.service.client.api.GlobalConfigurationApi;
import com.github.cafdataprocessing.processing.service.client.api.RepositoryConfigurationApi;
import com.github.cafdataprocessing.processing.service.client.api.TenantConfigurationApi;
import com.github.cafdataprocessing.processing.service.client.model.EffectiveRepositoryConfigValue;
import com.github.cafdataprocessing.processing.service.client.model.EffectiveRepositoryConfigValue.ValueTypeEnum;
import com.github.cafdataprocessing.processing.service.client.model.EffectiveRepositoryConfigs;
import com.github.cafdataprocessing.processing.service.client.model.GlobalConfig;
import com.github.cafdataprocessing.processing.service.client.model.GlobalConfig.ScopeEnum;
import com.github.cafdataprocessing.processing.service.client.model.GlobalConfigsEntry;
import com.github.cafdataprocessing.processing.service.client.model.RepositoryConfig;
import com.github.cafdataprocessing.processing.service.client.model.RepositoryConfigs;
import com.github.cafdataprocessing.processing.service.tests.utils.ApiClientProvider;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class RepositoryConfigIT
{
    public static final ApiClient API_CLIENT = ApiClientProvider.getApiClient();
    public static final ApiClient API_CLIENT_TWO = ApiClientProvider.getApiClient();
    public static final ApiClient API_CLIENT_THREE = ApiClientProvider.getApiClient();
    public static final RepositoryConfigurationApi REPOSITORY_API = new RepositoryConfigurationApi(API_CLIENT);
    private static final GlobalConfigurationApi GLOBAL_CONFIG_API = new GlobalConfigurationApi(API_CLIENT_TWO);
    private static final TenantConfigurationApi TENANT_API = new TenantConfigurationApi(API_CLIENT_THREE);

    @AfterMethod
    public void cleanUp() throws ApiException
    {
        //delete all records after tests
        for (final GlobalConfigsEntry config : GLOBAL_CONFIG_API.getGlobalConfigs()) {
            GLOBAL_CONFIG_API.deleteGlobalConfig(config.getKey());
        }
    }

    @Test(description = "Check creation and retrieval of global config with different scopes")
    public void creationAndRetrievalOfGlobalConfig() throws ApiException
    {
        createConfigInStore("key_1", "value_1", "description_1", ScopeEnum.TENANT);
        final GlobalConfig globalConfig = GLOBAL_CONFIG_API.getGlobalConfig("key_1");
        Assert.assertEquals(globalConfig.getDescription(), "description_1");
        // second attempt but with REPOSITORY scope
        createConfigInStore("key_2", "value_2", "description_2", ScopeEnum.REPOSITORY);
        final GlobalConfig globalConfig2 = GLOBAL_CONFIG_API.getGlobalConfig("key_2");
        Assert.assertEquals(globalConfig2.getDescription(), "description_2");
    }

    @Test(description = "Check scope value")
    public void checkScopeValues() throws ApiException
    {
        createConfigInStore("key_1", "value_1", "description_1", ScopeEnum.TENANT);
        final GlobalConfig globalConfig = GLOBAL_CONFIG_API.getGlobalConfig("key_1");
        Assert.assertEquals(globalConfig.getScope(), ScopeEnum.TENANT);
        // second attempt but with REPOSITORY scope
        createConfigInStore("key_2", "value_2", "description_2", ScopeEnum.REPOSITORY);
        final GlobalConfig globalConfig2 = GLOBAL_CONFIG_API.getGlobalConfig("key_2");
        Assert.assertEquals(globalConfig2.getScope(), ScopeEnum.REPOSITORY);
    }

    @Test(description = "Simple creation of a repository config and check the scope of the underlying global config")
    public void simpleRepositoryConfig() throws ApiException
    {
        // create the global config
        createConfigInStore("key_3", "value_3", "description_3", ScopeEnum.REPOSITORY);
        final GlobalConfig globalConfig3 = GLOBAL_CONFIG_API.getGlobalConfig("key_3");
        Assert.assertEquals(globalConfig3.getScope(), ScopeEnum.REPOSITORY);

        // create the repository config and then get its value
        REPOSITORY_API.setRepositoryConfig("tenantId_3", "repository_3", "key_3", "repo_description_3");
        final String repositoryConfig = REPOSITORY_API.getRepositoryConfig("tenantId_3", "repository_3", "key_3");
        Assert.assertEquals(repositoryConfig, "repo_description_3");
        
        //Clean up & check
        REPOSITORY_API.deleteRepositoryConfig("tenantId_3", "repository_3", "key_3");
        try {
            REPOSITORY_API.getRepositoryConfig("tenantId_3", "repository_3", "key_3");
        } catch (final ApiException ex) {
            Assert.assertEquals(ex.getCode(), 404);
        }
    }

    @Test(description = "This test verifies that the autogenerated client can make a call through the api to create a repository specific"
        + " configuration")
    public void creationAndRetrievalOfRepositoryConfig() throws ApiException
    {
        final String tenantId = "test_1";
        final String configKey = "ee.grammarMap_1";
        final String configValue = "{\"pii.xml\": []}";
        final String repositoryId = "123456_1";
        final String repositoryValue = "repo_pii.xml";

        createConfigInStore(configKey, configValue, "Some description of the config for testing", ScopeEnum.REPOSITORY);
        REPOSITORY_API.setRepositoryConfig(tenantId, repositoryId, configKey, repositoryValue);
        final String repositoryConfig = REPOSITORY_API.getRepositoryConfig(tenantId, repositoryId, configKey);
        final GlobalConfig globalConfigValue = GLOBAL_CONFIG_API.getGlobalConfig(configKey);
        Assert.assertEquals(repositoryValue, repositoryConfig,
                            "Expecting repository config value returned to match the one that was provided during the set");

        //Clean up & check
        REPOSITORY_API.deleteRepositoryConfig(tenantId, repositoryId, configKey);
        try {
            REPOSITORY_API.getRepositoryConfig(tenantId, repositoryId, configKey);
        } catch (final ApiException ex) {
            Assert.assertEquals(ex.getCode(), 404);
        }
    }

    @Test(description = "This test verifies that the autogenerated client can make a call through the api to create a repository specific"
        + " configuration and then update that same configuration with a new value.")
    public void updateAndRetrievalOfRepositoryConfig() throws ApiException
    {
        final String tenantId = "1";
        final String configKey = "ee.grammarMap";
        final String firstConfigValue = "{\"pii.xml\": []}";
        final String secondConfigValue = "{'pii.xml': ['internet.ecr']}";
        final String repositoryId = "123456";

        createConfigInStore(configKey, "SomeValue", "Some description of the config for testing", ScopeEnum.REPOSITORY);
        REPOSITORY_API.setRepositoryConfig(tenantId, repositoryId, configKey, firstConfigValue);
        final String repositoryConfigValue = REPOSITORY_API.getRepositoryConfig(tenantId, repositoryId, configKey);
        Assert.assertEquals(repositoryConfigValue, firstConfigValue, "Asserting first repository config value was set correctly");

        REPOSITORY_API.setRepositoryConfig(tenantId, repositoryId, configKey, secondConfigValue);
        final String secondRepositoryConfigValue = REPOSITORY_API.getRepositoryConfig(tenantId, repositoryId, configKey);
        Assert.assertEquals(secondRepositoryConfigValue, secondConfigValue,
                            "Asserting first tenant config value was updated correctly");

        //Clean up & check
        REPOSITORY_API.deleteRepositoryConfig(tenantId, repositoryId, configKey);
        try {
            REPOSITORY_API.getRepositoryConfig(tenantId, repositoryId, configKey);
        } catch (final ApiException ex) {
            Assert.assertEquals(ex.getCode(), 404);
        }
    }

    @Test(description = "This test verifies that the autogenerated client can make a call through the api to create multiple repositories"
        + " specific configurations and then verifies the get configurations call returns the same ones.")
    public void bulkCreationAndRetrievalOfRepositoryConfig() throws ApiException
    {
        final RepositoryConfigs repositoryConfigs = new RepositoryConfigs();
        final String tenantId = "bulk_1";
        final String repositoryId = "123456";
        final String configKey = "ee.grammarMap";
        final String configValue = "{\"pii.xml\": []}";
        for (int i = 0; i < 5; i++) {
            final RepositoryConfig config = new RepositoryConfig();
            final String key = configKey + i;
            config.setKey(key);
            config.setValue(configValue + i);
            repositoryConfigs.add(config);
            createConfigInStore(key, "SomeValue", "Some description of the config for testing", ScopeEnum.REPOSITORY);
        }
        REPOSITORY_API.setRepositoryConfigs(tenantId, repositoryId, repositoryConfigs);
        final RepositoryConfigs returnedRepositoryConfigs = REPOSITORY_API.getRepositoryConfigs(tenantId, repositoryId);

        repositoryConfigs.forEach((config) -> {
            Assert.assertTrue(returnedRepositoryConfigs.contains(config));
        });

        //Clean up & check
        for (int i = 0; i < 5; i++) {
            final String key = configKey + i;
            REPOSITORY_API.deleteRepositoryConfig(tenantId, repositoryId, key);
            try {
                REPOSITORY_API.getRepositoryConfig(tenantId, repositoryId, key);
            } catch (final ApiException ex) {
                Assert.assertEquals(ex.getCode(), 404);
            }
        }
    }

    @Test(description = "Verifies that the effective config call will return the effective config for a config that is not set.")
    public void effectiveConfig() throws ApiException
    {
        final String tenantId = "DefaultCheck";
        final String configKey = "ee.grammarMap";
        final String globalConfigValue = "{\"pii.xml\": []}";
        final String repositoryId = "123456";

        createConfigInStore(configKey, globalConfigValue, "Some description of the config for testing", ScopeEnum.REPOSITORY);

        final EffectiveRepositoryConfigValue configValue = REPOSITORY_API.getEffectiveRepositoryConfig(tenantId, repositoryId, configKey);

        Assert.assertTrue(configValue.getValueType().equals(ValueTypeEnum.DEFAULT));
        Assert.assertEquals(configValue.getValue(), globalConfigValue);
    }
    
    @Test(description = "Verifies the effective configs returned if the repo is present, if the tenant is present, or if only the "
        + "global config is present.")
    public void effectiveChainOfConfigs() throws ApiException
    {
        final String tenantId = "DefaultCheck";
        final String configKey = "ee.grammarMap";
        final String globalConfigValue = "{\"pii.xml\": []}";
        final String repositoryId = "123456";

        createConfigInStore(configKey, globalConfigValue, "Some description of the config for testing", ScopeEnum.REPOSITORY);

        final EffectiveRepositoryConfigValue configValue = REPOSITORY_API.getEffectiveRepositoryConfig(tenantId, repositoryId, configKey);

        Assert.assertTrue(configValue.getValueType().equals(ValueTypeEnum.DEFAULT));
        Assert.assertEquals(configValue.getValue(), globalConfigValue);
        
        TENANT_API.setTenantConfig(tenantId, configKey, "from_the_tenant");
        
        final EffectiveRepositoryConfigValue configValue2 = 
            REPOSITORY_API.getEffectiveRepositoryConfig(tenantId, repositoryId, configKey);
        
        Assert.assertTrue(configValue2.getValueType().equals(ValueTypeEnum.CUSTOM));
        Assert.assertEquals(configValue2.getValue(), "from_the_tenant");
        
        REPOSITORY_API.setRepositoryConfig(tenantId, repositoryId, configKey, "from_the_repo");
        
        final EffectiveRepositoryConfigValue configValue3 = 
            REPOSITORY_API.getEffectiveRepositoryConfig(tenantId, repositoryId, configKey);
        
        Assert.assertTrue(configValue3.getValueType().equals(ValueTypeEnum.CUSTOM));
        Assert.assertEquals(configValue3.getValue(), "from_the_repo");
        
        TENANT_API.deleteTenantConfigs(tenantId);
        REPOSITORY_API.deleteRepositoryConfigs(tenantId, repositoryId);
        Assert.assertTrue(TENANT_API.getTenantConfigs(tenantId).isEmpty());
        Assert.assertTrue(REPOSITORY_API.getRepositoryConfigs(tenantId, repositoryId).isEmpty());
    }

    @Test(description = "This test verifies that the autogenerated client can make a call through the api to create a repository specific"
        + " configuration and then delete that same configuration. This test will also verify that the get effective config call will "
        + "return the repository specific value if set but if it isn't that it will return the default.")
    public void deleteOfRepositoryConfig() throws ApiException
    {
        final String tenantId = "1";
        final String configKey = "ee.grammarMap";
        final String configValue = "SomeTestValue";
        final String repositoryId = "123456";
        createConfigInStore(configKey, configValue, "Some description of the config for testing", ScopeEnum.REPOSITORY);
        REPOSITORY_API.setRepositoryConfig(tenantId, repositoryId, configKey, configValue);
        final EffectiveRepositoryConfigValue repositoryConfigValue = REPOSITORY_API
            .getEffectiveRepositoryConfig(tenantId, repositoryId, configKey);
        Assert.assertEquals(repositoryConfigValue.getValue(), configValue);
        Assert.assertEquals(repositoryConfigValue.getValueType(), ValueTypeEnum.CUSTOM);

        REPOSITORY_API.deleteRepositoryConfig(tenantId, repositoryId, configKey);
        Assert.assertTrue(REPOSITORY_API.getRepositoryConfigs(tenantId, repositoryId).isEmpty());
    }

    @Test(description = "Tests that a repository specific config cannot be created when there is no corresponding global config")
    public void createRepositoryConfigWithNoCorrespondingGlobalConfig()
    {
        final String tenantId = "1";
        final String configKey = "ee.grammarMap";
        final String configValue = "{\"pii.xml\": []}";
        final String repositoryId = "123456";

        try {
            REPOSITORY_API.setRepositoryConfig(tenantId, repositoryId, configKey, configValue);
        } catch (final ApiException ex) {
            Assert.assertEquals(ex.getCode(), 405);
        }
    }

    @Test(description = "Tests that the effective configs are returned for a user even though no repository specific config was created.")
    public void getEffectiveConfigs() throws ApiException
    {
        final String tenantId = "EffectiveConfigsTest";
        final String configKey = "ee.grammarMapConfigsTest";
        final String configValue = "{\"pii.xml\": []}";
        final String repositoryId = "123456ConfigsTest";
        createConfigInStore(configKey, configValue, "Some description of the config for testing", ScopeEnum.REPOSITORY);

        final EffectiveRepositoryConfigs configs = REPOSITORY_API.getEffectiveRepositoryConfigs(tenantId, repositoryId);
        Assert.assertFalse(configs.isEmpty());
    }

    @Test(description = "Tests that the effective configs are returned for a user along side their repository specific configs.")
    public void getEffectiveConfigList() throws ApiException
    {
        final String tenantId = "EffectiveConfigTest";
        final String configKey = "ee.grammarMap";
        final String configValue = "{\"pii.xml\": []}";
        final String repositoryId = "123456";
        final String secondConfigKey = "AnotherTestConfig";
        final String secondConfigValue = "AnotherTestConfigValue";
        createConfigInStore(configKey, configValue, "Some description of the config for testing", ScopeEnum.REPOSITORY);
        createConfigInStore(secondConfigKey, configValue, "Some description of the config for testing", ScopeEnum.REPOSITORY);

        REPOSITORY_API.setRepositoryConfig(tenantId, repositoryId, secondConfigKey, secondConfigValue);

        final EffectiveRepositoryConfigs configs = REPOSITORY_API.getEffectiveRepositoryConfigs(tenantId, repositoryId);

        Assert.assertFalse(configs.isEmpty());

        configs.forEach((config) -> {
            if (config.getKey().equals(configKey)) {
                Assert.assertEquals(config.getValue(), configValue);
                Assert.assertEquals(config.getValueType(), ValueTypeEnum.DEFAULT);
            } else {
                Assert.assertEquals(config.getValue(), secondConfigValue);
                Assert.assertEquals(config.getValueType(), ValueTypeEnum.CUSTOM);
            }
        });
        REPOSITORY_API.deleteRepositoryConfig(tenantId, repositoryId, secondConfigKey);
        Assert.assertTrue(REPOSITORY_API.getRepositoryConfigs(tenantId, repositoryId).isEmpty());
    }

    @Test(description = "Tests that an invalid param triggers a 400 response.")
    public void testInvalidParams() throws ApiException
    {
        final String configKey = "ee.grammarMap";
        final String configValue = "{\"pii.xml\": []}";
        final String tenantId = "tenant_1";

        createConfigInStore(configKey, configValue, "Some description of the config for testing", ScopeEnum.REPOSITORY);

        String repositoryId = "";
        for (int i = 0; i < 260; i++) {
            repositoryId = repositoryId + i;
        }

        try {
            REPOSITORY_API.getEffectiveRepositoryConfigs(tenantId, repositoryId);
        } catch (final ApiException ex) {
            Assert.assertTrue(ex.getCode() == 400);
        }
    }

    @Test(description = "Check that a repository configuration is not inserted, if the corresponding global config has scope == 0")
    public void testScope() throws ApiException
    {
        final String tenantId = "EffectiveConfigTest";
        final String configKey = "ee.grammarMap";
        final String configValue = "{\"pii.xml\": []}";
        final String repositoryId = "123456";

        createConfigInStore(configKey, configValue, "Some description of the config for testing", ScopeEnum.TENANT);

        try {
            REPOSITORY_API.setRepositoryConfig(tenantId, repositoryId, configKey, configValue);
        } catch (final ApiException ex) {
            Assert.assertEquals(ex.getCode(), 405);
        }

    }

    private void createConfigInStore(final String key, final String value, final String description, final ScopeEnum scope)
        throws ApiException
    {
        GLOBAL_CONFIG_API.setGlobalConfig(key, buildGlobalConfig(value, description, scope));
    }

    private GlobalConfig buildGlobalConfig(String value, String descrtiption, ScopeEnum scope)
    {
        final GlobalConfig config = new GlobalConfig();
        config.setDefault(value);
        config.setDescription(descrtiption);
        config.setScope(scope);
        return config;
    }
}
