 package org.hanghae.markethub.domain.user.controller;

 import com.fasterxml.jackson.databind.ObjectMapper;
 import org.hanghae.markethub.domain.user.dto.UserRequestDto;
 import org.hanghae.markethub.domain.user.dto.UserResponseDto;
 import org.hanghae.markethub.domain.user.entity.User;
 import org.hanghae.markethub.domain.user.repository.UserRepository;
 import org.hanghae.markethub.domain.user.service.UserService;
 import org.hanghae.markethub.global.constant.Role;
 import org.hanghae.markethub.global.security.impl.UserDetailsImpl;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.DisplayName;
 import org.junit.jupiter.api.Test;
 import org.junit.runner.RunWith;
 import org.mockito.InjectMocks;
 import org.mockito.Mock;
 import org.mockito.MockitoAnnotations;
 import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
 import org.springframework.http.MediaType;
 import org.springframework.security.test.context.support.WithMockUser;
 import org.springframework.test.context.junit4.SpringRunner;
 import org.springframework.test.web.servlet.MockMvc;
 import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
 import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
 import org.springframework.test.web.servlet.setup.MockMvcBuilders;

 import static org.mockito.Mockito.*;
 import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
 import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

 @RunWith(SpringRunner.class)
 @AutoConfigureMockMvc
 class UserControllerTest {

     private MockMvc mockMvc;

     AutoCloseable openMocks;

     @InjectMocks
     private UserController userController;

     @Mock
     private UserService userService;

     @Mock
     private UserRepository userRepository;

     @Mock
     private UserDetailsImpl userDetails;

     private UserRequestDto userRequestDto;
     private UserResponseDto userResponseDto;
     private User user;

     @BeforeEach
     void setUp() {

         openMocks = MockitoAnnotations.openMocks(this);
         mockMvc = MockMvcBuilders.standaloneSetup(userController).build();

         userRequestDto = UserRequestDto.builder()
                 .email("test@example.com")
                 .password("password")
                 .name("Test User")
                 .phone("123-456-7890")
                 .address("123 Test St")
                 .role(Role.USER)
                 .build();

         userResponseDto = UserResponseDto.builder()
                 .id(1L)
                 .email("test@example.com")
                 .name("Test User")
                 .phone("123-456-7890")
                 .address("123 Test St")
                 .role(Role.USER)
                 .build();

         user = User.builder()
                 .id(1L)
                 .email("test@example.com")
                 .password("password")
                 .name("Test User")
                 .phone("123-456-7890")
                 .address("123 Test St")
                 .role(Role.USER)
                 .build();


     }

     @Test
     @DisplayName("회원 생성 성공")
     void createUser_Success() throws Exception {
         // given

         // when & then
         mockMvc.perform(MockMvcRequestBuilders
                         .post("/api/user/signup")
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(new ObjectMapper().writeValueAsString(userRequestDto)))
                 .andExpect(status().is3xxRedirection())
                 .andExpect(MockMvcResultMatchers.redirectedUrl("/api/user/loginFormPage"));
     }

     @Test
     void signup_WithValidUser_RedirectToLoginForm() throws Exception {
         // Given
         UserRequestDto userRequestDto = new UserRequestDto(); // valid user object

         // When & Then
         mockMvc.perform(MockMvcRequestBuilders.post("/api/user/signup")
                         .contentType("application/json")
                         .content("{\"email\":\"test@example.com\", \"password\":\"password\", \"name\":\"Test User\", \"phone\":\"123-456-7890\", \"address\":\"123 Test St\"}"))
                 .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                 .andExpect(MockMvcResultMatchers.redirectedUrl("/api/user/loginFormPage"));
     }

     @Test
     void checkEmail_ExistingEmail_ReturnsTrue() throws Exception {
         // Given
         String existingEmail = "existing@example.com";
         when(userService.checkEmailExists(existingEmail)).thenReturn(true);

         // When & Then
         mockMvc.perform(get("/api/user/checkEmail")
                         .param("email", existingEmail))
                 .andExpect(MockMvcResultMatchers.status().isOk())
                 .andExpect(MockMvcResultMatchers.content().string("[SUCCESS] Email available"));
     }

     @Test
     void checkEmail_ExistingEmail_ReturnsFalse() throws Exception {
         // Given
         String existingEmail = "existing@example.com";
         when(userService.checkEmailExists(existingEmail)).thenReturn(false);

         // When & Then
         mockMvc.perform(get("/api/user/checkEmail")
                         .param("email", existingEmail))
                 .andExpect(MockMvcResultMatchers.status().isOk())
                 .andExpect(MockMvcResultMatchers.content().string("false"));
     }


     @Test
     void checkEmail_ExceptionThrown_InternalServerError() throws Exception {
         // Given
         String email = "test@example.com";
         when(userService.checkEmailExists(email)).thenThrow(new RuntimeException("Something went wrong"));

         // When & Then
         mockMvc.perform(get("/api/user/checkEmail")
                         .param("email", email))
                 .andExpect(MockMvcResultMatchers.status().isInternalServerError());
     }

     @Test
     void updateUser_Success() throws Exception {
         // Given

         Long userId = 1L;

         UserRequestDto updatedUserRequestDto = UserRequestDto.builder()
                 .email("updated@example.com")
                 .password("newpassword")
                 .name("Updated User")
                 .phone("987-654-3210")
                 .address("456 Updated St")
                 .role(Role.ADMIN)
                 .build();

         when(userService.updateUser(userId, updatedUserRequestDto)).thenReturn(userResponseDto);

         // When & Then
         mockMvc.perform(MockMvcRequestBuilders.patch("/api/user/{userId}", userId)
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(new ObjectMapper().writeValueAsString(updatedUserRequestDto)))
                 .andExpect(MockMvcResultMatchers.status().isOk());
     }


     @Test
     void testLoginPage() throws Exception {
         mockMvc.perform(get("/api/user/loginFormPage"))
                 .andExpect(MockMvcResultMatchers.status().isOk())
                 .andExpect(MockMvcResultMatchers.view().name("login"));
     }

     @Test
     void testSignupPage() throws Exception {
         mockMvc.perform(get("/api/user/signupPage"))
                 .andExpect(MockMvcResultMatchers.status().isOk())
                 .andExpect(MockMvcResultMatchers.view().name("signup"));
     }


     @Test
     @WithMockUser(username = "channy", roles = "USER")
     public void testMyPage() throws Exception {

         when(userService.getUser(user.getId())).thenReturn(userResponseDto);
         when(userDetails.getUser()).thenReturn(user);
         mockMvc.perform(get("/api/user/mypagePage"))
                 .andExpect(MockMvcResultMatchers.status().isOk())
                 .andExpect(MockMvcResultMatchers.view().name("myPage"))
                 .andExpect(MockMvcResultMatchers.model().attributeExists("channy"));
     }

     @Test
     void testErrorPage() throws Exception {
         mockMvc.perform(get("/api/user/errorPage"))
                 .andExpect(MockMvcResultMatchers.status().isOk())
                 .andExpect(MockMvcResultMatchers.view().name("error"));
     }
 }
