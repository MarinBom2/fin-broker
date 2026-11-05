package com.marinbom2.finbroker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * NoColumnCheckMySQLDialect 검증. enum 칼럼이 varchar 로 만들어지고 값 목록 CHECK 제약이 없어야 한다.
 * 그래야 enum 에 값을 추가해도 ddl-auto=update 환경에서 새 값을 저장할 수 있다.
 * Docker 데몬이 없으면 건너뛴다.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create")
@Testcontainers(disabledWithoutDocker = true)
class EnumColumnSchemaTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void enumColumnsAreVarcharWithoutCheckConstraints() {
        String schema = jdbc.queryForObject("select database()", String.class);

        String roleType = jdbc.queryForObject("""
                select data_type from information_schema.columns
                where table_schema = ? and table_name = 'member' and column_name = 'role_type'
                """, String.class, schema);
        List<String> nativeEnumColumns = jdbc.queryForList("""
                select concat(table_name, '.', column_name) from information_schema.columns
                where table_schema = ? and data_type = 'enum'
                """, String.class, schema);
        List<String> checkConstraints = jdbc.queryForList("""
                select constraint_name from information_schema.check_constraints
                where constraint_schema = ?
                """, String.class, schema);

        assertThat(roleType).isEqualTo("varchar");
        assertThat(nativeEnumColumns).isEmpty();
        assertThat(checkConstraints).isEmpty();
    }
}
