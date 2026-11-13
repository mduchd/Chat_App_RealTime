
package com.teamchat.realtime_chat.dto;

import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(

        @NotNull(message = "ID thành viên không được để trống")
        Long userId

) {
}
