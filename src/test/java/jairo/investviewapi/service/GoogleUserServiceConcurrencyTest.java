package jairo.investviewapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import jairo.investviewapi.entity.User;
import jairo.investviewapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GoogleUserServiceConcurrencyTest {

    @Autowired
    private GoogleUserService googleUserService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldProvisionSameEmailConcurrentlyWithoutDuplicates() throws Exception {
        String email = "race-" + UUID.randomUUID() + "@example.com";
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<User>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return googleUserService.provision(email, "Race User", "google-sub-race");
                }));
            }
            start.countDown();

            List<Long> ids = new ArrayList<>();
            for (Future<User> future : futures) {
                User user = future.get(10, TimeUnit.SECONDS);
                ids.add(user.getId());
            }

            assertThat(ids).doesNotContainNull();
            assertThat(ids.stream().distinct().count()).isEqualTo(1);
            assertThat(userRepository.findByEmail(email)).isPresent();
            assertThat(userRepository.findByEmail(email).orElseThrow().getGoogleSub())
                    .isEqualTo("google-sub-race");
        } finally {
            executor.shutdownNow();
        }
    }
}
