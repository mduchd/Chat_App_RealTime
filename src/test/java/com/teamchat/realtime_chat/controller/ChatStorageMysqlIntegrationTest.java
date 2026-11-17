package com.teamchat.realtime_chat.controller;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

// Opt-in suite for an isolated real MySQL database. Never points to production by default.
@EnabledIfEnvironmentVariable(named="CHAT_MYSQL_TEST_URL",matches=".+")
@TestPropertySource(properties={
 "spring.datasource.url=${CHAT_MYSQL_TEST_URL}", "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
 "spring.datasource.username=root", "spring.datasource.password=chat_test_only",
 "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
 "spring.session.jdbc.initialize-schema=never", "spring.session.jdbc.platform=mysql"
})
@Sql(statements="SELECT 1",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ChatStorageMysqlIntegrationTest extends ChatStorageIntegrationTest { }
