package com.ragdoc.platform.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ragdoc.platform.auth.dto.UserResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCacheServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserCacheService userCacheService;

    @Test
    void getById_returnsUserResponse() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("테스트");
        user.setPassword("hashed");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userCacheService.getById(1L);

        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.name()).isEqualTo("테스트");
        verify(userRepository, times(1)).findById(1L);
    }
}
