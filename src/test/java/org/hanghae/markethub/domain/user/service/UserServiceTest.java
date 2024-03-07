 package org.hanghae.markethub.domain.user.service;

 import org.hanghae.markethub.domain.user.dto.UserRequestDto;
 import org.hanghae.markethub.domain.user.dto.UserResponseDto;
 import org.hanghae.markethub.domain.user.entity.User;
 import org.hanghae.markethub.domain.user.repository.UserRepository;
 import org.hanghae.markethub.global.constant.Role;
 import org.junit.jupiter.api.*;
 import org.junit.jupiter.api.extension.ExtendWith;
 import org.mockito.InjectMocks;
 import org.mockito.Mock;
 import org.mockito.junit.jupiter.MockitoExtension;
 import org.springframework.security.crypto.password.PasswordEncoder;

 import java.util.Arrays;
 import java.util.List;
 import java.util.Optional;

 import static org.assertj.core.api.Assertions.assertThat;
 import static org.hanghae.markethub.global.constant.Status.DELETED;
 import static org.junit.jupiter.api.Assertions.*;
 import static org.mockito.ArgumentMatchers.any;
 import static org.mockito.Mockito.when;

 @ExtendWith(MockitoExtension.class)
 class UserServiceTest {
     @Mock
     private UserRepository userRepository;

     @Mock
     PasswordEncoder passwordEncoder;

     @InjectMocks
     private UserService userService;

     private UserRequestDto userRequestDto;
     private UserResponseDto responseDto;

     @BeforeEach
     void setUp() {
         userRequestDto = UserRequestDto.builder()
                 .email("test@example.com")
                 .password("password")
                 .name("Test User")
                 .phone("123-456-7890")
                 .address("123 Test St")
                 .role(Role.USER)
                 .build();

         responseDto = UserResponseDto.builder()
                 .id(1L)
                 .email("test@example.com")
                 .name("Test User")
                 .phone("123-456-7890")
                 .address("123 Test St")
                 .role(Role.USER)
                 .build();
     }

     @AfterEach
     void tearDown() {
         // 테스트 종료 후 데이터 삭제
         userRepository.deleteAll();
     }

     @Test
     @DisplayName("사용자 생성")
     void createUser() {
         // given
         when(passwordEncoder.encode(userRequestDto.getPassword())).thenReturn("encodedPassword");

         // when
         boolean res = userService.createUser(userRequestDto);

         // then

         assertEquals(true, res);
     }

     @Test
     @DisplayName("사용자 생성 실패 - 중복된 이메일")
     void createUserFail() {
         // given
         when(passwordEncoder.encode(userRequestDto.getPassword())).thenReturn("encodedPassword");
         when(userRepository.existsByEmail(userRequestDto.getEmail())).thenReturn(true);
         // when

         // then
         // 중복된 이메일로 사용자 생성 시 예외 발생
         Assertions.assertThrows(IllegalArgumentException.class, () -> userService.createUser(userRequestDto));
     }


     @Test
     @DisplayName("사용자 조회 성공")
     void getUser() {
     // given
         User user = User.builder()
                 .id(1L)
                 .email("test@example.com")
                 .name("Test User")
                 .phone("123-456-7890")
                 .address("123 Test St")
                 .role(Role.USER)
                 .build();

         // userRepository.findById(any())가 Optional<User>를 반환하도록 스텁
         when(userRepository.findById(any())).thenReturn(Optional.of(user));

         // when
         UserResponseDto userResponseDto = userService.getUser(responseDto.getId());

         // then
         assertThat(responseDto).isNotNull(); // 사용자가 생성되었는지 확인
         assertThat(userResponseDto).isNotNull();
         assertThat(userResponseDto.getEmail()).isEqualTo(responseDto.getEmail());
         assertThat(userResponseDto.getName()).isEqualTo(responseDto.getName());
     }

     @Test
     @DisplayName("사용자 조회 실패 - 없는 사용자")
     void getUserFail() {
         // given
         when(userRepository.findById(any())).thenReturn(Optional.empty());

         // when
         // 존재하지 않는 사용자 조회 시 예외 발생
         Assertions.assertThrows(IllegalArgumentException.class, () -> userService.getUser(-1L));
     }

     @Test
     @DisplayName("모든 사용자 조회")
     void getAllUsers() {
         // given
         User user = User.builder()
                 .id(1L)
                 .email("test@example.com")
                 .name("Test User")
                 .phone("123-456-7890")
                 .address("123 Test St")
                 .role(Role.USER)
                 .build();

         User user2 = User.builder()
                 .id(1L)
                 .email("test@example.com")
                 .name("Test User")
                 .phone("123-456-7890")
                 .address("123 Test St")
                 .role(Role.USER)
                 .build();
         List<User> users = Arrays.asList(user, user2);
         when(userRepository.findAll()).thenReturn(users);

         // when
         List<UserResponseDto> allUsers = userService.getAllUsers();

         // then
         assertThat(allUsers).isNotNull();
         assertThat(allUsers).hasSize(2); // 생성된 모든 사용자 수가 2개인지 확인
     }

     @Test
     @DisplayName("사용자 정보 수정")
     void testUpdateUser() {
         // given
         when(userRepository.findById(any())).thenReturn(Optional.of(User.builder()
                 .id(1L)
                 .email("test@example.com")
                 .name("Test User")
                 .phone("123-456-7890")
                 .address("123 Test St")
                 .role(Role.USER)
                 .build()));


         // update 할 정보
         UserRequestDto updateUserRequestDto = UserRequestDto.builder()
                 .email("updated@example.com")
                 .password("newpassword")
                 .name("Updated User")
                 .phone("987-654-3210")
                 .address("456 Updated St")
                 .role(Role.ADMIN)
                 .build();

         // when
         UserResponseDto updatedUser = userService.updateUser(1L, updateUserRequestDto);

         // then
         assertThat(updatedUser).isNotNull();
         assertThat(updatedUser.getEmail()).isEqualTo("test@example.com"); // 이메일은 변경되지 않았는지 확인
         assertThat(updatedUser.getEmail()).isNotEqualTo(updateUserRequestDto.getEmail());
         assertThat(updatedUser.getName()).isEqualTo("Test User"); // 이름은 변경되지 않았는지 확인
         assertThat(updatedUser.getName()).isNotEqualTo(updateUserRequestDto.getName());
         assertThat(updatedUser.getPhone()).isEqualTo(updateUserRequestDto.getPhone());
         assertThat(updatedUser.getAddress()).isEqualTo(updateUserRequestDto.getAddress());
         assertThat(updatedUser.getRole()).isEqualTo(Role.USER); // 역할은 변경되지 않았는지 확인
         assertThat(updatedUser.getRole()).isNotEqualTo(updateUserRequestDto.getRole());
     }

     @Test
     @DisplayName("사용자 삭제")
     void testDeleteUser() {
         // given
         when(userRepository.findById(any())).thenReturn(Optional.of(User.builder()
                 .id(1L)
                 .email("test@example.com")
                 .name("Test User")
                 .phone("123-456-7890")
                 .address("123 Test St")
                 .role(Role.USER)
                 .build()));
         // when
         userService.deleteUser(1L);

         // then
         // 사용자 삭제 후 findById로 조회 시 null이 반환되는지 확인
         User deletedUser = userRepository.findById(1L).orElse(null);
         assertThat(deletedUser.getStatus()).isEqualTo(DELETED);
     }

     @Test
     @DisplayName("DeleteUser 이후, 같은 email로 유저 생성")
     void testDeleteUserAndCreateUser() {
         // given
         when(userRepository.existsByEmail(userRequestDto.getEmail())).thenReturn(true);

         // when

         // then
         // 사용자 삭제 후 같은 email로 생성시, 생성이 안 되는지 확인
         Assertions.assertThrows(IllegalArgumentException.class, () -> userService.createUser(userRequestDto));
     }

     @Test
     @DisplayName("DeleteUser 이후, 수정 안되는지 확인")
     void testDeleteUserAndUpdateUser() {
         // given
         when(userRepository.findById(any())).thenReturn(Optional.of(User.builder()
                 .id(1L)
                 .email("test@example.com")
                 .name("Test User")
                 .phone("123-456-7890")
                 .address("123 Test St")
                 .role(Role.USER)
                 .status(DELETED) // 이미 삭제되었다 가정
                 .build()));

         // update 할 정보
         UserRequestDto updateUserRequestDto = UserRequestDto.builder()
                 .email("updated@example.com")
                 .password("newpassword")
                 .name("Updated User")
                 .phone("987-654-3210")
                 .address("456 Updated St")
                 .role(Role.ADMIN)
                 .build();


         // when
         when(userRepository.findById(1L)).thenReturn(Optional.empty());
         // then
         // 사용자 삭제 후 같은 email로 생성시, 생성이 안 되는지 확인
         Assertions.assertThrows(IllegalArgumentException.class, () -> userService.updateUser(1L,updateUserRequestDto));
     }

     @Test
     @DisplayName("이메일로 사용자 조회")
     void getUserByEmail() {
         // given
         when(userRepository.findByEmail(any())).thenReturn(Optional.of(User.builder()
                 .id(1L)
                 .email("test@example.com")
                 .name("Test User")
                 .phone("123-456-7890")
                 .address("123 Test St")
                 .role(Role.USER)
                 .build()));
         // when
         UserResponseDto foundUser = userService.getUserByEmail("test@example.com");

         // then
         assertThat(foundUser).isNotNull();
         assertThat(foundUser.getEmail()).isEqualTo("test@example.com");
         assertThat(foundUser.getName()).isEqualTo("Test User");
         assertThat(foundUser.getPhone()).isEqualTo("123-456-7890");
         assertThat(foundUser.getAddress()).isEqualTo("123 Test St");
         assertThat(foundUser.getRole()).isEqualTo(Role.USER);
     }
 }
