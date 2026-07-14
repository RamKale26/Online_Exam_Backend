package com.example.demo;

import com.example.demo.model.Category;
import com.example.demo.model.User;
import com.example.demo.model.SystemSetting;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    UserRepository userRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    SystemSettingRepository systemSettingRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        ensureSystemSettingsSchema();

        // Seed Admin
        if (!userRepository.existsByEmail("admin@gmail.com")) {
            User admin = new User();
            admin.setName("Admin");
            admin.setEmail("admin@gmail.com");
            admin.setPassword(encoder.encode("admin2341"));
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);
        }

        // Seed Categories
        List<String> defaultCategories = Arrays.asList(
            "Aptitude & Reasoning Test",
            "Programming and Technical Test",
            "General Knowledge (GK)",
            "Mock Placement Test"
        );

        for (String catName : defaultCategories) {
            if (categoryRepository.findByName(catName).isEmpty()) {
                Category category = new Category();
                category.setName(catName);
                category.setTimeLimitMinutes(10);
                category.setQuestionCount(10);
                category.setPassingPercentage(50);
                categoryRepository.save(category);
            }
        }

        // Seed Default System Settings
        if (systemSettingRepository.count() == 0) {
            SystemSetting settings = new SystemSetting();
            settings.setAllowRegistration(true);
            settings.setShowAnswersToStudents(true);
            systemSettingRepository.save(settings);
        }
    }

    private void ensureSystemSettingsSchema() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = database() and table_name = 'system_settings'",
                Integer.class);

        if (tableCount == null || tableCount == 0) {
            return;
        }

        Integer idColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_schema = database() and table_name = 'system_settings' and column_name = 'id'",
                Integer.class);

        if (idColumnCount != null && idColumnCount > 0) {
            return;
        }

        Integer primaryKeyCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.key_column_usage where table_schema = database() and table_name = 'system_settings' and constraint_name = 'PRIMARY'",
                Integer.class);

        if (primaryKeyCount != null && primaryKeyCount > 0) {
            jdbcTemplate.execute("alter table system_settings add column id bigint not null first");
            jdbcTemplate.execute("set @system_settings_id := 0");
            jdbcTemplate.execute("update system_settings set id = (@system_settings_id := @system_settings_id + 1)");
            return;
        }

        jdbcTemplate.execute("alter table system_settings add column id bigint not null auto_increment primary key first");
    }
}
