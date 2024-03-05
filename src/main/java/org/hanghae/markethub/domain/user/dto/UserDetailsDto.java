package org.hanghae.markethub.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hanghae.markethub.global.constant.Role;

@Getter
@NoArgsConstructor
@Builder
public class UserDetailsDto {
    private String email;
    private String username;
    private Role role;


    public UserDetailsDto(String email, String username, Role role) {
        this.email = email;
        this.username = username;
        this.role = role;
    }
}
