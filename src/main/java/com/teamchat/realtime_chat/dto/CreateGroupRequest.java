
package com.teamchat.realtime_chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateGroupRequest(

        @NotBlank(message = "Tên nhóm không được để trống")
        String groupName,

        @NotNull(message = "Danh sách thành viên không được null")
        List<Long> memberIds

) {
}
